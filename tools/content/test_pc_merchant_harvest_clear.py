"""Original month guard and per-city clear block clear harvest in August.

Following facility notifications/disaster generation/settings are not emulated.
"""
import struct
import unittest
import test_pc_city_action_costs as support
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP, UC_X86_REG_EDI, UC_X86_REG_EBP


class MerchantHarvestClearTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        support.CityActionCostsTest.setUpClass(); cls.t = support.CityActionCostsTest()

    def test_original_month_guard_and_all_city_flags(self):
        t = self.t; d = t.d; u = d.u; global_state = 0x7201958
        for month in range(1, 13):
            for address, value in [(global_state+8, 190), (global_state+12, month), (global_state+16, 1), (global_state+0x5c, 0)]:
                u.mem_write(address, struct.pack('<i', value))
            u.reg_write(UC_X86_REG_ECX, global_state)
            self.assertEqual(month, t.call(0x4824f0))
            cities = []
            for native_id in range(42):
                u.reg_write(UC_X86_REG_ECX, global_state); city = t.call(0x490a10, native_id)
                self.assertTrue(0x7200000 <= city < 0x7500000)
                flags = native_id % 8
                u.mem_write(city+0x9c, struct.pack('<I', flags)); cities.append((city, flags))
            before = bytearray(u.mem_read(0x7200000, 0x300000)); rng = bytes(u.mem_read(0x8a5d44, 4))
            # Observation boundaries stop at the actual guard destination. No
            # getter, setter, instruction or rule result is replaced.
            stop=u.hook_add(UC_HOOK_CODE,lambda machine,address,size,user:machine.emu_stop(),begin=0x58f540,end=0x58f540)
            try:
                u.reg_write(UC_X86_REG_ESP, d.stack)
                u.emu_start(0x58f52b, 0x58f584, count=10000)
                self.assertEqual(0x58f540 if month==8 else 0x58f584,u.reg_read(UC_X86_REG_EIP))
            finally:u.hook_del(stop)
            if month==8:
                for native_id in range(42):
                    u.reg_write(UC_X86_REG_EDI,native_id);u.reg_write(UC_X86_REG_EBP,0);u.reg_write(UC_X86_REG_ESP,d.stack)
                    u.emu_start(0x58f540,0x58f555,count=10000)
                    self.assertEqual(0x58f555,u.reg_read(UC_X86_REG_EIP))
            for city, flags in cities:
                expected = flags & ~4 if month == 8 else flags
                self.assertEqual(expected, struct.unpack('<I', u.mem_read(city+0x9c, 4))[0])
                struct.pack_into('<I', before, city+0x9c-0x7200000, expected)
            self.assertEqual(bytes(before), bytes(u.mem_read(0x7200000, 0x300000)))
            self.assertEqual(rng, bytes(u.mem_read(0x8a5d44, 4)))
        print('original harvest clear months=12 city observations=504 full-world/RNG unchanged except August bit2')

    def test_original_global_monthly_day_guard(self):
        t=self.t;d=t.d;u=d.u;global_state=0x7201958
        stop=u.hook_add(UC_HOOK_CODE,lambda machine,address,size,user:machine.emu_stop(),begin=0x590c79,end=0x590c79)
        try:
            for month in range(1,13):
                for day in (1,11,21):
                    for address,value in [(global_state+8,190),(global_state+12,month),(global_state+16,day),(global_state+0x5c,0)]:u.mem_write(address,struct.pack('<i',value))
                    before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
                    u.reg_write(UC_X86_REG_ESP,d.stack);u.emu_start(0x590c67,0x590e7c,count=10000)
                    self.assertEqual(0x590c79 if day==1 else 0x590e7c,u.reg_read(UC_X86_REG_EIP))
                    self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)));self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)))
        finally:u.hook_del(stop)
        print('original monthly outer day guard cases=36; full controller/settings remain separate')


if __name__ == '__main__':
    unittest.main()

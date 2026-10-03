"""Native merchant maximum before XP versus final gold debit/credit after XP.

Executes the original ordered commit block; no replacement quote/RNG callbacks.
Keeps the complete world-memory comparison used by the prior experience oracle.
"""
import struct
import unittest
import test_pc_city_action_costs as support
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESI, UC_X86_REG_EDI, UC_X86_REG_EBX, UC_X86_REG_ESP, UC_X86_REG_EIP


class MerchantRoundingCapacityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        support.CityActionCostsTest.setUpClass()
        cls.t = support.CityActionCostsTest()

    def test_capacity_to_commit_boundary(self):
        t = self.t; d = t.d; u = d.u
        building, city, district, _ = t.actors()
        officer = d.root + 0xc0bc; command = d.stream + 0xc00
        u.reg_write(UC_X86_REG_ECX, officer); t.call(0x489f10)
        for offset, value in [(0xa0, 0), (0xa4, -1), (0x60, -1), (0x15c, 0)]:
            u.mem_write(officer + offset, struct.pack('<i', value))
        u.mem_write(officer + 0xd0, struct.pack('<5i', *([-1] * 5)))
        count = 0; credits = []
        for base in (1, 50, 80, 99):
            for rate in (30, 50, 70):
                for buy in (False, True):
                    gold, food = (50000, 900000)
                    u.mem_write(officer + 0xc8, bytes([50, 50, 50, base, 50]))
                    u.mem_write(officer + 0x12a, b'\0' * 10)
                    u.mem_write(officer + 0x130, struct.pack('<H', 95))
                    u.mem_write(officer + 0xae, struct.pack('<H', 100))
                    u.mem_write(city + 0x44, struct.pack('<II', gold, food))
                    u.mem_write(city + 0x7c, bytes([rate]))
                    u.mem_write(city + 0xa4, b'\x01'); u.mem_write(district + 0x2c, b'\x3c')
                    u.reg_write(UC_X86_REG_ECX, officer); t.call(0x48a2d0)
                    maximum = t.call(0x5ca770, building, officer, int(buy))
                    amount = maximum if buy else -maximum
                    u.mem_write(command, struct.pack('<III', building, officer, amount & 0xffffffff))
                    before = bytearray(u.mem_read(0x7200000, 0x300000))
                    rng = bytes(u.mem_read(0x8a5d44, 4))
                    u.reg_write(UC_X86_REG_ESI, command); u.reg_write(UC_X86_REG_EDI, officer)
                    u.reg_write(UC_X86_REG_EBX, city); u.reg_write(UC_X86_REG_ESP, d.stack)
                    u.emu_start(0x5cacb7, 0x5cad20, count=50000)
                    self.assertEqual(0x5cad20, u.reg_read(UC_X86_REG_EIP))
                    politics = min(100, base + 1)
                    quote = amount * (450-politics)*10//(rate*400) if buy else -(maximum*3200//((450-politics)*rate))
                    actual_gold, actual_food = struct.unpack('<II', u.mem_read(city + 0x44, 8))
                    self.assertEqual(min(100000, max(0, gold-quote)), actual_gold)
                    self.assertEqual(food+amount, actual_food)
                    if not buy: credits.append((base, rate, maximum, gold-quote, actual_gold))
                    offset = officer-0x7200000
                    struct.pack_into('<H', before, offset+0x130, 100)
                    struct.pack_into('<H', before, offset+0xae, 150)
                    before[offset+0x173] = politics; before[offset+0x178] = politics
                    offset = city-0x7200000
                    struct.pack_into('<II', before, offset+0x44, actual_gold, actual_food)
                    before[offset+0xa4] = 3; before[district+0x2c-0x7200000] = 40
                    self.assertEqual(bytes(before), bytes(u.mem_read(0x7200000, 0x300000)))
                    self.assertEqual(rng, bytes(u.mem_read(0x8a5d44, 4)))
                    count += 1
        self.assertEqual(24, count)
        print('original merchant maximum/XP final-credit cases=', count, 'sell boundary=', credits)


if __name__ == '__main__':
    unittest.main()

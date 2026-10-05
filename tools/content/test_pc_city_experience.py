"""Execute fixed city-command experience call sites in the read-only PC EXE.

Only the award blocks and the original award/current-ability functions execute.
Full command dispatch, three-actor effects and cultivation are separate work.
"""
import hashlib
import struct
import unittest

import test_pc_city_action_costs as support
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_EIP, UC_X86_REG_ESI, UC_X86_REG_ESP

CALLS = [
    ('recruit', 0x5c3b36, 0x5c3b47, 4, '6a016a026a0456b95c899907e88935eeff'),
    ('production', 0x5c66aa, 0x5c66bb, 2, '6a016a026a0256b95c899907e8150aeeff'),
    ('training', 0x5c4365, 0x5c4376, 1, '6a016a026a0156b95c899907e85a2deeff'),
    ('patrol', 0x5cbee7, 0x5cbef8, 0, '6a016a026a0056b95c899907e8d8b1edff'),
]


class CityExperienceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        support.CityActionCostsTest.setUpClass()
        cls.t = support.CityActionCostsTest()
        cls.d = cls.t.d

    def test_original_named_commands_and_experience_properties(self):
        t, d = self.t, self.d
        u = d.u
        for index, name in [(1, '徵兵'), (2, '生產'), (3, '巡察'), (6, '訓練')]:
            u.reg_write(UC_X86_REG_EAX, index)
            u.emu_start(0x48f1a0, 0x48f1b0, count=10)
            pointer = u.reg_read(UC_X86_REG_EAX)
            self.assertEqual(name, bytes(u.mem_read(pointer, 32)).split(b'\0')[0].decode('big5'))
        t.call(0x73ca80)
        for prop, name in [(30, '統率經驗'), (31, '武力經驗'), (32, '智力經驗'), (33, '政治經驗'), (34, '魅力經驗')]:
            u.reg_write(UC_X86_REG_ESI, prop)
            u.reg_write(UC_X86_REG_ESP, d.stack)
            u.emu_start(0x4c86eb, 0x4c86f6, count=100)
            pointer = u.reg_read(UC_X86_REG_ECX)
            self.assertEqual(name, bytes(u.mem_read(pointer, 32)).split(b'\0')[0].decode('big5'))

    def test_original_fixed_awards_full_world_and_rng(self):
        t, d = self.t, self.d
        u = d.u
        t.actors()
        officer = d.root + 0xc0bc
        u.reg_write(UC_X86_REG_ECX, officer)
        t.call(0x489f10)
        # Same isolated city-officer fields as the proven merchant oracle.
        for offset, value in [(0x9c, 0), (0xa0, 0), (0xa4, -1), (0x60, -1), (0x15c, 0)]:
            u.mem_write(officer + offset, struct.pack('<i', value))
        u.mem_write(officer + 0xd0, struct.pack('<5i', *([-1]*5)))
        self.assertEqual(1, t.call(0x47a630, officer))
        self.assertEqual(0, t.call(0x4a54a0, officer))
        for location in (-1, 0, 41, 86):
            u.mem_write(officer + 0x9c, struct.pack('<i', location))
            before = bytes(u.mem_read(0x7200000, 0x300000))
            rng = bytes(u.mem_read(0x8a5d44, 4))
            self.assertEqual(0, t.call(0x4a54a0, officer))
            self.assertEqual(before, bytes(u.mem_read(0x7200000, 0x300000)))
            self.assertEqual(rng, bytes(u.mem_read(0x8a5d44, 4)))
        u.mem_write(officer + 0x9c, struct.pack('<i', 0))
        count = 0
        for name, start, end, stat, expected in CALLS:
            actual = bytes(u.mem_read(start, end-start))
            self.assertEqual(expected, actual.hex(), name)
            for base in (1, 50, 80, 99, 100):
                for xp in (0, 98, 99, 100, 2998, 2999, 3000):
                    for injury in (-1, 0, 1, 2, 3):
                        u.mem_write(officer + 0xc8, bytes([base]*5))
                        values = [0]*5
                        values[stat] = xp
                        u.mem_write(officer + 0x12a, struct.pack('<5H', *values))
                        u.mem_write(officer + 0x15c, struct.pack('<i', injury))
                        u.reg_write(UC_X86_REG_ECX, officer)
                        t.call(0x48a2d0)
                        before = bytearray(u.mem_read(0x7200000, 0x300000))
                        rng = bytes(u.mem_read(0x8a5d44, 4))
                        u.reg_write(UC_X86_REG_ESI, officer)
                        u.reg_write(UC_X86_REG_ESP, d.stack)
                        u.emu_start(start, end, count=50000)
                        self.assertEqual(end, u.reg_read(UC_X86_REG_EIP), (name, base, xp, injury))
                        after_xp = min(3000, xp + 2)
                        offset = officer - 0x7200000
                        struct.pack_into('<H', before, offset + 0x12a + 2*stat, after_xp)
                        full = min(100, base + after_xp//100)
                        scale = 100 if stat == 4 or injury < 1 else (80, 50, 30)[injury-1]
                        before[offset + 0x170 + stat] = max(1, full*scale//100)
                        before[offset + 0x175 + stat] = full
                        self.assertEqual(bytes(before), bytes(u.mem_read(0x7200000, 0x300000)), (name, base, xp, injury))
                        self.assertEqual(rng, bytes(u.mem_read(0x8a5d44, 4)))
                        count += 1
            print(name, hex(start), 'stat', stat, 'amount2 block sha256', hashlib.sha256(actual).hexdigest())
        self.assertEqual(700, count)
        print('PASS original fixed city XP observations=', count, 'complete3MiB/RNG unchanged except XP/current caches')

    def test_original_fixed_merit_calls_after_xp(self):
        t, d = self.t, self.d
        u = d.u
        officer = d.root + 0xc0bc
        u.reg_write(UC_X86_REG_ECX, officer)
        t.call(0x489f10)
        for offset, value in [(0x9c, 0), (0xa0, 0), (0xa4, -1), (0x60, -1), (0x15c, 0)]:
            u.mem_write(officer + offset, struct.pack('<i', value))
        u.mem_write(officer + 0xd0, struct.pack('<5i', *([-1]*5)))
        for name, start, end, stat, _ in CALLS:
            for merit in (0, 59949, 59950, 59999, 60000):
                u.mem_write(officer + 0xc8, bytes([50]*5))
                u.mem_write(officer + 0x12a, b'\0'*10)
                u.mem_write(officer + 0xae, struct.pack('<H', merit))
                u.reg_write(UC_X86_REG_ECX, officer)
                t.call(0x48a2d0)
                before = bytearray(u.mem_read(0x7200000, 0x300000))
                rng = bytes(u.mem_read(0x8a5d44, 4))
                u.reg_write(UC_X86_REG_ESI, officer)
                u.reg_write(UC_X86_REG_ESP, d.stack)
                u.emu_start(start, end, count=50000)
                self.assertEqual(end, u.reg_read(UC_X86_REG_EIP))
                merit_start = 0x5cbf25 if name == 'patrol' else end
                merit_end = 0x5cbf32 if name == 'patrol' else end + 13
                self.assertEqual('6a3256b95c899907e8', bytes(u.mem_read(merit_start, 9)).hex())
                u.reg_write(UC_X86_REG_ESP, d.stack)
                u.emu_start(merit_start, merit_end, count=10000)
                self.assertEqual(merit_end, u.reg_read(UC_X86_REG_EIP))
                offset = officer - 0x7200000
                struct.pack_into('<H', before, offset + 0x12a + 2*stat, 2)
                struct.pack_into('<H', before, offset + 0xae, min(60000, merit + 50))
                self.assertEqual(bytes(before), bytes(u.mem_read(0x7200000, 0x300000)), (name, merit))
                self.assertEqual(rng, bytes(u.mem_read(0x8a5d44, 4)))
        print('PASS 20 actual original fixed XP then merit50/cap60000 world/RNG observations')


if __name__ == '__main__':
    unittest.main()

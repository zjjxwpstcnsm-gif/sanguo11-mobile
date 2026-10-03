"""Bounded original age-curve/current-politics composition; no startup claims.

Curve percentages come from original x86 instructions, not a Python curve model.
Date-source flags are controlled fixture inputs, not named game settings here.
Rank and spouse references are absent; experience/injury are explicit inputs.
"""
import struct
import unittest
from unicorn.x86_const import UC_X86_REG_ECX
import test_pc_city_action_costs as support


class OfficerGrowthTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        support.CityActionCostsTest.setUpClass()
        cls.t = support.CityActionCostsTest()
        cls.d = cls.t.d

    def test_constructor_does_not_initialize_payload(self):
        t, d = self.t, self.d
        u, officer = d.u, d.root + 0xc0bc
        for byte in (0, 0xa5, 0xff):
            u.mem_write(officer, bytes([byte]) * 0x180)
            expected = bytearray(u.mem_read(0x7200000, 0x300000))
            struct.pack_into('<I', expected, officer - 0x7200000, 0x79c780)
            rng = bytes(u.mem_read(0x8a5d44, 4))
            u.reg_write(UC_X86_REG_ECX, officer)
            t.call(0x489f10)
            self.assertEqual(bytes(expected), bytes(u.mem_read(0x7200000, 0x300000)))
            self.assertEqual(rng, bytes(u.mem_read(0x8a5d44, 4)))

    def test_original_curve_dispatch_and_politics_composition(self):
        t, d = self.t, self.d
        u, officer = d.u, d.root + 0xc0bc
        u.mem_write(officer, bytes(0x180))
        u.reg_write(UC_X86_REG_ECX, officer)
        t.call(0x489f10)
        for offset, value in ((0xa0, 0), (0xa4, -1), (0x60, -1)):
            u.mem_write(officer + offset, struct.pack('<i', value))
        # 48a030 uses year-birth+1 with1970=1, and bypasses curves if1980!=0.
        # This does not identify either setting's user-facing name or default.
        for address, value in ((0x7201970, 1), (0x7201980, 0), (0x7201960, 200)):
            u.mem_write(address, struct.pack('<I', value))
        functions = (0x488df0, 0x488e10, 0x488e50, 0x488e90, 0x488ec0,
                     0x488f00, 0x488f40, 0x488f60, 0x488f80)
        cases = 0
        for curve, address in enumerate(functions):
            for age in range(121):
                before = bytes(u.mem_read(0x7200000, 0x300000))
                rng = bytes(u.mem_read(0x8a5d44, 4))
                scale = t.call(address, age)
                self.assertEqual(before, bytes(u.mem_read(0x7200000, 0x300000)))
                self.assertEqual(rng, bytes(u.mem_read(0x8a5d44, 4)))
                u.mem_write(officer + 0x48, struct.pack('<i', 201 - age))
                u.mem_write(officer + 0xd0, struct.pack('<5i', *([curve] * 5)))
                for base, xp, injury in ((1, 0, 0), (50, 95, 1), (80, 100, 2),
                                         (99, 2995, 3), (100, 3000, -1)):
                    u.mem_write(officer + 0xc8, bytes([base] * 5))
                    u.mem_write(officer + 0x12a, struct.pack('<5H', *([xp] * 5)))
                    before = bytes(u.mem_read(0x7200000, 0x300000))
                    rng = bytes(u.mem_read(0x8a5d44, 4))
                    u.reg_write(UC_X86_REG_ECX, officer)
                    actual = t.call(0x48a110, 3, injury & 0xffffffff) & 0xff
                    expected = max(1, min(100, base * scale // 100 + xp // 100))
                    factor = {-1: 100, 0: 100, 1: 80, 2: 50, 3: 30}[injury]
                    expected = max(1, expected * factor // 100)
                    self.assertEqual(expected, actual, (curve, age, base, xp, injury))
                    self.assertEqual(before, bytes(u.mem_read(0x7200000, 0x300000)))
                    self.assertEqual(rng, bytes(u.mem_read(0x8a5d44, 4)))
                    cases += 1
        self.assertEqual(5445, cases)
        print('original age-curve/current-politics cases=', cases)


if __name__ == '__main__':
    unittest.main()

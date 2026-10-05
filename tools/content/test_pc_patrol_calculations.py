"""Execute native patrol arithmetic with real validators and empty surrounding map.

This proves bounded arithmetic, not full admission, enemy range or crew UI parity.
No game behavior is hooked and the PC directory is read only.
"""
import struct
import unittest
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_EIP, UC_X86_REG_ESP
from inspect_pc_scenario_tail import NativeTailDecoder


class PatrolCalculationsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        root = Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
        cls.d = NativeTailDecoder((root / 'san11pk.exe').read_bytes())
        cls.d.decode_tail((root / 'Media/scenario/Scenario.s11').read_bytes(), True)
        # Original 4843a0 reads a 200x200 array, twenty bytes per map cell.
        # Zero low two bits means no occupying unit for original 4b9960.
        cls.d.u.mem_map(0x6fb0000, 0x100000)

    def call(self, address, *args):
        d = self.d
        d.u.reg_write(UC_X86_REG_ESP, d.stack)
        d.u.mem_write(d.stack, struct.pack('<' + 'I' * (1 + len(args)), d.stop, *args))
        d.u.emu_start(address, d.stop, count=100000)
        self.assertEqual(d.stop, d.u.reg_read(UC_X86_REG_EIP))
        return d.u.reg_read(UC_X86_REG_EAX)

    def actors(self):
        d = self.d
        building, city = d.root + 0x89730, d.root + 0x1d8
        d.u.reg_write(UC_X86_REG_ECX, building)
        self.call(0x4880a0)
        d.u.mem_write(building + 8, struct.pack('<I', 0))
        crew = [d.root + 0xc0bc + n * 0x190 for n in range(3)]
        for officer in crew:
            d.u.reg_write(UC_X86_REG_ECX, officer)
            self.call(0x489f10)
            d.u.mem_write(officer + 0xa0, struct.pack('<I', 0))
        for actor in (building, city, *crew):
            self.assertEqual(1, self.call(0x47a630, actor), hex(actor))
        return building, city, crew

    def test_three_runtime_abilities_cap_and_pure_state(self):
        d = self.d
        building, city, officers = self.actors()
        crew = d.stream + 0x100
        count = 0
        for stats in ([1], [27], [28], [55], [56], [84], [100],
                      [40, 80], [80, 40], [100, 80, 60], [255, 255, 255]):
            for officer, stat in zip(officers, stats):
                d.u.mem_write(officer + 0x170, bytes([stat]))
            d.u.mem_write(crew, struct.pack('<III', *(officers[:len(stats)] + [0] * (3-len(stats)))))
            for order in (0, 80, 97, 99, 100):
                d.u.mem_write(city + 0x85, bytes([order]))
                before = bytes(d.u.mem_read(0x7200000, 0x300000))
                grid = bytes(d.u.mem_read(0x6fb0000, 0x100000))
                self.assertEqual(min(100-order, sum(stats)//28+2), self.call(0x5cba10, building, crew), (stats, order))
                self.assertEqual(before, bytes(d.u.mem_read(0x7200000, 0x300000)))
                self.assertEqual(grid, bytes(d.u.mem_read(0x6fb0000, 0x100000)))
                count += 1
        self.assertEqual(55, count)
        # An invalid first member is a real native rejection, not zero-stat work.
        d.u.mem_write(crew, struct.pack('<III', 0, officers[1], officers[2]))
        self.assertEqual(0, self.call(0x5cba10, building, crew))

    def test_runtime_field_170_is_derived_from_base_ability_zero(self):
        d = self.d
        # The original calculator has an explicit 700..799 branch that bypasses
        # age/experience/item modifiers. Use it to isolate index correspondence;
        # this does not assert ordinary officers ignore those modifiers.
        officer = d.root + 0xc0bc + 700 * 0x190
        d.u.reg_write(UC_X86_REG_ECX, officer)
        self.call(0x489f10)
        d.u.mem_write(officer + 0xa0, struct.pack('<I', 0))
        d.u.mem_write(officer + 0xc8, bytes([31, 47, 59, 73, 89]))
        before = bytearray(d.u.mem_read(0x7200000, 0x300000))
        d.u.reg_write(UC_X86_REG_ECX, officer)
        d.u.reg_write(UC_X86_REG_ESP, d.stack)
        # Execute all ten virtual calls to real48a110; stop before unrelated
        # post-update unit refresh, without replacing any arithmetic function.
        d.u.emu_start(0x48a2d0, 0x48a303, count=100000)
        self.assertEqual(0x48a303, d.u.reg_read(UC_X86_REG_EIP))
        self.assertEqual(bytes([31, 47, 59, 73, 89])*2, bytes(d.u.mem_read(officer+0x170, 10)))
        before[officer+0x170-0x7200000:officer+0x17a-0x7200000] = bytes([31, 47, 59, 73, 89])*2
        self.assertEqual(bytes(before), bytes(d.u.mem_read(0x7200000, 0x300000)))

    def test_original_surrounding_unit_predicate_halves_before_cap(self):
        d = self.d
        building, city, officers = self.actors()
        def integer(pointer, value):
            d.u.mem_write(pointer, struct.pack('<I', value))
        for n in range(2):
            force, district = d.root+0x7af8+n*0x12c, d.root+0xb20c+n*0x50
            integer(force+4, n)
            integer(district+4, n)
            integer(officers[n]+0x94, n)
            d.u.mem_write(force+0x50, bytes(8))
            d.u.mem_write(force+0x64, bytes(47))
            for actor in (force, district):
                self.assertEqual(1, self.call(0x47a630, actor))
        integer(city+0x38, 0)
        d.u.mem_write(building+0x1e, struct.pack('<HH', 50, 50))
        d.u.reg_write(UC_X86_REG_ECX, building)
        self.assertEqual(0, self.call(0x487eb0))
        crew, unit = d.stream+0x100, d.root+0x169730
        d.u.mem_write(crew, struct.pack('<III', officers[0], 0, 0))
        d.u.mem_write(officers[0]+0x170, bytes([84]))
        integer(unit+0xc, 1)
        self.assertEqual(1, self.call(0x47a630, unit))
        self.assertEqual(1, self.call(0x4b5cc0, 0, 1))
        self.assertEqual(0, self.call(0x4b5cc0, 0, 0))
        # Retain the numeric relation predicates until their UI names are
        # separately verified; the actual native callback decides the result.
        for leader, x, expected in ((1,51,2),(1,52,2),(1,53,2),(1,54,5),(0,51,5)):
            integer(unit+0xc, leader)
            d.u.mem_write(unit+0x3c, struct.pack('<HH', x, 50))
            cell = 0x6fb0e68+(x*200+50)*20
            integer(cell, 1)
            d.u.mem_write(cell+8, struct.pack('<H', 0))
            try:
                for order in (0,97,99,100):
                    d.u.mem_write(city+0x85, bytes([order]))
                    before = bytes(d.u.mem_read(0x7200000,0x300000))
                    grid = bytes(d.u.mem_read(0x6fb0000,0x100000))
                    self.assertEqual(min(100-order,expected),self.call(0x5cba10,building,crew),(leader,x,order))
                    self.assertEqual(before,bytes(d.u.mem_read(0x7200000,0x300000)))
                    self.assertEqual(grid,bytes(d.u.mem_read(0x6fb0000,0x100000)))
            finally:
                d.u.mem_write(cell,bytes(20))

        # Original ring traversal4843a0 must match the project's native odd-q
        # conversion in all six directions and both column parities, not just X.
        integer(unit+0xc,1)
        d.u.mem_write(city+0x85,bytes([0]))
        for origin_x in (50,51):
            d.u.mem_write(building+0x1e,struct.pack('<HH',origin_x,50))
            for dx in range(-4,5):
                for dy in range(-4,5):
                    x,y=origin_x+dx,50+dy
                    dq=(y-x//2)-(50-origin_x//2)
                    dr=dx
                    distance=(abs(dq)+abs(dr)+abs(dq+dr))//2
                    cell=0x6fb0e68+(x*200+y)*20
                    integer(cell,1)
                    d.u.mem_write(cell+8,struct.pack('<H',0))
                    d.u.mem_write(unit+0x3c,struct.pack('<HH',x,y))
                    before=bytes(d.u.mem_read(0x7200000,0x300000))
                    grid=bytes(d.u.mem_read(0x6fb0000,0x100000))
                    try:
                        self.assertEqual(2 if 1<=distance<=3 else 5,
                                         self.call(0x5cba10,building,crew),(origin_x,dx,dy,distance))
                        self.assertEqual(before,bytes(d.u.mem_read(0x7200000,0x300000)))
                        self.assertEqual(grid,bytes(d.u.mem_read(0x6fb0000,0x100000)))
                    finally:
                        d.u.mem_write(cell,bytes(20))

        # Predicate is directional: unit force's relation to the building force.
        force=d.root+0x7af8+0x12c
        for bit,counter,expected in ((0,0,1),(1,0,0),(2,0,1),(0,1,0),(0,255,0)):
            d.u.mem_write(force+0x50,struct.pack('<Q',bit))
            d.u.mem_write(force+0x64,bytes([counter]))
            before=bytes(d.u.mem_read(0x7200000,0x300000))
            self.assertEqual(expected,self.call(0x4b5cc0,1,0),(bit,counter))
            self.assertEqual(before,bytes(d.u.mem_read(0x7200000,0x300000)))
        d.u.mem_write(force+0x50,bytes(8))
        d.u.mem_write(force+0x64,bytes(47))


if __name__ == '__main__':
    unittest.main()

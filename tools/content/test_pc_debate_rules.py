#!/usr/bin/env python3
"""Native boundary, input mutation, RNG, and complete derived-model regression."""
import os,struct,unittest
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_ESI
from inspect_pc_debate_rules import DebateReader
from inspect_pc_debate_flow import NativeDebateFlow

class DebateRulesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.installation=Path(os.environ['PC_INSTALLATION']);cls.exe=(cls.installation/'san11pk.exe').read_bytes();cls.d=DebateReader(cls.exe)
    def test_capacity_includes_reconsider_and_does_not_mutate_world_or_rng(self):
        d=self.d
        for iq,slots in [(0,4),(69,4),(70,5),(79,5),(80,6),(89,6),(90,7),(100,7)]:
            d.u.mem_write(d.actor+0x172,bytes([iq]));d.u.reg_write(UC_X86_REG_ESI,d.actor)
            before=bytes(d.u.mem_read(0x7200000,0x300000));rng=bytes(d.u.mem_read(0x8a5d44,4))
            self.assertEqual(slots,d.call(0x51d470))
            self.assertEqual(before,bytes(d.u.mem_read(0x7200000,0x300000)))
            self.assertEqual(rng,bytes(d.u.mem_read(0x8a5d44,4)))
    def test_different_off_topic_sizes_and_original_special_priority(self):
        d=self.d
        # Topic2 is timing. Card1 is story(small),card6 is reason(large).
        self.assertEqual(1,d.comparison(2,1,6))
        self.assertEqual(0,d.comparison(2,6,1))
        self.assertEqual(-1,d.comparison(2,1,4))
        self.assertEqual(0,d.comparison(0,12,10)) #ignore beats shout
        self.assertEqual(0,d.comparison(0,10,11)) #shout beats guile
        self.assertEqual(0,d.comparison(0,11,9)) #guile beats topic
        self.assertEqual(0,d.comparison(0,1,9,2,1)) #nativebold fury overrides topic
        self.assertEqual(1,d.comparison(0,1,10,2,1)) #shout still beats bold fury
    def test_talk_bits_are_original_indices_three_to_seven(self):
        d=self.d
        for mask in range(32):
            d.u.mem_write(d.actor+0x124,struct.pack('<I',mask<<3));before=bytes(d.u.mem_read(d.actor,0x190))
            self.assertEqual([(mask>>i)&1 for i in range(5)],[d.call(0x489780,i,receiver=d.actor)for i in range(5)])
            self.assertEqual(0,d.call(0x489780,-1,receiver=d.actor));self.assertEqual(0,d.call(0x489780,5,receiver=d.actor))
            self.assertEqual(before,bytes(d.u.mem_read(d.actor,0x190)))
    def test_damage_original_rng_consumes_one_draw_and_no_model_write(self):
        d=self.d
        for seed in (0,1,23,0xffffffff):
            d.u.mem_write(d.fixture,bytes(0x200))
            for offset,value in [(0x168,0),(0x10+0x9c,1),(0x10+0x10,100)]:d.u.mem_write(d.fixture+offset,struct.pack('<i',value))
            before=bytes(d.u.mem_read(d.fixture,0x200));d.u.mem_write(0x8a5d44,struct.pack('<I',seed))
            damage=d.call(0x51ea10,0,1,receiver=d.fixture)
            next_seed=(seed*0x6c078965+0x3039)&0xffffffff
            self.assertEqual(next_seed,struct.unpack('<I',d.u.mem_read(0x8a5d44,4))[0])
            self.assertEqual(100+((next_seed>>16)%5),damage)
            self.assertEqual(before,bytes(d.u.mem_read(d.fixture,0x200)))
    def test_real_derived_model_ai_reconsider_fury_and_terminal(self):
        flow=NativeDebateFlow(self.installation,self.exe);case=flow.run([90,82],[1,2],[31,31],23)
        self.assertTrue(case['originalModelReachedTerminal']);self.assertFalse(case['campaignSettlementProven'])
        self.assertEqual(9,case['trace'][-1]['phase'])
        self.assertTrue(any(0 in row['selectedNativeCards']for row in case['trace']))
        self.assertTrue(any(any(row['fury'])for row in case['trace']))
        self.assertTrue(any(hp<=0 for hp in case['trace'][-1]['health']))
        original=bytes.fromhex(case['initialStateHex']);self.assertEqual((1000,1000),tuple(struct.unpack_from('<i',original,0x14+0xa0*i)[0]for i in range(2)))
        self.assertEqual((107,94),tuple(struct.unpack_from('<i',original,0x20+0xa0*i)[0]for i in range(2)))
        self.assertTrue(all(row['sha256'] for row in flow.original_pages))

if __name__=='__main__':unittest.main()

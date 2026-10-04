#!/usr/bin/env python3
"""Original accessor/interpreter regression, not a copied arithmetic model."""
import gzip
import json
import os
import struct
import unittest
from pathlib import Path
from inspect_pc_officer_fields import NativeFields,source_byte_map,source_value_offsets
from inspect_pc_biography_messages import NativeMessageInterpreter,text_spans
from inspect_pc_message_resources import decode
from inspect_pc_scenario_officers import ROOT,BASE


class NativeFieldTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.installation=Path(os.environ['PC_INSTALLATION']);cls.exe=(cls.installation/'san11pk.exe').read_bytes()
        cls.raw=(cls.installation/'Media/scenario/Scen000.s11').read_bytes()
        cls.native=NativeFields(cls.exe);cls.native.decode_units(cls.raw)
        for index in range(1100):cls.native.initialize_special_slot_flag(cls.actor(index))

    @classmethod
    def actor(cls,index):return cls.native.root+0xc0bc+index*0x190

    def test_unappeared_identity_and_special_slot(self):
        d=self.native;d.actor=self.actor(0)
        self.assertEqual(d.call(0x47a600,d.actor),1);self.assertEqual(d.call(0x47a630,d.actor),0)
        self.assertEqual(d.text_field(0x4905b0)['text'],'阿會喃');self.assertEqual(d.biography_selector()['messageId'],10000)
        d.actor=self.actor(700);self.assertEqual(d.text_field(0x4905b0)['text'],'靈帝')
        d.actor=self.actor(800);self.assertIn('unknown',d.biography_selector())

    def test_original_courtesy_and_exact_source_bytes(self):
        d=self.native;d.actor=self.actor(9)
        self.assertEqual(d.text_field(0x48e6d0)['text'],'伯業')
        mapping=source_byte_map(self.exe,self.raw[BASE:BASE+152])
        for offset in range(14,19):self.assertEqual(mapping[offset],[offset-4])
        d.reads=set();d.collect=True
        try:d.call(0x4c8720,d.actor,40)
        finally:d.collect=False
        health_offsets=sorted({i for off in d.reads for i in mapping.get(off,[])})
        self.assertEqual(health_offsets,[100]);self.assertEqual(source_value_offsets(40,health_offsets),[])
        self.assertEqual(source_value_offsets(20,[100]),[100])
        # Alter only the source courtesy-name bytes; original serializer/getter
        # must follow them, with native ID/other fields retained.
        changed=bytearray(self.raw);changed[BASE+9*152+10:BASE+9*152+15]='玄德'.encode('big5')+b'\0'
        probe=NativeFields(self.exe);probe.decode_units(bytes(changed));probe.actor=probe.root+0xc0bc+9*0x190
        self.assertEqual(probe.text_field(0x48e6d0)['text'],'玄德')
        self.assertEqual(probe.text_field(0x4905b0)['text'],'袁遺')

    def test_ancient_selector_is_not_project_id_or_native_index(self):
        d=self.native;d.actor=self.actor(800);before=bytes(d.u.mem_read(d.actor,0x190))
        try:
            # Synthetic valid ancient-slot actor, not evidence of scenario start.
            d.u.mem_write(d.actor+0xa0,struct.pack('<I',0))
            self.assertEqual(d.biography_selector()['messageId'],10725)
        finally:d.u.mem_write(d.actor,before)

    def test_source_portrait_and_numeric_current_are_not_faked(self):
        d=self.native;self.assertEqual(d.descriptors[3]['name'],'所屬');self.assertEqual(d.descriptors[93]['name'],'說明')
        d.actor=self.actor(9);before=bytes(d.u.mem_read(0x7200000,0x300000));rng=bytes(d.u.mem_read(0x8a5d44,4))
        for field in [6,7,8,9,11,12,13,15,20,21,23,24,25,35,116]:d.call(0x4c8720,d.actor,field)
        self.assertEqual(before,bytes(d.u.mem_read(0x7200000,0x300000)));self.assertEqual(rng,bytes(d.u.mem_read(0x8a5d44,4)))

    def test_original_biography_text_and_gaiji_are_retained(self):
        raw=(self.installation/'Media/msg/S11MSG02.s11').read_bytes();decoded,_=decode(self.exe,raw)
        renderer=NativeMessageInterpreter(self.exe,decoded);first=renderer.render(10000);spans=text_spans(first)
        self.assertTrue(spans[0]['text'].startswith('孟獲的部下，第三洞的元帥。'))
        self.assertEqual(b''.join(bytes.fromhex(s['rawHex']) for s in spans),first)
        self.assertEqual({s['rawHex'] for s in spans if s['kind']=='unknown_glyph'},{'fa60','fa49'})
        self.assertTrue(renderer.render(10725).decode('big5').startswith('儒教之祖。'))
        # The entire original person biography getter uses this same installed
        # interpreter, not a separately constructed biography-to-ID formula.
        renderer=NativeMessageInterpreter(self.exe,decoded,scenario_raw=self.raw);renderer.actor=renderer.root+0xc0bc
        self.assertEqual(bytes.fromhex(renderer.text_field(0x48e850)['rawHex']),first)


if __name__=='__main__':unittest.main()

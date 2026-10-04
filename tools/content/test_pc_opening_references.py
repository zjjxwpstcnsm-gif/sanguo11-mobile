#!/usr/bin/env python3
"""Native mutation/purity regression for source-specific opening getters."""
import gzip,json,os,struct,unittest
from pathlib import Path
from inspect_pc_opening_references import OpeningReader
from inspect_pc_scenario_fields import NativeScenarioFields
from pc_startup_platform import StartupPlatform
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha

class OpeningReferencesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.installation=Path(os.environ['PC_INSTALLATION'])
        exe=(cls.installation/'san11pk.exe').read_bytes()
        if sha(exe)!=EXE_SHA:raise ValueError('Changed original executable')
        cls.world=OpeningReader(exe);cls.platform=StartupPlatform(cls.world,cls.installation)
        shared=cls.world.load((cls.installation/'Media/scenario/Scenario.s11').read_bytes(),True)
        source=cls.world.load((cls.installation/'Media/scenario/Scen000.s11').read_bytes())
        cls.addresses=NativeScenarioFields.source_addresses(shared,'Media/scenario/Scenario.s11')
        cls.addresses.update(NativeScenarioFields.source_addresses(source,'Media/scenario/Scen000.s11'))
        cls.world.call(0x73c840);cls.world.call(0x73ca80)
        cls.world.call(0x493400,receiver=cls.world.root,count=50000000)
        # These blocks were already translated by postload before observe().
        cls.world.observe()
        packed=(ROOT/'docs/handoff/20261004/session1/opening-references-native.json.gz').read_bytes()
        decoded=gzip.decompress(packed)
        if sha(packed)!='42475ecca2b130a177a4e19260ac446620b4397dc53574699895a044824e697e':
            raise ValueError('Changed comparison capture')
        cls.source=json.loads(decoded)['sources'][0]

    def test_city_gate_port_inventory_dispatch_and_observation(self):
        w=self.world
        before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4))
        for native,kind in [(0,'city'),(44,'gate'),(55,'port')]:
            proxy=w.root+0x89730+native*0x38
            for field in range(45,57):
                actual=w.get(0x4c69a0,proxy,field,self.addresses)
                self.assertEqual(actual,self.source['sites'][native]['properties'][str(field)])
                dependencies=actual['serializedDependencies']
                self.assertEqual(len(dependencies),4,'translated getter must report source reads')
                self.assertTrue(all(d['kind']==kind for d in dependencies))
                address=int(dependencies[0]['address'],16)
                original=bytes(w.u.mem_read(address,4))
                try:
                    w.u.mem_write(address,struct.pack('<I',1234+field))
                    self.assertEqual(w.get(0x4c69a0,proxy,field,self.addresses)['value'],1234+field)
                finally:w.u.mem_write(address,original)
        self.assertEqual(before,bytes(w.u.mem_read(0x7200000,0x300000)))
        self.assertEqual(rng,bytes(w.u.mem_read(0x8a5d44,4)))

    def test_all_coordinates_parent_table_and_person_force_join(self):
        w=self.world
        before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4))
        for site in self.source['sites']:
            proxy=w.root+0x89730+site['nativeBuildingId']*0x38
            for field in (9,10):
                self.assertEqual(w.get(0x4c69a0,proxy,field,self.addresses),site['properties'][str(field)])
            self.assertEqual(w.call(0x4839f0,site['shexRegion']),site['parentNativeCityId'])
        # Source365 is the original installed Sun Jian record. Its owning force
        # is2; do not use the different affiliated-building property3 as force.
        actor=w.root+0xc0bc+365*0x190
        self.assertEqual(w.get(0x4c8720,actor,75,self.addresses)['value'],2)
        self.assertEqual(w.get(0x4c8720,actor,75,self.addresses),self.source['people'][365]['properties']['75'])
        self.assertEqual(before,bytes(w.u.mem_read(0x7200000,0x300000)))
        self.assertEqual(rng,bytes(w.u.mem_read(0x8a5d44,4)))

if __name__=='__main__':unittest.main()

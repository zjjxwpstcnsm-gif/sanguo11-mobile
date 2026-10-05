"""Original officer loop, source mutation, and live native unit validation."""
import struct
import unittest
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_EIP, UC_X86_REG_ESP
from inspect_pc_scenario_units import NativeScenarioUnitDecoder
from inspect_pc_scenario_officers import NativeOfficerDecoder, BASE, STRIDE


class ScenarioUnitTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.root=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
        cls.exe=(cls.root/'san11pk.exe').read_bytes()
        cls.raw=(cls.root/'Media/scenario/Scen000.s11').read_bytes()

    def test_officer_source_correspondence_and_mutation(self):
        d=NativeScenarioUnitDecoder(self.exe)
        report=d.decode_units(self.raw)
        self.assertEqual((1000,0), (report['unit_count'],report['valid_unit_count']))
        self.assertTrue(all(row['source_bytes']==0 for row in report['units']))
        self.assertEqual([],report['fixup']['changed_byte_addresses'])
        reference=NativeOfficerDecoder(self.exe)
        for index in (0,1,184,616,849):
            decoded,actor=reference.decode(self.raw[BASE+index*STRIDE:BASE+(index+1)*STRIDE],index)
            address=d.root+0xc0bc+index*0x190
            self.assertEqual(decoded['stats'],list(d.u.mem_read(address+0xc8,5)))
            self.assertEqual(actor[4:14],bytes(d.u.mem_read(address+4,10)))
            self.assertEqual(actor[0x3c:0x50],bytes(d.u.mem_read(address+0x3c,20)))
            self.assertEqual(actor[0xb0:0xc8],bytes(d.u.mem_read(address+0xb0,24)))
        first=d.records[0]
        offset=next(r['offset'] for r in first['reads'] if r['destination']=='actor+0xc8')
        changed=bytearray(self.raw)
        changed[offset]=17 if changed[offset]!=17 else 18
        mutated=d.decode_units(bytes(changed))
        self.assertEqual(changed[offset],d.u.mem_read(d.root+0xc0bc+0xc8,1)[0])
        self.assertNotEqual(report['officers']['registry_sha256'],mutated['officers']['registry_sha256'])
        self.assertEqual(0,mutated['valid_unit_count'])
        self.assertEqual(self.raw,(self.root/'Media/scenario/Scen000.s11').read_bytes(),'PC source was never changed')

    def test_unit_validity_really_resolves_a_live_officer(self):
        d=NativeScenarioUnitDecoder(self.exe)
        d.decode_units(self.raw)
        def valid(actor):
            d.u.reg_write(UC_X86_REG_ESP,d.stack)
            d.u.mem_write(d.stack,struct.pack('<II',d.stop,actor))
            d.u.emu_start(0x47a630,d.stop,count=100000)
            self.assertEqual(d.stop,d.u.reg_read(UC_X86_REG_EIP))
            return d.u.reg_read(UC_X86_REG_EAX)
        leader=next(i for i in range(850) if valid(d.root+0xc0bc+i*0x190))
        unit=d.root+0x169730
        self.assertEqual(0,valid(unit))
        original=bytes(d.u.mem_read(unit+0xc,4))
        d.u.mem_write(unit+0xc,struct.pack('<I',leader))
        self.assertEqual(1,valid(unit),'the actual validator must detect a valid registry leader')
        d.u.mem_write(unit+0xc,original)
        self.assertEqual(0,valid(unit))

    def test_officer_authority_follows_native_district_reference(self):
        from inspect_pc_scenario_placements import placements
        d=NativeScenarioUnitDecoder(self.exe)
        d.decode_units(self.raw)
        original=placements(d)
        self.assertEqual(850,len(original))
        actor=d.root+0xc0bc
        # Use two actually populated source districts with distinct native forces.
        choices={}
        for index in range(47):
            force=d.scalar(0x65d6c0,d.root+0xb20c+index*0x50)
            if force>=0:
                choices.setdefault(force,index)
        self.assertGreaterEqual(len(choices),2)
        for force,index in list(choices.items())[:2]:
            d.u.mem_write(actor+0x94,struct.pack('<i',index))
            current=placements(d)
            self.assertEqual((index,force),(current[0]['district_native_index'],current[0]['force_native_index']))
            self.assertEqual(original[1:],current[1:],'a district-reference change affects only that officer')
        d.u.mem_write(actor+0x94,struct.pack('<i',-1))
        self.assertEqual(-1,placements(d)[0]['force_native_index'])

    def test_original_status_and_ability_label_lookups(self):
        from inspect_pc_scenario_placements import native_labels
        d=NativeScenarioUnitDecoder(self.exe)
        labels=native_labels(d)
        self.assertEqual(['君主','都督','太守','一般','在野','俘虜','未登','未發','死亡'],
                         [row['name'] for row in labels['status']['entries']])
        self.assertEqual(['統率','武力','智力','政治','魅力'],
                         [row['name'] for row in labels['ability']['entries']])
        for group in labels.values():
            for row in group['entries']:
                raw=bytes.fromhex(row['raw_hex'])
                self.assertEqual(raw+b'\0',bytes(d.u.mem_read(int(row['pointer'],16),len(raw)+1)))
        self.assertEqual(labels,native_labels(d),'repeated native lookups are stable')


if __name__=='__main__':
    unittest.main()

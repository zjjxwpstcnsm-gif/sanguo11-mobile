#!/usr/bin/env python3
"""Native layered-reader regression; no Android fixture or PC process launch."""
import os
import struct
import tempfile
import unittest
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_ESP
from inspect_pc_layered_scenario import NativeLayeredWorld
from inspect_pc_scenario_fields import NativeScenarioFields
from pc_readonly_platform import ReadOnlyPlatform


class LayeredScenarioTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.installation = Path(os.environ['PC_INSTALLATION'])
        cls.exe = (cls.installation / 'san11pk.exe').read_bytes()
        cls.shared = (cls.installation / 'Media/scenario/Scenario.s11').read_bytes()
        cls.scenario = (cls.installation / 'Media/scenario/Scen000.s11').read_bytes()
        cls.world = NativeScenarioFields(cls.exe)
        cls.shared_report = cls.world.load(cls.shared, True)
        cls.report = cls.world.load(cls.scenario)

    def reload(self, raw):
        self.world.load(self.shared, True)
        return self.world.load(raw)

    def property(self, getter, actor, field):
        before = bytes(self.world.u.mem_read(0x7200000, 0x300000))
        rng = bytes(self.world.u.mem_read(0x8a5d44, 4))
        value = self.world.call(getter, actor, field)
        self.assertEqual(before, bytes(self.world.u.mem_read(0x7200000, 0x300000)))
        self.assertEqual(rng, bytes(self.world.u.mem_read(0x8a5d44, 4)))
        return value

    def test_complete_layered_read_and_shared_name(self):
        report = self.reload(self.scenario)
        self.assertEqual(len(self.shared), self.shared_report['readEnd'])
        self.assertEqual(len(self.scenario), report['readEnd'])
        self.assertFalse(report['sharedPostloadExecuted'])
        self.assertEqual([], report['invalidMemory'])
        names = [bytes(self.world.u.mem_read(self.world.root + 0x1d8 + i * 0x248 + 4, 5))
                 .split(b'\0')[0].decode('big5') for i in range(3)]
        self.assertEqual(['襄平', '北平', '薊'], names)
        self.assertEqual(850, sum(r['kind'] == 'officer' and r['bytes'] == 152 for r in report['records']))
        self.assertEqual(16384, sum(r['kind'] == 'grid' for r in report['records']))
        self.assertEqual(150, sum(r['kind'] == 'table_1a5050' for r in report['records']))
        self.assertEqual(50, sum(r['kind'] == 'table_1ba5ea' for r in report['records']))

    def test_city_troops_source_perturbation(self):
        report = self.reload(self.scenario)
        record = next(r for r in report['records'] if r['kind'] == 'city' and r['native_index'] == 1)
        request = next(r for r in record['reads'] if r['destination'] == 'actor+0x40')
        city = self.world.root + 0x1d8 + 0x248
        before = self.property(0x4c0c30, city, 7)
        raw = bytearray(self.scenario)
        stored = struct.unpack_from('<I', raw, request['offset'])[0]
        self.assertEqual(stored, before)
        struct.pack_into('<I', raw, request['offset'], stored + 1)
        self.reload(bytes(raw))
        self.assertEqual(before + 1, self.property(0x4c0c30, city, 7))
        self.reload(self.scenario)
        self.assertEqual(before, self.property(0x4c0c30, city, 7))

    def test_force_identity_and_diplomacy_source_perturbations(self):
        report = self.reload(self.scenario)
        record = next(r for r in report['records'] if r['kind'] == 'force' and r['native_index'] == 2)
        actor = self.world.root + 0x7af8 + 2 * 0x12c
        ruler = self.property(0x4c4260, actor, 3)
        self.assertEqual(365, ruler)
        raw = bytearray(self.scenario)
        struct.pack_into('<H', raw, record['offset'], ruler + 1)
        self.reload(bytes(raw))
        self.assertEqual(ruler + 1, self.property(0x4c4260, actor, 3))
        self.reload(self.scenario)
        relation = self.property(0x4c4260, actor, 224)
        request = next(r for r in record['reads'] if r['destination'] == 'actor+0xc')
        raw = bytearray(self.scenario)
        self.assertEqual(raw[request['offset']], relation)
        raw[request['offset']] += 1
        self.reload(bytes(raw))
        self.assertEqual(relation + 1, self.property(0x4c4260, actor, 224))
        self.reload(self.scenario)
        self.assertEqual(relation, self.property(0x4c4260, actor, 224))

    def test_inactive_force_does_not_claim_zero_truth(self):
        self.reload(self.scenario)
        actor = self.world.root + 0x7af8
        active, values = self.world.properties(actor, 0x12c, 0x4c4260,
                    [self.world.descriptors['force'][3]], {})
        self.assertFalse(active)
        self.assertIn('unknown', values['3'])
        self.assertNotIn('value', values['3'])

    def test_original_finance_aggregates_complete_without_source_claim(self):
        self.reload(self.scenario)
        actor = self.world.root + 0xb20c
        before = bytes(self.world.u.mem_read(0x7200000, 0x300000))
        active, values = self.world.properties(actor, 0x50, 0x4c2960,
            [self.world.descriptors['district'][i] for i in (14, 16, 17)], {})
        self.assertTrue(active)
        self.assertEqual([0, 3600, 1], [values[str(i)]['value'] for i in (14, 16, 17)])
        for value in values.values():
            self.assertFalse(value['completeOpening'])
            self.assertFalse(value['sourceValueProven'])
            self.assertEqual(2000000, value['originalInstructionBudget'])
            self.assertIn('not_collected_for_computed_aggregate', value['dependencyTrace'])
        self.assertEqual(before, bytes(self.world.u.mem_read(0x7200000, 0x300000)))

    def test_platform_import_guards_and_mutation_refusal(self):
        world = NativeLayeredWorld(self.exe)
        platform = ReadOnlyPlatform(world, self.installation)
        self.assertEqual(('KERNEL32.dll', 'InterlockedDecrement'), platform.imports(self.exe)[0x74e260])
        self.assertEqual(('KERNEL32.dll', 'OpenMutexA'), platform.imports(self.exe)[0x74e264])
        self.assertEqual(self.installation.resolve() / 'Media/script/event/00000064.eve',
                         platform.path('G:\\media\\script\\event\\00000064.eve'))
        for path in ('G:\\..\\escape', 'C:\\outside', '/outside'):
            with self.assertRaises(ValueError):
                platform.path(path)
        address = next(a for a, item in platform.callbacks.items() if item[0] == 'WriteFile')
        world.u.reg_write(UC_X86_REG_ESP, world.stack)
        world.u.mem_write(world.stack, struct.pack('<6I', world.stop, 1, 2, 3, 4, 5))
        with self.assertRaisesRegex(ValueError, 'forbidden installation mutation'):
            platform.callback(world.u, address, 1, None)
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            root = base / 'root'
            root.mkdir()
            outside = base / 'outside'
            outside.mkdir()
            (root / 'link').symlink_to(outside)
            platform.root = root.resolve()
            with self.assertRaisesRegex(ValueError, 'symlink'):
                platform.path('G:\\link\\*.eve', True)


if __name__ == '__main__':
    unittest.main()

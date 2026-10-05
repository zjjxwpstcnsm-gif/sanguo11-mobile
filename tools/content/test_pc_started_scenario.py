#!/usr/bin/env python3
"""Native startup-platform/event/postload checks, not Android flow tests."""
import os
import struct
import unittest
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_FPCW
from inspect_pc_layered_scenario import NativeLayeredWorld
from inspect_pc_started_scenario import assemble_events, ability_state, MANAGER
from pc_startup_platform import StartupPlatform
from audit_pc_restoration_sources import sha


class StartedScenarioTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.installation = Path(os.environ['PC_INSTALLATION'])
        cls.exe = (cls.installation / 'san11pk.exe').read_bytes()
        cls.world = NativeLayeredWorld(cls.exe)
        cls.platform = StartupPlatform(cls.world, cls.installation)
        cls.world.load((cls.installation / 'Media/scenario/Scenario.s11').read_bytes(), True)
        cls.world.load((cls.installation / 'Media/scenario/Scen000.s11').read_bytes())
        cls.events = assemble_events(cls.world, cls.platform)
        cls.before = bytes(cls.world.u.mem_read(0x7200000, 0x300000))
        cls.rng_before = bytes(cls.world.u.mem_read(0x8a5d44, 4))
        cls.postload = cls.world.call(0x493400, receiver=cls.world.root, count=50000000)

    def test_original_crt_context_and_float_precision(self):
        self.assertEqual(0x23f, self.world.u.reg_read(UC_X86_REG_FPCW))
        self.assertEqual(0, self.world.call(0x709048, 950))
        self.assertEqual(1, len(self.platform.fibers))
        self.assertNotEqual(0, self.platform.fibers[0]['value'])
        self.assertTrue(any(c['function'] == 'GetCPInfo' for c in self.platform.calls))

    def test_complete_original_installed_assembly_and_lazy_bodies(self):
        events = self.events
        self.assertEqual(326, events['recordCount'])
        self.assertEqual([0, 0, 0, 0, 31, 116, 20, 9, 19, 0, 1, 28, 80, 0, 0, 2, 4, 1, 13, 2], events['sectionRecordCounts'])
        self.assertEqual(216908, events['decodedMetadataBytes'])
        self.assertEqual(1096968, sum(r['storedBodyBytes'] for r in events['originalLazyBodyReferences']))
        self.assertEqual(326, len({r['stableId'] for r in events['originalLazyBodyReferences']}))
        for row in events['originalLazyBodyReferences']:
            raw = (self.installation / row['sourcePath']).read_bytes()
            self.assertEqual(row['sourceSha256'], sha(raw))
            body = raw[row['sourceFileOffset']:row['sourceFileOffset'] + row['storedBodyBytes']]
            self.assertEqual(body.hex(), row['bodyHex'])
            self.assertEqual(sha(body), row['bodySha256'])
            self.assertFalse(row['conditionsExecuted'])
            self.assertFalse(row['effectsExecuted'])
            self.assertFalse(row['actualActivationProven'])
        self.assertFalse(events['completeStartup'])
        self.assertFalse(events['openingEventsExecuted'])
        self.assertEqual({}, self.platform.files)
        self.assertEqual({}, self.platform.directories)

    def test_external_coverage_unknown_and_original_documents_suffix(self):
        calls = [c for c in self.platform.calls if c['function'] == 'FindFirstFileA']
        external = [c for c in calls if '__fixture_documents__' in c['path']]
        self.assertEqual(1, len(external))
        self.assertEqual('G:\\__fixture_documents__\\Koei\\San11 Tc\\Expansion/*.eve', external[0]['path'])
        self.assertEqual([], external[0]['matches'])
        self.assertFalse(next(c for c in self.platform.calls if c['function'] == 'SHGetSpecialFolderLocation')['actualWindowsDocumentsProven'])
        self.assertEqual({}, self.platform.pidls)
        with self.assertRaises(ValueError):
            self.platform.path('G:\\../escape')

    def test_original_postload_cache_calculations_and_readonly_queries(self):
        self.assertEqual(1, self.postload)
        before = bytes(self.world.u.mem_read(0x7200000, 0x300000))
        rng = bytes(self.world.u.mem_read(0x8a5d44, 4))
        checked = 0
        for native in (0, 1, 279, 333, 365, 699, 700, 799, 800, 849):
            actor = self.world.root + 0xc0bc + native * 0x190
            if self.world.call(0x47a630, actor):
                state = ability_state(self.world, actor, True)
                self.assertTrue(state['nativeCacheComparison'])
                self.assertFalse(state['currentValuesSourceProven'])
                self.assertFalse(state['completeOpening'])
                checked += 1
        self.assertGreater(checked, 3)
        self.assertNotEqual(self.before, before)
        self.assertEqual(self.rng_before, rng)
        self.assertEqual(before, bytes(self.world.u.mem_read(0x7200000, 0x300000)))
        self.assertEqual(rng, bytes(self.world.u.mem_read(0x8a5d44, 4)))

    def test_original_inactive_actor_cache_is_not_zero_ability_truth(self):
        actor = self.world.root + 0xc0bc
        self.assertTrue(self.world.call(0x47a600, actor))
        self.assertFalse(self.world.call(0x47a630, actor))
        state = ability_state(self.world, actor, True)
        self.assertFalse(state['nativeCacheComparison'])
        self.assertEqual([0] * 5, state['cachedWithCondition'])
        self.assertEqual([65, 74, 26, 33, 44], state['nativeComputedWithCondition'])
        self.assertIn('activity gate', state['unknown'])

    def test_original_extra_slot_activation_is_not_identity_coverage(self):
        actor = self.world.root + 0xc0bc + 700 * 0x190
        offset = actor - 0x7200000
        self.assertEqual(0, struct.unpack_from('<I', self.before, offset + 0x17c)[0])
        self.assertEqual(1, struct.unpack('<I', self.world.u.mem_read(actor + 0x17c, 4))[0])
        # No identity is synthesized by the loader from this slot flag.
        self.assertTrue(self.world.call(0x47a600, actor))

    def test_unknown_chinese_collation_rejected_without_substitute(self):
        with self.assertRaisesRegex(ValueError, 'NLS classification'):
            self.platform.character_type('劉')
        with self.assertRaisesRegex(ValueError, 'unverified Windows NLS collation'):
            self.world.call(self.platform.dynamic['CompareStringW'], 0x404, 0, 0, 1, 0, 1)

    def test_readonly_io_rejects_native_write_requests(self):
        callback = next(a for a, spec in self.platform.callbacks.items() if spec[0] == 'WriteFile')
        with self.assertRaisesRegex(ValueError, 'forbidden installation mutation'):
            self.world.call(callback, 0, 0, 0, 0, 0)


if __name__ == '__main__':
    unittest.main()

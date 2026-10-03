#!/usr/bin/env python3
"""Independent six-portrait evidence plus malformed and relocated record regressions."""
import json
import os
import unittest
from pathlib import Path

from inspect_pc_scenario_officers import NativeOfficerDecoder, BASE, STRIDE, ROOT


class NativeRecordsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.install = Path(os.environ['PC_INSTALLATION'])
        cls.decoder = NativeOfficerDecoder((cls.install / 'san11pk.exe').read_bytes())

    def record(self, source, native):
        raw = (self.install / source).read_bytes()
        return raw[BASE + STRIDE * native:BASE + STRIDE * (native + 1)]

    def test_independent_six_person_evidence_all_scenarios(self):
        evidence = json.loads((ROOT / 'docs/pc-visual/scenario-portraits-source-working.json').read_text())
        checked = 0
        for person in evidence['bindings']:
            for source in person['scenario_records']:
                row, actor = self.decoder.decode(self.record(source['source'], person['native_index']))
                for key in ('name', 'sex', 'birth', 'age_change'):
                    self.assertEqual(person[key], row[key], (source['source'], person['native_index'], key))
                for key in ('appearance', 'death', 'face_id'):
                    self.assertEqual(source[key], row[key])
                self.assertEqual(0x16c, len(actor))
                checked += 1
        self.assertEqual(96, checked)

    def test_scenario_local_identity_relocation(self):
        a, _ = self.decoder.decode(self.record('Media/scenario/Scen014.S11', 279))
        b, _ = self.decoder.decode(self.record('Media/scenario/Scen014.S11', 333))
        self.assertEqual(('宋憲', 157), (a['name'], a['birth']))
        self.assertEqual(('徐榮', 147), (b['name'], b['birth']))
        self.assertNotEqual(a['stats'], b['stats'])

    def test_unknown_glyphs_are_not_replaced_or_guessed(self):
        for native in (184, 229, 249, 616):
            row, _ = self.decoder.decode(self.record('Media/scenario/Scen000.s11', native))
            self.assertIsNone(row['name'])
            self.assertTrue(row['name_error'])
            self.assertTrue(any(row['name_bytes']))

    def test_width_and_executable_pin_fail_closed(self):
        with self.assertRaisesRegex(ValueError, 'Truncated'):
            self.decoder.decode(bytes(151))
        with self.assertRaisesRegex(ValueError, 'Executable changed'):
            NativeOfficerDecoder(bytes(4096))

    def test_native_scenario_count_branch_and_extra_actors(self):
        source = 'Media/scenario/Scen000.s11'
        row, _ = self.decoder.decode(self.record(source, 849), 849)
        self.assertEqual('古代５０', row['name'])
        with self.assertRaisesRegex(ValueError, 'not serialized'):
            self.decoder.decode(bytes(STRIDE), 850)
        with self.assertRaisesRegex(ValueError, 'not serialized'):
            self.decoder.decode(bytes(STRIDE), 1099)
        for native, name in ((700, '靈帝'), (729, '張讓'), (749, '商人'), (800, '孔丘')):
            row, _ = self.decoder.decode(self.record(source, native), native)
            self.assertEqual(name, row['name'])


if __name__ == '__main__':
    unittest.main()

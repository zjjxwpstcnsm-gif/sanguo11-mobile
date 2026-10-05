#!/usr/bin/env python3
import os
import unittest
from pathlib import Path
from inspect_pc_scenario_metadata import NativeMetadataDecoder


class NativeMetadataTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.install = Path(os.environ['PC_INSTALLATION'])
        cls.decoder = NativeMetadataDecoder((cls.install / 'san11pk.exe').read_bytes())

    def source(self, name):
        return (self.install / 'Media/scenario' / name).read_bytes()

    def test_native_prefix_and_actor_boundary(self):
        raw = self.source('Scen000.s11')
        result = self.decoder.decode_metadata(raw)
        self.assertEqual([184, 1, 1], result['decoded']['date'])
        self.assertEqual('黃巾之亂', result['decoded']['name'])
        self.assertEqual([(90, 16093), (16183, 11), (16194, 1566)], [(r['offset'], r['bytes']) for r in result['records']])
        self.assertEqual(raw[90:17760], b''.join(bytes.fromhex(r['raw_hex']) for r in result['records']))

    def test_filename_and_embedded_id_are_different_identities(self):
        original = self.decoder.decode_metadata(self.source('SCEN006.S11'))['decoded']
        custom = self.decoder.decode_metadata(self.source('Scen015.s11'))['decoded']
        self.assertEqual(6, original['native_id'])
        self.assertEqual(6, custom['native_id'])
        self.assertEqual(('南蠻征伐', [225, 7, 1]), (original['name'], original['date']))
        self.assertEqual(('滾滾長江', [279, 3, 1]), (custom['name'], custom['date']))
        self.assertNotEqual(original, custom)


if __name__ == '__main__':
    unittest.main()

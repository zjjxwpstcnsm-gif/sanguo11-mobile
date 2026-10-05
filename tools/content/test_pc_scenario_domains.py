#!/usr/bin/env python3
"""Native scenario boundary, record relocation, and state-isolation regressions."""
import os
import struct
import unittest
from pathlib import Path

from inspect_pc_scenario_domains import NativeDomainDecoder, scenario_header, TAIL_START


class NativeDomainTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.install = Path(os.environ['PC_INSTALLATION'])
        cls.decoder = NativeDomainDecoder((cls.install / 'san11pk.exe').read_bytes())
        cls.raw = (cls.install / 'Media/scenario/Scen000.s11').read_bytes()
        cls.reference = cls.decoder.decode(cls.raw)

    def test_original_contiguous_table_boundaries(self):
        rows = self.reference['records']
        self.assertEqual(281, len(rows))
        self.assertEqual(162702, self.reference['end'])
        self.assertEqual(self.raw[TAIL_START:162702], b''.join(bytes.fromhex(r['raw_hex']) for r in rows))
        counts = {kind: sum(r['kind'] == kind for r in rows) for kind in ('item', 'force', 'district', 'city', 'gate', 'port')}
        self.assertEqual(dict(item=100, force=47, district=47, city=42, gate=10, port=35), counts)
        item = bytes.fromhex(rows[0]['actor_hex'])
        self.assertEqual('赤兔馬', item[4:].split(b'\0')[0].decode('big5'))

    def test_city_record_swap_follows_source_bytes(self):
        cities = [r for r in self.reference['records'] if r['kind'] == 'city']
        left, right = cities[0], cities[1]
        self.assertEqual(left['bytes'], right['bytes'])
        self.assertNotEqual(left['actor_hex'], right['actor_hex'])
        modified = bytearray(self.raw)
        for destination, source in ((left, right), (right, left)):
            modified[destination['offset']:destination['offset'] + destination['bytes']] = bytes.fromhex(source['raw_hex'])
        result = self.decoder.decode(bytes(modified))
        for actual, original in zip(result['records'], self.reference['records']):
            expected = right if actual['kind'] == 'city' and actual['native_index'] == 0 else left if actual['kind'] == 'city' and actual['native_index'] == 1 else original
            self.assertEqual(expected['actor_hex'], actual['actor_hex'], (actual['kind'], actual['native_index']))
        self.assertEqual(self.reference, self.decoder.decode(self.raw), 'fresh native constructors restore deterministic VM state')

    def test_wrong_versions_and_executable_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Executable changed'):
            NativeDomainDecoder(bytes(4096))
        with self.assertRaisesRegex(ValueError, 'format'):
            scenario_header(self.raw[:-1])
        for offset in (24, 28):
            modified = bytearray(self.raw)
            struct.pack_into('<I', modified, offset, 0)
            with self.assertRaisesRegex(ValueError, 'version'):
                self.decoder.decode(bytes(modified))

    def test_shared_names_and_native_type_dispatch(self):
        raw = (self.install / 'Media/scenario/Scenario.s11').read_bytes()
        result = self.decoder.decode(raw, shared=True)
        self.assertEqual((90, 2310), (result['start'], result['end']))
        names = {}
        for row in result['records']:
            if row['kind'] not in ('city', 'gate', 'port'):
                self.assertEqual(0, row['bytes'])
                continue
            actor = bytes.fromhex(row['actor_hex'])
            name = actor[4:9 if row['kind'] == 'city' else 11].split(b'\0')[0].decode('big5')
            names[row['kind'], row['native_index']] = name
        self.assertEqual(87, len(names))
        self.assertEqual(87, len(set(names.values())))
        self.assertEqual(('襄平', '壺關', '虎牢關', '劍閣', '安平港', '巫縣港'),
                         tuple(names[key] for key in (('city', 0), ('gate', 0), ('gate', 1), ('gate', 6), ('port', 0), ('port', 34))))
        self.assertEqual(self.reference, self.decoder.decode(self.raw), 'shared dispatch cannot leak names or memory into type22')

    def test_ownership_uses_native_district_indirection(self):
        city = next(r for r in self.reference['records'] if r['kind'] == 'city' and r['native_index'] == 1)
        self.assertEqual(dict(district_native_index=4, force_native_index=11), city['decoded'])
        # First type22 city field is read by47c996 to+38. Changing the district
        # must follow that district's force, not treat its index as a force ID.
        modified = bytearray(self.raw)
        self.assertEqual(4, modified[city['offset']])
        modified[city['offset']] = 3
        result = self.decoder.decode(bytes(modified))
        updated = next(r for r in result['records'] if r['kind'] == 'city' and r['native_index'] == 1)
        self.assertEqual(dict(district_native_index=3, force_native_index=5), updated['decoded'])
        modified[city['offset']] = 255
        result = self.decoder.decode(bytes(modified))
        updated = next(r for r in result['records'] if r['kind'] == 'city' and r['native_index'] == 1)
        self.assertEqual(dict(district_native_index=-1, force_native_index=-1), updated['decoded'])


if __name__ == '__main__':
    unittest.main()

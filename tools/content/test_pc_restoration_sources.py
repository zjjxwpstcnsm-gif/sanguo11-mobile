#!/usr/bin/env python3
"""Source-join regression tests; use current PC input without modifying it."""
import copy
import json
import os
import tempfile
import unittest
from pathlib import Path
from audit_pc_restoration_sources import ROOT, inventory, join, read_audits, output_guard, source_variant, inspect_header


class SourceJoinTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.installation = Path(os.environ['PC_INSTALLATION']).resolve()
        cls.files = inventory(cls.installation)
        cls.reports, cls.provenance = read_audits(ROOT / 'docs/pc-data')
        cls.catalog = ROOT / 'core/src/main/resources/content/officers.tsv'

    def run_join(self, reports=None, files=None, catalog=None):
        return join(self.installation, self.files if files is None else files,
                    self.reports if reports is None else reports, self.provenance,
                    self.catalog if catalog is None else catalog)

    def test_distinct_sources_and_relocated_people(self):
        manifest, officers, requests = self.run_join()
        self.assertEqual(manifest['coverage']['sourceSlots'], 13600)
        self.assertEqual(manifest['coverage']['verifiedIdentityRecords'], 10656)
        self.assertEqual(manifest['coverage']['distinctVerifiedOfficers'], 666)
        self.assertEqual(manifest['coverage']['completeOfficerRecords'], 0)
        self.assertEqual(len(manifest['duplicateEmbeddedIds']['6']), 2)
        self.assertTrue(all(len(s['scenarioId'])<=80 for s in manifest['scenarios']))
        swapped = {r['nativeId']: r for r in officers if r['sourcePath'] == 'Media/scenario/Scen014.S11'}
        self.assertEqual(swapped[279]['officerId'], 10333)
        self.assertEqual(swapped[333]['officerId'], 10279)
        self.assertEqual(len(requests), 13600)
        unknown = [r for r in officers if r['name'] is None]
        self.assertEqual(len(unknown), 64)
        self.assertTrue(all(r['officerId'] is None and 'name_gaiji' in r['coverage']['unknown'] for r in unknown))
        self.assertTrue(all(not r['coverage']['complete'] for r in officers))

    def test_source_sha_change_rejected(self):
        files = copy.deepcopy(self.files)
        next(r for r in files if r['path'] == 'Media/scenario/Scen000.s11')['sha256'] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'Scenario source changed'):
            self.run_join(files=files)

    def test_cross_source_record_rejected(self):
        reports = copy.deepcopy(self.reports)
        row = reports['scenario-officers']['records'][279]
        row['source'] = 'Media/scenario/Scen014.S11'
        with self.assertRaisesRegex(ValueError, 'Officer record changed'):
            self.run_join(reports=reports)

    def test_identity_disagreement_rejected(self):
        reports = copy.deepcopy(self.reports)
        reports['scenario-placements']['sources'][0]['records'][0]['project_id'] = 10669
        with self.assertRaisesRegex(ValueError, 'identity disagreement'):
            self.run_join(reports=reports)

    def test_catalog_and_prior_audit_changes_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            catalog = Path(directory) / 'catalog.tsv'
            catalog.write_bytes(self.catalog.read_bytes() + b'\n')
            with self.assertRaisesRegex(ValueError, 'catalog changed'):
                self.run_join(catalog=catalog)
        reports = copy.deepcopy(self.reports)
        reports['officer-ability-sources']['prior_audit_sha256'] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'Ability identity provenance differs'):
            self.run_join(reports=reports)

    def test_output_guard_and_distinct_identical_backups(self):
        with self.assertRaisesRegex(ValueError, 'read-only'):
            output_guard(self.installation, self.installation / 'new/audit')
        self.assertNotEqual(source_variant('Media/scenario/Scen000.s11', 'a' * 64),
                            source_variant('Backup/Scen000.s11', 'a' * 64))
        self.assertEqual(inspect_header(b'LS11' + bytes(40)), {'format': 'LS11'})
        self.assertEqual(inspect_header(b'unknown'), {'format': 'unknown'})


if __name__ == '__main__':
    unittest.main()

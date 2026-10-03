"""Provenance rejection for the real source/project comparison, no input writes."""
import gzip
import json
import tempfile
import unittest
from pathlib import Path
from compare_pc_officer_assignments import compare, ROOT


class AssignmentComparisonTest(unittest.TestCase):
    def fixture(self,folder):
        for kind in ('officers','domains','metadata','placements'):
            name='scenario-'+kind+'-native.json.gz'
            (folder/name).write_bytes((ROOT/'docs/pc-data'/name).read_bytes())

    def corrupt(self,folder,kind,edit):
        path=folder/('scenario-'+kind+'-native.json.gz')
        report=json.loads(gzip.decompress(path.read_bytes()));edit(report)
        path.write_bytes(gzip.compress(json.dumps(report).encode(),mtime=0))

    def test_reject_cross_source_and_identity_without_output(self):
        for kind,edit,message in [
            ('domains',lambda r:r['sources'][0].update(sha256='0'*64),'Scenario source evidence differs'),
            ('placements',lambda r:r.update(identity_audit_sha256='0'*64),'Placement identity audit'),
            ('metadata',lambda r:r.update(source_executable_sha256='0'*64),'Native executable evidence differs')]:
            with self.subTest(kind=kind),tempfile.TemporaryDirectory() as temp:
                folder=Path(temp);self.fixture(folder);self.corrupt(folder,kind,edit)
                output=folder/'output.json'
                with self.assertRaisesRegex(ValueError,message):compare(folder,output)
                self.assertFalse(output.exists(),'invalid provenance cannot produce an audit')


if __name__=='__main__':unittest.main()

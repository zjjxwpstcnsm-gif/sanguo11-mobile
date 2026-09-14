#!/usr/bin/env python3
"""Mutation tests target semantic corruption, never repair map topology to pass."""
import copy, json, unittest
import build_content as b

class ImportTest(unittest.TestCase):
    def setUp(self):
        self.original=b.read
        self.data={p.name:b.read(p.name) for p in b.DATA.glob('*.json')}
        b.read=lambda name:copy.deepcopy(self.data[name])
    def tearDown(self):b.read=self.original
    def rejected(self):
        with self.assertRaises((ValueError,KeyError)):b.build()
    def test_duplicate_fields(self):
        with self.assertRaises(ValueError):json.loads('{"id":1,"id":2}',object_pairs_hook=b.unique)
    def test_duplicate_ids(self):
        self.data['officers-source.json']['rows'][1]['sourceId']=0;self.rejected()
    def test_unknown_aptitude(self):
        self.data['officers-source.json']['rows'][0]['values']['槍兵適性']='Z';self.rejected()
    def test_unknown_field(self):
        self.data['officers-source.json']['rows'][0]['values']['guessed']=0;self.rejected()
    def test_range(self):
        self.data['officers-source.json']['rows'][0]['values']['統率']='101';self.rejected()
    def test_bad_skill_fk(self):
        self.data['officers-source.json']['rows'][0]['values']['特技']='不存在';self.rejected()
    def test_missing_source(self):
        self.data['sites-source.json'][0]['source']='absent';self.rejected()
    def test_unknown_type(self):
        self.data['sites-source.json'][0]['kind']='city-or-port';self.rejected()
    def test_site_overlap(self):
        rows=self.data['sites-source.json'];rows[0]['rawX']=rows[1]['rawX'];rows[0]['rawY']=rows[1]['rawY'];self.rejected()
    def test_coordinate_range(self):
        self.data['sites-source.json'][0]['rawX']=200;self.rejected()
    def test_false_complete(self):
        self.data['scenarios-source.json'][0]['playable']=True;self.rejected()
    def test_count_difference(self):
        self.data['skills-source.json'].pop();self.rejected()
    def test_cross_source_difference(self):
        self.data['cross-checks.json'][0]['stats'][0]-=1;self.rejected()
    def test_order_independent_ids(self):
        original,_=b.build();self.data['officers-source.json']['rows'].reverse();self.data['sites-source.json'].reverse();updated,_=b.build()
        for name in ['officers.tsv','sites.tsv','aliases.tsv','relations.tsv','profiles.tsv']:
            self.assertEqual(original[b.OUT/name],updated[b.OUT/name])
    def test_no_silent_relation_binding(self):
        _,report=b.build();self.assertTrue(report['uncertainRelations']);self.assertTrue(all(x['targetId'] is None for x in report['uncertainRelations']))
        self.assertEqual(report['counts']['resolvedRelationRows'],869)
        self.assertEqual(report['counts']['unresolvedRelationRows'],99)
        self.assertEqual(len(report['swornGroupAnchors']),1)
        self.assertTrue(any(len(x['candidates'])==3 for x in report['uncertainRelations']))
    def test_unknown_personality(self):
        self.data['officers-source.json']['rows'][0]['values']['性格']='unknown';self.rejected()
    def test_invalid_lifespan_flag(self):
        self.data['officers-source.json']['rows'][0]['values']['自然死']='yes';self.rejected()
    def test_no_self_relation(self):
        row=self.data['officers-source.json']['rows'][0];row['values']['父親']=row['name'];self.rejected()
    def test_missing_relation_is_quarantined(self):
        row=self.data['officers-source.json']['rows'][0];row['values']['親近武將']='不存在的人物'
        _,report=b.build();self.assertTrue(any(x['raw']=='不存在的人物' and x['targetId'] is None for x in report['uncertainRelations']))

if __name__=='__main__':unittest.main()

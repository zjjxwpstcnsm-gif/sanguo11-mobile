#!/usr/bin/env python3
"""Check committed geographic evidence against the shipped authority, not the old map."""
import collections
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
REF=ROOT/'data/map/reference-v056'
def run():
    sites=json.loads((REF/'site-review.json').read_text())['sites']
    regions=json.loads((REF/'region-review.json').read_text())
    prop={k:v for line in (ROOT/'core/src/main/resources/maps/national-map-v056.properties').read_text().splitlines() if '=' in line and not line.startswith('#') for k,v in [line.split('=',1)]}
    assert len(sites)==87 and len({s['id'] for s in sites})==87
    assert collections.Counter(s['kind'] for s in sites)=={'CITY':42,'GATE':10,'PORT':35}
    actual={int(k.split('.')[1]):[int(i) for i in v.split(',')] for k,v in prop.items() if k.startswith('site.')}
    assert len(actual)==87
    for s in sites:
        x,y=s['source_coord'];assert 0<=x<200 and 0<=y<200
        assert actual[s['id']]==[x,y]
        assert s['axial_coord']==[y-(x-(x&1))//2+99,x]
        assert s['evidence_level'] in {'CONFIRMED_VISIBLE','MULTI_ANCHOR_CALIBRATED','ESTIMATED','UNREADABLE_REFERENCE'}
        assert len(s['reference_pixel'])==2
    covered=set()
    for region in regions['regions']:
        x0,y0,x1,y1=region['source_bounds'];assert 0<=x0<x1<=200 and 0<=y0<y1<=200
        members=[s for s in sites if x0<=s['source_coord'][0]<x1 and y0<=s['source_coord'][1]<y1]
        assert set(region['sites'])=={s['id'] for s in members},region['region']
        assert region['evidence_counts']==dict(collections.Counter(s['evidence_level'] for s in members))
        covered.update(region['sites'])
    assert covered=={s['id'] for s in sites},'Missing region membership: '+str({s['id'] for s in sites}-covered)
    assert regions['all_87_sites_have_region'] is True
    print('PASS: 87 unique sites, authority coordinates/axial conversion, 12 region bounds and evidence counts. Visual fidelity remains separately reviewed.')
if __name__=='__main__':run()

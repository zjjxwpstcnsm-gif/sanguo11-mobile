#!/usr/bin/env python3
"""Deterministic, read-only checks of map review metadata (not original gameplay verification)."""
from collections import Counter
import hashlib
import json
from pathlib import Path
from map_site_review import ROOT, build_index, data_svg

paths=list((ROOT/'core/src/main/resources/scenarios').glob('*.properties'))
before={p:hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
a,props,terrain=build_index();b,_,_=build_index()
assert a==b
assert Counter(s['kind'] for s in a['sites'])=={'CITY':42,'GATE':10,'PORT':35}
assert len({s['id'] for s in a['sites']})==87
assert len([s for s in a['sites'] if s['center_evidence']=='measured_city_icon_center'])==42
assert all(s['reference_collision_footprint'] is None for s in a['sites'])
assert all(s['center_evidence'].startswith('inferred') for s in a['sites'] if s['kind']!='CITY')
assert a['reference_calibration_with_column_stagger']['rms_pixels']<2.4
assert a['reference_calibration_without_stagger']['rms_pixels']>7.9
chars=set(''.join(''.join(row) for row in terrain))
from map_site_review import PALETTE
assert chars<=set(PALETTE),chars-set(PALETTE)
for s in a['sites']:
 x0,y0,x1,y1=s['crop_bounds'];assert 0<=x0<x1<=7200 and 0<=y0<y1<=6752
 assert '<svg ' in data_svg(s,a,props,terrain)
assert before=={p:hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
print('PASS: all 87 site records; 42 measured/45 inferred crop centers; no invented footprint counts; source snapshots unchanged')

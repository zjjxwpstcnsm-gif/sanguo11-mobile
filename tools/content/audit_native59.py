#!/usr/bin/env python3
"""Explicit v059 reviewed source edits; no image thresholding or topology-based fills.

The complete pre/post hashes, per-cell preimages, immutable sites/plots and both
historical ledgers are checked. --apply is idempotent only for the exact postimage.
Original reference images are inputs for fingerprint verification, never outputs.
"""
from pathlib import Path
from collections import Counter
import argparse, hashlib, json, re
from audit_native57 import properties, neighbors, components
from audit_native58 import transform, summary, footprint
ROOT=Path(__file__).resolve().parents[2]
MAP=ROOT/'core/src/main/resources/maps/national-map-v056.properties'
PLAN=ROOT/'data/map/reference-v059/terrain-corrections.json'
MANIFEST=ROOT/'tools/content/map-release-manifest.json'
def digest(b):return hashlib.sha256(b).hexdigest()
def ledger():return json.loads(PLAN.read_text())
def apply_bytes(raw,plan):
    if digest(raw)==plan['after_sha256']:
        # Idempotency still validates the entire ledger, not merely its post hash.
        assert transform(transform(raw,plan,True),plan)==raw
        return raw
    return transform(raw,plan)
def old58(raw):return transform(raw,ledger(),True)
def old57(raw):
    prior=json.loads((ROOT/'data/map/reference-v058/terrain-corrections.json').read_text())
    return transform(old58(raw),prior,True)
def distance(a,b):
    x,y=a;q,r=b
    ar=y-x//2;br=r-q//2
    return max(abs(x-q),abs(ar-br),abs(x+ar-q-br))
def audit(raw):
    plan=ledger();assert digest(raw)==plan['after_sha256'],'Unexpected map, refusing unreviewed content'
    before=old58(raw);assert apply_bytes(before,plan)==raw and apply_bytes(raw,plan)==raw
    p=properties(raw.decode());bp=properties(before.decode());g=[p[f'terrain.{y}']for y in range(200)]
    codes=dict(re.findall(r"case '([A-Z])' -> World\.Terrain\.([A-Z_]+)",(ROOT/'core/src/main/java/game/sanguo/core/TerrainCode.java').read_text()))
    assert len(codes)==15 and codes['D']=='MOUNTAIN_PATH' and codes['H']=='DAM' and codes['Q']=='NON_NAVIGABLE_WATER'
    assert p['revision']=='59' and p['columns']==p['rows']=='200'
    assert all(len(row)==200 and set(row)<=codes.keys()for row in g)
    immutable=lambda d:{k:v for k,v in d.items()if not k.startswith('terrain.')and k!='revision'}
    assert immutable(p)==immutable(bp),'Moved sites, development or crop data'
    sites,plots=footprint(p);assert len(plots)==591 and sum(k.startswith('site.')for k in p)==87
    calibration=json.loads((PLAN.parent/'calibration-review.json').read_text());regions={r['id']:r for r in calibration['regions']}
    assert calibration['reference']==plan['reference'] and calibration['actual_read']
    assert not plan['reference']['distributed']
    for c in plan['cells']:
        x,y=c['source'];region=regions[c['region']];x0,y0,x1,y1=region['source_bounds']
        assert (x,y)not in sites and(x,y)not in plots
        assert x0<=x<=x1 and y0<=y<=y1 and c['pixel_crop']==region['pixel_crop']
        assert len(region['anchors'])>=3 and c['calibration_id']==region['calibration_id']
        assert c['reference_sha256']==plan['reference']['sha256'] and c['evidence']=='CONFIRMED_VISIBLE'
        assert codes[c['before']]==c['before_terrain'] and codes[c['after']]==c['after_terrain']
        assert c['before']+c['after']in('VQ','RM'),'This review does not authorize other transitions'
        if c['after']=='Q':
            assert c['original_navigation_evidence']=='UNRESOLVED' and c['movement_policy']=='PRESERVE_PREEXISTING_BLOCKAGE'
    m=json.loads(MANIFEST.read_text());entry=next(e for e in m['files']if e['source_path']==str(MAP.relative_to(ROOT)))
    assert m['release']=='0.59.0' and entry['sha256']==digest(raw)
    expected=''.join(f"{c['source'][0]}\t{c['source'][1]}\t{c['before']}\t{c['after']}\n"for c in plan['cells'])
    assert (ROOT/'core/src/test/resources/reference59-corrections.tsv').read_text()==expected
    # Earlier signed ledgers remain exact; do not relabel inherited revisions.
    prior=json.loads((ROOT/'data/map/reference-v058/terrain-corrections.json').read_text())
    assert digest(before)==prior['after_sha256'] and transform(old57(raw),prior)==before
    assert digest(old57(raw))==json.loads((ROOT/'data/map/reference-v057/terrain-corrections.json').read_text())['after_sha256']
    a,b=summary(raw),summary(before)
    assert a['counts']['D']==b['counts']['D']==161 and a['counts'].get('H',0)==b['counts'].get('H',0)==0
    review=json.loads((PLAN.parent/'prior-estimates-review.json').read_text())
    sitePoints={k:tuple(map(int,v.split(',')))for k,v in p.items()if k.startswith('site.')}
    blocks=[]
    for y in range(0,200,25):
        for x in range(0,200,25):
            cells=[(xx,yy)for yy in range(y,y+25)for xx in range(x,x+25)if g[yy][xx]=='V']
            counts=Counter(g[yy][xx]for yy in range(y,y+25)for xx in range(x,x+25))
            near=sum(any(distance(c,s)<=8 for s in sitePoints.values())for c in cells)
            roadadj=sum(any(g[yy][xx]in 'RDB'for xx,yy in neighbors(*c))for c in cells)
            reviewed=sorted({c['region']for c in plan['cells']if x<=c['source'][0]<x+25 and y<=c['source'][1]<y+25})
            # Priority is a review queue, never a decision to change any terrain.
            blocks.append(dict(source_bounds=[x,y,x+24,y+24],remaining_source_void=len(cells),near_site_void=near,route_adjacent_void=roadadj,
                roads=counts['R'],mountain_paths=counts['D'],planks=counts['B'],reviewed_regions=reviewed,
                review_priority=near*4+roadadj*2+len(cells)+(30 if reviewed else 0),status='PARTIAL_REVIEW'if reviewed else'UNRESOLVED'))
    blocks.sort(key=lambda e:(-e['review_priority'],e['source_bounds']))
    for index,comp in enumerate(a['void_regions']):
        comp['review_id']=f'void59-{index+1:03d}';comp['classification']='UNRESOLVED'
        nearest=min(((distance(c,s),k)for c in comp['sample']for k,s in sitePoints.items()))
        comp['nearest_sample_site']=nearest[1];comp['sample_distance']=nearest[0]
    changes=Counter(c['before']+'->'+c['after']for c in plan['cells'])
    return dict(map_revision=59,map_sha256=digest(raw),baseline_revision=58,baseline_sha256=digest(before),
        source_grid=[200,200],source_cells=40000,axial_storage=[299,200],axial_padding=19800,
        unique_modified_national_cells=len(plan['cells']),transitions=dict(changes),
        evidence_counts=dict(Counter(c['evidence']for c in plan['cells'])),
        regions={r:dict(Counter(c['before']+'->'+c['after']for c in plan['cells']if c['region']==r))for r in regions},
        terrain_net_change={codes[k]:a['counts'].get(k,0)-b['counts'].get(k,0)for k in codes if a['counts'].get(k,0)!=b['counts'].get(k,0)},
        before=b,after=a,natural_facility_delta=0,
        prior_estimates_review='data/map/reference-v059/prior-estimates-review.json',
        prior_estimates_total=26,prior_estimates_terrain_edits=0,prior_estimates_promoted_overall=0,
        categories={'A_CONFIRMED_LEGAL_OUTLINE':0,'B_CORRECTED_SOURCE_VOID':changes['V->Q'],'C_AXIAL_PADDING':19800,
          'D_NEW_RENDER_CACHE_HOLE_REPRODUCED':False,'E_NEW_PROJECTION_MISMATCH_REPRODUCED':False,'UNRESOLVED_SOURCE_VOID':a['counts']['V']},
        national_blocks=blocks,reference_crops_distributed=False,
        caveat='2258 remaining source VOID cells are an unresolved review queue, not 2258 confirmed errors. Boundary contact is not legality. Q surface and original navigation evidence are separate. No new cache/minimap fix claimed.')
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--apply',action='store_true');ap.add_argument('--check',action='store_true');ap.add_argument('--reference',type=Path);ap.add_argument('--report',type=Path);ap.add_argument('--baseline58',type=Path);ap.add_argument('--baseline57',type=Path)
    a=ap.parse_args();plan=ledger();raw=MAP.read_bytes()
    if a.reference:assert digest(a.reference.read_bytes())==plan['reference']['sha256'],'Wrong actual reference'
    if a.apply:
        after=apply_bytes(raw,plan);m=json.loads(MANIFEST.read_text());entry=next(e for e in m['files']if e['source_path']==str(MAP.relative_to(ROOT)))
        assert entry['sha256']==digest(raw),'Release manifest baseline drift'
        assert m['release']==('0.58.0'if digest(raw)==plan['before_sha256']else'0.59.0')
        entry['sha256']=digest(after);m['release']='0.59.0'
        MAP.write_bytes(after);MANIFEST.write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n');raw=after
    report=audit(raw)
    for path,data in [(a.baseline58,old58(raw)),(a.baseline57,old57(raw))]:
        if path:path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    if a.report:a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('REFERENCE59 DATA PASS:',report['unique_modified_national_cells'],report['transitions'],'VOID=',report['after']['counts']['V'],'internal=',report['after']['interior_void_cells'],'components=',report['after']['void_components'])
if __name__=='__main__':main()

#!/usr/bin/env python3
"""Reviewed v060 batches. Reuses v058 transform and v059 odd-q audit/index contracts.

No classifier, reference rendering fallback, automatic topology fill, or old-payload replay.
Both preimage and idempotent postimage validate the full regional allowlist before any write.
"""
from pathlib import Path
from collections import Counter
import argparse, json, re
from audit_native58 import transform, summary, footprint, properties, digest
from audit_native59 import distance, neighbors
ROOT=Path(__file__).resolve().parents[2]
MAP=ROOT/'core/src/main/resources/maps/national-map-v056.properties'
DIR=ROOT/'data/map/reference-v060'
PLAN=DIR/'terrain-corrections.json'
MANIFEST=ROOT/'tools/content/map-release-manifest.json'
BASE_SHA='50ea7331d5b0a70cdb469ace3657f15f949ff645ea0e5f1d7255efab9c188cd3'
def ledger():return json.loads(PLAN.read_text())
def validate_plan(before,plan):
    assert plan['source_revision']==59 and plan['target_revision']==60
    assert plan['before_sha256']==BASE_SHA and digest(before)==BASE_SHA,'Unrecognized source map'
    p=properties(before.decode());sites,plots=footprint(p)
    review=json.loads((DIR/'calibration-review.json').read_text())
    assert review['actual_read'] and review['reference']==plan['reference'] and not plan['reference']['distributed']
    assert plan['reference']['sha256']=='a5a4e8c7f9785b6fbadc8487508ff6c937dcef2d782b099f2e876e48d167d4e0'
    assert [plan['reference']['width'],plan['reference']['height']]==[7200,6752]
    regions={r['id']:r for r in review['regions']};assert len(regions)==len(review['regions'])
    allowed={(r['id'],tuple(c))for r in regions.values()for c in r['accepted_source_coordinates']}
    seen=set();actual=set()
    for c in plan['cells']:
        x,y=c['source'];coord=(x,y)
        assert isinstance(x,int) and isinstance(y,int) and 0<=x<200 and 0<=y<200,'Source bounds; not padding'
        assert coord not in seen,'Duplicate source coordinate';seen.add(coord)
        assert coord not in sites and coord not in plots,'Protected site/city footprint/development plot'
        assert c['before']=='V' and c['after']=='Q' and p[f'terrain.{y}'][x]=='V','Only previously blocked VOID authorized'
        assert c['before_terrain']=='VOID' and c['after_terrain']=='NON_NAVIGABLE_WATER'
        r=regions[c['region']];a,b,z,t=r['source_bounds']
        assert a<=x<=z and b<=y<=t and (c['region'],coord)in allowed,'Regional source allowlist'
        assert c['pixel_crop']==r['pixel_crop'] and c['calibration_id']==r['calibration_id'] and len(r['anchors'])>=3
        assert c['reference_sha256']==plan['reference']['sha256'] and c['evidence']=='CONFIRMED_VISIBLE'
        assert c['original_navigation_evidence']=='UNRESOLVED' and c['movement_policy']=='PRESERVE_PREEXISTING_BLOCKAGE'
        actual.add((c['region'],coord))
    assert actual==allowed,'Partial/extra regional ledger: every approved cell must be present exactly once'
def apply_bytes(raw,plan):
    if digest(raw)==plan['after_sha256']:
        before=transform(raw,plan,True);validate_plan(before,plan)
        assert transform(before,plan)==raw
        return raw
    validate_plan(raw,plan)
    return transform(raw,plan)
def old59(raw):return transform(raw,ledger(),True)
def audit(raw, release_manifest=MANIFEST, release_version=ROOT/'version.properties'):
    # Optional *exact* historical release identities for tests on later maps. Defaults remain current production.
    plan=ledger();assert digest(raw)==plan['after_sha256']
    before=old59(raw);assert apply_bytes(before,plan)==raw and apply_bytes(raw,plan)==raw
    p=properties(raw.decode());bp=properties(before.decode())
    immutable=lambda d:{k:v for k,v in d.items()if not k.startswith('terrain.')and k!='revision'}
    assert immutable(p)==immutable(bp) and p['revision']=='60' and p['columns']==p['rows']=='200'
    codes=dict(re.findall(r"case '([A-Z])' -> World\.Terrain\.([A-Z_]+)",(ROOT/'core/src/main/java/game/sanguo/core/TerrainCode.java').read_text()))
    assert len(codes)==15 and codes['Q']=='NON_NAVIGABLE_WATER' and codes['D']=='MOUNTAIN_PATH' and codes['H']=='DAM'
    grid=[p[f'terrain.{y}']for y in range(200)];assert all(len(row)==200 and set(row)<=codes.keys()for row in grid)
    sites,plots=footprint(p);assert len(plots)==591 and sum(k.startswith('site.')for k in p)==87
    m=json.loads(release_manifest.read_text());e=next(e for e in m['files']if e['source_path']==str(MAP.relative_to(ROOT)))
    assert m['release']=='0.60.0' and e['sha256']==digest(raw)
    assert release_version.read_text()=='versionName=0.60.0\nversionCode=60\n'
    expected=''.join(f"{c['source'][0]}\t{c['source'][1]}\tV\tQ\n"for c in plan['cells'])
    assert (ROOT/'core/src/test/resources/reference60-corrections.tsv').read_text()==expected
    # Reverse the historical ledgers on bytes, not fabricated revision-only migrations.
    historical=before
    for rev in (59,58):
        previous=json.loads((ROOT/f'data/map/reference-v0{rev}/terrain-corrections.json').read_text())
        assert digest(historical)==previous['after_sha256']
        prior=transform(historical,previous,True);assert transform(prior,previous)==historical;historical=prior
    assert digest(historical)==json.loads((ROOT/'data/map/reference-v057/terrain-corrections.json').read_text())['after_sha256']
    retained=json.loads((DIR/'remaining-void.json').read_text());remaining={(x,y)for y in range(200)for x in range(200)if grid[y][x]=='V'}
    assert {tuple(c['source'])for c in retained['cells']}==remaining and len(retained['cells'])==len(remaining)
    assert retained['map_sha256']==digest(raw) and retained['confirmed_legal_exterior_cells']==0
    assert all(c['original_outline_legality']=='UNRESOLVED' and c['current']=='V'for c in retained['cells'])
    a,b=summary(raw),summary(before)
    assert all(a['counts'].get(c,0)==b['counts'].get(c,0)for c in codes if c not in 'VQ'),'Existing passable/route/terrain changed'
    # Extend the existing 25x25 review queue; do not establish a second coordinate system.
    oldindex=json.loads((ROOT/'docs/validation/v059/source-grid-audit.json').read_text())
    annotations={tuple(r['source_bounds']):r for r in json.loads((DIR/'calibration-review.json').read_text())['regions']}
    blocks=[]
    for inherited in oldindex['national_blocks']:
        block=dict(inherited);x,y,z,t=block['source_bounds'];counts=Counter(grid[yy][xx]for yy in range(y,t+1)for xx in range(x,z+1))
        region=annotations.get(tuple(block['source_bounds']));block.update(remaining_source_void=counts['V'],roads=counts['R'],mountain_paths=counts['D'],planks=counts['B'],status='ALL_BASELINE_VOID_REVIEWED'if region else'NO_BASELINE_VOID',reviewed_regions=[region['id']]if region else[],v060_modified=len(region['accepted_source_coordinates'])if region else 0)
        block['remaining_categories']=dict(Counter(c['classification']for c in retained['cells']if x<=c['source'][0]<=z and y<=c['source'][1]<=t));blocks.append(block)
    blocks.sort(key=lambda c:(c['source_bounds'][1],c['source_bounds'][0]))
    return {'map_revision':60,'map_sha256':digest(raw),'baseline_revision':59,'baseline_sha256':digest(before),'source_grid':[200,200],'source_cells':40000,'axial_storage':[299,200],'axial_padding':19800,'unique_modified_national_cells':len(plan['cells']),'transitions':{'V->Q':len(plan['cells'])},'existing_Q_W_rule_changes':0,'surface_evidence_counts':dict(Counter(c['evidence']for c in plan['cells'])),'original_navigation_evidence_counts':dict(Counter(c['original_navigation_evidence']for c in plan['cells'])),'areas':dict(Counter(c['area']for c in plan['cells'])),'before':b,'after':a,'remaining_classification_counts':retained['classification_counts'],'confirmed_legal_exterior_cells':0,'natural_facility_delta':0,'prior_estimates_total':26,'prior_estimates_new_geography':0,'prior_estimates_new_evidence_promotions':0,'inherited_local_candidates':{'resolved_surface_only':2,'still_unresolved':5},'national_blocks':blocks,'reference_crops_distributed':False,'new_cache_hole_reproduced':False,'new_projection_mismatch_reproduced':False,'caveat':'All baseline VOID blocks viewed; retained categories are not blanket legality proofs. Q appearance confirmed but navigation unresolved. No false nationwide 100% or VOID-zero claim.'}
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--apply',action='store_true');ap.add_argument('--check',action='store_true');ap.add_argument('--report',type=Path);ap.add_argument('--baseline59',type=Path);ap.add_argument('--reference',type=Path);a=ap.parse_args()
    raw=MAP.read_bytes();plan=ledger()
    if a.reference:assert digest(a.reference.read_bytes())==plan['reference']['sha256'],'Wrong actual reference file'
    if a.apply:
        result=apply_bytes(raw,plan)
        m=json.loads(MANIFEST.read_text());e=next(e for e in m['files']if e['source_path']==str(MAP.relative_to(ROOT)))
        assert e['sha256']==digest(raw) and m['release']==('0.59.0'if digest(raw)==BASE_SHA else'0.60.0'),'Manifest preimage mismatch'
        if result!=raw:
            e['sha256']=digest(result);m['release']='0.60.0';MAP.write_bytes(result);MANIFEST.write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n');raw=result
    report=audit(raw)
    if a.baseline59:a.baseline59.parent.mkdir(parents=True,exist_ok=True);a.baseline59.write_bytes(old59(raw))
    if a.report:a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('REFERENCE60 DATA PASS:',{k:report[k]for k in ('unique_modified_national_cells','transitions','surface_evidence_counts','original_navigation_evidence_counts','axial_padding')},'remaining VOID=',report['after']['counts']['V'])
if __name__=='__main__':main()

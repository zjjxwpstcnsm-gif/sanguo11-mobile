#!/usr/bin/env python3
"""Explicit v061 delta + separately scoped cartographic exterior. No classifier/flood-fill.
Reuses the v058 full-image transformer, v059 odd-q topology and existing 25x25 block index.
All guards run before writes and also on the idempotent postimage. Unknown is never exterior.
"""
from pathlib import Path
from collections import Counter
import argparse, json, re
from audit_native58 import transform, summary, footprint, properties, digest
ROOT=Path(__file__).resolve().parents[2]
MAP=ROOT/'core/src/main/resources/maps/national-map-v056.properties'
DIR=ROOT/'data/map/reference-v061'
PLAN=DIR/'terrain-corrections.json'
EXTERIOR=ROOT/'core/src/main/resources/maps/national-exterior-v061.properties'
BASE_SHA='9b9e8dc7682c787d90717e53a57c9f68471706273c2939fe4f01501a77b8b47d'
AFTER_SHA='d5d5659e40a23619dd8515713d21997715733d20cc937a964662591e22f076c4'
REFERENCE_SHA='a5a4e8c7f9785b6fbadc8487508ff6c937dcef2d782b099f2e876e48d167d4e0'
def ledger():return json.loads(PLAN.read_text())
def old60(raw):return transform(raw,ledger(),True)
def coordinate(cell):
    c=cell['source'];assert len(c)==2 and all(type(x)is int and 0<=x<200 for x in c),'Unique integer national source coordinates only'
    return tuple(c)
def validate_plan(before,plan):
    assert plan['source_revision']==60 and plan['target_revision']==61
    assert plan['before_sha256']==BASE_SHA==digest(before) and plan['after_sha256']==AFTER_SHA,'Entire fixed map pre/postimage required'
    p=properties(before.decode());sites,plots=footprint(p)
    review=json.loads((DIR/'calibration-review.json').read_text())
    assert review['actual_read'] and review['reference']==plan['reference'] and not plan['reference']['distributed']
    assert plan['reference']['sha256']==REFERENCE_SHA and [plan['reference']['width'],plan['reference']['height']]==[7200,6752]
    assert plan['reference']['archive_sha256']=='3eaee31c6b361e0310138ecdcb7e5e8b04f52dd2e55279d537ea46d89f4611f5'
    regions={r['id']:r for r in review['regions']};assert len(regions)==len(review['regions'])
    allowed={(r['id'],tuple(c))for r in regions.values()for c in r['accepted_source_coordinates']}
    seen=set();actual=set()
    for c in plan['cells']:
        x,y=coord=coordinate(c);assert coord not in seen,'Duplicate source coordinate';seen.add(coord)
        assert coord not in sites and coord not in plots,'Protected city seven-cell footprint/site/development plot'
        assert c['before']=='V' and c['after']=='Q' and p[f'terrain.{y}'][x]=='V','Only exact blocked-to-blocked water delta'
        assert c['before_terrain']=='VOID' and c['after_terrain']=='NON_NAVIGABLE_WATER'
        r=regions[c['region']];a,b,z,t=r['source_bounds']
        assert a<=x<=z and b<=y<=t and(c['region'],coord)in allowed,'Regional exact allowlist; not bounds alone'
        assert c['pixel_crop']==r['pixel_crop'] and c['calibration_id']==r['calibration_id'] and len(r['anchors'])>=3
        assert c['reference_sha256']==REFERENCE_SHA and c['evidence']=='CONFIRMED_VISIBLE' and c['validity_evidence']=='CONFIRMED_VISIBLE'
        assert c['original_navigation_evidence']=='UNRESOLVED' and c['movement_policy']=='PRESERVE_PREEXISTING_BLOCKAGE'
        actual.add((c['region'],coord))
    assert actual==allowed and len(seen)==206,'All reviewed cells, exactly once; no incomplete batches'
def apply_bytes(raw,plan):
    if digest(raw)==plan['after_sha256']:
        before=transform(raw,plan,True);validate_plan(before,plan);assert transform(before,plan)==raw;return raw
    validate_plan(raw,plan);return transform(raw,plan)
def validate_exterior(raw,scenery=None,resource=None,remaining=None):
    assert digest(raw)==AFTER_SHA,'Scenery only bound to exact new authoritative map'
    scenery=scenery if scenery is not None else json.loads((DIR/'scenery-cells.json').read_text())
    remaining=remaining if remaining is not None else json.loads((DIR/'remaining-void.json').read_text())
    resource=resource if resource is not None else EXTERIOR.read_bytes()
    review=json.loads((DIR/'calibration-review.json').read_text());regions={r['id']:r for r in review['regions']}
    assert scenery['reference']==review['reference'] and scenery['map_sha256']==AFTER_SHA and scenery['map_revision']==61 and scenery['before_map_sha256']==BASE_SHA
    assert scenery['boundary_basis']==review['boundary_basis'],'A category name is not boundary evidence'
    p=properties(raw.decode());sites,plots=footprint(p);seen=set();styles={}
    allowed={(r['id'],tuple(c))for r in regions.values()for c in r['exterior_source_coordinates']}
    for c in scenery['cells']:
        x,y=coord=coordinate(c);assert coord not in seen,'Duplicate exterior';seen.add(coord)
        assert p[f'terrain.{y}'][x]=='V' and c['before']==c['current']=='V','Cannot cover valid terrain'
        assert coord not in sites and coord not in plots and c['no_valid_tile_overlay'],'No protected objects'
        assert c['kind']in('SEA','ARID','ROCK') and c['boundary_basis']in review['boundary_basis']
        assert c['boundary_evidence']=='CONFIRMED_VISIBLE' and c['evidence']in('CONFIRMED_VISIBLE','ESTIMATED')
        assert c['validity']=='REVIEWED_EXTERIOR_PRESENTATION_ONLY' and c['original_navigation_evidence']=='UNRESOLVED'
        r=regions[c['region']];assert(c['region'],coord)in allowed and c['calibration_id']==r['calibration_id'] and c['pixel_crop']==r['pixel_crop'] and c['reference_sha256']==REFERENCE_SHA
        styles[coord]=c['kind']
    assert len(seen)==1051 and {(c['region'],coordinate(c))for c in scenery['cells']}==allowed
    unknown={coordinate(c)for c in remaining['unresolved_cells']}
    unknown_allowed={tuple(c)for r in regions.values()for c in r['unresolved_source_coordinates']}
    assert len(remaining['unresolved_cells'])==len(unknown)==14 and unknown==unknown_allowed and not(seen&unknown)
    assert not remaining['original_enabled_hex_table_obtained'],'Do not fabricate exact original engine boundary data'
    assert all(c['evidence']=='UNRESOLVED'for c in remaining['unresolved_cells'])
    assert seen|unknown=={(x,y)for y in range(200)for x in range(200)if p[f'terrain.{y}'][x]=='V'}
    assert remaining['map_sha256']==AFTER_SHA and remaining['raw_source_void']==1065 and remaining['scoped_reviewed_exterior']==1051
    rp={}
    for line in resource.decode().splitlines():
        if not line or line.startswith('#'):continue
        key,value=line.split('=',1);assert key not in rp,'Duplicate exterior key';rp[key]=value
    assert rp.pop('mapId')=='san11-national' and rp.pop('layout')=='native-200' and rp.pop('revision')=='61' and rp.pop('mapSha256')==AFTER_SHA and rp.pop('count')=='1051'
    assert rp=={f'outside.{x}.{y}':style for(x,y),style in styles.items()},'Exact runtime mask == reviewed cells (not every VOID)'
    assert Counter(styles.values())==Counter(SEA=869,ARID=126,ROCK=56)
    return styles,unknown

def audit(raw):
    plan=ledger();before=old60(raw);assert apply_bytes(before,plan)==raw and apply_bytes(raw,plan)==raw
    styles,unknown=validate_exterior(raw)
    p=properties(raw.decode());bp=properties(before.decode());immutable=lambda d:{k:v for k,v in d.items()if not k.startswith('terrain.')and k!='revision'}
    assert immutable(p)==immutable(bp) and p['revision']=='61' and p['columns']==p['rows']=='200'
    codes=dict(re.findall(r"case '([A-Z])' -> World\.Terrain\.([A-Z_]+)",(ROOT/'core/src/main/java/game/sanguo/core/TerrainCode.java').read_text()))
    assert len(codes)==15 and codes['Q']=='NON_NAVIGABLE_WATER' and codes['R']=='ROAD' and codes['D']=='MOUNTAIN_PATH' and codes['H']=='DAM'
    grid=[p[f'terrain.{y}']for y in range(200)];assert all(len(r)==200 and set(r)<=codes.keys()for r in grid)
    sites,plots=footprint(p);assert len(plots)==591 and sum(k.startswith('site.')for k in p)==87
    manifest=json.loads((ROOT/'tools/content/map-release-manifest.json').read_text());assert manifest['release']=='0.61.0'
    for path in(MAP,EXTERIOR):assert next(e for e in manifest['files']if e['source_path']==str(path.relative_to(ROOT)))['sha256']==digest(path.read_bytes())
    assert (ROOT/'version.properties').read_text()=='versionName=0.61.0\nversionCode=61\n'
    assert (ROOT/'core/src/test/resources/reference61-corrections.tsv').read_text()==''.join(f"{c['source'][0]}\t{c['source'][1]}\tV\tQ\n"for c in plan['cells'])
    historical=before
    for rev in(60,59,58):
        previous=json.loads((ROOT/f'data/map/reference-v0{rev}/terrain-corrections.json').read_text());assert digest(historical)==previous['after_sha256']
        prior=transform(historical,previous,True);assert transform(prior,previous)==historical;historical=prior
    a,b=summary(raw),summary(before);assert all(a['counts'].get(c,0)==b['counts'].get(c,0)for c in codes if c not in 'VQ')
    blocks=[]
    for inherited in json.loads((ROOT/'docs/validation/v060/source-grid-audit.json').read_text())['national_blocks']:
        block=dict(inherited);x,y,z,t=block['source_bounds'];inside=lambda c:x<=c[0]<=z and y<=c[1]<=t
        block.update(v061_modified=sum(inside(c['source'])for c in plan['cells']),v061_exterior=sum(inside(c)for c in styles),v061_unresolved=sum(inside(c)for c in unknown));blocks.append(block)
    return dict(map_revision=61,map_sha256=digest(raw),baseline_sha256=BASE_SHA,source_cells=40000,axial_padding=19800,
      unique_modified_national_cells=206,transitions={'V->Q':206},areas=dict(Counter(c['area']for c in plan['cells'])),existing_Q_W_rule_changes=0,natural_facility_delta=0,
      exterior_presentation_cells=1051,exterior_styles=dict(Counter(styles.values())),unresolved_coordinates=sorted(unknown),unresolved_cells=14,
      appearance_evidence=dict(Counter(c['evidence']for c in plan['cells'])),navigation_evidence={'UNRESOLVED':206},
      exterior_surface_evidence=dict(Counter(c['evidence']for c in json.loads((DIR/'scenery-cells.json').read_text())['cells'])),
      boundary_basis='Cartographic multi-cue review only, not extracted original engine enabled-hex flags',original_enabled_hex_table_obtained=False,
      before=b,after=a,national_blocks=blocks,reference_crops_distributed=False,cache_or_LOD_hole_reproduced=False,
      caveat='Not VOID=0 or 100% original boundaries/navigation. Fourteen mixed bank/wall cells remain unresolved and unpainted; old saves do not acquire v061 scenery.')
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--check',action='store_true');ap.add_argument('--apply',action='store_true');ap.add_argument('--report',type=Path);ap.add_argument('--baseline60',type=Path);ap.add_argument('--reference',type=Path);a=ap.parse_args();raw=MAP.read_bytes()
    if a.reference:assert digest(a.reference.read_bytes())==REFERENCE_SHA,'Wrong actual reference file'
    if a.apply:
        result=apply_bytes(raw,ledger());validate_exterior(result)
        if result!=raw:MAP.write_bytes(result)
        raw=result
    report=audit(raw)
    if a.report:a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    if a.baseline60:a.baseline60.parent.mkdir(parents=True,exist_ok=True);a.baseline60.write_bytes(old60(raw))
    print('REFERENCE61 DATA PASS:',{k:report[k]for k in('unique_modified_national_cells','transitions','exterior_styles','unresolved_cells','axial_padding')})
if __name__=='__main__':main()

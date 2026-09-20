#!/usr/bin/env python3
"""Apply/check ONLY the signed-by-digest explicit regional v058 correction ledger.

No reference image is redistributed or used as an automatic color classifier.
The six-neighbor source audit is separate from axial padding and visual validity.
"""
from pathlib import Path
from collections import Counter
import argparse, hashlib, json, re
from audit_native57 import properties, neighbors, components
ROOT=Path(__file__).resolve().parents[2]
MAP=ROOT/'core/src/main/resources/maps/national-map-v056.properties'
PLAN=ROOT/'data/map/reference-v058/terrain-corrections.json'
MANIFEST=ROOT/'tools/content/map-release-manifest.json'
def digest(data):return hashlib.sha256(data).hexdigest()
def ledger():return json.loads(PLAN.read_text())
def transform(raw,plan,reverse=False):
    source,target=('after','before')if reverse else('before','after')
    assert digest(raw)==plan[source+'_sha256'],'Baseline digest mismatch: refusing unreviewed overwrite'
    text=raw.decode();p=properties(text);grid=[p[f'terrain.{y}']for y in range(200)]
    assert p['revision']==str(plan['target_revision' if reverse else 'source_revision'])
    seen=set();changes={}
    for c in plan['cells']:
        x,y=c['source'];assert 0<=x<200 and 0<=y<200 and(x,y)not in seen;seen.add((x,y))
        assert grid[y][x]==c[source],f'Before mismatch at {x},{y}'
        row=changes.setdefault(y,list(grid[y]));row[x]=c[target]
    for y,row in changes.items():text=text.replace(f'terrain.{y}='+grid[y],f'terrain.{y}='+''.join(row))
    text=text.replace('revision='+p['revision']+'\n','revision='+str(plan['source_revision' if reverse else 'target_revision'])+'\n')
    result=text.encode();assert digest(result)==plan[target+'_sha256'],'Post-image does not match reviewed ledger'
    return result

def baseline57_bytes(raw):
    plan=ledger()
    if digest(raw)==plan['before_sha256']:return raw
    return transform(raw,plan,True)

def footprint(p):
    sites=set();plots=set()
    for key,v in p.items():
        if key.startswith('site.'):
            point=tuple(map(int,v.split(',')));sites.add(point)
            if int(key.split('.')[1])<20042:sites.update(neighbors(*point))
        elif key.startswith('plots.'):
            for v in v.split(';'):
                point=tuple(map(int,v.split(',')));assert point not in plots;plots.add(point)
    return sites,plots

def summary(raw):
    p=properties(raw.decode());g=[p[f'terrain.{y}']for y in range(200)];voids=components(g,'V')
    return dict(counts=dict(sorted(Counter(''.join(g)).items())),void_components=len(voids),
        interior_void_components=sum(not v['touches_source_boundary']for v in voids),
        interior_void_cells=sum(v['size']for v in voids if not v['touches_source_boundary']),
        boundary_connected_void_cells=sum(v['size']for v in voids if v['touches_source_boundary']),void_regions=voids)

def check(raw,plan):
    assert digest(raw)==plan['after_sha256'],'Unexpected authoritative map digest'
    old=transform(raw,plan,True);assert transform(old,plan)==raw
    p=properties(raw.decode());oldp=properties(old.decode());codes=dict(re.findall(r"case '([A-Z])' -> World\.Terrain\.([A-Z_]+)",(ROOT/'core/src/main/java/game/sanguo/core/TerrainCode.java').read_text()))
    assert len(codes)==15 and codes['Q']=='NON_NAVIGABLE_WATER' and codes['D']=='MOUNTAIN_PATH' and codes['H']=='DAM' and codes['R']=='ROAD'
    assert p['columns']==p['rows']=='200' and p['revision']=='58'
    assert all(len(p[f'terrain.{y}'])==200 and set(p[f'terrain.{y}'])<=codes.keys()for y in range(200))
    assert {k:v for k,v in p.items()if not k.startswith('terrain.')and k!='revision'}=={k:v for k,v in oldp.items()if not k.startswith('terrain.')and k!='revision'},'No moving sites, crop or development plots'
    sites,plots=footprint(p);assert len(plots)==591
    regions={r['id']:r for r in json.loads((PLAN.parent/'calibration-review.json').read_text())['regions']}
    for c in plan['cells']:
        x,y=c['source'];assert (x,y)not in sites and(x,y)not in plots,'Touched occupied footprint or development plot'
        assert c['evidence']in('CONFIRMED_VISIBLE','ESTIMATED')
        region=regions[c['region']];x0,y0,x1,y1=region['source_bounds'];assert x0<=x<=x1 and y0<=y<=y1
        assert c['pixel_crop']==region['pixel_crop']and len(region['anchors'])>=3
        assert codes[c['after']]==c['after_terrain']and codes[c['before']]==c['before_terrain']
        if c['after']=='Q':assert c['before']in('V','M'),'Never block an existing navigable cell under this ledger'
    count=summary(raw);before=summary(old)
    assert count['counts'].get('H',0)==before['counts'].get('H',0)==0
    assert count['counts']['D']==before['counts']['D']==161
    manifest=json.loads(MANIFEST.read_text());entry=next(e for e in manifest['files']if e['source_path']==str(MAP.relative_to(ROOT)))
    assert entry['sha256']==plan['after_sha256']and manifest['release']=='0.58.0'
    # Exact test sampling, generated from this ledger, is checked instead of drifting by hand.
    tsv='\n'.join(f"{c['source'][0]}\t{c['source'][1]}\t{c['before']}\t{c['after']}"for c in plan['cells'])+'\n'
    assert (ROOT/'core/src/test/resources/reference58-corrections.tsv').read_text()==tsv
    net={codes[k]:count['counts'].get(k,0)-before['counts'].get(k,0)for k in codes if count['counts'].get(k,0)!=before['counts'].get(k,0)}
    blocks=[]
    for y in range(0,200,25):
        for x in range(0,200,25):
            counts=Counter(p[f'terrain.{yy}'][xx]for yy in range(y,y+25)for xx in range(x,x+25))
            blocks.append({'source_bounds':[x,y,x+24,y+24],'remaining_source_void':counts['V'],'road':counts['R'],'plank':counts['B'],'mountain_path':counts['D'],'status':'PARTIAL_REGIONAL_REVIEW'if any(x<=c['source'][0]<x+25 and y<=c['source'][1]<y+25 for c in plan['cells'])else'UNRESOLVED'})
    return dict(map_revision=58,map_sha256=digest(raw),source_grid=[200,200],source_cells=40000,axial_storage=[299,200],axial_padding=19800,
        unique_modified_national_cells=len(plan['cells']),evidence_counts=dict(Counter(c['evidence']for c in plan['cells'])),terrain_net_change=net,
        before=before,after=count,natural_facility_delta={'DAM':0,'note':'No H changed; real instantiated counts separately verified by Java across scenarios'},
        categories={'A_CONFIRMED_LEGAL_OUTLINE':{'cells':0,'note':'Boundary contact alone does not confirm legality; no wholesale outline edits this round.'},
          'B_CORRECTED_SOURCE_VOID':sum(c['before']=='V'for c in plan['cells']),
          'C_AXIAL_PADDING':19800,'D_RENDER_CACHE_HOLES':{'reproduced':False,'note':'No new main-map cache hole demonstrated in sampled baseline; no fabricated cache fix.'},
          'E_PROJECTION_HIT':{'fixes':['minimap odd-q half-cell projection shared by terrain/city/unit/viewport/tap','main-map city label cannot select VOID/padding']},
          'UNRESOLVED_SOURCE_VOID':count['counts']['V']},
        national_blocks=blocks,reference_crops_distributed=False,
        caveat='Interior/boundary-connected counts are topology only, not confirmed error/outline counts. Q preserves original movement prohibition. Nine NW v057 sand estimates retained without confirmation. This is regional progress, not nationwide fidelity certification.')

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--apply',action='store_true');ap.add_argument('--check',action='store_true');ap.add_argument('--report',type=Path);ap.add_argument('--baseline57',type=Path);ap.add_argument('--reference',type=Path)
    a=ap.parse_args();plan=ledger();raw=MAP.read_bytes()
    if a.reference:
        assert digest(a.reference.read_bytes())==plan['reference']['sha256'],'Reference fingerprint mismatch'
    if a.apply:
        if digest(raw)==plan['before_sha256']:
            after=transform(raw,plan);manifest=json.loads(MANIFEST.read_text());entry=next(e for e in manifest['files']if e['source_path']==str(MAP.relative_to(ROOT)))
            assert entry['sha256']==plan['before_sha256'],'Manifest baseline mismatch'
            entry['sha256']=plan['after_sha256'];manifest['release']='0.58.0'
            MAP.write_bytes(after);MANIFEST.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n');raw=after
        else:assert digest(raw)==plan['after_sha256'],'Neither known baseline nor reviewed post-image; no write'
    report=check(raw,plan)
    if a.baseline57:a.baseline57.parent.mkdir(parents=True,exist_ok=True);a.baseline57.write_bytes(baseline57_bytes(raw))
    if a.report:a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('REFERENCE58 DATA PASS:',{k:report[k]for k in('unique_modified_national_cells','evidence_counts','terrain_net_change','axial_padding')},'remaining interior VOID=',report['after']['interior_void_cells'])
if __name__=='__main__':main()

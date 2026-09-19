#!/usr/bin/env python3
"""Compare v51 shipped data to v50, including deterministic replay and publication boundaries."""
from __future__ import annotations
import argparse
from collections import Counter
import hashlib,json,subprocess,tempfile,shutil
from pathlib import Path
from refine_geography_v044 import read,neighbors,distance
from map_reference_atlas import index
ROOT=Path(__file__).resolve().parents[2]
checks=0

def check(ok,why):
    global checks
    checks+=1
    if not ok:raise AssertionError(why)

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--base',default='7a85a47cce2545a70b86d3c7e3b723bd8f3af0fc');ap.add_argument('--skip-replay',action='store_true');a=ap.parse_args()
    ref=json.loads((ROOT/'data/map/reference-v051.json').read_text());report=json.loads((ROOT/'docs/validation/v051/geography.json').read_text())
    def before(path):return subprocess.check_output(['git','show',a.base+':'+str(path)],cwd=ROOT)
    directory=ROOT/'core/src/main/resources/scenarios'
    np,nt,nc=read(directory/'heroes-mobile-sandbox.properties')
    actual_national=[]
    for path in sorted(directory.glob('*.properties')):
        p,t,c=read(path);old=dict(l.split('=',1) for l in before(path.relative_to(ROOT)).decode().splitlines() if '=' in l and not l.startswith('#'))
        check(set(p)==set(old),(path.name,'keys'))
        delta=Counter((nc[k][0]-v[0],nc[k][1]-v[1]) for k,v in c.items());check(len(delta)==1,(path.name,'coordinate frame'))
        dx,dy=next(iter(delta));check(dy%2==0,(path.name,'odd-r parity'))
        changed=0
        for key,value in p.items():
            if key.startswith('terrain.'):continue
            check(int(value)==int(old[key])+1 if key=='revision' else value==old[key],(path.name,key,'non-geographic data'))
        for y,row in enumerate(t):
            for x,ch in enumerate(row):
                check(ch==nt[y+dy][x+dx],(path.name,x,y,'national/regional seam'))
                oldch=old['terrain.'+str(y)][x]
                check((ch in 'WOV')==(oldch in 'WOV'),(path.name,x,y,'coastline and existing water topology'))
                if ch!=oldch:
                    changed+=1
                    if path.name=='heroes-mobile-sandbox.properties':actual_national.append({'cell':[x,y],'before':oldch,'after':ch})
        check(changed==report['maps'][path.name]['changed_cells'],(path.name,'documented difference count'))
        print('PASS v51 snapshot',path.name,changed,'changed terrain cells; sites/plots/gameplay unchanged')
    check(actual_national==report['changes'],'exact audited cell changes')
    check(len(actual_national)==1047,'reviewed national footprint')
    check(sum(k.startswith('development-plot.') for k in np)==591,'591 unchanged domestic slots')
    check(len(nc)==87,'all site identities retained')
    slots={tuple(map(int,v.split('|')[1:3])) for k,v in np.items() if k.startswith('development-plot.')}
    for route in report['road_corridors']:
        path=list(map(tuple,route['cells']))
        for x,y in path[1:-1]:check((x,y) not in slots|set(nc.values()) and nt[y][x] in 'PFAZBX',(route['name'],'dry unobstructed corridor'))
        for u,v in zip(path,path[1:]):check(distance(u,v)==1,(route['name'],'hex adjacency'))
    for x,y in ref['difficult_march_cells']:check(nt[y][x]=='D',('source red pass',x,y))
    stored=json.loads((ROOT/'data/map/city-review-v051.json').read_text())
    check(stored==index(),'reproducible city review index')
    check(len(stored['cities'])==42 and stored['reviewed_city_count']==13,'42 indexed; 13 region-reviewed, not all 42 newly completed')
    check(stored['reference_sha256']==ref['reference_sha256'],'same reference image fingerprint')
    baseline=json.loads((ROOT/'tools/content/map-v050-manifest.json').read_text())
    current=json.loads((ROOT/'tools/content/map-release-manifest.json').read_text())
    for entry in current['files']:
        check(digest(ROOT/entry['source_path'])==entry['sha256'],entry['source_path']+' packaged integrity')
    for entry in baseline['files']:
        path=entry['source_path']
        if path.startswith('app/src/main/assets/'):
            check(digest(ROOT/path)==entry['sha256'],path+' exact v50 art')
    check(not any(ROOT.glob('app/src/main/assets/**/MAP_SAN11*')),'source JPEG is not a runtime asset')
    # Re-application must fail before writes; an already-corrected map must not drift.
    start={e['source_path']:digest(ROOT/e['source_path']) for e in current['files']}
    attempted=subprocess.run(['python3','tools/content/refine_geography_v051.py','--apply'],cwd=ROOT,capture_output=True,text=True)
    check(attempted.returncode!=0 and 'Only v0.50' in attempted.stderr,'repeated migration rejected')
    check(start=={k:digest(ROOT/k) for k in start},'rejected migration wrote no assets')
    if not a.skip_replay:
        with tempfile.TemporaryDirectory(prefix='sg11-map51-') as temp:
            clone=Path(temp)/'repo'
            shutil.copytree(ROOT,clone,ignore=shutil.ignore_patterns('.git','build','.gradle','__pycache__'))
            for entry in baseline['files']:
                path=entry['source_path'];(clone/path).write_bytes(before(path))
            (clone/'tools/content/map-release-manifest.json').write_text(json.dumps(baseline,ensure_ascii=False,indent=2)+'\n')
            subprocess.run(['python3','tools/content/refine_geography_v051.py','--apply'],cwd=clone,check=True,capture_output=True,text=True)
            for entry in current['files']:check(digest(clone/entry['source_path'])==entry['sha256'],entry['source_path']+' deterministic rebuild')
            check((clone/'docs/validation/v051/geography.json').read_bytes()==(ROOT/'docs/validation/v051/geography.json').read_bytes(),'deterministic audit report')
    print('PASS v51 content:',checks,'checks, deterministic replay and publication boundaries')

if __name__=='__main__':main()

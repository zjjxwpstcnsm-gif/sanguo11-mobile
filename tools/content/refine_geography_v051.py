#!/usr/bin/env python3
"""Guarded v50 -> v51 regional geography migration. No runtime overlay or artwork changes."""
from __future__ import annotations
import argparse
from collections import Counter
import hashlib
import heapq
import json
from pathlib import Path
from refine_geography_v044 import read, neighbors, distance, line_distance
from refine_geography_v050 import inside_polygon

ROOT = Path(__file__).resolve().parents[2]
DIR = ROOT / 'core/src/main/resources/scenarios'
REFERENCE = ROOT / 'data/map/reference-v051.json'
BASE = ROOT / 'tools/content/map-v050-manifest.json'
MANIFEST = ROOT / 'tools/content/map-release-manifest.json'
NATIONAL = 'heroes-mobile-sandbox.properties'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def refine():
    ref = json.loads(REFERENCE.read_text())
    data = {p.name: read(p) for p in sorted(DIR.glob('*.properties'))}
    np, nt, sites = data[NATIONAL]
    original = [row[:] for row in nt]
    terrain = [row[:] for row in nt]
    h, w = len(nt), len(nt[0])
    assert (w, h) == (100, 100)
    valid = lambda p: 0 <= p[0] < w and 0 <= p[1] < h
    offsets, parcels, openings = {}, set(), set()
    for name, (p, t, c) in data.items():
        frames = Counter((sites[k][0]-v[0], sites[k][1]-v[1]) for k,v in c.items())
        assert len(frames) == 1, (name, 'inconsistent coordinates')
        dx,dy = next(iter(frames)); assert dy % 2 == 0
        offsets[name] = (dx,dy)
        for key,value in p.items():
            f=value.split('|')
            if key.startswith('development-plot.'):
                parcels.add((int(f[1])+dx,int(f[2])+dy))
            elif key.startswith('initial-unit.'):
                openings.add((int(f[6])+dx,int(f[7])+dy))
            elif key.startswith('initial-camp.'):
                openings.add((int(f[2])+dx,int(f[3])+dy))
    # Pin all prior dock hinterland routes and gate retaining walls; filling farms
    # later must not seal a port, and a regional repaint must not reopen a gate.
    v50=json.loads((ROOT/'docs/validation/v050/geography.json').read_text())
    fixed=set(sites.values())|parcels|openings
    for cells in v50['dock_lanes'].values(): fixed.update(map(tuple,cells))
    for gate in v50['gate_corridors']:
        fixed.update(map(tuple,gate['wall']));fixed.update(map(tuple,gate['outer_ridge']));fixed.update(map(tuple,gate['approaches']))
        fixed.update(neighbors(sites[gate['id']]))
    # Preserve source-backed v50 paths, all coastal topology, and uncommon terrains.
    v50ref=json.loads((ROOT/'data/map/reference-v050.json').read_text())
    for y,row in enumerate(original):
        for x,t in enumerate(row):
            if t in 'WOVDBHSZ': fixed.add((x,y))
            if t in 'PFA' and any(line_distance((x,y),r['points'])<=.9 for r in v50ref['roads']): fixed.add((x,y))
    touched=set()
    def paint(p,t):
        if valid(p) and p not in fixed:
            terrain[p[1]][p[0]]=t;touched.add(p)
    for region in ref['regions']:
        x0,y0,x1,y1=region['bounds']
        for y in range(y0,y1+1):
            for x in range(x0,x1+1):paint((x,y),region['base'])
    bounds=lambda p:any(a<=p[0]<=c and b<=p[1]<=d for a,b,c,d in [r['bounds'] for r in ref['regions']])
    for zone in ref['zones']:
        pts=zone['points']
        for y in range(max(0,int(min(p[1] for p in pts))),min(h,int(max(p[1] for p in pts))+1)):
            for x in range(max(0,int(min(p[0] for p in pts))),min(w,int(max(p[0] for p in pts))+1)):
                if bounds((x,y)) and inside_polygon((x+.25,y+.25),pts):paint((x,y),zone['terrain'])
    restricted=set(map(tuple,ref['difficult_march_cells']))
    for p in restricted:
        assert p not in fixed, ('source difficult pass collides with protected cell',p)
        paint(p,'D')
    paths=[]
    for road in ref['roads']:
        points=road['points'];start=tuple(points[0]);end=tuple(points[-1]);width=road['width']
        # The centreline is reviewed; a constrained hex search avoids visual-only
        # diagonals and existing parcels. City approaches may use existing dry
        # basins within seven cells, but new mountain gaps stay in the corridor.
        blocked=(set(sites.values())-{start,end})|parcels|openings|restricted
        dist={start:0.};previous={start:None};queue=[(0.,start)]
        while queue:
            cost,u=heapq.heappop(queue)
            if cost != dist[u]:continue
            if u == end:break
            for v in neighbors(u):
                if not valid(v) or v in blocked:continue
                c=terrain[v[1]][v[0]];off=line_distance(v,points)
                if c in 'WOVDHS':continue
                if off>width and not (min(distance(v,start),distance(v,end))<=7 and c!='M'):continue
                if c=='M' and (v in fixed or not bounds(v)):continue
                # Prefer source corridor and existing walkable cells. Forests,
                # poison and plank-road mechanics are retained, not paved over.
                extra=1+off*3+(4 if c=='M' else 0)
                nc=cost+extra
                if nc<dist.get(v,float('inf')):
                    dist[v]=nc;previous[v]=u;heapq.heappush(queue,(nc,v))
        assert end in previous, ('reviewed corridor disconnected; expand annotation, not runtime rules',road['name'], 'reachable',len(previous),'nearest', sorted(previous,key=lambda q:distance(q,end))[:8])
        path=[];u=end
        while u is not None:path.append(u);u=previous[u]
        path.reverse()
        assert all(distance(a,b)==1 for a,b in zip(path,path[1:]))
        cleared=[]
        for x,y in path:
            if terrain[y][x]=='M':
                assert (x,y) not in fixed and bounds((x,y))
                terrain[y][x]='P';cleared.append((x,y));touched.add((x,y))
        paths.append({'name':road['name'],'region':road['region'],'cells':path,'mountain_gaps_opened':cleared})
    # Source data only changes terrain. Every site, parcel, officer, resource,
    # era owner and unit start remains byte-for-byte identical to v50.
    for p in parcels|set(sites.values()):assert terrain[p[1]][p[0]]=='P'
    for p in fixed:assert terrain[p[1]][p[0]]==original[p[1]][p[0]], ('protected cell changed',p)
    changes={(x,y):c for y,row in enumerate(terrain) for x,c in enumerate(row) if c!=original[y][x]}
    regional=[]
    for region in ref['regions']:
        x0,y0,x1,y1=region['bounds']
        cells=[(x,y) for y in range(y0,y1+1) for x in range(x0,x1+1)]
        regional.append({**region,'changed_cells':sum(p in changes for p in cells),'terrain':dict(Counter(terrain[y][x] for x,y in cells))})
    maps={}
    for name,(p,t,c) in data.items():
        dx,dy=offsets[name];n=0
        for (x,y),v in changes.items():
            xx,yy=x-dx,y-dy
            if 0<=yy<len(t) and 0<=xx<len(t[0]):
                assert t[yy][xx]==original[y][x], (name,'baseline seam',x,y)
                t[yy][xx]=v;n+=1
        maps[name]={'offset':[dx,dy],'changed_cells':n}
    report={'release':'0.51.0','baseline_commit':ref['baseline_commit'],'source_sha256':ref['reference_sha256'],'national_changed_cells':len(changes),'unchanged_site_count':len(sites),'unchanged_domestic_slots':len(parcels),'regions':regional,'difficult_march_cells':sorted(restricted),'road_corridors':paths,'maps':maps,'changes':[{'cell':[x,y],'before':original[y][x],'after':c} for (x,y),c in sorted(changes.items(),key=lambda item:(item[0][1],item[0][0]))]}
    return data,report


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--apply',action='store_true');args=p.parse_args()
    if json.loads(MANIFEST.read_text())['release']!='0.50.0':raise SystemExit('Only v0.50 can be migrated; already-applied or unrelated baseline refused')
    baseline=json.loads(BASE.read_text())
    for entry in baseline['files']:
        assert sha(ROOT/entry['source_path'])==entry['sha256'], ('v50 input drift',entry['source_path'])
    data,report=refine()
    print(json.dumps({k:v for k,v in report.items() if k not in ('changes','road_corridors')},ensure_ascii=False,indent=2))
    if not args.apply:return
    for name,(p,t,c) in data.items():
        file=DIR/name;out=[]
        for line in file.read_text().splitlines():
            if line.startswith('terrain.'):
                key=line.split('=',1)[0];line=key+'='+''.join(t[int(key.split('.')[1])])
            elif line.startswith('revision='):line='revision='+str(int(p['revision'])+1)
            out.append(line)
        file.write_text('\n'.join(out)+'\n')
    index=DIR/'index.txt';out=[]
    for line in index.read_text().splitlines():
        if line and not line.startswith('#'):
            name=line.split()[0];line=name+' '+sha(DIR/(name+'.properties'))
        out.append(line)
    index.write_text('\n'.join(out)+'\n')
    manifest=json.loads(BASE.read_text());manifest['release']='0.51.0';manifest['baseline_manifest']='tools/content/map-v050-manifest.json'
    for entry in manifest['files']:entry['sha256']=sha(ROOT/entry['source_path'])
    MANIFEST.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    out=ROOT/'docs/validation/v051';out.mkdir(parents=True,exist_ok=True)
    (out/'geography.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')

if __name__=='__main__':main()

#!/usr/bin/env python3
"""Reviewed incremental v44 edits to the v40/v43 curated odd-r snapshots.

Never call the retired national generator. Preserve all sites, development
parcels, special terrain and sprite resources. --apply accepts only the pinned
baseline, so rerunning cannot repeatedly expand the river.
"""
import argparse
from collections import Counter
import hashlib
from heapq import heappop, heappush
import json
from math import hypot
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
DIR=ROOT/'core/src/main/resources/scenarios'
BASE=ROOT/'tools/content/map-v040-manifest.json'
CURRENT=ROOT/'tools/content/map-release-manifest.json'

def neighbors(p):
    x,y=p
    return [(x-1,y),(x+1,y),(x-(1-y%2),y-1),(x+y%2,y-1),(x-(1-y%2),y+1),(x+y%2,y+1)]

def distance(a,b):
    ax,ay=a;bx,by=b;aq=ax-(ay-(ay&1))//2;bq=bx-(by-(by&1))//2
    return max(abs(aq-bq),abs(ay-by),abs(aq+ay-bq-by))

def line_distance(p,line):
    x,y=p;best=1e9
    for (ax,ay),(bx,by) in zip(line,line[1:]):
        dx,dy=bx-ax,by-ay;t=max(0,min(1,((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy)))
        best=min(best,hypot(x-ax-dx*t,y-ay-dy*t))
    return best

def read(path):
    p=dict(l.split('=',1) for l in path.read_text().splitlines() if '=' in l and not l.startswith('#'))
    assert p['coordinates']=='odd-r',path
    terrain=[list(p[f'terrain.{y}']) for y in range(int(p['height']))]
    cities={int(f[0]):(int(f[2]),int(f[3])) for k,v in p.items() if k.startswith('city.') for f in [v.split('|')]}
    return p,terrain,cities

def refine():
    data={path.name:read(path) for path in sorted(DIR.glob('*.properties'))}
    _,source,positions=data['heroes-mobile-sandbox.properties'];original=[r[:] for r in source];terrain=[r[:] for r in original]
    width,height=len(terrain[0]),len(terrain)
    offsets={};protected=set();bank_protected=set();gate_protected=set()
    for name,(p,t,cities) in data.items():
        candidates=Counter((positions[k][0]-v[0],positions[k][1]-v[1]) for k,v in cities.items() if k in positions)
        offset=candidates.most_common(1)[0][0];assert len(candidates)==1,(name,candidates);offsets[name]=offset
        dx,dy=offset
        for k,v in cities.items():
            h=(v[0]+dx,v[1]+dy);protected.add(h);bank_protected.add(h);bank_protected.update(neighbors(h))
        for key,value in p.items():
            if key.startswith('development-plot.'):
                f=value.split('|');protected.add((int(f[1])+dx,int(f[2])+dy))
            elif key.startswith('initial-unit.'):
                f=value.split('|');protected.add((int(f[6])+dx,int(f[7])+dy))
            elif key.startswith('site-kind.') and '|GATE|' in value:
                h=cities[int(value.split('|')[0])];h=(h[0]+dx,h[1]+dy)
                gate_protected.update((x,y) for y in range(h[1]-2,h[1]+3) for x in range(h[0]-2,h[0]+3) if distance(h,(x,y))<=2)
    bank_protected.update(protected);bank_protected.update(gate_protected)
    valid=lambda p:0<=p[0]<width and 0<=p[1]<height
    # Follow the actual curated river, not the obsolete 200x200 generator vectors.
    river=[(24,7),(28,10),(33,14),(33,16),(30,19),(30,30),(30,33),(40,33),(49,34),(57,32),(64,31),(70,27),(72,23),(74,18)]
    water={(x,y) for y,row in enumerate(original) for x,t in enumerate(row) if t in 'WS' and line_distance((x,y),river)<=2.4}
    expansion={p for h in water for p in neighbors(h) if valid(p) and p not in bank_protected and original[p[1]][p[0]] in 'PFMZ'}
    for x,y in expansion:terrain[y][x]='W'
    # Open compact plains and explicit overland links, keeping Luoyang's gate walls.
    opened=set()
    def open_land(h):
        if valid(h) and h not in gate_protected and h not in protected and terrain[h[1]][h[0]]=='M':
            terrain[h[1]][h[0]]='P';opened.add(h)
    for key in [20013,20028]:
        center=positions[key]
        for y in range(center[1]-3,center[1]+4):
            for x in range(center[0]-3,center[0]+3+1):
                if distance(center,(x,y))<=3:open_land((x,y))
    links=[(20013,20028),(20013,20016),(20013,20012),(20013,20014),(20028,20016),(20028,20014)]
    routes=[]
    for a,b in links:
        start,end=positions[a],positions[b];queue=[(0,start)];cost={start:0};prev={}
        lo=(min(start[0],end[0])-3,min(start[1],end[1])-3);hi=(max(start[0],end[0])+3,max(start[1],end[1])+3)
        while queue:
            n,h=heappop(queue)
            if n!=cost[h]:continue
            if h==end:break
            for v in neighbors(h):
                if not valid(v) or not(lo[0]<=v[0]<=hi[0] and lo[1]<=v[1]<=hi[1]):continue
                t=terrain[v[1]][v[0]]
                if t not in 'PFMA' or v in gate_protected or v in protected and v not in (start,end):continue
                # Prefer compact direct openings to long detours around a mountain wall.
                nc=n+(4 if t=='M' else 2 if t=='F' else 1)
                if nc<cost.get(v,1e9):cost[v]=nc;prev[v]=h;heappush(queue,(nc,v))
        assert end in cost,('no overland corridor',a,b)
        h=end;route=[h]
        while h!=start:
            open_land(h)
            for v in neighbors(h):open_land(v)
            h=prev[h];route.append(h)
        routes.append({'from':a,'to':b,'steps':len(route)-1,'cells':route})
    # Sand replaces only ordinary land: not mountain passes, water, ports or parcels.
    northwest=set();ye=set();lujiang=set()
    for y in range(height):
        for x in range(width):
            h=(x,y)
            if h in protected or h in bank_protected or terrain[y][x] not in 'PF':continue
            n=min(((x-cx)/rx)**2+((y-cy)/ry)**2 for cx,cy,rx,ry in [(5,20,14,9),(14,27,10,7),(22,15,9,7)])
            if n<.86 or n<1 and (x*7+y*13)%5!=0:
                terrain[y][x]='A';northwest.add(h)
            elif 46<=x<=63 and 27<=y<=32 and any(distance(h,w)<=2 for w in water):
                terrain[y][x]='A';ye.add(h)
    # A small connected riverbank patch northeast of Lujiang, not a desert belt.
    candidates={(x,y) for y in range(55,63) for x in range(69,79) if (x,y) not in bank_protected and terrain[y][x] in 'PF' and 2<=distance((x,y),positions[20025])<=6}
    if candidates:
        start=min(candidates,key=lambda p:(distance(p,(76,57)),p));queue=[start]
        while queue and len(lujiang)<10:
            h=queue.pop(0)
            if h in lujiang or h not in candidates:continue
            lujiang.add(h);terrain[h[1]][h[0]]='A';queue.extend(sorted(neighbors(h),key=lambda p:(distance(p,start),p)))
    assert len(expansion)>80 and len(northwest)>100 and len(ye)>=8 and 4<=len(lujiang)<=10
    changes={(x,y):t for y,row in enumerate(terrain) for x,t in enumerate(row) if t!=original[y][x]}
    summaries={}
    for name,(p,t,cities) in data.items():
        dx,dy=offsets[name];before=[r[:] for r in t];changed=0
        for (x,y),value in changes.items():
            xx,yy=x-dx,y-dy
            if 0<=yy<len(t) and 0<=xx<len(t[0]):
                assert before[yy][xx]==original[y][x],('snapshot disagreement',name,x,y)
                t[yy][xx]=value;changed+=1
        for h in protected:
            x,y=h[0]-dx,h[1]-dy
            if 0<=y<len(t) and 0<=x<len(t[0]):assert t[y][x]==before[y][x],('protected cell changed',name,h)
        summaries[name]={'offset':offsets[name],'changed_cells':changed,'sand_cells':sum(r.count('A') for r in t)}
    return data,{'release':'0.44.0','source_commit':'7f135f334eb185819b6d2115fa1b97eaf759286b','yellow_river_added_water':len(expansion),'opened_mountain_cells':len(opened),'northwest_sand':len(northwest),'ye_sand':len(ye),'lujiang_sand':len(lujiang),'maps':summaries,'overland_routes':routes}

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    baseline=json.loads(BASE.read_text())
    for entry in baseline['files']:
        path=ROOT/entry['source_path']
        assert hashlib.sha256(path.read_bytes()).hexdigest()==entry['sha256'],f'Not the pinned pre-v44 baseline: {path}; refusing repeated expansion'
    data,report=refine()
    print(json.dumps({k:v for k,v in report.items() if k!='overland_routes'},ensure_ascii=False,indent=2))
    if not args.apply:return
    for name,(p,t,c) in data.items():
        path=DIR/name;lines=path.read_text().splitlines();out=[]
        for line in lines:
            if line.startswith('terrain.'):
                key=line.split('=',1)[0];line=key+'='+''.join(t[int(key.split('.')[1])])
            elif line.startswith('revision='):line='revision='+str(int(line.split('=')[1])+1)
            out.append(line)
        path.write_text('\n'.join(out)+'\n')
    index=DIR/'index.txt';lines=[]
    for line in index.read_text().splitlines():
        if line and not line.startswith('#'):
            name=line.split()[0];line=name+' '+hashlib.sha256((DIR/(name+'.properties')).read_bytes()).hexdigest()
        lines.append(line)
    index.write_text('\n'.join(lines)+'\n')
    manifest={'release':'0.44.0','baseline_manifest':'tools/content/map-v040-manifest.json','files':baseline['files']}
    for entry in manifest['files']:entry['sha256']=hashlib.sha256((ROOT/entry['source_path']).read_bytes()).hexdigest()
    CURRENT.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    path=ROOT/'docs/validation/v044';path.mkdir(parents=True,exist_ok=True)
    (path/'geography.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')

if __name__=='__main__':main()

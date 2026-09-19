#!/usr/bin/env python3
"""Apply the reviewed, image-registered v50 geography to the existing snapshots.

This is a guarded, offline, deterministic data migration, not a runtime overlay.
The retired national generator is never called. All non-geographic scenario
records, IDs, era ownership, artwork and map dimensions are preserved.
"""
from __future__ import annotations
import argparse
from collections import Counter, deque
import hashlib
import heapq
import json
from math import hypot
from pathlib import Path
from refine_geography_v044 import read, neighbors, distance, line_distance

ROOT = Path(__file__).resolve().parents[2]
DIR = ROOT / 'core/src/main/resources/scenarios'
MANIFEST = ROOT / 'tools/content/map-release-manifest.json'
REFERENCE = ROOT / 'data/map/reference-v050.json'
NATIONAL = 'heroes-mobile-sandbox.properties'

def inside_polygon(p, points):
    x,y=p;odd=False
    for a,b in zip(points,points[1:]+points[:1]):
        ax,ay=a;bx,by=b
        if (ay>y)!=(by>y) and x<(bx-ax)*(y-ay)/(by-ay)+ax:odd=not odd
    return odd

def component(start, allowed):
    seen={start};queue=deque([start])
    while queue:
        for v in neighbors(queue.popleft()):
            if v in allowed and v not in seen:seen.add(v);queue.append(v)
    return seen

def refine():
    reference=json.loads(REFERENCE.read_text())
    data={p.name:read(p) for p in sorted(DIR.glob('*.properties'))}
    national,original,oldpos=data[NATIONAL]
    oldpos=dict(oldpos)
    original=[r[:] for r in original]
    terrain=[r[:] for r in original];height=len(terrain);width=len(terrain[0])
    assert (width,height)==(100,100),'Only the reviewed 100×100 map is supported'
    offsets={};parcels={};units=set();camps=set()
    for name,(p,t,pos) in data.items():
        delta=Counter((oldpos[k][0]-h[0],oldpos[k][1]-h[1]) for k,h in pos.items())
        assert len(delta)==1,(name,'inconsistent coordinate frame',delta)
        dx,dy=next(iter(delta));assert dy%2==0,(name,'odd-r parity');offsets[name]=(dx,dy)
        for key,value in p.items():
            f=value.split('|')
            if key.startswith('development-plot.'):
                h=(int(f[1])+dx,int(f[2])+dy);owner=int(f[0]);assert h not in parcels or parcels[h]==owner
                parcels[h]=owner
            elif key.startswith('initial-unit.'):units.add((int(f[6])+dx,int(f[7])+dy))
            elif key.startswith('initial-camp.'):camps.add((int(f[2])+dx,int(f[3])+dy))
    pos=dict(oldpos)
    for key,entry in reference['sites'].items():
        cid=int(key);assert tuple(entry['baseline'])==oldpos[cid],('source-site baseline changed',cid)
        pos[cid]=tuple(entry['target'])
    assert len(set(pos.values()))==len(pos),'Site collision'
    valid=lambda h:0<=h[0]<width and 0<=h[1]<height
    protected=set(parcels)|units|camps|set(pos.values())
    water={(x,y) for y,row in enumerate(reference['water']) for x,c in enumerate(row) if c=='W' and original[y][x]!='V'}
    original_special={(x,y):c for y,row in enumerate(original) for x,c in enumerate(row) if c in 'DBXH'}
    def paint(h,t,force=False):
        x,y=h
        if valid(h) and original[y][x]!='V' and (force or h not in protected) and (force or h not in original_special):terrain[y][x]=t
    # Remove obsolete water in authored snapshots, including offshore holes at old ports.
    for y,row in enumerate(original):
        for x,c in enumerate(row):
            if c in 'WOS' and (x,y) not in water:paint((x,y),'P')
    for region in reference['regions']:
        x0,y0,x1,y1=region['bounds']
        for y in range(y0,y1+1):
            for x in range(x0,x1+1):paint((x,y),region['base'])
    for zone in reference['zones']:
        points=zone['points']
        for y in range(max(0,int(min(p[1] for p in points))),min(height,int(max(p[1] for p in points))+1)):
            for x in range(max(0,int(min(p[0] for p in points))),min(width,int(max(p[0] for p in points))+1)):
                if inside_polygon((x+.5,y+.5),points):paint((x,y),zone['terrain'])
    # Reference water is authoritative over land paint; no straight-line road bridges.
    for x,y in water:paint((x,y),'O' if x>=83 and y<58 or x>=95 else 'W')
    roads=set();road_cells={}
    for road in reference['roads']:
        cells=[];points=road['points']
        for y in range(max(0,int(min(p[1] for p in points))-1),min(height,int(max(p[1] for p in points))+2)):
            for x in range(max(0,int(min(p[0] for p in points))-1),min(width,int(max(p[0] for p in points))+2)):
                h=(x,y)
                if h not in water and line_distance(h,points)<=road['width']:
                    paint(h,road['terrain']);roads.add(h);cells.append(h)
        road_cells[road['name']]=cells
    # At two-to-one resolution a rasterized polyline can have diagonal gaps on odd-r rows.
    # Fill only hex-adjacent gaps inside its reviewed narrow land corridor, never water.
    for road in reference['roads']:
        points=road['points'];cells=set(road_cells[road['name']])
        for h in list(cells):
            for v in neighbors(h):
                if valid(v) and v not in water and line_distance(v,points)<=max(road['width'],.85):
                    paint(v,road['terrain']);roads.add(v)
    gate_walls=set();gate_rims=set();gate_details=[]
    for spec in reference['gate_walls']:
        cid=spec['site'];gx,gy=pos[cid];axis=spec['axis'];n=spec['extent'];wall=[]
        for k in range(-n,n+1):
            h=(gx,gy+k) if axis=='NS' else (gx+k,gy)
            if valid(h) and distance(h,(gx,gy))>1:
                paint(h,'M');gate_walls.add(h);wall.append(h)
        # A gate occupies its tile. Friendly troops walk around its six-cell rim;
        # World.gateBlocks disallows those same approach edges for hostile troops.
        rim=set(neighbors((gx,gy)))|{(gx,gy)}
        for h in rim:paint(h,'P',True);gate_rims.add(h)
        if axis=='NS':approaches=[(gx-2,gy),(gx+2,gy)]
        else:approaches=[(gx,gy-2),(gx,gy+2)]
        for h in approaches:paint(h,'P',True);roads.add(h)
        gate_details.append({'id':cid,'axis':axis,'wall':wall,'approaches':approaches})
    # A one-cell raster gap can be entered and exited through non-adjacent
    # outside corners, bypassing the runtime gate's controlled ring edge.
    # Keep a two-cell throat: mountains on the outer ring, two axial entries.
    gate_entries={tuple(h) for d in gate_details for h in d['approaches']}
    harbor_rims={v for cid,h in pos.items() if cid>=20052 for v in neighbors(h)}
    for detail in gate_details:
        center=pos[detail['id']];collar=[]
        for y in range(center[1]-3,center[1]+4):
            for x in range(center[0]-3,center[0]+4):
                h=(x,y)
                if valid(h) and distance(h,center)==2 and h not in gate_rims|gate_entries|set(pos.values())|harbor_rims:
                    gate_walls.add(h);paint(h,'M');collar.append(h)
        detail['outer_ridge']=collar
    gate_walls-=gate_rims|gate_entries
    for cid,h in pos.items():paint(h,'P',True)
    # Preserve every domestic slot, moving only slots that actually collide with
    # a newly corrected site/wall or documented water. No lost city capacity.
    # Keep one dry approach free even when all neighboring city slots are built.
    dock_approaches=set();dock_routes={}
    for cid,h in pos.items():
        if cid<20052:continue
        candidates=[v for v in neighbors(h) if valid(v) and v not in water|set(pos.values())|units|camps|gate_walls and terrain[v[1]][v[0]] not in 'VOH' and any(n in water for n in set(neighbors(v)) & set(neighbors(h)))]
        assert candidates,('no reference-bank approach',cid,h)
        # Reserve a real hinterland lane, not an empty square encircled by
        # future farms. Weighted search only follows existing reviewed land.
        queue=[];best={};prev={}
        for v in candidates:
            if terrain[v[1]][v[0]] not in 'PFAZB':continue
            cost=8 if v in parcels else 1;best[v]=cost;prev[v]=None;heapq.heappush(queue,(cost,v))
        goal=None
        while queue:
            cost,v=heapq.heappop(queue)
            if cost!=best[v]:continue
            if distance(v,h)>=5 and v not in parcels:goal=v;break
            for n in neighbors(v):
                if not valid(n) or n in water|set(pos.values())|units|camps|gate_walls or terrain[n[1]][n[0]] not in 'PFAZB' or distance(n,h)>8:continue
                extra=8 if n in parcels else 1
                if cost+extra<best.get(n,10**9):best[n]=cost+extra;prev[n]=v;heapq.heappush(queue,(cost+extra,n))
        assert goal is not None,('reference dock has no dry hinterland lane',cid,h)
        route=[]
        while goal is not None:route.append(goal);goal=prev[goal]
        route.reverse();dock_routes[str(cid)]=route
        for v in route:dock_approaches.add(v);paint(v,'P',True)
    occupied=set(pos.values())|units|camps|roads|gate_rims|gate_walls|dock_approaches
    parcel_moves={};newparcels={}
    for h,owner in sorted(parcels.items(),key=lambda e:(e[1],e[0])):
        if h not in occupied and h not in water:
            newparcels[h]=owner;paint(h,'P',True);continue
        candidates=[]
        for y in range(max(0,pos[owner][1]-7),min(height,pos[owner][1]+8)):
            for x in range(max(0,pos[owner][0]-7),min(width,pos[owner][0]+8)):
                v=(x,y)
                if v in occupied or v in water or v in parcels or v in newparcels or terrain[y][x] not in 'PFAZ':continue
                if distance(v,pos[owner])>max(7,distance(h,pos[owner])):continue
                candidates.append((distance(h,v),distance(pos[owner],v),y,x))
        assert candidates,('no nearby replacement parcel',owner,h)
        _,_,y,x=min(candidates);v=(x,y);parcel_moves[h]=v;newparcels[v]=owner;paint(v,'P',True)
    assert Counter(newparcels.values())==Counter(parcels.values())
    # Complete traced water at relocated domestic slots (previously protected).
    for h in water:
        if h not in set(newparcels)|set(pos.values())|units|camps:
            x,y=h;paint(h,'O' if x>=83 and y<58 or x>=95 else 'W',True)
    # Vacated slots must not leave a passable hole through a gate's retaining ridge.
    for h in gate_walls:
        assert h not in set(newparcels)|set(pos.values())|units|camps
        paint(h,'M',True)
    # A harbor must have a water front and land access in the SAME cell model used
    # by Army.canTraverse. Candidate fronts are chosen only from traced water.
    port_details=[]
    for cid,h in pos.items():
        if cid<20052:continue
        fronts=[v for v in neighbors(h) if valid(v) and terrain[v[1]][v[0]] in 'WO']
        assert fronts,('reference port missing water frontage',cid,h)
        approaches=[v for v in neighbors(h) if valid(v) and v not in set(pos.values())|set(newparcels) and terrain[v[1]][v[0]] in 'PFAZB']
        if not approaches:
            candidates=[v for v in neighbors(h) if valid(v) and v not in water|set(pos.values())|set(newparcels)|gate_walls and terrain[v[1]][v[0]] not in 'VHX']
            assert candidates,('reference port land approach unavailable',cid,h)
            v=min(candidates,key=lambda v:(distance(v,oldpos[cid]),v));paint(v,'P',True);approaches=[v]
        port_details.append({'id':cid,'position':h,'water_fronts':fronts,'land_approaches':approaches})
    # The screenshot does not unambiguously distinguish every lowland gameplay
    # subtype. Keep established swamp/shallows on surviving dry banks rather than
    # silently erase their rules; never turn traced open water into a free ford.
    for y,row in enumerate(original):
        for x,c in enumerate(row):
            h=(x,y)
            if c in 'ZS' and terrain[y][x]=='P' and h not in water|occupied|set(newparcels):
                terrain[y][x]=c
    for h in units|camps:assert terrain[h[1]][h[0]]==original[h[1]][h[0]],('opening entity terrain changed',h)
    for h in newparcels:assert terrain[h[1]][h[0]]=='P'
    changes={(x,y):c for y,row in enumerate(terrain) for x,c in enumerate(row) if c!=original[y][x]}
    summaries={}
    for name,(p,t,cities) in data.items():
        dx,dy=offsets[name];before=[r[:] for r in t];changed=0
        for (x,y),c in changes.items():
            xx,yy=x-dx,y-dy
            if 0<=yy<len(t) and 0<=xx<len(t[0]):
                assert t[yy][xx]==original[y][x],('baseline regional mismatch',name,x,y)
                t[yy][xx]=c;changed+=1
        for cid in cities:
            h=pos[cid];local=(h[0]-dx,h[1]-dy)
            assert 0<=local[0]<len(t[0]) and 0<=local[1]<len(t),('site outside regional map',name,cid,local)
            cities[cid]=local
        summaries[name]={'offset':[dx,dy],'changed_cells':changed,'dimensions':[len(t[0]),len(t)],'terrain':dict(Counter(''.join(map(''.join,t))))}
    return data,parcel_moves,offsets,{'release':reference['release'],'baseline_commit':reference['baseline_commit'],'reference_sha256':reference['reference']['sha256'],'reference_regions':reference['regions'],'moved_sites':{str(k):{'name':reference['sites'][str(k)]['name'],'before':oldpos[k],'after':v} for k,v in pos.items() if oldpos[k]!=v},'national_changed_cells':len(changes),'preserved_city_ids':42,'preserved_gates':10,'preserved_ports':35,'preserved_domestic_slots':len(parcels),'moved_domestic_slots':[{'owner':parcels[h],'before':h,'after':v} for h,v in sorted(parcel_moves.items())],'dock_lanes':dock_routes,'port_fronts':port_details,'gate_corridors':gate_details,'maps':summaries}

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    manifest=json.loads(MANIFEST.read_text())
    if manifest['release']=='0.50.0':raise SystemExit('v50 geography is already applied; refusing a repeated migration')
    assert manifest['release']=='0.45.0','Expected the v45 geography preserved by v49'
    for entry in manifest['files']:
        assert hashlib.sha256((ROOT/entry['source_path']).read_bytes()).hexdigest()==entry['sha256'],('baseline checksum',entry['source_path'])
    data,parcel_moves,offsets,report=refine()
    print(json.dumps({k:v for k,v in report.items() if k not in ('port_fronts','gate_corridors','moved_domestic_slots')},ensure_ascii=False,indent=2))
    if not args.apply:return
    for name,(p,t,cities) in data.items():
        file=DIR/name;out=[];dx,dy=offsets[name]
        for line in file.read_text().splitlines():
            if line.startswith('terrain.'):
                key=line.split('=',1)[0];line=key+'='+''.join(t[int(key.split('.')[1])])
            elif line.startswith('city.'):
                key,value=line.split('=',1);f=value.split('|');f[2],f[3]=map(str,cities[int(f[0])]);line=key+'='+'|'.join(f)
            elif line.startswith('development-plot.'):
                key,value=line.split('=',1);f=value.split('|');h=(int(f[1])+dx,int(f[2])+dy)
                if h in parcel_moves:
                    v=parcel_moves[h];f[1],f[2]=str(v[0]-dx),str(v[1]-dy)
                    assert 0<=int(f[1])<len(t[0]) and 0<=int(f[2])<len(t)
                    line=key+'='+'|'.join(f)
            elif line.startswith('revision='):line='revision='+str(int(line.split('=')[1])+1)
            out.append(line)
        file.write_text('\n'.join(out)+'\n')
    index=DIR/'index.txt';lines=[]
    for line in index.read_text().splitlines():
        if line and not line.startswith('#'):
            name=line.split()[0];line=name+' '+hashlib.sha256((DIR/(name+'.properties')).read_bytes()).hexdigest()
        lines.append(line)
    index.write_text('\n'.join(lines)+'\n')
    manifest['release']='0.50.0';manifest['baseline_commit']=report['baseline_commit'];manifest['reference_data']='data/map/reference-v050.json'
    for entry in manifest['files']:entry['sha256']=hashlib.sha256((ROOT/entry['source_path']).read_bytes()).hexdigest()
    MANIFEST.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    dest=ROOT/'docs/validation/v050';dest.mkdir(parents=True,exist_ok=True)
    (dest/'geography.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')

if __name__=='__main__':main()

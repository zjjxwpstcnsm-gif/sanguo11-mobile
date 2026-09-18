#!/usr/bin/env python3
"""One-time v44 -> v45 curated snapshots: oases/roads, forest belt and real chokepoint.
No regeneration of prior terrain, cities, officers, domestic plots or sprite assets.
"""
import argparse, hashlib, json
from collections import Counter, deque
from heapq import heappop, heappush
from pathlib import Path
from refine_geography_v044 import read, neighbors, distance, line_distance
ROOT=Path(__file__).resolve().parents[2]
DIR=ROOT/'core/src/main/resources/scenarios'
CURRENT=ROOT/'tools/content/map-release-manifest.json'
NATIONAL='heroes-mobile-sandbox.properties'
MOVES={20042:(49,23),20070:(88,55)}

def refine():
    data={p.name:read(p) for p in sorted(DIR.glob('*.properties'))}
    p,source,positions=data[NATIONAL];old=[r[:] for r in source];pos=dict(positions);terrain=[r[:] for r in old];height=len(terrain);width=len(terrain[0])
    parcels=set();initial=set();offsets={};city_tiles=set(pos.values())
    for name,(p,t,c) in data.items():
        diff=Counter((pos[k][0]-h[0],pos[k][1]-h[1]) for k,h in c.items());assert len(diff)==1,(name,diff)
        dx,dy=next(iter(diff));offsets[name]=(dx,dy)
        for k,v in p.items():
            f=v.split('|')
            if k.startswith('development-plot.'):parcels.add((int(f[1])+dx,int(f[2])+dy))
            elif k.startswith('initial-unit.'):initial.add((int(f[6])+dx,int(f[7])+dy))
    protected=parcels|initial|city_tiles
    inside=lambda h:0<=h[0]<width and 0<=h[1]<height
    def setland(h,value,override=False):
        if inside(h) and (override or h not in protected) and terrain[h[1]][h[0]] in 'PFMA':terrain[h[1]][h[0]]=value
    routes=[]
    def path(a,b,pad=5,plain=True,allow_mountain=True):
        lo=(min(a[0],b[0])-pad,min(a[1],b[1])-pad);hi=(max(a[0],b[0])+pad,max(a[1],b[1])+pad)
        heap=[(0,a)];cost={a:0};prev={}
        while heap:
            n,h=heappop(heap)
            if n!=cost[h]:continue
            if h==b:break
            for v in neighbors(h):
                if not inside(v) or not(lo[0]<=v[0]<=hi[0] and lo[1]<=v[1]<=hi[1]) or v in protected and v not in (a,b):continue
                t=terrain[v[1]][v[0]]
                if t not in ('PFAM' if allow_mountain else 'PFA'):continue
                nc=n+{'P':10,'A':12,'F':15,'M':65}[t]
                if nc<cost.get(v,10**9):cost[v]=nc;prev[v]=h;heappush(heap,(nc,v))
        assert b in cost,('unreachable route',a,b)
        out=[b];h=b
        while h!=a:h=prev[h];out.append(h)
        out.reverse()
        if plain:
            for h in out:setland(h,'P')
        routes.append({'from':a,'to':b,'steps':len(out)-1,'cells':out})
        return set(out)
    # Preserve every development parcel, with green oases only around existing towns.
    northwest={(x,y) for y in range(8,36) for x in range(0,32) if old[y][x]=='A'}
    roads=set()
    for a,b in [(20021,20019),(20019,20020),(20021,20020),(20019,20017),(20020,20017)]:roads|=path(pos[a],pos[b])
    for city in [20021,20019,20020]:
        h=pos[city]
        for y in range(h[1]-2,h[1]+3):
            for x in range(h[0]-2,h[0]+3):
                if distance((x,y),h)<=2:setland((x,y),'P')
    # Small coherent groves, never across the plain road or a development parcel.
    for center,radius in [((9,23),2),((13,19),1),((22,17),2),((10,30),1),((3,28),1)]:
        for y in range(center[1]-radius,center[1]+radius+1):
            for x in range(center[0]-radius,center[0]+radius+1):
                h=(x,y)
                if h in northwest and h not in roads and distance(h,center)<=radius:setland(h,'F')
    # Forest-dominant Xinye-Xuchang belt, but a narrow plain road fits siege engines too.
    start,end=pos[20028],pos[20013];belt=set()
    for y in range(43,58):
        for x in range(38,55):
            h=(x,y)
            if line_distance(h,[start,end])<=3.2 and min(distance(h,start),distance(h,end))>2 and h not in protected and terrain[y][x] in 'PFA':
                terrain[y][x]='F';belt.add(h)
    forestroad=path(start,end,pad=3)
    # Move sites, preserve their identity/ownership and data in every era.
    assert all(h not in protected for h in MOVES.values())
    oldgate=pos[20042];oldport=pos[20070]
    terrain[oldgate[1]][oldgate[0]]='P'
    terrain[oldport[1]][oldport[0]]='O' # obsolete offshore harbor becomes sea again
    gate=MOVES[20042];port=MOVES[20070]
    # Mountain spine separates east/west locally; the only opening is the six-cell gate rim.
    for y in range(14,33):setland((49,y),'M')
    terrain[gate[1]][gate[0]]='P'
    for h in neighbors(gate):
        assert h not in parcels|initial|city_tiles,('gate rim collides with protected cell',h)
        terrain[h[1]][h[0]]='P'
    # Routes terminate on either side, never carve through the mountain spine elsewhere.
    protected.update((49,y) for y in range(14,33) if abs(y-23)>1)
    path(pos[20005],(48,23),pad=3)
    path((50,23),pos[20006],pad=3)
    terrain[port[1]][port[0]]='P'
    # Wu's harbor has a genuine water frontage and a clear land approach.
    for h in [(89,55),(90,55),(90,54)]:setland(h,'P')
    assert any(terrain[h[1]][h[0]] in 'WO' for h in neighbors(port))
    path(port,pos[20023],pad=2)
    protected-=city_tiles
    for x,y in parcels:assert terrain[y][x]=='P',('domestic plot lost',x,y)
    sand=sum(terrain[y][x]=='A' for x,y in northwest)
    assert sand>len(northwest)*.6,(sand,len(northwest))
    forest=sum(terrain[y][x]=='F' for x,y in belt)
    assert forest>len(belt)*.65,(forest,len(belt))
    # Gate-controlled walkability: no bypass in the local pass corridor.
    def crossing(friendly):
        a,b=pos[20005],pos[20006];queue=deque([a]);seen={a};blocked=parcels|city_tiles|{gate};blocked.discard(a);blocked.discard(b)
        while queue:
            h=queue.popleft()
            for v in neighbors(h):
                if v in seen or v in blocked or not(40<=v[0]<=58 and 15<=v[1]<=31) or terrain[v[1]][v[0]] not in 'PFA':continue
                if not friendly and distance(h,gate)==1 and distance(v,gate)==1:continue
                if v==b:return True
                seen.add(v);queue.append(v)
        return False
    assert crossing(True),'friendly Huguan route disconnected'
    assert not crossing(False),'hostile Huguan still bypassable in pass corridor'
    changes={(x,y):t for y,row in enumerate(terrain) for x,t in enumerate(row) if old[y][x]!=t}
    summaries={}
    for name,(p,t,cities) in data.items():
        dx,dy=offsets[name];changed=0
        for (x,y),value in changes.items():
            xx,yy=x-dx,y-dy
            if 0<=yy<len(t) and 0<=xx<len(t[0]):
                assert t[yy][xx]==old[y][x],('snapshot mismatch',name,x,y)
                t[yy][xx]=value;changed+=1
        for cid,h in MOVES.items():
            if cid in cities:
                dest=(h[0]-dx,h[1]-dy);assert 0<=dest[0]<len(t[0]) and 0<=dest[1]<len(t),(name,cid,dest);cities[cid]=dest
        summaries[name]={'offset':(dx,dy),'changed_cells':changed,'sand':sum(row.count('A') for row in t)}
    return data,{'release':'0.45.0','baseline':'c4bc410223365cd083d6d6196b0303255b740f1c','northwest_preexisting_sand':len(northwest),'northwest_sand_retained':sand,'northwest_new_forest':sum(terrain[y][x]=='F' for x,y in northwest),'forest_belt_cells':len(belt),'forest_belt_forest':forest,'domestic_plots_preserved':len(parcels),'moved_sites':{str(k):{'from':pos[k],'to':h} for k,h in MOVES.items()},'huguan_local_friendly_passable':True,'huguan_local_hostile_blocked':True,'routes':routes,'maps':summaries}

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    manifest=json.loads(CURRENT.read_text());assert manifest['release']=='0.44.0','Only the pinned v44 snapshot may be refined; no repeated edits'
    for e in manifest['files']:assert hashlib.sha256((ROOT/e['source_path']).read_bytes()).hexdigest()==e['sha256'],e['source_path']
    data,report=refine()
    print(json.dumps({k:v for k,v in report.items() if k!='routes'},ensure_ascii=False,indent=2))
    if not args.apply:return
    for name,(p,t,cities) in data.items():
        file=DIR/name;out=[]
        for line in file.read_text().splitlines():
            if line.startswith('terrain.'):
                key=line.split('=',1)[0];line=key+'='+''.join(t[int(key.split('.')[1])])
            elif line.startswith('city.'):
                key,value=line.split('=',1);f=value.split('|');cid=int(f[0]);f[2],f[3]=map(str,cities[cid]);line=key+'='+'|'.join(f)
            elif line.startswith('revision='):line='revision='+str(int(line.split('=')[1])+1)
            out.append(line)
        file.write_text('\n'.join(out)+'\n')
    index=DIR/'index.txt';out=[]
    for line in index.read_text().splitlines():
        if line and not line.startswith('#'):
            name=line.split()[0];line=name+' '+hashlib.sha256((DIR/(name+'.properties')).read_bytes()).hexdigest()
        out.append(line)
    index.write_text('\n'.join(out)+'\n')
    manifest['release']='0.45.0';manifest['baseline_commit']=report['baseline']
    for e in manifest['files']:e['sha256']=hashlib.sha256((ROOT/e['source_path']).read_bytes()).hexdigest()
    CURRENT.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    dest=ROOT/'docs/validation/v045';dest.mkdir(parents=True,exist_ok=True);(dest/'geography.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()

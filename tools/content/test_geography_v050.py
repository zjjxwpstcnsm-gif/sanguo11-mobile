#!/usr/bin/env python3
"""Validate installed snapshot geography against reviewed pins and the v49 baseline."""
import argparse,hashlib,json,subprocess,sys
from collections import Counter,deque
from pathlib import Path
from refine_geography_v044 import read,neighbors,distance
ROOT=Path(__file__).resolve().parents[2]
REF=json.loads((ROOT/'data/map/reference-v050.json').read_text())
checks=0

def check(ok,why):
    global checks
    checks+=1
    if not ok:raise AssertionError(why)

def main():
    a=argparse.ArgumentParser();a.add_argument('--base',default=REF['baseline_commit']);args=a.parse_args()
    directory=ROOT/'core/src/main/resources/scenarios'
    np,nt,nc=read(directory/'heroes-mobile-sandbox.properties')
    ns={int(k):tuple(v['target']) for k,v in REF['sites'].items()}
    for file in sorted(directory.glob('*.properties')):
        p,t,c=read(file);relative=str(file.relative_to(ROOT))
        old=subprocess.check_output(['git','show',args.base+':'+relative],cwd=ROOT,text=True)
        before=dict(line.split('=',1) for line in old.splitlines() if '=' in line and not line.startswith('#'))
        oldcities={int(v.split('|')[0]):tuple(map(int,v.split('|')[2:4])) for k,v in before.items() if k.startswith('city.')}
        check(set(before)==set(p),(file.name,'record keys'))
        offset=Counter((nc[id][0]-h[0],nc[id][1]-h[1]) for id,h in c.items());check(len(offset)==1,(file.name,'offset'))
        dx,dy=next(iter(offset));check(dy%2==0,(file.name,'odd-r parity'))
        plot_count=Counter();plotpos=set()
        for key,value in p.items():
            f=value.split('|');oldf=before[key].split('|')
            if key.startswith('terrain.'):continue
            if key=='revision':check(int(value)==int(before[key])+1,(file.name,key));continue
            if key.startswith('city.'):
                check(f[:2]+f[4:]==oldf[:2]+oldf[4:],(file.name,key,'non-geographic site data'))
                cid=int(f[0]);globalpos=(int(f[2])+dx,int(f[3])+dy)
                if cid in ns:check(globalpos==ns[cid],(file.name,cid,'reference pin'))
                else:check(c[cid]==oldcities[cid],(file.name,cid,'42 original city anchors'))
            elif key.startswith('development-plot.'):
                check(f[0]==oldf[0],(file.name,key,'slot owner'))
                h=tuple(map(int,f[1:3]));plot_count[int(f[0])]+=1
                check(h not in plotpos and h not in c.values(),(file.name,key,'occupied slot'))
                check(t[h[1]][h[0]]=='P',(file.name,key,'developable terrain'));plotpos.add(h)
                check(distance(h,c[int(f[0])])<=7,(file.name,key,'local slot radius'))
            else:check(value==before[key],(file.name,key,'retained gameplay'))
        for y,row in enumerate(t):
            for x,ch in enumerate(row):check(ch==nt[y+dy][x+dx],(file.name,x,y,'national/regional seam'))
        check(plot_count==Counter(int(v.split('|')[0]) for k,v in before.items() if k.startswith('development-plot.')),(file.name,'every city capacity'))
        if len(t)==100:check(sum(plot_count.values())==591,(file.name,'591 slots'))
        for id,h in c.items():
            check(t[h[1]][h[0]]=='P',(file.name,id,'site terrain'))
            dry=[v for v in neighbors(h) if 0<=v[0]<len(t[0]) and 0<=v[1]<len(t) and t[v[1]][v[0]] in 'PFAZB' and v not in plotpos and v not in c.values()]
            if id>=20052:
                water=[v for v in neighbors(h) if 0<=v[0]<len(t[0]) and 0<=v[1]<len(t) and t[v[1]][v[0]] in 'WO']
                check(any(distance(a,b)==1 for a in dry for b in water),(file.name,id,'dock edge with every plot built'))
            elif id<20042:check(dry,(file.name,id,'unblocked deployment after building every slot'))
        print('PASS snapshot',file.name,len(c),'sites',sum(plot_count.values()),'slots')
    # Sourced wide Yangtze, Han and lake network must be navigable, not isolated pools.
    allowed={(x,y) for y,row in enumerate(nt) for x,c in enumerate(row) if c in 'WO'}
    sizes=[];labels={}
    for start in sorted(allowed):
        if start in labels:continue
        queue=deque([start]);labels[start]=len(sizes);size=0
        while queue:
            h=queue.popleft();size+=1
            for v in neighbors(h):
                if v in allowed and v not in labels:labels[v]=labels[start];queue.append(v)
        sizes.append(size)
    components={id:{labels[v] for v in neighbors(h) if v in labels} for id,h in ns.items() if id>=20052}
    yangtze=set.intersection(*(components[id] for id in range(20067,20087)))
    check(yangtze,'All twenty Han/Yangtze/lake ports must share a water component')
    check(max(sizes[i] for i in yangtze)>500,'Yangtze is a real navigable network')
    # No changed sprite resources, no image included as an unlicensed runtime asset.
    manifest=json.loads((ROOT/'tools/content/map-release-manifest.json').read_text())
    for entry in manifest['files']:
        path=entry['source_path'];raw=(ROOT/path).read_bytes()
        check(hashlib.sha256(raw).hexdigest()==entry['sha256'],path+' checksum')
        if path.startswith('app/src/main/assets/'):
            old=subprocess.check_output(['git','show',args.base+':'+path],cwd=ROOT);check(raw==old,path+' exact v49 art')
    print('PASS v50 geography:',checks,'checks; water components',sorted(sizes,reverse=True)[:10])
if __name__=='__main__':main()

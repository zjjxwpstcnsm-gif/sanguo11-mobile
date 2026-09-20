#!/usr/bin/env python3
"""v55 authored Guan-Luo correction + mechanical 7-cell footprint adaptation.
Pixel anchors below were read directly from the user-supplied 7200x6752 reference,
not produced from project coordinates. Native column-offset cells are downsampled
2:1 to the existing odd-row map: this is NOT a native 200x200 migration.
No image pixels or copyrighted reference are embedded in the game.
"""
from pathlib import Path
import hashlib,json,re
from collections import Counter
ROOT=Path(__file__).resolve().parents[2]
SCENARIOS=ROOT/'core/src/main/resources/scenarios'
DIRS=((1,0),(1,-1),(0,-1),(-1,0),(-1,1),(0,1))
def axial(p):x,y=p;return x-(y-(y&1))//2,y
def source(p):q,r=p;return q+(r-(r&1))//2,r
def neighbors(p):q,r=axial(p);return [source((q+a,r+b)) for a,b in DIRS]
def distance(a,b):q,r=axial(a);s,t=axial(b);return max(abs(q-s),abs(r-t),abs(q+r-s-t))
def props(text):return dict(l.split('=',1) for l in text.splitlines() if l and not l.startswith('#') and '=' in l)
# Independent visual observations, source pixel box (1450,2050)-(3250,2900).
ANCHORS=[
 (20017,'長安',(1886,2502),(53,73),(26,36)),
 (20044,'潼關',(2135,2628),(61,77),(30,38)),
 (20045,'函谷關',(2369,2644),(68,78),(34,38)),
 (20015,'洛陽',(2723,2596),(79,76),(39,38)),
 (20043,'虎牢關',(3079,2609),(90,77),(45,38)),
 (20064,'解縣港',(2272,2239),(65,65),(32,32)),
 (20065,'新豐港',(2239,2516),(64,74),(32,36)),
 (20063,'孟津港',(2724,2401),(79,70),(39,35)),
]
# Middle of elongated gate artwork is NOT a collision mask; integer placement
# is chosen at the passage, not from its painted height. Ports snap to shoreline.
REGIONAL={}
for x,lo,hi in ((30,35,42),(34,35,42),(45,35,44)):
 for y in range(lo,hi+1):REGIONAL[x,y]='P' if y==38 else 'M'
# Southern Qinling/Mt. Song ridge visible below the Luoyang basin. The previous
# isolated P patches created bypasses; preserve Wu gate and its two side valleys.
for y in (41,42):
 for x in range(35,45):REGIONAL[x,y]='M'
# Named gate approach cells: one real passable gate, not a three-cell force field.
for x in (30,34,45):
 for dx in (-1,1):REGIONAL[x+dx,38]='P'
# South bank of the bend: restore water at old Mengjin marker; actual dock on bank.
REGIONAL[39,34]='W';REGIONAL[39,35]='P'
# Reference forest belts NE of Chang'an, NE and SW of Luoyang (not mountains).
for p in ((24,32),(25,32),(25,33),(26,33),(27,33),(27,34),
          (41,34),(42,34),(42,35),(43,35),(42,36),(43,36),
          (36,39),(36,40),(37,40)):
 REGIONAL[p]='F'
# Accessible bank-to-port approach, not a new shore crossing exception.
for p in ((32,35),(32,36),(32,37),(39,36),(40,35),(32,31),(33,32)):
 REGIONAL[p]='P'

def refine(path,offset):
 original=path.read_text();d=props(original)
 rows=[list(d[f'terrain.{y}']) for y in range(int(d['height']))];before=[r[:] for r in rows]
 width=len(rows[0]);height=len(rows)
 sites={int(v.split('|')[0]):(k,v.split('|')) for k,v in d.items() if re.fullmatch(r'city\.\d+',k)}
 kinds={int(v.split('|')[0]):v.split('|')[1] for k,v in d.items() if re.fullmatch(r'site-kind\.\d+',k)}
 changes=[];positions=[]
 dx,dy=offset
 regional={(x-dx,y-dy):v for (x,y),v in REGIONAL.items() if 0<=x-dx<width and 0<=y-dy<height}
 # Only the independently reviewed eight sites; do not batch move other cities.
 for sid,name,px,native,position in ANCHORS:
  if sid not in sites:continue
  key,f=sites[sid];old=tuple(map(int,f[2:4]));new=(position[0]-dx,position[1]-dy)
  if old!=new:positions.append({'id':sid,'name':name,'before':old,'after':new})
  f[2:4]=map(str,new);d[key]='|'.join(f)
 for (x,y),kind in regional.items():rows[y][x]=kind
 bodies={};centers={}
 for sid,(key,f) in sites.items():
  point=tuple(map(int,f[2:4]));centers[sid]=point
  body=[point]+(neighbors(point) if kinds.get(sid,'CITY')=='CITY' else [])
  for p in body:
   assert p not in bodies,(sid,p,bodies.get(p),'site overlap');bodies[p]=sid
   x,y=p
   if kinds.get(sid,'CITY')=='CITY':rows[y][x]='P'
   elif rows[y][x] in 'WVM':rows[y][x]='P'
 # No field development inside any body. Preserve capacity and avoid choke/shore
 # access even after the player builds every parcel. Only choose existing plains.
 plots=[(k,v.split('|')) for k,v in d.items() if re.fullmatch(r'development-plot\.\d+',k)]
 reserved=set(bodies)
 for sid,p in centers.items():
  if kinds.get(sid) in ('PORT','GATE'):reserved.update(neighbors(p))
 # Two explicit land exits per city retained free of economic buildings.
 for sid,p in centers.items():
  if kinds.get(sid,'CITY')!='CITY':continue
  for sign in (-1,1):
   for n in (2,3):reserved.add((p[0]+sign*n,p[1]))
 used=set();relocate=[]
 for key,f in plots:
  p=tuple(map(int,f[1:3]));x,y=p
  if p in reserved or p in used or rows[y][x]!='P':relocate.append((key,f,p))
  else:used.add(p)
 moved=[]
 for key,f,old in relocate:
  sid=int(f[0]);center=centers[sid]
  candidates=[(x,y) for y in range(max(0,center[1]-8),min(height,center[1]+9))
       for x in range(max(0,center[0]-8),min(width,center[0]+9))
       if (x,y) not in reserved and (x,y) not in used and rows[y][x]=='P'
       and distance(center,(x,y))<=8]
  assert candidates,('no replacement plot',sid,old)
  chosen=min(candidates,key=lambda p:(distance(p,old),distance(p,center),p[1],p[0]));used.add(chosen)
  f[1:3]=map(str,chosen);d[key]='|'.join(f)
  moved.append({'city':sid,'before':old,'after':chosen})
 assert len(used)==len(plots)==int(d['development-plots'])
 for y,row in enumerate(rows):
  for x,new in enumerate(row):
   if new!=before[y][x]:changes.append({'xy':[x,y],'before':before[y][x],'after':new,
    'reason':'guanluo-reference-review' if (x,y) in regional else 'mechanical-seven-tile-footprint-not-new-geographic-calibration'})
  d[f'terrain.{y}']=''.join(row)
 # Keep revision stable on re-run. It identifies this new-game geography.
 d['revision']='9'
 updated='\n'.join(d.get(l.split('=',1)[0],None) is not None and l.split('=',1)[0]+'='+d[l.split('=',1)[0]] or l if '=' in l and not l.startswith('#') else l for l in original.splitlines())+'\n'
 path.write_text(updated)
 return {'scenario':path.stem,'offset':offset,'positions':positions,'terrain':changes,'development_relocated':moved,
         'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}

def main():
 national=props((SCENARIOS/'heroes-mobile-sandbox.properties').read_text())
 centers={int(v.split('|')[0]):tuple(map(int,v.split('|')[2:4])) for k,v in national.items() if re.fullmatch(r'city\.\d+',k)}
 offsets={}
 for p in sorted(SCENARIOS.glob('*.properties')):
  d=props(p.read_text());frames=Counter((centers[int(v.split('|')[0])][0]-int(v.split('|')[2]),centers[int(v.split('|')[0])][1]-int(v.split('|')[3])) for k,v in d.items() if re.fullmatch(r'city\.\d+',k))
  offset,count=frames.most_common(1)[0];assert offset[1]%2==0 and count>=len(centers)/4 or len(frames)==1
  offsets[p]=offset
 reports=[refine(p,offset) for p,offset in offsets.items()]
 byname={r['scenario']:r['sha256'] for r in reports};index=SCENARIOS/'index.txt'
 index.write_text(''.join(l.split()[0]+' '+byname[l.split()[0]]+'\n' if l.strip() and not l.startswith('#') else l+'\n' for l in index.read_text().splitlines()))
 audit={'version':'0.55.0','reference_sha256':'a5a4e8c7f9785b6fbadc8487508ff6c937dcef2d782b099f2e876e48d167d4e0',
  'reference_size':[7200,6752],'crop':[1450,2050,3250,2900],
  'mapping':'Original column-staggered ~200x200 to retained odd-row 100x100; no native-coordinate migration; floor 2:1 plus bank/pass snap. Pixel landmarks measured independently before comparison.',
  'anchors':[{'id':i,'name':n,'pixel':p,'native_estimate':v,'source_cell':c,
              'precision':'manually identified landmark, pixel +/-6; native gate midpoint/port anchor estimate, not a collision outline'} for i,n,p,v,c in ANCHORS],
  'scope':'Eight reviewed Guan-Luo landmarks; original seven footprints are product geometry, not painted icon bounds. Other regions only mechanical footprint/parcel adaptation, NOT newly calibrated geography.',
  'scenarios':reports}
 out=ROOT/'data/map/review-v055.json'
 # Never erase the first before/after audit with an idempotent zero-change pass.
 if not out.exists() or any(r['terrain'] or r['positions'] or r['development_relocated'] for r in reports):out.write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n')
 for r in reports:print(r['scenario'],len(r['terrain']),'terrain;',len(r['development_relocated']),'plots;',len(r['positions']),'sites')
if __name__=='__main__':main()

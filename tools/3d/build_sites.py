#!/usr/bin/env python3
"""Original CC0 strategic architecture. No Blender/service needed; deterministic GLB 2.0 export."""
import json, math, struct, hashlib, os
from pathlib import Path
from PIL import Image
ROOT=Path(os.environ.get('ASSET_OUTPUT_ROOT',Path(__file__).resolve().parents[2]))
(ROOT/'docs/3d').mkdir(parents=True,exist_ok=True)
OUT=ROOT/'app/src/main/assets/3d/sites'
OUT.mkdir(parents=True,exist_ok=True)
# Shared stone / roof tile / timber atlas: restrained repeatable texture, no external source.
im=Image.new('RGB',(192,64))
for y in range(64):
 for x in range(192):
  panel=x//64; u=x%64; noise=((x*37+y*71+x*y*3)%11)-5
  if panel==0:
   seam=y%12<2 or (u+(8 if y//12%2 else 0))%24<2
   c=(81,79,70) if seam else (148,143,126)
  elif panel==1:
   seam=u%8<2 or y%13<2; c=(43,51,51) if seam else (87,97,94)
  else:
   seam=u%13<2; c=(66,45,29) if seam else (124+(y%5),88,57)
  im.putpixel((x,y),tuple(max(0,min(255,a+noise)) for a in c))
im.save(OUT/'atlas.png',optimize=True)
class Mesh:
 def __init__(self): self.p=[]; self.c=[]; self.uv=[]; self.idx=[]
 def face(self,points,mat=0,shade=1):
  n=len(self.p); self.p.extend(points);self.c.extend([(shade if shade<.5 else 1,)*3+(1,)]*len(points))
  self.uv.extend([((mat+(u*.94+.03))/3,v*.94+.03) for u,v in [(0,0),(1,0),(1,1),(0,1)][:len(points)]])
  for i in range(1,len(points)-1):self.idx.extend([n,n+i+1,n+i])
 def box(self,x,y,z,w,h,d,mat=0):
  X,Y,Z=x+w,y+h,z+d
  self.face([(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],mat,1)
  self.face([(x,y,z),(X,y,z),(X,Y,z),(x,Y,z)],mat,.8)
  self.face([(X,y,Z),(x,y,Z),(x,Y,Z),(X,Y,Z)],mat,.9)
  self.face([(x,y,Z),(x,y,z),(x,Y,z),(x,Y,Z)],mat,.72)
  self.face([(X,y,z),(X,y,Z),(X,Y,Z),(X,Y,z)],mat,.95)
 def roof(self,x,z,w,d,y,h):
  # Hip roof with flared eaves and a raised ridge, genuinely volumetric on all four sides.
  a=(x-w/2,y,z-d/2);b=(x+w/2,y,z-d/2);c=(x+w/2,y,z+d/2);e=(x-w/2,y,z+d/2)
  r=(x-w*.24,y+h,z);s=(x+w*.24,y+h,z)
  self.face([a,b,s,r],1,.95);self.face([c,e,r,s],1,.8)
  self.face([e,a,r],1,.72);self.face([b,c,s],1,1)
  self.box(x-w*.26,y+h,z-.025,w*.52,.03,.05,1)
 def hall(self,x,z,w,d,h,lod):
  self.box(x-w/2,.07,z-d/2,w,h,d,2)
  self.roof(x,z,w*1.25,d*1.3,h+.07,h*.4)
  if lod==0:
   for xx in [x-w*.4,x,x+w*.4]:self.box(xx-.015,.07,z+d/2,.03,h,.035,2)
   self.box(x-w*.55,.04,z+d/2,w*1.1,.04,.09)
 def wall(self,x,z,w,d,lod):
  self.box(x,0,z,w,.27,d)
  if lod==0:
   n=max(1,int(max(w,d)/.14))
   for k in range(n):self.box(x+(k*w/n if w>d else 0),.27,z+(k*d/n if d>=w else 0),min(w,.075),.065,min(d,.075))
 def curved_roof(self,x,z,w,d,y,h,lod):
  # Three hip rings: raised eave tips, concave slope and shortened ridge.
  rings=[(w*.5,d*.5,y+.035),(w*.36,d*.34,y+h*.25),(w*.23,0,y+h)]
  if lod==2:rings=[rings[0],rings[-1]]
  for (rx,rz,yy),(tx,tz,Y) in zip(rings,rings[1:]):
   lower=[(x-rx,yy,z-rz),(x+rx,yy,z-rz),(x+rx,yy,z+rz),(x-rx,yy,z+rz)]
   upper=[(x-tx,Y,z-tz),(x+tx,Y,z-tz),(x+tx,Y,z+tz),(x-tx,Y,z+tz)]
   for i in range(4):
    j=(i+1)%4
    self.face([lower[i],lower[j],upper[j]] if upper[i]==upper[j] else [lower[i],lower[j],upper[j],upper[i]],1)
  self.box(x-w*.26,y+h,z-.017,w*.52,.026,.034,1)
 def pavilion(self,x,z,w,d,h,lod,base=.045):
  self.box(x-w*.54,base-.045,z-d*.54,w*1.08,.06,d*1.08)
  self.box(x-w/2,base,z-d/2,w,h,d,2)
  self.curved_roof(x,z,w*1.22,d*1.27,base+h,h*.40,lod)
  if lod<2:
   front=z+d/2+.002
   self.face([(x-w*.13,base,front),(x+w*.13,base,front),(x+w*.13,base+h*.72,front),(x-w*.13,base+h*.72,front)],2,.35)
   for xx in [x-w*.40,x+w*.40]:self.box(xx-.009,base,front,.018,h,.024,2)
   for side in [-1,1]:
    xx=x+side*w*.30
    self.face([(xx-w*.065,base+h*.35,front),(xx+w*.065,base+h*.35,front),(xx+w*.065,base+h*.68,front),(xx-w*.065,base+h*.68,front)],2,.36)
   if lod==0:
    for k in range(3):self.box(x-w*.19,base-.045+k*.015,z+d*.54+(2-k)*.025,w*.38,.015,.025)
 def segment(self,a,b,lod):
  # Wall follows a chamfered perimeter; a battered foot embeds into the flat city pad.
  dx,dz=b[0]-a[0],b[1]-a[1];length=math.hypot(dx,dz);ux,uz=dx/length,dz/length
  nx,nz=-uz,ux
  def point(t,n,y):return(a[0]+ux*t+nx*n,y,a[1]+uz*t+nz*n)
  for sign in [-1,1]:self.face([point(0,sign*.065,-.035),point(length,sign*.065,-.035),point(length,sign*.045,.27),point(0,sign*.045,.27)])
  self.face([point(0,-.045,.27),point(length,-.045,.27),point(length,.045,.27),point(0,.045,.27)])
  for t in [0,length]:self.face([point(t,-.065,-.035),point(t,.065,-.035),point(t,.045,.27),point(t,-.045,.27)])
  if lod==0:
   n=max(1,int(length/.14))
   for k in range(n):
    t=(k+.5)*length/n;pts=[point(t+u,v,y) for y in [.27,.33] for u,v in [(-.035,-.048),(.035,-.048),(.035,.048),(-.035,.048)]]
    self.face(pts[4:]);
    for i in range(4):self.face([pts[i],pts[(i+1)%4],pts[(i+1)%4+4],pts[i+4]])
 def build(self,kind,lod):
  if kind.startswith('city'):
   v=int(kind[-1]);w=[1.04,.96,.90][v];d=[.78,.78,.71][v];cut=.20 if v!=2 else .32
   perimeter=[(-w+cut,-d),(w-cut,-d),(w,-d+cut),(w,d-cut),(w-cut,d),(-w+cut,d),(-w,d-cut),(-w,-d+cut)]
   for i,a in enumerate(perimeter):
    b=perimeter[(i+1)%8]
    # Four open axial streets rather than an opaque front door or collision proxy.
    if i%2==0:
     mx,mz=(a[0]+b[0])/2,(a[1]+b[1])/2;dx,dz=b[0]-a[0],b[1]-a[1];ln=math.hypot(dx,dz);ux,uz=dx/ln,dz/ln
     self.segment(a,(mx-ux*.13,mz-uz*.13),lod);self.segment((mx+ux*.13,mz+uz*.13),b,lod)
     first=len(self.p);self.pavilion(0,0,.33,.18,.15,lod,.29)
     angle=math.atan2(uz,ux);c,s=math.cos(angle),math.sin(angle)
     self.p[first:]=[(mx+c*x-s*z,y,mz+s*x+c*z) for x,y,z in self.p[first:]]
    else:self.segment(a,b,lod)
   # Common height and readable towers persist at all LODs.
   for x,z in [(-w+cut/2,-d+cut/2),(w-cut/2,-d+cut/2),(-w+cut/2,d-cut/2),(w-cut/2,d-cut/2)]:
    self.pavilion(x,z,.21,.21,.30,lod)
   if v==0: # Broad central hall and axial courtyards of the plains.
    self.pavilion(0,-.28,.62,.38,.43,lod)
    self.curved_roof(0,-.28,.50,.34,.63,.13,lod)
    houses=[(-.58,-.27,.24,.30),(.58,-.27,.24,.30),(-.57,.27,.27,.21),(.57,.27,.27,.21)]
   elif v==1: # Low waterside lanes and paired long warehouses.
    self.pavilion(-.34,-.29,.30,.44,.28,lod);self.pavilion(.34,-.29,.30,.44,.28,lod)
    houses=[(-.55,.23,.24,.32),(.55,.23,.24,.32),(-.19,.31,.22,.20),(.19,.31,.22,.20)]
   else: # Compact basin settlement, stepped inner citadel, tall watch hall.
    self.box(-.40,-.025,-.57,.80,.12,.47)
    self.pavilion(0,-.32,.48,.34,.49,lod,.095)
    self.curved_roof(0,-.32,.39,.30,.70,.14,lod)
    houses=[(-.50,.02,.23,.25),(.50,.02,.23,.25),(-.38,.35,.25,.20),(.38,.35,.25,.20)]
   for x,z,ww,dd in houses:self.pavilion(x,z,ww,dd,.16 if v==1 else .20,lod)
   if lod==0:
    for x in [-.27,.27]:self.pavilion(x,.10,.17,.15,.13,lod)
  elif kind=='port':
   # +Z is navigable water. Buildings remain behind shore, piles meet the water.
   self.box(-.37,-.055,-.38,.74,.11,.47)
   self.pavilion(-.08,-.19,.43,.27,.25,lod)
   self.box(-.10,.005,.08,.20,.055,.74,2)
   self.box(-.36,.005,.65,.72,.055,.13,2)
   if lod<2:
    for x in [-.32,.29]:
     for z in [.66,.75]:self.box(x,-.13,z,.035,.23,.035,2)
    for z in [.22,.44,.78]:
     for x in [-.085,.055]:self.box(x,-.13,z,.03,.23,.03,2)
    if lod==0:
     for z in [.12,.20,.28,.36,.44,.52,.60,.68,.76]:self.box(-.102,.061,z,.204,.008,.012,2)
     self.pavilion(.23,-.18,.19,.22,.16,lod)
     # Timber hoist and diagonal brace, away from the central landing path.
     self.box(-.31,.055,.51,.026,.34,.026,2);self.box(-.31,.365,.51,.20,.025,.025,2)
     self.face([(-.31,.24,.51),(-.18,.365,.51),(-.21,.365,.51),(-.31,.28,.51)],2)
  else:
   for x in [-.46,.17]:self.wall(x,-.16,.29,.32,lod)
   # Battered ends visually join terrain without extending into adjacent cell centres.
   for side in [-1,1]:
    self.face([(side*.48,-.04,-.18),(side*.48,-.04,.18),(side*.42,.27,.14),(side*.42,.27,-.14)])
   self.box(-.18,.30,-.16,.36,.15,.32)
   self.curved_roof(0,0,.63,.46,.45,.17,lod)
   if lod<2:self.curved_roof(0,0,.50,.36,.61,.12,lod)
   if lod==0:
    for side in [-1,1]:
     for step in range(4):self.box(side*(.175-step*.020)-.016,.17+step*.035,.163,.032,.035,.035)
     self.box(side*.25-.012,.33,.17,.024,.15,.024,2)
    self.box(-.14,.31,.164,.28,.025,.035,2)
  return self
 def write(self,name):
  arrays=[(self.p,'f',5126,'VEC3'),(self.c,'f',5126,'VEC4'),(self.uv,'f',5126,'VEC2'),(self.idx,'I',5125,'SCALAR')]
  data=b''; views=[];access=[]
  for arr,fmt,typ,shape in arrays:
   flat=[n for v in arr for n in v] if shape!='SCALAR' else arr
   buf=struct.pack('<'+fmt*len(flat),*flat);views.append({'buffer':0,'byteOffset':len(data),'byteLength':len(buf)});data+=buf
   a={'bufferView':len(views)-1,'componentType':typ,'count':len(arr),'type':shape}
   if shape=='VEC3':a.update(min=[min(p[i] for p in arr) for i in range(3)],max=[max(p[i] for p in arr) for i in range(3)])
   access.append(a)
  png=(OUT/getattr(self,'atlas_file','atlas.png')).read_bytes();views.append({'buffer':0,'byteOffset':len(data),'byteLength':len(png)});data+=png;data+=b'\0'*((-len(data))%4)
  doc={'asset':{'version':'2.0','generator':'sanguo11 original modular architecture CC0'},'scene':0,'scenes':[{'nodes':[0]}],'nodes':[{'mesh':0}], 'meshes':[{'primitives':[{'attributes':{'POSITION':0,'COLOR_0':1,'TEXCOORD_0':2},'indices':3,'material':0,'mode':4}]}], 'buffers':[{'byteLength':len(data)}],'bufferViews':views,'accessors':access,'images':[{'bufferView':4,'mimeType':'image/png'}],'textures':[{'source':0}],'materials':[{'doubleSided':True,'pbrMetallicRoughness':{'baseColorTexture':{'index':0},'metallicFactor':0,'roughnessFactor':1}}]}
  js=json.dumps(doc,separators=(',',':')).encode();js+=b' '*((-len(js))%4)
  glb=struct.pack('<III',0x46546c67,2,28+len(js)+len(data))+struct.pack('<II',len(js),0x4e4f534a)+js+struct.pack('<II',len(data),0x004e4942)+data
  (OUT/(name+'.glb')).write_bytes(glb)
  return {'file':name+'.glb','sha256':hashlib.sha256(glb).hexdigest(),'bytes':len(glb),'triangles':len(self.idx)//3,'vertices':len(self.p),'materials':1,'texture':[192,64],'bounds':access[0]['min']+access[0]['max']}
report=[Mesh().build(k,l).write(k+'-lod'+str(l)) for k in ['city0','city1','city2','port','gate'] for l in range(3)]
(ROOT/'docs/3d/site-assets.json').write_text(json.dumps({'license':'CC0-1.0 original project geometry and atlas','unit':'1 projected tile span','origin':'ground center, +Y up, port +Z points to water','loader':'SiteGlb: static triangle POSITION/COLOR_0/TEXCOORD_0, uint32 indices; one embedded PNG; no animation/compression/extensions','assets':report},indent=2)+'\n')
print('Generated',len(report),'GLBs;',sum(a['bytes'] for a in report),'bytes')

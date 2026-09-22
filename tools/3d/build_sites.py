#!/usr/bin/env python3
"""Original CC0 strategic architecture. No Blender/service needed; deterministic GLB 2.0 export."""
import json, math, struct, hashlib
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
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
  n=len(self.p); self.p.extend(points);self.c.extend([(shade,shade,shade,1)]*len(points))
  self.uv.extend([((mat+(u*.94+.03))/3,v*.94+.03) for u,v in [(0,0),(1,0),(1,1),(0,1)][:len(points)]])
  for i in range(1,len(points)-1):self.idx.extend([n,n+i,n+i+1])
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
 def build(self,kind,lod):
  if kind.startswith('city'):
   v=int(kind[-1]);w=2.12 if v!=1 else 1.96;d=1.65 if v!=2 else 1.5
   self.box(-w/2,0,-d/2,w,.04,d)
   self.wall(-w/2,-d/2,w,.10,lod);self.wall(-w/2,-d/2,.10,d,lod);self.wall(w/2-.10,-d/2,.10,d,lod)
   self.wall(-w/2,d/2-.10,w/2-.16,.10,lod);self.wall(.16,d/2-.10,w/2-.16,.10,lod)
   self.box(-.2,.25,d/2-.13,.4,.12,.16);self.roof(0,d/2-.04,.5,.32,.4,.12)
   if lod<2:
    for x in [-w/2+.07,w/2-.07]:
     for z in [-d/2+.07,d/2-.07]:self.hall(x,z,.21,.21,.38,lod)
    self.hall(0,-.28,.65 if v!=1 else .42,.44,.5 if v==0 else .38,lod)
    if v==0:self.roof(0,-.28,.53,.4,.72,.15)
    if v==1:
     self.hall(-.56,-.1,.31,.38,.28,lod);self.hall(.56,-.1,.31,.38,.28,lod)
    if v==2:self.wall(-.85,-.13,1.7,.065,lod)
    if lod==0:
     for x in [-.64,-.33,.33,.64]:self.hall(x,.34,.20,.26,.18,lod)
     if v==0:self.hall(-.65,-.45,.23,.22,.22,lod);self.hall(.65,-.45,.23,.22,.22,lod)
   else:self.roof(0,-.25,.75,.5,.35,.22)
  elif kind=='port':
   self.box(-.37,0,-.38,.74,.06,.58)
   self.hall(0,-.18,.46,.29,.26,lod)
   self.box(-.12,.015,.12,.24,.05,.75,2)
   self.box(-.38,.015,.69,.76,.05,.16,2)
   if lod<2:
    for x in [-.3,.3]:
     for z in [.3,.75]:self.box(x,-.08,z,.05,.2,.05,2)
    if lod==0:
     self.box(.2,.07,-.2,.12,.12,.12,2);self.box(.23,.02,.42,.15,.055,.3,2)
  else:
   for x in [-.48,.18]:self.wall(x,-.16,.30,.32,lod)
   self.box(-.2,.3,-.16,.4,.18,.32)
   if lod<2:self.roof(0,0,.65,.5,.5,.18)
   if lod==0:
    for x in [-.42,.34]:self.box(x,.3,-.13,.08,.1,.26)
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
  png=(OUT/'atlas.png').read_bytes();views.append({'buffer':0,'byteOffset':len(data),'byteLength':len(png)});data+=png;data+=b'\0'*((-len(data))%4)
  doc={'asset':{'version':'2.0','generator':'sanguo11 original modular architecture CC0'},'scene':0,'scenes':[{'nodes':[0]}],'nodes':[{'mesh':0}], 'meshes':[{'primitives':[{'attributes':{'POSITION':0,'COLOR_0':1,'TEXCOORD_0':2},'indices':3,'material':0,'mode':4}]}], 'buffers':[{'byteLength':len(data)}],'bufferViews':views,'accessors':access,'images':[{'bufferView':4,'mimeType':'image/png'}],'textures':[{'source':0}],'materials':[{'doubleSided':True,'pbrMetallicRoughness':{'baseColorTexture':{'index':0},'metallicFactor':0,'roughnessFactor':1}}]}
  js=json.dumps(doc,separators=(',',':')).encode();js+=b' '*((-len(js))%4)
  glb=struct.pack('<III',0x46546c67,2,28+len(js)+len(data))+struct.pack('<II',len(js),0x4e4f534a)+js+struct.pack('<II',len(data),0x004e4942)+data
  (OUT/(name+'.glb')).write_bytes(glb)
  return {'file':name+'.glb','sha256':hashlib.sha256(glb).hexdigest(),'bytes':len(glb),'triangles':len(self.idx)//3,'vertices':len(self.p),'materials':1,'texture':[192,64],'bounds':access[0]['min']+access[0]['max']}
report=[Mesh().build(k,l).write(k+'-lod'+str(l)) for k in ['city0','city1','city2','port','gate'] for l in range(3)]
(ROOT/'docs/3d/site-assets.json').write_text(json.dumps({'license':'CC0-1.0 original project geometry and atlas','unit':'1 projected tile span','origin':'ground center, +Y up, port +Z points to water','loader':'SiteGlb: static triangle POSITION/COLOR_0/TEXCOORD_0, uint32 indices; one embedded PNG; no animation/compression/extensions','assets':report},indent=2)+'\n')
print('Generated',len(report),'GLBs;',sum(a['bytes'] for a in report),'bytes')

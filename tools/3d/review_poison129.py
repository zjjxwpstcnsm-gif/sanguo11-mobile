#!/usr/bin/env python3
"""HOST ONLY. Rasterize real exported mesh attributes and emulate ground baseColor.
This is an albedo-design diagnostic, not a Filament screenshot or APK acceptance.
"""
from pathlib import Path
import sys,struct,json,hashlib
import numpy as np
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parents[2]
out=Path(sys.argv[1]);out.mkdir(exist_ok=True,parents=True)
def mesh(path):
 data=memoryview(path.read_bytes());offset=0
 def ints(n):
  nonlocal offset
  a=np.frombuffer(data[offset:offset+n*4],dtype='>i4').astype('int32');offset+=n*4;return a
 def floats(n):
  nonlocal offset
  a=np.frombuffer(data[offset:offset+n*4],dtype='>f4').astype('float32');offset+=n*4;return a
 magic,version,count=ints(3);assert magic==0x50533129
 all=[]
 for _ in range(count):
  v,i,land=ints(3);all.append((floats(v).reshape(-1,7),floats(v//7*8).reshape(-1,8),ints(i).reshape(-1,3),land))
 assert offset==len(data)
 return version,all
oldversion,old=mesh(out/'before/mesh.bin');version,new=mesh(out/'after/mesh.bin')
changed=0
for a,b in zip(old,new):
 assert np.array_equal(a[0],b[0]),'terrain positions and biome weights must stay byte-identical'
 assert np.array_equal(a[1][:,:7],b[1][:,:7]),'UVs, normals and shore metadata must stay byte-identical'
 assert np.array_equal(a[2],b[2]) and a[3]==b[3],'indices and exact water partition must stay byte-identical'
 changed+=int(np.count_nonzero(a[1][:,7]!=b[1][:,7]))
assert changed>0

def smooth(a,b,x):
 t=np.clip((x-a)/(b-a),0,1);return t*t*(3-2*t)
def linear(rgb):return np.where(rgb<=.04045,rgb/12.92,((rgb+.055)/1.055)**2.4)
def srgb(rgb):return np.where(rgb<=.0031308,rgb*12.92,1.055*np.maximum(rgb,0)**(1/2.4)-.055)
textures=[linear(np.asarray(Image.open(root/f'app/src/main/assets/3d/terrain/{name}_color.png').convert('RGB'),dtype=np.float32)/255) for name in ('grass','soil','sand','rock')]
def sample(tex,u,v):
 h,w=tex.shape[:2];x=(u%1)*w-.5;y=(v%1)*h-.5;x0=np.floor(x).astype(int);y0=np.floor(y).astype(int);a=(x-x0)[...,None];b=(y-y0)[...,None]
 return ((tex[y0%h,x0%w]*(1-a)+tex[y0%h,(x0+1)%w]*a)*(1-b)+(tex[(y0+1)%h,x0%w]*(1-a)+tex[(y0+1)%h,(x0+1)%w]*a)*b)
def shade(f,marked,width):
 uv=f[:,:,:2];x,y=uv[:,:,0],uv[:,:,1];u,v=x*.35,y*.35
 ub=(u*.8-v*.6)*.731+.317;vb=(u*.6+v*.8)*.731+.619
 weights=np.maximum(f[:,:,2:6],0);weights/=np.maximum(weights.sum(2,keepdims=True),1e-5)
 color=np.zeros_like(f[:,:,:3]);broad=np.zeros_like(color)
 for i,t in enumerate(textures):color+=sample(t,u,v)*weights[:,:,i,None];broad+=sample(t,ub,vb)*weights[:,:,i,None]
 color=color*.62+broad*.38
 color*=1+(np.array([.90,1.04,.85])-1)*weights[:,:,0,None]
 art=np.array([1,.98,.94]);macro=.97+.025*np.sin(x*.113+np.sin(y*.071))+.015*np.cos(y*.139-x*.047)
 wet=1-smooth(0,.38,np.abs(f[:,:,6]));tone=f[:,:,7];poison=smooth(1.02,1.22,tone) if marked else np.zeros_like(tone)
 normal_tone=.96+.04*np.sin(x*.19)*np.cos(y*.17)
 terrain_tone=np.minimum(tone,1)*(1-smooth(1,1.04,tone))+normal_tone*smooth(1,1.04,tone) if marked else tone
 ground=color*art*macro[:,:,None]*terrain_tone[:,:,None]*(1-.36*wet[:,:,None])
 if marked:
  # National production payload uses staggered=1, offset=99.
  row=np.floor(x+.5);column=np.floor(-y-row*.5+99+.5)
  lx=x-row;ly=-y-(column+row*.5-99);angle=np.arctan2(ly,lx);seed=row*.77+(column+row*.5-99)*.55
  radius=.32+.045*np.sin(angle*3+seed)+.025*np.sin(angle*5-seed*1.7);distance=np.sqrt(lx*lx+ly*ly)
  poison*=1-smooth(radius-.025,radius+.045,distance)
  px,py=x*3.1,y*3.1;drift=np.sin(px*1.7+py*.83)+np.sin(py*2.3-px*.69)
  mineral=.5+.25*np.sin(px*3.7+drift)+.25*np.sin(py*4.1-drift)
  basin=1-smooth(radius*.43,radius*.73,distance+(mineral-.5)*.055)
  sulfur=1-smooth(.018,.052,np.abs(distance-radius*.83))
  sulfur*=smooth(.48,.80,mineral)
  sulfur*=1-smooth(.5,2,np.sqrt(2)*3.1*width/f.shape[0])
  spring=np.array([.075,.095,.078])*(1-basin[:,:,None])+np.array([.10,.21,.185])*basin[:,:,None]
  spring=spring*(1-sulfur[:,:,None]*.58)+np.array([.30,.285,.115])*sulfur[:,:,None]*.58
  spring*=.88+.24*mineral[:,:,None]
  ground=ground*(1-poison[:,:,None])+spring*art*macro[:,:,None]*poison[:,:,None]
 return np.clip(srgb(np.maximum(ground,0)),0,1),poison

def raster(meshes,center,width,size=650):
 lo=np.array(center)-width/2;hi=lo+width;f=np.zeros((size,size,8),dtype=np.float32);covered=np.zeros((size,size),bool);water=covered.copy()
 for vertices,surface,indices,land in meshes:
  for ti,tri in enumerate(indices):
   p=(vertices[tri][:,[0,2]]-lo)/width*size
   if p[:,0].max()<0 or p[:,1].max()<0 or p[:,0].min()>=size or p[:,1].min()>=size:continue
   x0,y0=np.maximum(np.floor(p.min(0)).astype(int),0);x1,y1=np.minimum(np.ceil(p.max(0)).astype(int)+1,size)
   if x1<=x0 or y1<=y0:continue
   xx,yy=np.meshgrid(np.arange(x0,x1)+.5,np.arange(y0,y1)+.5)
   a,b,c=p;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
   if abs(den)<1e-8:continue
   u=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den
   v=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;t=1-u-v;mask=(u>=-1e-6)&(v>=-1e-6)&(t>=-1e-6)
   attrs=np.concatenate([surface[tri,:2],vertices[tri,3:7],surface[tri,6:8]],axis=1)
   values=u[:,:,None]*attrs[0]+v[:,:,None]*attrs[1]+t[:,:,None]*attrs[2]
   f[y0:y1,x0:x1][mask]=values[mask];covered[y0:y1,x0:x1][mask]=True;water[y0:y1,x0:x1][mask]=ti*3>=land
 return f,covered,water
try:font=ImageFont.truetype('DejaVuSans.ttf',16);small=ImageFont.truetype('DejaVuSans.ttf',13);title=ImageFont.truetype('DejaVuSans.ttf',22)
except OSError:font=small=title=ImageFont.load_default()
checks=[]
for label,center,width in [('ordinary',(17,164),18),('near',(17,165),5)]:
 sheets=[];baseline_rgb=None;baseline_field=None;unchanged_pixels=0
 for meshes,marked,caption in [(old,False,'v128 input: existing green/soil mixture'),(new,True,'v129 candidate: contained mineral springs')]:
  f,mask,water=raster(meshes,center,width);rgb,poison=shade(f,marked,width);rgb[water]=[.28,.40,.43];rgb[~mask]=[.16,.18,.16]
  if not marked:baseline_rgb=rgb.copy();baseline_field=f.copy()
  else:
   unchanged=np.all(f==baseline_field,axis=2)&mask&~water
   assert np.array_equal(baseline_rgb[unchanged],rgb[unchanged]),'non-POISON fragment outputs changed'
   unchanged_pixels=int(np.count_nonzero(unchanged))
  img=Image.fromarray(np.uint8(rgb*255));d=ImageDraw.Draw(img)
  # This is the exact authoritative grid footprint, only an overlay for inspection.
  grid=img.copy();gd=ImageDraw.Draw(grid)
  for rr in range(200):
   x=float(rr)
   for q in range(299):
    z=q+rr*.5-99
    if abs(x-center[0])>width/2+.5 or abs(z-center[1])>width/2+.5:continue
    xx=(x-center[0]+width/2)/width*650;yy=(z-center[1]+width/2)/width*650;half=325/width
    gd.rectangle([xx-half,yy-half,xx+half,yy+half],outline=(190,192,162),width=1)
  panel=Image.new('RGB',(650,688),(27,31,30));panel.paste(img,(0,38));ImageDraw.Draw(panel).text((12,10),caption,fill=(233,233,220),font=font);sheets.append(panel)
  if marked:
   panel2=Image.new('RGB',(650,688),(27,31,30));panel2.paste(grid,(0,38));ImageDraw.Draw(panel2).text((12,10),'v129 with logical cell-edge guide',fill=(233,233,220),font=font);sheets.append(panel2)
   checks.append(dict(view=label,world_center=center,width=width,poison_pixels=int(np.count_nonzero(poison>.5)),pixels=650*650,unchanged_fragment_pixels=unchanged_pixels))
 sheet=Image.new('RGB',(1950,785),(17,21,20));d=ImageDraw.Draw(sheet)
 d.text((16,10),'HOST MATERIAL REVIEW ONLY | actual production mesh, emulated baseColor | NOT AN APK SCREENSHOT',font=title,fill=(231,220,171))
 d.text((16,42),f'{label}: world X/Z {center}; {width} world units across. No Filament lighting / structures / vegetation. Water is neutral diagnostic fill.',font=small,fill=(201,207,196))
 for i,p in enumerate(sheets):sheet.paste(p,(i*650,80))
 sheet.save(out/f'poison129-{label}-host-review.png')
summary=dict(status='HOST_DIAGNOSTIC_ONLY',baseline_field_version=int(oldversion),candidate_field_version=int(version),changed_tone_vertices=changed,geometry_weights_normals_uvs_shore_indices='byte-identical',views=checks,notes='Production mesh data with shader-baseColor emulation. Not rendered by Filament; not APK, device, performance, or PC-reference evidence.')
(out/'review-summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))

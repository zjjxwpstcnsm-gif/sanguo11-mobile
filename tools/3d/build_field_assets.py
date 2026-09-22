#!/usr/bin/env python3
"""CC0 original Han-inspired field assets + rigid-joint animation library.
No external service, licensed stock model, or Blender dependency. Shared S03 GLB subset.
"""
import runpy, math, json, hashlib
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
base=runpy.run_path(str(Path(__file__).with_name('build_sites.py')))
Base=base['Mesh']; OUT=ROOT/'app/src/main/assets/3d/field';OUT.mkdir(parents=True,exist_ok=True)
Base.write.__globals__['OUT']=OUT
# Eight opaque panels: masonry, tiles, timber, linen, iron, skin, foliage, earth.
palette=[(142,136,118),(70,83,82),(119,82,47),(158,136,103),(92,103,103),(195,159,126),(64,91,47),(110,96,64)]
im=Image.new('RGB',(512,64))
for y in range(64):
 for x in range(512):
  k=x//64;u=x%64;c=palette[k];n=((x*37+y*71+x*y*3)%17)-8
  seam=(y%12<2 or (u+(8 if y//12%2 else 0))%24<2) if k==0 else (u%8<2 or y%13<2) if k==1 else u%13<1 if k==2 else (u%4==0 or y%4==0) if k==3 else (y%8==0 and u%8<3) if k==4 else False
  im.putpixel((x,y),tuple(max(0,min(255,int(v*(.65 if seam else 1))+n)) for v in c))
im.save(OUT/'atlas.png',optimize=True)
class Mesh(Base):
 def face(self,points,mat=0,shade=1):
  n=len(self.p);self.p.extend(points);self.c.extend([(shade,shade,shade,1)]*len(points))
  self.uv.extend([((mat+u*.94+.03)/8,v*.94+.03) for u,v in [(0,0),(1,0),(1,1),(0,1)][:len(points)]])
  for i in range(1,len(points)-1):self.idx.extend([n,n+i,n+i+1])
 def frustum(self,x,y,z,rx,rz,h,top=.8,mat=2,n=8):
  for i in range(n):
   a=i*math.tau/n;b=(i+1)*math.tau/n
   self.face([(x+rx*math.cos(a),y,z+rz*math.sin(a)),(x+rx*math.cos(b),y,z+rz*math.sin(b)),(x+rx*top*math.cos(b),y+h,z+rz*top*math.sin(b)),(x+rx*top*math.cos(a),y+h,z+rz*top*math.sin(a))],mat,.8+.15*math.sin(a+1))
   self.face([(x,y+h,z),(x+rx*top*math.cos(a),y+h,z+rz*top*math.sin(a)),(x+rx*top*math.cos(b),y+h,z+rz*top*math.sin(b))],mat)
 def oval(self,x,y,z,rx,ry,rz,mat=0,n=8,bands=4):
  for j in range(bands):
   a=-math.pi/2+j*math.pi/bands;b=a+math.pi/bands
   for i in range(n):
    p=i*math.tau/n;q=p+math.tau/n
    self.face([(x+rx*math.cos(a)*math.cos(p),y+ry*math.sin(a),z+rz*math.cos(a)*math.sin(p)),(x+rx*math.cos(a)*math.cos(q),y+ry*math.sin(a),z+rz*math.cos(a)*math.sin(q)),(x+rx*math.cos(b)*math.cos(q),y+ry*math.sin(b),z+rz*math.cos(b)*math.sin(q)),(x+rx*math.cos(b)*math.cos(p),y+ry*math.sin(b),z+rz*math.cos(b)*math.sin(p))],mat,.84+.14*math.sin(p+1))
 def beam(self,a,b,width,mat=2):
  # Beam along arbitrary direction, with a true cross section.
  import numpy as np
  a=np.array(a);b=np.array(b);v=b-a;v=v/np.linalg.norm(v);u=np.cross(v,[0,1,0] if abs(v[1])<.9 else [1,0,0]);u=u/np.linalg.norm(u)*width/2;t=np.cross(v,u)
  p=[a+u+t,a-u+t,a-u-t,a+u-t];q=[b+u+t,b-u+t,b-u-t,b+u-t]
  self.face([tuple(x) for x in q],mat)
  for i in range(4):self.face([tuple(p[i]),tuple(p[(i+1)%4]),tuple(q[(i+1)%4]),tuple(q[i])],mat,.75+i*.06)
 def wheel(self,x,y,z,r,mat=2,lod=0):
  for i in range(8 if lod else 12):
   a=i*math.tau/(8 if lod else 12);b=(i+1)*math.tau/(8 if lod else 12)
   for xx in [x-.012,x+.012]:self.face([(xx,y,z),(xx,y+r*math.sin(a),z+r*math.cos(a)),(xx,y+r*math.sin(b),z+r*math.cos(b))],mat,.85)
   self.face([(x-.012,y+r*math.sin(a),z+r*math.cos(a)),(x+.012,y+r*math.sin(a),z+r*math.cos(a)),(x+.012,y+r*math.sin(b),z+r*math.cos(b)),(x-.012,y+r*math.sin(b),z+r*math.cos(b))],4)
 def tent(self,x,z,w=.22,d=.28,h=.22):
  self.face([(x-w/2,0,z-d/2),(x+w/2,0,z-d/2),(x,h,z-d/2)],3)
  self.face([(x-w/2,0,z+d/2),(x+w/2,0,z+d/2),(x,h,z+d/2)],3)
  self.face([(x-w/2,0,z-d/2),(x-w/2,0,z+d/2),(x,h,z+d/2),(x,h,z-d/2)],3,.8)
  self.face([(x+w/2,0,z-d/2),(x+w/2,0,z+d/2),(x,h,z+d/2),(x,h,z-d/2)],3)
 def palisade(self,lod,stone=False):
  if stone:
   for x,z,w,d in [(-.42,-.4,.84,.08),(-.42,-.4,.08,.8),(.34,-.4,.08,.8),(-.42,.32,.30,.08),(.12,.32,.3,.08)]:self.wall(x,z,w,d,lod)
  else:
   for i in range(6 if lod else 10):
    t=-.38+i*.76/(5 if lod else 9)
    for x,z in [(t,-.38),(-.38,t),(.38,t)]:self.frustum(x,0,z,.026,.026,.26,0,2,5)
 def boat(self,size=1,tower=0,lod=0):
  rings=[(-.43,.015),(-.28,.15),(.23,.15),(.45,0)]
  for (z,w),(Z,W) in zip(rings,rings[1:]):
   self.face([(-w,.12,z),(w,.12,z),(W,.12,Z),(-W,.12,Z)],2)
   for side in [-1,1]:self.face([(side*w,.12,z),(side*W,.12,Z),(side*W*.6,0,Z),(side*w*.6,0,z)],2,.75)
  if tower:self.hall(0,-.10,.23,.26,.22+tower*.06,lod)
  if tower>1:
   self.beam((0,.1,.06),(0,.65,.06),.025)
   self.face([(-.20,.32,.06),(.20,.32,.06),(.16,.59,.06),(-.18,.59,.06)],3)
  else:self.box(-.11,.13,-.12,.22,.07,.24,2)
 def engine(self,kind,lod):
  self.box(-.15,.07,-.22,.3,.07,.44,2)
  for x in [-.17,.17]:
   for z in [-.16,.16]:self.wheel(x,.085,z,.085,lod=lod)
  if kind=='RAM':
   for x in [-.12,.12]:self.beam((x,.13,-.12),(x,.35,0),.035);self.beam((x,.13,.12),(x,.35,0),.035)
   self.beam((0,.24,-.27),(0,.24,.34),.07);self.frustum(0,.2,.33,.055,.035,.09,1,4)
  elif kind=='SIEGE_TOWER':
   for y in [.13,.32,.51]:
    self.box(-.15,y,-.16,.3,.03,.32,2)
    for x in [-.13,.10]:self.box(x,y,-.13,.03,.20,.03,2);self.box(x,y,.10,.03,.20,.03,2)
   self.roof(0,0,.40,.40,.70,.10)
   if lod==0:
    for y in [.18,.24,.30,.36,.42]:self.box(-.07,y,-.24,.14,.015,.025,2)
  elif kind=='CATAPULT':
   for x in [-.12,.12]:self.beam((x,.1,-.15),(x,.34,.02),.035);self.beam((x,.1,.15),(x,.34,.02),.035)
  elif kind=='WOODEN_BEAST':
   self.oval(0,.25,0,.15,.13,.23,2,8,3);self.frustum(0,.22,.2,.06,.1,.1,.7,4)
  else:
   for x in [-.07,.06]:self.frustum(x,.14,0,.065,.09,.18,.85,3)
 def facility(self,kind,level,lod):
  if kind=='FARM':
   self.box(-.43,0,-.4,.86,.025,.8,7)
   for i in range(3+level):
    x=-.35+i*.65/(2+level);self.box(x,.025,-.33,.032,.035,.65,6)
    if lod==0:
     for z in [-.25,0,.25]:self.frustum(x,.06,z,.027,.027,.06,0,6,4)
   self.hall(.24,-.21,.18,.18,.14,lod)
  elif kind in ['MARKET','BLACK_MARKET']:
   for i in range(1+level):
    x=-.27+(i%2)*.48;z=-.23+(i//2)*.44
    self.box(x-.12,.02,z-.08,.24,.1,.16,2)
    for xx in [x-.13,x+.11]:self.box(xx,0,z,.025,.27,.025,2)
    self.face([(x-.16,.25,z-.14),(x+.16,.25,z-.14),(x+.16,.28,z+.14),(x-.16,.28,z+.14)],3 if kind=='MARKET' else 2)
    if lod==0:
     for xx in [x-.07,x+.05]:self.oval(xx,.15,z,.04,.045,.04,7,6,3)
  elif kind in ['CAMP','FORT','FORTRESS']:
   self.palisade(lod,kind=='FORTRESS');self.tent(0,0,.26,.30,.27)
   if kind!='CAMP':
    self.tent(-.22,-.19,.17,.20,.17);self.tent(.22,-.19,.17,.20,.17)
   if kind=='FORTRESS':self.hall(0,-.19,.3,.20,.32,lod)
  elif kind in ['ARROW_TOWER','CROSSBOW_TOWER','CATAPULT_TOWER']:
   self.box(-.23,0,-.23,.46,.07,.46,0)
   for x in [-.17,.13]:
    for z in [-.17,.13]:self.box(x,.07,z,.04,.52,.04,2)
   for y in [.25,.50]:self.box(-.21,y,-.21,.42,.035,.42,2)
   if kind=='CATAPULT_TOWER':
    self.beam((0,.53,-.20),(0,.82,.23),.05);self.oval(0,.82,.23,.08,.025,.08,2,6,3)
   else:
    self.roof(0,0,.53,.53,.70,.15)
    self.beam((-.22,.58,.23),(.22,.58,.23),.035,4)
    if kind=='CROSSBOW_TOWER':self.beam((-.22,.64,.23),(.22,.64,.23),.035,4)
  elif kind in ['MUSIC','DRUM']:
   self.box(-.28,0,-.24,.56,.12,.48,0)
   self.frustum(0,.12,0,.18,.14,.18,.85,2)
   if kind=='DRUM':self.frustum(0,.30,0,.17,.13,.025,1,3)
   else:
    for x in [-.18,.16]:self.box(x,.12,0,.025,.36,.025,2)
    self.beam((-.18,.47,0),(.18,.47,0),.025)
    for i in range(4):self.frustum(-.12+i*.08,.29,0,.025,.025,.1,.6,4,6)
  elif kind=='STONE_MAZE':
   for i in range(8):
    a=i*math.tau/8;self.frustum(.28*math.cos(a),0,.28*math.sin(a),.07,.05,.18+(i%3)*.03,.8,0,5)
  elif kind in ['EARTH_WALL','STONE_WALL','DAM']:
   self.frustum(0,0,0,.43,.18,.26 if kind!='DAM' else .42,.72,7 if kind=='EARTH_WALL' else 0,4)
   if kind=='STONE_WALL':
    for i in range(6):self.box(-.35+i*.13,.24,-.06,.07,.06,.12,0)
   if kind=='DAM':
    for x in [-.3,0,.3]:self.beam((x,0,.25),(x,.4,0),.065)
  elif kind.endswith('SEED') or kind.endswith('BALL'):
   n=1 if kind.startswith('FIRE') else 2 if kind.startswith('FLAME') else 3
   if kind.endswith('BALL'):
    self.oval(0,.17,0,.19,.18,.19,2,8,4)
    for z in [-.12,.12]:self.beam((-.2,.06,z),(.2,.3,z),.026,4)
   else:
    for i in range(n):self.frustum(-.12+i*.12,0,0,.075,.075,.19,.85,2)
   for i in range(n):self.beam((-.1+i*.1,.15,-.1),(-.07+i*.1,.27,-.1),.015,3)
  elif kind=='FIRE_SHIP':
   self.boat(lod=lod)
   for x in [-.06,.06]:self.frustum(x,.18,0,.055,.055,.18,.8,2)
  elif kind=='GRANARY':
   for x in [-.22,.22]:self.frustum(x,.04,0,.17,.20,.31,.9,2);self.roof(x,0,.40,.46,.35,.18)
  elif kind=='BRONZE_TERRACE':
   for i in range(3):self.box(-.38+i*.07,i*.09,-.34+i*.06,.76-i*.14,.09,.68-i*.12,0)
   self.hall(0,0,.37,.30,.48,lod);self.oval(0,.76,0,.035,.04,.1,4,6,3)
   self.beam((-.14,.75,0),(.14,.75,0),.025,4)
  elif kind=='SHIPYARD':
   self.hall(-.22,-.18,.30,.3,.25,lod);self.boat(lod=lod)
   for x in [-.32,.30]:self.box(x,0,.2,.025,.4,.025,2)
   self.beam((-.32,.4,.2),(.32,.4,.2),.03)
  else:
   self.hall(-.08,-.08,.45,.38,.23+level*.065,lod)
   if level>1:self.hall(.25,.22,.23,.25,.19,lod)
   if level>2:self.wall(-.43,-.40,.85,.045,lod)
   if kind=='BARRACKS':self.tent(.24,.20,.23,.24,.18)
   elif kind=='STABLE':
    self.oval(.22,.15,.2,.065,.055,.13,2,6,3)
    for x in [.18,.26]:self.box(x,.03,.12,.02,.13,.02,2);self.box(x,.03,.26,.02,.13,.02,2)
   elif kind=='SMITH':
    self.frustum(.25,0,.20,.095,.095,.23,.75,0);self.frustum(.25,.23,.2,.045,.045,.3,.8,0)
    self.box(-.20,.03,.23,.14,.1,.12,4)
   elif kind=='MINT':
    for x in [-.22,.1]:self.frustum(x,.02,.24,.09,.09,.06,1,4)
   elif kind=='WORKSHOP':self.engine('CATAPULT',lod)
  return self

report=[]
def save(mesh,name):
 r=mesh.write(name);r['texture']=[512,64];report.append(r)

# Names and upgradeability are checked against Java enums by the asset contract test.
domestic=['MARKET','FARM','BARRACKS','SMITH','MINT','GRANARY','STABLE','BLACK_MARKET','WORKSHOP','SHIPYARD','BRONZE_TERRACE']
military=['CAMP','ARROW_TOWER','MUSIC','FIRE_SEED','FORT','FORTRESS','CROSSBOW_TOWER','CATAPULT_TOWER','DRUM','STONE_MAZE','EARTH_WALL','STONE_WALL','FIRE_BALL','FLAME_SEED','FLAME_BALL','INFERNO_SEED','INFERNO_BALL','FIRE_SHIP','DAM']
for family,names in [('domestic',domestic),('military',military)]:
 for kind in names:
  for level in (range(1,4) if kind in ['MARKET','FARM','BARRACKS','SMITH','STABLE'] else [1]):
   for lod in range(3):save(Mesh().facility(kind,level,lod),f'{family}-{kind}-{level}-lod{lod}')
for lod in range(2):
 m=Mesh();m.frustum(0,0,0,.027,.027,.32,.65,2,5)
 for y,r in [(.20,.16),(.34,.13),(.47,.09)][:3-lod]:m.frustum(0,y,0,r,r,.24,0,6,7 if lod==0 else 5)
 save(m,'tree-lod'+str(lod))
# Dedicated construction scaffold and opaque fire tongue modules (no alpha overdraw).
m=Mesh()
for x in [-.4,.4]:
 for z in [-.4,.4]:m.box(x,0,z,.025,.50,.025,2)
for y in [.18,.38]:
 for z in [-.4,.4]:m.beam((-.4,y,z),(.4,y,z),.025)
save(m,'scaffold')
m=Mesh()
for x,z,h in [(-.15,0,.3),(.1,.13,.4),(.05,-.15,.28)]:
 start=len(m.c);m.frustum(x,0,z,.09,.07,h,0,3,5)
 for i in range(start,len(m.c)):m.c[i]=(1,.40,.08,1)
save(m,'fire')

# Modules are rigid-jointed rather than skeletal-skinned: shared meshes + authored
# rotation keyframes. Each vertex belongs to exactly one pivoted part, no root motion.
rigs={}
def humanoid(m,bone,lod,cavalry=False,weapon='SWORD'):
 oy=.23 if cavalry else 0
 def part(name,parent,pivot,fn):bone(name,parent,(pivot[0],pivot[1]+oy,pivot[2]),fn)
 def body():
  m.frustum(0,.16+oy,0,.065,.036,.14,.80,4,8);m.frustum(0,.13+oy,0,.070,.044,.045,.85,3,8)
  m.frustum(0,.30+oy,0,.022,.021,.025,1,5,6)
  m.oval(0,.354+oy,0,.042,.05,.037,5,8,4)
  m.oval(0,.387+oy,-.003,.047,.026,.042,4,8,3)
  m.frustum(0,.403+oy,0,.012,.012,.045,0,3,5)
  if lod==0:
   for yy in [.20,.23,.26]:
    for xx in [-.032,0,.032]:m.box(xx-.012,yy,.033,.024,.021,.006,4)
   for xx in [-.015,.015]:m.box(xx-.005,.36+oy,.034,.008,.006,.006,2)
 part('body',-1,(0,0,0),body)
 for side in [-1,1]:
  suffix='L' if side<0 else 'R';x=side*.07
  part('arm'+suffix,'body',(x,.285,0),lambda x=x:m.frustum(x-.006,.205+oy,0,.022,.022,.08,.85,3,6))
  def hand(x=x,side=side):
   m.frustum(x,.14+oy,0,.018,.018,.07,.95,5,6)
   if side==1:
    if weapon in ['SPEAR','HALBERD','CAVALRY']:
     m.beam((x,.05+oy,.035),(x,.51+oy,.035),.009)
     m.frustum(x,.50+oy,.035,.018,.012,.065,0,4,5)
     if weapon=='HALBERD':m.face([(x,.48+oy,.035),(x+.045,.49+oy,.035),(x+.035,.55+oy,.035),(x,.53+oy,.035)],4)
    elif weapon=='CROSSBOW':
     m.beam((x-.07,.16+oy,.03),(x+.07,.16+oy,.03),.013,2);m.beam((x,.16+oy,-.03),(x,.16+oy,.10),.015,2)
    else:m.beam((x,.16+oy,.02),(x,.34+oy,.04),.016,4)
   elif weapon in ['SWORD','HALBERD']:
    m.box(x-.04,.15+oy,.02,.065,.11,.014,2)
  part('hand'+suffix,'arm'+suffix,(x,.21,0),hand)
  x=side*.029
  part('leg'+suffix,'body',(x,.16,0),lambda x=x:m.frustum(x,.075+oy,0,.024,.026,.085,.9,3,6))
  def shin(x=x):m.frustum(x,.017+oy,0,.019,.02,.06,1,4,6);m.box(x-.022,oy,.0,.044,.026,.054,2)
  part('shin'+suffix,'leg'+suffix,(x,.08,0),shin)

weapons=['SPEAR','HALBERD','CROSSBOW','CAVALRY','SWORD','RAM','SIEGE_TOWER','WOODEN_BEAST','CATAPULT','transport','BOAT','TOWER_SHIP','WARSHIP']
for kind in weapons:
 for lod in range(2):
  m=Mesh();parts=[];ids={}
  def bone(name,parent,pivot,fn):
   start=len(m.p);fn();ids[name]=len(parts)
   parts.append({'name':name,'parent':-1 if parent==-1 else ids[parent],'pivot':pivot,'first':start,'count':len(m.p)-start})
  infantry=kind in weapons[:5]
  if infantry:
   humanoid(m,bone,lod,kind=='CAVALRY',kind)
   if kind=='CAVALRY':
    def horse():
     m.oval(0,.20,0,.067,.085,.16,2,8,4);m.oval(0,.28,.13,.045,.09,.055,2,8,4);m.oval(0,.33,.18,.04,.04,.065,2,8,3)
     m.box(-.05,.26,-.055,.1,.035,.11,3);m.beam((0,.21,-.15),(0,.12,-.23),.02)
    bone('horse',-1,(0,0,0),horse)
    for i,(x,z) in enumerate([(-.04,-.1),(.04,-.1),(-.04,.1),(.04,.1)]):
     def leg(x=x,z=z):m.frustum(x,.015,z,.017,.018,.16,.85,2,6);m.box(x-.02,0,z-.015,.04,.025,.055,4)
     bone('horseLeg'+str(i),'horse',(x,.16,z),leg)
  elif kind in ['BOAT','TOWER_SHIP','WARSHIP']:
   bone('hull',-1,(0,0,0),lambda:m.boat(tower=0 if kind=='BOAT' else 1 if kind=='TOWER_SHIP' else 2,lod=lod))
   for side in [-1,1]:
    def oars(side=side):
     for z in [-.22,-.07,.08,.23]:m.beam((side*.13,.10,z),(side*.32,.03,z-.06),.014)
    bone('oarsL' if side<0 else 'oarsR','hull',(side*.13,.1,0),oars)
  else:
   # Wheels and working arm are separate moving modules, chassis is shared.
   bone('chassis',-1,(0,0,0),lambda:m.engine(kind,lod))
   for x in [-.185,.185]:
    def spokes(x=x):
     for z in [-.16,.16]:
      m.beam((x,.015,z),(x,.155,z),.015,4);m.beam((x,.085,z-.07),(x,.085,z+.07),.015,4)
    bone('wheelL' if x<0 else 'wheelR','chassis',(x,.085,0),spokes)
   if kind=='CATAPULT':
    def arm():m.beam((0,.16,-.2),(0,.52,.22),.035);m.oval(0,.52,.22,.065,.025,.07,2,6,3)
    bone('lever','chassis',(0,.30,0),arm)
  name='unit-'+kind+'-lod'+str(lod);save(m,name)
  rigs[name]={'parts':parts,'formation':8 if infantry and kind!='CAVALRY' else 4 if kind=='CAVALRY' else 1,'frames':12}
(OUT/'rigs.json').write_text(json.dumps({'version':1,'rigs':rigs},separators=(',',':'))+'\n')
# Actual keyframes: radians about local X, Y, Z. Clip phase comes from the shared
# journal player or each instance's deterministic idle clock, never gameplay RNG.
clips={}
for clip in ['idle','walk','turn','prepare','attack','hit','defeat','enter']:
 frames=[]
 for frame in range(12):
  t=frame/11;wave=math.sin(t*math.tau);poses={}
  def rot(bone,x=0,y=0,z=0):poses[bone]=[x,y,z]
  if clip=='walk':
   for i,s in enumerate(['L','R']):
    v=wave*(1 if i==0 else -1);rot('arm'+s,v*.55);rot('leg'+s,-v*.65);rot('shin'+s,max(0,v)*.8)
   for i in range(4):rot('horseLeg'+str(i),math.sin(t*math.tau+(i%2)*math.pi)*.65)
   rot('wheelL',t*math.tau);rot('wheelR',t*math.tau)
   rot('oarsL',0,.35*wave);rot('oarsR',0,-.35*wave)
  elif clip in ['prepare','attack']:
   a=t if clip=='prepare' else (math.sin(t*math.pi))
   rot('armR',-a*1.35);rot('handR',-a*.4);rot('armL',-a*.35);rot('body',0,a*.22);rot('lever',-a*.9);rot('hull',0,a*.04)
  elif clip=='hit':rot('body',-.3*math.sin(t*math.pi));rot('horse',-.08*math.sin(t*math.pi));rot('hull',0,0,.10*math.sin(t*math.pi));rot('chassis',-.07*math.sin(t*math.pi))
  elif clip=='defeat':rot('body',0,0,t*1.45);rot('horse',0,0,t*.8);rot('hull',0,0,t*.55);rot('chassis',0,0,t*.7)
  elif clip=='turn':rot('body',0,.15*wave);rot('horse',0,.06*wave)
  else:rot('armL',.025*wave);rot('armR',-.025*wave);rot('hull',0,0,.015*wave)
  frames.append(poses)
 clips[clip]=frames
(OUT/'clips.json').write_text(json.dumps({'version':1,'fps':12,'clips':clips},separators=(',',':'))+'\n')
(ROOT/'docs/3d/field-assets.json').write_text(json.dumps({'license':'CC0-1.0 original geometry, texture and animation keyframes','generator':'tools/3d/build_field_assets.py','axes':'+Y up, +Z forward; one tile = 1 world span','material':'shared opaque atlas, precompiled S03 material','rig':'rigid-jointed modules, GLB rest geometry + rigs.json pivot/ranges + clips.json keyframes; no skinning or root motion','clips':list(clips),'assets':report},indent=2)+'\n')
print('Generated',len(report),'field GLBs;',sum(a['bytes'] for a in report),'bytes')

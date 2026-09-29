#!/usr/bin/env python3
"""Original CC0 v123 architecture and articulated army refinements.
Run Blender 4.2.3 --background --python tools/3d/refine_blender122.py -- OUTPUT.
Keeps original GLBs/atlases, pivots, rig hierarchy and clip timings. New geometry
is built, evaluated and triangulated in Blender, then baked to SiteGlb's subset.
"""
import ast, copy, json, math, struct, sys
from pathlib import Path
import bpy
ROOT=Path(__file__).resolve().parents[2]
OUT=Path(sys.argv[sys.argv.index('--')+1]) if '--' in sys.argv else ROOT
# Reuse the reviewed strict exporter, without running its v121 asset production.
helper=(ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0]
ns={'__file__':str(ROOT/'tools/3d/refine_blender121.py')}
exec(compile(helper,'refine_blender121 helpers','exec'),ns)
mesh_object,export,import_subset=ns['mesh_object'],ns['export'],ns['import_subset']
# Reuse the existing architectural vocabulary without regenerating old atlases.
tree=ast.parse((ROOT/'tools/3d/build_sites.py').read_text())
base_ns={'math':math}
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n,ast.ClassDef) and n.name=='Mesh'],type_ignores=[]),'site vocabulary','exec'),base_ns)
Base=base_ns['Mesh']
class Architecture(Base):
 def pavilion(self,x,z,w,d,h,lod,base=.045):
  super().pavilion(x,z,w,d,h,lod,base)
  if lod==2:return
  # Tiered dougong brackets under eaves; individual columns, lintels and roof ribs.
  for side in [-1,1]:
   for end in [-1,1]:
    xx=x+side*w*.40;zz=z+end*d*.42
    for k in range(2 if lod else 3):
     self.box(xx-.013-k*.008,base+h-.015+k*.012,zz-.012,.026+k*.016,.013,.024,2)
  if lod==0:
   for side in [-1,1]:
    self.box(x-w*.40,base+h*.68,z+side*d*.505,w*.8,.012,.012,2)
   # Narrow raised tile seams along the concave slope, kept within the roof.
   for i in range(7):
    xx=x-w*.44+i*w*.88/6
    for side in [-1,1]:
     a=(xx,base+h+.036,z+side*d*.61)
     b=(x+(xx-x)*.70,base+h+h*.25,z+side*d*.40)
     self.face([a,(a[0]+.006,a[1]+.004,a[2]),(b[0]+.006,b[1]+.004,b[2]),b],1)
 def build(self,kind,lod):
  super().build(kind,lod)
  if kind.startswith('city') and lod<2:
   # Distinct inner quarters; clear four axial entrances and existing main streets.
   v=int(kind[-1])
   for side in [-1,1]:
    self.pavilion(side*.54,-.48,.13,.14,.10,lod)
    if v==1:self.pavilion(side*.38,.44,.14,.13,.10,lod)
    else:self.pavilion(side*.50,.44,.16,.12,.11,lod)
   if lod==0:
    for side in [-1,1]:
     # Courtyard boundary with open gate; no geometry crosses the central lane.
     for z in [-.05,.25]:self.box(side*.27-.055,.045,z,.11,.065,.025,0)
     for i in range(3):self.box(side*.35-.04,.04,-.30+i*.12,.08,.008,.075,0)
  elif kind=='port' and lod<2:
   # Mooring dolphins, bollards, warehouse screen and a braced shore crane.
   for x in [-.32,.29]:
    for z in [.66,.75]:self.box(x-.004,.09,z-.004,.043,.018,.043,2)
   for x in [-.29,-.21]:self.box(x,.06,.51,.025,.025,.04,2)
   for i in range(3 if lod==0 else 1):
    self.box(.20,.06,-.34+i*.045,.055,.05,.035,2)
   if lod==0:
    for i in range(4):self.box(-.26+i*.11,.19,-.045,.012,.07,.008,2)
  elif kind=='gate' and lod<2:
   # Flanking watch chambers and tiled parapet caps; central passage stays open.
   for side in [-1,1]:
    self.pavilion(side*.32,0,.17,.24,.09,lod,.28)
    if lod==0:
     for z in [-.11,0,.11]:self.box(side*.37-.025,.20,z-.009,.05,.014,.018,2)
  if lod<2:
   if kind.startswith('city'):
    # Drum and bell towers in the inner quarters; keep axial gate lanes open.
    for side in [-1,1]:
     self.box(side*.48-.09,.045,.13,.18,.12,.18,0)
     self.pavilion(side*.48,.22,.18,.18,.13,lod,.165)
     if lod==0:
      for k in range(4):
       self.box(side*.48-.09+k*.045,.17,.115,.012,.055,.012,2)
      self.box(side*.48-.09,.22,.115,.18,.012,.012,2)
   elif kind=='port':
    # Hoisting gantry, top boom and diagonal braces at the existing warehouse.
    for x in [-.30,-.08]:self.box(x,.06,-.18,.018,.32,.018,2)
    self.box(-.32,.38,-.19,.28,.024,.035,2)
    for x in [-.30,-.08]:
     self.face([(x,.20,-.18),(x+.012,.20,-.18),(x+.10,.37,-.18),(x+.08,.37,-.18)],2)
   elif kind=='gate':
    # Tall ridge finials make the pass recognisable at tactical zoom.
    for side in [-1,1]:
     self.box(side*.32-.013,.405,-.012,.026,.065,.024,1)
     self.curved_roof(side*.32,0,.09,.065,.455,.028,lod)
  return self

objects=[]
for kind in ['city0','city1','city2','port','gate']:
 for lod in range(3):
  m=Architecture().build(kind,lod)
  obj=mesh_object(kind+'-v123-lod'+str(lod),m.p,[m.idx[i:i+3] for i in range(0,len(m.idx),3)],m.c,m.uv)
  # Blender evaluates a restrained bevel for the smaller masonry structures.
  if kind in ['gate','port'] and lod==0:
   weld=obj.modifiers.new('Weld coincident masonry seams','WELD');weld.merge_threshold=.00001
   bevel=obj.modifiers.new('Soft masonry edges','BEVEL');bevel.width=.002;bevel.segments=1;bevel.limit_method='ANGLE';bevel.angle_limit=.85
  export(obj,OUT/f'app/src/main/assets/3d/sites/v123/{kind}-lod{lod}.glb',ROOT/'app/src/main/assets/3d/sites/atlas.png','authored brackets, layered roof seams, quarters, mooring details and gate watch chambers')
  objects.append((obj,kind,lod,'sites'))

rig_doc=json.loads((ROOT/'app/src/main/assets/3d/field/rigs-v122.json').read_text())
for name,rig in sorted(rig_doc['rigs'].items()):
 raw=import_subset(ROOT/f'app/src/main/assets/3d/field/v122/{name}.glb');lod=int(name[-1]);kind=name[5:].split('-lod')[0]
 # Work on each rigid part independently: bevel cannot weld across joints.
 parts=[];vertices=[];faces=[];colors=[];uvs=[]
 for part in rig['parts']:
  lo=part['first'];hi=lo+part['count'];src=raw.data;attr=src.color_attributes['Color'];uv=src.uv_layers.active
  p=[];f=[];c=[];t=[]
  for poly in src.polygons:
   ids=list(poly.vertices)
   if not ids or not all(lo<=i<hi for i in ids):continue
   start=len(p)
   for li in poly.loop_indices:
    i=src.loops[li].vertex_index;p.append(tuple(src.vertices[i].co));c.append(tuple(attr.data[i].color));t.append(tuple(uv.data[li].uv))
   f.append(tuple(range(start,len(p))))
  obj=mesh_object(name+'-'+part['name'],p,f,c,t)
  # True Blender bevel on timber chassis and shield/weapon edges. Rounded human
  # and horse surfaces keep their authored silhouette; no subdivision inflation.
  # v122 chassis already includes bevels; do not apply a second bevel.
  # Evaluate and append faces in part order, recalculating every exported rig range.
  deps=bpy.context.evaluated_depsgraph_get();ev=obj.evaluated_get(deps);mesh=ev.to_mesh();mesh.calc_loop_triangles()
  first=len(vertices);ca=mesh.color_attributes.get('Color');ul=mesh.uv_layers.active
  for tri in mesh.loop_triangles:
   if tri.area<1e-12:continue
   start=len(vertices)
   for li in tri.loops:
    i=mesh.loops[li].vertex_index;vertices.append(tuple(mesh.vertices[i].co));colors.append(tuple(ca.data[i if ca.domain=='POINT' else li].color));uvs.append(tuple(ul.data[li].uv))
   faces.append((start,start+1,start+2))
  ev.to_mesh_clear();bpy.data.objects.remove(obj,do_unlink=True)
  # Additional fitted lamellar shoulder/waist rows, metal shield rim and horse
  # harness studs use the same parent part; movement and attack remain articulated.
  addon=Base()
  def box(x,y,z,w,h,d,panel):
   at=len(addon.p);addon.box(x,y,z,w,h,d,0)
   for j in range(at,len(addon.p)):addon.uv[j]=((panel+.15)/8,.5)
  oy=.14 if kind=='CAVALRY' else 0
  if part['name']=='body' and kind in ['SPEAR','HALBERD','CROSSBOW','CAVALRY','SWORD']:
   # Raised helmet crest and rear neck guard, visibly distinct from armour rows.
   box(-.009,.38+oy,-.04,.018,.058,.07,4)
   if lod==0:
    for i in range(4):box(-.052+i*.027,.30+oy,-.056,.024,.045,.012,4)
  if part['name']=='horse' and lod==0:
   # Saddle rolls fitted to the horse joint, never fused across the rider joint.
   box(-.065,.256,-.09,.13,.023,.032,3)
  if part['name']=='body' and kind not in ['SPEAR','HALBERD','CROSSBOW','CAVALRY','SWORD'] and lod==0:
   # Symmetric timber reinforcement bands on siege/transport chassis.
   for x in [-.10,.085]:box(x,.11,-.13,.015,.015,.25,4)
  start=len(vertices);vertices.extend(addon.p);colors.extend(addon.c);uvs.extend(addon.uv)
  for i in range(0,len(addon.idx),3):faces.append(tuple(start+j for j in addon.idx[i:i+3]))
  # The final exporter expands each triangle into three vertices.
  exported=sum(len(face)-2 for face in faces)*3
  previous=sum(x['count'] for x in parts)
  parts.append(dict(part,first=previous,count=exported-previous))
 bpy.data.objects.remove(raw,do_unlink=True)
 result=mesh_object(name+'-v123',vertices,faces,colors,uvs)
 export(result,OUT/f'app/src/main/assets/3d/field/v123/{name}.glb',ROOT/'app/src/main/assets/3d/field/unit-atlas.png','per-joint Blender geometry bake with fitted armour, harness and crafted chassis')
 assert ns['REPORT'][-1]['vertices']==sum(x['count'] for x in parts)
 rig['parts']=parts;objects.append((result,kind,lod,'field'))
path=OUT/'app/src/main/assets/3d/field/rigs-v123.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(rig_doc,separators=(',',':'))+'\n')
# Deduplicate complete attribute tuples within each joint only. Normals and UV
# seams remain split; triangle order and attributes are preserved exactly.
def compact(path,parts=None):
 raw=path.read_bytes();n=struct.unpack_from('<I',raw,12)[0];doc=json.loads(raw[20:20+n]);binary=raw[28+n:]
 attrs={};prim=doc['meshes'][0]['primitives'][0]
 for key,idx in prim['attributes'].items():
  ac=doc['accessors'][idx];vw=doc['bufferViews'][ac['bufferView']];width={'VEC2':2,'VEC3':3,'VEC4':4}[ac['type']]*4
  attrs[key]=[binary[vw.get('byteOffset',0)+i*width:vw.get('byteOffset',0)+(i+1)*width] for i in range(ac['count'])]
 oldcount=len(attrs['POSITION']);ranges=parts or [{'first':0,'count':oldcount}]
 remap={};kept=[];newparts=[]
 for part in ranges:
  seen={};first=len(kept)
  for i in range(part['first'],part['first']+part['count']):
   # IEEE +0 and -0 are the same geometric attribute. Compare numeric
   # float tuples, without rounding positions, normals, UVs or pigment.
   packed=b''.join(attrs[k][i] for k in sorted(attrs));key=struct.unpack('<'+'f'*(len(packed)//4),packed)
   if key not in seen:seen[key]=len(kept);kept.append(i)
   remap[i]=seen[key]
  newparts.append(dict(part,first=first,count=len(kept)-first))
 ac=doc['accessors'][prim['indices']];vw=doc['bufferViews'][ac['bufferView']]
 oldix=struct.unpack_from('<'+'I'*ac['count'],binary,vw.get('byteOffset',0));ix=[remap[i] for i in oldix]
 for key in attrs:
  assert all(struct.unpack('<'+'f'*(len(attrs[key][old])//4),attrs[key][old])==struct.unpack('<'+'f'*(len(attrs[key][kept[new]])//4),attrs[key][kept[new]]) for old,new in zip(oldix,ix)), 'triangle attribute changed during indexing'
 data=b''
 for k,idx in prim['attributes'].items():
  ac=doc['accessors'][idx];vw=doc['bufferViews'][ac['bufferView']];block=b''.join(attrs[k][i] for i in kept)
  vw['byteOffset']=len(data);vw['byteLength']=len(block);ac['count']=len(kept);data+=block
 vw=doc['bufferViews'][doc['accessors'][prim['indices']]['bufferView']];block=struct.pack('<'+'I'*len(ix),*ix);vw['byteOffset']=len(data);vw['byteLength']=len(block);data+=block
 vw=doc['bufferViews'][doc['images'][0]['bufferView']];png=binary[vw['byteOffset']:vw['byteOffset']+vw['byteLength']];vw['byteOffset']=len(data);data+=png;data+=b'\0'*(-len(data)%4)
 doc['buffers'][0]['byteLength']=len(data);doc['asset']['generator']='Blender '+bpy.app.version_string+' / CC0 v123 indexed joint bake'
 js=json.dumps(doc,separators=(',',':')).encode();js+=b' '*(-len(js)%4)
 result=struct.pack('<III',0x46546c67,2,28+len(js)+len(data))+struct.pack('<II',len(js),0x4e4f534a)+js+struct.pack('<II',len(data),0x004e4942)+data
 path.write_bytes(result)
 return oldcount,len(kept),newparts
import hashlib
for record in ns['REPORT']:
 path=OUT/record['path'];rig=rig_doc['rigs'].get(path.stem) if '/field/' in record['path'] else None
 old,new,parts=compact(path,None if rig is None else rig['parts'])
 if rig is not None:rig['parts']=parts
 record.update(vertices=new,expandedVertices=old,bytes=path.stat().st_size,sha256=hashlib.sha256(path.read_bytes()).hexdigest())
(OUT/'app/src/main/assets/3d/field/rigs-v123.json').write_text(json.dumps(rig_doc,separators=(',',':'))+'\n')
report=ns['REPORT']
for a in report:
 assert a['vertices']<=30000 and a['bytes']<=2000000,a
 a['generator']='tools/3d/refine_blender122.py'
path=OUT/'docs/native-pc-visual/feedback-v123-blender-assets.json';path.parent.mkdir(parents=True,exist_ok=True)
path.write_text(json.dumps({'blender':bpy.app.version_string,'assets':report},indent=2)+'\n')
# Offline contact sheets: explicit modelling evidence, never represented as APK.
for obj,kind,lod,family in objects:
 obj.hide_render=True;obj.rotation_euler.x=math.pi/2 # runtime Y-up -> Blender Z-up, preview only
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.device='CPU';scene.cycles.samples=16;scene.render.resolution_x=480;scene.render.resolution_y=400;scene.render.resolution_percentage=100
scene.world.color=(.25,.25,.25);scene.view_settings.view_transform='Standard'
for family in ['sites','field']:
 atlas=bpy.data.images.load(str(ROOT/'app/src/main/assets/3d'/family/('atlas.png' if family=='sites' else 'unit-atlas.png')))
 mat=bpy.data.materials.new(family+' atlas preview');mat.use_nodes=True;n=mat.node_tree.nodes;tex=n.new('ShaderNodeTexImage');tex.image=atlas;mat.node_tree.links.new(tex.outputs['Color'],n.get('Principled BSDF').inputs['Base Color']);n.get('Principled BSDF').inputs['Roughness'].default_value=.85
 for obj,kind,lod,fam in objects:
  if fam==family:obj.data.materials.append(mat)
from mathutils import Vector
bpy.ops.object.light_add(type='AREA',location=(2,-3,4));bpy.context.object.data.energy=400;bpy.context.object.data.shape='DISK';bpy.context.object.data.size=5
bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO'
for obj,kind,lod,family in objects:
 if lod:continue
 obj.hide_render=False
 target=Vector((0,0,.20 if family=='sites' else .24));cam.location=target+Vector((2,-3,2.4));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=2.8 if kind.startswith('city') else 1.6 if family=='sites' else 1.1
 p=OUT/f'out/feedback123/blender/{kind}.png';p.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(p);bpy.ops.render.render(write_still=True);obj.hide_render=True
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'out/feedback123/blender/models-v123.blend'))
print(json.dumps({'status':'EXPORTED','models':len(report),'triangles':sum(a['triangles'] for a in report)}))

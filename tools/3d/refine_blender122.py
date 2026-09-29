#!/usr/bin/env python3
"""Original CC0 v122 architecture and articulated army refinements.
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
  return self

objects=[]
for kind in ['city0','city1','city2','port','gate']:
 for lod in range(3):
  m=Architecture().build(kind,lod)
  obj=mesh_object(kind+'-v122-lod'+str(lod),m.p,[m.idx[i:i+3] for i in range(0,len(m.idx),3)],m.c,m.uv)
  # Blender evaluates a restrained bevel for the smaller masonry structures.
  if kind in ['gate','port'] and lod==0:
   weld=obj.modifiers.new('Weld coincident masonry seams','WELD');weld.merge_threshold=.00001
   bevel=obj.modifiers.new('Soft masonry edges','BEVEL');bevel.width=.002;bevel.segments=1;bevel.limit_method='ANGLE';bevel.angle_limit=.85
  export(obj,OUT/f'app/src/main/assets/3d/sites/v122/{kind}-lod{lod}.glb',ROOT/'app/src/main/assets/3d/sites/atlas.png','authored brackets, layered roof seams, quarters, mooring details and gate watch chambers')
  objects.append((obj,kind,lod,'sites'))

rig_doc=json.loads((ROOT/'app/src/main/assets/3d/field/rigs.json').read_text())
for name,rig in sorted(rig_doc['rigs'].items()):
 raw=import_subset(ROOT/f'app/src/main/assets/3d/field/{name}.glb');lod=int(name[-1]);kind=name[5:].split('-lod')[0]
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
  if lod==0 and (part['name'].startswith('wheel') or kind not in ['SPEAR','HALBERD','CROSSBOW','CAVALRY','SWORD']):
   weld=obj.modifiers.new('Weld this rigid part only','WELD');weld.merge_threshold=.000005
   bevel=obj.modifiers.new('Crafted edge','BEVEL');bevel.width=.0009;bevel.segments=1;bevel.limit_method='ANGLE';bevel.angle_limit=1.0
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
   for side in [-1,1]:
    for row in range(3 if lod==0 else 2):box(side*.058-.018,.274+oy-row*.012,.018,.036,.010,.021,4)
   if lod==0:
    for i in range(5):box(-.05+i*.021,.146+oy,.038,.016,.037,.004,4)
    box(-.012,.181+oy,.041,.024,.013,.005,4)
  if part['name']=='horse' and lod==0:
   for side in [-1,1]:
    for z in [-.08,.04]:box(side*.065-.003,.215,z,.006,.07,.01,3)
  if part['name']=='body' and kind in ['RAM','CATAPULT','transport'] and lod==0:
   for x in [-.13,.12]:
    for z in [-.19,.17]:box(x,.115,z,.015,.01,.015,4)
  start=len(vertices);vertices.extend(addon.p);colors.extend(addon.c);uvs.extend(addon.uv)
  for i in range(0,len(addon.idx),3):faces.append(tuple(start+j for j in addon.idx[i:i+3]))
  # The final exporter expands each triangle into three vertices.
  exported=sum(len(face)-2 for face in faces)*3
  previous=sum(x['count'] for x in parts)
  parts.append(dict(part,first=previous,count=exported-previous))
 bpy.data.objects.remove(raw,do_unlink=True)
 result=mesh_object(name+'-v122',vertices,faces,colors,uvs)
 export(result,OUT/f'app/src/main/assets/3d/field/v122/{name}.glb',ROOT/'app/src/main/assets/3d/field/unit-atlas.png','per-joint Blender geometry bake with fitted armour, harness and crafted chassis')
 assert ns['REPORT'][-1]['vertices']==sum(x['count'] for x in parts)
 rig['parts']=parts;objects.append((result,kind,lod,'field'))
path=OUT/'app/src/main/assets/3d/field/rigs-v122.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(rig_doc,separators=(',',':'))+'\n')
report=ns['REPORT']
for a in report:
 assert a['vertices']<=30000 and a['bytes']<=2000000,a
 a['generator']='tools/3d/refine_blender122.py'
path=OUT/'docs/native-pc-visual/feedback-v122-blender-assets.json';path.parent.mkdir(parents=True,exist_ok=True)
path.write_text(json.dumps({'blender':bpy.app.version_string,'assets':report},indent=2)+'\n')
# Offline contact sheets: explicit modelling evidence, never represented as APK.
for obj,kind,lod,family in objects:obj.hide_render=True
scene=bpy.context.scene;scene.render.engine='BLENDER_EEVEE_NEXT';scene.render.resolution_x=480;scene.render.resolution_y=400;scene.render.resolution_percentage=100
scene.world.color=(.25,.25,.25);scene.view_settings.view_transform='Standard'
for family in ['sites','field']:
 atlas=bpy.data.images.load(str(ROOT/'app/src/main/assets/3d'/family/('atlas.png' if family=='sites' else 'unit-atlas.png')))
 mat=bpy.data.materials.new(family+' atlas preview');mat.use_nodes=True;n=mat.node_tree.nodes;tex=n.new('ShaderNodeTexImage');tex.image=atlas;mat.node_tree.links.new(tex.outputs['Color'],n.get('Principled BSDF').inputs['Base Color']);n.get('Principled BSDF').inputs['Roughness'].default_value=.85
 for obj,kind,lod,fam in objects:
  if fam==family:obj.data.materials.append(mat)
from mathutils import Vector
bpy.ops.object.light_add(type='AREA',location=(2,4,3));bpy.context.object.data.energy=400;bpy.context.object.data.shape='DISK';bpy.context.object.data.size=5
bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO'
for obj,kind,lod,family in objects:
 if lod:continue
 obj.hide_render=False
 target=Vector((0,.20 if family=='sites' else .24,0));cam.location=target+Vector((2,2.4,3));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=2.8 if kind.startswith('city') else 1.6 if family=='sites' else 1.1
 p=OUT/f'out/feedback122/blender/{kind}.png';p.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(p);bpy.ops.render.render(write_still=True);obj.hide_render=True
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'out/feedback122/blender/models-v122.blend'))
print(json.dumps({'status':'EXPORTED','models':len(report),'triangles':sum(a['triangles'] for a in report)}))

#!/usr/bin/env python3
"""Original CC0 resource refinement. Run with Blender 4.2.3 --background --python.
Blender constructs/triangulates meshes and bevels gate masonry. The exporter bakes
its evaluated geometry to this project's strict single-primitive GLB subset.
No PC geometry, external models, textures, paid service or network at generation.
"""
import bpy, math, json, struct, hashlib, sys
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
OUT=Path(args[0]) if args else ROOT
ASSETS=OUT/'app/src/main/assets/3d'
REPORT=[]
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)

def mesh_object(name,vertices,faces,colors=None,uvs=None):
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(vertices,[],faces);mesh.update()
    obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj)
    color=mesh.color_attributes.new(name='Color',type='FLOAT_COLOR',domain='POINT')
    for i,v in enumerate(mesh.vertices):color.data[i].color=colors[i] if colors else (1,1,1,1)
    uv=mesh.uv_layers.new(name='UVMap')
    for loop in mesh.loops:
        v=mesh.vertices[loop.vertex_index].co
        uv.data[loop.index].uv=uvs[loop.vertex_index] if uvs else ((.03+(v.x+.5)*.94)/8,.03+(v.y%1)*.94)
    return obj

def export(obj,path,atlas,source):
    bpy.context.view_layer.objects.active=obj
    deps=bpy.context.evaluated_depsgraph_get();evaluated=obj.evaluated_get(deps);m=evaluated.to_mesh();m.calc_loop_triangles()
    pos=[];colors=[];uv=[];normals=[];indices=[]
    attr=m.color_attributes.get('Color');layer=m.uv_layers.active
    for tri in m.loop_triangles:
        if tri.area<1e-12:continue # Discard zero-area faces from welded modular seams.
        for li in tri.loops:
            vi=m.loops[li].vertex_index;v=m.vertices[vi]
            pos.append(tuple(v.co));indices.append(len(indices))
            colors.append(tuple(min(1.0,max(0.0,x)) for x in attr.data[vi if attr.domain=='POINT' else li].color) if attr else (1,1,1,1))
            uv.append(tuple(min(1.0,max(0.0,x)) for x in layer.data[li].uv) if layer else (.03,.03))
            normal=v.normal if m.polygons[tri.polygon_index].use_smooth else tri.normal
            normals.append(tuple(normal.normalized()))
    arrays=[(pos,'f',5126,'VEC3'),(colors,'f',5126,'VEC4'),(uv,'f',5126,'VEC2'),(indices,'I',5125,'SCALAR'),(normals,'f',5126,'VEC3')]
    data=b'';views=[];access=[]
    for arr,fmt,typ,shape in arrays:
        flat=arr if shape=='SCALAR' else [x for row in arr for x in row]
        block=struct.pack('<'+fmt*len(flat),*flat);views.append(dict(buffer=0,byteOffset=len(data),byteLength=len(block)));data+=block
        a=dict(bufferView=len(views)-1,componentType=typ,count=len(arr),type=shape)
        if len(access)==0:a.update(min=[min(v[k] for v in pos) for k in range(3)],max=[max(v[k] for v in pos) for k in range(3)])
        access.append(a)
    png=atlas.read_bytes();views.append(dict(buffer=0,byteOffset=len(data),byteLength=len(png)));data+=png;data+=b'\0'*(-len(data)%4)
    doc=dict(asset=dict(version='2.0',generator='Blender '+bpy.app.version_string+' / original CC0 v121 subset bake'),scene=0,scenes=[dict(nodes=[0])],nodes=[dict(mesh=0)],meshes=[dict(primitives=[dict(attributes={'POSITION':0,'COLOR_0':1,'TEXCOORD_0':2,'NORMAL':4},indices=3,material=0,mode=4)])],buffers=[dict(byteLength=len(data))],bufferViews=views,accessors=access,images=[dict(bufferView=5,mimeType='image/png')],textures=[dict(source=0)],materials=[dict(doubleSided=True,pbrMetallicRoughness=dict(baseColorTexture=dict(index=0),metallicFactor=0,roughnessFactor=1))])
    js=json.dumps(doc,separators=(',',':')).encode();js+=b' '*(-len(js)%4)
    glb=struct.pack('<III',0x46546c67,2,28+len(js)+len(data))+struct.pack('<II',len(js),0x4e4f534a)+js+struct.pack('<II',len(data),0x004e4942)+data
    path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(glb)
    REPORT.append(dict(path=str(path.relative_to(OUT)),sha256=hashlib.sha256(glb).hexdigest(),bytes=len(glb),triangles=len(indices)//3,vertices=len(pos),primitives=1,bounds=access[0]['min']+access[0]['max'],source=source,tool='Blender '+bpy.app.version_string,license='CC0-1.0 original project geometry; existing project CC0 atlas',axes='+Y up / +Z forward',pivot=[0,0,0]))
    evaluated.to_mesh_clear()

def import_subset(path):
    raw=path.read_bytes();n=struct.unpack_from('<I',raw,12)[0];d=json.loads(raw[20:20+n]);start=28+n
    def arr(i):
        a=d['accessors'][i];v=d['bufferViews'][a['bufferView']];width={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']]
        fmt='f' if a['componentType']==5126 else 'I';p=start+v.get('byteOffset',0)+a.get('byteOffset',0)
        val=struct.unpack_from('<'+fmt*(width*a['count']),raw,p)
        return [tuple(val[j:j+width]) for j in range(0,len(val),width)] if width>1 else val
    p=d['meshes'][0]['primitives'][0];a=p['attributes'];ix=arr(p['indices'])
    return mesh_object(path.stem,arr(a['POSITION']),[ix[j:j+3] for j in range(0,len(ix),3)],arr(a['COLOR_0']),arr(a['TEXCOORD_0']))

# Original authored swept, bent flame volumes: independent asymmetrical tongues,
# warm base -> yellow body -> orange tips. Heat is a bounded display animation.
p=[];f=[];c=[]
def tongue(cx,cz,height,radius,phase,core=False):
    start=len(p);sides=9;rings=8
    for ring in range(rings):
        t=ring/(rings-1);rad=radius*(1-t)**.72*(1+.18*math.sin(t*11+phase))+.001
        bend=math.sin(t*5+phase)*t*.105
        for k in range(sides):
            a=k*2*math.pi/sides
            p.append((cx+math.cos(a)*rad+bend,height*t,cz+math.sin(a)*rad+math.cos(t*4+phase)*t*.045))
            if core:color=(1,.86-.40*t,.26*(1-t),1)
            else:color=(.92+.08*(1-t),.24+.37*math.sin(t*math.pi),.015+.045*(1-t),1)
            c.append(color)
    for ring in range(rings-1):
        for k in range(sides):
            a=start+ring*sides+k;b=start+ring*sides+(k+1)%sides
            f.append((a,b,b+sides,a+sides))
    f.append(tuple(start+k for k in reversed(range(sides))))
for i,(x,z,h,r) in enumerate([(-.20,-.12,.61,.095),(.13,-.16,.82,.11),(.23,.11,.55,.08),(-.13,.17,.96,.10),(0,0,1.08,.12)]):
    tongue(x,z,h,r,i*1.8);tongue(x,z-.025,h*.61,r*.78,i*1.8,True)
# Low charred bed and scattered embers remain attached to the ground anchor.
for i in range(16):
    a=i*2*math.pi/16;rad=.18+.09*math.sin(i*2.1);x=math.cos(a)*rad;z=math.sin(a)*rad;n=len(p)
    p.extend([(x-.038,.004,z-.025),(x+.05,.004,z-.025),(x+.04,.032,z+.03),(x-.025,.026,z+.03)])
    ember=(.55,.10,.012,1) if i%3==0 else (.075,.055,.038,1);c.extend([ember]*4);f.append((n,n+1,n+2,n+3))
flame=mesh_object('fire-v121',p,f,c)
for poly in flame.data.polygons:poly.use_smooth=True
export(flame,ASSETS/'field/fire-v121.glb',ROOT/'app/src/main/assets/3d/field/atlas.png','swept tapered volumes with ten curved tongues')
# Smoke is a restrained, irregular cluster with a rounded silhouette rather than
# the old octahedron. It stays opaque on the existing baseline material; no fake
# claim of fluid simulation or transparent volumetrics.
p=[];f=[];c=[]
for i in range(4):
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=2,radius=1)
    o=bpy.context.object;start=len(p);rad=.16+i*.035
    for v in o.data.vertices:
        k=len(p);n=1+.10*math.sin(k*1.731)
        p.append((v.co.x*rad*n+i*.025,v.co.z*rad*.68+.12+i*.15,v.co.y*rad*n))
        shade=.23+i*.035+v.co.z*.045;c.append((shade*1.02,shade,shade*.94,1))
    for poly in o.data.polygons:f.append(tuple(start+j for j in poly.vertices))
    bpy.data.objects.remove(o,do_unlink=True)
smoke=mesh_object('smoke-v121',p,f,c)
for poly in smoke.data.polygons:poly.use_smooth=True
export(smoke,ASSETS/'field/smoke-v121.glb',ROOT/'app/src/main/assets/3d/field/atlas.png','four irregular smoke lobes; opaque baseline')
# Weathered stratified rocks: unequal rings, fractured ledges and overhangs.
for lod in range(2):
    p=[];f=[];c=[];uv=[];sides=12 if lod==0 else 8;rings=8 if lod==0 else 5
    for j in range(rings):
        t=j/(rings-1);radius=(.35*(1-t)**.35+.025)*(1+.15*math.sin(j*2.0))
        for i in range(sides):
            a=i*2*math.pi/sides;fracture=1+.17*math.sin(i*2.4)+.08*math.cos(i*4+j*.4)
            x=math.cos(a)*radius*fracture+t*.07;z=math.sin(a)*radius*.67*fracture
            p.append((x,t*.46+.018*math.sin(a*3+j),z));tone=.77+.12*math.sin(j*2.2)+.035*math.cos(a)
            c.append((tone,tone,min(1,tone*.96),1));uv.append(((4.03+(i/sides)*.94)/8,.03+.94*t))
    for j in range(rings-1):
        for i in range(sides):a=j*sides+i;b=j*sides+(i+1)%sides;f.append((a,b,b+sides,a+sides))
    f.append(tuple((rings-1)*sides+i for i in range(sides)))
    rock=mesh_object('rock-strata-v121-lod'+str(lod),p,f,c,uv)
    export(rock,ASSETS/('field/rock-strata-v121-lod'+str(lod)+'.glb'),ROOT/'app/src/main/assets/3d/field/atlas.png','asymmetric layered sedimentary outcrop')
# Refine the original project gate, retaining opening, pivot and LOD silhouettes.
# Mesh weld + bevel adds actual chamfers and removes razor sharp box edges.
for lod in range(3):
    gate=import_subset(ROOT/('app/src/main/assets/3d/sites/gate-lod'+str(lod)+'.glb'))
    bpy.context.view_layer.objects.active=gate;gate.select_set(True)
    weld=gate.modifiers.new('Weld original modular seams','WELD');weld.merge_threshold=.00001
    bevel=gate.modifiers.new('Weathered stone chamfer','BEVEL');bevel.width=.005 if lod<2 else .003;bevel.segments=2 if lod==0 else 1;bevel.limit_method='ANGLE';bevel.angle_limit=.65
    export(gate,ASSETS/('sites/gate-v121-lod'+str(lod)+'.glb'),ROOT/'app/src/main/assets/3d/sites/atlas.png','existing CC0 gate, Blender welded/beveled masonry; opening retained')
    gate.select_set(False)
report=OUT/'docs/native-pc-visual/feedback-v121-blender-assets.json';report.parent.mkdir(parents=True,exist_ok=True)
report.write_text(json.dumps(dict(generator='tools/3d/refine_blender121.py',blender=bpy.app.version_string,assets=REPORT),indent=2)+'\n')
# Editable source scene is reproducible from this script; retained as a CI artifact,
# not multiplied in the repository. Positions use the runtime Y-up convention.
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'feedback-v121-models.blend'))
print(json.dumps(dict(status='EXPORTED',assets=len(REPORT),triangles=sum(a['triangles'] for a in REPORT))))

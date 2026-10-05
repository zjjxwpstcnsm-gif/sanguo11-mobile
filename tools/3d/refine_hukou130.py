#!/usr/bin/env python3
"""Author a compact Hukou gorge, retaining the native v129 atlas/UV contract.

Blender 4.3.2:
  blender -b --python-exit-code 1 --python tools/3d/refine_hukou130.py -- OUTPUT
Outputs only NEW v130 Hukou assets, editable source, host previews and manifest.
+Y up, +Z downstream. Water lip z=.34/y=1; foot z=.50/y=.015.
Original project CC0 geometry. No imported models, textures or map modification.
The preview is explicitly OFFLINE BLENDER SOURCE, never APK evidence.
"""
import bpy, math, json, struct, hashlib, sys, time
from pathlib import Path
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[2]
args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
OUT=Path(args[0]).resolve() if args else ROOT
ASSETS=OUT/'app/src/main/assets/3d/field/v130'
ART=OUT/'out/landmarks130/hukou'
ASSETS.mkdir(parents=True,exist_ok=True); ART.mkdir(parents=True,exist_ok=True)
ATLAS=ROOT/'app/src/main/assets/3d/field/v129/scenery-atlas.png'
assert bpy.app.version[:3]==(4,3,2),bpy.app.version_string
# Existing exact exporter is reused without running any v129 constructors or writes.
ns={'__file__':str(ROOT/'tools/3d/refine_blender121.py')}
low=(ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0]
exec(compile(low,'existing strict mesh helpers','exec'),ns)
REPORT=[]
old=(ROOT/'tools/3d/refine_blender129.py').read_text()
exporter=old[old.index('def export_indexed'):old.index('def blend')]
exporter=exporter.replace('original CC0 v129 strict indexed bake','original CC0 v130 Hukou strict indexed bake')
exec(compile(exporter,'v129 exact indexed exporter','exec'))

class Sculpt:
    def __init__(self): self.p=[];self.c=[];self.uv=[];self.f=[];self.sm=[];self.groups=[]
    def v(self,x,y,z,col=(1,1,1),panel=0,u=.5,v=.5):
        self.p.append((x,y,z)); self.c.append((*col,1))
        self.uv.append(((panel+.06+.88*u)/8,.06+.88*v)); return len(self.p)-1
    def face(self,ids,smooth=False): self.f.append(tuple(ids));self.sm.append(smooth)
    def group(self,name,start): self.groups.append((name,start,len(self.p)))
    def object(self,name):
        ob=ns['mesh_object'](name,self.p,self.f,self.c,self.uv)
        for poly,smooth in zip(ob.data.polygons,self.sm):poly.use_smooth=smooth
        for name,start,end in self.groups:
            ob.vertex_groups.new(name=name).add(list(range(start,end)),1,'REPLACE')
        return ob

def lerp(a,b,t):return a+(b-a)*t
def clamp(v):return max(0,min(1,v))
def gauss(x,w):return math.exp(-(x/w)**2)
def tint(c,k):return tuple(max(.01,min(1,a*k)) for a in c)
def mix(a,b,t):return tuple(lerp(x,y,t) for x,y in zip(a,b))
def linear(points,t):
    for (a,x),(b,y) in zip(points,points[1:]):
        if t<=b:return lerp(x,y,clamp((t-a)/(b-a)))
    return points[-1][1]
def top(t):return 1 if t<=.34 else max(.015,1-(t-.34)/.16)
def water_half(t):
    return linear([(0,.205),(.13,.177),(.26,.123),(.34,.097),(.405,.112),(.50,.151),(.62,.211),(.79,.249),(1,.217)],t)
def shoulder_outer(t,side):
    # The wide shoulders occupy the upstream mountain bank. Both the lip and
    # foot are tighter so the true river/mountain-only corridor can still fit.
    radius=linear([(0,.31),(.065,.425),(.17,.458),(.255,.390),(.34,.268),(.43,.242),(.50,.215),(.58,.17)],t)
    return radius*(1+.021*math.sin(t*31+side*1.4))
def centerline(t):return .010*math.sin(t*8)*math.sin(math.pi*t)
def point_water(t,u):
    edge=2*u-1; fall=clamp((t-.34)/.16)
    width=water_half(t)*(1+.023*math.sin(t*47)+.012*math.cos(u*13+t*33))
    x=centerline(t)+edge*width
    phase=u*math.tau*4.6+t*19
    fold=(.5+.5*math.sin(phase))
    bulge=math.sin(math.pi*fall)
    y=top(t)+.025*bulge*(fold-.26)
    # Upstream flow relief stays below the 1.00 contract except low amplitude
    # turbulence. Exact .34 and .50 rows remain the support/impact landmarks.
    if t<.34:y=1-.005*(.5+.5*math.sin(u*25+t*53))*math.sin((.34-t)/.34*math.pi)
    if t>.50:
        y=.012+.010*gauss(t-.59,.09)*math.sin(u*16+t*7)**2
        y+=.0035*math.sin(u*19+t*61)*math.sin(math.pi*clamp((t-.5)/.5))
    z=t+.022*bulge*(.20+.80*fold)
    if t<.06:z+=.012*(1-t/.06)*(.5+.5*math.sin(u*17+.2))
    if .50<t<.79:z+=.040*math.sin(math.pi*(t-.50)/.29)*(.25+.75*math.sin(u*13+.6)**2)
    if t>.90:z-=(t-.90)/.1*(.008+.023*(.5+.5*math.sin(u*15)))
    return x,y,z

def bridge_grid(s,rows,cols,start,smooth):
    for j in range(rows-1):
        for k in range(cols-1):
            a=start+j*cols+k
            ids=[a,a+1,a+cols+1,a+cols]
            if s.p[a+1][0]>s.p[a][0]:ids.reverse()
            s.face(ids,smooth)

def water(s,lod):
    # Geometry follows actual turbulence rather than subdividing a flat plane.
    cols=25 if lod==0 else 11
    stages=[([0,.06,.13,.20,.26,.30,.34,.355,.374,.397,.421,.447,.472,.49,.50] if not lod else [0,.13,.26,.34,.385,.435,.47,.50],3),
            ([.50,.522,.546,.575,.603,.64] if not lod else [.50,.54,.59,.64],4),
            ([.64,.69,.76,.84,.91,1] if not lod else [.64,.76,.88,1],5)]
    for rows,panel in stages:
        start=len(s.p)
        for t in rows:
            for k in range(cols):
                u=k/(cols-1); x,y,z=point_water(t,u)
                if panel==3:
                    c=tint((.98,.93,.81),.94+.06*math.sin(u*29+t*23)**2)
                    uv_t=t
                elif panel==4:
                    # Existing pale froth region, spatially coherent contact apron.
                    c=mix((.99,.93,.79),(.75,.91,.88),clamp((t-.50)/.14)*.35)
                    c=tint(c,.88+.12*math.sin(u*29+t*47)**2)
                    uv_t=.835+.045*math.sin(u*13+t*19)
                else:
                    c=mix((.91,.96,.89),(.78,.93,.91),clamp((t-.64)/.36))
                    uv_t=.75+.20*clamp((t-.64)/.36)
                s.v(x,y,z,c,panel,u,uv_t)
        bridge_grid(s,len(rows),cols,start,True)
    s.group('Continuous ochre throat, pale impact and river tail',0)

def shoulders(s,lod):
    # Two eroded terraced banks, not a vertical closed pillar. Outer boundary
    # tapers almost to zero; neck and foot taper along the real corridor.
    rows=[0,.045,.085,.13,.17,.21,.25,.285,.315,.34,.365,.40,.435,.47,.50,.535,.57] if not lod else [0,.085,.17,.25,.315,.34,.40,.47,.53,.57]
    across=[0,.10,.23,.35,.43,.46,.59,.67,.71,.82,.90,1] if not lod else [0,.23,.43,.59,.71,1]
    for side in [-1,1]:
        start=len(s.p)
        for j,t in enumerate(rows):
            inner=water_half(min(t,.5))*.91
            outside=max(inner+.015,shoulder_outer(t,side))
            # Base/front slopes are tapered down so small terrain lifting cannot
            # expose an upright pedestal. All Y is normalized and placement owned.
            longitudinal=linear([(0,.995),(.13,.997),(.27,.995),(.34,.99),(.39,.76),(.44,.48),(.50,.15),(.57,.015)],t)
            for k,a in enumerate(across):
                step=linear([(0,1),(.10,1.012),(.23,.99),(.35,.95),(.43,.925),(.46,.75),(.59,.72),(.67,.68),(.71,.45),(.82,.42),(.9,.31),(1,.012)],a)
                # Asymmetric bedding joints are genuine height not only texture.
                joint=(.022*gauss(t-(.16+.025*a*side),.012)+.019*gauss(t-(.285-.023*a),.010))*math.sin(math.pi*a)
                wave=.009*math.sin(t*66+side+a*11)*math.sin(math.pi*a)
                y=min(.995,max(.001,longitudinal*step-joint+wave))
                x=centerline(t)+side*lerp(inner,outside,a)
                z=max(0,t+.008*math.sin(a*23+side+t*31)*math.sin(math.pi*a))
                color=tint((.97,.90,.76),.90+.07*math.sin(t*47+side+a*2)-joint*2)
                s.v(x,y,z,color,0,.12+.76*t/.57,.12+.45*y)
        bridge_grid(s,len(rows),len(across),start,False)
        s.group(('Left' if side<0 else 'Right')+' irregular stratified gorge shoulder',start)
    # A supporting center bed terminates under the falling sheet. It is a narrow
    # coherent ledge; most of the width is the terraced shoulders, not an upright box.
    rows=[0,.075,.15,.22,.28,.315,.34,.37,.40,.435,.47,.50] if not lod else [0,.15,.28,.34,.40,.47,.50]
    cols=9 if not lod else 5;start=len(s.p)
    for t in rows:
        for k in range(cols):
            u=k/(cols-1);half=water_half(t)*.97
            s.v(centerline(t)+(2*u-1)*half,max(0,top(t)-.006),t,(.89,.83,.70),0,u,.13+.42*top(t))
    bridge_grid(s,len(rows),cols,start,False)
    s.group('Recessed support ledge ending beneath foot',start)

def low_lobe(s,cx,cz,rx,rz,h,seed,lod,foam=False):
    # Compact, closed shallow relief; no spray particles or floating splinters.
    start=len(s.p); n=12 if not lod else 8
    bands=[(1,0),(.92,.30),(.70,.77),(.29,1)] if not lod else [(1,0),(.66,.70),(.22,1)]
    for j,(r,yy) in enumerate(bands):
        for k in range(n):
            a=math.tau*k/n; noise=1+.07*math.sin(a*3+seed)+.035*math.sin(a*7-seed)
            x=cx+math.cos(a)*rx*r*noise;z=cz+math.sin(a)*rz*r*noise
            base=.013 if foam else .005
            y=base+h*yy*(.94+.06*math.sin(a*2+seed))
            col=(.98,.93,.82) if foam else (.91,.84,.70)
            s.v(x,y,z,tint(col,.87+.12*yy),4 if foam else 0,k/n,.85+.03*yy if foam else .16+.36*yy)
    for j in range(len(bands)-1):
        for k in range(n):
            a=start+j*n+k;b=start+j*n+(k+1)%n;s.face([a,a+n,b+n,b],foam)
    s.face([start+(len(bands)-1)*n+k for k in reversed(range(n))],foam)
    # Underside excluded: mesh rests directly on support/water; adding a hidden
    # planar disk would only spend vertices and cannot improve the native image.
    s.group(('Attached pale rolling impact' if foam else 'Bedrock foot spall')+str(seed),start)

def make(lod):
    s=Sculpt();water(s,lod);shoulders(s,lod)
    impacts=[(-.103,.546,.075,.023,.032),(.020,.567,.098,.032,.040),(.120,.589,.068,.030,.027),(-.070,.616,.083,.029,.021)]
    for i,(x,z,rx,rz,h) in enumerate(impacts):
        if lod and i==3:continue
        low_lobe(s,x,z,rx,rz,h,i,lod,True)
    for i,(x,z,rx,rz,h) in enumerate([(-.190,.490,.049,.058,.08),(.185,.462,.048,.069,.125),(-.225,.390,.052,.058,.19)]):
        if lod and i==2:continue
        low_lobe(s,x,z,rx,rz,h,i+6,lod)
    return s

objects=[]
for lod in [0,1]:
    sculpt=make(lod);ob=sculpt.object('fall-hukou-lod'+str(lod));objects.append(ob)
    export_indexed(ob,ASSETS/(ob.name+'.glb'),'Original low terraced Hukou gorge: unequal horizontal shoulder ledges, tightly folded ochre throat, continuous pale impact, low widening blue-green river contact')
    budget=4000 if not lod else 850
    assert REPORT[-1]['triangles']<=budget,(lod,REPORT[-1]['triangles'])
    if not lod:assert REPORT[-1]['triangles']>=2500,REPORT[-1]['triangles']
    assert max(p[1] for p in sculpt.p)==1.0
    assert max(abs(p[0]) for p in sculpt.p)<.48
    ob['source_axes']='+Y up, +Z downstream';ob['lip_z']=.34;ob['foot_z']=.50
    ob['runtime_atlas']='byte-identical v129 atlas; PNG row0=UV.v0'

# Accurate source material: runtime PNG row0=UV.v0, Blender image row0 is bottom.
mat=bpy.data.materials.new('Hukou130 v129 atlas x Color; explicit runtime V flip');mat.use_nodes=True
nt=mat.node_tree;nt.nodes.clear();out=nt.nodes.new('ShaderNodeOutputMaterial');bs=nt.nodes.new('ShaderNodeBsdfPrincipled');bs.inputs['Roughness'].default_value=.92
tex=nt.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(ATLAS));tex.interpolation='Linear'
uv=nt.nodes.new('ShaderNodeTexCoord');sep=nt.nodes.new('ShaderNodeSeparateXYZ');comb=nt.nodes.new('ShaderNodeCombineXYZ');flip=nt.nodes.new('ShaderNodeMath');flip.operation='SUBTRACT';flip.inputs[0].default_value=1
nt.links.new(uv.outputs['UV'],sep.inputs[0]);nt.links.new(sep.outputs['X'],comb.inputs['X']);nt.links.new(sep.outputs['Y'],flip.inputs[1]);nt.links.new(flip.outputs[0],comb.inputs['Y']);nt.links.new(comb.outputs[0],tex.inputs['Vector'])
vc=nt.nodes.new('ShaderNodeVertexColor');vc.layer_name='Color';mul=nt.nodes.new('ShaderNodeMixRGB');mul.blend_type='MULTIPLY';mul.inputs[0].default_value=1
nt.links.new(tex.outputs['Color'],mul.inputs[1]);nt.links.new(vc.outputs['Color'],mul.inputs[2]);nt.links.new(mul.outputs[0],bs.inputs['Base Color']);nt.links.new(bs.outputs[0],out.inputs[0])
for ob in objects:ob.data.materials.append(mat);ob.hide_render=True
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=20;scene.cycles.use_denoising=False
scene.render.resolution_x=1000;scene.render.resolution_y=850;scene.render.resolution_percentage=100
scene.world.color=(.25,.25,.25);scene.view_settings.view_transform='Standard';scene.view_settings.look='None'
scene.view_settings.exposure=0;scene.view_settings.gamma=1
bpy.ops.object.light_add(type='AREA',location=(-3,-4,7));bpy.context.object.data.energy=600;bpy.context.object.data.size=5
bpy.ops.object.light_add(type='AREA',location=(4,2,5));bpy.context.object.data.energy=340;bpy.context.object.data.size=4
bpy.ops.object.camera_add();camera=bpy.context.object;scene.camera=camera;camera.data.type='ORTHO'
# Native coordinates remain editable; render proxies alone use Blender Z-up.
raw=bpy.data.collections.new('Editable runtime Y-up source meshes');scene.collection.children.link(raw)
for ob in objects:
    for col in list(ob.users_collection):col.objects.unlink(ob)
    raw.objects.link(ob)
# Actual GLB reimport comparison uses the same exact normals as the runtime subset.
def import_actual(path):
    ob=ns['import_subset'](path);r=path.read_bytes();n=struct.unpack_from('<I',r,12)[0];d=json.loads(r[20:20+n])
    a=d['accessors'][4];v=d['bufferViews'][a['bufferView']];f=struct.unpack_from('<'+'f'*a['count']*3,r,28+n+v.get('byteOffset',0))
    normals=[f[k:k+3] for k in range(0,len(f),3)]
    for poly in ob.data.polygons:poly.use_smooth=True
    ob.data.normals_split_custom_set_from_vertices(normals);ob.data.materials.append(mat);ob.hide_render=True;return ob
baseline=import_actual(ROOT/'app/src/main/assets/3d/field/v129/fall-hukou-lod0.glb');baseline.name='REFERENCE v129 exact baked GLB'
proxies=[]
for ob in [baseline,*objects]:
    proxy=ob.copy();proxy.data=ob.data.copy();scene.collection.objects.link(proxy);proxy.hide_render=True
    proxy.rotation_euler.x=math.pi/2;proxy.scale=(1,.44,1.15);proxies.append(proxy)
center=Vector((0,-.51,.18));camera.location=center+Vector((1.4,-1.9,1.45));camera.rotation_euler=(center-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.ortho_scale=1.68
previews=[]
for state,ob in zip(['v129-same-low-rise','v130-near','v130-far'],proxies):
    ob.hide_render=False;scene.render.filepath=str(ART/(state+'-OFFLINE.png'));bpy.ops.render.render(write_still=True);ob.hide_render=True
    previews.append({'path':str(Path(scene.render.filepath).relative_to(OUT)),'kind':'OFFLINE_BLENDER_SOURCE_NOT_APK','camera':list(camera.location),'orthographic_scale':camera.data.ortho_scale,'presentation_scale':[1,.44,1.15]})
# Separate map-scale source comparison: measured strategy-camera tilt 55 degrees,
# pixel density 61.6 at 1232px / (2*span10). No made-up terrain or background map.
scene.render.resolution_x=246;scene.render.resolution_y=246;camera.data.ortho_scale=4
camera.location=center+Vector((0,-math.cos(math.radians(55))*5,math.sin(math.radians(55))*5));camera.rotation_euler=(center-camera.location).to_track_quat('-Z','Y').to_euler()
for state,ob in zip(['v129-map-scale','v130-map-scale','v130-far-map-scale'],proxies):
    ob.hide_render=False;scene.render.filepath=str(ART/(state+'-OFFLINE.png'));bpy.ops.render.render(write_still=True);ob.hide_render=True
    previews.append({'path':str(Path(scene.render.filepath).relative_to(OUT)),'kind':'OFFLINE_SOURCE_AT_APK_PIXEL_DENSITY_NOT_APK','camera':list(camera.location),'orthographic_scale':4,'pixels_per_world_unit':61.5,'presentation_scale':[1,.44,1.15]})
for ob in proxies:bpy.data.objects.remove(ob,do_unlink=True)
# Preserve packed editable source including the exact v129 reference for review.
bpy.ops.file.pack_all();bpy.ops.wm.save_as_mainfile(filepath=str(ART/'hukou-v130-editable.blend'))
for entry in REPORT:
    entry['license']='CC0-1.0 original project geometry; unchanged v129 atlas'
    entry.pop('export_cpu_seconds',None)
report={'version':130,'family':'fall-hukou','blender':bpy.app.version_string,'generator':'tools/3d/refine_hukou130.py','generator_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),'source_dependencies':{p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in ['tools/3d/refine_blender121.py','tools/3d/refine_blender129.py']},'assets':REPORT,'editable_source':str((ART/'hukou-v130-editable.blend').relative_to(OUT)),'atlas':{'path':'app/src/main/assets/3d/field/v129/scenery-atlas.png','sha256':hashlib.sha256(ATLAS.read_bytes()).hexdigest(),'dimensions':[512,64],'runtime_uv':'PNG row0 = UV.v0, preview flips V','new_texture_count':0},'contract':{'up_axis':'+Y','downstream_axis':'+Z','lip_z':.34,'lip_y':1,'foot_z':.50,'foot_y':.015,'normalized_height':True,'terrain_map_river_modifications':False},'budget_reason':'Near vertices describe unequal terraced shoulders, real recessed strata and sampled turbulent water curvature. Far keeps the silhouette, short throat, connected apron and river tail. No generic subdivision.','input_visual':'out/landmarks129/runtime/lavapipe-candidate/images/hukou-menu-span10-surface.png','reference_status':'Exact v129 APK composition inspected. No exact same-region PC screenshot available: REFERENCE_MISSING.','placement_caveat':'Source geometry is not sufficient by itself: integration must remove the existing minimum .78 / bedHigh+.45 vertical extrusion and seat the contact tail on unchanged TerrainSurface. Proposed runtime physical rise is controlled by caller.','previews':previews,'verification':'OFFLINE_BLENDER_SOURCE_NOT_APK. Real source-matched APK visual review and ARM64 performance remain separate requirements.'}
path=OUT/'docs/native-pc-visual/hukou-v130-source.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'status':'EXPORTED','assets':[(x['id'],x['triangles'],x['bytes']) for x in REPORT],'artifact_dir':str(ART)}))

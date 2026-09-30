#!/usr/bin/env python3
"""CC0 landscape, architecture and articulated army refinement in Blender 4.2.3.
Run blender --background --python tools/3d/refine_blender124.py -- OUTPUT [--no-preview].
Old resources, terrain topology, placement seed and animation clips stay intact.
Only evaluated original geometry is baked into the existing strict GLB subset.
"""
import ast, json, math, struct, sys, hashlib
from pathlib import Path
import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[2]
args = sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
OUT = Path(args[0]) if args else ROOT
helper = (ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0]
ns = {'__file__': str(ROOT/'tools/3d/refine_blender121.py')}
exec(compile(helper, 'reviewed subset exporter', 'exec'), ns)
mesh_object, export, import_subset = ns['mesh_object'], ns['export'], ns['import_subset']
base_ns = {'math': math}
tree = ast.parse((ROOT/'tools/3d/build_sites.py').read_text())
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n, ast.ClassDef) and n.name=='Mesh'], type_ignores=[]), 'site vocabulary', 'exec'), base_ns)
Base = base_ns['Mesh']
reuse = {'Base': Base, 'math': math, 'bpy': bpy, 'struct': struct, 'json': json}
tree = ast.parse((ROOT/'tools/3d/refine_blender123.py').read_text().replace('v123 indexed joint bake','v124 indexed joint bake'))
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n, ast.ClassDef) and n.name=='Architecture' or isinstance(n, ast.FunctionDef) and n.name=='compact'], type_ignores=[]), 'v123 vocabulary and exact indexing', 'exec'), reuse)
Architecture, compact = reuse['Architecture'], reuse['compact']
objects = []

class Architecture124(Architecture):
    def build(self, kind, lod):
        super().build(kind, lod)
        if lod==2:
            return self
        if kind.startswith('city'):
            # Covered galleries frame existing courtyards, away from four axial lanes.
            for side in [-1, 1]:
                for end in [-1, 1]:
                    x, z = side*.50, end*.28
                    self.curved_roof(x, z, .25, .10, .19, .045, lod)
                    for dx in [-.10, .10]:
                        self.box(x+dx-.008, .045, z-.04, .016, .145, .016, 2)
                    if lod==0:
                        for dx in [-.07, 0, .07]:
                            self.box(x+dx-.006, .045, z+.04, .012, .065, .012, 2)
                        self.box(x-.12, .11, z+.04, .24, .010, .014, 2)
        elif kind=='port':
            # Deck fascia and ladder establish a crafted timber silhouette at water.
            for x in [-.31, .31]:
                self.box(x-.009, .095, .45, .018, .032, .29, 2)
            if lod==0:
                for x in [-.23, -.15]:
                    self.box(x, -.045, .74, .012, .16, .014, 2)
                for y in [-.01, .035, .08]:
                    self.box(-.23, y, .747, .092, .008, .018, 2)
                for x in [-.30, -.08]:
                    self.box(x, .39, -.19, .018, .018, .035, 0)
        elif kind=='gate':
            # Voussoirs outline the open passage; no stones enter its clearance.
            for side in [-1, 1]:
                self.box(side*.13-.014, .035, .187, .028, .22, .026, 0)
            segments = 9 if lod==0 else 5
            for i in range(segments):
                a, b = i*math.pi/segments, (i+1)*math.pi/segments
                self.face([(.13*math.cos(a), .245+.085*math.sin(a), .21),
                           (.13*math.cos(b), .245+.085*math.sin(b), .21),
                           (.158*math.cos(b), .245+.111*math.sin(b), .21),
                           (.158*math.cos(a), .245+.111*math.sin(a), .21)], 0)
        return self

for kind in ['city0', 'city1', 'city2', 'port', 'gate']:
    for lod in range(3):
        m = Architecture124().build(kind, lod)
        obj = mesh_object(f'{kind}-v124-lod{lod}', m.p, [m.idx[i:i+3] for i in range(0, len(m.idx), 3)], m.c, m.uv)
        if kind in ['gate', 'port'] and lod==0:
            weld = obj.modifiers.new('Masonry seam weld', 'WELD'); weld.merge_threshold=.00001
            bevel = obj.modifiers.new('Soft stone and timber edges', 'BEVEL'); bevel.width=.002; bevel.segments=1; bevel.limit_method='ANGLE'; bevel.angle_limit=.85
        export(obj, OUT/f'app/src/main/assets/3d/sites/v124/{kind}-lod{lod}.glb', ROOT/'app/src/main/assets/3d/sites/atlas.png', 'original covered courtyard galleries, open stone arch and fitted wharf details')
        objects.append((obj, kind, lod, 'sites'))

rig_doc = json.loads((ROOT/'app/src/main/assets/3d/field/rigs-v123.json').read_text())
for name, rig in sorted(rig_doc['rigs'].items()):
    raw = import_subset(ROOT/f'app/src/main/assets/3d/field/v123/{name}.glb')
    lod, kind = int(name[-1]), name[5:].split('-lod')[0]
    parts, p, faces, colors, uv = [], [], [], [], []
    src=raw.data; ca=src.color_attributes['Color']; layer=src.uv_layers.active
    for part in rig['parts']:
        lo, hi = part['first'], part['first']+part['count']; first=len(p)
        for poly in src.polygons:
            if not all(lo<=i<hi for i in poly.vertices):
                continue
            start=len(p)
            for li in poly.loop_indices:
                vi=src.loops[li].vertex_index
                p.append(tuple(src.vertices[vi].co)); colors.append(tuple(ca.data[vi].color)); uv.append(tuple(layer.data[li].uv))
            faces.append(tuple(range(start, len(p))))
        addon=Base()
        def panel_box(x,y,z,w,h,d,panel):
            at=len(addon.p); addon.box(x,y,z,w,h,d,0)
            for j in range(at,len(addon.p)):
                addon.uv[j]=((panel+.35)/8,.48)
        oy=.14 if kind=='CAVALRY' else 0
        if part['name']=='body' and kind in ['SPEAR','HALBERD','CROSSBOW','CAVALRY','SWORD']:
            # Belt and segmented apron read as armour rather than a plain cylinder.
            for side in [-1,1]:
                panel_box(side*.037-.010,.133+oy,.040,.020,.045,.008,4)
            panel_box(-.047,.167+oy,.035,.094,.010,.010,2)
            if lod==0:
                panel_box(-.010,.161+oy,.045,.020,.020,.006,4)
        if part['name']=='handL' and kind in ['SPEAR','HALBERD','SWORD'] and lod==0:
            # Shield boss/rim follow the hand joint, preserving attack articulation.
            panel_box(-.091,.202+oy,.050,.030,.033,.009,4)
        if part['name']=='horse' and lod==0:
            # Saddle blanket/straps remain wholly inside the horse rigid range.
            for side in [-1,1]:
                panel_box(side*.068-.007,.204,-.055,.014,.07,.10,3)
        if part['name']=='hull' and lod==0:
            for z in [-.23,-.08,.07,.20]:
                panel_box(-.14,.127,z,.28,.012,.013,2)
        if part['name']=='body' and kind not in ['SPEAR','HALBERD','CROSSBOW','CAVALRY','SWORD']:
            # Iron corner straps on chassis; small bolt heads only on close LOD.
            for side in [-1,1]:
                panel_box(side*.105-.009,.07,-.115,.018,.10,.024,4)
                if lod==0:
                    panel_box(side*.112-.008,.134,.11,.016,.016,.010,4)
        start=len(p);p.extend(addon.p);colors.extend(addon.c);uv.extend(addon.uv)
        faces.extend(tuple(start+j for j in addon.idx[i:i+3]) for i in range(0,len(addon.idx),3))
        exported=sum((len(face)-2)*3 for face in faces)
        previous=sum(x['count'] for x in parts)
        parts.append(dict(part, first=previous, count=exported-previous))
    bpy.data.objects.remove(raw,do_unlink=True)
    obj=mesh_object(name+'-v124',p,faces,colors,uv)
    export(obj,OUT/f'app/src/main/assets/3d/field/v124/{name}.glb',ROOT/'app/src/main/assets/3d/field/unit-atlas.png','original fitted armour, shield bosses, saddle blanket, deck planks and chassis straps; original rigid joints')
    assert ns['REPORT'][-1]['vertices']==sum(x['count'] for x in parts)
    rig['parts']=parts;objects.append((obj,kind,lod,'units'))

def append_baked(p,faces,colors,uv,obj,panel,color):
    first=len(p)
    lo=[min(v.co[k] for v in obj.data.vertices) for k in range(3)]
    hi=[max(v.co[k] for v in obj.data.vertices) for k in range(3)]
    for v in obj.data.vertices:
        co=obj.matrix_world@v.co
        p.append(tuple(co));colors.append(color)
        # A continuous atlas patch per crown avoids modulo wrap inside triangles.
        u=(v.co.x-lo[0])/max(.0001,hi[0]-lo[0]);t=(v.co.z-lo[2])/max(.0001,hi[2]-lo[2])
        uv.append(((panel+.08+.84*u)/8,.08+.84*t))
    faces.extend(tuple(first+j for j in poly.vertices) for poly in obj.data.polygons)
    bpy.data.objects.remove(obj,do_unlink=True)

for kind in ['tree','tree-upland','shrub','rock-strata-v121']:
    for lod in range(2):
        p,faces,colors,uv=[],[],[],[]
        if kind=='rock-strata-v121':
            # Leaning strata form a broken ledge, not an isolated conical hill.
            sides=12 if lod==0 else 8
            for ring in range(4):
                y=[0,.10,.22,.32][ring];rad=[.26,.31,.23,.12][ring]
                for k in range(sides):
                    a=k*math.tau/sides;variation=1+.15*math.sin(k*2.3+ring*.6)
                    p.append((math.cos(a)*rad*variation+ring*.035,y+.024*math.sin(a*3+ring),math.sin(a)*rad*.65*variation))
                    shade=.64+.065*ring+.045*math.sin(a+1);colors.append((shade,shade*.98,shade*.92,1));uv.append(((7+.08+.84*k/sides)/8,.05+.90*ring/3))
            for ring in range(3):
                for k in range(sides):
                    a=ring*sides+k;b=ring*sides+(k+1)%sides;faces.append((a,b,b+sides,a+sides))
            faces.append(tuple(3*sides+k for k in range(sides)))
        else:
            broad=kind=='tree';small=kind=='shrub';height=.32 if small else .93 if broad else 1.13
            bpy.ops.mesh.primitive_cone_add(vertices=7 if lod==0 else 5,radius1=.023 if small else .041,radius2=.01,depth=height*.70,location=(0,height*.35,0),rotation=(-math.pi/2,0,0))
            append_baked(p,faces,colors,uv,bpy.context.object,2,(.75,.76,.72,1))
            if broad:
                crowns=[(-.18,.65,.01,.24),(.17,.70,-.08,.24),(.01,.82,.12,.25)] if lod==0 else [(0,.74,0,.32)]
            elif small:
                crowns=[(-.10,.19,0,.18),(.10,.23,.06,.16)] if lod==0 else [(0,.21,0,.21)]
            else:
                crowns=[(0,.43,0,.32),(0,.66,0,.26),(0,.87,0,.18)]
            for i,(x,y,z,r) in enumerate(crowns):
                if kind=='tree-upland':
                    bpy.ops.mesh.primitive_cone_add(vertices=10 if lod==0 else 6,radius1=r,radius2=.025,depth=.38,location=(x,y,z),rotation=(-math.pi/2,0,0))
                else:
                    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1 if lod==0 else 0,radius=r,location=(x,y,z))
                    for v in bpy.context.object.data.vertices:
                        v.co.y*=.75;v.co*=1+.075*math.sin(v.index*2.37+i)
                append_baked(p,faces,colors,uv,bpy.context.object,6,(.80+i*.03,.89+i*.02,.77+i*.02,1))
        obj=mesh_object(f'{kind}-v124-lod{lod}',p,faces,colors,uv)
        if kind!='rock-strata-v121':
            for poly in obj.data.polygons:poly.use_smooth=True
        export(obj,OUT/f'app/src/main/assets/3d/field/v124/{kind}-lod{lod}.glb',ROOT/'app/src/main/assets/3d/field/atlas.png','original irregular broadleaf clusters, tapered upland canopy, understory and leaning rock strata; no terrain-height replacement')
        objects.append((obj,kind,lod,'field'))

for record in ns['REPORT']:
    path=OUT/record['path'];rig=rig_doc['rigs'].get(path.stem) if '/field/' in record['path'] else None
    old,new,parts=compact(path,None if rig is None else rig['parts'])
    if rig is not None:rig['parts']=parts
    record.update(vertices=new,expandedVertices=old,bytes=path.stat().st_size,sha256=hashlib.sha256(path.read_bytes()).hexdigest(),generator='tools/3d/refine_blender124.py')
    assert new<=30000 and record['bytes']<=2000000
    if path.stem.startswith(('tree','shrub','rock')):assert record['triangles']<=300
path=OUT/'app/src/main/assets/3d/field/rigs-v124.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(rig_doc,separators=(',',':'))+'\n')
path=OUT/'docs/native-pc-visual/feedback-v124-blender-assets.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps({'blender':bpy.app.version_string,'assets':ns['REPORT']},indent=2)+'\n')

if '--no-preview' not in args:
    # Offline previews are modelling evidence only. They are never APK screenshots.
    for obj,kind,lod,family in objects:
        obj.hide_render=True;obj.rotation_euler.x=math.pi/2
    scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.device='CPU';scene.cycles.samples=8
    scene.render.resolution_x=480;scene.render.resolution_y=400;scene.render.resolution_percentage=100
    scene.world.color=(.22,.22,.22);scene.view_settings.view_transform='Standard'
    for family in ['sites','units','field']:
        folder='sites' if family=='sites' else 'field';file='unit-atlas.png' if family=='units' else 'atlas.png'
        atlas=bpy.data.images.load(str(ROOT/'app/src/main/assets/3d'/folder/file))
        mat=bpy.data.materials.new(family+' atlas preview');mat.use_nodes=True;nodes=mat.node_tree.nodes
        tex=nodes.new('ShaderNodeTexImage');tex.image=atlas;mat.node_tree.links.new(tex.outputs['Color'],nodes.get('Principled BSDF').inputs['Base Color']);nodes.get('Principled BSDF').inputs['Roughness'].default_value=.85
        for obj,kind,lod,fam in objects:
            if fam==family:obj.data.materials.append(mat)
    bpy.ops.object.light_add(type='AREA',location=(2,-3,4));bpy.context.object.data.energy=400;bpy.context.object.data.size=5
    bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO'
    for obj,kind,lod,family in objects:
        if lod:continue
        obj.hide_render=False;target=Vector((0,0,.20 if family=='sites' else .45 if family=='field' else .24))
        cam.location=target+Vector((2,-3,2.4));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler()
        cam.data.ortho_scale=2.8 if kind.startswith('city') else 1.6 if family=='sites' else 1.45 if family=='field' else 1.1
        p=OUT/f'out/feedback124/blender/{kind}.png';p.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(p);bpy.ops.render.render(write_still=True);obj.hide_render=True
    bpy.ops.file.pack_all();bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'out/feedback124/blender/models-v124.blend'))
print(json.dumps({'status':'EXPORTED','models':len(ns['REPORT']),'triangles':sum(a['triangles'] for a in ns['REPORT'])}))

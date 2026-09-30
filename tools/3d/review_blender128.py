#!/usr/bin/env python3
"""Matching-camera BEFORE/AFTER source review, explicitly not Android screenshots.
Run after refine_blender128.py:
blender -b --python-exit-code 1 --python tools/3d/review_blender128.py -- OUTPUT
"""
import bpy,sys,json,math,hashlib,struct
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
OUT=Path(args[0]).resolve() if args else ROOT
ART=OUT/'out/feedback128/blender';bpy.ops.wm.open_mainfile(filepath=str(ART/'landmarks-v128.blend'))
ns={'__file__':str(ROOT/'tools/3d/refine_blender121.py')}
source=(ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0]
source=source.replace("bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)",'')
exec(compile(source,'strict subset reader','exec'),ns)
scene=bpy.context.scene;camera=scene.camera
for obj in scene.objects:
    if obj.type=='MESH':obj.hide_render=True
newmat=bpy.data.materials['v128 atlas x authored vertex color (runtime V orientation)']
records=[]
for family,version in [('fall-wide',127),('fall-hukou',127),('cliff-granite',126),('wall-earth',126)]:
    new=bpy.data.objects[family+'-lod0'];base=ROOT/f'app/src/main/assets/3d/field/v{version}/{family}-lod0.glb'
    before=ns['import_subset'](base);before.name='BEFORE v'+str(version)+' '+family
    # Preserve the actual exported custom normals, not guessed smoothing.
    raw=base.read_bytes();n=struct.unpack_from('<I',raw,12)[0];doc=json.loads(raw[20:20+n]);acc=doc['accessors'][doc['meshes'][0]['primitives'][0]['attributes']['NORMAL']];bv=doc['bufferViews'][acc['bufferView']]
    vals=struct.unpack_from('<'+'f'*(3*acc['count']),raw,28+n+bv.get('byteOffset',0)+acc.get('byteOffset',0));normals=[vals[k:k+3] for k in range(0,len(vals),3)]
    before.data.normals_split_custom_set_from_vertices(normals)
    oldmat=newmat.copy();oldmat.name='Before atlas v'+str(version);oldimg=bpy.data.images.load(str(ROOT/f'app/src/main/assets/3d/field/v{version}/scenery-atlas.png'))
    for node in oldmat.node_tree.nodes:
        if node.type=='TEX_IMAGE':node.image=oldimg
    before.data.materials.append(oldmat);before.hide_render=True
    proxies=[]
    for src in [before,new]:
        ob=src.copy();ob.data=src.data.copy();scene.collection.objects.link(ob);ob.hide_render=True;ob.rotation_euler.x=math.pi/2
        if family.startswith('fall-'):ob.scale=(2.3,.53 if family=='fall-hukou' else .76,1.3)
        proxies.append(ob)
    bpy.context.view_layer.update()
    points=[ob.matrix_world @ Vector(p) for ob in proxies for p in ob.bound_box]
    mn=Vector(tuple(min(p[k] for p in points) for k in range(3)));mx=Vector(tuple(max(p[k] for p in points) for k in range(3)));center=(mn+mx)*.5;size=max(mx-mn)
    camera.location=center+Vector((size*1.25,-size*1.65,size*1.2));camera.rotation_euler=(center-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.ortho_scale=size*1.58
    for state,ob in zip(['before','after'],proxies):
        ob.hide_render=False;scene.render.filepath=str(ART/(family+'-'+state+'-matched.png'));bpy.ops.render.render(write_still=True);ob.hide_render=True
        path=Path(scene.render.filepath);records.append({'family':family,'state':state,'path':str(path.relative_to(OUT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'kind':'OFFLINE_BLENDER_SOURCE_NOT_APK','camera':list(camera.location),'ortho_scale':camera.data.ortho_scale,'presentation_scale':list(ob.scale)})
    for ob in proxies:bpy.data.objects.remove(ob,do_unlink=True)
    bpy.data.objects.remove(before,do_unlink=True)
(ART/'matched-review.json').write_text(json.dumps(records,indent=2)+'\n')
print('MATCHED_SOURCE_REVIEW_COMPLETE')
# Pillow is an optional presentation-only dependency: raw matched renders above
# remain the evidence if it is absent in a Blender distribution.
try:
    from PIL import Image, ImageDraw, ImageFont
    font_path='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    large=ImageFont.truetype(font_path,24) if Path(font_path).exists() else ImageFont.load_default()
    small=ImageFont.truetype(font_path,18) if Path(font_path).exists() else ImageFont.load_default()
    for family,version in [('fall-wide',127),('fall-hukou',127),('cliff-granite',126),('wall-earth',126)]:
        sheet=Image.new('RGB',(1200,680),'#eeeeea');draw=ImageDraw.Draw(sheet)
        draw.text((18,12),'OFFLINE Blender source review | '+family,fill='#222222',font=large)
        draw.text((18,44),'Matching camera + lighting; source geometry only, NOT APK screenshots',fill='#444444',font=small)
        scales='Presentation XYZ scale 2.3 / '+('.53' if family=='fall-hukou' else '.76')+' / 1.3' if family.startswith('fall-') else 'Presentation scale 1 / 1 / 1'
        draw.text((18,70),scales+'; runtime terrain fitting differs',fill='#444444',font=small)
        for i,state in enumerate(['before','after']):
            im=Image.open(ART/(family+'-'+state+'-matched.png')).convert('RGB').resize((590,531));sheet.paste(im,(i*600+5,140))
            draw.text((i*600+18,106),'BEFORE v'+str(version) if i==0 else 'AFTER v128',fill='#222222',font=large)
        sheet.save(ART/(family+'-before-after.png'))
except ImportError:
    print('Pillow absent: use the eight raw matched source preview PNGs')

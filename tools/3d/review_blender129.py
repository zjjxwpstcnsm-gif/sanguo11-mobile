#!/usr/bin/env python3
"""Same-camera OFFLINE Blender review, v128->v129; never APK evidence.
blender -b --python-exit-code 1 --python tools/3d/review_blender129.py -- OUTPUT
The map-scale comparison is a fixed illustrative source layout, not a game map.
"""
import bpy,sys,json,math,struct,hashlib
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [];OUT=Path(args[0]).resolve() if args else ROOT
ART=OUT/'out/feedback129/blender';bpy.ops.wm.open_mainfile(filepath=str(ART/'landmarks-v129.blend'))
ns={'__file__':str(ROOT/'tools/3d/refine_blender121.py')}
source=(ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0].replace("bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)",'')
exec(compile(source,'strict subset import','exec'),ns)
scene=bpy.context.scene;camera=scene.camera;mat=bpy.data.materials['v129 atlas x authored vertex color (runtime V orientation)']
scene.render.resolution_x=720;scene.render.resolution_y=648;scene.cycles.samples=16
for ob in scene.objects:
    if ob.type=='MESH':ob.hide_render=True
families=['fall-wide','fall-hukou','cliff-granite','cliff-sandstone','cliff-karst','wall-earth','beacon-han','shore-reeds','shore-rock','fall-narrow'];records=[];before={}
def import_actual(path):
    ob=ns['import_subset'](path);raw=path.read_bytes();n=struct.unpack_from('<I',raw,12)[0];d=json.loads(raw[20:20+n]);a=d['accessors'][4];v=d['bufferViews'][a['bufferView']]
    vals=struct.unpack_from('<'+'f'*a['count']*3,raw,28+n+v.get('byteOffset',0));normals=[vals[k:k+3] for k in range(0,len(vals),3)]
    for poly in ob.data.polygons:poly.use_smooth=True
    ob.data.normals_split_custom_set_from_vertices(normals);ob.data.materials.append(mat);ob.hide_render=True;return ob
for family in families:
    before[family]=import_actual(ROOT/f'app/src/main/assets/3d/field/v128/{family}-lod0.glb');before[family].name='BEFORE128 '+family
    proxies=[]
    for src in [before[family],bpy.data.objects[family+'-lod0'],bpy.data.objects[family+'-lod1']]:
        ob=src.copy();ob.data=src.data.copy();scene.collection.objects.link(ob);ob.hide_render=True;ob.rotation_euler.x=math.pi/2
        if family.startswith('fall-'):ob.scale=(2.3,.53 if family=='fall-hukou' else .76,1.3)
        proxies.append(ob)
    bpy.context.view_layer.update();pts=[ob.matrix_world @ Vector(p) for ob in proxies for p in ob.bound_box]
    mn=Vector(tuple(min(p[k] for p in pts) for k in range(3)));mx=Vector(tuple(max(p[k] for p in pts) for k in range(3)));center=(mn+mx)*.5;size=max(mx-mn)
    camera.location=center+Vector((size*1.25,-size*1.65,size*1.2));camera.rotation_euler=(center-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.ortho_scale=size*1.58
    for state,ob in zip(['before','after','far'],proxies):
        ob.hide_render=False;scene.render.filepath=str(ART/(family+'-'+state+'-matched.png'));bpy.ops.render.render(write_still=True);ob.hide_render=True
        path=Path(scene.render.filepath);records.append({'family':family,'state':state,'path':str(path.relative_to(OUT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'kind':'OFFLINE_BLENDER_SOURCE_NOT_APK','camera':list(camera.location),'ortho_scale':camera.data.ortho_scale,'presentation_scale':list(ob.scale)})
    for ob in proxies:bpy.data.objects.remove(ob,do_unlink=True)
# Identical illustrative world-scale arrangement, no camera autoscale per state.
# 15 cliff instances emulate a dense chunk, not a claim about actual placements.
groundmat=bpy.data.materials.new('OFFLINE context ground');groundmat.diffuse_color=(.24,.30,.17,1)
bpy.ops.mesh.primitive_plane_add(size=30,location=(0,0,-.006));ground=bpy.context.object;ground.data.materials.append(groundmat)
layout=[]
for i in range(15):
    family=['cliff-granite','cliff-sandstone','cliff-karst'][i%3];x=(i%5-2)*1.45;y=(i//5-1)*1.45
    layout.append((family,(x,y,0),(.8,.8,.8),i*.58))
layout += [('fall-wide',(-.9,-3,0),(2.3,.76,1.3),0),('wall-earth',(2.1,-3,0),(1,1,1),-.35),('beacon-han',(2.4,-2.7,0),(1,1,1),-.35),('shore-reeds',(-1.9,-2.9,0),(.9,.9,.9),.4)]
scene.render.resolution_x=1080;scene.render.resolution_y=1232;scene.cycles.samples=24
center=Vector((0,-.5,0));camera.location=center+Vector((7,-10,13));camera.rotation_euler=(center-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.ortho_scale=10.0
for state in ['before','after']:
    shown=[]
    for family,loc,scale,angle in layout:
        src=before[family] if state=='before' else bpy.data.objects[family+'-lod0'];ob=src.copy();ob.data=src.data.copy();scene.collection.objects.link(ob);ob.hide_render=False;ob.location=loc;ob.rotation_euler=(math.pi/2,0,angle);ob.scale=scale;shown.append(ob)
    scene.render.filepath=str(ART/('map-scale-'+state+'.png'));bpy.ops.render.render(write_still=True)
    records.append({'family':'illustrative_15_cliff_map_scale','state':state,'path':str(Path(scene.render.filepath).relative_to(OUT)),'kind':'OFFLINE_ILLUSTRATIVE_LAYOUT_NOT_APK_NOT_GAME_MAP','camera':list(camera.location),'ortho_scale':camera.data.ortho_scale,'layout':layout})
    for ob in shown:bpy.data.objects.remove(ob,do_unlink=True)
(ART/'matched-review.json').write_text(json.dumps(records,indent=2)+'\n')
print('MATCHED_SOURCE_REVIEW_COMPLETE')
# Labels live in separate review sheets; raw source frames remain unaltered.
try:
    from PIL import Image,ImageDraw,ImageFont
    font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',22)
    small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',17)
    for family in families:
        sheet=Image.new('RGB',(1200,660),'#eeeeea');d=ImageDraw.Draw(sheet)
        d.text((15,8),'OFFLINE Blender source | '+family+' | NOT APK',fill='#222222',font=font)
        d.text((15,40),'Same camera, lighting, atlas and framing; geometry only. PC same-view reference missing.',fill='#444444',font=small)
        for c,state in enumerate(['before','after']):
            d.text((c*600+15,73),'BEFORE v128' if c==0 else 'AFTER v129',fill='#222222',font=font)
            im=Image.open(ART/(family+'-'+state+'-matched.png')).convert('RGB');im.thumbnail((592,540));sheet.paste(im,(c*600+4,110))
        sheet.save(ART/(family+'-before-after.png'))
    for group,names in [('hero',['fall-wide','cliff-granite','cliff-karst']),('earth',['cliff-sandstone','wall-earth','beacon-han']),('shore',['shore-reeds','shore-rock','fall-hukou'])]:
        sheet=Image.new('RGB',(1200,1230),'#eeeeea');d=ImageDraw.Draw(sheet);d.text((15,8),'OFFLINE Blender | same camera | geometry only | NOT APK',fill='#222222',font=font)
        for r,family in enumerate(names):
            for c,state in enumerate(['before','after','far']):
                y=55+r*390;d.text((c*400+8,y),family+' '+{'before':'v128','after':'v129','far':'v129 far'}[state],fill='#222222',font=font)
                im=Image.open(ART/(family+'-'+state+'-matched.png')).convert('RGB');im.thumbnail((392,352));sheet.paste(im,(c*400+4,y+32))
        sheet.save(ART/(group+'-comparison.png'))
    sheet=Image.new('RGB',(1600,1060),'#eeeeea');d=ImageDraw.Draw(sheet)
    d.text((16,8),'OFFLINE illustrative source layout, span10 | NOT APK, NOT an actual map',fill='#222222',font=font)
    d.text((16,40),'Same 15-cliff placement and camera. Cliff scale 0.8, representative of production 0.65-0.95 range.',fill='#444444',font=small)
    for c,state in enumerate(['before','after']):
        d.text((c*800+16,75),'BEFORE v128' if c==0 else 'AFTER v129',fill='#222222',font=font)
        im=Image.open(ART/('map-scale-'+state+'.png')).convert('RGB');im.thumbnail((790,946));sheet.paste(im,(c*800+5,110))
    sheet.save(ART/'map-scale-before-after.png')
except ImportError:
    print('Pillow absent; use raw renders and matched-review.json')

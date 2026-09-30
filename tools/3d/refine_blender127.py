#!/usr/bin/env python3
"""Blender4.2.3: authored spillway, steep falling sheet, rock support and foam basin.
Run: blender -b --python-exit-code 1 --python tools/3d/refine_blender127.py -- OUTPUT
Original CC0 geometry/texture; existing exporter; no gameplay/map edits.
"""
import bpy, math, json, hashlib, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
OUT=Path(args[0]) if args else ROOT
ns={'__file__':str(ROOT/'tools/3d/refine_blender121.py')}
exec(compile((ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0],'existing subset bake','exec'),ns)
atlas=OUT/'app/src/main/assets/3d/field/v127/scenery-atlas.png';atlas.parent.mkdir(parents=True,exist_ok=True)
image=bpy.data.images.load(str(ROOT/'app/src/main/assets/3d/field/v126/scenery-atlas.png'))
width,height=image.size;pixels=list(image.pixels[:])
for y in range(height):
    for panel in [3,4,5]:
        for k in range(width//8):
            u=k/(width//8);t=y/height
            grain=math.sin(u*53+t*67)*math.sin(u*97-t*31)
            if panel==4:
                if t<.65:
                    strata=.025*math.sin(t*47+math.sin(u*19)*1.1)
                    c=(.19+strata+.013*grain,.205+strata+.013*grain,.18+strata+.011*grain)
                else:
                    bubbles=.04*math.sin(u*43+t*71)*math.sin(u*77-t*51)
                    c=(.61+bubbles,.68+bubbles,.65+bubbles)
            else:
                # Broken variable-width foam, not parallel full-height painted stripes.
                streak=(.5+.5*math.sin(u*39+math.sin(t*23+u*7)*1.6))**8
                broken=.35+.65*(.5+.5*math.sin(t*51+u*13))
                foam=.30*streak*broken+.035*grain
                base=(.29,.23,.13) if panel==3 else (.085,.20,.20)
                c=tuple(max(.02,min(.85,x+foam)) for x in base)
            i=(y*width+panel*width//8+k)*4;pixels[i:i+4]=[*c,1]
image.pixels=pixels;image.filepath_raw=str(atlas);image.file_format='PNG';image.save()
for family,breadth,panel in [('fall-narrow',.34,5),('fall-wide',.46,5),('fall-hukou',.46,3)]:
    for lod in [0,1]:
        p,f,c,uv=[],[],[],[];smooth=[]
        def vertex(x,y,z,color=(1,1,1),slot=panel,u=.5,v=.5):
            p.append((x,y,z));c.append((*color,1));uv.append(((slot+.06+.88*u)/8,.06+.88*v));return len(p)-1
        def face(indices,water=False):f.append(indices);smooth.append(water)
        rows=[0,.10,.22,.32,.34,.36,.38,.40,.42,.44,.46,.48,.50,.58,.72,.86,1] if lod==0 else [0,.22,.34,.38,.42,.46,.50,.72,1]
        columns=8 if lod==0 else 4
        for t in rows:
            fall=1 if t<=.34 else max(.015,1-(t-.34)/.16)
            w=breadth*(1-.15*math.sin(t*math.pi))*(1+.09*t)
            for k in range(columns+1):
                u=k/columns;vertex((u-.5)*w,fall+.004*math.sin(u*31+t*29),t,(.97,.99,1),u=u,v=t)
        for j in range(len(rows)-1):
            for k in range(columns):
                a=j*(columns+1)+k;face((a,a+1,a+columns+2,a+columns+1),True)
        # A continuous irregular rock shelf supports the elevated headwater.
        # Its front ends behind the falling sheet, avoiding a floating water slab.
        sides=12 if lod==0 else 8;start=len(p)
        for ring in range(3):
            for k in range(sides):
                a=k*math.tau/sides
                x=math.cos(a)*breadth*.68*(1+.055*math.sin(k*2.7))
                z=.15+math.sin(a)*.175
                y=0 if ring==0 else .49 if ring==1 else .96+.006*math.sin(k*1.8)
                vertex(x,y,z,(.88,.92,.89),4,k/sides,.10+.33*ring/2)
        for j in range(2):
            for k in range(sides):
                a=start+j*sides+k;b=start+j*sides+(k+1)%sides;face((a,b,b+sides,a+sides))
        face(tuple(start+2*sides+k for k in range(sides)))
        # Low irregular annular foam at impact; no billboard mist/transparent fog.
        sides=24 if lod==0 else 12;start=len(p)
        for ring in [0,1]:
            for k in range(sides):
                a=k*math.tau/sides;r=(.50 if ring==0 else 1)*(1+.085*math.sin(k*2.1))
                x=math.cos(a)*breadth*.63*r;z=.91+math.sin(a)*.17*r
                color=(.70,.80,.78) if ring==0 else (.96,.99,.98)
                vertex(x,.018+.004*math.sin(k),z,color,4,.5+.40*math.cos(a),.82+.09*math.sin(a))
        for k in range(sides):
            a=start+k;b=start+(k+1)%sides;face((a,b,b+sides,a+sides),True)
        obj=ns['mesh_object'](family+'-lod'+str(lod),p,f,c,uv)
        for poly,value in zip(obj.data.polygons,smooth):poly.use_smooth=value
        ns['export'](obj,OUT/f'app/src/main/assets/3d/field/v127/{family}-lod{lod}.glb',atlas,'original steep spillway with supported rock shelf and annular impact foam; PC exact reference missing')
        assert ns['REPORT'][-1]['triangles']<700
report={'blender':bpy.app.version_string,'generator':'tools/3d/refine_blender127.py','license':'CC0-1.0 original waterfalls and water/rock texture; retained project atlas panels','assets':ns['REPORT'],'atlas':{'path':str(atlas.relative_to(OUT)),'sha256':hashlib.sha256(atlas.read_bytes()).hexdigest(),'bytes':atlas.stat().st_size},'retained_panels':[0,1,2,6,7]}
path=OUT/'docs/native-pc-visual/feedback-v127-blender-assets.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(report,indent=2)+'\n')
folder=OUT/'out/feedback127/blender';folder.mkdir(parents=True,exist_ok=True);bpy.ops.file.pack_all();bpy.ops.wm.save_as_mainfile(filepath=str(folder/'waterfalls-v127.blend'))
print(json.dumps({'status':'EXPORTED','models':6,'triangles':sum(x['triangles'] for x in ns['REPORT'])}))

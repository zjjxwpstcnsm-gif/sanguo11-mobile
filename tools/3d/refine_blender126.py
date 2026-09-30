#!/usr/bin/env python3
"""Original landscape landmarks; Blender 4.2.3, existing strict GLB bake.
Run: blender -b --python-exit-code 1 --python tools/3d/refine_blender126.py -- OUTPUT
Coordinates +Y up, walls/downstream +Z. No extracted game resources.
"""
import json, math, sys, hashlib
from pathlib import Path
import bpy
ROOT=Path(__file__).resolve().parents[2]
args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
OUT=Path(args[0]) if args else ROOT
ns={'__file__':str(ROOT/'tools/3d/refine_blender121.py')}
exec(compile((ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0],'existing subset exporter','exec'),ns)
atlas=OUT/'app/src/main/assets/3d/field/v126/scenery-atlas.png'
atlas.parent.mkdir(parents=True,exist_ok=True)
image=bpy.data.images.load(str(ROOT/'app/src/main/assets/3d/field/v125/scenery-atlas.png'))
width,height=image.size;pixels=list(image.pixels[:])
# Slot 3 is reserved for this river. Other seven panels retain their source pixels.
for y in range(height):
    for x in range(width*3//8,width*4//8):
        u=(x-width*3//8)/(width/8);t=y/height
        foam=(.5+.5*math.sin(u*45+math.sin(t*15)*.6))**14
        noise=.012*math.sin(u*73+t*35)
        color=(.29+.25*foam+noise,.20+.29*foam+noise,.085+.31*foam+noise)
        i=(y*width+x)*4;pixels[i:i+4]=[*color,1]
image.pixels=pixels;image.filepath_raw=str(atlas);image.file_format='PNG';image.save()
objects=[]
for name in ['wall-earth','beacon-han','cliff-sandstone','cliff-granite','cliff-karst','cascade-hukou']:
    for lod in range(2):
        p,f,c,uv=[],[],[],[]
        def v(x,y,z,color=(1,1,1),panel=0,u=.5,t=.5):
            p.append((x,y,z));c.append((*color,1));uv.append(((panel+.06+.88*u)/8,.06+.88*t))
        def taper(cx,cz,width,depth,bottom,top,taper=.78,color=(1,.91,.73)):
            start=len(p)
            for y,scale in [(bottom,1),(top,taper)]:
                for x,z in [(-1,-1),(1,-1),(1,1),(-1,1)]:v(cx+x*width*scale/2,y,cz+z*depth*scale/2,color,0,(x+1)/2,(y*8)%1)
            f.extend([(start,start+3,start+2,start+1),(start+4,start+5,start+6,start+7)])
            for k in range(4):f.append((start+k,start+(k+1)%4,start+(k+1)%4+4,start+k+4))
        if name=='wall-earth':
            # Curved, eroded crown and visible rammed-earth courses, not a Ming tower.
            slices=14 if lod==0 else 7;layers=6 if lod==0 else 3
            for side in [-1,1]:
                start=len(p)
                for j in range(layers+1):
                    t=j/layers
                    for k in range(slices+1):
                        z=k/slices;crest=.20+.015*math.sin(z*11)+.009*math.sin(z*29)
                        x=side*(.055-.019*t)+.006*math.sin(z*10)
                        color=(.98-.13*(j%2),.87-.10*(j%2),.69-.08*(j%2))
                        v(x,t*crest,z,color,0,z,t)
                for j in range(layers):
                    for k in range(slices):
                        a=start+j*(slices+1)+k;face=(a,a+1,a+slices+2,a+slices+1)
                        f.append(face if side==1 else tuple(reversed(face)))
            stride=(layers+1)*(slices+1)
            for k in range(slices):
                a=layers*(slices+1)+k;b=a+stride;f.append((a,b,b+1,a+1))
            for k in [0,slices]:
                f.append(tuple([j*(slices+1)+k for j in range(layers+1)]+[stride+j*(slices+1)+k for j in reversed(range(layers+1))]))
            if lod==0:
                for z in [.18,.52,.83]:taper(.054,z,.025,.065,0,.045,.8,(.66,.61,.50))
        elif name=='beacon-han':
            levels=8 if lod==0 else 4
            for j in range(levels):
                y=j*.34/levels;size=.23-.10*j/levels
                taper(0,0,size,size,y,y+.34/levels,.95,(.99-.08*(j%2),.87-.06*(j%2),.67-.05*(j%2)))
            # Low parapet, recessed dark fire platform and log pile.
            taper(0,0,.13,.13,.34,.35,1,(.37,.35,.30))
            for side in [-1,1]:
                taper(side*.065,0,.018,.15,.34,.40,1)
                taper(0,side*.065,.12,.018,.34,.40,1)
            if lod==0:
                for j in range(3):taper((j-1)*.025,0,.017,.075,.35,.365,1,(.32,.27,.19))
        elif name.startswith('cliff-'):
            sides=14 if lod==0 else 8;rings=7 if lod==0 else 4
            for j in range(rings):
                t=j/(rings-1)
                for k in range(sides):
                    a=k*math.tau/sides
                    if name=='cliff-sandstone':radius=(1-.35*t)*(.92+.07*math.sin(j*2.7));height=.31;col=(.99,.91,.77)
                    elif name=='cliff-granite':radius=(1-.50*t)*(.86+.10*math.sin(k*2.3+j));height=.38;col=(.84,.91,.94)
                    else:radius=(1-.68*t)*(.91+.10*math.cos(k*2.1));height=.49;col=(.83,.88,.80)
                    shade=.82+.14*t-.07*(j%2)
                    v(math.cos(a)*.30*radius+t*.04,height*t,math.sin(a)*.21*radius,tuple(x*shade for x in col),0,k/sides,t)
            for j in range(rings-1):
                for k in range(sides):
                    a=j*sides+k;b=j*sides+(k+1)%sides;f.append((a,b,b+sides,a+sides))
            f.append(tuple((rings-1)*sides+k for k in range(sides)))
        else:
            rows=24 if lod==0 else 12;cols=10 if lod==0 else 5
            for j in range(rows+1):
                t=j/rows
                # Broad upper lip converges into a rock-slot throat, then fans at foot.
                breadth=.33-.16*math.sin(t*math.pi)+.025*t
                drop=1 if t<.14 else max(0,1-((t-.14)/.86)**.62)
                for k in range(cols+1):
                    u=k/cols;v((u-.5)*breadth,drop+.007*math.sin(u*34+t*19),t,(.95,.95,.95),3,u,t)
            for j in range(rows):
                for k in range(cols):
                    a=j*(cols+1)+k;f.append((a,a+1,a+cols+2,a+cols+1))
        obj=ns['mesh_object'](name+'-lod'+str(lod),p,f,c,uv)
        for poly in obj.data.polygons:poly.use_smooth=name.startswith('cascade')
        ns['export'](obj,OUT/f'app/src/main/assets/3d/field/v126/{name}-lod{lod}.glb',atlas,'original layered cliff / rammed-earth wall / Han beacon / convergent silt waterfall; geographic interpretation, PC reference missing')
        assert ns['REPORT'][-1]['triangles']<750
        objects.append(obj)
manifest={'blender':bpy.app.version_string,'generator':'tools/3d/refine_blender126.py','license':'CC0-1.0 original geometry and silt texture; existing project atlas','assets':ns['REPORT'],'atlas':{'path':str(atlas.relative_to(OUT)),'sha256':hashlib.sha256(atlas.read_bytes()).hexdigest(),'bytes':atlas.stat().st_size}}
path=OUT/'docs/native-pc-visual/feedback-v126-blender-assets.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(manifest,indent=2)+'\n')
folder=OUT/'out/feedback126/blender';folder.mkdir(parents=True,exist_ok=True)
bpy.ops.file.pack_all();bpy.ops.wm.save_as_mainfile(filepath=str(folder/'landmarks-v126.blend'))
print(json.dumps({'status':'EXPORTED','models':len(objects),'triangles':sum(x['triangles'] for x in ns['REPORT'])}))

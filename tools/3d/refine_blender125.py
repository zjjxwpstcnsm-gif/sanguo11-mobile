#!/usr/bin/env python3
"""Original CC0 ledges, talus and stepped cascades, baked with Blender 4.2.3.
blender --background --python-exit-code 1 --python tools/3d/refine_blender125.py -- OUTPUT
Meshes use the existing strict single-primitive subset and merged landscape draws.
"""
import ast, json, math, hashlib, sys
from pathlib import Path
import bpy
ROOT=Path(__file__).resolve().parents[2]
args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
OUT=Path(args[0]) if args else ROOT
ns={'__file__':str(ROOT/'tools/3d/refine_blender121.py')}
exec(compile((ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0],'existing subset exporter','exec'),ns)
mesh_object,export=ns['mesh_object'],ns['export']
atlas_path=OUT/'app/src/main/assets/3d/field/v125/scenery-atlas.png';atlas_path.parent.mkdir(parents=True,exist_ok=True)
image=bpy.data.images.load(str(ROOT/'app/src/main/assets/3d/field/atlas.png'))
width,height=image.size;pixels=list(image.pixels[:])
for y in range(height):
    for x in range(width*5//8,width*6//8):
        u=(x-width*5//8)/(width/8);v=y/height
        foam=(.5+.5*math.sin(u*50+math.sin(v*18)*.6))**12
        noise=.5+.5*math.sin(u*81+v*73)
        # Blender pixels are linear, then saved as sRGB. Restrained blue-green
        # water with thin foam streaks avoids the previous overexposed white band.
        c=(.08+.35*foam,.24+.35*foam,.29+.35*foam)
        i=(y*width+x)*4;pixels[i:i+4]=[min(1,k+.018*noise) for k in c]+[1]
image.pixels=pixels;image.filepath_raw=str(atlas_path);image.file_format='PNG';image.save()
objects=[]
for kind in ['rock-ledge','rock-talus','cascade-narrow','cascade-wide']:
    for lod in range(2):
        p,f,c,uv=[],[],[],[]
        def vertex(x,y,z,color,panel,u,v):
            p.append((x,y,z));c.append((*color,1));uv.append(((panel+.06+.88*u)/8,.06+.88*v))
        def rock(cx,cz,sx,sy,sz,phase):
            start=len(p);sides=10 if lod==0 else 6;rings=5 if lod==0 else 3
            for ring in range(rings):
                t=ring/(rings-1);radius=(1-.48*t)*(.85+.14*math.sin(t*math.pi*4+phase))
                for k in range(sides):
                    a=k*math.tau/sides;irregular=1+.13*math.sin(k*2.37+phase)
                    shade=.73+.16*t+.05*math.cos(a)
                    vertex(cx+math.cos(a)*sx*radius*irregular+t*.045,sy*t,cz+math.sin(a)*sz*radius*irregular,(shade,shade*.98,shade*.92),0,k/sides,t)
            for r in range(rings-1):
                for k in range(sides):
                    a=start+r*sides+k;b=start+r*sides+(k+1)%sides;f.append((a,b,b+sides,a+sides))
            f.append(tuple(start+(rings-1)*sides+k for k in range(sides)))
        if kind=='rock-ledge':
            rock(-.06,0,.34,.32,.20,.7);rock(.22,.06,.21,.18,.15,2.1)
        elif kind=='rock-talus':
            for i in range(6 if lod==0 else 3):
                a=i*2.4;r=.10+.026*i;rock(math.cos(a)*r,math.sin(a)*r,.10+.012*(i%3),.09+.025*(i%3),.075+i*.006,i*.71)
        else:
            # Width X, height Y and downstream Z are normalized independently.
            # Water ribbons have irregular lips and a shallow fan of foam at foot.
            columns=8 if lod==0 else 4;rows=20 if lod==0 else 10
            breadth=.18 if kind=='cascade-narrow' else .32
            for r in range(rows+1):
                t=r/rows;drop=1-t+.022*math.sin(t*math.pi*8)*(1-t)
                for k in range(columns+1):
                    u=k/columns;spread=1+.23*t
                    x=(u-.5)*breadth*spread+.01*math.sin(t*17+u*5)
                    y=max(0,drop+.008*math.sin(u*30+t*7))
                    shade=.87+.11*math.sin(u*37+t*2)**2
                    vertex(x,y,t,(shade,shade,shade),5,u,t)
            for r in range(rows):
                for k in range(columns):
                    a=r*(columns+1)+k;f.append((a,a+1,a+columns+2,a+columns+1))
            # Thin, opaque foam patches: bounded geometry, no billboard fog.
            start=len(p)
            for k in range(12 if lod==0 else 8):
                a=k*math.tau/(12 if lod==0 else 8)
                vertex(math.cos(a)*breadth*.66,.025,1+math.sin(a)*.10,(.98,.99,1),5,.5+.3*math.cos(a),.5+.3*math.sin(a))
            f.append(tuple(range(start,len(p))))
        obj=mesh_object(f'{kind}-lod{lod}',p,f,c,uv)
        for poly in obj.data.polygons:poly.use_smooth=kind.startswith('cascade')
        export(obj,OUT/f'app/src/main/assets/3d/field/v125/{kind}-lod{lod}.glb',atlas_path,'original layered ledge, broken talus or stepped ribbon cascade; no PC resource extraction')
        objects.append(obj)
        assert ns['REPORT'][-1]['triangles']<600
manifest={'blender':bpy.app.version_string,'generator':'tools/3d/refine_blender125.py','license':'CC0-1.0 original geometry and authored cascade texture; retained project CC0 atlas','assets':ns['REPORT'],'atlas':{'path':str(atlas_path.relative_to(OUT)),'sha256':hashlib.sha256(atlas_path.read_bytes()).hexdigest(),'bytes':atlas_path.stat().st_size}}
path=OUT/'docs/native-pc-visual/feedback-v125-blender-assets.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(manifest,indent=2)+'\n')
folder=OUT/'out/feedback125/blender';folder.mkdir(parents=True,exist_ok=True)
bpy.ops.file.pack_all();bpy.ops.wm.save_as_mainfile(filepath=str(folder/'terrain-v125.blend'))
print(json.dumps({'status':'EXPORTED','models':len(objects),'triangles':sum(x['triangles'] for x in ns['REPORT'])}))

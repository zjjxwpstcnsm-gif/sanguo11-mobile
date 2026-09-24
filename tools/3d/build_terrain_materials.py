#!/usr/bin/env python3
"""Original CC0 periodic grass blades, soil grains, dune ripples and fractured rock.
Python 3, numpy, Pillow. No downloaded imagery or baked directional lighting.
"""
from pathlib import Path
import numpy as np
from PIL import Image
import hashlib,json
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'app/src/main/assets/3d/terrain';OUT.mkdir(parents=True,exist_ok=True)
N=512
y,x=np.mgrid[0:N,0:N]/N
rng=np.random.default_rng(31010)
def noise(bands):
    v=np.zeros((N,N))
    for freq,amplitude in bands:
        for _ in range(12):
            a,b=rng.integers(-freq,freq+1,2);phase=rng.uniform(0,6.283185)
            v+=amplitude*np.sin(2*np.pi*(a*x+b*y)+phase)/12
    return v
macro=noise([(3,1),(8,.4)])
records=[]
for name,base,rough in [('grass',[105,116,78],.91),('soil',[123,108,86],.96),('sand',[151,139,108],.86),('rock',[123,125,113],.82)]:
    fine=noise([(24,.8),(60,.3)])
    if name=='grass':
        h=.35*macro+.2*fine
        # Toroidal individual tapered blades, varied orientation, no directional cast shadow.
        for _ in range(1700):
            cx,cy=rng.random(2);angle=rng.uniform(-np.pi,np.pi);length=rng.uniform(.008,.04)
            dx=(x-cx+.5)%1-.5;dy=(y-cy+.5)%1-.5
            u=dx*np.cos(angle)-dy*np.sin(angle);v=dx*np.sin(angle)+dy*np.cos(angle)
            blade=np.maximum(0,1-np.abs(u)/.002)*np.maximum(0,1-np.abs(v)/length)
            h+=blade*rng.uniform(.12,.5)
    elif name=='soil': h=.5*macro+.65*fine+np.maximum(0,noise([(42,1)])-.1)*1.5
    elif name=='sand':
        # Fine grains and broad deposits; no regular dune sine train at every repeat.
        h=.20*macro+.18*fine+noise([(90,.08)])
        for _ in range(240):
            cx,cy=rng.random(2);dx=(x-cx+.5)%1-.5;dy=(y-cy+.5)%1-.5
            h+=rng.uniform(.03,.12)*np.exp(-(dx*dx+dy*dy)/rng.uniform(.000003,.00002))
    else:
        points=rng.random((35,2));dist=[]
        for cx,cy in points:
            dx=(x-cx+.5)%1-.5;dy=(y-cy+.5)%1-.5;dist.append(dx*dx+dy*dy)
        nearest=np.sort(dist,axis=0)[:2];crack=np.exp(-(nearest[1]-nearest[0])*2000)
        h=.4*macro+.17*fine-.65*crack
    rgb=np.clip(np.array(base)[None,None,:]*(1+h[:,:,None]*.28),0,255).astype('uint8')
    # Central wrap derivatives ensure matching periodic normal detail across repeat seam.
    dx=(np.roll(h,-1,axis=1)-np.roll(h,1,axis=1))*1.1
    dy=(np.roll(h,-1,axis=0)-np.roll(h,1,axis=0))*1.1
    normal=np.stack([-dx,-dy,np.ones_like(h)],axis=2);normal/=np.linalg.norm(normal,axis=2)[:,:,None]
    nr=np.dstack([normal*.5+.5,np.clip(rough+h*.05,.65,1)])
    for suffix,arr,space,channels in [('color',rgb,'sRGB','RGB albedo'),('normal_roughness',(nr*255).astype('uint8'),'linear','RGB tangent normal; A roughness')]:
        path=OUT/f'{name}_{suffix}.png';Image.fromarray(arr).save(path)
        records.append(dict(path=str(path.relative_to(ROOT)),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),bytes=path.stat().st_size,resolution=[N,N],colorSpace=space,channels=channels,license='CC0-1.0',source='Original procedural build_terrain_materials.py',consumer='FilamentMapView.loadGroundMaterials',mips='runtime generateMipmaps; full periodic texture per sampler; REPEAT',compression='PNG lossless / GPU SRGB8_A8 or RGBA8'))
p=ROOT/'docs/3d/world-art/assets-manifest.json';p.parent.mkdir(parents=True,exist_ok=True)
manifest=json.loads(p.read_text()) if p.exists() else {}
manifest.update(dict(version=2,scale='world UV: primary .35 repeats/unit, secondary rotated .35*.731; identical all qualities',resolutionReason='512 texels, ~179 texels/world unit primary; 8 RGBA8 textures with full mip chains ~10.67 MiB',assets=records))
p.write_text(json.dumps(manifest,indent=2)+'\n')

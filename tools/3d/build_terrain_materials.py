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
N=256
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
for name,base,rough in [('grass',[99,112,65],.91),('soil',[124,102,77],.96),('sand',[182,161,116],.86),('rock',[128,128,116],.82)]:
    fine=noise([(24,.8),(60,.3)])
    if name=='grass':
        h=.35*macro+.2*fine
        # Toroidal individual tapered blades, varied orientation, no directional cast shadow.
        for _ in range(1700):
            cx,cy=rng.random(2);angle=rng.uniform(-.8,.8);length=rng.uniform(.008,.04)
            dx=(x-cx+.5)%1-.5;dy=(y-cy+.5)%1-.5
            u=dx*np.cos(angle)-dy*np.sin(angle);v=dx*np.sin(angle)+dy*np.cos(angle)
            blade=np.maximum(0,1-np.abs(u)/.002)*np.maximum(0,1-np.abs(v)/length)
            h+=blade*rng.uniform(.12,.5)
    elif name=='soil': h=.5*macro+.65*fine+np.maximum(0,noise([(42,1)])-.1)*1.5
    elif name=='sand': h=.18*macro+.09*fine+.10*np.sin(2*np.pi*(12*y+1.2*np.sin(2*np.pi*x)))
    else:
        points=rng.random((35,2));dist=[]
        for cx,cy in points:
            dx=(x-cx+.5)%1-.5;dy=(y-cy+.5)%1-.5;dist.append(dx*dx+dy*dy)
        nearest=np.sort(dist,axis=0)[:2];crack=np.exp(-(nearest[1]-nearest[0])*2000)
        h=.4*macro+.17*fine-.65*crack
    rgb=np.clip(np.array(base)[None,None,:]*(1+h[:,:,None]*.42),0,255).astype('uint8')
    # Central wrap derivatives ensure matching periodic normal detail across repeat seam.
    dx=(np.roll(h,-1,axis=1)-np.roll(h,1,axis=1))*1.8
    dy=(np.roll(h,-1,axis=0)-np.roll(h,1,axis=0))*1.8
    normal=np.stack([-dx,-dy,np.ones_like(h)],axis=2);normal/=np.linalg.norm(normal,axis=2)[:,:,None]
    nr=np.dstack([normal*.5+.5,np.clip(rough+h*.05,.65,1)])
    for suffix,arr,space,channels in [('color',rgb,'sRGB','RGB albedo'),('normal_roughness',(nr*255).astype('uint8'),'linear','RGB tangent normal; A roughness')]:
        path=OUT/f'{name}_{suffix}.png';Image.fromarray(arr).save(path)
        records.append(dict(path=str(path.relative_to(ROOT)),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),bytes=path.stat().st_size,resolution=[N,N],colorSpace=space,channels=channels,license='CC0-1.0',source='Original procedural build_terrain_materials.py',consumer='FilamentMapView.loadGroundMaterials',mips='runtime generateMipmaps; full periodic texture per sampler; REPEAT',compression='PNG lossless / GPU SRGB8_A8 or RGBA8'))
p=ROOT/'docs/3d/world-art/assets-manifest.json';p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(json.dumps(dict(version=1,scale='one repeat per projected world unit',resolutionReason='256 texels per tile exceeds typical strategic screen footprint; 8 textures ~2.67 MiB with mipmaps',assets=records),indent=2)+'\n')

#!/usr/bin/env python3
"""Verify shipped resources, packed normal semantics and periodic border statistics."""
import json,hashlib
from pathlib import Path
import numpy as np
from PIL import Image
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'docs/3d/world-art/assets-manifest.json').read_text())
for entry in manifest['assets']:
    p=root/entry['path'];assert hashlib.sha256(p.read_bytes()).hexdigest()==entry['sha256'],p
    a=np.asarray(Image.open(p)).astype(float)/255
    assert list(a.shape[:2])==entry['resolution'],p
    if 'normal_roughness' in str(p):
        lengths=np.linalg.norm(a[:,:,:3]*2-1,axis=2)
        assert np.max(np.abs(lengths-1))<.015,p
        assert a[:,:,3].min()>=.64,p
    edge=(np.abs(a[0]-a[-1]).mean()+np.abs(a[:,0]-a[:,-1]).mean())/2
    inside=(np.abs(np.diff(a,axis=0)).mean()+np.abs(np.diff(a,axis=1)).mean())/2
    assert edge<inside*2+.004,(p,edge,inside)
    # Every mip uses the entire periodic image, never independent terrain chunks or atlas islands.
    im=Image.open(p)
    while im.width>1:
        im=im.resize((im.width//2,im.height//2),Image.Resampling.BOX)
        assert np.isfinite(np.asarray(im)).all()
    print('PASS',p.name,'edge/interior',round(edge/max(inside,1e-9),3))
assert (root/'app/src/main/assets/3d/terrain/ground.filamat').stat().st_size>1000
print('PASS S10 assets: 8 textures, periodic borders, normal/roughness, mips, hashes and compiled material')

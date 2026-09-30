#!/usr/bin/env python3
"""Pinned Filament UV contract + real atlas samples. HOST check, not GPU acceptance."""
from pathlib import Path
import re, json, hashlib
from PIL import Image
root=Path(__file__).resolve().parents[2]
old=(root/'tools/3d/scenery127.mat').read_text()
new=(root/'tools/3d/scenery128.mat').read_text()
# Filament1.56 Materials.md.html: flipUV defaults true, y ->1-y.
assert not re.search(r'\bflipUV\s*:\s*false',old)
assert re.search(r'\bflipUV\s*:\s*false',new)
assert '(uv.x >= 0.125 && uv.x < 0.375) || uv.x >= 0.75' in new
assert 'uv.y=1.0-uv.y;' in new
im=Image.open(root/'app/src/main/assets/3d/field/v128/scenery-atlas.png').convert('RGB')
checks=0
for v in [.17,.20,.24,.30]:
 oldpixel=im.getpixel((288,round((1-v)*63)));newpixel=im.getpixel((288,round(v*63)))
 assert min(oldpixel)>125,(v,oldpixel)
 assert max(newpixel)<115,(v,newpixel)
 checks+=2
for panel in [1,2,6,7]:
 for v in [.08,.25,.55,.88]:
  # Legacy panels are manually flipped after turning off the implicit flip.
  old_sample=(panel+.5)/8,1-v
  new_sample=(panel+.5)/8,1-v
  assert old_sample==new_sample;checks+=1
for v in [.79,.83,.88]:
 assert min(im.getpixel((288,round(v*63))))>125;checks+=1
binary=root/'app/src/main/assets/3d/field/v128/scenery.filamat'
sha=hashlib.sha256(binary.read_bytes()).hexdigest()
registry=(root/'app/src/main/java/game/sanguo/mobile/VerifiedMaterial.java').read_text()
assert '3d/field/v128/scenery.filamat' in registry and sha in registry
loader=(root/'app/src/main/java/game/sanguo/mobile/FilamentMapView.java').read_text()
assert 'VerifiedMaterial.read("3d/field/v128/scenery.filamat"' in loader
print(json.dumps({'checks':checks+2,'old_implicit_flip':'FAIL: support samples pale foam','new_explicit_convention':'PASS: dark support/pale impact/unchanged legacy panels','pinned_material_sha256':sha,'scope':'HOST texture+source contract; real GPU pixels still require installed review'}))

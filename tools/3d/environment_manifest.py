"""Update only the shipped environment asset section; preserve S10/S11 records."""
from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[2]
p=root/'docs/3d/world-art/assets-manifest.json';manifest=json.loads(p.read_text())
files=[root/'tools/3d/site.mat']
for group in ['sites','field']:
    files+=sorted((root/'app/src/main/assets/3d'/group).glob('*'))
manifest['environmentVersion']=1
manifest['environmentAssets']=[dict(path=str(f.relative_to(root)),sha256=hashlib.sha256(f.read_bytes()).hexdigest(),bytes=f.stat().st_size,
    source='tools/3d/build_environment.sh; original modular CC0 geometry; matc 1.56.0 (56)',license='CC0-1.0 assets; Apache-2.0 compiler',
    consumer='FilamentMapView / SiteGlb / FieldAssets',
    colorSpace='sRGB atlas; linear vertex colors and normal-aligned quaternion frames',
    normals='Generated on split face vertices at load; regenerate once per cached CPU pose',
    compression='GLB uncompressed float / uint32; atlas ETC2 SRGB8 plus PNG fallback; no normal-map textures',
    mips='atlas baked linear-light BOX; PNG fallback runtime mip chain') for f in files if f.is_file()]
p.write_text(json.dumps(manifest,indent=2)+'\n')
print('S12 manifest:',len(manifest['environmentAssets']),'files')

#!/usr/bin/env python3
"""Re-export in a clean destination; byte-compare every shipped model/atlas/rig.
Runtime exports are committed; this tool is a CI/review gate, not assemble dependency.
"""
import argparse, hashlib, json, os, struct, subprocess, sys, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def inventory(root):
 result=[]
 for family in ('sites','field'):
  atlas=root/f'app/src/main/assets/3d/{family}/atlas.png'
  for p in sorted((root/f'app/src/main/assets/3d/{family}').glob('*.glb')):
   b=p.read_bytes();n=struct.unpack_from('<I',b,12)[0];doc=json.loads(b[20:20+n]);base=28+n
   v=doc['bufferViews'][doc['images'][0]['bufferView']];embedded=b[base+v.get('byteOffset',0):base+v.get('byteOffset',0)+v['byteLength']]
   if embedded!=atlas.read_bytes():raise ValueError(f'{p.name}: embedded and runtime atlas differ')
   a=doc['accessors'][0];idx=doc['accessors'][3]
   result.append(dict(id=f'{family}/{p.stem}',version=4,runtime=str(p.relative_to(root)),sha256=digest(p),bytes=len(b),triangles=idx['count']//3,primitives=1,lod=int(p.stem[-1]) if '-lod' in p.stem else None,bounds=a['min']+a['max'],pivot=[0,0,0],axes='+Y up / +Z forward',unit='one projected tile span',license='CC0-1.0',author='project procedural asset source',source='tools/3d/build_sites.py' if family=='sites' else 'tools/3d/build_field_assets.py',texture=f'3d/{family}/atlas.png',textureSha256=digest(atlas),colorSpace='sRGB',animation='rigid parts v1; no skin/root motion' if p.stem.startswith('unit-') else 'static',mapping='SiteVisual.model' if family=='sites' else 'Vegetation.build' if p.stem.startswith(('tree','shrub','rock-')) else 'FieldAssets.unit/facility; construction/fire state'))
 return result
if __name__=='__main__':
 ap=argparse.ArgumentParser();ap.add_argument('--check',action='store_true');args=ap.parse_args()
 with tempfile.TemporaryDirectory(prefix='sanguo-assets-') as temp:
  out=Path(temp);env=dict(os.environ,ASSET_OUTPUT_ROOT=str(out))
  subprocess.run([sys.executable,str(ROOT/'tools/3d/build_field_assets.py')],env=env,check=True)
  subprocess.run([sys.executable,str(ROOT/'tools/3d/compress_atlases.py')],env=env,check=True)
  first={str(p.relative_to(out)):digest(p) for p in out.rglob('*') if p.is_file()}
  subprocess.run([sys.executable,str(ROOT/'tools/3d/build_field_assets.py')],env=env,check=True)
  subprocess.run([sys.executable,str(ROOT/'tools/3d/compress_atlases.py')],env=env,check=True)
  second={str(p.relative_to(out)):digest(p) for p in out.rglob('*') if p.is_file()}
  assert first==second,'nondeterministic export'
  for name in first:
   target=ROOT/name
   if args.check:
    assert target.is_file() and digest(target)==first[name],f'stale runtime output {name}'
   else:
    target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes((out/name).read_bytes())
  catalog={'schema':1,'cohort':'R08-v4','generatorDependencies':{'python':'3.11+','Pillow':'12.3.0','numpy':'2.3.5','etcpak':'0.9.15','texture2ddecoder':'1.0.6','Filament':'1.56.0 / matc56'},'sourceHashes':{s:digest(ROOT/s) for s in ['tools/3d/build_sites.py','tools/3d/build_field_assets.py','tools/3d/compress_atlases.py']},'assets':inventory(out)}
  path=ROOT/'docs/native-pc-visual/asset-catalog.json';text=json.dumps(catalog,indent=2)+'\n'
  if args.check:assert path.read_text()==text,'stale catalog'
  else:path.write_text(text)
 subprocess.run([sys.executable,str(ROOT/('scripts/verify-environment-assets.py' if args.check else 'tools/3d/environment_manifest.py'))],check=True)
 print(f'PASS reproducibility: {len(first)} byte-identical outputs; {len(catalog["assets"])} models; embedded/shared atlases match')

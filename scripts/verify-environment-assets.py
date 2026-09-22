from pathlib import Path
import json,hashlib
root=Path(__file__).resolve().parents[1]
m=json.loads((root/'docs/3d/world-art/assets-manifest.json').read_text())
for a in m['environmentAssets']:
    p=root/a['path']
    assert p.stat().st_size==a['bytes'],p
    assert hashlib.sha256(p.read_bytes()).hexdigest()==a['sha256'],p
assert 'shadingModel : lit' in (root/'tools/3d/site.mat').read_text()
for family in ['sites','field']:
    records=json.loads((root/f'docs/3d/{"site" if family=="sites" else "field"}-assets.json').read_text())['assets']
    for a in records:
        p=root/f'app/src/main/assets/3d/{family}'/a['file']
        assert hashlib.sha256(p.read_bytes()).hexdigest()==a['sha256'],p
print('PASS S12 shipped environment manifest:',len(m['environmentAssets']),'hashes and both GLB reports')

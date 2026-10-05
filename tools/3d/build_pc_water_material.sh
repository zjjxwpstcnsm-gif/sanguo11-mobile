#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to Filament1.56.0 matc}"
[ "$("$MATC" --version)" = 56 ] || { echo 'Requires matc56' >&2; exit 1; }
"$MATC" -p mobile -a opengl -o app/src/main/assets/3d/pc-map/water.filamat tools/3d/pc-water.mat
python3 - <<'PY'
from pathlib import Path
import hashlib,json,re
path=Path('app/src/main/assets/3d/pc-map/water.filamat')
digest=hashlib.sha256(path.read_bytes()).hexdigest()
pin=Path('app/src/main/java/game/sanguo/mobile/VerifiedMaterial.java')
text,count=re.subn(r'(EXPECTED.put\("3d/pc-map/water.filamat", ")[a-f0-9]{64}("\);)',lambda m:m[1]+digest+m[2],pin.read_text())
if count!=1:raise ValueError('Expected one PC water material pin')
pin.write_text(text)
manifest=Path('tools/content/map-release-manifest.json')
data=json.loads(manifest.read_text())
entries=[e for e in data['files'] if e['source_path']==str(path)]
if len(entries)>1:raise ValueError('Duplicate water integrity entry')
if not entries:
    data['files'].append(dict(apk_path='assets/3d/pc-map/water.filamat',source_path=str(path),sha256=digest))
else:entries[0]['sha256']=digest
manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
print(digest)
PY

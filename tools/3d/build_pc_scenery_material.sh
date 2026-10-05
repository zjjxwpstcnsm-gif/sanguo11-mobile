#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to Filament 1.56.0 matc}"
[ "$("$MATC" --version)" = 56 ] || { echo 'Requires Filament 1.56.0 / matc56' >&2; exit 1; }
"$MATC" -p mobile -a opengl -o app/src/main/assets/3d/pc-scenery/scenery.filamat tools/3d/pc-scenery.mat
python3 - <<'PY'
from pathlib import Path
import hashlib,json,re
path=Path('app/src/main/assets/3d/pc-scenery/scenery.filamat')
digest=hashlib.sha256(path.read_bytes()).hexdigest()
pin=Path('app/src/main/java/game/sanguo/mobile/VerifiedMaterial.java')
text=pin.read_text()
pattern=r'(EXPECTED.put\("3d/pc-scenery/scenery.filamat", ")[a-f0-9]{64}("\);)'
text,count=re.subn(pattern,lambda m:m[1]+digest+m[2],text)
if count!=1:raise ValueError('Expected one PC scenery material pin')
pin.write_text(text)
manifest=Path('tools/content/map-release-manifest.json')
data=json.loads(manifest.read_text())
entries=[e for e in data['files'] if e['source_path']==str(path)]
if len(entries)!=1:raise ValueError('Expected one material integrity entry')
entries[0]['sha256']=digest
manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
print(digest)
PY

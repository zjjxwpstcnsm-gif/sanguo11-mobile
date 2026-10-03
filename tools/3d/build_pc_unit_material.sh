#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to Filament 1.56.0 matc}"
[ "$("$MATC" --version)" = 56 ] || { echo 'Requires Filament 1.56.0 / matc56' >&2; exit 1; }
for variant in unit unit-alpha; do
  "$MATC" -p mobile -a opengl -o "app/src/main/assets/3d/pc-units/$variant.filamat" "tools/3d/pc-$variant.mat"
done
python3 - <<'PY'
from pathlib import Path
import hashlib,json,re
pin=Path('app/src/main/java/game/sanguo/mobile/VerifiedMaterial.java')
manifest=Path('tools/content/map-release-manifest.json')
text=pin.read_text(); data=json.loads(manifest.read_text())
for name in ('unit','unit-alpha'):
    relative='3d/pc-units/'+name+'.filamat'
    path=Path('app/src/main/assets')/relative
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    pattern=r'(EXPECTED.put\("'+re.escape(relative)+r'", ")[a-f0-9]{64}("\);)'
    text,count=re.subn(pattern,lambda m:m[1]+digest+m[2],text)
    if count!=1:raise ValueError('Expected one PC unit material pin: '+relative)
    entries=[e for e in data['files'] if e['source_path']==str(path)]
    if len(entries)!=1:raise ValueError('Expected one material integrity entry: '+relative)
    entries[0]['sha256']=digest
    print(relative,digest)
pin.write_text(text)
manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
PY

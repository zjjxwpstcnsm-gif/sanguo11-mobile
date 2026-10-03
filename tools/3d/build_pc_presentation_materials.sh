#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to Filament1.56.0 matc}"
[ "$("$MATC" --version)" = 56 ] || { echo 'Requires matc56' >&2; exit 1; }
mkdir -p app/src/main/assets/3d/pc-presentations
for blend in over add backdrop over-encoded add-encoded; do
  "$MATC" -p mobile -a opengl -o "app/src/main/assets/3d/pc-presentations/$blend.filamat" "tools/3d/pc-presentation-$blend.mat"
done
/usr/bin/python3 - <<'PY'
from pathlib import Path
import hashlib,json,re
manifest=Path('tools/content/map-release-manifest.json');data=json.loads(manifest.read_text())
pin=Path('app/src/main/java/game/sanguo/mobile/VerifiedMaterial.java');text=pin.read_text()
for blend in ('over','add','backdrop','over-encoded','add-encoded'):
    relative=f'3d/pc-presentations/{blend}.filamat';path=Path('app/src/main/assets')/relative
    digest=hashlib.sha256(path.read_bytes()).hexdigest();replacement=f'EXPECTED.put("{relative}", "{digest}");'
    pattern=r'EXPECTED.put\("'+re.escape(relative)+r'", "[a-f0-9]{64}"\);'
    if re.search(pattern,text):text=re.sub(pattern,lambda _:replacement,text)
    else:text=text.replace('    static {','    static {\n        '+replacement,1)
    entries=[e for e in data['files']if e['source_path']==str(path)]
    if len(entries)>1:raise ValueError('Duplicate presentation material pin')
    if entries:entries[0]['sha256']=digest
    else:data['files'].append(dict(apk_path='assets/'+relative,source_path=str(path),sha256=digest))
    print(blend,digest)
pin.write_text(text);manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
PY

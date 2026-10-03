#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to Filament1.56.0 matc}"
[ "$("$MATC" --version)" = 56 ] || { echo 'Requires matc56' >&2; exit 1; }
"$MATC" -p mobile -a opengl -o app/src/main/assets/3d/pc-effects/quad.filamat tools/3d/pc-effect-quad.mat
/usr/bin/python3 - <<'PY'
from pathlib import Path
import hashlib,json,re
relative='3d/pc-effects/quad.filamat'
path=Path('app/src/main/assets')/relative
digest=hashlib.sha256(path.read_bytes()).hexdigest()
pin=Path('app/src/main/java/game/sanguo/mobile/VerifiedMaterial.java')
text=pin.read_text();pattern=r'EXPECTED.put\("'+re.escape(relative)+r'", "[a-f0-9]{64}"\);'
replacement='EXPECTED.put("'+relative+'", "'+digest+'");'
if re.search(pattern,text):text=re.sub(pattern,lambda _:replacement,text)
else:text=text.replace('    static {','    static {\n        '+replacement,1)
pin.write_text(text)
manifest=Path('tools/content/map-release-manifest.json');data=json.loads(manifest.read_text())
entries=[e for e in data['files'] if e['source_path']==str(path)]
if len(entries)>1:raise ValueError('Duplicate effect material pin')
if entries:entries[0]['sha256']=digest
else:data['files'].append(dict(apk_path='assets/'+relative,source_path=str(path),sha256=digest))
manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
print(digest)
PY

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to Filament 1.56.0 matc}"
[ "$("$MATC" --version)" = 56 ] || { echo 'Requires matc56' >&2; exit 1; }
"$MATC" -p mobile -a opengl -o app/src/main/assets/3d/pc-map/ground.filamat tools/3d/pc-ground.mat
"$MATC" -p mobile -a opengl -o app/src/main/assets/3d/pc-map/ground-outline.filamat tools/3d/pc-ground-outline.mat
python3 - <<'PY'
from pathlib import Path
import hashlib,json,re
pin=Path('app/src/main/java/game/sanguo/mobile/VerifiedMaterial.java')
manifest=Path('tools/content/map-release-manifest.json');data=json.loads(manifest.read_text())
for name in ('ground','ground-outline'):
    path=Path('app/src/main/assets/3d/pc-map/'+name+'.filamat');digest=hashlib.sha256(path.read_bytes()).hexdigest()
    relative='3d/pc-map/'+name+'.filamat';source=pin.read_text()
    pattern=r'(EXPECTED.put\("'+re.escape(relative)+r'", ")[a-f0-9]{64}("\);)'
    text,count=re.subn(pattern,lambda m:m[1]+digest+m[2],source)
    if count==0 and name=='ground-outline':text=source.replace('    static {','    static {\n        EXPECTED.put("'+relative+'", "'+digest+'");')
    elif count!=1:raise ValueError('Expected one material pin:'+name)
    pin.write_text(text)
    entries=[e for e in data['files'] if e['source_path']==str(path)]
    if not entries:data['files'].append(dict(source_path=str(path),apk_path='assets/'+relative,sha256=digest))
    elif len(entries)==1:entries[0]['sha256']=digest
    else:raise ValueError('Duplicated material integrity entry')
    print(name,digest)
manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
PY

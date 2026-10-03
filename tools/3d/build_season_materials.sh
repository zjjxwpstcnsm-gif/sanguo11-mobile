#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to official Filament 1.56.0 matc}"
[ "$("$MATC" --version)" = 56 ]
for pair in ground:terrain/ground water:terrain/water site:sites/site unit:field/unit; do
  name=${pair%%:*}; target=${pair#*:}
  "$MATC" -p mobile -a opengl -o "app/src/main/assets/3d/$target.filamat" "tools/3d/$name.mat"
done
python3 tools/3d/environment_manifest.py
python3 - <<'PY'
from pathlib import Path
import hashlib,json
p=Path('docs/3d/world-art/assets-manifest.json');m=json.loads(p.read_text())
for a in m['assets']:
    if a.get('kind')=='material':
        f=Path(a['path']);a['sha256']=hashlib.sha256(f.read_bytes()).hexdigest();a['bytes']=f.stat().st_size
p.write_text(json.dumps(m,indent=2)+'\n')
p=Path('docs/native-pc-visual/assets.json');m=json.loads(p.read_text())
paths=['app/src/main/java/game/sanguo/mobile/SeasonStyle.java','app/src/main/java/game/sanguo/mobile/EnvironmentProfile.java']
for name,target in [('ground','terrain/ground'),('water','terrain/water'),('site','sites/site'),('unit','field/unit')]:
    paths+=['tools/3d/'+name+'.mat','app/src/main/assets/3d/'+target+'.filamat']
m['R14']={'profile':'ink-landscape-r14-v1','compiler':'Filament1.56.0/matc56, mobile OpenGL',
 'source':'Original project art parameters and shaders; not KOEI algorithms','license':'CC0-1.0 shaders / Apache-2.0 compiler',
 'consumer':'normal FilamentMapView date projection and material instances','colorSpace':'linear material constants, existing sRGB albedos',
 'textures':'existing R04/R10 textures retained; zero added sampler/texture/transparent pass',
 'snow':'none: no reliable geographic/season reference','art_status':'PARTIAL; PC seasonal references and physical GPU missing',
 'files':[{'path':s,'sha256':hashlib.sha256(Path(s).read_bytes()).hexdigest(),'bytes':Path(s).stat().st_size} for s in paths]}
p.write_text(json.dumps(m,indent=2)+'\n')
PY
python3 scripts/verify-terrain-materials.py
python3 scripts/verify-environment-assets.py

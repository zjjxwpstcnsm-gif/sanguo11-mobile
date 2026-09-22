#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to the official Filament v1.56.0 matc executable}"
[ "$("$MATC" --version)" = "56" ] || { echo 'Requires Filament 1.56.0 matc (version 56)' >&2; exit 1; }
python3 tools/3d/build_terrain_materials.py
"$MATC" -p mobile -a opengl -o app/src/main/assets/3d/terrain/ground.filamat tools/3d/ground.mat
python3 - <<'PY'
from pathlib import Path
import json,hashlib
p=Path('docs/3d/world-art/assets-manifest.json');m=json.loads(p.read_text())
for path in ['tools/3d/ground.mat','app/src/main/assets/3d/terrain/ground.filamat']:
 f=Path(path);m['assets'].append(dict(path=path,sha256=hashlib.sha256(f.read_bytes()).hexdigest(),bytes=f.stat().st_size,kind='material',source='tools/3d/ground.mat; official matc v1.56.0 (56), mobile OpenGL',license='original shader CC0-1.0; Filament compiler Apache-2.0',consumer='FilamentMapView.loadGroundMaterials'))
p.write_text(json.dumps(m,indent=2)+'\n')
PY
python3 scripts/verify-terrain-materials.py

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to official Filament 1.56.0 matc}"
[ "$("$MATC" --version)" = 56 ]
python3 tools/3d/build_field_assets.py
"$MATC" -p mobile -a opengl -o app/src/main/assets/3d/sites/site.filamat tools/3d/site.mat
python3 tools/3d/environment_manifest.py

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${MATC:?Set MATC to official Filament 1.56.0 matc}"
[ "$("$MATC" --version)" = 56 ] || { echo 'Requires Filament 1.56.0 / matc56' >&2; exit 1; }
mkdir -p app/src/main/assets/3d/terrain/v129
for source in ground ground-overview; do
  "$MATC" -p mobile -a opengl -o "app/src/main/assets/3d/terrain/v129/$source.filamat" "tools/3d/${source}129.mat"
done
sha256sum tools/3d/ground129.mat tools/3d/ground-overview129.mat app/src/main/assets/3d/terrain/v129/*.filamat

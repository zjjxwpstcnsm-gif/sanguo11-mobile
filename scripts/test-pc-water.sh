#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-pc-map.sh out/pc-visual/v146-water-source-reference.bin
java -Xmx1200m -cp app/build/pc-map-check:core/src/main/resources:core/src/test/resources game.sanguo.mobile.PcWaterRestorationTest "${1:-out/pc-visual/v146-water-coarse-source.bin}"

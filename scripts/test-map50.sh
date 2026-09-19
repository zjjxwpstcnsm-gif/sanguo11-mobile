#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-personnel49.sh
java -Dfile.encoding=UTF-8 -Xmx1200m -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.MapFidelity50Test
python3 tools/content/test_geography_v050.py "$@"
python3 scripts/verify-map-release.py

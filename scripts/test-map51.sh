#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-personnel49.sh
CP=core/build/check:core/src/main/resources:core/src/test/resources
java -Dfile.encoding=UTF-8 -Xmx1200m -cp "$CP" game.sanguo.core.MapFidelity50Test
java -Dfile.encoding=UTF-8 -Xmx1200m -cp "$CP" game.sanguo.core.MapFidelity51Test
python3 tools/content/test_geography_v051.py "$@"
python3 scripts/verify-map-release.py

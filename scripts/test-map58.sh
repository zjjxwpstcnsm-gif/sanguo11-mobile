#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Current strict suite also reconstructs signed historical057/058 checkpoints.
if grep -qx "versionName=0.59.0" version.properties; then exec bash scripts/test-map59.sh; fi
# Preserve inherited gameplay/geometry checks, updating only exact current-map goldens.
bash scripts/test-map57.sh
CP=core/build/map57:core/src/main/resources:core/src/test/resources
java -Xmx1500m -Dreference58.legacy57="${REFERENCE58_LEGACY57:-}" -Dreference58.baselineCanonical="${REFERENCE58_BASELINE_CANONICAL:-}" -cp "$CP" game.sanguo.core.Reference58Test
javac -encoding UTF-8 --release 17 -cp "$CP" -d core/build/map57 \
 app/src/main/java/game/sanguo/mobile/MapRaster.java \
 app/src/test/java/game/sanguo/mobile/Reference58ProjectionTest.java
java -Xmx1500m -cp "$CP" game.sanguo.mobile.Reference58ProjectionTest
python3 tools/content/audit_native58.py --check --report docs/validation/v058/source-grid-audit.json
python3 tools/content/test_audit58.py

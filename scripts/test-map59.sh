#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/map59
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/map59/sources.txt
javac -encoding UTF-8 --release 17 -d core/build/map59 @core/build/map59/sources.txt
CP=core/build/map59:core/src/main/resources:core/src/test/resources
for test in MapTap57Test NativeMap56Test CityFootprint55Test Reference58Test; do
 java -Xmx1500m -cp "$CP" "game.sanguo.core.$test"
done
java -Xmx1500m -Dreference59.legacy58="${REFERENCE59_LEGACY58:-}" -Dreference59.baselineCanonical="${REFERENCE59_BASELINE_CANONICAL:-}" -cp "$CP" game.sanguo.core.Reference59Test
java -Xmx1500m -cp "$CP" game.sanguo.core.PortReplayTest
javac -encoding UTF-8 --release 17 -cp "$CP" -d core/build/map59 \
 app/src/main/java/game/sanguo/mobile/{TileGeometry,MapCamera,TerrainArt,TerrainConnections,MapRaster}.java \
 app/src/test/java/game/sanguo/mobile/{MapTap57ProjectionTest,Reference58ProjectionTest}.java
java -Xmx1500m -cp "$CP" game.sanguo.mobile.MapTap57ProjectionTest
java -Xmx1500m -cp "$CP" game.sanguo.mobile.Reference58ProjectionTest
python3 tools/content/audit_native59.py --check --baseline57 core/build/map59/baseline57.properties --baseline58 core/build/map59/baseline58.properties
python3 tools/content/audit_native57.py --check --map-file core/build/map59/baseline57.properties
python3 tools/content/test_audit59.py
python3 scripts/verify-map-release.py

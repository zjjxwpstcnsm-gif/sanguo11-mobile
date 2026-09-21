#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/map61
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/map61/sources.txt
javac -encoding UTF-8 --release 17 -d core/build/map61 @core/build/map61/sources.txt
CP=core/build/map61:core/src/main/resources:core/src/test/resources
java -Xmx1500m -Dreference61.legacy="${REFERENCE61_LEGACY:-}" -Dreference61.requireLegacy="${REFERENCE61_REQUIRE_LEGACY:-false}" -cp "$CP" game.sanguo.core.Reference61Test
for test in MapTap57Test NativeMap56Test CityFootprint55Test Reference58Test Reference59Test;do java -Xmx1500m -cp "$CP" "game.sanguo.core.$test";done
java -Xmx1500m -Dreference60.legacy="${REFERENCE61_LEGACY:-}" -Dreference60.requireLegacy="${REFERENCE61_REQUIRE_LEGACY:-false}" -cp "$CP" game.sanguo.core.Reference60Test
java -Xmx1500m -cp "$CP" game.sanguo.core.PortReplayTest
javac -encoding UTF-8 --release 17 -cp "$CP" -d core/build/map61 \
 app/src/main/java/game/sanguo/mobile/{TileGeometry,MapCamera,TerrainArt,TerrainConnections,MapRaster}.java \
 app/src/test/java/game/sanguo/mobile/{MapTap57ProjectionTest,Reference58ProjectionTest,Reference60ProjectionTest,Reference61ProjectionTest}.java
for test in MapTap57ProjectionTest Reference58ProjectionTest Reference61ProjectionTest;do java -Xmx1500m -cp "$CP" "game.sanguo.mobile.$test";done
python3 tools/content/audit_native61.py --check --baseline60 core/build/map61/baseline60.properties
python3 tools/content/test_audit61.py
python3 tools/content/test_audit60.py
python3 tools/content/test_audit59.py
python3 scripts/verify-map-release.py

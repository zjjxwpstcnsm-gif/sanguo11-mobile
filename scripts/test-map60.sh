#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/map60
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/map60/sources.txt
javac -encoding UTF-8 --release 17 -d core/build/map60 @core/build/map60/sources.txt
CP=core/build/map60:core/src/main/resources:core/src/test/resources
for test in MapTap57Test NativeMap56Test CityFootprint55Test Reference58Test Reference59Test; do
 java -Xmx1500m -cp "$CP" "game.sanguo.core.$test"
done
java -Xmx1500m -Dreference60.legacy="${REFERENCE60_LEGACY:-}" -Dreference60.requireLegacy="${REFERENCE60_REQUIRE_LEGACY:-false}" -cp "$CP" game.sanguo.core.Reference60Test
java -Xmx1500m -cp "$CP" game.sanguo.core.PortReplayTest
javac -encoding UTF-8 --release 17 -cp "$CP" -d core/build/map60 \
 app/src/main/java/game/sanguo/mobile/{TileGeometry,MapCamera,TerrainArt,TerrainConnections,MapRaster}.java \
 app/src/test/java/game/sanguo/mobile/{MapTap57ProjectionTest,Reference58ProjectionTest,Reference60ProjectionTest}.java
for test in MapTap57ProjectionTest Reference58ProjectionTest Reference60ProjectionTest;do java -Xmx1500m -cp "$CP" "game.sanguo.mobile.$test";done
python3 tools/content/audit_native60.py --check --baseline59 core/build/map60/baseline59.properties
python3 tools/content/test_audit60.py
python3 scripts/verify-map-release.py

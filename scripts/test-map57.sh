#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/map57
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/map57/sources.txt
javac -encoding UTF-8 --release 17 -d core/build/map57 @core/build/map57/sources.txt
CP=core/build/map57:core/src/main/resources:core/src/test/resources
java -Xmx1500m -cp "$CP" game.sanguo.core.MapTap57Test
java -Xmx1500m -cp "$CP" game.sanguo.core.NativeMap56Test
java -Xmx1500m -cp "$CP" game.sanguo.core.CityFootprint55Test
javac -encoding UTF-8 --release 17 -cp "$CP" -d core/build/map57 \
 app/src/main/java/game/sanguo/mobile/TileGeometry.java \
 app/src/main/java/game/sanguo/mobile/MapCamera.java \
 app/src/main/java/game/sanguo/mobile/TerrainArt.java \
 app/src/main/java/game/sanguo/mobile/TerrainConnections.java \
 app/src/test/java/game/sanguo/mobile/MapTap57ProjectionTest.java
java -Xmx1500m -cp "$CP" game.sanguo.mobile.MapTap57ProjectionTest
python3 tools/content/audit_native58.py --check --baseline57 core/build/map57/baseline57.properties
python3 tools/content/audit_native57.py --check --map-file core/build/map57/baseline57.properties
python3 scripts/verify-map-release.py

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/native56
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/native56/sources.txt
javac -encoding UTF-8 --release 17 -d core/build/native56 @core/build/native56/sources.txt
java -Xmx1500m -cp core/build/native56:core/src/main/resources:core/src/test/resources game.sanguo.core.NativeMap56Test
javac -encoding UTF-8 --release 17 -cp core/build/native56 -d core/build/native56 app/src/main/java/game/sanguo/mobile/TileGeometry.java app/src/main/java/game/sanguo/mobile/MapCamera.java app/src/test/java/game/sanguo/mobile/AndroidProjectionTest.java
java -cp core/build/native56:core/src/main/resources game.sanguo.mobile.AndroidProjectionTest
java -Xmx1g -cp core/build/native56:core/src/main/resources game.sanguo.core.army.MarchScale56Benchmark

# Regression for fixture injection/report determinism, and for complete region membership.
java -cp core/build/native56:core/src/main/resources:core/src/test/resources game.sanguo.core.ContentIntegrationTest
python3 tools/content/validate_native56_evidence.py

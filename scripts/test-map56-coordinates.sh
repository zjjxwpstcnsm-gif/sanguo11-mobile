#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/map56-coordinates
find core/src/main/java -name '*.java' > core/build/map56-coordinates/sources.txt
for name in LegacyMapCoordinateCheckpointTest MapCoordinateTest Native56Checks; do echo core/src/test/java/game/sanguo/core/$name.java >> core/build/map56-coordinates/sources.txt; done
javac -encoding UTF-8 --release 17 -d core/build/map56-coordinates @core/build/map56-coordinates/sources.txt
java -cp core/build/map56-coordinates:core/src/main/resources game.sanguo.core.LegacyMapCoordinateCheckpointTest
java -cp core/build/map56-coordinates:core/src/main/resources game.sanguo.core.MapCoordinateTest

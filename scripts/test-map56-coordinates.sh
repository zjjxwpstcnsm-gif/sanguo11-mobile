#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/map56-coordinates
javac -encoding UTF-8 --release 17 -d core/build/map56-coordinates \
  core/src/main/java/game/sanguo/core/Hex.java \
  core/src/main/java/game/sanguo/core/SourceGridCoord.java \
  core/src/main/java/game/sanguo/core/MapCoordinates.java \
  core/src/test/java/game/sanguo/core/MapCoordinateTest.java
java -cp core/build/map56-coordinates game.sanguo.core.MapCoordinateTest

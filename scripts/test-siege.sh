#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=core/build/siege
mkdir -p "$out"
find core/src/main/java core/src/testFixtures/java -name '*.java' > "$out/sources.txt"
printf '%s\n' core/src/test/java/game/sanguo/core/SiegeRulesTest.java core/src/test/java/game/sanguo/core/FacilityProductionTest.java app/src/main/java/game/sanguo/mobile/SiegeOverlay.java app/src/test/java/game/sanguo/mobile/SiegeOverlayTest.java >> "$out/sources.txt"
javac -encoding UTF-8 --release 17 -d "$out" @"$out/sources.txt"
java -cp "$out:core/src/main/resources:core/src/test/resources" game.sanguo.core.SiegeRulesTest
java -cp "$out:core/src/main/resources:core/src/test/resources" game.sanguo.mobile.SiegeOverlayTest

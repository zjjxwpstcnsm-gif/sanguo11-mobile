#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=out/parity/city-displacement-check
mkdir -p "$out/classes"
rg --files core/src/main/java game-api/src/main/java game-runtime/src/main/java -g '*.java' > "$out/sources.txt"
cat >> "$out/sources.txt" <<'SOURCES'
core/src/testFixtures/java/game/sanguo/core/DisplacementFixture.java
core/src/test/java/game/sanguo/core/CityDisplacementTest.java
game-runtime/src/test/java/game/sanguo/runtime/CityDisplacementSessionTest.java
SOURCES
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d "$out/classes" @"$out/sources.txt"
java -Xmx768m -cp "$out/classes:core/src/main/resources" game.sanguo.core.CityDisplacementTest
java -Xmx768m -cp "$out/classes:core/src/main/resources" game.sanguo.runtime.CityDisplacementSessionTest

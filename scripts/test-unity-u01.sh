#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/u01-check
find core/src/main/java game-api/src/main/java game-runtime/src/main/java -name '*.java' > core/build/u01-sources.txt
printf '%s\n' game-runtime/src/test/java/game/sanguo/runtime/bridge/BridgeSessionTest.java game-runtime/src/test/java/game/sanguo/runtime/bridge/BridgeFixtureRecorder.java >> core/build/u01-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d core/build/u01-check @core/build/u01-sources.txt
java -cp core/build/u01-check:core/src/main/resources game.sanguo.runtime.bridge.BridgeSessionTest
java -cp core/build/u01-check:core/src/main/resources game.sanguo.runtime.bridge.BridgeFixtureRecorder > core/build/u01-fixture.json
cmp unity/Assets/Resources/U01_fixture.json core/build/u01-fixture.json

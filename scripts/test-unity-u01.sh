#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/u01-check
find core/src/main/java -name '*.java' > core/build/u01-sources.txt
printf '%s\n' core/src/test/java/game/sanguo/core/BridgeSessionTest.java core/src/test/java/game/sanguo/core/BridgeFixtureRecorder.java >> core/build/u01-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d core/build/u01-check @core/build/u01-sources.txt
java -cp core/build/u01-check:core/src/main/resources game.sanguo.core.BridgeSessionTest
java -cp core/build/u01-check:core/src/main/resources game.sanguo.core.BridgeFixtureRecorder > core/build/u01-fixture.json
cmp unity/Assets/Resources/U01_fixture.json core/build/u01-fixture.json

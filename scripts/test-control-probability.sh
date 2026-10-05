#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/control-check
find core/src/main/java core/src/testFixtures/java -name '*.java' > core/build/control-sources.txt
echo core/src/test/java/game/sanguo/core/ControlProbabilityTest.java >> core/build/control-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -d core/build/control-check @core/build/control-sources.txt
java -Xmx1g -cp core/build/control-check:core/src/main/resources game.sanguo.core.ControlProbabilityTest

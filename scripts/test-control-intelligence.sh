#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/intelligence-check
find core/src/main/java core/src/testFixtures/java -name '*.java' > core/build/intelligence-sources.txt
echo core/src/test/java/game/sanguo/core/ControlIntelligenceTest.java >> core/build/intelligence-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -d core/build/intelligence-check @core/build/intelligence-sources.txt
java -Xmx1g -cp core/build/intelligence-check:core/src/main/resources game.sanguo.core.ControlIntelligenceTest "$@"

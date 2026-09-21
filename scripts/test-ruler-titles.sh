#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/ruler-titles
find core/src/main/java core/src/testFixtures/java -name '*.java' > core/build/ruler-titles/sources.txt
printf '%s\n' core/src/test/java/game/sanguo/core/RulerTitleTest.java >> core/build/ruler-titles/sources.txt
javac -encoding UTF-8 --release 17 -d core/build/ruler-titles @core/build/ruler-titles/sources.txt
java -Dfile.encoding=UTF-8 -cp core/build/ruler-titles:core/src/main/resources:core/src/test/resources game.sanguo.core.RulerTitleTest

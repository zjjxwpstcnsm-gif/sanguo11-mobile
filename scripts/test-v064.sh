#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/v064
find core/src/main/java core/src/testFixtures/java -name '*.java' > core/build/v064-sources.txt
printf '%s\n' core/src/test/java/game/sanguo/core/Ux64Test.java core/src/test/java/game/sanguo/core/FacilityProductionTest.java >> core/build/v064-sources.txt
javac -encoding UTF-8 --release 17 -d core/build/v064 @core/build/v064-sources.txt
CP=core/build/v064:core/src/main/resources:core/src/test/resources
java -Dfile.encoding=UTF-8 -cp "$CP" game.sanguo.core.Ux64Test
java -Dfile.encoding=UTF-8 -cp "$CP" game.sanguo.core.FacilityProductionTest

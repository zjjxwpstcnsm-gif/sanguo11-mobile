#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/v066
find core/src/main/java core/src/testFixtures/java -name '*.java' > core/build/v066-sources.txt
printf '%s\n' core/src/test/java/game/sanguo/core/Governance66Test.java core/src/test/java/game/sanguo/core/CityFootprint55Test.java core/src/test/java/game/sanguo/core/FacilityProductionTest.java >> core/build/v066-sources.txt
javac -encoding UTF-8 --release 17 -d core/build/v066 @core/build/v066-sources.txt
java -cp core/build/v066:core/src/main/resources:core/src/test/resources game.sanguo.core.Governance66Test

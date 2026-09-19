#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/check
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/sources47.txt
javac -encoding UTF-8 --release 17 -d core/build/check @core/build/sources47.txt
CP=core/build/check:core/src/main/resources:core/src/test/resources
for suite in CoreTest PortReplayTest CampaignAiTest FacilityProductionTest SandTerrainTest ObjectiveOrdersTest; do
  java -Xmx1200m -cp "$CP" "game.sanguo.core.$suite"
done

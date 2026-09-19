#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/check
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/sources49.txt
javac -encoding UTF-8 --release 17 -d core/build/check @core/build/sources49.txt
CP=core/build/check:core/src/main/resources:core/src/test/resources
for suite in Personnel49Test CoreTest ContentTest ContentProfilesTest BalanceTest ArchitectureRulesTest CampaignAiTest Turn48Test PortReplayTest SandTerrainTest FacilityProductionTest ObjectiveOrdersTest DisplacementTest MobileShortcutsTest TerritoryAiTest Release42Test; do
  java -Dfile.encoding=UTF-8 -Xmx1200m -cp "$CP" "game.sanguo.core.$suite"
done
python3 tools/content/build_content.py --check
python3 tools/content/test_content.py

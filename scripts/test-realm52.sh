#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/check
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/check.sources
javac -encoding UTF-8 --release 17 -d core/build/check @core/build/check.sources
for test in Realm52Test Turn48Test Personnel49Test MapFidelity50Test MapFidelity51Test PortReplayTest ObjectiveOrdersTest DisplacementTest FacilityProductionTest ArchitectureRulesTest; do
  java -Dfile.encoding=UTF-8 -Xmx1500m -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.$test
done
python3 scripts/verify-map-release.py

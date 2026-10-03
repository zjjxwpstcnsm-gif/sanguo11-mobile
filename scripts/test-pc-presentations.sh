#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/pc-presentations-check
rg --files core/src/main/java game-api/src/main/java -g '*.java' > app/build/pc-presentations-sources.txt
for name in PcEffectCoordinates PcPresentationPlan PcPresentationTimeline PcPresentationClock PcPresentationLens CombatSequence CombatReplayLedger; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/pc-presentations-sources.txt
done
echo core/src/testFixtures/java/game/sanguo/core/PcCriticalsFixture.java >> app/build/pc-presentations-sources.txt
echo tools/content/PcCriticalsProbe.java >> app/build/pc-presentations-sources.txt
javac -encoding UTF-8 --release 17 -d app/build/pc-presentations-check @app/build/pc-presentations-sources.txt
java -Xmx1200m -cp app/build/pc-presentations-check:core/src/main/resources game.sanguo.mobile.PcCriticalsProbe app/src/main/assets/3d/pc-presentations

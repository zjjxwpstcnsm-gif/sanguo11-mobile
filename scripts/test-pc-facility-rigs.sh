#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[ "$#" = 1 ] || { echo 'Provide independent raw PC platform reference' >&2; exit 1; }
mkdir -p app/build/pc-facility-rigs-check
rg --files core/src/main/java game-api/src/main/java -g '*.java' > app/build/pc-facility-rigs-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion UnitAnimation UnitLod CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay PcUnits PcFacilities PcFacilityRigs CombatSequence CombatReplayLedger; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/pc-facility-rigs-sources.txt
done
echo core/src/testFixtures/java/game/sanguo/core/PcFacilityRigsFixture.java >> app/build/pc-facility-rigs-sources.txt
echo app/src/test/java/game/sanguo/mobile/PcFacilityRigsRestorationTest.java >> app/build/pc-facility-rigs-sources.txt
javac -encoding UTF-8 --release 17 -d app/build/pc-facility-rigs-check @app/build/pc-facility-rigs-sources.txt
java -Xmx1200m -cp app/build/pc-facility-rigs-check:core/src/main/resources game.sanguo.mobile.PcFacilityRigsRestorationTest "$1"

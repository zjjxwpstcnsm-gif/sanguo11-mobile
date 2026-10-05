#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[ "$#" = 1 ] || { echo 'Provide independent raw-source PCUREF file' >&2; exit 1; }
mkdir -p app/build/pc-units-check
rg --files core/src/main/java game-api/src/main/java -g '*.java' > app/build/pc-units-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion UnitAnimation UnitLod CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay PcUnits PcUnitFormation; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/pc-units-sources.txt
done
echo core/src/testFixtures/java/game/sanguo/core/PcUnitsFixture.java >> app/build/pc-units-sources.txt
echo tools/content/PcUnitCommandsProbe.java >> app/build/pc-units-sources.txt
echo app/src/test/java/game/sanguo/mobile/PcUnitsRestorationTest.java >> app/build/pc-units-sources.txt
javac -encoding UTF-8 --release 17 -d app/build/pc-units-check @app/build/pc-units-sources.txt
java -Xmx1200m -cp app/build/pc-units-check:core/src/main/resources game.sanguo.mobile.PcUnitsRestorationTest "$1"
java -Xmx1200m -cp app/build/pc-units-check:core/src/main/resources game.sanguo.core.PcUnitCommandsProbe

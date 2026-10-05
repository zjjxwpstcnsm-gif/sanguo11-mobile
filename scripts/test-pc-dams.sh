#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/pc-dams-check
rg --files core/src/main/java game-api/src/main/java -g '*.java' > app/build/pc-dams-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay PcFacilities PcDams; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/pc-dams-sources.txt
done
echo app/src/test/java/game/sanguo/mobile/PcDamsRestorationTest.java >> app/build/pc-dams-sources.txt
echo core/src/testFixtures/java/game/sanguo/core/PcDamsFixture.java >> app/build/pc-dams-sources.txt
javac -encoding UTF-8 --release 17 -d app/build/pc-dams-check @app/build/pc-dams-sources.txt
java -Xmx1200m -cp app/build/pc-dams-check:core/src/main/resources game.sanguo.mobile.PcDamsRestorationTest "$@"

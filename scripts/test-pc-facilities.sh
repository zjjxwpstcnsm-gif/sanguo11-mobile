#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/pc-facilities-check
rg --files core/src/main/java game-api/src/main/java -g '*.java' > app/build/pc-facilities-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay PcFacilities; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/pc-facilities-sources.txt
done
echo app/src/test/java/game/sanguo/mobile/PcFacilitiesRestorationTest.java >> app/build/pc-facilities-sources.txt
echo core/src/testFixtures/java/game/sanguo/core/PcFacilitiesFixture.java >> app/build/pc-facilities-sources.txt
javac -encoding UTF-8 --release 17 -d app/build/pc-facilities-check @app/build/pc-facilities-sources.txt
java -Xmx1200m -cp app/build/pc-facilities-check:core/src/main/resources game.sanguo.mobile.PcFacilitiesRestorationTest

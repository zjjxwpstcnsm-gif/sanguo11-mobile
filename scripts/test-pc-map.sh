#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/pc-map-check
find core/src/main/java game-api/src/main/java -name '*.java' > app/build/pc-map-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/pc-map-sources.txt
done
echo app/src/test/java/game/sanguo/mobile/PcMapRestorationTest.java >> app/build/pc-map-sources.txt
echo app/src/test/java/game/sanguo/mobile/PcWaterRestorationTest.java >> app/build/pc-map-sources.txt
javac -encoding UTF-8 --release 17 -d app/build/pc-map-check @app/build/pc-map-sources.txt
java -Xmx1200m -cp app/build/pc-map-check:core/src/main/resources:core/src/test/resources game.sanguo.mobile.PcMapRestorationTest "$@"

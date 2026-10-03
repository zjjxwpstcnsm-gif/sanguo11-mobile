#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/pc-sites-check
rg --files core/src/main/java game-api/src/main/java -g '*.java' > app/build/pc-sites-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay PcSites; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/pc-sites-sources.txt
done
echo app/src/test/java/game/sanguo/mobile/PcSitesRestorationTest.java >> app/build/pc-sites-sources.txt
javac -encoding UTF-8 --release 17 -d app/build/pc-sites-check @app/build/pc-sites-sources.txt
java -Xmx1200m -cp app/build/pc-sites-check:core/src/main/resources game.sanguo.mobile.PcSitesRestorationTest

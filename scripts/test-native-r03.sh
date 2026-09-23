#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/r03-check
find core/src/main/java game-api/src/main/java -name '*.java' > app/build/r03-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay; do
 echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/r03-sources.txt
done
echo app/src/test/java/game/sanguo/mobile/NativeR03Test.java >> app/build/r03-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d app/build/r03-check @app/build/r03-sources.txt
java -Xmx2g -cp app/build/r03-check:core/src/main/resources game.sanguo.mobile.NativeR03Test

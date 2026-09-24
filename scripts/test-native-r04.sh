#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/r04-check
find core/src/main/java game-api/src/main/java -name '*.java' > app/build/r04-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay; do
 echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/r04-sources.txt
done
echo app/src/test/java/game/sanguo/mobile/NativeR04Test.java >> app/build/r04-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d app/build/r04-check @app/build/r04-sources.txt
java -Xmx2g -cp app/build/r04-check:core/src/main/resources game.sanguo.mobile.NativeR04Test

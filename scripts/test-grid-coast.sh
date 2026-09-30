#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/grid-coast-check
find core/src/main/java game-api/src/main/java -name '*.java' > app/build/grid-coast-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/grid-coast-sources.txt
done
for name in NativeGridCoastTest TerrainSurfaceTest TerrainMaterialFieldTest NativeR02Test NativeR05Test WaterLandformTest; do
  echo "app/src/test/java/game/sanguo/mobile/$name.java" >> app/build/grid-coast-sources.txt
done
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d app/build/grid-coast-check @app/build/grid-coast-sources.txt
for name in NativeGridCoastTest TerrainSurfaceTest TerrainMaterialFieldTest NativeR02Test NativeR05Test WaterLandformTest; do
  java -Xmx2g -cp app/build/grid-coast-check:core/src/main/resources game.sanguo.mobile.$name
done

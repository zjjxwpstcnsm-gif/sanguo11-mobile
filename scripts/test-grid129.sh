#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/grid129-check
find core/src/main/java core/src/testFixtures/java game-api/src/main/java game-runtime/src/main/java -name '*.java' > app/build/grid129-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/grid129-sources.txt
done
printf '%s\n' core/src/test/java/game/sanguo/core/DifficultMarch129Test.java app/src/test/java/game/sanguo/mobile/NativeGrid129Test.java >> app/build/grid129-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -d app/build/grid129-check @app/build/grid129-sources.txt
java -Xmx1200m -cp app/build/grid129-check:core/src/main/resources:core/src/test/resources game.sanguo.core.DifficultMarch129Test
java -Xmx1200m -cp app/build/grid129-check:core/src/main/resources:core/src/test/resources game.sanguo.mobile.NativeGrid129Test

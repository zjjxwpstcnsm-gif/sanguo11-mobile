#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/interaction-check
find core/src/main/java game-api/src/main/java -name '*.java' > app/build/interaction-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/interaction-sources.txt
done
printf '%s\n' app/src/test/java/game/sanguo/mobile/InteractionEditorTest.java core/src/test/java/game/sanguo/core/MapEditorContinuationTest.java >> app/build/interaction-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d app/build/interaction-check @app/build/interaction-sources.txt
java -Xmx1g -cp app/build/interaction-check:core/src/main/resources game.sanguo.mobile.InteractionEditorTest
java -Xmx1g -cp app/build/interaction-check:core/src/main/resources game.sanguo.core.MapEditorContinuationTest

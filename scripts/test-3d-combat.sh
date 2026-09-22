#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_TEST_JAR:?Set JSON_TEST_JAR to org.json host test jar}"
mkdir -p app/build/combat-check
find core/src/main/java -name '*.java' > app/build/combat-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera SceneMesh SiteVisual TerrainSurface TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay UnitVisual UnitMotion UnitAnimation CombatVisual; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/combat-sources.txt
done
for name in Turn48Fixture Realm52Fixture CombatSceneFixture; do
  echo "core/src/testFixtures/java/game/sanguo/core/$name.java" >> app/build/combat-sources.txt
done
echo app/src/test/java/game/sanguo/mobile/CombatVisualTest.java >> app/build/combat-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp "$JSON_TEST_JAR" -d app/build/combat-check @app/build/combat-sources.txt
java -Xmx1200m -cp "app/build/combat-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.CombatVisualTest

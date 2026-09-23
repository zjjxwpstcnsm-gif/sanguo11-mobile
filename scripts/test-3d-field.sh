#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_TEST_JAR:?Set JSON_TEST_JAR to org.json host test jar}"
mkdir -p app/build/field-check
find core/src/main/java game-api/src/main/java -name '*.java' > app/build/field-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera SceneMesh SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay UnitVisual UnitMotion UnitAnimation CombatVisual SiteGlb FieldAssets Vegetation; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/field-sources.txt
done
printf '%s\n' core/src/testFixtures/java/game/sanguo/core/SceneFacilityFixture.java app/src/test/java/game/sanguo/mobile/FieldAssetsTest.java >> app/build/field-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp "$JSON_TEST_JAR" -d app/build/field-check @app/build/field-sources.txt
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.FieldAssetsTest
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check core/src/testFixtures/java/game/sanguo/core/FieldSceneFixture.java core/src/testFixtures/java/game/sanguo/core/FieldLifecycleFixture.java app/src/test/java/game/sanguo/mobile/FieldFlowTest.java
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.FieldFlowTest

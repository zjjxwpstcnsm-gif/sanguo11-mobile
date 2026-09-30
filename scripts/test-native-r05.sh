#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/r05-check
find core/src/main/java game-api/src/main/java -name '*.java' > app/build/r05-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay; do
 echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/r05-sources.txt
done
echo app/src/test/java/game/sanguo/mobile/NativeR05Test.java >> app/build/r05-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d app/build/r05-check @app/build/r05-sources.txt
java -Xmx2g -cp app/build/r05-check:core/src/main/resources game.sanguo.mobile.NativeR05Test

java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp app/build/r05-check -d app/build/r05-check app/src/test/java/game/sanguo/mobile/WaterLandformTest.java core/src/test/java/game/sanguo/core/PortReplayTest.java
java -Xmx2g -cp app/build/r05-check:core/src/main/resources game.sanguo.mobile.WaterLandformTest

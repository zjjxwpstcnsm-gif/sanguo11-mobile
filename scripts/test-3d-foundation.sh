#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/scene-check
find core/src/main/java -name '*.java' > app/build/scene-sources.txt
for name in TileGeometry GridWorldTransform SceneCamera SceneMesh MapSceneSnapshot FactionColors SiegeOverlay; do
  echo "app/src/main/java/game/sanguo/mobile/$name.java" >> app/build/scene-sources.txt
done
echo app/src/test/java/game/sanguo/mobile/SceneFoundationTest.java >> app/build/scene-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d app/build/scene-check @app/build/scene-sources.txt
java -cp app/build/scene-check:core/src/main/resources game.sanguo.mobile.SceneFoundationTest
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -cp app/build/scene-check -d app/build/scene-check core/src/testFixtures/java/game/sanguo/core/SceneFacilityFixture.java app/src/test/java/game/sanguo/mobile/SceneFacilityStateTest.java
java -cp app/build/scene-check:core/src/main/resources game.sanguo.mobile.SceneFacilityStateTest

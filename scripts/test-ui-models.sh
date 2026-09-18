#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/ui-check
find core/src/main/java -name '*.java' > app/build/ui-sources.txt
printf '%s\n' core/src/testFixtures/java/game/sanguo/core/TestScenarios.java app/src/main/java/game/sanguo/mobile/UiModels.java app/src/main/java/game/sanguo/mobile/MapCamera.java app/src/main/java/game/sanguo/mobile/FactionColors.java app/src/main/java/game/sanguo/mobile/PortraitCatalog.java app/src/test/java/game/sanguo/mobile/PresentationTest.java >> app/build/ui-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d app/build/ui-check @app/build/ui-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -cp app/build/ui-check -d app/build/ui-check app/src/main/java/game/sanguo/mobile/TerrainConnections.java app/src/test/java/game/sanguo/mobile/TerrainConnectionsTest.java
java -cp app/build/ui-check:core/src/main/resources:core/src/test/resources game.sanguo.mobile.PresentationTest
java -cp app/build/ui-check game.sanguo.mobile.TerrainConnectionsTest

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-3d-field.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check core/src/testFixtures/java/game/sanguo/core/{CombatSceneFixture,Realm52Fixture,Turn48Fixture}.java app/src/test/java/game/sanguo/mobile/NativeFeedback120Test.java
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeFeedback120Test
mkdir -p app/build/forest119-source app/build/forest119-control
git show 252aeea39e4b7bdfb0cbdf4cc6884c5b400a45a7:app/src/main/java/game/sanguo/mobile/Vegetation.java > app/build/forest119-source/Vegetation.java
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/forest119-control app/build/forest119-source/Vegetation.java
java -Xmx1200m -cp "app/build/forest119-control:app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeFeedback120Test baseline

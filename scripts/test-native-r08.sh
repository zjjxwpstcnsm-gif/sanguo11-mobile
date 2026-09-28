#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-native-r07.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/src/test/java/game/sanguo/mobile/NativeR08Test.java
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeR08Test

java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/src/test/java/game/sanguo/mobile/NativePreviewWorkTest.java
# v118 intentionally adds forest canopy; keep the complete v117 placement/triangle
# goldens executable against the exact historical generator (never bless new hashes).
# Current generator separately proves every old placement is retained, with bounded
# additions, same protected clearances and full authority/RNG immutability.
mkdir -p app/build/vegetation-v117 app/build/vegetation-control
# This file is an exact git-object extraction, not a rewritten approximation.
git show 856d18c17c5f9295b0e1e4f828fa7dd4cd65a635:app/src/main/java/game/sanguo/mobile/Vegetation.java > app/build/vegetation-v117/Vegetation.java
sed 's/final class Vegetation {/final class Vegetation117 {/' app/build/vegetation-v117/Vegetation.java > app/build/vegetation-v117/Vegetation117.java
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/build/vegetation-v117/Vegetation117.java core/src/testFixtures/java/game/sanguo/core/{CombatSceneFixture,Realm52Fixture,Turn48Fixture}.java app/src/test/java/game/sanguo/mobile/ForestDensityTest.java
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.ForestDensityTest
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/vegetation-control app/build/vegetation-v117/Vegetation.java
java -Xmx1200m -cp "app/build/vegetation-control:app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativePreviewWorkTest

# Immutable pre-R15 material and pre-shore geometry control: retain every original
# v105 full-byte golden. Current terrain deliberately transports X/Z and anchors.
# Compile separately so its package-private production class cannot enter the APK.
mkdir -p app/build/preview-v105-control
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/preview-v105-control app/src/test/fixtures/native-v105/TerrainMaterialField.java app/src/test/fixtures/native-v116/SceneMesh.java app/src/test/fixtures/native-v116/TerrainSurface.java app/src/test/fixtures/native-v116/NativePreviewWorkTest.java
java -Xmx1200m -cp "app/build/preview-v105-control:app/build/vegetation-control:app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativePreviewWorkTest legacy-palette-control

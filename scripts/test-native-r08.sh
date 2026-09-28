#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-native-r07.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/src/test/java/game/sanguo/mobile/NativeR08Test.java
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeR08Test

java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/src/test/java/game/sanguo/mobile/NativePreviewWorkTest.java
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativePreviewWorkTest

# Immutable pre-R15 material and pre-shore geometry control: retain every original
# v105 full-byte golden. Current terrain deliberately transports X/Z and anchors.
# Compile separately so its package-private production class cannot enter the APK.
mkdir -p app/build/preview-v105-control
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/preview-v105-control app/src/test/fixtures/native-v105/TerrainMaterialField.java app/src/test/fixtures/native-v116/SceneMesh.java app/src/test/fixtures/native-v116/TerrainSurface.java app/src/test/fixtures/native-v116/NativePreviewWorkTest.java
java -Xmx1200m -cp "app/build/preview-v105-control:app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativePreviewWorkTest legacy-palette-control

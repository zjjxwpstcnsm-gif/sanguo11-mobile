#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-native-r07.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/src/test/java/game/sanguo/mobile/NativeR08Test.java
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeR08Test

java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/src/test/java/game/sanguo/mobile/NativePreviewWorkTest.java
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativePreviewWorkTest

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-waterfall127.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/src/test/java/game/sanguo/mobile/NativeLandmarks128Test.java app/src/test/java/game/sanguo/mobile/NativeWaterfallSeam128Test.java
java -Xmx1500m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeLandmarks128Test
java -Xmx1500m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeWaterfallSeam128Test

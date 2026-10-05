#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_TEST_JAR:?org.json 20240303 required}"
bash scripts/test-3d-field.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check core/src/testFixtures/java/game/sanguo/core/*.java app/src/androidTest/java/game/sanguo/mobile/R15TourPlan.java app/src/test/java/game/sanguo/mobile/NativeR15Test.java
java -Xmx1500m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeR15Test

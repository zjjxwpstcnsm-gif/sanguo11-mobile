#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_TEST_JAR:?org.json 20240303 required}"
# Reuse the existing production compilation and existing R11 gates unchanged.
bash scripts/test-native-r11.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 \
  -cp "app/build/combat-check:$JSON_TEST_JAR" -d app/build/combat-check \
  app/src/test/java/game/sanguo/core/FirebasePlotAuditFixture.java \
  app/src/test/java/game/sanguo/mobile/FirebasePlotAuditTest.java
java -Xmx1200m -cp "app/build/combat-check:core/src/main/resources:$JSON_TEST_JAR" \
  game.sanguo.mobile.FirebasePlotAuditTest

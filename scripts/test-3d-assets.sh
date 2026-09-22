#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_TEST_JAR:?Set JSON_TEST_JAR to org.json:json:20240303 host test jar (not shipped in APK)}"
bash scripts/test-3d-foundation.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/scene-check:$JSON_TEST_JAR" -d app/build/scene-check app/src/main/java/game/sanguo/mobile/SiteGlb.java app/src/test/java/game/sanguo/mobile/SiteGlbTest.java
java -cp "app/build/scene-check:$JSON_TEST_JAR" game.sanguo.mobile.SiteGlbTest

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-native-r03.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp app/build/r03-check -d app/build/r03-check \
  app/src/main/java/game/sanguo/mobile/SceneWorkQueue.java \
  app/src/test/java/game/sanguo/mobile/SceneWorkQueueTest.java \
  app/src/test/java/game/sanguo/mobile/NativeGroundStreamTest.java
java -cp app/build/r03-check game.sanguo.mobile.SceneWorkQueueTest
java -Xmx1200m -cp app/build/r03-check:core/src/main/resources game.sanguo.mobile.NativeGroundStreamTest

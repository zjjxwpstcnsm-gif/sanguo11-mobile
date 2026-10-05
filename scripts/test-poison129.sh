#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-native-r04.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp app/build/r04-check -d app/build/r04-check app/src/test/java/game/sanguo/mobile/PoisonSpring129Test.java
java -Xmx1600m -cp app/build/r04-check:core/src/main/resources game.sanguo.mobile.PoisonSpring129Test

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-3d-interaction.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp app/build/interaction-check -d app/build/interaction-check app/src/main/java/game/sanguo/mobile/SeasonStyle.java app/src/test/java/game/sanguo/mobile/NativeR14Test.java
java -Xmx1200m -cp app/build/interaction-check:core/src/main/resources game.sanguo.mobile.NativeR14Test
python3 scripts/check-architecture.py

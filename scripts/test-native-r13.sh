#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-3d-interaction.sh
javac --release 17 -encoding UTF-8 -cp app/build/interaction-check -d app/build/interaction-check app/src/test/java/game/sanguo/mobile/NativeR13Test.java
java -Xmx1200m -cp app/build/interaction-check:core/src/main/resources game.sanguo.mobile.NativeR13Test
bash scripts/test-map-editor67.sh
bash scripts/test-custom-officers.sh
python3 scripts/check-architecture.py

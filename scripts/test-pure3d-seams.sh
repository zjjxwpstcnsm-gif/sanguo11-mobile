#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-pc-map.sh
javac --release 17 -encoding UTF-8 -cp app/build/pc-map-check -d app/build/pc-map-check app/src/test/java/game/sanguo/mobile/Pure3dSeamTest.java app/src/test/java/game/sanguo/mobile/GridPrimitiveEquivalenceTest.java
java -Xmx768m -cp app/build/pc-map-check:core/src/main/resources game.sanguo.mobile.Pure3dSeamTest
java -Xmx768m -cp app/build/pc-map-check:core/src/main/resources game.sanguo.mobile.GridPrimitiveEquivalenceTest

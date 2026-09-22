#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-3d-foundation.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp app/build/scene-check -d app/build/scene-check app/src/test/java/game/sanguo/mobile/NationwideAcceptanceTest.java
java -Xmx2g -cp app/build/scene-check:core/src/main/resources game.sanguo.mobile.NationwideAcceptanceTest

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/audit/host
javac --release 17 -encoding UTF-8 -d out/audit/host app/src/main/java/game/sanguo/mobile/SceneRenderGate.java app/src/test/java/game/sanguo/mobile/SceneRenderGateTest.java
java -cp out/audit/host game.sanguo.mobile.SceneRenderGateTest
python3 scripts/check-architecture.py

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/r01/host
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -d out/r01/host app/src/main/java/game/sanguo/mobile/SceneWorkQueue.java app/src/test/java/game/sanguo/mobile/SceneWorkQueueTest.java
java -cp out/r01/host game.sanguo.mobile.SceneWorkQueueTest
python3 scripts/check-architecture.py

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/r17/host-classes
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -d out/r17/host-classes app/src/main/java/game/sanguo/mobile/VerifiedMaterial.java app/src/test/java/game/sanguo/mobile/VerifiedMaterialTest.java
java -cp out/r17/host-classes game.sanguo.mobile.VerifiedMaterialTest
python3 scripts/check-architecture.py

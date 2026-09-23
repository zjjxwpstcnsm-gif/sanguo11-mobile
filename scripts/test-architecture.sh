#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export LANG=C.UTF-8 LC_ALL=C.UTF-8
out=game-runtime/build/architecture-check
mkdir -p "$out";find core/src/main/java game-api/src/main/java game-runtime/src/main/java game-runtime/src/test/java -name '*.java' > "$out/sources.txt"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d "$out" @"$out/sources.txt"
java -Xmx768m -cp "$out:core/src/main/resources:game-runtime/src/test/resources" game.sanguo.runtime.GameSessionTest
bash scripts/test-unity-u01.sh

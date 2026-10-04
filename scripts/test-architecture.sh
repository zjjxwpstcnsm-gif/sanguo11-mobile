#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export LANG=C.UTF-8 LC_ALL=C.UTF-8
python3 scripts/check-architecture.py
out=game-runtime/build/architecture-check
mkdir -p "$out"/isolated-api "$out"/isolated-core
find game-api/src/main/java -name '*.java' > "$out/api.txt"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d "$out/isolated-api" @"$out/api.txt"
find core/src/main/java -name '*.java' > "$out/core.txt"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d "$out/isolated-core" @"$out/core.txt"
find core/src/main/java game-api/src/main/java game-runtime/src/main/java game-runtime/src/test/java -name '*.java' > "$out/sources.txt"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d "$out" @"$out/sources.txt"
java -Xmx768m -cp "$out:core/src/main/resources:game-runtime/src/test/resources" game.sanguo.runtime.GameSessionTest
bash scripts/test-unity-u01.sh

java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -cp "$out" -d "$out" app/src/main/java/game/sanguo/mobile/FactionColors.java app/src/main/java/game/sanguo/mobile/presentation/MapLayerData.java app/src/main/java/game/sanguo/mobile/MapProjectionQuery.java app/src/test/java/game/sanguo/mobile/MapProjectionTest.java
java -Xmx768m -cp "$out":core/src/main/resources game.sanguo.mobile.MapProjectionTest

java -cp "$out:game-runtime/src/test/resources" game.sanguo.runtime.GridLayoutTest

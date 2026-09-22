#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Run without other builds/tests for comparable CPU samples. Host timings are not phone timings.
: "${JSON_TEST_JAR:?Set JSON_TEST_JAR}"
bash scripts/test-3d-combat.sh
baseline=${1:-4e7d2920d5389623826870659c9ef5aec11a7b28}
bench=$(mktemp -d)
trap 'rm -rf "$bench"' EXIT
mkdir -p "$bench/old/game/sanguo/core" "$bench/common"
for name in TurnJournal CombatEffects; do
  git show "$baseline:core/src/main/java/game/sanguo/core/$name.java" > "$bench/old/game/sanguo/core/$name.java"
done
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp app/build/combat-check -d "$bench/common" tools/3d/S06Benchmark.java
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp app/build/combat-check -d "$bench/old" "$bench/old/game/sanguo/core/TurnJournal.java" "$bench/old/game/sanguo/core/CombatEffects.java"
java -Xmx1400m -cp "$bench/common:app/build/combat-check:core/src/main/resources" S06Benchmark
java -Xmx1400m -cp "$bench/old:$bench/common:app/build/combat-check:core/src/main/resources" S06Benchmark

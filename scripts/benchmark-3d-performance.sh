#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_TEST_JAR:?Set JSON_TEST_JAR}"
bash scripts/test-3d-field.sh
baseline=${1:-3439247d634a0df9f7f62b14348323eaf413fcd9}
bench=$(mktemp -d)
trap 'rm -rf "$bench"' EXIT
mkdir -p "$bench/old" "$bench/common"
git show "$baseline:app/src/main/java/game/sanguo/mobile/FieldAssets.java" > "$bench/old/FieldAssets.java"
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d "$bench/common" tools/3d/S08Benchmark.java app/src/main/java/game/sanguo/mobile/SceneQuality.java app/src/test/java/game/sanguo/mobile/SceneQualityTest.java
java -cp "$bench/common" game.sanguo.mobile.SceneQualityTest
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d "$bench/old" "$bench/old/FieldAssets.java"
java -Xmx1g -cp "$bench/old:$bench/common:app/build/field-check:$JSON_TEST_JAR" game.sanguo.mobile.S08Benchmark
java -Xmx1g -cp "$bench/common:app/build/field-check:$JSON_TEST_JAR" game.sanguo.mobile.S08Benchmark

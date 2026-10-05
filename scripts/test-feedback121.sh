#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-3d-field.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/src/test/java/game/sanguo/mobile/NativeFeedback121Test.java
java -Xmx1600m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeFeedback121Test
mkdir -p app/build/terrain120-source app/build/terrain120-control
git show afac21886f8b092ed3099aa1bf88eba966955285:app/src/main/java/game/sanguo/mobile/TerrainSurface.java > app/build/terrain120-source/TerrainSurface.java
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/terrain120-control app/build/terrain120-source/TerrainSurface.java
java -Xmx1600m -cp "app/build/terrain120-control:app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeFeedback121Test baseline

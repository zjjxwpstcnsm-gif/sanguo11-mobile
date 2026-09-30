#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_TEST_JAR:?org.json 20240303 required}"
bash scripts/test-native-r06.sh
find core/src/testFixtures/java -name '*.java' ! -path '*/battle/*' > app/build/r10-fixtures.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check @app/build/r10-fixtures.txt app/src/test/java/game/sanguo/mobile/NativeR10Test.java
java -Xmx1200m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeR10Test

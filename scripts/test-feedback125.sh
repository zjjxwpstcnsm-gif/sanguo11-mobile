#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/feedback125 core/build/feedback125
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/feedback125/sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -d core/build/feedback125 @core/build/feedback125/sources.txt
java -cp core/build/feedback125:core/src/main/resources:core/src/test/resources game.sanguo.core.Cavalry125Test
bash scripts/test-3d-field.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/src/test/java/game/sanguo/mobile/NativeFeedback125Test.java
java -Xmx1500m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeFeedback125Test
mkdir -p game-runtime/build/feedback125
find game-api/src/main/java game-runtime/src/main/java -name '*.java' > game-runtime/build/feedback125/sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp core/build/feedback125 -d game-runtime/build/feedback125 @game-runtime/build/feedback125/sources.txt app/src/test/java/game/sanguo/mobile/ContestSession125Test.java
java -cp game-runtime/build/feedback125:core/build/feedback125:core/src/main/resources:core/src/test/resources game.sanguo.mobile.ContestSession125Test

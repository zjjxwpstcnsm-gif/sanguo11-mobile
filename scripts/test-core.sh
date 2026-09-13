#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/check
find core/src/main/java core/src/test/java -name '*.java' -print > core/build/sources.txt
if command -v javac >/dev/null 2>&1; then
  javac -encoding UTF-8 --release 17 -d core/build/check @core/build/sources.txt
else
  java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d core/build/check @core/build/sources.txt
fi
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.CoreTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.ScenarioTest

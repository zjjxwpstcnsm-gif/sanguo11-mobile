#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dfile.encoding=UTF-8"
mkdir -p core/build/editor67
find core/src/main/java -name '*.java' | sort > core/build/editor67-sources.txt
printf '%s\n' core/src/test/java/game/sanguo/core/MapBrushGeometryTest.java core/src/test/java/game/sanguo/core/MapRevisionLedgerTest.java core/src/test/java/game/sanguo/core/MapEditorContinuationTest.java >> core/build/editor67-sources.txt
javac -encoding UTF-8 --release 17 -d core/build/editor67 @core/build/editor67-sources.txt
for test in MapBrushGeometryTest MapRevisionLedgerTest; do
  java -Xmx768m -cp core/build/editor67:core/src/main/resources game.sanguo.core.$test
done
java -Xmx768m -cp core/build/editor67:core/src/main/resources game.sanguo.core.MapEditorContinuationTest "${1:-out/editor67/phone-export-fixture.json}"

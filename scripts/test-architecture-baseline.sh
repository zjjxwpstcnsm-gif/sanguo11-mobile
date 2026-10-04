#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export LANG=C.UTF-8 LC_ALL=C.UTF-8
baseline=9548bb350051150b21a61213f9068ffb1b7506c0
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
git archive "$baseline" core/src/main | tar -xf - -C "$work"
find "$work/core/src/main/java" -name '*.java' > "$work/sources.txt"
printf '%s\n' "$PWD/game-runtime/src/test/java/game/sanguo/runtime/BaselineSequence.java" >> "$work/sources.txt"
mkdir "$work/classes"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d "$work/classes" @"$work/sources.txt"
java -Xmx768m -cp "$work/classes:$work/core/src/main/resources" game.sanguo.runtime.BaselineSequence "$work/actual.txt"
cmp game-runtime/src/test/resources/architecture/baseline-9548bb35.txt "$work/actual.txt"
echo 'Old-source baseline PASS: seven complete save states, RNG included, unchanged native and bridge rule paths'

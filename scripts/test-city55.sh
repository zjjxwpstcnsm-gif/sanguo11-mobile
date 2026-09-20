#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/city55 dist
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/city55-sources.txt
javac -encoding UTF-8 --release 17 -d core/build/city55 @core/build/city55-sources.txt
java -Xmx1500m -cp core/build/city55:core/src/main/resources:core/src/test/resources game.sanguo.core.CityFootprint55Test | tee dist/city55-checks.txt

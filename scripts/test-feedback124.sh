#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_TEST_JAR:?org.json host jar required}"
bash scripts/test-3d-field.sh
mkdir -p app/build/feedback124-check out/feedback124
# Independently compile the exact input algorithms. Only class names and asset
# routing change to compare identical new assets, never the original assertions.
git show 13fa313e39a9bc9ff157701b721d79c62144ce1e:app/src/main/java/game/sanguo/mobile/Vegetation.java | sed 's/class Vegetation/class VegetationBaseline124/' > app/build/feedback124-check/VegetationBaseline124.java
git show 13fa313e39a9bc9ff157701b721d79c62144ce1e:app/src/main/java/game/sanguo/mobile/FieldAssets.java | sed 's/FieldAssets/FieldAssetsBaseline124/g; s/rigs-v123/rigs-v124/g; s/v123\//v124\//g' > app/build/feedback124-check/FieldAssetsBaseline124.java
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp "app/build/field-check:$JSON_TEST_JAR" -d app/build/field-check app/build/feedback124-check/{VegetationBaseline124,FieldAssetsBaseline124}.java app/src/test/java/game/sanguo/mobile/NativeFeedback124Test.java
java -Xmx1500m -cp "app/build/field-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeFeedback124Test

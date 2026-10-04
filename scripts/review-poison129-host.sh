#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=${1:-out/poison129-host}
mkdir -p "$out" app/build/poison129-baseline
bash scripts/test-native-r04.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp app/build/r04-check -d app/build/r04-check app/src/test/java/game/sanguo/mobile/PoisonSpring129Export.java
java -Xmx1600m -cp app/build/r04-check:core/src/main/resources game.sanguo.mobile.PoisonSpring129Export "$out/after"
sed 's#app/src/main/java/game/sanguo/mobile/TerrainMaterialField.java#app/src/test/fixtures/native-v128/TerrainMaterialField.java#; /NativeR04Test.java/d' app/build/r04-sources.txt > app/build/poison129-baseline-sources.txt
printf '%s\n' app/src/test/java/game/sanguo/mobile/PoisonSpring129Export.java >> app/build/poison129-baseline-sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -d app/build/poison129-baseline @app/build/poison129-baseline-sources.txt
java -Xmx1600m -cp app/build/poison129-baseline:core/src/main/resources game.sanguo.mobile.PoisonSpring129Export "$out/before"
python3 tools/3d/review_poison129.py "$out"

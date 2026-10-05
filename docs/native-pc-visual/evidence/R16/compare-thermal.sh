#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../../.."
baseline=f355536d94d22f623fc44e33c91410f66938c7d0
out=out/r16/thermal-comparison
mkdir -p "$out"/{baseline,candidate}
git show "$baseline:app/src/main/java/game/sanguo/mobile/SceneQuality.java" > "$out/baseline/SceneQuality.java"
cp app/src/main/java/game/sanguo/mobile/SceneQuality.java "$out/candidate/SceneQuality.java"
printf '%s\n' 'variant,synthetic_seconds,thermal_status,constrained,cap_fps,scale,transitions' > "$out/raw.csv"
for variant in baseline candidate; do
  java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -d "$out/$variant" "$out/$variant/SceneQuality.java" docs/native-pc-visual/evidence/R16/R16ThermalComparison.java
  java -cp "$out/$variant" game.sanguo.mobile.R16ThermalComparison "$variant" >> "$out/raw.csv"
done
echo 'Synthetic policy comparison, not physical temperature/FPS. Source copies and full timeline retained.' > "$out/SCOPE.txt"

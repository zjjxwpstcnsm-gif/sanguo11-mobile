#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/smoke
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Landscape 1080p profiles. One emulator, three real display configurations.
for display in 1080x1920 1080x2340 1080x2400; do
  adb shell wm size "$display"
  adb shell wm density 420
  adb shell pm clear game.sanguo.mobile.dev
  adb shell settings put system accelerometer_rotation 0
  adb shell settings put system user_rotation 1
  adb logcat -c
  mkdir -p "app/build/smoke/$display"
  adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee "app/build/smoke/$display/instrumentation.txt"
  adb logcat -d > "app/build/smoke/$display/logcat.txt"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke "app/build/smoke/$display/screenshots"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/content-performance.txt "app/build/smoke/$display/content-performance.txt" || true
  python3 - "$display" <<'CHECK'
from pathlib import Path
import sys
folder=Path('app/build/smoke')/sys.argv[1]
text=(folder/'instrumentation.txt').read_text()
assert 'SMOKE PASS:' in text and 'SMOKE FAIL:' not in text, text
logs=(folder/'logcat.txt').read_text()
assert 'FATAL EXCEPTION' not in logs and 'ANR in game.sanguo.mobile' not in logs, 'Runtime crash/ANR detected'
CHECK
done
adb shell wm size reset
adb shell wm density reset

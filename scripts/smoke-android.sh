#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/smoke
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Check the new recovery/font gate first, then retain the entire historical UI flow.
if [ "${SMOKE_DISPLAYS:-1080x1920}" = "1080x1920" ]; then
  adb shell wm size 1080x1920
  adb shell pm clear game.sanguo.mobile.dev
  adb shell wm density 360
  adb shell settings put system font_scale 1.3
  adb shell settings put secure show_ime_with_hard_keyboard 1
  mkdir -p app/build/smoke/recovery
  adb logcat -c
  adb shell am instrument -w -e recovery prepare game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee app/build/smoke/recovery/prepare.txt
  adb shell am force-stop game.sanguo.mobile.dev
  adb shell am instrument -w -e recovery check game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee app/build/smoke/recovery/check.txt
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke app/build/smoke/recovery/screenshots
  adb logcat -d > app/build/smoke/recovery/logcat.txt
  python3 - <<'RECOVERY'
from pathlib import Path
for name in ['prepare','check']:
    s=(Path('app/build/smoke/recovery')/(name+'.txt')).read_text()
    assert 'RECOVERY '+name+' PASS' in s,s
RECOVERY
  adb shell settings put system font_scale 1.0
  for gate in architecture33 navigation32 mapPerformance fidelity balance41; do
    adb shell pm clear game.sanguo.mobile.dev
    adb shell am instrument -w -e "$gate" true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee "app/build/smoke/$gate.txt"
    python3 - "$gate" <<'GATE'
from pathlib import Path
import sys
s=Path('app/build/smoke',sys.argv[1]+'.txt').read_text()
assert ' PASS:' in s and 'SMOKE FAIL:' not in s,s
GATE
  done
  adb shell settings put secure show_ime_with_hard_keyboard 0
fi

# Each display verifies portrait and landscape via the in-game orientation picker.
for display in ${SMOKE_DISPLAYS:-1080x1920 1080x2340 1080x2400}; do
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

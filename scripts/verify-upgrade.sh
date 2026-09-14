#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/smoke/upgrade
adb install -r app/build/upgrade/v09.apk
base64 --decode core/src/test/resources/legacy-v8.sg11.b64 > app/build/upgrade/auto.sg11
adb push app/build/upgrade/auto.sg11 /data/local/tmp/sanguo-upgrade.sg11
adb shell run-as game.sanguo.mobile.dev mkdir -p files
adb shell 'run-as game.sanguo.mobile.dev sh -c "cat /data/local/tmp/sanguo-upgrade.sg11 > files/auto.sg11"'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb exec-out run-as game.sanguo.mobile.dev cat files/auto.sg11 > app/build/upgrade/retained.sg11
cmp app/build/upgrade/auto.sg11 app/build/upgrade/retained.sg11
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb logcat -c
adb shell am instrument -w -e upgrade true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee app/build/smoke/upgrade/instrumentation.txt
adb logcat -d > app/build/smoke/upgrade/logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke app/build/smoke/upgrade/screenshots
python3 - <<'PY'
from pathlib import Path
root=Path('app/build/smoke/upgrade')
text=(root/'instrumentation.txt').read_text()
assert 'UPGRADE PASS:' in text and 'SMOKE FAIL:' not in text, text
logs=(root/'logcat.txt').read_text()
assert 'FATAL EXCEPTION' not in logs and 'ANR in game.sanguo.mobile' not in logs
PY

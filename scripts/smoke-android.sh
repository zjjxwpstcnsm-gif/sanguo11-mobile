#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/smoke
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear game.sanguo.mobile
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell am instrument -w game.sanguo.mobile.test/game.sanguo.mobile.GameSmokeRunner | tee app/build/smoke/instrumentation.txt
adb logcat -d > app/build/smoke/logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile/files/smoke app/build/smoke/screenshots || true
python3 - <<'PY'
from pathlib import Path
text=Path('app/build/smoke/instrumentation.txt').read_text()
assert 'SMOKE PASS:' in text and 'SMOKE FAIL:' not in text, text
PY

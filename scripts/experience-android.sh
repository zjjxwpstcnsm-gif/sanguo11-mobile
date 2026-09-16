#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/experience
adb shell wm size 1080x1920
adb shell wm density 420
adb install -r app/build/baseline/v027.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear game.sanguo.mobile.dev
adb logcat -c
adb shell am instrument -w -e experience true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee app/build/experience/instrumentation.txt
adb logcat -d > app/build/experience/logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files app/build/experience/files
python3 - <<'PY'
from pathlib import Path
s=Path('app/build/experience/instrumentation.txt').read_text()
assert 'EXPERIENCE PASS' in s and 'SMOKE FAIL' not in s,s
PY

#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/displacement
adb shell wm size 1080x1920
adb shell wm density 420
adb uninstall game.sanguo.mobile.dev || true
adb uninstall game.sanguo.mobile.dev.test || true
adb install app/build/baseline/v028.apk
adb install app/build/baseline/observer.apk
adb logcat -c
adb shell am instrument -w -e displacement baseline game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee app/build/displacement/instrumentation.txt
adb logcat -d > app/build/displacement/logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files app/build/displacement/files
python3 - <<'PY'
from pathlib import Path
s=Path('app/build/displacement/instrumentation.txt').read_text()
assert 'DISPLACEMENT PASS' in s and 'SMOKE FAIL' not in s,s
PY

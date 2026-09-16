#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/smoke/upgrade-v025 app/build/upgrade
# Start a separate genuine v0.25 installation after the v0.9 gate.
adb uninstall game.sanguo.mobile.dev
adb install app/build/upgrade/v025.apk
base64 --decode core/src/test/resources/legacy-v19-logistics.sg11.b64 > app/build/upgrade/v19.sg11
adb shell run-as game.sanguo.mobile.dev mkdir -p files
adb shell -T 'run-as game.sanguo.mobile.dev sh -c "cat > files/auto.sg11"' < app/build/upgrade/v19.sg11
adb shell am start -W -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity
sleep 2
adb shell am force-stop game.sanguo.mobile.dev
adb exec-out run-as game.sanguo.mobile.dev cat files/auto.sg11 > app/build/upgrade/v19-old-app.sg11
cmp app/build/upgrade/v19.sg11 app/build/upgrade/v19-old-app.sg11
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb exec-out run-as game.sanguo.mobile.dev cat files/auto.sg11 > app/build/upgrade/v19-retained.sg11
cmp app/build/upgrade/v19-old-app.sg11 app/build/upgrade/v19-retained.sg11
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
adb shell am instrument -w -e upgrade 25 game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee app/build/smoke/upgrade-v025/instrumentation.txt
adb logcat -d > app/build/smoke/upgrade-v025/logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke app/build/smoke/upgrade-v025/screenshots
python3 - <<'PY'
from pathlib import Path
root=Path('app/build/smoke/upgrade-v025')
assert 'UPGRADE25 PASS:' in (root/'instrumentation.txt').read_text()
text=(root/'logcat.txt').read_text()
assert 'FATAL EXCEPTION' not in text and 'ANR in game.sanguo.mobile' not in text
PY

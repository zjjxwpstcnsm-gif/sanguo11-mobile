#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/smoke/upgrade-v027 app/build/upgrade
# Start a separate genuine v0.27 installation after the v0.9 gate.
adb uninstall game.sanguo.mobile.dev
adb install app/build/upgrade/v027.apk
base64 --decode core/src/test/resources/legacy-v20-upgrade.sg11.b64 > app/build/upgrade/v20.sg11
adb shell run-as game.sanguo.mobile.dev mkdir -p files
adb shell -T 'run-as game.sanguo.mobile.dev sh -c "cat > files/auto.sg11"' < app/build/upgrade/v20.sg11
adb shell am start -W -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity
sleep 2
adb shell input keyevent 3
sleep 1
adb shell am force-stop game.sanguo.mobile.dev
adb exec-out run-as game.sanguo.mobile.dev cat files/auto.sg11 > app/build/upgrade/v20-old-app.sg11
# The actual v0.27 app reads the real old fixture and writes its own v21 bytes.
python3 - <<'CHECK'
import struct
from pathlib import Path
raw=Path('app/build/upgrade/v20-old-app.sg11').read_bytes()
assert struct.unpack('>i',raw[4:8])[0]==21
CHECK
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb exec-out run-as game.sanguo.mobile.dev cat files/auto.sg11 > app/build/upgrade/v20-retained.sg11
cmp app/build/upgrade/v20-old-app.sg11 app/build/upgrade/v20-retained.sg11
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
adb shell am instrument -w -e upgrade 27 game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee app/build/smoke/upgrade-v027/instrumentation.txt
adb logcat -d > app/build/smoke/upgrade-v027/logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke app/build/smoke/upgrade-v027/screenshots
python3 - <<'PY'
from pathlib import Path
root=Path('app/build/smoke/upgrade-v027')
assert 'UPGRADE27 PASS:' in (root/'instrumentation.txt').read_text()
text=(root/'logcat.txt').read_text()
assert 'FATAL EXCEPTION' not in text and 'ANR in game.sanguo.mobile' not in text
PY

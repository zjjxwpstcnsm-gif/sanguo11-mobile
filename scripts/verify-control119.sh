#!/usr/bin/env bash
set -euo pipefail
out=out/control119/runtime
mkdir -p "$out"
adb wait-for-device
adb shell input keyevent 82
adb install -r out/control119/sanguo11-mobile-v119.apk > "$out/install.txt"
adb install -r out/control119/test.apk >> "$out/install.txt"
remote=$(adb shell pm path game.sanguo.mobile.dev | tr -d '\r' | sed -n 's/^package://p' | head -1)
adb pull "$remote" "$out/installed.apk"
cmp out/control119/sanguo11-mobile-v119.apk "$out/installed.apk"
sha256sum "$out/installed.apk" > "$out/installed.sha256"
rm "$out/installed.apk"
adb shell pm clear game.sanguo.mobile.dev > "$out/clear.txt"
adb shell getprop > "$out/device.txt"
adb logcat -c
code=0
timeout 300 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.ControlIntelligence119Instrumentation > "$out/instrumentation.txt" 2>&1 || code=$?
echo "$code" > "$out/process.exit"
adb logcat -d > "$out/logcat.txt"
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$out/images" || true
[ "$code" = 0 ]
grep -q 'PASS CONTROL119' "$out/instrumentation.txt"
! grep -Eq 'FAIL|INSTRUMENTATION_FAILED' "$out/instrumentation.txt"

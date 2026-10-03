#!/usr/bin/env bash
set -euo pipefail
mode=${1:?hukou, wall, taishan or southwest}
mkdir -p out/landmarks126/runtime
adb install -r out/landmarks126/sanguo11-mobile-v126.apk
adb install -r out/landmarks126/test.apk
path=$(adb shell pm path game.sanguo.mobile.dev | tr -d '\r' | sed -n 's/^package://p' | head -1)
adb pull "$path" /tmp/installed126.apk
cmp /tmp/installed126.apk out/landmarks126/sanguo11-mobile-v126.apk
sha256sum /tmp/installed126.apk out/landmarks126/sanguo11-mobile-v126.apk > out/landmarks126/runtime/installed-apk-sha256.txt
adb shell pm clear game.sanguo.mobile.dev
adb logcat -c
adb shell screenrecord --time-limit 180 /sdcard/landmarks126.mp4 > out/landmarks126/runtime/record.log 2>&1 &
recording=$!
code=0
timeout 900 adb shell am instrument -w -e mode "$mode" game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeLandmarks126Instrumentation > out/landmarks126/runtime/instrumentation.txt 2>&1 || code=$?
adb shell pkill -INT screenrecord || true
wait "$recording" || true
adb logcat -d > out/landmarks126/runtime/logcat.txt
adb shell getprop > out/landmarks126/runtime/device.txt
adb shell dumpsys meminfo game.sanguo.mobile.dev > out/landmarks126/runtime/meminfo.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 out/landmarks126/runtime/images || true
adb pull /sdcard/landmarks126.mp4 out/landmarks126/runtime/ || true
test "$code" = 0
grep -q 'PASS LANDMARK126' out/landmarks126/runtime/instrumentation.txt
! grep -Eq 'FAIL|INSTRUMENTATION_FAILED' out/landmarks126/runtime/instrumentation.txt

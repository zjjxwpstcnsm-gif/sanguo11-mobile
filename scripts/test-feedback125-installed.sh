#!/usr/bin/env bash
set -euo pipefail
mode=${1:?battle, taishan or southwest}
mkdir -p out/feedback125/runtime
adb install -r out/feedback125/sanguo11-mobile-v125.apk
adb install -r out/feedback125/test.apk
path=$(adb shell pm path game.sanguo.mobile.dev | tr -d '\r' | sed -n 's/^package://p' | head -1)
adb pull "$path" /tmp/installed125.apk
cmp /tmp/installed125.apk out/feedback125/sanguo11-mobile-v125.apk
sha256sum /tmp/installed125.apk out/feedback125/sanguo11-mobile-v125.apk > out/feedback125/runtime/installed-apk-sha256.txt
adb shell pm clear game.sanguo.mobile.dev
adb logcat -c
adb shell screenrecord --time-limit 180 /sdcard/feedback125.mp4 > out/feedback125/runtime/record.log 2>&1 &
recording=$!
code=0
timeout 900 adb shell am instrument -w -e mode "$mode" game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeFeedback125Instrumentation > out/feedback125/runtime/instrumentation.txt 2>&1 || code=$?
adb shell pkill -INT screenrecord || true
wait "$recording" || true
adb logcat -d > out/feedback125/runtime/logcat.txt
adb shell getprop > out/feedback125/runtime/device.txt
adb shell dumpsys meminfo game.sanguo.mobile.dev > out/feedback125/runtime/meminfo.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 out/feedback125/runtime/images || true
adb pull /sdcard/feedback125.mp4 out/feedback125/runtime/ || true
test "$code" = 0
grep -q 'PASS FEEDBACK125' out/feedback125/runtime/instrumentation.txt
! grep -Eq 'FAIL|INSTRUMENTATION_FAILED' out/feedback125/runtime/instrumentation.txt

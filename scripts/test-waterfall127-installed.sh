#!/usr/bin/env bash
set -euo pipefail
mode=${1:?waterfall scene}
variant=${2:?candidate or input}
mkdir -p out/waterfall127/runtime
if [ "$variant" = input ]; then
  main=out/waterfall127/sanguo11-mobile-v126.apk
  runner=game.sanguo.mobile.NativeLandmarks126Instrumentation
  pass='PASS LANDMARK126'
else
  main=out/waterfall127/sanguo11-mobile-v127.apk
  runner=game.sanguo.mobile.NativeWaterfall127Instrumentation
  pass='PASS WATERFALL127'
fi
adb install -r "$main"
adb install -r out/waterfall127/test.apk
path=$(adb shell pm path game.sanguo.mobile.dev | tr -d '\r' | sed -n 's/^package://p' | head -1)
adb pull "$path" /tmp/installed127.apk
cmp /tmp/installed127.apk "$main"
sha256sum /tmp/installed127.apk "$main" > out/waterfall127/runtime/installed-apk-sha256.txt
adb shell pm clear game.sanguo.mobile.dev
adb logcat -c
adb shell screenrecord --time-limit 180 /sdcard/waterfall127.mp4 > out/waterfall127/runtime/record.log 2>&1 &
recording=$!
code=0
timeout 900 adb shell am instrument -w -e mode "$mode" "game.sanguo.mobile.dev.test/$runner" > out/waterfall127/runtime/instrumentation.txt 2>&1 || code=$?
adb shell pkill -INT screenrecord || true
wait "$recording" || true
adb logcat -d > out/waterfall127/runtime/logcat.txt
adb shell getprop > out/waterfall127/runtime/device.txt
adb shell dumpsys meminfo game.sanguo.mobile.dev > out/waterfall127/runtime/meminfo.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 out/waterfall127/runtime/images || true
adb pull /sdcard/waterfall127.mp4 out/waterfall127/runtime/ || true
test "$code" = 0
grep -q "$pass" out/waterfall127/runtime/instrumentation.txt
! grep -Eq 'FAIL|INSTRUMENTATION_FAILED' out/waterfall127/runtime/instrumentation.txt

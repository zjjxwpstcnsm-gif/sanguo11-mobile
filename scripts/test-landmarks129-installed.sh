#!/usr/bin/env bash
set -euo pipefail
mode=${1:?landmark scene}
variant=${2:?candidate or input}
root=out/landmarks129
mkdir -p "$root/runtime"
main="$root/sanguo11-mobile-v129.apk"
if [ "$variant" = input ]; then main="$root/input/sanguo11-mobile-v128.apk"; fi
adb install -r "$main"
adb install -r "$root/test.apk"
path=$(adb shell pm path game.sanguo.mobile.dev | tr -d '\r' | sed -n 's/^package://p' | head -1)
adb pull "$path" /tmp/installed129.apk
cmp /tmp/installed129.apk "$main"
sha256sum /tmp/installed129.apk "$main" > "$root/runtime/installed-apk-sha256.txt"
adb shell pm clear game.sanguo.mobile.dev
adb logcat -c
adb shell screenrecord --time-limit 180 /sdcard/landmarks129.mp4 > "$root/runtime/record.log" 2>&1 &
recording=$!
code=0
timeout 900 adb shell am instrument -w -e mode "$mode" game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeLandmarks128Instrumentation > "$root/runtime/instrumentation.txt" 2>&1 || code=$?
adb shell pkill -INT screenrecord || true
wait "$recording" || true
adb logcat -d > "$root/runtime/logcat.txt"
adb shell getprop > "$root/runtime/device.txt"
adb shell dumpsys meminfo game.sanguo.mobile.dev > "$root/runtime/meminfo.txt"
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$root/runtime/images" || true
adb pull /sdcard/landmarks129.mp4 "$root/runtime/" || true
test "$code" = 0
grep -q 'PASS LANDMARK128' "$root/runtime/instrumentation.txt"
! grep -Eq 'FAIL|INSTRUMENTATION_FAILED' "$root/runtime/instrumentation.txt"

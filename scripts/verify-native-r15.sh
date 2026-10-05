#!/usr/bin/env bash
set -euo pipefail
out=out/r15/runtime
mkdir -p "$out"
adb wait-for-device
adb shell input keyevent 82
adb shell getprop > "$out/device.txt"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
failed=0
for mode in layout tour; do
  mkdir -p "$out/$mode"
  adb shell pm clear game.sanguo.mobile.dev
  adb logcat -c
  (for part in {1..12}; do [ ! -f "$out/$mode/stop" ] || break; adb shell screenrecord --time-limit 180 "/sdcard/r15-$mode-$part.mp4"; done) > "$out/$mode/record.txt" 2>&1 &
  record_job=$!
  code=0
  timeout 2400 adb shell am instrument -w -e mode "$mode" game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR15Instrumentation > "$out/$mode/instrumentation.txt" 2>&1 || code=$?
  printf '%s\n' "$code" > "$out/$mode/process-exit.txt"
  touch "$out/$mode/stop"
  adb shell pkill -INT screenrecord || true
  wait "$record_job" || true
  adb logcat -d > "$out/$mode/logcat.txt"
  adb shell dumpsys meminfo game.sanguo.mobile.dev > "$out/$mode/meminfo.txt"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$out/$mode/images" || true
  for part in {1..12}; do adb pull "/sdcard/r15-$mode-$part.mp4" "$out/$mode/" 2>/dev/null || true; done
  if [ "$code" != 0 ] || ! grep -q "PASS R15 $mode" "$out/$mode/instrumentation.txt"; then failed=1; fi
  adb shell am force-stop game.sanguo.mobile.dev
 done
exit "$failed"

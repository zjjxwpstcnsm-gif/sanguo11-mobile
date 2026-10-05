#!/usr/bin/env bash
set -euo pipefail
label=${1:?baseline or candidate}
out=out/r16/runtime/$label
mkdir -p "$out"
adb wait-for-device
adb shell input keyevent 82
adb shell getprop > "$out/device.txt"
adb shell dumpsys SurfaceFlinger > "$out/driver.txt"
adb install -r "out/r16/$label/app.apk"
adb install -r "out/r16/$label/test.apk"
failed=0
for mode in local national; do
  dest="$out/$mode"
  mkdir -p "$dest"
  adb shell pm clear game.sanguo.mobile.dev
  adb logcat -c
  adb shell dumpsys battery > "$dest/battery-before.txt"
  adb shell dumpsys thermalservice > "$dest/thermal-before.txt"
  adb shell perfetto -o /data/misc/perfetto-traces/r16.trace -t 150s -b 32mb sched freq idle am wm gfx view binder_driver dalvik > "$dest/perfetto.log" 2>&1 &
  trace_job=$!
  adb shell screenrecord --time-limit 180 /sdcard/r16.mp4 > "$dest/record.txt" 2>&1 &
  record_job=$!
  code=0
  timeout 600 adb shell am instrument -w -e mode "$mode" game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR16Instrumentation > "$dest/instrumentation.txt" 2>&1 || code=$?
  printf '%s\n' "$code" > "$dest/process-exit.txt"
  adb shell pkill -INT screenrecord || true
  wait "$record_job" || true
  wait "$trace_job" || true
  adb pull /data/misc/perfetto-traces/r16.trace "$dest/trace.perfetto-trace" > "$dest/trace-pull.txt" 2>&1 || true
  adb logcat -d > "$dest/logcat.txt"
  adb shell dumpsys meminfo game.sanguo.mobile.dev > "$dest/meminfo.txt"
  adb shell dumpsys thermalservice > "$dest/thermal-after.txt"
  adb shell dumpsys SurfaceFlinger --list > "$dest/layers.txt"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$dest/images" || true
  adb pull /sdcard/r16.mp4 "$dest/" || true
  if [ "$code" != 0 ] || ! grep -q "PASS R16 $mode" "$dest/instrumentation.txt"; then failed=1; fi
  adb shell am force-stop game.sanguo.mobile.dev
done
exit "$failed"

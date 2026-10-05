#!/usr/bin/env bash
set -euo pipefail
source_sha=$(cat out/r18/SOURCE_COMMIT)
test "$source_sha" = "$GITHUB_SHA"
sha256sum -c out/r18/SHA256SUMS
out=out/r18/runtime
mkdir -p "$out"
adb wait-for-device
adb shell input keyevent 82
adb shell getprop > "$out/device.txt"
adb shell dumpsys SurfaceFlinger > "$out/driver.txt"
adb install -r out/r18/sanguo11-r18-v116.apk > "$out/install.txt"
adb install -r out/r18/test.apk >> "$out/install.txt"
adb shell dumpsys package game.sanguo.mobile.dev > "$out/package.txt"
remote_apk=$(adb shell pm path game.sanguo.mobile.dev | tr -d '\r' | sed -n 's/^package://p' | head -1)
adb pull "$remote_apk" "$out/installed.apk"
cmp out/r18/sanguo11-r18-v116.apk "$out/installed.apk"
sha256sum "$out/installed.apk" > "$out/installed.sha256"
rm "$out/installed.apk"
failed=0
for mode in recovery cold parity; do
  dest="$out/$mode"; mkdir -p "$dest"
  adb shell pm clear game.sanguo.mobile.dev > "$dest/clear.txt"
  adb logcat -c
  adb shell screenrecord --time-limit 180 /sdcard/r18.mp4 > "$dest/record.txt" 2>&1 &
  record_job=$!
  case "$mode" in
    recovery) runner=NativeR18Instrumentation; marker='PASS R18 recovery';;
    cold) runner=NativeColdStartInstrumentation; marker='PASS';;
    parity) runner=NativeP0ParityInstrumentation; marker='PASS P0_PARITY';;
  esac
  code=0
  timeout 1100 adb shell am instrument -w -e source "$source_sha" game.sanguo.mobile.dev.test/game.sanguo.mobile.$runner > "$dest/instrumentation.txt" 2>&1 || code=$?
  echo "$code" > "$dest/process.exit"
  adb shell pkill -INT screenrecord || true
  wait "$record_job" || true
  adb logcat -d > "$dest/logcat.txt"
  adb shell dumpsys meminfo game.sanguo.mobile.dev > "$dest/meminfo.txt"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$dest/images" || true
  adb pull /sdcard/r18.mp4 "$dest/" || true
  if [ "$code" != 0 ] || ! grep -q "$marker" "$dest/instrumentation.txt" || grep -Eq 'FAIL|INSTRUMENTATION_FAILED' "$dest/instrumentation.txt"; then failed=1; fi
  adb shell am force-stop game.sanguo.mobile.dev
done
exit "$failed"

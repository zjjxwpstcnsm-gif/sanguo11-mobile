#!/usr/bin/env bash
set -euo pipefail
root=out/feedback123
mkdir -p "$root/runtime"
adb wait-for-device
adb shell input keyevent 82
adb shell getprop > "$root/runtime/device.txt"
adb shell dumpsys SurfaceFlinger > "$root/runtime/driver.txt"
failed=0
for phase in baseline candidate; do
  if [ "$phase" = baseline ]; then apk="$root/baseline/sanguo11-mobile-v122.apk"; else apk="$root/sanguo11-mobile-v123.apk"; fi
  adb install -r "$apk" > "$root/runtime/$phase-install.txt"
  adb install -r "$root/test.apk" >> "$root/runtime/$phase-install.txt"
  remote=$(adb shell pm path game.sanguo.mobile.dev | tr -d '\r' | sed -n 's/^package://p' | head -1)
  adb pull "$remote" "$root/runtime/$phase-installed.apk"
  cmp "$apk" "$root/runtime/$phase-installed.apk"
  sha256sum "$root/runtime/$phase-installed.apk" > "$root/runtime/$phase-installed.sha256"
  for mode in fixture national port; do
    dest="$root/runtime/$phase-$mode"; mkdir -p "$dest"
    adb shell pm clear game.sanguo.mobile.dev > "$dest/clear.txt"
    adb logcat -c
    adb shell screenrecord --time-limit 180 /sdcard/feedback123.mp4 > "$dest/record.txt" 2>&1 &
    recording=$!
    code=0
    timeout 900 adb shell am instrument -w -e phase "$phase" -e mode "$mode" -e focused "${FEEDBACK123_FOCUSED:-false}" game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeFeedback123Instrumentation > "$dest/instrumentation.txt" 2>&1 || code=$?
    echo "$code" > "$dest/process.exit"
    adb shell pkill -INT screenrecord || true
    wait "$recording" || true
    adb logcat -d > "$dest/logcat.txt"
    adb shell dumpsys meminfo game.sanguo.mobile.dev > "$dest/meminfo.txt"
    adb shell dumpsys gfxinfo game.sanguo.mobile.dev > "$dest/gfxinfo.txt"
    adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$dest/images" || true
    adb pull /sdcard/feedback123.mp4 "$dest/" || true
    if [ "$code" != 0 ] || ! grep -q 'PASS FEEDBACK123' "$dest/instrumentation.txt" || grep -Eq 'FAIL|INSTRUMENTATION_FAILED' "$dest/instrumentation.txt"; then failed=1; fi
    adb shell am force-stop game.sanguo.mobile.dev
  done
done
exit "$failed"

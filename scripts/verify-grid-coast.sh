#!/usr/bin/env bash
set -euo pipefail
root=out/grid-coast
mkdir -p "$root/runtime"
adb wait-for-device
adb shell input keyevent 82
adb shell getprop > "$root/runtime/device.txt"
adb shell dumpsys SurfaceFlinger > "$root/runtime/driver.txt"
failed=0
for phase in baseline candidate; do
  dest="$root/runtime/$phase"; mkdir -p "$dest"
  if [ "$phase" = baseline ]; then
    apk="$root/baseline/sanguo11-r18-v116.apk"
    source=140ea58b2ae219960a62ab3ff05bd632a8b6b300
    echo "4ee59d43b91937bcda35bfc3b7b95ba504ba2bda4d54151d097fff4b5d3d37f7  $apk" | sha256sum -c -
  else
    apk="$root/sanguo11-mobile-v117.apk";source=$(cat "$root/SOURCE_COMMIT")
    test "$source" = "$GITHUB_SHA"
  fi
  adb install -r "$apk" > "$dest/install.txt"
  adb install -r "$root/test.apk" >> "$dest/install.txt"
  remote=$(adb shell pm path game.sanguo.mobile.dev | tr -d '\r' | sed -n 's/^package://p' | head -1)
  adb pull "$remote" "$dest/installed.apk"
  cmp "$apk" "$dest/installed.apk"
  sha256sum "$dest/installed.apk" > "$dest/installed.sha256"
  rm "$dest/installed.apk"
  adb shell pm clear game.sanguo.mobile.dev > "$dest/clear.txt"
  adb logcat -c
  adb shell screenrecord --time-limit 180 /sdcard/grid-coast.mp4 > "$dest/record.txt" 2>&1 &
  recording=$!
  code=0
  timeout 1000 adb shell am instrument -w -e phase "$phase" -e source "$source" game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeGridCoastInstrumentation > "$dest/instrumentation.txt" 2>&1 || code=$?
  echo "$code" > "$dest/process.exit"
  adb shell pkill -INT screenrecord || true
  wait "$recording" || true
  adb logcat -d > "$dest/logcat.txt"
  adb shell dumpsys meminfo game.sanguo.mobile.dev > "$dest/meminfo.txt"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$dest/images" || true
  adb pull /sdcard/grid-coast.mp4 "$dest/" || true
  if [ "$code" != 0 ] || ! grep -q 'PASS GRID_COAST' "$dest/instrumentation.txt" || grep -Eq 'FAIL|INSTRUMENTATION_FAILED' "$dest/instrumentation.txt"; then failed=1; fi
  adb shell am force-stop game.sanguo.mobile.dev
done
exit "$failed"

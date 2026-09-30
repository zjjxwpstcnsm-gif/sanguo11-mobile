#!/usr/bin/env bash
set -euo pipefail
main=${1:?candidate APK}
test_apk=${2:?test APK}
root=${3:-out/grid129/runtime}
mkdir -p "$root"
adb install -r "$main"
adb install -r "$test_apk"
path=$(adb shell pm path game.sanguo.mobile.dev | tr -d '\r' | sed -n 's/^package://p' | head -1)
adb pull "$path" "$root/installed.apk"
cmp "$root/installed.apk" "$main"
sha256sum "$root/installed.apk" "$main" > "$root/installed-apk-sha256.txt"
adb shell pm clear game.sanguo.mobile.dev
adb logcat -c
code=0
timeout 600 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeGrid129Instrumentation > "$root/instrumentation.txt" 2>&1 || code=$?
adb logcat -d > "$root/logcat.txt"
adb shell getprop > "$root/device.txt"
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$root/images" || true
test "$code" = 0
grep -q 'PASS GRID129' "$root/instrumentation.txt"
! grep -Eq 'FAIL|INSTRUMENTATION_FAILED' "$root/instrumentation.txt"

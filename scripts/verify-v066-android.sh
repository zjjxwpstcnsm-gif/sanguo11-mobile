#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/v066/android
adb wait-for-device
adb shell wm size 640x1280
adb shell wm density 320
adb shell settings put system font_scale 1.0
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
cleanup() {
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/governance66 out/v066/android/ >/dev/null 2>&1 || true
  adb logcat -d > out/v066/android/logcat.txt || true
  adb shell wm size reset || true
  adb shell wm density reset || true
}
trap cleanup EXIT
adb shell am instrument -w -e governance66 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee out/v066/android/instrumentation.txt
grep -q 'GOVERNANCE66 ANDROID PASS:' out/v066/android/instrumentation.txt
if grep -E 'FAILURES!!!|INSTRUMENTATION_FAILED|AssertionError|Process crashed' out/v066/android/instrumentation.txt; then exit 1; fi

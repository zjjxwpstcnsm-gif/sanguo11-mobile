#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/custom-officers/android
adb wait-for-device
adb shell wm size 720x1440
adb shell wm density 320
adb shell settings put system font_scale 1.0
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
cleanup() {
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/custom-officers out/custom-officers/android/ >/dev/null 2>&1 || true
  adb logcat -d > out/custom-officers/android/logcat.txt || true
  adb shell wm size reset || true
  adb shell wm density reset || true
}
trap cleanup EXIT
adb shell am instrument -w -e customOfficers create game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee out/custom-officers/android/create.txt
grep -q 'CUSTOM OFFICERS ANDROID PASS:' out/custom-officers/android/create.txt
adb shell am force-stop game.sanguo.mobile.dev
adb shell am instrument -w -e customOfficers restart game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee out/custom-officers/android/restart.txt
grep -q 'CUSTOM OFFICERS ANDROID PASS:' out/custom-officers/android/restart.txt
if grep -E 'FAILURES!!!|INSTRUMENTATION_FAILED|AssertionError|Process crashed' out/custom-officers/android/{create,restart}.txt; then exit 1; fi

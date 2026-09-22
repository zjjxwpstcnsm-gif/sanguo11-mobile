#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/editor67/android
adb wait-for-device
adb shell wm size 720x1440
adb shell wm density 320
adb shell settings put system font_scale 1.0
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell mkdir -p /sdcard/Android/data/game.sanguo.mobile.dev/files/editor67
adb push out/editor67/phone-export-fixture.json /sdcard/Android/data/game.sanguo.mobile.dev/files/editor67/fixture.json
adb logcat -c
cleanup(){
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/editor67 out/editor67/android/ >/dev/null 2>&1 || true
  adb logcat -d > out/editor67/android/logcat.txt || true
}
trap cleanup EXIT
adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.MapEditor67Instrumentation | tee out/editor67/android/instrumentation.txt
grep -q 'MAP_EDITOR67 ANDROID PASS:' out/editor67/android/instrumentation.txt
if grep -E 'FAILURES!!!|INSTRUMENTATION_FAILED|AssertionError|Process crashed|ANDROID FAIL' out/editor67/android/instrumentation.txt; then exit 1; fi

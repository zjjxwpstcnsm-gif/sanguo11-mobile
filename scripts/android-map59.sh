#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/android
apk=$(find out -maxdepth 1 -name 'sanguo11-mobile-v0.59.0-*.apk' -print -quit)
test -n "$apk"
sha256sum "$apk" > out/TESTED_APK_SHA256
cmp "$apk" app/build/outputs/apk/debug/app-debug.apk
adb shell wm size 1080x2340
adb shell wm density 420
adb install -r "$apk" | tee out/INSTALL.txt
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell mkdir -p /sdcard/Android/data/game.sanguo.mobile.dev/files/legacy59
for scenario in coalition-190 heroes-250 central-mobile-sandbox jingxiang-mobile-sandbox; do
  adb push "out/baseline/android/baseline-v058-$scenario.sg11" /sdcard/Android/data/game.sanguo.mobile.dev/files/legacy59/
done
adb shell dumpsys package game.sanguo.mobile.dev > out/INSTALLED_PACKAGE.txt
adb logcat -b all -c
adb logcat -G 16M || true
set +e
timeout 900 adb shell am instrument -w -e reference59 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner > out/reference59-instrumentation.txt
instrumentation_exit=$?
set -e
adb logcat -b all -d -v threadtime > out/reference59-logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/reference59
reference=false
if test "$instrumentation_exit" -eq 0 && grep -q 'REFERENCE59 ANDROID PASS:' out/reference59-instrumentation.txt && ! grep -q 'FATAL EXCEPTION' out/reference59-logcat.txt; then reference=true; fi
adb shell am force-stop game.sanguo.mobile.dev
adb logcat -b all -c
set +e
timeout 400 adb shell am instrument -w -e native56 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner > out/preserved-instrumentation.txt
preserved_exit=$?
set -e
adb logcat -b all -d -v threadtime > out/preserved-logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/preserved
preserved=false
if test "$preserved_exit" -eq 0 && grep -q 'NATIVE56 ANDROID PASS:' out/preserved-instrumentation.txt && ! grep -q 'FATAL EXCEPTION' out/preserved-logcat.txt; then preserved=true; fi
printf '{"reference59":%s,"retained_native200_gameplay":%s,"api":29,"abi":"x86_64","physical_arm_tested":false}\n' "$reference" "$preserved" > out/ANDROID_STATUS.json
sha256sum -c out/TESTED_APK_SHA256
$reference && $preserved

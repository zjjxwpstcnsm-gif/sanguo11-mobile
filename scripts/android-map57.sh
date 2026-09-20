#!/usr/bin/env bash
set -uo pipefail
mkdir -p out/android
adb shell wm size 1080x2340
adb shell wm density 420
adb install -r out/baseline-v056.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
(timeout 120 adb shell am instrument -w -e repro57 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner || true) | tee out/baseline-instrumentation.txt
adb logcat -d > out/baseline-logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/baseline || true
baseline=false
if grep -q 'FATAL EXCEPTION' out/baseline-logcat.txt && grep -q 'ArrayIndexOutOfBoundsException' out/baseline-logcat.txt && grep -q 'MainActivity.showTerrain' out/baseline-logcat.txt; then baseline=true; fi
adb shell am force-stop game.sanguo.mobile.dev
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell setprop log.tag.MapTap57 DEBUG
adb logcat -c
(timeout 600 adb shell am instrument -w -e mapTap57 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner || true) | tee out/v057-instrumentation.txt
adb logcat -d > out/v057-logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/fixed || true
fixed=false
if grep -q 'MAP57 ANDROID PASS:' out/v057-instrumentation.txt && ! grep -q 'FATAL EXCEPTION' out/v057-logcat.txt; then fixed=true; fi
adb shell am force-stop game.sanguo.mobile.dev
adb logcat -c
(timeout 400 adb shell am instrument -w -e native56 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner || true) | tee out/native56-instrumentation.txt
adb logcat -d > out/preserved-gameplay-logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/preserved || true
preserved=false
if grep -q 'NATIVE56 ANDROID PASS:' out/native56-instrumentation.txt && ! grep -q 'FATAL EXCEPTION' out/preserved-gameplay-logcat.txt; then preserved=true; fi
printf '{"baseline_crash_reproduced":%s,"fixed_taps_and_saves":%s,"preserved_gameplay":%s}\n' "$baseline" "$fixed" "$preserved" > out/ANDROID_STATUS.json
# Baseline is internal test material only; do not confuse it with the delivered new APK.
rm -f out/baseline-v056.apk
$baseline && $fixed && $preserved

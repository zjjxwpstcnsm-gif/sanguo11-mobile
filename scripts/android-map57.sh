#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/android
# Keep complete raw buffers AND marker-bounded stage logs. Old crash-buffer records
# sometimes survive adb clearing; they must not be attributed to a new process/run.
start_phase(){
  adb logcat -b all -c || true
  adb shell log -p i -t Map57Stage "BEGIN_$1"
}
capture_phase(){
  adb logcat -b all -d -v threadtime > "out/$2-logcat-full.txt"
  awk -v marker="BEGIN_$1" '/Map57Stage/ && index($0,marker){seen=1} seen' "out/$2-logcat-full.txt" > "out/$2-logcat.txt"
  grep -q "BEGIN_$1" "out/$2-logcat.txt"
}
adb shell wm size 1080x2340
adb shell wm density 420
adb install -r out/baseline-v056.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
start_phase BASELINE
(timeout 120 adb shell am instrument -w -e repro57 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner || true) | tee out/baseline-instrumentation.txt
capture_phase BASELINE baseline
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/baseline || true
baseline=false
if grep -q 'FATAL EXCEPTION' out/baseline-logcat.txt && grep -q 'ArrayIndexOutOfBoundsException' out/baseline-logcat.txt && grep -q 'MainActivity.showTerrain' out/baseline-logcat.txt; then baseline=true; fi
adb shell am force-stop game.sanguo.mobile.dev
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell dumpsys package game.sanguo.mobile.dev > out/INSTALLED_PACKAGE.txt
adb shell setprop log.tag.MapTap57 DEBUG
start_phase FIXED
(timeout 600 adb shell am instrument -w -e mapTap57 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner || true) | tee out/v057-instrumentation.txt
capture_phase FIXED v057
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/fixed || true
fixed=false
if grep -q 'MAP57 ANDROID PASS:' out/v057-instrumentation.txt && ! grep -q 'FATAL EXCEPTION' out/v057-logcat.txt; then fixed=true; fi
adb shell am force-stop game.sanguo.mobile.dev
start_phase PRESERVED
(timeout 400 adb shell am instrument -w -e native56 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner || true) | tee out/native56-instrumentation.txt
capture_phase PRESERVED preserved-gameplay
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/preserved || true
preserved=false
if grep -q 'NATIVE56 ANDROID PASS:' out/native56-instrumentation.txt && ! grep -q 'FATAL EXCEPTION' out/preserved-gameplay-logcat.txt; then preserved=true; fi
printf '{"baseline_crash_reproduced":%s,"fixed_taps_and_saves":%s,"preserved_gameplay":%s}\n' "$baseline" "$fixed" "$preserved" > out/ANDROID_STATUS.json
rm -f out/baseline-v056.apk
$baseline && $fixed && $preserved

#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/r14
collect(){
 touch out/r14/stop-recording
 adb shell pkill -INT screenrecord || true
 if [ -n "${record_job:-}" ]; then wait "$record_job" || true; fi
 for part in {1..14}; do adb pull "/sdcard/r14-operation-$part.mp4" out/r14/ || true; done
 adb logcat -d > out/r14/logcat.txt
 adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 out/r14/images || true
 adb shell dumpsys meminfo game.sanguo.mobile.dev > out/r14/meminfo.txt
}
trap collect EXIT
adb root; adb wait-for-device
adb shell setprop dalvik.vm.heapgrowthlimit 256m
adb shell setprop dalvik.vm.heapsize 512m
adb shell setprop sys.boot_completed 0; adb shell stop; adb shell start
adb shell 'for i in $(seq 1 60); do [ "$(getprop sys.boot_completed)" = "1" ] && exit 0; sleep 1; done; exit 1'
adb shell input keyevent 82
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear game.sanguo.mobile.dev
adb logcat -c
(for part in {1..14}; do [ ! -f out/r14/stop-recording ] || break; adb shell screenrecord --time-limit 180 "/sdcard/r14-operation-$part.mp4"; done) > out/r14/recording.txt 2>&1 &
record_job=$!
timeout 2500 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR14Instrumentation > out/r14/interaction.txt
grep -q 'PASS R14' out/r14/interaction.txt

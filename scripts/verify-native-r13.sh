#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/r13
legacy_status=0
bash scripts/verify-map-editor67-android.sh || legacy_status=$?
printf '%s\n' "$legacy_status" > out/r13/legacy-editor-exit.txt
adb shell pm clear game.sanguo.mobile.dev
collect(){
 touch out/r13/stop-recording
 adb shell pkill -INT screenrecord || true
 if [ -n "${record_job:-}" ]; then wait "$record_job" || true; fi
 for part in {1..10}; do adb pull "/sdcard/r13-operation-$part.mp4" out/r13/ || true; done
 adb logcat -d > out/r13/logcat.txt
 adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 out/r13/images || true
 adb shell dumpsys meminfo game.sanguo.mobile.dev > out/r13/meminfo.txt
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
adb shell mkdir -p /sdcard/Android/data/game.sanguo.mobile.dev/files/editor67
adb push out/editor67/phone-export-fixture.json /sdcard/Android/data/game.sanguo.mobile.dev/files/editor67/fixture.json
adb logcat -c
(for part in {1..10}; do [ ! -f out/r13/stop-recording ] || break; adb shell screenrecord --time-limit 180 "/sdcard/r13-operation-$part.mp4"; done) > out/r13/recording.txt 2>&1 &
record_job=$!
timeout 2200 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR13Instrumentation > out/r13/interaction.txt
grep -q 'PASS R13' out/r13/interaction.txt

test "$legacy_status" = 0

#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/r11
collect(){
 adb shell pkill -INT screenrecord || true
 if [ -n "${record_job:-}" ]; then kill "$record_job" 2>/dev/null || true; fi
 for part in {1..10}; do adb pull "/sdcard/r11-operation-$part.mp4" out/r11/ || true; done
 adb logcat -d > out/r11/logcat.txt
 adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 out/r11/images || true
 adb shell dumpsys meminfo game.sanguo.mobile.dev > out/r11/meminfo.txt
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
adb logcat -c
(for part in {1..10}; do adb shell screenrecord --time-limit 180 "/sdcard/r11-operation-$part.mp4"; done) > out/r11/recording.txt 2>&1 &
record_job=$!
timeout 2200 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR11Instrumentation > out/r11/interaction.txt
grep -q 'PASS R11' out/r11/interaction.txt

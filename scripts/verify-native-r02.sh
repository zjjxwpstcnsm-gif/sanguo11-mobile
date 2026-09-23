#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/r02
collect(){
 adb shell pkill -INT screenrecord || true
 adb pull /sdcard/r02-operation.mp4 out/r02/r02-operation.mp4 || true
 adb logcat -d > out/r02/logcat.txt
 adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 out/r02/images || true
 adb shell dumpsys meminfo game.sanguo.mobile.dev > out/r02/meminfo.txt
}
trap collect EXIT
adb root
adb wait-for-device
adb shell setprop dalvik.vm.heapgrowthlimit 256m
adb shell setprop dalvik.vm.heapsize 512m
adb shell setprop sys.boot_completed 0
adb shell stop
adb shell start
adb shell 'for i in $(seq 1 60); do [ "$(getprop sys.boot_completed)" = "1" ] && exit 0; sleep 1; done; exit 1'
adb shell input keyevent 82
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
adb shell screenrecord --time-limit 180 /sdcard/r02-operation.mp4 > out/r02/recording.txt 2>&1 &
timeout 1500 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR02Instrumentation > out/r02/interaction.txt
grep -q 'PASS R02' out/r02/interaction.txt

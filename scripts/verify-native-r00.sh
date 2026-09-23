#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/r00
collect() {
  adb logcat -d > out/r00/logcat.txt
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 out/r00/images || true
  adb shell dumpsys meminfo game.sanguo.mobile.dev > out/r00/meminfo.txt
  adb shell dumpsys SurfaceFlinger > out/r00/surfaceflinger.txt
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
adb shell am instrument -w -e mode fresh game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR00Instrumentation > out/r00/fresh.txt
grep -q 'PASS R00 fresh' out/r00/fresh.txt
adb shell am force-stop game.sanguo.mobile.dev
adb shell am start -W -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity > out/r00/cold-launch.txt
sleep 15
adb exec-out screencap -p > out/r00/cold-launch.png
adb logcat -d > out/r00/cold-launch-logcat.txt
adb shell am force-stop game.sanguo.mobile.dev
adb shell am instrument -w -e mode reload game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR00Instrumentation > out/r00/reload.txt
grep -q 'PASS R00 reload' out/r00/reload.txt

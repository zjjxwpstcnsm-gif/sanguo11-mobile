#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/r00
collect() {
  adb shell pkill -INT screenrecord || true
  adb logcat -d > out/r00/logcat.txt
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 out/r00/images || true
  adb pull /sdcard/r00-operation.mp4 out/r00/r00-operation.mp4 || true
  adb pull /data/user/0/game.sanguo.mobile.dev/files/auto.sg11 out/r00/auto.sg11 || true
  adb pull /data/user/0/game.sanguo.mobile.dev/files/manual.sg11 out/r00/manual.sg11 || true
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
# Baseline reproduction uses the verified pre-R00 APK and a fixed real official save,
# independently of the candidate's normal-new-game test below.
adb install /tmp/r00-baseline/sanguo11-architecture-native.apk
adb shell am start -W -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity > out/r00/baseline-menu-launch.txt
sleep 5
adb exec-out screencap -p > out/r00/baseline-menu.png
adb shell am force-stop game.sanguo.mobile.dev
adb push /tmp/r00-baseline.sg11 /data/user/0/game.sanguo.mobile.dev/files/auto.sg11
app_uid=$(adb shell stat -c %u /data/user/0/game.sanguo.mobile.dev | tr -d '\r')
adb shell chown "$app_uid:$app_uid" /data/user/0/game.sanguo.mobile.dev/files/auto.sg11
adb shell chmod 600 /data/user/0/game.sanguo.mobile.dev/files/auto.sg11
adb logcat -c
adb shell am start -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity > out/r00/baseline-save-launch.txt
sleep 3
adb exec-out screencap -p > out/r00/baseline-save-3s.png
sleep 15
adb exec-out screencap -p > out/r00/baseline-save-18s.png
adb logcat -d > out/r00/baseline-cold-logcat.txt
adb uninstall game.sanguo.mobile.dev
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
adb shell screenrecord --time-limit 180 /sdcard/r00-operation.mp4 > out/r00/screenrecord.txt 2>&1 &
record_job=$!
timeout 600 adb shell am instrument -w -e mode fresh game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR00Instrumentation > out/r00/fresh.txt
grep -q 'PASS R00 fresh' out/r00/fresh.txt
adb shell pkill -INT screenrecord || true
wait "$record_job" || true
adb shell am force-stop game.sanguo.mobile.dev
adb shell am start -W -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity > out/r00/cold-launch.txt
sleep 15
adb exec-out screencap -p > out/r00/cold-launch.png
adb logcat -d > out/r00/cold-launch-logcat.txt
adb shell am force-stop game.sanguo.mobile.dev
timeout 600 adb shell am instrument -w -e mode reload game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR00Instrumentation > out/r00/reload.txt
grep -q 'PASS R00 reload' out/r00/reload.txt

#!/usr/bin/env bash
set -euo pipefail
mode=baseline
case "$mode" in baseline|candidate) ;; *) echo "Unknown mode: $mode" >&2; exit 2;; esac
root="out/remediation/$mode"; mkdir -p "$root"
adb root; adb wait-for-device
adb shell setprop dalvik.vm.heapgrowthlimit 256m
adb shell setprop dalvik.vm.heapsize 512m
adb shell setprop sys.boot_completed 0; adb shell stop; adb shell start
adb shell 'for i in $(seq 1 60); do [ "$(getprop sys.boot_completed)" = "1" ] && exit 0; sleep 1; done; exit 1'
adb shell input keyevent 82
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
label=focused-window
mkdir -p "$root/$label"
adb shell am force-stop game.sanguo.mobile.dev
adb shell pm clear game.sanguo.mobile.dev
adb logcat -c
rc=0
timeout 2200 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeAuditInstrumentation > "$root/$label/interaction.txt" 2>&1 || rc=$?
printf '%s\n' "$rc" > "$root/$label/shell-exit.txt"
adb logcat -d > "$root/$label/logcat.txt"
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$root/$label/images"
adb shell dumpsys meminfo game.sanguo.mobile.dev > "$root/$label/meminfo.txt"
printf 'Original v105 APK. No screenrecord process started. Original instrumentation and independent PNG captures unchanged.\n' > "$root/CAPTURE_CONTROL.txt"
grep -q 'PASS AUDIT' "$root/$label/interaction.txt"

#!/usr/bin/env bash
set -euo pipefail
mode="${1:-candidate}"
case "$mode" in baseline|candidate) ;; *) echo "Unknown mode: $mode" >&2; exit 2;; esac
root="out/audit/$mode"; mkdir -p "$root"
adb root; adb wait-for-device
adb shell setprop dalvik.vm.heapgrowthlimit 256m
adb shell setprop dalvik.vm.heapsize 512m
adb shell setprop sys.boot_completed 0; adb shell stop; adb shell start
adb shell 'for i in $(seq 1 60); do [ "$(getprop sys.boot_completed)" = "1" ] && exit 0; sleep 1; done; exit 1'
adb shell input keyevent 82
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
run_probe(){
  local cls="$1" label="$2"
  mkdir -p "$root/$label"
  adb shell am force-stop game.sanguo.mobile.dev
  adb shell pm clear game.sanguo.mobile.dev
  adb logcat -c
  set +e
  timeout 2200 adb shell am instrument -w "game.sanguo.mobile.dev.test/game.sanguo.mobile.$cls" > "$root/$label/interaction.txt" 2>&1
  local rc=$?
  set -e
  printf '%s\n' "$rc" > "$root/$label/shell-exit.txt"
  adb logcat -d > "$root/$label/logcat.txt"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$root/$label/images" || true
  adb shell dumpsys meminfo game.sanguo.mobile.dev > "$root/$label/meminfo.txt"
}
run_probe NativeAuditInstrumentation focused-window
if [[ "$mode" == baseline ]]; then
  # A raw FAIL is required: this is a bug reproduction, NOT a baseline runtime PASS.
  grep -q 'FAIL AUDIT' "$root/focused-window/interaction.txt"
  grep -q 'covered map stops native frame callback' "$root/focused-window/interaction.txt"
  printf '%s\n' 'REPRODUCED: unchanged R14 keeps submitting while covered; raw instrumentation FAIL preserved.' > "$root/CLASSIFICATION.txt"
else
  audit_rc=0; grep -q 'PASS AUDIT' "$root/focused-window/interaction.txt" || audit_rc=1
  run_probe NativeR12Instrumentation r12-full
  r12_rc=0; grep -q 'PASS R12' "$root/r12-full/interaction.txt" || r12_rc=1
  printf 'focused-window=%s\nr12-full=%s\n' "$audit_rc" "$r12_rc" > "$root/EXIT_CODES.txt"
  test "$audit_rc" = 0 && test "$r12_rc" = 0
fi

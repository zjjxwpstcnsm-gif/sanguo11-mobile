#!/usr/bin/env bash
set -euo pipefail
mode="${1:-candidate}"
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
run_probe(){
  local cls="$1" label="$2"
  mkdir -p "$root/$label"
  adb shell am force-stop game.sanguo.mobile.dev
  adb shell pm clear game.sanguo.mobile.dev
  adb logcat -c
  (for part in {1..10}; do adb shell screenrecord --time-limit 180 "/sdcard/remediation-$part.mp4"; done) > "$root/$label/recording.txt" 2>&1 &
  local recorder=$!
  set +e
  timeout 2200 adb shell am instrument -w "game.sanguo.mobile.dev.test/game.sanguo.mobile.$cls" > "$root/$label/interaction.txt" 2>&1
  local rc=$?
  set -e
  printf '%s\n' "$rc" > "$root/$label/shell-exit.txt"
  adb shell pkill -INT screenrecord || true
  kill "$recorder" 2>/dev/null || true
  for part in {1..10}; do adb pull "/sdcard/remediation-$part.mp4" "$root/$label/" >/dev/null 2>&1 || true; done
  adb shell rm -f '/sdcard/remediation-*.mp4'
  adb shell pidof game.sanguo.mobile.dev > "$root/$label/pid.txt" || true
  adb logcat -d > "$root/$label/logcat.txt"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$root/$label/images" || true
  adb shell dumpsys meminfo game.sanguo.mobile.dev > "$root/$label/meminfo.txt"
}
run_probe NativeAuditInstrumentation focused-window
audit_rc=0; grep -q 'PASS AUDIT' "$root/focused-window/interaction.txt" || audit_rc=1
run_probe NativeR12Instrumentation r12-full
r12_rc=0; grep -q 'PASS R12' "$root/r12-full/interaction.txt" || r12_rc=1
printf 'focused-window=%s\nr12-full=%s\n' "$audit_rc" "$r12_rc" > "$root/EXIT_CODES.txt"
cold_rc=0
if [[ "$mode" = candidate ]]; then
  run_probe NativeColdStartInstrumentation cold-start
  grep -q 'PASS COLD_START' "$root/cold-start/interaction.txt" || cold_rc=1
  printf 'cold-start=%s\n' "$cold_rc" >> "$root/EXIT_CODES.txt"
fi
test "$audit_rc" = 0 && test "$r12_rc" = 0 && test "$cold_rc" = 0

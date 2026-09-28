#!/usr/bin/env bash
set -euo pipefail
out=out/r17/runtime/api${1:?API}
mkdir -p "$out"
adb wait-for-device
adb shell input keyevent 82
adb shell getprop > "$out/device.txt"
adb shell dumpsys SurfaceFlinger > "$out/driver.txt"
adb install -r out/r17/previous.apk > "$out/install-previous.txt"
adb install -r out/r17/test.apk > "$out/install-test.txt"
# No pm clear/uninstall: ordinary save and settings must survive actual replacement.
adb shell am instrument -w -e mode seed game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR17Instrumentation > "$out/seed.txt" 2>&1
grep -q 'PASS R17 seed' "$out/seed.txt"
adb shell am force-stop game.sanguo.mobile.dev
adb install -r out/r17/sanguo11-r17-v115.apk > "$out/install-upgrade.txt"
failed=0
adb shell am instrument -w -e mode verify game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR17Instrumentation > "$out/upgrade.txt" 2>&1 || failed=1
grep -q 'PASS R17 verify' "$out/upgrade.txt" || failed=1
adb logcat -d > "$out/upgrade-logcat.txt"
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$out/upgrade-files" || true
# Installed exact production configuration and same authoritative fixtures.
timeout 1200 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeP0ParityInstrumentation > "$out/parity.txt" 2>&1 || failed=1
grep -q 'PASS P0' "$out/parity.txt" || failed=1
adb logcat -d > "$out/parity-logcat.txt"
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 "$out/parity-files" || true
adb shell dumpsys package game.sanguo.mobile.dev > "$out/package.txt"
exit "$failed"

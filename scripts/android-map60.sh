#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/android
apk=$(find out -maxdepth 1 -name 'sanguo11-mobile-v0.60.0-*.apk' -print -quit)
test -n "$apk"
sha256sum "$apk" > out/TESTED_APK_SHA256
cmp "$apk" app/build/outputs/apk/debug/app-debug.apk
adb root
adb wait-for-device
adb shell wm size 1080x2340
adb shell wm density 420
# Real signed in-place upgrade, preserving a real old APK manual file.
adb install -r out/upgrade-v059.apk > out/UPGRADE_BASE_INSTALL.txt
adb push out/legacy60/raw/baseline-v059-coalition-190.sg11 /data/local/tmp/legacy-v059.sg11
adb shell mkdir -p /data/data/game.sanguo.mobile.dev/files
adb shell cp /data/local/tmp/legacy-v059.sg11 /data/data/game.sanguo.mobile.dev/files/manual1.sg11
uid=$(adb shell stat -c %u /data/data/game.sanguo.mobile.dev | tr -d '\r')
adb shell chown "$uid:$uid" /data/data/game.sanguo.mobile.dev/files /data/data/game.sanguo.mobile.dev/files/manual1.sg11
adb shell chmod 700 /data/data/game.sanguo.mobile.dev/files
adb shell chmod 600 /data/data/game.sanguo.mobile.dev/files/manual1.sg11
adb shell sha256sum /data/data/game.sanguo.mobile.dev/files/manual1.sg11 > out/UPGRADE_SAVE_BEFORE
adb install -r "$apk" | tee out/INSTALL.txt
adb shell sha256sum /data/data/game.sanguo.mobile.dev/files/manual1.sg11 > out/UPGRADE_SAVE_AFTER
cmp out/UPGRADE_SAVE_BEFORE out/UPGRADE_SAVE_AFTER
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell mkdir -p /sdcard/Android/data/game.sanguo.mobile.dev/files/legacy60
for file in out/legacy60/raw/*.sg11;do adb push "$file" /sdcard/Android/data/game.sanguo.mobile.dev/files/legacy60/;done
adb shell dumpsys package game.sanguo.mobile.dev > out/INSTALLED_PACKAGE.txt
adb logcat -b all -c
adb logcat -G 16M || true
set +e
timeout 1100 adb shell am instrument -w -e reference60 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner > out/reference60-instrumentation.txt
instrumentation_exit=$?
set -e
adb logcat -b all -d -v threadtime > out/reference60-logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/reference60
reference=false
if test "$instrumentation_exit" -eq 0 && grep -q 'REFERENCE60 ANDROID PASS:' out/reference60-instrumentation.txt && ! grep -q 'FATAL EXCEPTION' out/reference60-logcat.txt;then reference=true;fi
adb shell am force-stop game.sanguo.mobile.dev
# Remove only copied instrumentation output in this disposable emulator, never saves.
adb shell rm -rf /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke
adb logcat -b all -c
set +e
timeout 400 adb shell am instrument -w -e native56 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner > out/preserved-instrumentation.txt
preserved_exit=$?
set -e
adb logcat -b all -d -v threadtime > out/preserved-logcat.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/smoke out/android/preserved
preserved=false
if test "$preserved_exit" -eq 0 && grep -q 'NATIVE56 ANDROID PASS:' out/preserved-instrumentation.txt && ! grep -q 'FATAL EXCEPTION' out/preserved-logcat.txt;then preserved=true;fi
printf '{"reference60":%s,"retained_native200_gameplay":%s,"in_place_signed_upgrade":true,"old_manual_save_byte_identical_after_install":true,"api":29,"abi":"x86_64","physical_arm_tested":false}\n' "$reference" "$preserved" > out/ANDROID_STATUS.json
sha256sum -c out/TESTED_APK_SHA256
$reference && $preserved

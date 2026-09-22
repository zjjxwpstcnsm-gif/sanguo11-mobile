#!/usr/bin/env bash
set -euo pipefail
[ "${1:-false}" = "true" ] || exit 0
adb shell settings put global animator_duration_scale 0
adb shell screenrecord --time-limit 150 /sdcard/field-scene.mp4 >/dev/null 2>&1 &
timeout 600 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.FieldSceneInstrumentation | tee evidence/field-installed.txt
timeout 600 adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.SceneInstrumentation | tee evidence/installed.txt
adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files/s01 evidence/installed-full
adb logcat -d > evidence/logcat-full.txt
python3 scripts/field-ci-summary.py evidence/logcat-full.txt
adb pull /sdcard/field-scene.mp4 evidence/field-scene.mp4
adb pull /sdcard/field-port.mp4 evidence/field-port.mp4
grep -q 'PASS S04 S05' evidence/field-installed.txt
grep -q 'PASS S01' evidence/installed.txt

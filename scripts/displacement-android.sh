#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/displacement
adb shell wm size 1080x1920
adb shell wm density 420
adb uninstall game.sanguo.mobile.dev || true
adb uninstall game.sanguo.mobile.dev.test || true
for phase in before after; do
  mkdir -p "app/build/displacement/$phase"
  if [ "$phase" = before ]; then
    adb install app/build/baseline/v028.apk
    adb install app/build/baseline/observer.apk
  else
    # In-place replacement keeps the real v0.28 game and UI files.
    adb exec-out run-as game.sanguo.mobile.dev cat files/auto.sg11 > app/build/displacement/upgrade-v028.sg11
    adb push app/build/displacement/upgrade-v028.sg11 /sdcard/Android/data/game.sanguo.mobile.dev/files/upgrade-v028.sg11
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    adb shell am instrument -w -e displacement upgrade game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee app/build/displacement/upgrade.txt
    python3 -c 'from pathlib import Path; assert "UPGRADE28 PASS" in Path("app/build/displacement/upgrade.txt").read_text()'
  fi
  adb logcat -c
  adb shell am instrument -w -e displacement "$phase" game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee "app/build/displacement/$phase/instrumentation.txt"
  adb logcat -d > "app/build/displacement/$phase/logcat.txt"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files "app/build/displacement/$phase/files"
  python3 - "$phase" <<'PY'
from pathlib import Path
import sys
p=Path('app/build/displacement')/sys.argv[1]
s=(p/'instrumentation.txt').read_text()
assert 'DISPLACEMENT PASS' in s and 'SMOKE FAIL' not in s,s
logs=(p/'logcat.txt').read_text()
assert 'FATAL EXCEPTION' not in logs and 'ANR in game.sanguo.mobile' not in logs
PY
done

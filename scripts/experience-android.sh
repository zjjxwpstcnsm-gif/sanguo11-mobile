#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/experience
adb shell wm size 1080x1920
adb shell wm density 420
# Observe the candidate first so failures surface before repeating the pinned old flows.
# Both versions still run on this one emulator when the comparison succeeds.
for phase in after before; do
  adb uninstall game.sanguo.mobile.dev || true
  adb uninstall game.sanguo.mobile.dev.test || true
  if [ "$phase" = before ]; then
    adb install app/build/baseline/v027.apk
    adb install app/build/baseline/observer.apk
  else
    adb install app/build/outputs/apk/debug/app-debug.apk
    adb install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  fi
  adb logcat -c
  mkdir -p "app/build/experience/$phase"
  adb shell am instrument -w -e experience true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner | tee "app/build/experience/$phase/instrumentation.txt"
  adb logcat -d > "app/build/experience/$phase/logcat.txt"
  adb shell dumpsys gfxinfo game.sanguo.mobile.dev > "app/build/experience/$phase/gfxinfo.txt"
  adb pull /sdcard/Android/data/game.sanguo.mobile.dev/files "app/build/experience/$phase/files"
  python3 - "$phase" <<'CHECK'
from pathlib import Path
import sys
folder=Path('app/build/experience')/sys.argv[1]
s=(folder/'instrumentation.txt').read_text()
assert 'EXPERIENCE PASS' in s and 'SMOKE FAIL' not in s,s
logs=(folder/'logcat.txt').read_text()
assert 'FATAL EXCEPTION' not in logs and 'ANR in game.sanguo.mobile' not in logs
CHECK
done

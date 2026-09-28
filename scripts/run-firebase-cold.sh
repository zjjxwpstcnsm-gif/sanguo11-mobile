#!/usr/bin/env bash
set -euo pipefail
: "${FIREBASE_PROJECT_ID:?}" "${MODEL:?Choose a physical model from catalog}" "${API:?}" "${REPEATS:?}"
[[ "$API" =~ ^(29|35)$ && "$REPEATS" =~ ^[1-3]$ ]] || exit 2
python3 - <<'PY'
import json, os
models = json.load(open('out/firebase-results/models.json'))
matches = [m for m in models if m['id'] == os.environ['MODEL']]
assert len(matches) == 1, 'Model absent from current catalog'
m = matches[0]
assert m.get('form') == 'PHYSICAL', 'Physical hardware required; no emulator substitution'
assert os.environ['API'] in m.get('supportedVersionIds', []), 'Requested original API unavailable on this model'
PY
failed=0
for ((attempt=1; attempt<=REPEATS; attempt++)); do
  folder="out/firebase-results/cold-$attempt"
  mkdir -p "$folder"
  # Separate matrices mean fresh app installation, never a warm in-process loop.
  code=0
  gcloud firebase test android run --quiet --project="$FIREBASE_PROJECT_ID" \
    --type=instrumentation \
    --app=out/firebase-build/app-debug.apk \
    --test=out/firebase-build/app-debug-androidTest.apk \
    --test-runner-class=game.sanguo.mobile.FirebaseColdStartInstrumentation \
    --device="model=$MODEL,version=$API,locale=zh_CN,orientation=landscape" \
    --timeout=10m --no-use-orchestrator --num-flaky-test-attempts=0 \
    --no-auto-google-login --record-video --no-performance-metrics \
    --directories-to-pull=/sdcard/Android/data/game.sanguo.mobile.dev/files/s01 \
    --results-dir="native-cold/${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}/api$API-$attempt" \
    --client-details="matrixLabel=native-cold-${GITHUB_SHA:0:12}-api$API-$attempt" \
    2>&1 | tee "$folder/gcloud.log" || code=$?
  echo "$code" > "$folder/GCLOUD_EXIT"
  # Preserve failure even when later independent attempts pass.
  if ((code != 0)); then failed=1; fi
  # Authentication/quota/infrastructure errors should not trigger more submissions.
  if ((code != 0 && code != 10)); then break; fi
done
exit "$failed"

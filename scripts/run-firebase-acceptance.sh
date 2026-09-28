#!/usr/bin/env bash
set -euo pipefail
: "${FIREBASE_PROJECT_ID:?}" "${AUDIT_MESSAGE:?}"
[[ "$GITHUB_RUN_ATTEMPT" == 1 ]] || { echo 'BLOCKED: manual workflow rerun would spend another execution'; exit 3; }
case "$AUDIT_MESSAGE" in
  'ci(firebase): acceptance api35 second 20260928') MODEL=shiba; API=35; AUDIT_LONG=false; TEST_TIMEOUT=15m ;;
  'ci(firebase): acceptance api35 third 20260928') MODEL=houji; API=35; AUDIT_LONG=true; TEST_TIMEOUT=43m ;;
  'ci(firebase): acceptance api29 second 20260928') MODEL=starlte; API=29; AUDIT_LONG=false; TEST_TIMEOUT=15m ;;
  *) exit 2 ;;
esac
export MODEL API
python3 - <<'PY'
import json,pathlib,os
p=pathlib.Path('out/firebase-preflight')
billing=json.load(open(p/'billing.json')); monitor=json.load(open(p/'usage-allocation-usage.json'))
assert billing.get('billingEnabled') is False or 'requires billing to be enabled' in monitor.get('error',{}).get('message',''), 'BLOCKED: unbilled project not confirmed'
u=json.load(open(p/'execution-usage.json'))
assert u['complete'] and u['rolling24hDeviceExecutions']<5,'BLOCKED: Spark free capacity exhausted or evidence incomplete'
m=next(m for m in json.load(open(p/'models.json')) if m['id']==os.environ['MODEL'])
assert m['form']=='PHYSICAL' and os.environ['API'] in m['supportedVersionIds'],'BLOCKED: exact API unavailable on physical model'
print('ONE_PHYSICAL_EXECUTION free-only:',u,'model',m['id'],'API',os.environ['API'])
PY
code=0
gcloud firebase test android run --quiet --project="$FIREBASE_PROJECT_ID" \
 --type=instrumentation --app=out/firebase-original/app-debug.apk \
 --test=out/firebase-audit/test-build/app-debug-androidTest.apk \
 --test-runner-class=game.sanguo.mobile.FirebaseAcceptanceInstrumentation \
 --device="model=$MODEL,version=$API,locale=zh_CN,orientation=landscape" \
 --timeout="$TEST_TIMEOUT" --environment-variables="auditLong=$AUDIT_LONG" --no-use-orchestrator --num-flaky-test-attempts=0 --no-auto-google-login \
 --record-video --no-performance-metrics \
 --directories-to-pull=/sdcard/Android/data/game.sanguo.mobile.dev/files/s01 \
 --results-dir="native-audit/${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}/" \
 --client-details="matrixLabel=R00-R14-audit-${GITHUB_SHA:0:12}-api$API" \
 2>&1 | tee out/firebase-audit/gcloud.log || code=$?
printf '%s\n' "$code" > out/firebase-audit/GCLOUD_EXIT
exit "$code"

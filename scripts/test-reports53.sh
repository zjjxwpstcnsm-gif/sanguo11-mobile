#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/check dist
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/sources.txt
javac -encoding UTF-8 --release 17 -d core/build/check @core/build/sources.txt 2>&1 | tee dist/core-compile.txt
cp_current="core/build/check:core/src/main/resources:core/src/test/resources"
java -cp "$cp_current" game.sanguo.core.Reports53Test 2>&1 | tee dist/core-checks.txt
java -Xmx1500m -cp "$cp_current" game.sanguo.core.Reports53NationalTest 2>&1 | tee dist/national-checks.txt
base=$(mktemp -d)
git worktree add --detach "$base" 63a3d5e3f946145fb8b5e4a52b29d5d3cccaf140
trap 'git worktree remove --force "$base" >/dev/null 2>&1 || true' EXIT
(
  cd "$base"
  mkdir -p core/build/check
  find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/sources.txt
  javac -encoding UTF-8 --release 17 -d core/build/check @core/build/sources.txt
)
for suite in ArmyTest BattleFeedbackTest CoreTest; do
  set +e
  timeout 120 java -cp "$cp_current" game.sanguo.core.$suite > "dist/$suite-current.txt" 2>&1
  current=$?
  (cd "$base" && timeout 120 java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.$suite) > "dist/$suite-baseline.txt" 2>&1
  baseline=$?
  set -e
  cat "dist/$suite-current.txt"
  if [ "$current" = 0 ]; then
    echo "$suite PASS" | tee -a dist/core-checks.txt
  elif [ "$current" = "$baseline" ] && [ "$baseline" != 124 ] && diff <(grep -m1 'Exception in thread' "dist/$suite-current.txt") <(grep -m1 'Exception in thread' "dist/$suite-baseline.txt"); then
    grep -q 'Exception in thread' "dist/$suite-current.txt"
    echo "$suite EXISTING BASELINE FAILURE (not a new regression): $(grep -m1 'Exception in thread' "dist/$suite-baseline.txt")" | tee -a dist/core-checks.txt
  else
    echo "$suite NEW REGRESSION, current=$current baseline=$baseline"
    cat "dist/$suite-baseline.txt"
    exit 1
  fi
done
bash scripts/test-ui-models.sh 2>&1 | tee dist/ui-model-checks.txt

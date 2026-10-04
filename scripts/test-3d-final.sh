#!/usr/bin/env bash
# S09 host gate. All suites run, even when an earlier suite fails.
set -uo pipefail
cd "$(dirname "$0")/.."
out="${1:-build/s09-host}"
mkdir -p "$out"
printf 'suite\tstatus\n' > "$out/matrix.tsv"
failed=0
for suite in 3d-foundation 3d-interaction 3d-assets 3d-field 3d-combat ui-models city55 map61 v064 v066 ruler-titles siege map-editor67 custom-officers turn48; do
  if bash "scripts/test-$suite.sh" > "$out/$suite.log" 2>&1; then result=PASS; else result=FAIL; failed=1; fi
  printf '%s\t%s\n' "$suite" "$result" | tee -a "$out/matrix.tsv"
done
exit "$failed"

#!/usr/bin/env bash
# Run every invocation from the unchanged original suite, preserving all exit codes.
# Unlike test-core.sh this collector continues after an assertion, not after a failed compile.
set -uo pipefail
cd "$(dirname "$0")/.."
root="$(realpath -m "${1:?output directory required}")"
mkdir -p "$root"
: > "$root/EXIT_CODES.tsv"
count=0
javac(){ command javac "$@" || exit $?; }
java(){
  if [[ "${1:-}" = -m && "${2:-}" = jdk.compiler/com.sun.tools.javac.Main ]]; then
    command java "$@" || exit $?
    return 0
  fi
  count=$((count+1))
  printf -v n '%03d' "$count"
  printf '%q ' java "$@" > "$root/$n.command"
  command java "$@" > "$root/$n.log" 2>&1
  local code=$?
  printf '%s\t%s\t%s\n' "$n" "$code" "$(cat "$root/$n.command")" >> "$root/EXIT_CODES.tsv"
  return "$code"
}
source <(sed -e '/^set -euo pipefail$/d' -e '/^cd "$(dirname "$0")\/\.\."$/d' scripts/test-core.sh)
awk -F '\t' '$2!=0 {failed=1} END{exit failed}' "$root/EXIT_CODES.tsv"
code=$?
printf '%s\n' "$code" > "$root/aggregate.exit"
exit "$code"

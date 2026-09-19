#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-turn47.sh
java -Xmx1200m -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.Turn48Test

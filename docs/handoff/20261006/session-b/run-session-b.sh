#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../../.."
export PYTHONDONTWRITEBYTECODE=1
export JAVA_HOME=/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home
export GRADLE_USER_HOME="$PWD/out/session-b/gradle-home"
export ANDROID_HOME=/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
export PYTHONPATH=/Users/paopao/workspace/sanguo11-mobile/out/toolchain/pc-emulate:/Users/paopao/workspace/sanguo11-mobile/out/toolchain/pc-inspect:"$PWD/tools/content${PYTHONPATH:+:$PYTHONPATH}"
exec "$@"

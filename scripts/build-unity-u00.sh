#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
editor="${UNITY_EDITOR:-}"
if [[ ! -x "$editor" ]]; then
  echo 'BLOCKED: UNITY_EDITOR must point to an executable Unity 6000.3.16f1 Editor with an authorized Android build license and Android Build Support.' >&2
  exit 2
fi
if [[ -z "${ANDROID_HOME:-}" || ! -d "$ANDROID_HOME" ]]; then
  echo 'BLOCKED: ANDROID_HOME must point to an installed SDK.' >&2
  exit 2
fi
output="${U00_OUTPUT:-$root/out/unity-u00}"
mkdir -p "$output"
export U00_UNITY_EXPORT="$output/export"
"$editor" -batchmode -nographics -quit -projectPath "$root/unity" \
  -executeMethod ExportAndroid.Run -logFile "$output/unity-export.log"
test -s "$U00_UNITY_EXPORT/unityLibrary/build.gradle"

# Reject a different Unity export before invoking Gradle. A blank export isn't a Unity APK.
python3 "$root/scripts/verify-unity-u00.py" export "$U00_UNITY_EXPORT"
"$root/gradlew" --no-daemon :app:assembleDebug \
  -PtargetAbi=arm64-v8a -PunityExport="$U00_UNITY_EXPORT" \
  > "$output/android-build.log" 2>&1 || { tail -n 80 "$output/android-build.log" >&2; exit 1; }
apk="$root/app/build/outputs/apk/debug/app-debug.apk"
python3 "$root/scripts/verify-unity-u00.py" apk "$apk"
cp "$apk" "$output/sanguo11-mobile-unity-u00.apk"
sha256sum "$output/sanguo11-mobile-unity-u00.apk" > "$output/SHA256SUMS.txt"
echo "APK: $output/sanguo11-mobile-unity-u00.apk"

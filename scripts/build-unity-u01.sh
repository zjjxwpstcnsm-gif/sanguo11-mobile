#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
export U00_OUTPUT="${U01_OUTPUT:-$root/out/unity-u01}"
bash "$root/scripts/build-unity-u00.sh"
source_apk="$U00_OUTPUT/sanguo11-mobile-unity-u00.apk"
final_apk="$U00_OUTPUT/sanguo11-mobile-unity-u01.apk"
test -s "$source_apk"
mv "$source_apk" "$final_apk"
python3 "$root/scripts/verify-unity-u00.py" apk "$final_apk"
sha256sum "$final_apk" > "$U00_OUTPUT/SHA256SUMS.txt"
git -C "$root" rev-parse HEAD > "$U00_OUTPUT/SOURCE_COMMIT"
echo "U01 APK: $final_apk"

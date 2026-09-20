# v0.56 IN PROGRESS — coordinate/recovery checkpoint, NOT a completed map release

Branch: `agent/native-map-200-city-art-v056` · draft PR #44.
Base main: `bb8e58ebe32e96ddf453ee68fd23e21509f71247` (v0.55.0 / 55).
Recovered transport HEAD: `d7f11e2dbee52598e5e89501949ef816acc517f3`.

The complete previous progress log is preserved byte-for-byte in [docs/history/progress-through-v055.md](docs/history/progress-through-v055.md).

## DONE

- Restored complete main source from Actions artifact 10600481282, run 35493986688; no downgrade to the historical APK source.
- Re-extracted the user attachment: MAP_SAN11.jpg, 7200×6752, SHA256 `a5a4e8c7f9785b6fbadc8487508ff6c937dcef2d782b099f2e876e48d167d4e0`. Reference image remains outside the repository/APK.
- Added typed `SourceGridCoord` and `MapCoordinates.toAxial/toSource`. The formal ScenarioData normalization path now uses the typed conversion; legacy callers retain equivalent odd-r behavior. Off-map neighbors remain algebraically reversible; bounds checking is explicit.
- Added `MapCoordinateTest` and `scripts/test-map56-coordinates.sh`, invoked by the existing UI-model CI entry point.
- Local Java 17-target tests: MapCoordinateTest 1,168,801 assertions across all 40,000 coordinates plus parity/direction/distance/bounds; CityFootprint55Test 11,070 checks; UI projection/camera 49,017; terrain connection 451; tile geometry 1,136,811. Counts include parameterized iterations, not distinct player workflows.

## IN PROGRESS

- Inspect/recover `.ci/native56-01.b64` through `.ci/native56-06.b64`. These six existing transport files have NOT been applied or certified. Do not report their commit count as completed source integration.
- Source-grid contract is implemented for the existing odd-r engine layout. Reference-image column staggering is a distinct calibration concern, not silently treated as identical.

## TODO

- Native national terrain/map identity/shared resource, 87-site calibrated migration, 591 plots, nine scenarios and save revision rejection.
- All 42 city atlases and near/mid/far rendering, gate/port artwork and Android projection/culling integration.
- Full v056 regression, marching scale, Android build/install/smoke/screenshots, APK unpack/signature/source identity checks.

## BLOCKER / validation limits

- Shell GitHub DNS resolution and git clone failed; the GitHub connector/source artifact path works. Use connector writes for this branch; do not force-push main.
- No Android SDK, Gradle distribution or emulator is installed in the active local environment. Java/core tests above ran locally. Android Actions results must be inspected independently.
- Version remains 0.55.0 until the actual v056 map is integrated. Formal national scenarios still load the old map. No new-city-art or native-map APK is claimed.
- Main's previous Android workflow failed; that historical failure is not evidence about this coordinate checkpoint.

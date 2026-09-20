# v0.56 native200 + city atlas — installed development build, acceptance in progress

Branch `agent/native-map-200-city-art-v056`, draft PR #44. Base main `bb8e58ebe32e96ddf453ee68fd23e21509f71247` is unchanged. Prior history is preserved in `docs/history/progress-through-v055.md`.

## DONE
- Formal native200 shared map, seven national scenarios and two explicit crops; 87 sites, 591 plots, 42 real seven-cell cities. Scenario political/personnel/resource fields retained.
- Source/axial/screen conversion, camera/culling, runtime movement/combat/AI and SaveCodec31 map identity/old-save rejection.
- All 42 cities bind the new CityArtCatalog/CityAtlas. Thirty real RGBA PNGs cover six city variants, independent Luoyang/Changan, gates/ports and all three LODs. Units render above cities; no old city-sprite fallback.
- Source and assets materialized and pushed in `d352f48ca3e1a5e15624ef8c2e6959b6327edaaf`.
- Delivery run 35509322568 passed targeted core/UI regression, Android build and actual installed Native56Probe: 92 checks, 29 screenshots, minimap, seven-cell touches, real sortie/transit/entry/edge attack, Next Turn, actual save/read and Activity recreation.
- d352f48 APK verified: 0.56.0/56, 22,330,815 bytes, SHA256 `0e1359e49d1c41804c0565772eb3e2840934476563422f95c89c72d9e55d4c05`; map and 30 PNGs present, reference image absent; unchanged development signing certificate. Final rebuild's BUILD_COMMIT/APK_IDENTITY supersedes this initial successful artifact.
- Region evidence corrected to include Jiangxia/Chaisang/Jiujiang/Lukou in the Jiang-Huai review envelope; validator checks all 87 coordinates against the actual national resource and regional membership. Evidence remains 48 calibrated / 39 estimated, not 87 visually confirmed.
- Fixed ContentIntegrationTest's manually injected fixture report baseline; all 143 assertions now pass without weakening its byte-equal deterministic save assertion. Full historical suite is not claimed green.

## IN PROGRESS
- Rebuild this final checkpoint (region/fixture/validation documentation) and repeat installed checks. Read its artifact before reporting final APK identity.
- Nationwide manual geographic review and visual quality acceptance.

## TODO
- Resolve noisy road/mountain/forest digitization and verify minor shoal/plank/dam features. Per-region records deliberately do not claim complete cell-by-cell visual confirmation.
- Further port/ship march benchmarks and physical ARM-device profiling. Emulator frame intervals were around 50 ms; this is NOT evidence of 60 FPS on a real phone.
- Historical ScenarioTest still expects automatic v1 migration, contrary to the explicit native200 old-save rejection policy. Other old workflow/fixture expectations need reconciliation; no tests were removed to create a green full-suite claim.
- The old materialize_native56.py is a one-shot recovery record, not the authority for ongoing review edits. Do not rerun it over newer manual evidence; validate_native56_evidence.py detects incomplete memberships.

## BLOCKER / provenance
- No shell GitHub DNS, local SDK or physical Android device; authenticated connector and Actions used. Main was not force-pushed.
- Original transport lacked its tail. Only the complete first 48 file patches were applied after full SHA/post-image verification. Missing original extraction/calibration code was not invented. The original MAP_SAN11 image is outside the repository and APK.
- See `docs/validation/v056/ANDROID.md`, `docs/MARCH_SCALE_V056.md`, `data/map/reference-v056/` and the delivery artifact. This is a working installed native-map/art checkpoint, not certification of perfect original-game geographic or artistic fidelity.

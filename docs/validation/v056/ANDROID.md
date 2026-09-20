# v056 installed Android validation

## First successful native200 build

Source `d352f48ca3e1a5e15624ef8c2e6959b6327edaaf`; run https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35509322568 ; artifact 10603939370. The entire delivery job passed, not just compilation.

Actual commands: `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`; install both APKs on API29 x86_64 Pixel2 emulator; execute `GameSmokeRunner -e native56 true`. Native56Probe passed 92 assertions and produced 29 actual device-window screenshots. It used shipped coalition-190 and heroes-250 scenarios. The attack setup relocates a legitimately deployed army to the real Luoyang exterior; no terrain, site, allegiance or fake city assets were substituted.

Verified: native200/revision56, 87 sites, real decoded city atlas; FAR national view; NEAR/MID Luoyang, Changan, Chengdu, Xiangyang, Jianye, northern/southern cities, gate and port; touch all seven city cells; one-cell gate hit testing; minimap jump and pan; real sortie wizard; field army on city; cross-city MOVE without garrison; explicit GARRISON from outer cell; city edge attack; Next Turn; actual auto.sg11 file read/write and Activity recreation.

## Measured performance — emulator, not a phone guarantee

| Measurement | Value |
|---|---:|
| City atlas decoded memory | 13,475,840 bytes |
| Overview cache | 16,364,856 bytes |
| Pan frame interval p50 / p95 | 50.00 / 72.23 ms |
| Zoom frame interval p50 / p95 | 50.01 / 66.69 ms |
| Pan / zoom CPU onDraw p95 | 4.61 / 4.98 ms |

The 50-frame Choreographer samples include emulator/display scheduling. These values do not establish 60 FPS or physical ARM-device performance. Inspect `android/v056-checks.txt` for each repeat run's measurements rather than reusing this first run's values.

## APK identity

First successful artifact: versionName 0.56.0, versionCode 56, 22,330,815 bytes, SHA256 `0e1359e49d1c41804c0565772eb3e2840934476563422f95c89c72d9e55d4c05`. APK v2 signature verified; certificate SHA256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24` (repository's development-only certificate). Exact native map and all 30 PNG resources verified inside the APK; MAP_SAN11 reference excluded.

A later documentation/region/fixture commit requires its own build. Delivery artifacts carry `BUILD_COMMIT`, `APK_IDENTITY.json`, `APK_INFO.txt`, `SIGNATURE.txt`, `SHA256SUMS`, `VALIDATION_STATUS.json`, source archive and Android screenshots/logcat. Those machine-generated records identify the final APK; do not rename the initial APK to impersonate a later commit.

## Remaining acceptance work

Terrain classification noise and 39 estimated site anchors require continued manual reference review. The city art is an original procedural historical-strategy rendering, not proof of exact original-game artistic fidelity. Full historical regression/upgrade workflows still need reconciliation with deliberate v31 old-save rejection. Main is unchanged and PR #44 remains draft until these gaps are resolved.

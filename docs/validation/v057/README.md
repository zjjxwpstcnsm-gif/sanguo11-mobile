# v057 targeted delivery / diagnosis

## Evidence discipline
The delivery workflow builds unmodified main 7e3d88e5d91f3e398d843d6f8f44c7cab23ea1b5 as a baseline APK, then the checked-in final v057 source as a separate APK. The same test APK injects real window pointer DOWN/UP events through MapView. Candidate source(77,76) is an actual ROAD in both coalition-190 and heroes-250, not a claimed user-tapped coordinate. The baseline test is expected to terminate in AndroidRuntime; only a captured FATAL with ArrayIndexOutOfBoundsException and MainActivity.showTerrain certifies reproduction. A successful build alone is not installed verification.

Inspect artifact `VALIDATION_STATUS.json`, `baseline-logcat.txt`, `v057-logcat.txt`, `v057-instrumentation.txt`, `native56-instrumentation.txt`, `android/*`, `BUILD_COMMIT`, `APK_IDENTITY.json`, `SIGNATURE.txt`. A failed lane remains failed and still uploads the APK/evidence. Logs are not imported from the earlier v056 validation.

## ROAD crash
MainActivity.showTerrain formerly indexed a 13-element name array with ROAD.ordinal()==13. TerrainPresentation uses an exhaustive enum switch; its shared name/description serves both actual detail and VisualGuide. TerrainCode independently defines the stable source alphabet. No enum reordering, try/catch masking or ROAD-to-PLAIN rule substitution. Per-tap logs are opt-in (`adb shell setprop log.tag.MapTap57 DEBUG`), including scenario/map/revision, national/local/axial, screen/scale/LOD, actual callback, and structure id/kind/owner; nothing logs every frame.

## Wall-like appearance is not proof of a structure
Authoritative source alphabet is PFMWDSBXOVZHAR. D means MOUNTAIN_PATH, **not DAM**; H means DAM. The grid has 7315 R, 161 D, 167 B and **zero H**. The official national openings therefore generate **zero dams and zero walls**. No atlas index mismatch was found in the reviewed terrain-vs-military mappings.

The old connection renderer shared a raised/shadowed strip for roads and mountain trails. ROAD now has a broad worn surface without an offset shadow or wall rim; mountain paths and elevated plank decks are separate styles. Both near view and overview use TerrainArt.ground. A genuine DAM terrain uses bank/shallows ground and its one Structure owns the destructible physical body. This avoids double physical models without deleting durability/flood behavior. NaturalStructures.seedOpening is idempotent and opening-only. Save decode does not call it, so damaged/destroyed facilities retain state.

Installed neutral DAM / bare DAM / EARTH_WALL / STONE_WALL coverage uses four explicitly synthetic fixture cells on an otherwise real scenario, because no genuine H source exists. Fixtures live only in testFixtures/androidTest and are absent from the game APK. This validates safe neutral owner=-1 details and save restoration; it does not claim those objects were the user's actual offending cells.

## VOID data vs padding
The 200x200 native source contains 2421 VOID before correction: 1455 cells in boundary-connected components and 966 cells in wholly interior components. Neither category proves geographic correctness. Internal axial storage is 299x200, with **19800 additional padding cells**, not part of the 40000 source cells. Production inside/draw/hit deliberately exclude real VOID as well as padding, explaining why source holes are invisible and untappable. Exhaustive shared-coordinate projection/culling tests found no separate source/axial mismatch to patch; no arbitrary conversion or contour changes were made.

Exactly 11 isolated source VOID cells whose six odd-q neighbors are all identical were changed: V->A at (26,1),(21,3),(30,5),(28,6),(22,9),(30,9),(10,11),(22,11),(20,16); V->M at (47,155),(43,158). All are **ESTIMATED**, not confirmed original terrain. Counts after correction: V2410, A1168, M9290; ROAD7315 / MOUNTAIN_PATH161 / PLANK_ROAD167 / DAM0 unchanged. Still unresolved: **955 interior VOID cells**, plus fidelity of the legal contour and existing digitization. No city/plot/site coordinate changed, and no river or mountain corridor was cut for convenience.

`tools/content/audit_native57.py --check` verifies the exact resource hash, all corrected cells and homogeneous neighbors, actual parser alphabet, component report and revised map manifest. `--apply` accepts only the recorded exact preimage; it cannot overwrite newer manual data. Do not rerun the old materialize_native56.py.

## Save policy and retained paths
SaveCodec remains31. Native revision56 saves load their own original terrain, cities, armies and facilities; a visible notice explains that a new game is required to use the 11 map corrections. UI/crash/render fixes still apply to old saves. No automatic terrain transplant, save deletion, city/army relocation or silent upgrade occurs. Revision57 new games use the revised shared source; all seven national and both local crop scenarios remain connected.

## Running targeted checks
- `bash scripts/test-map57.sh`: all14 definitions/codes, all valid source details, both national/crops, source/screen roundtrip and conservative culling, road masks/styles, neutral DAM/wall save state, legacy56 exact roundtrip, native200 / 87 sites / 591 plots / city atlas / seven-cell movement/garrison/combat/AI.
- Baseline installed: `adb shell am instrument -w -e repro57 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner` (expected process crash; run on old APK only).
- Fixed installed: `adb shell am instrument -w -e mapTap57 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner`.
- Preserved installed gameplay: `adb shell am instrument -w -e native56 true game.sanguo.mobile.dev.test/game.sanguo.mobile.GameSmokeRunner`.

API29 x86_64 emulator evidence is not a physical ARM test and does not establish 60FPS. Full historical suites are deliberately not a prerequisite of this emergency lane; no full-suite green claim is made.

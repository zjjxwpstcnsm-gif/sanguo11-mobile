# v0.58 reference terrain and projection validation

## Source and reference

Parent main: `29c18ec127b7baeafaf9faf9f3b03ee5c1cd2a78` (PR45). Original JPG was actually extracted from user-supplied MAP_SAN11(1).rar and inspected in local crops. 7200 x 6752, SHA256 `a5a4e8c7f9785b6fbadc8487508ff6c937dcef2d782b099f2e876e48d167d4e0`. No reference image/crop is committed or packaged. `calibration-review.json` records the inherited affine model, local review scope, anchor residuals and uncertainty. In-sample anchor residuals are NOT an independent accuracy certificate; Puyang is an inherited 11.59px outlier. NW lettering obscures the nine prior sand estimates and they have NOT been upgraded to visible evidence.

## Actual changes (one national map, not seven copies)

89 unique national source cells: 72 CONFIRMED_VISIBLE, 17 ESTIMATED. 67 V -> Q; two prior v057 M estimates -> Q; nine B -> P; eight R -> M; three R -> F. Regions: Chenliu north/south/eastern ground, Wu closed water basin, Yongan/Wuling channels, Ye ground.

`Q=NON_NAVIGABLE_WATER` is appended after the original 14 terrain ordinals. Reviewed narrow channels / enclosed water were black unselectable holes. Turning them into normal W would introduce unsubstantiated ship routes. Q shows the existing WATER ground art and a specific clickable detail while remaining impassable to ALL armies/ships, undevelopable and excluded from shipyard/port shortcuts. Q is NOT a transparency workaround: it has an explicit geographic identity and closed movement rules. Two former estimated mountains are corrected to water without opening a route. Nine Ye plank tiles have visible non-deck ground, but plain-vs-other-walkable subtype remains ESTIMATED. No D mountain paths or H dams are bulk deleted/added.

Remaining source VOID: **2343**, including **888 internal** and **1455 boundary-connected** cells. Boundary connectivity alone does not certify legality; all remaining source VOID stays UNRESOLVED pending local review. Axial padding is independently **19800**. The remaining nine earlier NW sand estimates are an additional unresolved review scope, not counted as remaining VOID. No changes to 42 city / 10 gate / 35 port anchors, seven-cell footprints, or 591 development plots. New-game natural dams remain zero.

## Independent display / click root cause

Old minimap painted unshifted source pixels, used separately rounded coordinates for site markers, and a different transform for unit/viewport markers. Odd-q half-row phase and crop parity were not represented consistently. `MapRaster` now supplies a half-tile raster in the same projected plane as TileGeometry/MapOverview. Terrain, seven-cell city footprints, city/unit markers, viewport polygon and navigator inverse share this transform. Padding/VOID taps cannot navigate or select. Main MapView rejects a VOID/padding inverse hit before far-city-label fallback. No main terrain-cache hole was reproduced; visible sampled holes were source V, not a fabricated cache issue. v057 flat ROAD and ordinal crash fixes are retained, not counted as new work.

## Data / save boundaries

Authoritative path still `maps/national-map-v056.properties`; revision58, SHA256 `327317cc28f662c93308a7c87d086842d4a4af98bc2105cf7fbc365f60cf9589`. CityAtlas revision remains56 and all 50 map/art resource pins remain checked.

SaveCodec remains31 because field structure is unchanged; the appended terrain value is validated explicitly, and rejected under an old native map identity. **New terrain requires a new game.** v056/v057 saves keep their actual original terrain, cities and units and native200 movement budget; a notice explains the new-game requirement. The application does not silently replace map data or rewrite only the revision. v058 saves containing Q require v058 or newer; do not downgrade them to v057. No original files are deleted.

## Reproducible checks

`python3 tools/content/audit_native58.py --check --report docs/validation/v058/source-grid-audit.json`

`python3 tools/content/audit_native58.py --apply` accepts only the exact recorded before digest or exact after digest, validates every before value, and is idempotent. Corrupt/foreign baselines error. `test_audit58.py` covers these guards. Historical v057 audits run against a checked, in-memory inverse exported to a temporary file, never by overwriting the new map or replaying the old materializer.

`bash scripts/test-map58.sh` retains native56, city55, map57 and geometry assertions, adds all-nine-scenario / appended-terrain / source-padding / all-unit Q blockage / saved-object tests and shared-raster exhaustive inverse tests. Test-only TSV is checked against the authoritative review ledger. To test real baseline saves, run `scripts/canonical-legacy58.sh` then set REFERENCE58_LEGACY57 and REFERENCE58_BASELINE_CANONICAL. Android-origin bytes re-encode differently on the JVM even with unchanged v057 code; compare the independently compiled unchanged v057 encoder and new v058 encoder on the same JVM. The two encoders produced identical state bytes locally; original files are unchanged.

## Actual Android evidence and scope

Fresh unchanged v057 baseline sampling was performed on API29 x86_64, two scenarios, actual window pointer events and real screenshots: Actions run 35515083807, source29c18ec. Instrumentation passed and logcat had no fatal crash, but the workflow final shell assertion was incorrectly quoted and exited127; this is not a successful overall workflow. The v058 delivery fixes that workflow line. No old ROAD-crash reproduction is rerun.

At source submission: local directed suites passed; v058 installed validation is IN PROGRESS, not yet claimed passed. `scripts/android-map58.sh` installs the exact APK hashed for delivery, exercises fresh national and cropped maps through actual touches, paired coordinates/LOD, non-navigable water, preserved roads/path/planks, edges, minimap parity, true pan/pinch, territory states, actual raw v057 saves and explicitly labelled test-only wall/dam/march fixtures. Separate retained Native56Probe covers real sortie wizard, seven-cell transit, next turn, explicit entry, edge combat, minimap and actual save/read. The final Actions artifact supplies the actual status, source, signature, hashes, screenshots and logs. Physical ARM devices and exhaustive historical CI are not certified.

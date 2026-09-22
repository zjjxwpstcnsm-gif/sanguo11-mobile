> Historical checkpoint. Superseded for current S04/S05 implementation by
> [s04-s05-runtime.md](s04-s05-runtime.md); retain this file as the earlier record.

# S05 — unit playback contract repair (PARTIAL / art prerequisite BLOCKED)

2026-09-22. Input branch `de8ef122a28b8f3ab2800b24ac46bc7636501769`;
main remains `1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`.
Continue `agent/3d-s01-renderer-foundation`, PR #62; do not merge main.

## Predecessor and scope

S02/S03 source and assets are now present. S04 facility art, vegetation and installed
acceptance remain incomplete, as recorded in s04-preflight.md and PR #62. The task
archive says to repair a broken predecessor before layering further art. This delivery
repairs the existing live unit playback contract, not a completed S05 art stage.
The actual entry is a normal game/save → 视图 → 3D 试验模式; default remains 2D.

## Production changes

- Detached UnitVisual captures core ID, owner, commander, actual Weapon/Ship enums,
  mission/transport, naval mode, troops, wounded, energy, food, gold, status and burning.
  Labels show commander, equipment, strength, energy and abnormal states.
- Naval classification comes from Army.water, not TerrainSurface.water: decorative
  shallows/non-navigable water must not accidentally turn troops into naval units.
- Each renderer object owns its own UnitMotion. Only the active journal actor changes
  its interpolated pose; no actorCopy or mesh creation per frame. Offscreen actor poses
  remain available for culling but skip GPU transform writes.
- Recorded path segments use the existing staggered GridWorldTransform and terrain
  mesh height. Heading follows each segment. Legacy non-neighbor endpoint-only
  displacements snap at the endpoint instead of inventing an unrecorded shortcut.
- Clearing/replacing playback settles the previous actor to its snapshot; new snapshots
  settle surviving objects to the new authoritative cell. Removed objects use existing
  renderable-before-buffer destruction; release clears the event and animated reference.
- Model hit testing resolves the moving instance to its snapshot hex/stable unit ID.
  Labels, ground rings and culling follow the presentation pose. Existing tap/details
  behavior is preserved through the same TileListener. Installed touch tests are pending.
- TurnJournal previously omitted wounded/woundRemainder in copy and change detection.
  Both now survive playback snapshots. No combat formula, movement cost, map, RNG or
  command execution changed. Playback still never executes a command.

## Actual model / animation coverage

All rows currently use the existing S01 unit silhouette. Keys below are state keys, NOT
claims of delivered model assets. No new model, UV, texture, skeleton or animation clip
is supplied in this checkpoint. No image generation or modeling service was used.

| Actual core type | State model key | Required art status |
|---|---|---|
| 枪 / SPEAR | weapon/SPEAR | BLOCKED |
| 戟 / HALBERD | weapon/HALBERD | BLOCKED |
| 弩 / CROSSBOW | weapon/CROSSBOW | BLOCKED |
| 骑 / CAVALRY | weapon/CAVALRY | BLOCKED, horse locomotion absent |
| 剑 / SWORD | weapon/SWORD | BLOCKED |
| 冲车 / RAM | weapon/RAM | BLOCKED |
| 井阑 / SIEGE_TOWER | weapon/SIEGE_TOWER | BLOCKED |
| 木兽 / WOODEN_BEAST | weapon/WOODEN_BEAST | BLOCKED |
| 投石 / CATAPULT | weapon/CATAPULT | BLOCKED |
| 运输/任务队 | transport | BLOCKED |
| 走舸 / BOAT | ship/BOAT | BLOCKED |
| 楼船 / TOWER_SHIP | ship/TOWER_SHIP | BLOCKED |
| 斗舰 / WARSHIP | ship/WARSHIP | BLOCKED |

Required idle, walk/run, attack preparation/attack, hit, defeat clips and formation LOD
are NOT implemented. Pose yaw/path interpolation is not skeletal animation. Model-key
classification is not a delivered port conversion animation. Existing reusable GPU
silhouettes are not GPU instancing or an object pool.

## Verification

- `bash scripts/test-3d-foundation.sh`: foundation 3,116,362; facility 133;
  terrain 902,230; sites 1,645; S05 unit 18,426 assertions, plus wounded journal regression.
- S05 covers bent paths in both projections, endpoint clamping, NaN input, clear/settle,
  independent instance state, all weapon/ship enums, all terrain naval classifications,
  detached resources, real national-scenario deployment/move and command-result equality.
- 50/100 simultaneous pose arrays are explicit synthetic CPU correctness stress only;
  they are not rendered stress scenes and provide no FPS claim.
- APK, Android test APK and lintDebug build succeeded using cached Gradle 8.11.1,
  JDK 21 (Java 17 target), Android SDK 35, Filament 1.56.0. Lint: 0 errors, 73 warnings.
- Build also runs the existing presentation, topology, source-grid and siege regressions.
- PortReplayTest fails `AI completes embark, sail and land false [7,6, 10,5]`.
  Replacing the only changed core class with the original input TurnJournal reproduces
  the exact failure. This is a retained baseline failure, NOT a passing port lifecycle test.
- No ADB device attached. Installed 2D/3D, embark/disembark/transport/garrison,
  auto-march arrival actions, moving-touch behavior, resource lifetime, video and actual
  50/100 rendered-unit stress remain UNTESTED.
- CPU/GPU frame percentiles, PSS, cold start, memory and 30 FPS gate: N/A (not measured).
  Final committed APK identity/bytes/hash are attached to PR #62; no false performance data.

## Continue

Finish and verify S04 facility/vegetation production assets; then use these detached
unit states and common TurnPlayback to implement real shared modular/skeletal assets,
per-instance clip state, formations/LOD, legal-node model transitions and lifecycle effects.
Keep this PR open/draft and preserve all existing S02/S03 assets and rules. Do not treat
this checkpoint as permission to skip S05's art, installed, recordings or performance gates.

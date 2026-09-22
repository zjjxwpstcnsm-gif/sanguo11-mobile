# S04/S05 runtime delivery and acceptance

This report supersedes the S04 and S05 preflight asset blockers. Main baseline:
`1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`; branch
`agent/3d-s01-renderer-foundation`, PR #62, no merge. Entry: load normal game/save →
视图 → 3D 试验模式. Version 0.72.0-3d-s04-s05, development signature unchanged.

## Facility matrix

All resources live in `app/src/main/assets/3d/field`; each row has LOD 0/1/2.
Every row is parsed by the actual runtime loader and checked for finite UV/geometry,
indices, level selection and checksums. `SceneFacilityStateTest` checks detached live
construction/HP/fire/owner/direction state. Native lifecycle uses real CAMP and FARM
commands; this is not a claim that every one of the 40 combinations was manually played.

| Core type / level | Asset stem | Triangles LOD 0/1/2 | State / verification |
|---|---|---|---|
| domestic-MARKET-1 | `domestic-MARKET-1-lod[0-2].glb` | 208/64/64 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-MARKET-2 | `domestic-MARKET-2-lod[0-2].glb` | 312/96/96 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-MARKET-3 | `domestic-MARKET-3-lod[0-2].glb` | 416/128/128 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-FARM-1 | `domestic-FARM-1-lod[0-2].glb` | 346/162/88 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-FARM-2 | `domestic-FARM-2-lod[0-2].glb` | 392/172/98 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-FARM-3 | `domestic-FARM-3-lod[0-2].glb` | 438/182/108 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-BARRACKS-1 | `domestic-BARRACKS-1-lod[0-2].glb` | 158/118/44 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-BARRACKS-2 | `domestic-BARRACKS-2-lod[0-2].glb` | 310/230/82 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-BARRACKS-3 | `domestic-BARRACKS-3-lod[0-2].glb` | 380/240/92 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-SMITH-1 | `domestic-SMITH-1-lod[0-2].glb` | 210/170/96 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-SMITH-2 | `domestic-SMITH-2-lod[0-2].glb` | 362/282/134 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-SMITH-3 | `domestic-SMITH-3-lod[0-2].glb` | 432/292/144 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-MINT-1 | `domestic-MINT-1-lod[0-2].glb` | 200/160/86 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-GRANARY-1 | `domestic-GRANARY-1-lod[0-2].glb` | 80/80/80 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-STABLE-1 | `domestic-STABLE-1-lod[0-2].glb` | 228/188/114 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-STABLE-2 | `domestic-STABLE-2-lod[0-2].glb` | 380/300/152 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-STABLE-3 | `domestic-STABLE-3-lod[0-2].glb` | 450/310/162 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-BLACK_MARKET-1 | `domestic-BLACK_MARKET-1-lod[0-2].glb` | 208/64/64 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-WORKSHOP-1 | `domestic-WORKSHOP-1-lod[0-2].glb` | 394/290/216 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-SHIPYARD-1 | `domestic-SHIPYARD-1-lod[0-2].glb` | 210/170/96 | type/level, HP, construction/fire, owner; loader + state tests |
| domestic-BRONZE_TERRACE-1 | `domestic-BRONZE_TERRACE-1-lod[0-2].glb` | 228/188/114 | type/level, HP, construction/fire, owner; loader + state tests |
| military-CAMP-1 | `military-CAMP-1-lod[0-2].glb` | 456/276/276 | type/level, HP, construction/fire, owner; loader + state tests |
| military-ARROW_TOWER-1 | `military-ARROW_TOWER-1-lod[0-2].glb` | 96/96/96 | type/level, HP, construction/fire, owner; loader + state tests |
| military-MUSIC-1 | `military-MUSIC-1-lod[0-2].glb` | 136/136/136 | type/level, HP, construction/fire, owner; loader + state tests |
| military-FIRE_SEED-1 | `military-FIRE_SEED-1-lod[0-2].glb` | 34/34/34 | type/level, HP, construction/fire, owner; loader + state tests |
| military-FORT-1 | `military-FORT-1-lod[0-2].glb` | 468/288/288 | type/level, HP, construction/fire, owner; loader + state tests |
| military-FORTRESS-1 | `military-FORTRESS-1-lod[0-2].glb` | 410/180/106 | type/level, HP, construction/fire, owner; loader + state tests |
| military-CROSSBOW_TOWER-1 | `military-CROSSBOW_TOWER-1-lod[0-2].glb` | 106/106/106 | type/level, HP, construction/fire, owner; loader + state tests |
| military-CATAPULT_TOWER-1 | `military-CATAPULT_TOWER-1-lod[0-2].glb` | 116/116/116 | type/level, HP, construction/fire, owner; loader + state tests |
| military-DRUM-1 | `military-DRUM-1-lod[0-2].glb` | 58/58/58 | type/level, HP, construction/fire, owner; loader + state tests |
| military-STONE_MAZE-1 | `military-STONE_MAZE-1-lod[0-2].glb` | 120/120/120 | type/level, HP, construction/fire, owner; loader + state tests |
| military-EARTH_WALL-1 | `military-EARTH_WALL-1-lod[0-2].glb` | 12/12/12 | type/level, HP, construction/fire, owner; loader + state tests |
| military-STONE_WALL-1 | `military-STONE_WALL-1-lod[0-2].glb` | 72/72/72 | type/level, HP, construction/fire, owner; loader + state tests |
| military-FIRE_BALL-1 | `military-FIRE_BALL-1-lod[0-2].glb` | 94/94/94 | type/level, HP, construction/fire, owner; loader + state tests |
| military-FLAME_SEED-1 | `military-FLAME_SEED-1-lod[0-2].glb` | 68/68/68 | type/level, HP, construction/fire, owner; loader + state tests |
| military-FLAME_BALL-1 | `military-FLAME_BALL-1-lod[0-2].glb` | 104/104/104 | type/level, HP, construction/fire, owner; loader + state tests |
| military-INFERNO_SEED-1 | `military-INFERNO_SEED-1-lod[0-2].glb` | 102/102/102 | type/level, HP, construction/fire, owner; loader + state tests |
| military-INFERNO_BALL-1 | `military-INFERNO_BALL-1-lod[0-2].glb` | 114/114/114 | type/level, HP, construction/fire, owner; loader + state tests |
| military-FIRE_SHIP-1 | `military-FIRE_SHIP-1-lod[0-2].glb` | 76/76/76 | type/level, HP, construction/fire, owner; loader + state tests |
| military-DAM-1 | `military-DAM-1-lod[0-2].glb` | 42/42/42 | type/level, HP, construction/fire, owner; loader + state tests |

## Unit matrix

Each row has near and reduced GLB rest meshes and entries in `rigs.json`. Shared
`clips.json` supplies actual 12-frame local rotations. Infantry has moving limbs,
horses moving legs, four independent wheel axles, siege mechanisms and ship oars.
The renderer uses rigid module articulation, not a skinned glTF skeleton. Models use
original low-poly geometry and a shared textured atlas; this is strategic-distance art,
not high-resolution character work. Offline contact sheets are labeled accordingly.

| Actual core type | Resource | Formation near/mid/far | Verified animation |
|---|---|---|---|
| SPEAR | `unit-SPEAR-lod[0-1].glb` | 8/4/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| HALBERD | `unit-HALBERD-lod[0-1].glb` | 8/4/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| CROSSBOW | `unit-CROSSBOW-lod[0-1].glb` | 8/4/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| CAVALRY | `unit-CAVALRY-lod[0-1].glb` | 4/2/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| SWORD | `unit-SWORD-lod[0-1].glb` | 8/4/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| RAM | `unit-RAM-lod[0-1].glb` | 1/1/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| SIEGE_TOWER | `unit-SIEGE_TOWER-lod[0-1].glb` | 1/1/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| WOODEN_BEAST | `unit-WOODEN_BEAST-lod[0-1].glb` | 1/1/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| CATAPULT | `unit-CATAPULT-lod[0-1].glb` | 1/1/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| transport | `unit-transport-lod[0-1].glb` | 1/1/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| BOAT | `unit-BOAT-lod[0-1].glb` | 1/1/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| TOWER_SHIP | `unit-TOWER_SHIP-lod[0-1].glb` | 1/1/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |
| WARSHIP | `unit-WARSHIP-lod[0-1].glb` | 1/1/1 | idle, walk, turn, prepare, attack, hit, defeat, enter |

## Verification and limits

- `JSON_TEST_JAR=... bash scripts/test-3d-field.sh`: 5,957,590 asset assertions;
  all 120 facility GLBs and 26 rigs; animated vertex movement and finite ranges.
- Bounded forest dirty test preserves 16/25 chunks for the tested edit and dependency
  halo. Exclusion, opaque two-LOD trees and terrain-only far distance are checked.
- Real legal port crossing switches land → ship → land and keeps full SaveCodec equality.
- Real build → complete → technology upgrade → enemy attack → repair → destruction,
  plus actual domestic build/finish/demolish, preserve exact rules versus reference.
- Actual deployment, convoy dispatch and automatic march/garrison complete in eight
  next-turn calls. Every turn matches full serialized reference state. Sampled normal,
  accelerated and skipped animation never executes core commands.
- Fixed a presentation-only journal deduplication bug: MOVE and ENTER at the same
  actor/target must remain distinct events. No gameplay formula changed.
- `FieldSceneInstrumentation`: synthetic 50/100-unit, dense forest and mixed facility
  scenes; spans 7/15/28; surface PixelCopy; actual facility lifecycle; port replay;
  automatic garrison and convoy. Fixtures do not change or trim official scenarios.
- Existing installed SceneInstrumentation retains normal national scenario, native
  Surface pixel assertion, ten renderer switches, ten HOME/foreground cycles,
  seven-cell picks, real deployment/movement/turn and autosave reload.
- APK/test APK/lint and host regressions are required independently of installed tests.
  CI result and reviewed screenshot evidence are recorded in the final handoff.

No physical arm64/midrange device is attached. Device FPS, GPU frame percentiles,
power, installed data and GPU memory remain **UNMEASURED**. Emulator rendered submission
counts are not displayed FPS; callback timing is not GPU timing. Original opaque atlas
is 512×64 RGBA (128 KiB texel estimate); buffers and driver memory are separate.
Do not claim full performance acceptance or enable 3D by default from these results.
Next stages must retain shared core playback, stable IDs, default 2D and safe fallback.


## Existing core regression comparison

Only `core/.../TurnJournal.java` differs from the main gameplay source. Compiling the
main version of that class into an isolated comparison classpath reproduces exactly
the same output for CoreTest, PortReplayTest, CampaignAiTest, SandTerrainTest,
ObjectiveOrdersTest and MarchOrdersTest failures (AI deployment/supply and older
map/fixture expectations). No baseline gameplay code was changed to silence them.
FacilityProductionTest (73) and Turn48Test (120) pass in both comparisons. This does
not turn the failing legacy suite into a passing suite; the failures remain tracked.

Native resource reporting labels scene primitives, indexed triangles, known uploaded
buffer bytes and a bounded CPU submission-time P50/P95/P99 sample explicitly.
These are not driver Draw Calls, present-frame FPS or GPU memory/frame-time measures.
National terrain shading reuses half-cell height samples, avoiding redundant 63-cell
filtering for interior triangle colors. Height/geometry and core coordinates are unchanged.

The same height filter now reads precomputed immutable per-cell target/constraint arrays
and primitive projected coordinates; this removes per-neighbor Hex and enum-array
allocations. Terrain tests retain the same maximum height, topology and ray-pick results.

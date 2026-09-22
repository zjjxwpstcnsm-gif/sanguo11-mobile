# S05 — unit playback contract checkpoint

Status: **PARTIAL / S04 art prerequisite BLOCKED**. Input `de8ef122a28b8f3ab2800b24ac46bc7636501769`.
Continue PR #62 on `agent/3d-s01-renderer-foundation`; main not merged.
S02/S03 have been recovered and pushed; the historical remote-blocked wording below is superseded.
Production changes: detached real unit/equipment state; per-instance path/yaw poses;
moving labels/rings/culling/picking; clear/skip pose settling; wounded journal copies.
No final unit models, skeletal animation, formations or water-transition art is claimed.
See [S05 status, coverage and verification](acceptance/s05-status.md).
APK and Android test APK build; lint 0 errors / 73 warnings. No device attached.
PortReplayTest's AI dock failure reproduces with the original input core class.
Next: finish S04 art and installed gates before claiming S05 art/animation completion.

---

# S02 — continuous terrain checkpoint

Status: **PARTIAL**, remote delivery **BLOCKED**. Continue existing PR #62 branch by explicit user request; main is not merged.
Production entry remains game → 视图 → 3D 试验模式. Version 0.69.0-3d-s02 / 69.
See [S02 acceptance](acceptance/s02-status.md) and [CPU checks](acceptance/s02-cpu.txt).
Terrain, water mask, site bases, height-aware picking/overlays, seam-safe two-level meshes
and local chunk invalidation are implemented. Core and national map data are untouched.
No S02 on-device or performance acceptance is claimed. S03 must retain this branch and
resolve S02 remote/device/visual acceptance before treating the stage as complete.

---

# S01 — renderer foundation

- Input main: `1a883a4ffd1098ca85f7da38464b7c5fbfdc000f` (merged prior PR #61 as requested).
- Branch: `agent/3d-s01-renderer-foundation`; PR #62.
- Status: S01 production implementation present; physical-device/performance/manual touch acceptance remains PARTIAL. Latest installed check and artifact results are recorded in PR #62.
- Production entry: load/new game → 视图 → 2D / 3D 试验模式.
- Version: 0.68.0-3d-s01 / 68; existing package and development signing key retained.

Implemented: production host; exact shared grid conversion; real immutable terrain/object
snapshots; cache reuse across identical turn-world clones; async cancellable chunk geometry;
visible chunk upload/residency; stable dynamic IDs; six explicit transition silhouettes;
Surface/native ownership; camera pan/pinch/focus/picking; selection/move/target/siege/facility
range overlays; common command callbacks and TurnPlayback; default 2D and recovery marker;
optional diagnostics; offline material and source/asset/toolchain documentation.

Local evidence: SceneFoundationTest 545,728 assertions, 38,949 valid cells / 177 chunks /
87 initial objects on the first real national scenario. All valid cell centers at both
projection conventions and three elevations roundtrip, seven-cell cities resolve correctly,
mesh indices/counts match, snapshots do not mutate World, and deployment/move/next-turn/save
results match with/without presentation. Existing presentation/terrain/coordinate/siege
regressions pass. APK + Android test APK + lint build passed on the preceding checkpoint.

Installed checkpoint 11989388: real native 3D engine and ten 2D/3D cycles passed, but the
runner then failed on a transient `queued` flag sampled off the render thread. That flag is
false during doFrame itself, so it was not a valid recovery assertion. The revised check
requires a new callback timestamp after pause reset, adds actual HOME/task-foreground cycles,
and exercises production command/turn/save paths. A new run must pass before claiming it.

Installed checkpoint 1d3504b5 completed the ten actual HOME/foreground cycles and reached
command/next-turn verification. Its API29 system image then clamped the Java heap at 16 MiB
and failed SaveCodec decoding with OOM. The verification AVD now explicitly uses 4 GiB RAM,
256 MiB Java growth limit / 512 MiB maximum, records those properties and Runtime.maxMemory,
and restarts zygote before installing. This changes only the test device, not app heap flags.
First-frame screenshots also wait for actual GPU output; native PixelCopy confirms real
terrain pixels. Loading status reports pending terrain uploads; native pixel readiness is checked separately
by PixelCopy, rather than assuming optional driver completion callbacks are supported.
Checkpoint d28a1349 passed all 93 installed checks; the subsequent screenshot review corrected
status/diagnostic text overlap and waits for all national chunks before the overview capture.

Remaining gates: final installed rerun (see PR), wizard touch-through manual acceptance, physical
midrange/arm64 device frame-time percentiles, PSS/native/GPU memory, long-run leaks, battery,
cold-start and installed-data measurements. Do not infer these from emulator timing or
Choreographer callback intervals. 3D art remains S01 transition quality. 2D-only overlays
and editors remain available through compatibility fallback. S02 reuses this foundation;
it must not declare the pending hardware/performance gates complete.

# S04 request — prerequisite BLOCKED; facility state repair delivered

- Input branch SHA: `df1c1f94210bd558b1be9abcf8ff564ce4783dc2`; main remains
  `1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`. Same branch and PR #62, no merge.
- Remote preflight found S01 only: S02 terrain and S03 site assets/model contract are absent.
  Do not treat the user's earlier stage requests as delivered code. S04 remains **PARTIAL/BLOCKED**.
- Production repair: detached facility type/level/construction/upgrade/HP/fire/owner/direction
  state; visible status labels and domestic ownership color; unreferenced GPU mesh reclamation;
  reposition surviving objects when the ground projection changes. Existing shared core coverage remains.
- Coverage matrix and exact blockers: `acceptance/s04-preflight.md` (11 domestic + 19 military types).
  All final facility art remains explicitly blocked, not passed off as placeholder coverage.
- Local verification: existing S01 545,728 assertions; new facility-state 133 assertions;
  UI 49,018, terrain connections 451, tile geometry 1,136,811, coordinate checkpoint 1,168,801,
  map coordinate cumulative 898,305, siege rules 11,371 and overlay 14 all pass.
- Offline Gradle APK, Android test APK and lintDebug passed using installed JDK21 with Java17
  source/target and the existing Android35 SDK/Filament1.56.0. Final APK source identity and
  checksum are recorded beside the exported artifact and in PR #62, not in a self-referential commit.
- No attached device or local emulator/KVM: installed state transitions, native resource lifetime,
  screenshots, CPU/GPU/frame percentiles, PSS, texture memory and density stress tests remain unmeasured.
- No new final models, forest placement, vegetation LOD/instancing or dirty-region updates are claimed.
  Remain on this branch; resume S04 only after actual S02/S03 delivery and preflight verification.

## S03 — PARTIAL, remote delivery BLOCKED

- User override: continue existing PR #62 branch, never merge main.
- Remote starting head df1c1f94210bd558b1be9abcf8ff564ce4783dc2; main remains
  1a883a4ffd1098ca85f7da38464b7c5fbfdc000f. Local committed S02 prerequisite
  5c8c8239f067 was fast-forwarded from the prior clean workspace; no uncommitted work taken.
- S03 normal production entry now loads textured runtime GLBs: 3 city layouts, port, gate,
  3 LODs, exact seven-cell foundation, 87 national mappings, custom default, navigable-water
  port orientation, faction flags, damage, incremental ownership/removal, reverse view.
- Version 0.70.0-3d-s03 / 70; same package/development signing key.
- Test/build evidence and open gates: acceptance/s03-status.md. Asset contract and counts:
  site-assets.json; tools/3d/build_sites.py is the complete reproducible source.
- Git push failed with missing HTTPS credentials; GitHub app failed HTTP 400 Invalid MCP
  request metadata. Local commits/APK are NOT the remote PR head. Do not claim remote delivery.
- S02 water animation/quality toggle/shore quality and device gates remain unpassed. S03
  physical-device/installed visual review cannot be waived based on compilation. Do not
  declare S03 fully accepted or promote 3D as default. S04 can inspect code but retains gates.

## Local S02/S03 recovery and integration

The earlier S04 preflight inspected only its checkout and remote, missing other local
workspaces. On user request we found clean local S02 commits afdfd55 / 5c8c823 and S03
commits 9a50d1d / 606b4f1, integrated them with the remote S04 facility-state fixes, and
resolved snapshot/renderer/test/progress conflicts. This supersedes the earlier assertion
that prerequisite implementation was unavailable: code and runtime assets are now present.
Previous installed-art/performance acceptance gaps still apply; S04 art is not completed.
Same PR #62 branch; main is not merged or modified. Remote delivery identity is in PR #62.

Integrated verification: foundation 3,116,362, terrain 902,230, site semantics 1,645,
facility state 133 and actual GLB loader 38,032 checks passed. All 15 GLBs loaded.
Android APK/test APK/lint build and existing presentation regressions passed locally.
No installed visual/performance acceptance is inferred from these checks.


## S04/S05 runtime implementation recovery (2026-09-22)

The remaining facility/vegetation/unit implementation is now present on the same PR
#62 branch. This supersedes earlier claims that these runtime assets were absent.
150 original textured GLBs cover 40 facility type/level combinations at three LODs,
13 unit/vehicle/ship types at two mesh LODs, trees and state modules. Articulation uses
shared rigid-joint keyframes, independent instance phases and 8/4/1 formations.
Opaque vegetation merges in 8x8 chunks with local reuse and road/site exclusions.
See `acceptance/s04-s05-runtime.md` and `field-assets.json` for exact coverage and limits.

Main input remains `1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`; user explicitly overrides
archive instructions to create a new branch or merge predecessors. Keep existing branch
`agent/3d-s01-renderer-foundation`, leave main untouched. Runtime entry is the production
3D view toggle, not a demo. Version is 0.72.0-3d-s04-s05 / 72.

5,957,590 geometry/animation checks and real lifecycle/port/automatic garrison/convoy
checks pass. Native emulator tests are separate gates; final CI and artifact identities
belong to the final report/PR. Physical device performance and visual art acceptance
remain open; source/build checks never replace those gates. Do not mark all S01–S05
accepted merely because the missing S04/S05 implementation is present.

## S06 combat implementation (same PR #62)

Source entry: normal game → view → 3D. Version 0.73.0-3d-s06 / 73. Main remains
`1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`; no merge. Includes concurrent fixture
fix 1bd28d9. Shared TurnJournal/TurnWork/TurnPlayback now feed bounded native combat,
ordered actual counter/support hits, exact damage/status text, persistent core fires,
compact critical portraits and cleanup. No gameplay formulas or report APIs replaced.

Host combat/full-save equivalence and S01–S05 suites pass; APK/test APK/lint pass at
implementation checkpoints. Final source identity and exact gate output accompany the
APK. Native recording/installed checks run in CI; hardware and art acceptance remain
open. See acceptance/s06-combat.md. S07 must preserve this shared historical playback,
object-pool ownership, default 2D and all existing rule/editor contracts.

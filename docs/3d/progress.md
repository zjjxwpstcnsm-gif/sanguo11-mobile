# S01 — renderer foundation

- Input main: `1a883a4ffd1098ca85f7da38464b7c5fbfdc000f` (merged prior PR #61 as requested).
- Branch: `agent/3d-s01-renderer-foundation`; PR #62.
- Status: implementation present, installed validation in progress; physical-device acceptance pending.
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

Remaining gates: final installed rerun, wizard touch-through manual acceptance, physical
midrange/arm64 device frame-time percentiles, PSS/native/GPU memory, long-run leaks, battery,
cold-start and installed-data measurements. Do not infer these from emulator timing or
Choreographer callback intervals. 3D art remains S01 transition quality. 2D-only overlays
and editors remain available through compatibility fallback. S02 reuses this foundation;
it must not declare the pending hardware/performance gates complete.

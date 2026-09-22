# S07 interaction and editors — candidate, acceptance PARTIAL

Continue PR #62 on `agent/3d-s01-renderer-foundation`; do not merge main.
Input main `1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`, S06 head `0a4a01b`.

Implemented in production:
- Camera finite/range validation, focal zoom, two-finger pan, long-press selection,
  explicit reverse/reset controls, versioned presentation preferences and bad-type fallback.
- Actual mesh triangle selection with terrain occlusion; command targets use surface
  cells instead of expanding attack/enter rules to model bounds. Selected seven-cell
  cities retain priority labels; terrain-attached legal range overlays remain core queries.
- Tactic preview, unit labels, territory view and navigation stay in 3D. In 3D the
  navigator opens the full-map view. Existing native strategic command UI is retained.
- Scenario faction preview has a 2D/3D toggle, ruler/realm labels and advisor card.
  Its chosen renderer continues into the new game; restored UI mode respects the native
  crash marker. Multiple map hosts share a process-local native health count.
- Existing MapEditorActivity uses MapHost, including terrain brush, placement footprints,
  selection, metadata properties and undo/redo. No parallel editor implementation.
- Optional format-2 visual metadata has a version, map UUID and logical content hash.
  Heights are 0..2600 thousandths of a cell; site variants 0..2 and orientation 0..359.
  Only bundled model families are used. Strict parser retains 4 MiB, integer-only,
  duplicate/unknown field, reference and base checks. No arbitrary resource paths.
- No-visual maps still encode exactly as format 1. Visual fields do not alter the
  logical map fingerprint or SaveCodec bytes. Height changes invalidate ground/chunks;
  terrain painting drops affected overrides and undo restores them. Water/site bases
  stay flat. Site deletion removes its appearance; copying rebinds metadata identity.
- Visual metadata survives turn render/computed clones and local save reload through
  an identity-checked presentation sidecar. Legacy strategic save exports explicitly
  warn that visual metadata requires the separate map JSON. Camera is never in core save.

Host tests: 32 S07 checks and 136 existing map editor integration assertions pass.
S01-S05 host foundation and field suites pass, including terrain ray/LOD and vegetation
reuse. Optimized APK, test APK and lint pass. Exact final native/build evidence is
tracked in the delivery manifest and PR description. Height-only overrides participate
in both terrain and vegetation chunk fingerprints; a dedicated test prevents stale meshes.

Still requires installed/manual acceptance: complete wizard-only new game/deploy/attack/
turn/report/save/load flow, system document picker export/import in a separate clean
installation, every custom-officer/avatar workflow, long sessions, malformed/deleted
asset fallback, dense label art review and midrange arm64 timing/PSS/GPU/battery.
Automated tests calling real commands are not equivalent to all wizard touches.
A native S07 suite is wired into scene CI and records the actual Surface/editor.
Default remains 2D. S06's critical-fire save restriction remains outside this visual task.

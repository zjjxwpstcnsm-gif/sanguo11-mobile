# Native PC visual progress

Branch: agent/native-pc-visual. PR #67 remains draft, targets main; do not merge.
Backend locked: existing Filament 1.56.0 / OpenGL. No Unity restart.

R00: PARTIAL. Inherited HEAD bdc685679f81796cf8c19b0dd657ffcae44b7419.
Original R00 report/ledger/handoff were absent. R01 recovered CI run 35846793589:
fresh 246 and reload 149 installed checks pass. Inspected actual Surface capture
shows terrain, city, ports, vegetation and water, not a uniform background.
It also shows existing stepped shores and coarse art; PC similarity is not PASS.
Main ac29b458 includes architecture PR #66; old architecture reports saying it is
unmerged are historical. ARM64 and PC reference acceptance remain missing.

R01: PARTIAL. See reports/R01.md, handoffs/R01.md and OWNERSHIP.md.
Only R01 was requested. R02 and later remain NOT_STARTED in this execution.

Final source 8744cb00ff90077dddff849874602957c8cf0295: native build/lint PASS;
R01 emulator 1,225 checks / 20+20 cycles PASS (35855187043); normal startup
fresh 285 / reload 192 PASS (35855186990). Stage remains PARTIAL for documented
implementation, fault-matrix, physical-device and visual-reference gaps.

R02: PARTIAL. Source 2965bcddd50564f80cb194aa71127767d822dd10; latest exact-source
CI35865854066 PASS. Prior HIGH PixelCopy failure and physical/reference/manual
matrix gaps remain historical evidence. R02 report/handoff were absent at initial HEAD, then synchronized from 943ddcf.

R03: PARTIAL. TerrainSurface v3, production view-window CPU meshes, three planar
LOD levels, local revision-safe terrain halo invalidation; legacy vegetation seed retained.
See reports/R03.md and handoffs/R03.md. No R04 started; await exact-source runtime.
R02: PARTIAL. Continuous camera, actual fan ray picking, rotated overlays/culling,
height anchor and command-safe input integrated in the normal game. No new art or
rule/map edits. Source 2965bcddd50564f80cb194aa71127767d822dd10. Host suites pass;
Android build/lint passes. Final runtime/device scope and artifact identity are in
evidence/R02.json and reports/R02.md. R03 remains NOT_STARTED.

R02 final CI35865854066: build/lint and 1,043 installed checks PASS, all three
quality captures. Same-source R00 startup CI35865854274 PASS. First HIGH capture
failure retained; retries were not needed in the final passing run. Physical, PC
reference and complete hand-operated interaction/performance gates still open.

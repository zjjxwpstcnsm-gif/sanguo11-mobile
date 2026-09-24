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

R05: PARTIAL. Shared textured bank/shallow/deep water and actual interior shore/flow
sampling integrated. R05/S11/R04/R03/architecture host checks PASS. Legacy port AI
replay fails identically on R04 baseline; not relabeled PASS. Android exact-source
build/installed evidence pending; physical/PC/boat/edit acceptance open. See R05
report and handoff. Do not start R06 or merge main.

## R04 current execution

PARTIAL. R04 source2c87b4c96e33fe3dca764dada98c8b4b6fd058ec; final host/material/
APK/lint PASS, CI35943061193. Runtime cancelled by concurrent external R05 commit
 d40ea743; exact-source R03 launch also blocked by emulator ZIP failure. NOT_RUN
for final installed acceptance. Candidate b78566b R04 PASS643 with actual farm.
R04 host87683/S13 nationwide33829951 pass, original budget and rules preserved.
PC reference found; V1/complete visual acceptance FAIL, ARM64/performance NOT_RUN.
This execution did not start R05; upstream R05 progress above is preserved as external.
See reports/R04.md, handoffs/R04.md and evidence/R04.json.

R05 final: PARTIAL. Source b97e01c, CI35943948593 build/lint/runtime SUCCESS;
542 installed assertions,32 raw Surface captures,7 operation recordings. Same-source
R00 fresh183/reload110 PASS. Shared textured bank improves color contact; stepped
silhouette remains VISUAL FAIL. Synthetic boat commands/save parity PASS, but stale
UI over fixture Surface observed; full motion/editor/PC/physical gates remain open.
APK88/0.88.0-native-r05; see R05 report/evidence. PR67 draft, no merge, no R06.

R06: PARTIAL, current execution. Strict GLB/PNG/rig subset, bounded asynchronous
object decode and owner upload, shared GPU references, deterministic production
asset pipeline and gate/tree/catapult sample improvements. Host/Khronos PASS;
exact-source Android build/runtime pending. No R07 or main merge.

R07: PARTIAL. Regional city/port/gate production GLBs and facility joinery improved;
terrain-derived region and diagonal gate approach, live catalog/enum coverage.
See reports/R07.md. R06 CI35950156358 now SUCCESS; its prior pending text is historical.
R07 exact-source build/runtime pending; do not merge main or begin R08.

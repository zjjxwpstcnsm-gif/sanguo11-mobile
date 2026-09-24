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

R08: PARTIAL. Stable world-space clustered tree/shrub/rock scatter, view-window CPU
residency, conforming logical roads and farm meshes on normal Filament path.
R07 final CI35961885126 SUCCESS. R08 exact-source CI outcomes pending; see R08 report.
No main merge or R09 execution. Art/PC/ARM64/manual gates remain separate and open.

### R08 final delivery
PARTIAL. Source284ee89042e28ecabd0685f426a6faf8066e1233, CI35982639814 SUCCESS,
exact APK e4360075b85cdec265c75ec1b456aa301b70b066b808767cbe54787c28276e26.
R08 host9,870,055 / installed1,399 checks PASS; 18 required views and24 total
Surface captures reviewed;9 video segments retained (sample-frame review only).
Stable visible-window forest chunks, conifer/shrub/rock assets, terrain-conforming
explicit paths/farms integrated into normal game; ordinary ROAD lattice corrected.
PC art FAIL/REFERENCE_MISSING, full motion and ARM64/performance NOT_RUN.
Same draft PR67; final docs-only SHA in evidence delivery. No merge or R09.

## R09 final delivery — PARTIAL
Source dd11eeaaf7866ed2695e16ee3a093d05291b011a; CI35993038671 SUCCESS.
Official coalition-190/player5 Luoyang/Hulao/Mengjin 32x32 sample, production
LandscapeProfile, joined terrain-contact paths and .86 canopy proportions.
Host R09 1602 / retained R08 9,942,677; installed R09 522 checks PASS, including
real enemy22 attack damage, embark/garrison, turn, 2D/3D and full save/RNG parity.
28 Surface /29 UI captures,6 original recordings retained; sampled video review.
User reference02 recovered (640x372); reference01 returned placeholder data.
V2 FAIL, UI/overlay/modal flow FAIL; three-quality identity preserved but visual
switch acceptance incomplete. Full motion/physical ARM64/thermal NOT_RUN.
Exact APK hash0970bfbb9bda2a0bf0f16e36c7a83cab300adc61feebeb9db41e123598d7da31.
See R09 report, ten-item V2 table, figures and handoff. Same draft PR67; no merge/R10.

## R10 current execution
PARTIAL candidate. Real all-category unit asset upgrades, troop-count instancing,
per-member height/contact, journal-synchronized loss/HUD and legal-segment heading,
LOD hysteresis, stable-ID picking integrated. Filament/OpenGL and architecture
retained, no core/map production changes. Local R10 2,221,136 / field9,862,760 /
R06 199 checks and architecture PASS. matc56 unit material compiled. Exact Android
build/runtime evidence pending; see reports/R10.md. R09 V2/overlay/manual/device
and all inherited failures remain open. No merge, no R11.

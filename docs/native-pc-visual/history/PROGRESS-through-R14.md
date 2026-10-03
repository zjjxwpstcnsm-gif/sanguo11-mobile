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

## R11 current execution — PARTIAL candidate
Continue exact R10 source5df81a04; mainac29b458/architecture66 retained. Typed
journal IDs/tactics/naval/scalar impacts, eight-mesh64-particle terrain-aligned
combat sampler, normal player-command presentation after authority autosave,
immutable before/final snapshots, bounded retained de-duplication and immediate
pause/skip/cancellation are integrated. HostR11 99,443 / combat25,470 PASS;
independent R10 full-save golden parity16/16. Exact Android build/runtime pending.
R10 installed run was actually FAIL at removed RAM actor probe; source probe repaired,
not retroactively passed. R09 visual/UI and legacy rule/device defects remain open.

## R12 current execution — PARTIAL candidate
Native north-up overview/input, independent host preferences, authoritative faction/site boundaries, sampled/dashed tactical overlays, stable-ID selection and panel/date layout fixes. R11 CI36019140571 verified success; old pending prose is historical. Exact R12 build/runtime pending; preserve all R09 art and physical/manual gates. No main merge or R13.

## Final inspected delivery — PARTIAL (2026-09-24)

APK source: `a57d3f2e13cbaa9a4da3dca781b4f6ae7de103ec`. Later commits only package evidence and update reports; no runtime changes. APK SHA-256: `1964899b3969f9d71e8a9c7a2350860c5c7c64bcd4e786600f14f875506fb6ae`, 37,346,041 bytes. Application `game.sanguo.mobile.dev`, version95 / 0.95.0-native-r12; arm64-v8a, armeabi-v7a, x86, x86_64. Filament1.56/OpenGL; installed evidence is API29 x86_64 Pixel2, 1080x1920, SwANGLE/SwiftShader, not physical GPU evidence. Certificate SHA256: `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`.

Build/androidTest/lint PASS in run36047497184. R12 host395124, retained interaction32/editor136, R11 99443, combat25470 and architecture suites PASS. Full core retains `CoreTest.logistics:75: AI uses deployment commands` FAIL, independently also present in R11 logs; the workflow continue-on-error does not make this test pass.

Installed R12 overall FAIL: four grid/territory combinations, 2D parity, Activity recreation, temporary editor override, full-save equality, north-up minimap with yaw67/no command leakage, panel interception and date width PASS. Opening preview then timed out in SceneInstrumentation.ready (NativeR12Instrumentation.preview:64): span86.256714, submitted21, pending654, output WAITING_FRAME. This is a real unresolved large-view loading/readiness problem under software rendering; root cause and physical-device behavior are unproven. No assertion was removed, timeout enlarged, or 2D fallback counted. Downstream scenario start, official deployment/move/turn/autosave/load and explicit attack fixture were NOT_RUN in this R12 attempt. Host range equality is not a substitute for that flow.

Same-source separate R00 run36047504121 PASS fresh139/reload97; its screenshots confirm native scene/startup/load only, not R12 end-to-end acceptance and not binary identity with the delivered APK. R03 run36047504298 success. R12 release publication succeeded: https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/tag/native-r12-a57d3f2e13cb . Earlier superseded run release403 is not the final result.

Evidence: raw installed UI/surface captures, six operation recording segments, interaction failure stack, runtime diagnostics/logcat and build/host logs retained. Transfer packaging run36049535452 succeeded; reconstructed raw ZIP verified SHA256025f7f5dd6618ca04198c1a455cf622184200269173d9c6fdae5f258fd246e4f. No full-video or touch-only acceptance is claimed.

Visual inspection sampled actual composed four-combination/panel images and R00 fresh/load: date and action buttons readable at this size, terrain/city/ports and north-up overview visible; minimap is noisy and changes aspect ratio with panel viewport, coastline remains visibly stepped, vegetation sparse. One four-combination image retains loading text despite content detection. Labels still cover parts of sites. Grid/faction Canvas annotations are not per-fragment depth tested; centre-ray dashed tactical outlines do not solve partial-cell occlusion. R09 V2/PC-art acceptance remains open.

Physical Adreno/Mali, full touch-only tactics/facility/siege/report flow, all lists, small/large landscape/portrait matrix, dynamic-resolution touch parity, cold restart preference matrix and long-duration performance remain NOT_RUN or partial. Preserve these gates before declaring R12 complete. Do not merge main or start R13.

## R13 — delivered PARTIAL
Source a9d444e13332c6f0103c664d89db56070c27a273; CI36059464649 scoped installed PASS348
and legacy MAP_EDITOR67 ANDROID PASS, host suites/build/APK/lint PASS. Actual editor
persistence rollback, surface retention, ground picking/source labels and optional
visual recovery/save independence are integrated. Custom scenario command/turn/save/load
reference equality passed. Turn latency137728ms on software emulator is NOT_ACCEPTED;
composed date freshness is unverified. CoreTest.logistics inherited FAIL remains.
R12 full-map preview and R09 V2/physical/manual gates remain open. reports/R13.md,
handoffs/R13.md, R13_FORMAT.md and R13_COVERAGE.tsv contain exact scope and identity.
No R14 or main merge. Final evidence-only commit does not change the runtime tree.

## R14 current candidate — PARTIAL
Authoritative month projection, shared seasonal environment and separate grass/foliage/crop/water material response integrated; uniform-only date updates preserve terrain resources. Pinned matc56 compiled four production materials. Local R14 host590 and material manifests PASS. Exact Android/runtime evidence pending; R09/R12/R13 legacy gaps and physical/PC gates remain. See reports/R14.md. No main merge or R15.

## R14 final delivery — PARTIAL
Source32f278ff8c56cc6e898f1553468751173228797a; CI36081028678 SUCCESS, host590/installed909 scoped PASS; APK97 built and verified. Four-season uniforms, stable resources and real quarter turn/save/load verified. 37 paired captures reviewed. UI date/playback freshness FAIL, software turn75.333s NOT accepted; PC V2/physical/temporal shadows remain open. Core inherited FAIL retained. See reports/R14.md and evidence/R14.json. No merge or R15.

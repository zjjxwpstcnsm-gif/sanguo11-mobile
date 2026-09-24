# Native defects

| ID | Classification | Status | Evidence / next action |
|---|---|---|---|
| R00-REPORT | inherited | PARTIAL | R00 report/ledger/handoff absent at bdc6856; current audit recovers real CI evidence, does not invent historical authoring. |
| ART-01 | inherited | PARTIAL | Actual R00 Surface has stepped shorelines and coarse/repetitive assets; retain for later art stages. |
| REF-01 | inherited | BLOCKED | REFERENCE_MISSING: no matching PC screenshots supplied; similarity not accepted. |
| DEVICE-01 | environment | BLOCKED | No physical ARM64 Adreno/Mali phone; long-run native memory, thermal and performance NOT_RUN. |
| RULE-01 | inherited | PARTIAL | Old reports record core AI deployment failures. No core/rule/map changes in R01. Full Android legacy workflows have independent baseline failures; do not relabel as R01 pass. |
| R01-BUILD-01 | new, fixed | PASS | First CI found one remaining meshTask reference in loadVisible; replaced with bounded queue.pending, retained failure run 35850696246. |
| R01-DECODE | inherited | PARTIAL | Mesh generation is bounded/off-thread. Existing asset/pose decode and texture initialization still execute on owner; moving entire decode pipeline is not claimed complete. |
| R01-FAULTS | validation gap | PARTIAL | Injected worker failure and missing asset-provider constructor exercised by installed suite; actual corrupt texture/native upload failure and native abort recovery require broader fault matrix. |
| R01-REVISION | implementation limit | PARTIAL | Session/generation and content invalidation connected. Asset revision fixed to bundled cohort; no hot reload. Standalone editor does not own GameSession; content identity still invalidates mesh but full editor replacement/event coverage needs R13. |
| LEGACY-UI | inherited, reproduced by baseline/candidate CI | PARTIAL | experience: `UI content not reachable by scrolling: 收起`, baseline run 35846793585 and candidate 35850964174. |
| LEGACY-SAVE | inherited, reproduced by baseline/candidate CI | PARTIAL | displacement pinned v0.28: old map save rejected by existing policy, baseline 35846793583 and candidate 35850964238. No policy/assertion change. |
| LEGACY-UI-TITLE | inherited | PARTIAL | Android workflow smoke expects `190 讨伐董卓 · 重建  ·`; same assertion fails in baseline job 107134769785 and candidate 107148279521. |
| DRIVER-S10 | inherited observed failure; root cause unresolved | PARTIAL | swiftshader_indirect instrument gate exits 1 on baseline job 107134800174 and candidate 107148278723; SwANGLE material job passes. No blanket driver compatibility claim. |
| LEGACY-S11 | inherited timeout | PARTIAL | WaterLandformInstrumentation exceeds 900 s on baseline job 107134794758 and candidate 107148278864. Do not equate timeout with successful full water validation. |
| PERF-R01 | environment/validation limitation | PARTIAL | Software-emulator CPU submit tails exceed 1 s in lifecycle logs. No handset timing pass. Post-instrumentation dumpsys reports no process, so PSS is unavailable, not zero. |
| PROBE-PAUSE | newly observed, pre-existing probe ordering | PASS | 04521e8 startup run 35853388195 timed out with worker_pending=0 and pending=6 after the probe paused rendering. Fixed capture-before-pause order in 8744cb0; all assertions retained. Retest 35855186990 passed fresh 285/reload 192. |
| R02-DOC | inherited, resolved | PASS | R02 reports arrived in concurrent documentation commit 943ddcf and were merged intact during R03. |
| R03-SHORE | inherited constraint limit | PARTIAL | Smooth cap release retains exact water/site/path footprints; minimum caps and cell shoreline outlines can still crease; R05 acceptance required. |
| R03-VEGETATION | inherited memory debt | PARTIAL | Terrain now view-bounded; vegetation still retains nationwide CPU meshes and revision-dependent placement. |
| R03-PERF | validation gap | PARTIAL | Two uploads/frame is not a 2ms guarantee; 3-level geometry preserves planes and near subdivisions need handset profiling. |
| R02-INPUT | new, fixed | PASS | Continuous camera basis now shared by render/projection/picking; long press no longer invokes armed command tap. See R02_COORDINATES.md. |
| R02-NUMERIC | validation finding | PASS | Initial new zoom test assumed 0.001 pixel despite float world storage; observed 0.0012 pixel rounding at high zoom. Explicit 0.01 view-pixel bound; existing assertions unchanged. Independent world-height oracle retains 0.001 world-unit tolerance. |
| R02-MANUAL | validation gap | PARTIAL | Full hand-operated attack preview, moving-unit/label occlusion, notch/landscape, long-press and three-finger phone matrix not yet accepted. Automated emulator scope reported separately. |
| R02-CAPTURE | observed emulator failure | PARTIAL | First run35864250919 HIGH PixelCopy fails after LOW/MEDIUM pass; dequeueBuffer -110. Explicit transient retry/status and continued independent checks retain the failing gate. Final run35865854066 passes all captures on first attempt; intermittent root cause is not claimed closed. |
| R02-COMPOSITOR | observed software-emulator limitation | PARTIAL | Immediate post-gesture UI capture can retain old overlay while Surface advances; settled MEDIUM aligns. Physical-device frame/presentation latency NOT_RUN. |
| R03-VEGETATION-REGRESSION | new, fixed | PASS | CI35869690421 FieldAssetsTest rejected changed version-dependent vegetation contract; production seed restored, assertion unchanged; full CI retest required. |
| R03-PROBE | new, fixed | PARTIAL | S10/S11/S12 installed probes reflected removed distantTerrain field. Retained it as an actual accepted-chunk all-coarse diagnostic, exposed in report; assertions unchanged. Rebuild/retest required. |
| R03-FOCUS | new, fixed | PARTIAL | Manual inspection of 38eddcf found initial city focus clamped to the first streamed window. Bounds now come from complete immutable Ground, with all-site host and installed initial-focus assertions; new source rebuild/retest required. |
| R03-TURN-PROBE | new observed validation failure, race identified | PARTIAL | CI35872850213 passed focus/geometry shots but failed full turn-byte parity. Probe read UI world off-owner after aiRunning cleared but before bindSession could finish. Read now crosses main-thread barrier; unchanged equality assertion plus failure save dumps. Exact-source retest required; no gameplay edits. |
| R05-PORT-AI | inherited, reproduced | PARTIAL | PortReplayTest.aiDocks(false): AI completes embark/sail/land fails trace [7,6, 10,5] on both untouched b78566b and R05; no rule changes. |
| R05-CONTOUR | implementation/visual gap | PARTIAL | Shared bank textures and true interior distances implemented; exact step silhouette is retained. Curved geometric coast and PC comparison not accepted. |
| R05-DEVICE | validation gap | PARTIAL | ARM64, installed boat embark/sail/landing recording and edited-map continued travel remain NOT_RUN. |


## R04 audit updates

- R03-PROBE / R03-FOCUS / R03-TURN-PROBE: final bf687c7 CI35874694865 PASS423. Earlier failure logs remain valid history.
- REF-01: official PC manual 小沛/farm reference recovered (REFERENCE_INDEX.csv). Exact camera/season-matched screenshots remain missing; similarity/V1 not automatically PASS.
- R04-GENERATOR (inherited, fixed): old terrain generator erased unrelated environment manifest fields. Preserve these fields; reproducibility verified separately.
- R04-TEST-FIXTURE (new, fixed): synthetic no-city World cannot be saved by valid SaveCodec policy. Map invariance tested directly; complete official save remains strictly compared.
- R04-V1 (validation gap): actual exact-source image and video review pending CI, not replaced by numeric texture statistics.
- R04-DEVICE (environment): physical ARM64/performance/thermal NOT_RUN; ground sampler bandwidth and residency have increased.

- R04-RUNNER (new, fixed; retest pending): CI35937501014 installed APKs, then INSTRUMENTATION_FAILED before any scene because NativeR04Instrumentation was missing from androidTest Manifest. Registered in8d916b2; no production rules changed. Prior failure is retained, not relabelled a renderer pass.

### R04 actual-image corrections
- NEW / FIXED IN SOURCE: ordinary ROAD centre soil stamps created repeated dots.
  Removed those stamps; retained continuous road biome identity and added regression.
- INHERITED / FIXED IN SOURCE: VOID backdrop coarse8-unit weights and .86 tone
  showed square patches. Shared foreground attributes and2-unit sampling; final
  APK review required. No map/VOID/picking edits.
- NEW TEST COVERAGE: real farm now built via normal authority and turns, with
  full reference SaveCodec parity; no fixture injection. Await final CI result.
- PC reference found (official manual 小沛, spring195). Exact camera/content
  matching remains open; reference is comparison-only, never a runtime asset.

- R04-BUDGET (NEW, source corrected): b78566b dense duplicated backdrop exceeds
  existing1MiB S13 gate. Shared vertices and2.25 spacing pass unmodified national
  budget; R04 CI now requires national suite. Final-source rebuild/runtime pending.

### R04 final checkpoint
- R04-BUDGET: final host PASS33829951 with original<1MiB assertion. Runtime pending
  was superseded by cancellation, not success.
- R04-CONCURRENT: external R05 d40ea743 cancelled final R04 CI35943061193.
  Final-source installed matrix NOT_RUN; do not replace with candidate643PASS.
- R04-EMULATOR: final R03 CI35943061138 failed Android Emulator archive download,
  `Error on ZipFile unknown archive`, before game launch (environment).
- V1 complete visual acceptance FAIL; remaining backdrop tonal edges/shore geometry,
  PC mismatch and full motion review remain. ARM64/handset performance NOT_RUN.

| R05-FIXTURE-UI | observed validation gap | PARTIAL | CI35943948593 segment7: synthetic Surface with retained official UI/header labels. Authority parity passes; UI synchronization and physical reproduction not accepted. |
| R05-METADATA | probe metadata limitation | PARTIAL | Synthetic shot TXT hardcodes coalition-190; raw files retained and corrected shot-index explicitly marks fixture; no official-map art claim for fixture. |
| R05-RUNTIME | automated runtime | PASS | Exact b97e01c CI35943948593 PASS542,32 first-attempt PixelCopy captures; scoped automatic checks, not full manual/physical acceptance. |

| R06-INIT | implementation limit | PARTIAL | Geometry decode off owner; small rigid JSON/atlas initialization remains owner-thread. Texture streaming not claimed. |
| R06-ART | validation gap | PARTIAL | Gate/tree/catapult samples replace production resources; whole-map final art, matching PC views and ARM64 remain unaccepted. |

| R07-ART | art validation gap | PARTIAL | All site/facility IDs in R07 mappings explicitly transitional; low-resolution shared atlas reused, matching PC comparison missing. |
| R07-CONTACT | implementation gap | PARTIAL | City pad/embedded walls and port piles retained; arbitrary custom slope and explicit port rotations not fully conformed; actual multi-angle review required. |
| R07-DEVICE | environment | BLOCKED | Physical ARM64 Adreno/Mali, thermal and handset performance NOT_RUN. |
| R07-REGION | new, fixed in candidate | PASS | Initial one-ring terrain region missed water-city family; existing five-family assertion retained; three rings passes. |

| R07-MANIFEST | new, fixed; CI retest pending | PARTIAL | Initial candidate left old S12 environment sizes/hashes after intentional GLB replacement. Existing generator refreshes metadata; pipeline now checks this manifest too. Original assertions retained; first failure jobs107510218388/107510218248 preserved. |

| R08-ART | validation gap | PARTIAL | New opaque conifer/shrub/rock families remain transitional; shared atlas reused; matching PC forest/valley/farm missing. |
| R08-STRUCTURES | implementation limit | PARTIAL | Surface-conforming plank strips do not include full hanging trestle structure; embedded rocks are not complete cliff walls. |
| R08-MOTION | validation gap | PARTIAL | Stable LOD anchors and unit exclusion implemented; motion popping, in-between marching anchors and arbitrary slope contacts need actual review. |
| R08-PERF | environment/validation | PARTIAL | Host geometry bytes/timing are estimates, not GPU/physical FPS; wide-view and ARM64 long-run budgets unaccepted. |
| R07-MANIFEST-RETEST | inherited, resolved | PASS | Final R07 CI35961885126 SUCCESS, including manifest and installed acceptance; original failed runs retained. |

| R08-ROAD-LATTICE | new, reproduced and corrected | PASS |1a728e6 R00 raw Surface shows triangular ribbons on wide ordinary ROAD. Removed invented area adjacency lines; ordinary ROAD keeps R04 continuous grass/soil. Explicit mountain/plank strips remain. Final source284ee89 R00 CI35982640008 PASS; fresh/recreated actual Surface reviewed, lattice absent. |
| R08-READY | newly observed test ordering race, corrected | PASS | R00 fails visible terrain after Activity recreation: CPU pending cleared before visible GPU frame. Wait on owner for uploaded terrain/submitted Surface; original assertion/timeout retained. Final source284ee89 R00 fresh130/reload79 PASS with recreated terrain visible. |

| R08-RUNTIME | exact final source | PASS | CI35982639814 installed1399;18 matrix/24 total Surface shots,9 recordings. Automatic checks only. |
| R08-PATH-ART | observed visual gap | PARTIAL | Final captures retain blunt brown Y/triangle joins on explicit mountain/plank paths; ordinary ROAD lattice removed. Weak forest-road contrast. |
| R08-LOADING | observed runtime/validation gap | PARTIAL | Sampled recording has static waits and black loading during recreation; final Surface populated. CPU P99 up to2664.76ms on swangle. Continuous motion/physical responsiveness unaccepted. |
| R08-MEMINFO | evidence limitation | PARTIAL | Post-instrumentation dumpsys says No process found; no process PSS result claimed. Geometry/texture estimates only. |

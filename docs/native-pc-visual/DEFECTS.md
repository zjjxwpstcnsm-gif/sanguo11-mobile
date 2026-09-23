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
| R03-VEGETATION | inherited memory debt | PARTIAL | Terrain now view-bounded; vegetation still retains nationwide CPU meshes. |
| R03-PERF | validation gap | PARTIAL | Two uploads/frame is not a 2ms guarantee; 3-level geometry preserves planes and near subdivisions need handset profiling. |
| R02-INPUT | new, fixed | PASS | Continuous camera basis now shared by render/projection/picking; long press no longer invokes armed command tap. See R02_COORDINATES.md. |
| R02-NUMERIC | validation finding | PASS | Initial new zoom test assumed 0.001 pixel despite float world storage; observed 0.0012 pixel rounding at high zoom. Explicit 0.01 view-pixel bound; existing assertions unchanged. Independent world-height oracle retains 0.001 world-unit tolerance. |
| R02-MANUAL | validation gap | PARTIAL | Full hand-operated attack preview, moving-unit/label occlusion, notch/landscape, long-press and three-finger phone matrix not yet accepted. Automated emulator scope reported separately. |
| R02-CAPTURE | observed emulator failure | PARTIAL | First run35864250919 HIGH PixelCopy fails after LOW/MEDIUM pass; dequeueBuffer -110. Explicit transient retry/status and continued independent checks retain the failing gate. Final run35865854066 passes all captures on first attempt; intermittent root cause is not claimed closed. |
| R02-COMPOSITOR | observed software-emulator limitation | PARTIAL | Immediate post-gesture UI capture can retain old overlay while Surface advances; settled MEDIUM aligns. Physical-device frame/presentation latency NOT_RUN. |

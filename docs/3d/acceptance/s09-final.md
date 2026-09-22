# S09 final candidate — PARTIAL, not a certified default-3D release

Input branch: `4091035a3fb67e032aa49692bc682ff98916ef8d` (S08).
Main: `1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`; PR #62 stays draft on
`agent/3d-s01-renderer-foundation`. User explicitly requests no main merge.
Version 0.76.0-3d-s09-rc1 / 76, same package and public development signing key.

## Implemented repairs

- Critical FIRE already produces a three-turn fire in War.resolvePlot. CampaignSave
  rejected values above two, preventing saving a legal command result. Accept 1–3;
  keep invalid 4 rejected, wire the real SHENSUAN action into the full-save/event
  equivalence suite at 1x/2x/4x/skip. No damage, RNG, duration or save-format change.
  This resolves the specific critical-fire blocker recorded in S06 and S07.
- Latch interrupted native-session state once per process. A second host/editor
  must not clear the automatic-restoration block just because another host cleared
  the persisted active marker. Java/native-link failures also latch recovery and
  persist exception type/time. Explicit manual retry stays available. Native crashes
  remain handled by the pre-load disk marker on next launch, not Java catch.
- Check advertised OpenGL ES 3.0 before native initialization. This is a capability
  prerequisite, not device certification. Unknown devices still default to 2D;
  no device allowlist is invented and no saved manual choice is forcibly upgraded.
- FinalSceneInstrumentation checks injected Java failure, same/new-host restoration,
  manual retry, native PixelCopy, normal release and unchanged full strategic bytes.
  It does not simulate an actual native crash, process kill or power loss.
- `scripts/test-3d-final.sh` executes independent existing regression suites and
  retains every failure in a TSV instead of silently stopping at the first one.
  CI reruns full S01/S04/S05 native checks as well as S06/S07/S08/S09. Run only PR
  synchronization/manual events to avoid duplicate push/PR emulator jobs.

## Final acceptance matrix

Host command/serialization coverage is distinct from installed UI acceptance.
Exact latest results are in the delivered evidence/matrix and build manifest.

| Scope | Reproducible coverage | Remaining gate |
|---|---|---|
| Startup, restore, 2D fallback, lifecycle | S01/S08 + S09 installed probes | real process-death/upgrade matrix and phones |
| Scenario/faction, national browsing | foundation, interaction; S01 native | full wizard-only user flow |
| Three-officer deployment, land/naval/transport/siege units, automatic march | core check, field asset/flow suites | all commands via wizard on device |
| Attack/tactic/plot, critical fire, events, reports | combat 16 legal cases, turn48, core | manual action/animation UI matrix |
| Seven-cell entry/attack, center exit costs | city55, field flow | manual touch at all legal cells |
| Ports, water, invalid border, road-as-plain | map61, foundation, field flow | exhaustive nationwide visual inspection |
| Domestic, districts, lists, advisor/title, wounded/recruitment | v064, v066, titles, UI, core | full native list/navigation touch-through |
| Facility range, siege, adjacent capture, single settlement | siege, city55, turn48, field | real-device visual overlay review |
| Saves, maps/Patch, custom officers/avatars | map-editor67, custom-officers, interaction, core | system picker + cross-version install without data reset |
| 2D/3D speed/skip/pause/background/quality | combat host, S06/S08 installed | long controlled physical-device comparison |
| Near/far models, forest/city density, reversed camera | actual GLB parsing and native S01/S04/S05 | final art acceptance in every region/preset |
| Packaging, dependencies, offline assets | APK audit, pinned Filament/matc 1.56.0, texture requirements | offline cold start on supported phones |

## Assets, performance and limits

No new model or texture is claimed in S09. All 165 runtime GLBs, original generation
scripts, source/license/manifests and 2D fallback resources remain available. Site
and field asset reports are authoritative for triangles, UVs, LOD and animation.
Static water, low-resolution baked atlases, generic effect geometry and synchronous
first-use GLB decoding remain limitations. No whole-map art approval is inferred
from loader tests; finalArtAccepted remains false. No asset is moved to a cloud.
No unreferenced APK resource was established safely enough to delete during S09.

Physical arm64 midrange FPS/P50/P95/P99, 20-minute PSS/leaks, GPU, power/thermal,
16 KiB installation, installed/cache bytes and fair S01/current performance comparison
are UNMEASURED. APK bytes and 16 KiB ELF/ZIP alignment are recorded separately;
alignment is not proof of a 16 KiB-device installation. Do not declare basic 3D
finished, remove trial labeling or turn on 3D for all devices.

Final APK is explicitly the PR-head arm64 candidate, not a main build. Source SHA,
SHA-256, signing certificate, exact bytes, ABI and build results belong to the
artifact-side manifest to avoid self-referential commit hashes.

## Host execution result

Final matrix checkpoint: 13 PASS / 2 FAIL. map61 stops at its hard-coded revision61
assertion (current national map is revision63); turn48 wrapper stops in historical
CoreTest (AI deployment). The direct Turn48Test passes separately. These failures
remain FAIL; the CI enforcement step runs after native probes so useful device
results are still collected without turning the matrix green.

Full `:core:check --continue`: 31 failing tasks. Replacing only CampaignSave with
the S08 original in an isolated classpath reproduces the same first exception for
all 31. This isolates this turn's only production-core change; it is not an audit
of every historical failure or proof that the old failures are harmless. Raw
comparison is delivered with the candidate. Do not call the full project green.

Concurrent S08 commit ed8a08a was retained before final integration: retired native
scene CPU references are released and crash diagnostics are collected. No remote
history was overwritten. Final native CI, APK audit and build identities are
reported with the downloadable artifacts rather than predicted in this document.

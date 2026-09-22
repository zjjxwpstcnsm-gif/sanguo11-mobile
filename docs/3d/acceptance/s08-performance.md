# S08 performance / packaging — PARTIAL, device gates open

Input branch `3439247d634a0df9f7f62b14348323eaf413fcd9`; main remains
`1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`. Continue PR #62; never merge main.
Normal game → 视图 → 3D 画质. Default renderer remains 2D; medium quality is default.

## Production changes

- Low / medium / high: 30 / 30 / 60 fps, 70 / 85 / 100% Surface buffer resolution,
  native UI/input projection unchanged, atlas max 256 / 512 / 1024, lower forest
  mesh and formation LOD on low, high-only 4x MSAA on GLES3.1+; GLES3.0 compatibility disables this optional resolve. No dynamic resolution or second
  scheduler. Fractional vsync pacing verified on 60/90/120/144 Hz. Android29+ severe thermal status caps 30fps / 70% scale through that same
  scheduler; recovery waits for light/none (hysteresis). Older devices report N/A. Switching quality rebuilds the engine through existing
  camera-preserving 2D/3D ownership path, without modifying World or saves.
- Primitive pose buffers replace Float/Integer lists: same geometry hash across
  600 poses. Host median 286.70 → 81.92 ms (-71.4%); allocations 1,086,301,200 →
  231,133,200 bytes (-78.7%). See raw seven-round/warmup benchmark methodology.
  This is host pose generation, NOT phone FPS or whole-game improvement.
- CPU rest-mesh LRU has a 24 MiB retention budget (single oversized item exception);
  unreferenced GPU pose caches have preset 64/96/128 limits; referenced meshes are
  never destroyed to satisfy a count. Scratch sets reused; trim skips below budget.
  Assets still have first-use synchronous GLB decoding; that remaining stall is not
  hidden behind a claim of fully asynchronous loading.
- ETC2_SRGB8 direct block upload and complete mip chains for both opaque atlases.
  Offline etcpak 0.9.15; linear-light mip filtering, RGB/alpha/size checks, independent
  texture2ddecoder 1.0.6 decode and PSNR gate. Encoder's installed 0.9.15 RGBA input
  verified empirically: the old README's BGRA example swaps red/blue, caught by test.
  No normal/roughness/occlusion textures exist here; do not apply sRGB to future data
  maps. PNG fallback only if compressed format is unsupported. These tiny atlases
  do not justify ASTC plus ETC2 duplicate high-resolution sets or a Basis transcoder.
  Native installed format and PixelCopy are separate gates. PSNR is not full art review.
- Fixed water treatment and no realtime shadows retained: not advertised as new
  shadow/water quality controls. Combat event pool remains bounded; effects unchanged.
- `-PtargetAbi=arm64-v8a` standalone phone build; `x86_64` emulator build, explicit
  armeabi-v7a/x86 and existing universal build still available. Same package/signing.
  Locked Filament 1.56.0; no runtime material compiler/download requirement.
- 2D diagnostic toggle null access repaired.

## Reproduction and evidence

`JSON_TEST_JAR=... bash scripts/benchmark-3d-performance.sh` compares the exact input
FieldAssets class against current implementation with identical assets and geometry.
`python3 tools/3d/compress_atlases.py` regenerates two ETC2 assets plus manifest.
`python3 scripts/audit-3d-apk.py APK --abi arm64-v8a --require-16k` records ZIP ledger,
ELF PT_LOAD alignments and uncompressed native ZIP offsets. Candidate arm64 is ~26.3 MiB;
final exact bytes/hash belong to the final artifact manifest. All arm64 native PT_LOAD
segments and APK entry align to 16384. This does not prove installation on a 16 KiB phone.
R8 APK + test APK + lint compile with Java17 source/target, AGP8.9.2 and Android35.

CI builds arm64 separately from x86_64, then runs 20 native quality/lifecycle cycles,
actual compressed-format checks, native PixelCopy, full-map/home travel and unchanged
SaveCodec comparisons, logging PSS/Java/native memory separately. Final CI identity
and pass/fail must be inspected; merely wiring the suite is not a pass.

`capture-3d-device.py` requires exact SurfaceFlinger layer plus controlled-input manifest;
it collects Perfetto, raw presentation timestamps, PSS, thermal and battery records for
1200 seconds. Run original-2d, current-2d and current-3d separately on the same phone.
Trace FrameTimeline coverage must be reviewed before interpreting Surface intervals.
Unknown GPU duration/energy remains null, never a fabricated zero.

## Open gates / S09 handoff

No physical phone attached. Device model/GPU/driver, 20-minute mixed-load P50/P95/P99,
thermal/power, installed/cache sizes, cold start and 16 KiB-device install are UNMEASURED.
Three-way S01/current comparison, route-controlled dense forest/facility/army/fire,
heavy next-turn and continuous editing need a controlled physical-device session.
The collector records these gates; it does not synthesize results or drive every route.
20 scene/quality native cycles do not replace 20 editor/save-switch manual cycles.
No midrange 30fps, 60fps, <800MiB PSS or leak-free claim is made. S06/S07 manual/art
limits remain open. Preserve default 2D and same branch; S09 cannot mark full acceptance
until those gates and actual visual review pass.

The exact texture tool versions are pinned in `tools/3d/texture-requirements.txt`.
Both decoded atlas base levels exceed 36 dB PSNR. Compressed mip payload is 30,112
bytes vs 180,224 bytes for prior RGBA base levels (or 240,316 RGBA mip bytes).
This is atlas GPU payload, not total process/GPU memory. PNG fallback adds only the
existing small atlas files; no duplicate ASTC or full external content pack is included.


## Native issue found during S08 acceptance

`ed8a08a` reproduces a HIGH-first native SIGABRT on API29 / x86_64 / SwiftShader
GLES3.0: Filament 1.56 `blitLow` fragment shader fails compilation (structure
constructor mismatch) when MSAA adds an intermediate resolve. This is not Java OOM.
The earlier LOW/MEDIUM passes prove real ETC2 texture uploads and Surface rendering,
not full lifecycle acceptance. Keep native logs in the delivered evidence.
MSAA now requires the conservative GLES3.1+ profile; GLES3.0 HIGH remains full 3D,
full resolution, high LOD and 60fps cap, with AA disabled and reported in diagnostics.
GLES3.1+ MSAA remains a physical-driver acceptance gate. No emulator-only model/map
removal or permanent 2D fallback is used to mask the failure.
Retired views now immediately drop CPU meshes/snapshot/rest cache ownership as well
as releasing native resources; this independent lifetime improvement is retained.
Concurrent S09 commits on this shared branch are preserved, including version76,
critical-fire save validation and safe-restore latching. S08 APK source identity must
therefore distinguish the integrated branch from the earlier S08-only measurements.


## Installed acceptance checkpoint 7c70a3a (2026-09-22)

Run 35714682829 PASSED: 20 complete HIGH/LOW/MEDIUM cycles, 1234 checks including
readiness polling (not 1234 independent workflows), actual compressed texture formats,
PixelCopy at every cycle, buffer/UI coordinate dimensions, unchanged SaveCodec bytes,
worker shutdown and immediate CPU/native ownership release. HIGH-first no longer
crashes on SwiftShader GLES3.0. Screenshots retain the actual production game scene.
PSS ranged 241.62–270.62 MiB; first 266.64, last 244.54. No monotonic growth was
observed across these 20 samples. This is emulator instrumentation-process PSS, not a
phone measurement, GPU memory, 20-minute soak or proof against every possible leak.
Raw samples: benchmarks/s08/native-cycles.tsv; metadata: native-checkpoint.json.

An idle-host rerun (JDK21, no other local CPU build) gave 275.17 → 86.13 ms per
600 poses (-68.7%); allocated bytes remain -78.7%, with identical geometry hash.
Both initial and idle measurements are retained. No phone FPS conclusion follows.
The final handoff commit only records evidence/manifests after this runtime checkpoint.
Overall status remains PARTIAL for the physical-device/manual/art gates listed above.
S09's unrelated full-core historical failures remain visible in s09-final.md.

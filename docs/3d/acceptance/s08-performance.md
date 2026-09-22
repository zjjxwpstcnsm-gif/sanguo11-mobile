# S08 performance / packaging — PARTIAL, device gates open

Input branch `3439247d634a0df9f7f62b14348323eaf413fcd9`; main remains
`1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`. Continue PR #62; never merge main.
Normal game → 视图 → 3D 画质. Default renderer remains 2D; medium quality is default.

## Production changes

- Low / medium / high: 30 / 30 / 60 fps, 70 / 85 / 100% Surface buffer resolution,
  native UI/input projection unchanged, atlas max 256 / 512 / 1024, lower forest
  mesh and formation LOD on low, high-only 4x MSAA. No dynamic resolution or second
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

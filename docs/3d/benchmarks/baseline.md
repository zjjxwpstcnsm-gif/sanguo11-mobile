# S01 baseline and measurement boundary

## Original 2D build

- Exact input SHA: 1a883a4ffd1098ca85f7da38464b7c5fbfdc000f.
- Built from isolated detached worktree, no renderer changes.
- AGP 8.9.2 / Gradle 8.11.1 / Android SDK 35; Java source/target 17.
- Host compiler: Temurin 21.0.12.1 (the supplied Java 17 runtime lacked a javac executable).
- Normal minified non-debuggable debug APK, existing public development key.
- APK: **23,368,598 bytes** (22.286 MiB). No native ABI before Filament.
- First 3D checkpoint APK: **32,424,650 bytes** (30.922 MiB), +9,056,052 bytes
  (+8.637 MiB; +38.75%). Universal ABI packaging, NOT arm64-only.
- Final SHA, exact bytes and SHA-256 are in the deliverable build manifest and PR, not
  recursively committed into the source revision they identify.

## Runtime / performance

Installed native correctness uses Android 29 x86_64 emulator, SwiftShader OpenGL ES 3,
1080x1920 display. The final correctness AVD explicitly uses 4 GiB RAM and a 256 MiB
Java growth limit / 512 MiB maximum; runtime heap properties are archived. The system-image
default previously clamped the heap to 16 MiB after backgrounding, causing a SaveCodec OOM
during the test that holds an additional reference World. This is not a phone RAM result. Surface dimensions appear in the app diagnostic report, separately from
display size. This is NOT a phone performance benchmark. Initial test collected 2D/3D
screenshots and exercised ten switches before a test sampling issue; final acceptance is
tracked separately.

Physical device/system/GPU/driver: N/A (not available).
Presented-frame P50/P95/P99, jank, GPU time, PSS, Java/native memory, thermal/battery,
cold start, scene-ready time, installed data/cache: N/A — not measured.
Same-device baseline vs 3D pure-turn/animation/total time comparison: N/A.
Snapshot/headless next-turn equivalence: automated PASS, not a performance measurement.

CPU terrain payload on the tested world: 38,949 * (4*28 + 6*4) = 5,297,064 bytes of
vertex/index arrays, excluding JVM array/object overhead and staging copies. This is
an asset budget, NOT measured GPU or process memory. GPU residency is view-dependent.

Initial targets retained: 30 FPS midrange, optional 60 high-end; arm64 standalone <300 MiB,
400 MiB warning; PSS <800 MiB; investigate >10% unexplained pure-compute regression;
250/28-faction total-turn goal 10s where available. No target is declared achieved or
silently relaxed. Diagnostic callback interval is explicitly not presented FPS.

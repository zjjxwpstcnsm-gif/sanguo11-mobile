# U00 baseline (2026-09-23)

## Repository state

- Remote `main` at inspection: `73249f12f269effece33816884364f8dbe1714e7`, clean checkout. GitHub open PR search: zero; no unrelated PR merged.
- `settings.gradle`: `:core`, `:app`. Rules remain Java 17 under `core/src/main/java/game/sanguo/core/`. Main game is `app/src/main/java/game/sanguo/mobile/MainActivity.java`; 2D `MapView`, optional 3D `MapHost` / `FilamentMapView` with Filament Android 1.56.0; the latter releases native resources on mode exit. No Unity Player or Unity project was present at baseline.
- Main game reads/writes autosave through `AtomicFile` in `MainActivity`. The actual `SaveCodec` write version is **33**. The existing map editor and custom-officer editor are `MapEditorActivity` and `CustomOfficerActivity` respectively.
- Package is `game.sanguo.mobile.dev`; `version.properties` at baseline is versionCode **80**, versionName `0.80.0-3d-s13-rc1`. `tools/android/dev-debug.keystore` is a public development-only signing key; documented SHA-256 certificate fingerprint is `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`. It is not a production signature. U00 raises versionCode to 81.
- Android Gradle Plugin 8.9.2 and Gradle 8.11.1 at baseline; workflows in `.github/workflows/` build native Android on hosted runners. `app/src/main/assets` occupies roughly 29 MiB on this checkout (directory disk usage, **not** APK size).

## Baseline failure and measurement limits

- The exact-main Android APK workflow run [35805294731](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35805294731) built Android successfully, but an existing emulator smoke test failed: `UI text not found: 190 讨伐董卓 · 重建  ·` in `GameSmokeRunner.waitText`. This failure predates U00.
- Its failed run did not publish a main APK artifact. This checkout has no Android SDK, Gradle distribution cache, `javac` executable, Unity Editor, active Unity build authorization, Android Build Support, `adb`, or connected ARM64 device. Native main APK bytes and startup PSS therefore remain **unmeasured** here.
- The first `bash scripts/test-ui-models.sh` attempt stopped after 49,018 UI, 451 terrain and 1,136,811 geometry checks because the image has no `javac` executable. A temporary `/tmp` shim invoking the installed `jdk.compiler` module then let the **entire script pass**: additionally 1,168,801 legacy coordinate assertions, 898,305 map coordinate checks, 11,371 siege checks and 14 overlay checks. The shim was not added to the repository or substituted for Android/Unity compilation.
- `bash scripts/test-core.sh` with the same compiler shim stops at `CoreTest.logistics` assertion `AI uses deployment commands`. U00 has no changes under `core/`; this is a current-main rule-test failure, not evidence of a Unity bridge regression.

## U00 comparison slots

| Measurement | Native main `73249f1` | Unity U00 `a59c1c4` source tree |
|---|---|---|
| APK bytes | unavailable (failed CI run omitted APK artifact) | unavailable (Unity Editor absent) |
| Launch PSS / peak | unavailable | unavailable |
| Package/ABI | `game.sanguo.mobile.dev`; native ABI selection supported | intended same package; ARM64 export specified, APK unverified |
| Installed 10-entry return loop | not applicable | unverified; requires physical ARM64 device |

Do not use the 29 MiB source assets or an older APK as the measured APK baseline.

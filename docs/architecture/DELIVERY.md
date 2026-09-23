# Architecture delivery evidence — 2026-09-23

## Source and branch

- Baseline: Unity `9548bb350051150b21a61213f9068ffb1b7506c0`; main `73249f12f269effece33816884364f8dbe1714e7`.
- Work branch: `agent/architecture-foundation`; Draft PR #66 targets `agent/unity-pc-visual`. Neither PR #65 nor main was merged.
- Exact production source used for the delivered APK: `b1928687d27da1fb87513a6338be945ed2f2a351`.
- The delivery workflow was triggered by `3a514ae391d95830acd974e7e509324799b35c18`, then applied and committed the prepared changes before compilation. Its `SOURCE_COMMIT`, source bundle and build log identify `b1928687d27da1fb87513a6338be945ed2f2a351`, not the trigger SHA, as the APK source.
- This follow-up adds evidence only. It does not change production source, game data, signatures, protocol or Uxx stage status; the APK was not relabelled as a build from the evidence commit.

## APK

- File: `sanguo11-architecture-native.apk`.
- Type: native Android debug/development host; built with `-PunityBridgeProbe=true`; no Unity as a Library export.
- Package: `game.sanguo.mobile.dev` (existing development application ID).
- Version: `0.82.0-unity-u01-bridge`, versionCode `82` (unchanged from the existing build configuration; the name is not a Unity Player verification claim).
- Minimum SDK 26; target/compile SDK 35.
- ABIs: arm64-v8a, armeabi-v7a, x86, x86_64.
- Size: 35,372,358 bytes (approximately 33.73 MiB).
- APK SHA-256: `108cc216b974f3523392d63ab36d3b2fffdb387abdea6c0c4a769cc4cccfb589`.
- Signing certificate SHA-256: `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`; existing development signing configuration retained.
- Archive integrity and APK SHA-256 were independently checked after downloading. The APK has no `libunity.so`.

## CI and downloadable evidence

Successful exact-source workflow: https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35836626100

- `architecture-native-apk-and-evidence`, artifact `10739931636`: APK, build log, instrumentation result, logcat, screenshot, metadata/signing and Java/C# regression logs. Archive SHA-256: `368f1d08a1c969b81b40128499b5ce40cd29f03ebc7816d96d2a44e7499e39c1`.
- `architecture-final-source`, artifact `10738983625`: actual Git bundle, source SHA and commit list. Archive SHA-256: `9b5e37b19435e8e3d865b907821b544d949bd2a13f4ccffe56692cf7ba24be08`.
- Artifacts are retained by Actions and can expire. The conversation delivery separately provides the extracted standalone APK, not just the workflow page.
- The separate PR-triggered generic Android workflow `35836650435` reported `action_required`. It is not the successful exact-source delivery job and must not be described as green.

## Verified tests

The following were checked in the downloaded exact-source CI logs:

- Independent old-source baseline: seven complete SaveCodec states, including serialized RNG, matched unchanged native and bridge rule calls.
- GameSessionTest: 1,673 assertions passed (full-save parity, native/bridge commands, isolation, lifecycle, stale turn rejection and protocol).
- Existing U01 BridgeSession tests and exact fixture comparison passed.
- MapProjectionTest: 119,600 old-query comparisons passed, including detached values and unchanged complete save.
- GridLayoutTest: 32 shared coordinate assertions passed.
- Actual platform-free C# Contracts/Client/Fixture tests: 37 assertions passed; this is not Unity Editor/Player compilation.
- Existing UI model, 3D foundation, 3D interaction, map editor, custom-officer and siege scripts passed.
- `:game-api:check :game-runtime:check :app:assembleDebug :app:assembleDebugAndroidTest` succeeded; 82 Gradle tasks executed.
- Android API 29 x86_64 emulator: APK installation and `UnityBridgeInstrumentation` passed. The actual test exercises native command entry, bridge commands, duplicate/error handling, complete-state equality, Activity recreation retaining the same session, native renderer switching preserving state and stale requests after world replacement.

After retrieving the Git bundle into a fresh local workspace, `test-architecture-baseline.sh` and `test-architecture.sh` were independently rerun and passed. `test-core.sh` was also rerun and failed at `CoreTest.logistics`: `AI uses deployment commands`. No assertion was removed or relaxed.

## Explicitly not complete / not proven

- This is a partial acceptance, not an all-green game release. `CORE_BASELINE_MATRIX.md` records 12 pass / 30 fail on both old and refactored trees for the original 42-invocation matrix. The follow-up rerun independently confirms the first core failure, not a new execution of all 42 entries.
- The final CI `native-screen.png` was visually inspected and shows a background-only screen, not a usable game-map screenshot. It is NOT accepted as visual/cold-start success evidence. The saved logcat was captured before the separate final launch, so it cannot establish why that screenshot is blank. Protocol/lifecycle instrumentation passing does not resolve this visual acceptance gap.
- Full manual new-game/save/load/next-turn/movement/attack/editor interaction and sustained Filament rendering/performance are not established by the limited instrumentation result.
- Current local checks found no Unity executable/Editor, adb or device; the CI capability log contains no Unity Editor discovery. Unity Editor compilation, Android export, Unity Player JNI/ARM64/device runtime and PC-art comparison remain unverified. No U00/U01 completion is claimed.
- Existing 3D combat host-test execution was recorded as blocked by missing JSON_TEST_JAR; it is not counted as passed.
- Legacy native forms, authoring tools and some assembly/presentation adapters retain the explicit direct-core whitelist. Only recruit/patrol are fully typed migrated commands; other native operations use the compatibility transaction boundary. World and its nested entities remain compatibility models.
- SaveCodec is unchanged: current format 33 and the pre-existing reader compatibility/rejections are retained, not broadened. Copy/validate-before-commit allocation cost has not been benchmarked on ARM64 hardware.

Keep PR #66 Draft until remaining acceptance decisions are made. Do not treat this architecture delivery as a Unity visual-stage completion.

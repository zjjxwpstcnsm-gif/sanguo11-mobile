# U00 toolchain and integration contract

## Pinned target (verified against official compatibility tables)

| Component | Version / location | Evidence or gate |
|---|---|---|
| Unity Editor | 6000.3.16f1, Unity 6.3 LTS | [Unity release](https://unity.com/releases/editor/whats-new/6000.3.16f1); must verify actual installed executable/authorization |
| URP package | `com.unity.render-pipelines.universal` 17.3.0 | [package documentation](https://docs.unity3d.com/Packages/com.unity.render-pipelines.universal@17.3/changelog/CHANGELOG.html); pinned manifest, rendering not yet validated |
| Android build | Unity Android Build Support including IL2CPP, SDK, NDK, OpenJDK | [Unity 6.3 dependencies](https://docs.unity3d.com/6000.3/Documentation/Manual/android-supported-dependency-versions.html) |
| NDK / JDK | r27c (27.2.12479018) / 17 | Unity 6.3 dependency table; prefer Editor-installed tools |
| Unity SDK tools | Build Tools 36.0.0, command-line tools 16, platform-tools 36.0.0 | Unity 6.3 dependency table; host compiles with Android platform 35 |
| Host Gradle / AGP | 8.13 / 8.10.0 | [Unity 6.3 Gradle compatibility](https://docs.unity3d.com/6000.3/Documentation/Manual/android-gradle-version-compatibility.html) for 6000.3.1f1–.16f1; [Gradle ZIP checksum](https://gradle.org/release-checksums/) |

Current machine has only a Java 17 runtime; no authorized Unity runner was found in the existing workflow definitions. `.github/workflows/unity-u00.yml` is deliberately manual and requires a provisioned `unity-android` runner plus `UNITY_EDITOR_PATH`. It has **not** been run. The existing native CI uses hosted Android SDK and does not grant a Unity license by itself.

## Clean checkout build

1. Supply a licensed Editor 6000.3.16f1 with Android Build Support and SDK, set `UNITY_EDITOR` and `ANDROID_HOME`; execute `bash scripts/build-unity-u00.sh`.
2. The script exports a single ARM64 `unityLibrary` from `unity/` via `ExportAndroid.Run`, fails if Player binaries are missing, and invokes the existing `:app:assembleDebug` with `-PunityExport=... -PtargetAbi=arm64-v8a`. Plain `./gradlew :app:assembleDebug` remains the native build path.
3. Output inspection requires `lib/arm64-v8a/libunity.so` and `assets/bin/Data/`; it reports compressed APK bytes and refuses a missing/empty APK. Capture signer, `aapt` package identity, source SHA, boot PSS, 10 launch/return cycles and video on a named ARM64 device. Check Unity/Filament native library overlap and merged Manifest on the actual export.

The host opens `UnityPlayerActivity` only by explicit menu action when the module is integrated. The host passes its current World scenario name and source map dimensions through Intent extras, without copying or mutating gameplay state. It leaves Filament before starting the full-screen Unity activity. Unity 6.3 officially limits the embedded Runtime to a full-screen single instance and warns that unloading retains memory: [Unity as a Library](https://docs.unity3d.com/6000.3/Documentation/Manual/UnityasaLibrary-Android.html). The return button calls `Application.Unload`; lifecycle and 10-cycle behavior remain unverified until a real build. Default map remains native.

No authorization file, SDK cache, exported player, or signing secret belongs in git.

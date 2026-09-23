# U00 defects and blockers

| ID | Status | Repro / required evidence |
|---|---|---|
| U00-01 | BLOCKED | Unity 6000.3.16f1 Editor, Android Build Support and usable build authorization not found; export, C# compile, merge and Player test cannot run here. Provision licensed runner and execute `scripts/build-unity-u00.sh`. |
| U00-02 | BLOCKED | No connected ARM64 Android device or `adb`; install, 10 return cycles, gameplay continuity, launch PSS and screen recording need device run. |
| U00-03 | PARTIAL | Current local environment cannot download Gradle 8.13 and has no Android SDK / `javac` executable. Hosted PR CI successfully built and linted **native-only** Android on source `a59c1c4`; its emulator follow-up was pending. Existing main CI emulator flow already fails a scenario text assertion. Pure-Java UI suite passed with a temporary `java -m jdk.compiler/...` shim. |
| U00-04 | OPEN | Unity exported module Manifest, exported Gradle DSL, URP import and UnityActivity `Application.Unload` callback require a licensed 6.3.16f1 build before being treated as compatible. If merge differs, fix source and re-export; do not report a successful Unity APK from static inspection. |
| U00-05 | OPEN (inherited) | `scripts/test-core.sh` fails `CoreTest.logistics`: `AI uses deployment commands`; `core/` has no U00 edits. Investigate independently from Unity export. |
| U01-01 | BLOCKED | U00-01 persists: Unity Editor 6000.3.16f1, licensed Android Build Support and authorized runner absent locally; cannot compile C#, export Player or build a U01 Unity APK. `bash scripts/build-unity-u01.sh` must produce validated APK from source commit. |
| U01-02 | BLOCKED | U00-02 persists: no ARM64 device / adb here. C#→JNI command roundtrip, 10 enter/return cycles, installed APK recording, frame/PSS metrics and leak probe have no runtime evidence. Native Android emulator host probe is separately labeled and cannot substitute for these. |
| U01-03 | OPEN | Android host CI and instrumentation may expose compile or lifecycle defects. Track run on source commit `faaa0e40` and resolve before treating the Android side as verified. |
| U01-04 | OPEN | Current U01 snapshot covers sites and units plus terrain; other presentation entities, broader command routing, native event playback and a complete map HUD remain for subsequent stages. Editor real-scenario fixture is read-only. |

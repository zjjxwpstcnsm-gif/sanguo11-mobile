# U00 defects and blockers

| ID | Status | Repro / required evidence |
|---|---|---|
| U00-01 | BLOCKED | Unity 6000.3.16f1 Editor, Android Build Support and usable build authorization not found; export, C# compile, merge and Player test cannot run here. Provision licensed runner and execute `scripts/build-unity-u00.sh`. |
| U00-02 | BLOCKED | No connected ARM64 Android device or `adb`; install, 10 return cycles, gameplay continuity, launch PSS and screen recording need device run. |
| U00-03 | PARTIAL | Current local environment cannot download Gradle 8.13 and has no Android SDK / `javac` executable. Hosted PR CI successfully built and linted **native-only** Android on source `a59c1c4`; its emulator follow-up was pending. Existing main CI emulator flow already fails a scenario text assertion. Pure-Java UI suite passed with a temporary `java -m jdk.compiler/...` shim. |
| U00-04 | OPEN | Unity exported module Manifest, exported Gradle DSL, URP import and UnityActivity `Application.Unload` callback require a licensed 6.3.16f1 build before being treated as compatible. If merge differs, fix source and re-export; do not report a successful Unity APK from static inspection. |
| U00-05 | OPEN (inherited) | `scripts/test-core.sh` fails `CoreTest.logistics`: `AI uses deployment commands`; `core/` has no U00 edits. Investigate independently from Unity export. |

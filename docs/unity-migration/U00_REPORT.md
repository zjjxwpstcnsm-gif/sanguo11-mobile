# U00 report — BLOCKED

- **Source base:** `main` `73249f12f269effece33816884364f8dbe1714e7`.
- **Source commit on remote:** `a59c1c430070de7dd5eb01b14bb4f0cdb928f702`, with an evidence-only documentation commit following it in [Draft PR #65](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/65). No Unity-bearing APK was built from this source SHA.
- **Branch:** `agent/unity-pc-visual`; `main` untouched.
- **Implemented:** Unity project with pinned package and text/meta settings; batch Android export; conditional Gradle module and Manifest integration; host menu trial, true scenario/map dimensions and native Filament release; read-only diagnostic Unity screen with return action; artifact validation and an opt-in CI job. First remote CI exposed a native Lint `MissingClass` for the Unity activity; the source commit isolates that Manifest declaration to the Unity-enabled build.
- **Not implemented/verified:** Unity Player build and Android module merge; APK byte size/hash/signature for Unity variant; ARM64 install/10 cycles; boot recording and memory measurements; gameplay start/load regression on a Unity-bearing APK. No Unity APK link exists.
- **Java checks:** full `scripts/test-ui-models.sh` passed after a temporary shim invoked the JDK compiler module (counts in `BASELINE.md`); `scripts/test-core.sh` fails an inherited `CoreTest.logistics` assertion. `git diff --check` passed. Unity export preflight correctly returned exit code 2 when Editor missing; artifact verifier rejects a nonexistent export. Android and Unity build tests remain blocked.
- **Signing and identity target:** `game.sanguo.mobile.dev`, versionCode 81, public development-only signer documented in `BASELINE.md`; actual U00 APK identity and signature unverified.
- **Visual comparison:** U00 diagnostic only; no PC art or map parity claim.
- **U01 entry gate:** complete U00-01/U00-02, build from clean checkout and prove ten Unity enter/return cycles plus native game start/load on a named ARM64 device; record native and Unity APK/PSS baselines.

# R16 evidence / reproduction

The APK source is 2626153bbe54a44812280b52a00b18e8202076f8. Reports after it are evidence only.
Do not confuse host tests, x86_64 software rendering, ARM64 packaging, or physical-device acceptance.

- `conditions.json`: frozen acceptance budgets and known device conditions.
- `apk-identities.json`, `package-comparison.json`: exact delivered binaries; debug development certificate.
- `final-ci-*.log`: final-source CI host checks; core.exit remains 1.
- `reproduce-visibility.py`: extracts actual before/after production methods; GPU replaced with mocks.
- `compare-thermal.sh`: actual before/after thermal policy with simulated input; no physical temperatures.
- `analyze-runtime.py <extracted-runtime-root>`: loading/route samples and raw frame rings; never presented FPS.
- `delivery.json`: final build/runtime artifact IDs, sources and outcome matrix.

Commands used by workflow (the build and paired test APK must come from the same build artifact):

```sh
adb install -r out/r16/candidate/app.apk
adb install -r out/r16/candidate/test.apk
adb shell am instrument -w -e mode local game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR16Instrumentation
adb shell am instrument -w -e mode national game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeR16Instrumentation
```

Instrumentation launches `game.sanguo.mobile.MainActivity` in application ID `game.sanguo.mobile.dev`,
loads `coalition-190` at faction 5 via its existing entry, switches normal MapHost to native 3D,
then exercises camera span 8/14/30 and yaw 0/90 if initial readiness passes. It does not claim pure-touch input.
`mode=soak` is implemented but **NOT_RUN on physical devices this round**; it requires successful readiness
before the 30-minute clock and performs 20 view/home/load cycles. Full editor and dense-effects coverage is absent.

External `R16-evidence.zip` contains original runtime ZIPs with Surface/screen captures, screenrecord,
logcat, device/driver/power snapshots and Perfetto traces, plus build logs and these documents.
Initial and final runs are both retained. The 150-second traces and <=180-second recordings do not
necessarily span the whole route. Missing marker strings/semantic presentation analysis is not evidence
of zero GPU cost. Video null decoding may warn about nonmonotonic DTS; diagnostics are retained.
CI artifacts expire 2026-10-28; the delivered evidence ZIP preserves raw runtime bytes separately.

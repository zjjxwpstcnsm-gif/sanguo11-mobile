# Unity visual migration progress

| Stage | State | Source | Evidence / outstanding gate |
|---|---|---|---|
| U00 | BLOCKED | Draft PR #65, source commit `a59c1c430070de7dd5eb01b14bb4f0cdb928f702` plus report follow-up | Unity project and host trial source pushed; Unity Editor/Android support/authorized runner and ARM64 device absent; no Unity APK or footage |
| U01 | BLOCKED | Draft PR #65, bridge source tree `fbcea99b89d420aeffe1c82cf93e21a4a5053be5` at remote source commit `faaa0e40ae6b1ab1c1fc8f26d6d1c32745b4a33c` | Java parity and native Android host build from exact branch HEAD passed; diagnostic APK verified without Unity. Unity Player compile, Unity APK, real JNI/device roundtrip and footage blocked by U00-01/U00-02. See `U01_REPORT.md`. |
| U02–U22 | NOT_STARTED | — | Do not treat native S01–S13 as Unity stage completion; first close U00 and U01 hardware gates. |

Five axes for U00: function **PARTIAL** (source only); visual **NOT_STARTED** (diagnostic scene); performance **BLOCKED** (no Player APK/device); device **BLOCKED** (no ARM64 test); push **COMPLETE** (PR #65, subject to final remote HEAD check). See `U00_REPORT.md` and `DEFECTS.md`.

Five axes for U01: function **PARTIAL** (Java parity and bridge source verified; no Unity Player execution); visual **NOT_STARTED** (temporary diagnostic map); performance **BLOCKED** (Java host time counters only, no GPU/frame/PSS measurements); device **BLOCKED** (ARM64 absent); push **COMPLETE** for source commit, documentation follow-up subject to final remote check. Overall **BLOCKED**.

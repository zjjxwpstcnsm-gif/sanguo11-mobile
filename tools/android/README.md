# Development APK identity

`dev-debug.keystore` is an intentionally public, **development-only** Android signing key.
Store password and key password: `android`; alias: `androiddebugkey`.
It signs `game.sanguo.mobile.dev` so development builds can upgrade one another across clean CI runners.
Do not use it for production or a privileged signature-based trust boundary.

Certificate SHA256: `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`.
CI verifies this fingerprint and no longer downloads expired historical APK artifacts to complete a build.

The new application ID preserves the old installed `game.sanguo.mobile` package and its private saves.
Use the game's document import/export commands to move save files; neither app can silently read the other's private files.

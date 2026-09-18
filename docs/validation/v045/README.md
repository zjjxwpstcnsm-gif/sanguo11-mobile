# v0.45 validation

Baseline: `c4bc410223365cd083d6d6196b0303255b740f1c`.

Local: Java 17 target compilation; `ObjectiveOrdersTest` 94 checks PASS; 18 map/art source checksums PASS; Huguan local crossing blocked for hostile and open for friendly, both with empty and occupied parcels.

Build: Actions artifact includes `BUILD_COMMIT`, `SOURCE_CHECKSUMS.json`, `output-metadata.json`, `SIGNATURE.txt`, `SHA256SUMS`, `geography.json`, `objective-checks.txt`, and the APK. These files, not the workbench workflow SHA, identify the actual runtime source.

Not run: full historical test suite, lint, emulator, ARM device or end-to-end UI acceptance. New map geometry requires a new game; legacy maps are intentionally retained on load.

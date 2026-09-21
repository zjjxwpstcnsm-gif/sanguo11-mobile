# Main integration and rebuild — 2026-09-21

User request: merge the existing remote work and pending PR into main, defer generated-art improvements, and deliver a newly built standalone APK.

## Integrated commits

- PR #51: `agent/void-road-generated-art-v062`, head `ca40692347dc785a6fa477d962780fe3e40ce1a2`, merged as `a99c496b086a88a30ce517309ec93ad6631b3e7e`.
- PR #52: `agent/generated-art-city-movement-ports-v063`, head `0563e5f7bf03d392f34adc1d6d99c042697749f6`, merged as `2c6b2ccb834001f7204f6f0c253259165e8db1bd`.
- PR #51 contains installation/publication workflow and v062 documentation. The v063 branch contains eight workflow files, not gameplay or map modifications.

## Corrected handoff status

The provided `sanguo11-v063-INCOMPLETE-source-checkpoint.zip` contains an empty `source.patch`; its STATUS.json states `source_file_count: 0`, `files: []`, and an empty local base commit. The only substantive preserved payload is `data/map/reference-v063/revision.json`, a proposed revision record, not the changed production files.

The prior chat response claimed local city-movement, port and VOID changes and passing tests. Those production changes and test logs are not present in the provided checkpoint or the compared remote v063 branch. They must NOT be advertised as part of this rebuild. No proposed map changes are blindly applied from that JSON.

## Actual runtime scope

- Preserve the already delivered ROAD-as-PLAIN visual change and the existing native200 game.
- Generated-art work is deferred by the user. Existing assetRevision 56 remains active.
- Retain the committed application version `0.62.0-road-candidate` / versionCode 62 instead of presenting a tooling-only merge as completed v0.63 gameplay.
- Geography remains mapRevision 61; SaveCodec remains 31.
- The six-port corrections, city departure billing repair, and remaining 14 VOID cells are NOT newly fixed by this integration.
- No old saves are rewritten or deleted by this integration.

## Build and validation provenance

The `Consolidated main standalone APK` workflow builds its exact checked-out main commit, verifies merged ancestry, resource hashes and the existing development signing certificate, and publishes an independently downloadable APK. It records the actual BUILD_COMMIT, version, APK hash/size, Android execution outcome and release re-download verification as release/workflow artifacts. Success must be determined from the actual run; this document does not predeclare any build or installation result.

The exact installed baseline and newly built APK are checked separately. Emulator testing is not physical ARM-device testing. No full v063 acceptance or generated-art completion is claimed.

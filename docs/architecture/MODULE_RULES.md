# Module rules and migration fence

## Executable checks

Run `bash scripts/test-architecture.sh`. It compiles game-api and core on independent classpaths, compiles actual runtime/tests, checks explicit architectural fences, verifies saved-state baseline hashes (including RNG), exercises session and bridge behavior, compares every map-layer query and runs shared coordinate examples. Existing `scripts/test-unity-u01.sh` still performs exact fixture comparison. `:game-runtime:test` and `:game-runtime:check` execute the custom main-style session/bridge/grid tests; they are not assumed to be JUnit discovery.

Run `bash scripts/test-architecture-baseline.sh` with baseline Git history available to independently recompile the exact old source and verify the seven saved-state hashes. It never overwrites the checked-in golden.

Run `bash scripts/test-client-contracts.sh` with .NET SDK 8: it compiles the **actual** platform-free C# Contracts/Client/Fixture sources and verifies long counters, atomic display sync, failed receipts, gaps, deletion, resync and fixture read-only behavior. This does **not** compile UnityEngine presentation/JNI or validate a Unity Player. Editor tests live in the Editor-only Tests assembly and require a real Unity environment.

`check-architecture.py` additionally checks the exact direct-core file allowlist, no World in Filament, no Activity-dependent JNI business requests, JNI calls confined to Transport/Android, pure Contracts/Client assemblies, assembly DAG, small composition bootstrap and preserved original .meta contents. These checks supplement, not replace, compilation and behavioral tests.

## Existing direct-core whitelist

`LEGACY_CORE_ALLOWLIST.json` enumerates actual production paths (not package wildcards). The whole app still needs a compile dependency on core during this migration. Adding a file that imports core fails the fence until explicitly reviewed.

The remaining clusters are deliberate:

- NativeGameHost/MainActivity/TurnWork/TurnPlayback: assembly, detached native view and existing journal playback. Only GameSession owns authority; all commands and replacement/capture go through its boundaries.
- Existing *Ui classes and related pickers: rule previews and authoring of lazy LegacyCommandSink operations. Recruit/patrol use the typed API already; other commands (movement, attacks, diplomacy, governance, research, transport and facilities) use validated legacy transactions. Do not call a rule eagerly then pass a Result.
- MapHost/MapProjectionQuery, MapView, MapSceneSnapshot and native geometry/model helpers: mapping from a detached view and existing pure rule values. FilamentMapView no longer imports or queries World. This was verified by actual query parity, not inferred merely from old method signatures.
- MapEditorActivity/MapLibrary and custom-officer/editor/import helpers: isolated **authoring** worlds and existing library serialization. They are not independent active campaigns. Play/new/load installs through GameSession. Their existing core tests and Android compile paths remain.

Do not extend the whitelist by a directory wildcard. Do not expose session.world(), accept Activity in runtime, add Unity rules, move rendering fields into SaveCodec, or silently version the schema. World nested entities and package-private rules are not made public en masse; extracting them requires separate compatibility tests.

## Old -> new

- core BridgeSession -> game-runtime/bridge/BridgeSession.
- nested bridge Entity/Message -> game-api/bridge/BridgeEntity and BridgeMessage.
- Activity authority -> GameApplication -> NativeGameHost -> GameSession.
- Activity raw save encoding/storage -> SessionSaves + AndroidSaveStore.
- MainActivity-dependent JNI -> stable UnityBridge facade -> AndroidGameBridge -> GameApi.
- TerrainCode/SourceGridCoord/MapBrushGeometry -> core/map; MarchScale -> core/army.
- Filament editor/territory rule queries -> MapProjectionQuery -> detached MapLayerData.
- TrialEntry protocol/JNI/state/sync/diagnostic logic -> Contracts, Transport, Client, Features/UI; TrialEntry stays Bootstrap composition.
- Assets/Scripts.meta -> Sanguo/Bootstrap.meta; existing TrialEntry and ExportAndroid .meta files moved byte-for-byte, not regenerated.

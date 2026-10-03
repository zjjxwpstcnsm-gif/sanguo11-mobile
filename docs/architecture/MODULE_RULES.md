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

### R11 reviewed transient presentation boundary
CombatSequence and CombatReplayLedger consume only immutable TurnJournal.Event
facts (legacy projection payload) in app. Their additional allowlist entries are
paired with a static import/authority-call guard in check-architecture.py. They
never accept World, session, RNG or save APIs. Gameplay ownership remains
GameSession; no game-api/game-runtime dependency or authority change is introduced.

### v125 current-contest command boundary

`ContestCommand` contains only the five operations already called by ContestUi: duel move, debate card/rethink/finish and concede. GameSession validates the normal state token plus current contest ID/revision, copies authority, runs the existing core operation and installs only a valid success. Ordinary typed/legacy commands and turn tickets remain blocked during contests. No World or callback enters game-api, no save schema changes, and no whitelist entry is widened. `scripts/test-feedback125.sh` additionally runs full-save/RNG comparisons and stale/type/turn rejection checks through this real boundary.

### v139 reviewed PC source visual boundary

The exact new paths PcScenery, PcSites and PcCliffWalls consume only Hex plus detached Ground/source geometry; PcConstructibleWalls consumes SourceGridCoord. PcUnitFormation reads the War.Status enum already copied into UnitVisual. PcUnits reads Hex, the status/tactic enum and immutable TurnJournal.Event/Strike; PcFacilityRigs reads only the immutable journal. None accepts World, a session, SaveCodec, strategy or gameplay RNG. Source rig readers allocate their own arrays and meshes; journal queries only select source assets and presentation frames. The allowlist names these seven files individually, with a new exact-import and no-authority-symbol fence for the cohort; there is no directory wildcard.

CombatSequence retains its default timing constructor for legacy/custom maps. The optional duration function is presentation-only; MapHost/TurnPlayback choose the source platform clock on the source map. Source timing never enters core saves or turn computation. Independent raw-PC pose checks, all unit normal-command save comparisons and two complete platform next-turn comparisons prove the tested boundaries; full PC pixels/timing and installed platform verification remain separate gates. Existing session, snapshot, JNI and baseline tests remain required, without changed golden hashes.

### v159 original critical presentation facts

PcPresentationPlan is individually allowed to import only TurnJournal and War
(the existing plot enum). It selects source template/texture identifiers from
immutable tactic critical and PlotOutcome facts; it cannot access World, a
session, save APIs or gameplay RNG. PlotOutcome records the already computed
success/critical/cause for normal, reflected and chained plots. The recorder
never repeats a rule check or consumes randomness. The source conversion uses
an isolated visual VM and its own fixed visual random initialization.

The current750ms cue duration follows the user's requested Android timing
(accepted range500–1000ms). It does not certify PC wall-clock timing. The
inherited source now binds six canonical tactic portraits with age variants,
SORCERY and LIGHTNING; CONFUSE has no source fullscreen binding. Installed
validation of the newly inherited age cases remains separate from old APK results.
The source packet player preserves original layer order and source pixels;
source camera, face variants and encoded-color blend parity remain separate
acceptance requirements. Full-save comparisons cover the observed rule boundary.

The inherited PcDams and SceneCamera references are also now individually
reviewed: they use only the numeric PcMap.SCALE and PcMap.MAX_HEIGHT constants.
Exact qualified-reference guards reject broader core references. Neither
class accepts or calls gameplay authority. Source screen cue clocks use actual
elapsed visual time; the existing50ms action cap remains for subsequent actions
and for legacy/custom maps. Resource/shader preparation finishes before this
screen clock starts, and the independent background visual VM is suspended
while the screen cue plays. These changes do not affect rules or saves.

PcPresentationStage owns a separate visual Scene/View, one captured real map
RGBA texture, a 1x1 shader warmup target and their lifecycle. PixelCopy and
texture delivery precede the 750ms source clock. It imports no core authority
and freezes only the underlying visual pose for the screen prelude. Original
source vertices/material order remain intact; contiguous equal-state quads
share draws. Public Engine Fence proves driver processing, never GPU completion.
Installed phase/cadence evidence and real video remain required.

The current stage composes the captured map and original source quads in
encoded RGBA8 without a second HDR/LUT pass. When the swap chain performs
sRGB encoding, a separate final quad decodes that completed image once.
This prevents double processing of the captured map; original PC fullscreen
color-state/lens acceptance remains separate. The optional capture observer
is null in normal play and exists only for installed bitmap comparison.

### UI11 cargo form extraction review

`CargoWizard.java` extracts the existing DomesticUi cargo draft and confirmation into a dedicated native form. Its only rule inspection uses the existing detached World and `Domestic.transportError`, `shipCargoError`, and `transportPreview`; it never installs, copies, serializes or mutates authority. The sole transport call is a lazy supplier inside `MainActivity.applyResult(expected, ...)`, entered only from the existing revision-checked `commandDialog`. Draft crew IDs, quantities and UI page remain Bundle intent, not a second campaign. The inherited UI ration and hard-coded transport-capacity calculations are removed. Full-state/RNG, rejected-preview, cancellation and real double-tap dispatch checks cover this extraction. Only this exact source path is added to the existing legacy consumer list; no module or wildcard boundary is broadened. A structured transport DTO remains requested before replacing this compatibility inspection.

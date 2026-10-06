# B contracts and A adaptation requests

Ownership uses ../parallel-repair/CONTRACT.md; historical 20261002 A/B role names do not override current ownership. Implemented B admission/fee APIs are listed below; additional detached scene facts remain pending.

Existing authoritative facts: GameSession state/sessionId/generation/revision; LegacyCommandSink lazy checked transaction; source officer metadata/nativeId/sourceVariant and current ability DTO; existing TurnJournal fire/facility/action facts. Rendering must not execute rules or consume either RNG.

Pending exact A adaptations will be listed here with consumer path and same-state example after B interface is implemented and tested. MainActivity, MapSceneSnapshot, MapHost, faction picker, theme/media and frozen bridge are read-only for B.

## Installed B probe runner

The new exact B-owned instrumentation class is selected only through session-b/instrumentation.init.gradle at test build invocation. Existing shared root/Android Gradle and AndroidTest manifest stay byte-identical. The init script changes only testInstrumentationRunner, not production APK input or JNI. Final integration can register the runner in the A-owned test manifest sequentially if desired; it is not a production menu dependency.

## Fieldwork admission diagnostics (B implementation candidate)

`Fieldworks.buildCheck(unitId,kind,target,direction)` returns immutable Validation `{code,field,detail,allowed()}`; buildError/sites/build share this single pure rule check. Same coordinates remain axial Hex; user display conversion only uses MapCoordinates.display/nationalSource. No rule distance/terrain/gold/tech relaxation, no saved schema or RNG changes.

Stable codes: NONE, UNIT_COMMAND, BUILD_TECHNOLOGY, BUILD_DIRECTION, UNIT_BUILDING, UNIT_GOLD, TARGET_ADJACENCY, TARGET_OCCUPIED_OR_FIRE, TERRAIN_NON_NAVIGABLE, TERRAIN_FOREST_OR_SWAMP, BUILD_TERRAIN, SITE_DISTANCE, MILITARY_DISTANCE, STRUCTURE_LIMIT. detail is display text, never parse it as a protocol.

`withdrawCheck(unitId,cityId,gold)`, `withdrawMaximum` and `fundingSites` share actual canEnterSite admission; funding sites include seven-cell city positions. Stable codes: UNIT_COMMAND, FUND_SITE_OWNER, FUND_SITE_ENTRY, FUND_AMOUNT, UNIT_GOLD_CAPACITY, SITE_GOLD. Same command remains lazy LegacyCommandSink/GameSession. Successful construction/funding preserves previous debit/action/save semantics. Example: source14/LiuBei/Yongan/unit1 carries3000 at202,46: CAMP target202,45 rejected SITE_DISTANCE; after ordinary move202,45 target202,44 allows NONE and1500 debit/action/incomplete structure.

FieldworkUi candidate consumes A existing commandDialog/trackDialog theme+stale/double boundaries. No MainActivity/MapHost/MapSceneSnapshot/UiTheme change needed. A map error callback receives this exact core code/detail and actual chosen coordinate. Source execution6x81 proves original city footprint-distance<=2 guard; it remains intact.

A theme contract received: completed source reference5431de8d at2191/session-a/THEME_CONTRACT.md. New readable/readableTree require A frozenUiTheme plus FactionColors dependency. B does not copy A WIP; current dialogs use common existing UiTheme.dialog via trackDialog and commandDialog. Theme-specific final adaptation is pending frozen A increment.

## A exact movement adapter request (delivered frozen47326188; A-owned file unedited by B)

MainActivity.java:778 previewMarch currently infers a friendly city target as `world.marches.previewCity(unit.id,c.id)` even when the player selected ordinary "行军". For the ordinary march command, use existing pure `world.marches.previewMove(unit.id,target)` and keep the actual chosen axial target. Reserve previewCity/GARRISON for the explicit existing "进驻" action (ArmyUi.garrison/previewGarrison already provides it); preserve explicit auto-attack/approach pathways and routeMove restoration flag. B core candidate now permits normal tile movement to stop on friendly seven-cell positions, while explicit/saved-legacy CITY garrison still docks. Please apply only in A's branch and deliver a frozen increment with exact SHA. B will not edit this A path or copy WIP.

UI evidence target: Source14/LiuBei/Yongan ordinary deployment3000 gold -> move onto lawful city cell -> "补充携金" lists it -> cancel is pure -> confirmed transfer charges city/u and action once -> nextturn construction -> saved resume. The current inherited UI automatically calls garrison, so the city-center branch cannot be declared closed until this adapter is installed.

## Confirmed category distance correction

Original spatial probe expanded: six centers×81 cells×9 native kinds; original camp3/category1 rejects footprintDistance<=2, native walls8/9 category2 and traps16–19/21/22 category3 allow empty city-near cells. Original5a4170 checks separate0x100000 versus0x200000 bits; complete416690→4848f0→4847a0 only clears category1 radius around all seven city cells. Candidate Fieldworks now applies the siteDistance<=2 guard only to existing military(kind), retaining every other admission requirement and seven-cell occupancy. Normal Source14/Yongan wall/trap formerly had SITE_DISTANCE on legal free tiles. This is finite original spatial evidence, not whole construction cost/progress/AI restore.

Foreign/neutral/ally province admission of original5a3460 is not closed by the null-unit spatial fixture. Candidate distance correction therefore only permits city-near walls/traps at own sites; existing distance bounds at non-owned sites stay pending source verification. This does not claim all placement legality or original diplomacy parity.

## Fresh-source military base fee API (candidate)

Fieldworks.baseBuildCost(kind) returns the saved base fee; buildCost(unitId,kind,targetHex) is the authoritative current base-only quote consumed by buildCheck/build and B confirmation. Fresh explicit PC-source new games save pc-military-base-fees-v1 with full sourceId/sourceVariant/sourceSha/SharedSha/EXESha and18 verified native mappings. CAMP/FORT/FORTRESS now500 in those new games. Old31–39 worlds lacking it and authored engineering worlds retain legacy1500 without decode backfill/refund/HP/RNG or capability changes. Scalar base query is pure; militaryHQ80percent/target-region discount is explicitly unimplemented. A must use this API if showing fees, never infer them from StructureKind.gold. No A path adaptation found necessary outside B FieldworkUi; all app fee references audited. Future discount rules must use the target-aware API and same StateToken.

## Existing immutable officer contract and remaining scene facts

GameApi.officers -> GameSession.officers -> OfficerQuery.capture(authority,state) returns immutable OfficerSnapshot on the serial logic thread. Join Officer.id with SourceInfo nativeId/sourceVariant/sourcePath/sourceSha/recordSha; canonicalOfficerId is nullable. The four verified original-glyph identities have proven canonical links; never derive runtimeId from nativeId arithmetic. Current/base/growth/experience/aptitudes, presence, owner, city, unit, role, merit, office and commandLimit come from the current save. Older missing source/ability policy stays unknown. Metadata and media manifests remain separate ID-linked objects. Rendering must not run commands or draw either rule RNG.

Existing GameSnapshot projects terrain/layout/sites/units, but does not yet expose complete fire lifetimes, construction/destruction, date and region/relationship facts. Those fields have not been delivered through a new API. GameEvent.id uses session/generation/revision/kind; TurnJournal.Event.id uses journalId/sequence. Do not conflate these identities, invent parents, or parse human messages to derive events. Structured speaker/original voice profile and full fire/facility facts still require B DTOs and exact examples before A adaptation. The raw voice number is currently human original-information text; A should not parse that prose to choose audio. MainActivity/MapSceneSnapshot/MapHost and bridge serialization remain untouched by B.

Frozen A47326188 six-file dependency package is staged read-only: five theme files and one completed ordinary-MOVE adapter. All16 B pages compile against its UiTheme.dialog/readable interface. No A WIP was copied.

## Pending A original budget editor adapter (APK32 still in acceptance)

A-owned EditorUi.java:76: existing faction-editor label0–60 becomes“第一军团行动力（0—255）”only when PcArmyActionPolicy.enabled(w); oldabsence retains0–60. Existingw.actionPoints[side] is first-army projection in newpolicy; coreEditor.faction edits uniqueoriginalfirstarmy and rejectsambiguity. No B edit ofApath. Additive detached SceneFactsSnapshot.NativeArmy.originalValid/actionPoints (−1absence/unknown) retainold constructors; do not fabricate60/enableinvalidrawowner0. RequestsenttoA withoutWIPintegrationauthorization; onlycompletedfrozenBdelta afteractualAPK/restore.

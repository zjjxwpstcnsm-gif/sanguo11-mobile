# Architecture foundation (independent of Unity U00/U01 visual stages)

Baseline: Unity branch `9548bb350051150b21a61213f9068ffb1b7506c0`; stacked PR #66 targets `agent/unity-pc-visual`. Main and PR #65 are not merged by this work.

## Production dependency direction

`app assembly -> game-runtime -> game-api + core`; `game-api -> JDK only`; `core -> JDK only`.

- **core** retains authoritative rule implementations, World entity model and v33 SaveCodec. Four low-risk production types move into `map` and `army`; package-private rule clusters remain beside World until a separately tested extraction is possible.
- **game-api** is used in production: commands, results, state tokens, events, query snapshots, bridge data and dimensionless grid layout. No World, Android, UnityEngine, Filament, surfaces or selected-tile UI data.
- **game-runtime** holds the only authoritative World in GameSession. SessionSaves coordinates captures/restores; SnapshotQuery projects facts; BridgeSession observes commits and implements schema-1 transport sequencing/dedup/deltas/backpressure.
- **app** owns platform UI, files, JNI and native rendering. GameApplication owns an instance-scoped NativeGameHost; it assembles the runtime and retains the turn job, not an Activity. AndroidGameBridge lives under `bridge`, AndroidSaveStore under `platform/save`, MapLayerData under `presentation`.
- **unity** owns a display replica and presentation. Bootstrap assembles Contracts, Client, Transport and Presentation assemblies. It does not implement game rules. Existing diagnostic visuals remain diagnostic visuals, not PC-quality 3D.

## Ownership, writes and lifecycle

GameSession is created and mutated on one logic thread (Android main thread today). Its private authority is never returned. LegacyView contains a **detached draft**, not authority. Typed commands copy, execute existing Java rules, validate and copy before committing. LegacyCommandSink carries a lazy operation plus its registered draft token; failed/throwing/stale operations cannot partially modify authority. Every successful write increments the central revision once and publishes an immutable event. Subscribers cannot reenter writes. Unsubscribing removes callbacks; Activity subscription uses a weak reference and is removed on destroy.

Native and Unity recruit/patrol both call GameSession.execute -> World.recruit/patrol -> existing rules. Chinese strings are presentation text; native consumers and tests use typed command/result/event fields. Schema-1 event text is retained for wire compatibility, not parsed into rules by C#.

New scenarios and load/import replace the current World through GameSession.replace. A new identity/generation invalidates old drafts, requests and turn tickets. Configuration recreation binds the same Application-owned session; switching Canvas/Filament or closing Unity does not create another game. Ordinary destruction only detaches views. Explicit exit closes the session. Process death is recovered via existing saves, not a permanent static World.

TurnWork computes from captured SaveCodec bytes on its worker. Before committing it validates the ticket/session/generation/revision. Completion commits and autosaves **before and independently of playback**. Pausing, skipping or detaching animations cannot choose a different rule outcome. The worker's computed World is not another writer to authority.

## Map and 3D boundary

SnapshotQuery produces game facts. MapSceneSnapshot stays in app: selection, terrain surface, visual geometry and native overlays are not game-api contracts. MapProjectionQuery runs on the host using a detached legacy view; Filament receives copied MapLayerData, blocked-cell keys and portrait Drawables rather than querying a World. Canvas is a documented legacy consumer of a detached view.

GridLayout defines q/r to dimensionless X/Z. Source-origin metadata is not added again as a world offset. Column-staggered layouts transpose the axes; source row staggering uses half-row offset. Java GridWorldTransform delegates to this contract. Java/C# share explicit CSV examples, not class binaries. These coordinates do not define movement cost, path validity or attack range. A future continuous terrain mesh need not create one model per logical cell.

## Compatibility

SaveCodec remains v33 with the same fields, IDs, random state, coordinates, custom content and existing reader policy. No object-graph Unity serialization. No art, map values, gameplay formula or version-number changes.

Schema remains 1. JNI facade stays `game.sanguo.mobile.UnityBridge`; C# hardcoded ABI is preserved. Terrain wire codes explicitly pin the original A-O mapping without reordering the enum. All long counters stay integer/Int64. Snapshot/delta/deletion, retries, sequence and version checks, queue limits and resync are maintained. Authority revisions advance even for changes invisible in exported entities.

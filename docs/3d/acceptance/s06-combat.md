# S06 event-driven combat — implementation complete, native/hardware gates pending

User override: same PR #62 and `agent/3d-s01-renderer-foundation`, no main merge.
Input main: `1a883a4ffd1098ca85f7da38464b7c5fbfdc000f`; starting branch:
`4e7d2920d5389623826870659c9ef5aec11a7b28`. Concurrent fixture compatibility
commit `1bd28d9` is retained. 2D remains default. Normal entry: game → view → 3D.

## Event coverage and authoritative boundary

| Event | Existing producer / payload | 3D behavior |
| --- | --- | --- |
| CHANGE | before/after entity keys, hex, owner, HP, troops, energy, status, duration; impacts | recorded status/destruction text, actual fire snapshots |
| MOVE | actor ID, owner, start, target, legal recorded route | existing walk/turn/ship transitions; no inferred route |
| ATTACK | actor/equipment; ordered **applied** physical strikes (source/target IDs, positions, owner, troop before/after) | prepare, melee/cavalry/arrow; counter, double hit and support follow recorded strike order |
| TACTIC | same delta plus existing tactic label and optional CriticalHit | arrows/stone/fire/melee, target reaction after hit; no damage/miss prediction |
| PLOT | existing plot label, start/target, deltas and impacts | fire, lightning, generic status/recovery; failed plots never create damage bursts |
| FACILITY_ATTACK | stable structure/site ID, type, owner, source/target | tower arrows/catapult arc, site arrows |
| FACILITY_COUNTER | stable structure/site ID and same exact deltas | camp/melee or site arrow response; never another counter command |
| RECOVER | actual music/skill producer, capped energy delta | recovery pulse plus exact gained energy |
| ENTER | actor copy plus removed unit and changed site | existing enter/scale clip then removal/updated site |
| DEPLOY | new entity delta | materialize only when applied; no invented transit |

All rows retain ordered producer events, batches and original battle reports. `StateChange`
contains detached primitive values. Existing Node images remain private and `applyVisual`
still updates only the render World. `CombatEffects.physical` only appends a presentation
record after the existing hit; RNG calls, payments and combat formulas are unchanged.
No renderer calls a command. Strike detail does not add business events or reports.
A target removed from the computed world remains in the historical render world until
its event is applied. Intermediate troop labels use recorded strike results.

Core currently exposes the whole strategic map: no separate fog/hidden-entity contract
exists. 3D uses the same event candidates as 2D, including impact-only events, then
viewport-culls effects. Off-screen events still apply all deltas and retain reports.
If fog-of-war is added later, filter both snapshot and event data at that shared boundary.

## Timeline, lifecycle and budget

For each physical strike: preparation 0–22%, flight/action 22–62%, hit 62%, text 68%,
settle/removal at event completion. Multi-hit events divide the same normalized event
fraction into recorded strike order (600 ms each, capped at 1800 ms total). No wall
clock advances paused hit effects. Existing speed 1/2/4, pause, skip and eight-second
presentation budget remain; critical duration also respects speed.

Critical highlight reuses `OfficerPortrait` and immutable `CriticalHit` in a small
92dp card; click or normal skip dismisses it. No full-screen flash, camera shake or
new audio (sound-event budget zero). Existing reduced-motion setting omits transient
combat; persistent fire remains static. The core fire remaining-turn value controls
existence; no animation creates, extends or spreads a logical fire.

48 reusable renderable slots; six shared opaque meshes; up to 16 visible persistent
fires; eight feedback labels and six affected-cell burst targets. No alpha particle
layers, no unbounded emitter allocation, no texture downloads or shader compiler.
Temporary entities are hidden on replay clear/background and destroyed on renderer
release; detaching TurnPlayback clears replay and critical state. All Filament resource
operations retain the existing main Looper ownership. Resource diagnostics include
effect primitives and known buffer bytes, not driver GPU-memory estimates.

## Verification

- `scripts/test-3d-combat.sh`: real legal attacks, cavalry, crossbow, catapult tactic,
  paid fire/lightning, actual seed/ball/ship trap detonations, critical, ordinary counter,
  defeated target, enemy attack, tower/camp/music and city capture/auto-entry.
  1/2/4/skip samples retain full SaveCodec bytes (including RNG/reports), event counts,
  detached final troop/status values, finite geometry and bounded pools.
- `scripts/test-3d-foundation.sh`: national terrain/site picking and lifecycle contracts,
  wounded-journal replay, unit-motion regressions.
- `scripts/test-3d-field.sh`: existing facility lifecycle and real port/march/garrison.
- Offline APK/test APK/lint; existing presentation/geometry/siege tests.
- `CombatSceneInstrumentation`: real native effect sequences/PixelCopy/screenshots,
  screen recording; actual TurnWork + TurnPlayback for 2D normal, 3D 4x, 3D pause/
  detach/rebind/background and skip, all checked against a separately computed turn.
  CI status and exact source/APK identity belong in the final delivery report.

No attached Android/KVM device locally. Native checks and video are **pending until CI
actually passes**. Physical arm64 midrange FPS/PSS/GPU/thermal/battery, installed size,
long sessions and artistic acceptance remain **UNMEASURED**, not passed. Do not claim
full S06 acceptance or enable 3D by default on that basis. Generic status effects remain
stylized, not individual cinematics; there is no additional rule for passive drum auras.

## Existing core save restriction discovered (not changed)

`CampaignSave.validate` currently caps fire remaining at 2 although critical fire can
create 3 (reproduced with SHENSUAN). S06 does not alter this gameplay/save rule. The
full-save gate for that existing critical-fire combination remains BLOCKED; ordinary
fire, all three trap types and the other listed cases use exact full-save hashes.
FIRE_SHIP causes its actual explosion but `Fieldworks.flame` deliberately does not
persist water fires; the renderer follows that behavior. A follow-up core/save repair
is required before claiming all critical-fire scenarios accepted.

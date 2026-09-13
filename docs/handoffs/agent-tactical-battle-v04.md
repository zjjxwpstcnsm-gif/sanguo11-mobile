# Agent 2 — Tactical battle v0.4 handoff

## Delivery and scope

Branch: `agent/tactical-battle-v04`
Base main: `f23d656a92781573226fbcf03aa92c79da46f2bf`
Commit message: `feat: add tactical battle engine`

This is an implemented, headless Java 17-compatible tactical engine, not a design-only deliverable. It is an independent package inside the existing `core` Java library. It does not replace `World.attack`, strategic movement, domestic commands, saves, or Android pages. Balance numbers are original engineering rules inspired by the requested experience, not claims about SAN11's exact proprietary formulas.

Implemented: five playable weapon types; immutable unit/commander snapshots; finite axial battlefield; nine terrain types; weighted A* and bounded Dijkstra; normal attacks; energy; four strategy-defined tactics; confusion, immobilization and morale-break traits; explicit player/AI turns; deterministic AI; elimination and stronghold victory; bounded draw termination; strategic DTOs and a read-only legacy World adapter; regression, fuzz, simulation and performance tests.

## Architecture

All production code: `core/src/main/java/game/sanguo/core/battle/`.

| Area | Classes / responsibility |
| --- | --- |
| Geometry / terrain | `HexPos`, `Terrain`, `Battlefield`: immutable local map; missing cells impassable |
| Central balance | `BattleRules`, `WeaponType`: costs, weapon profiles, matchup, damage and energy constants |
| Unit / status | `BattleUnit`, `BattleUnit.Commander`, `StatusEffect`: immutable values, remaining owner-turn durations |
| Paths | `Pathfinder`: A*, multi-goal A*, bounded reachable-tile Dijkstra |
| Combat | `DamageCalculator`: explicit seeded RNG, bounded square-root troop scaling |
| Tactics | `Tactic`, `Tactics`: registry and separate strategy classes; no tactic-id switch in engine |
| State machine | `BattleEngine`, `CommandResult`: validated commands; occupancy index; turn phases; snapshots |
| Objectives / result | `Stronghold`, `BattleResult`: siege/capture boundary, casualty conservation, winner or explicit draw |
| AI / executable demo | `BattleAi`, `BattleSimulation`: same public commands as the UI |
| Adapters | `adapter.BattleAdapters`, `adapter.LegacyWorldBattleAdapter`: no strategic mutations |

The engine is a **single-writer** object. Serialize commands on the game thread or an executor. Returned unit values, snapshots, maps and lists are immutable and may be shared with renderers. No Android imports or external libraries are required by the battle package. Only the optional legacy adapter imports `World`.

## Public API

`HexPos(q, r)` exposes `neighbor(Direction/int)`, `neighbors()`, `distance(other)`, `range(radius)` and `directionTo(other)`. Directions are E, NE, NW, W, SW, SE. A ray direction is absent for the center or a nonaligned hex; no arbitrary snapping is hidden inside displacement.

`Pathfinder.findPath(field, weapon, start, goal, occupied, maxCost)` returns `Optional<Path>`. A path includes its start, total entry cost and expanded-node count. `findPathToAny(...)` chooses the cheapest reachable destination. `reachableTiles(...)` returns an immutable hex-to-cost map including the origin at zero. Both allies and enemies block movement; the actor's own start can be included in an externally supplied occupancy set. Negative budgets are rejected. Ordinary engine moves use remaining movement, not the entire strategic map.

`BattleEngine` exposes:

```java
startTurn();                         // only AWAITING_TURN -> ACTIVE
startTurn(forceId);                  // validates scheduled force
move(unitId, destination);           // weighted path, finite remaining movement
canAttack(attackerId, targetId);      // pure eligibility query
attack(attackerId, targetId);
canUseTactic(attackerId, targetId, tacticId);
useTactic(attackerId, targetId, tacticId);
canSiege(attackerId, strongholdId);
siege(attackerId, strongholdId);
endUnitAction(unitId);
endTurn();
reachableTiles(unitId);
unit(unitId); unitAt(hex); units(); strongholds();
blockedTiles(unitId); snapshot(); result();
phase(); round(); activeForceId(); expectedForceId(); expectedControl();
```

Commands return `CommandResult` with `ok`, typed `error`, `message`, actual `damage`, and movement `path`. A rejected command leaves state and combat RNG unchanged. Eligibility queries never roll randomness. Invalid setup data throws `IllegalArgumentException`; programmer errors in third-party tactic implementations are not converted to player-command errors.

Exactly **two distinct forces** participate in v0.4. Explicit `Force(id, Control.PLAYER/AI)` definitions select initiative order and controller; the convenience constructor sorts the two force IDs and assigns PLAYER then AI. Both sides must initially have living units. Unit IDs, commander IDs and occupied deployment cells must be unique.

### Minimal working integration example

```java
Battlefield field = Battlefield.rectangle(12, 8, Terrain.PLAIN);
BattleUnit a = new BattleUnit(1, 0,
        new BattleUnit.Commander(101, "Commander A", 90, 85, 80),
        WeaponType.SPEAR, new HexPos(1, 2), 5000, 80, 100, 100);
BattleUnit b = new BattleUnit(2, 1,
        new BattleUnit.Commander(102, "Commander B", 80, 82, 70),
        WeaponType.CAVALRY, new HexPos(5, 2), 5000, 80, 100, 100);
BattleEngine battle = new BattleEngine(field, java.util.Arrays.asList(a, b), 31104L);
battle.startTurn(0);
CommandResult moved = battle.move(1, new HexPos(4, 2));
if (moved.ok && battle.canUseTactic(1, 2, "spear-thrust").ok) {
    battle.useTactic(1, 2, "spear-thrust");
}
if (battle.phase() == BattleEngine.Phase.ACTIVE) battle.endTurn();
if (battle.phase() != BattleEngine.Phase.FINISHED) new BattleAi(17L).playTurn(battle);
BattleEngine.Snapshot renderState = battle.snapshot();
```

Use imports from `game.sanguo.core.battle`. Keep a `BattleAi` instance across turns instead of reseeding it each time. Its decision RNG is separate from combat RNG. Replaying identical setup, seed and command order reproduces a battle; a resumable tactical-save/RNG-state codec is not included yet.

## Action, energy and status rules

A unit may split movement across commands, but every traversed cell spends its entry cost. Movement itself costs no energy. A no-op move is rejected. Normal attack, tactic, siege or `endUnitAction` closes the unit's action and clears remaining movement. There is no move-after-attack or multiple-attack loophole. Repeated `startTurn` is rejected.

At its owner's start, a living unit receives +6 energy, capped at 100, and its movement/action flags reset. Normal attack and siege spend 5 energy, floored at zero, and do not require a minimum energy balance. A tactic must have its full listed cost and spends that cost only, not an additional normal-attack surcharge.

Statuses store `remainingOwnerTurns`. They tick only at the affected force's `endTurn`, never at the caster's end. Confusion blocks movement and attack for one affected-owner turn; a confused unit starts with its action already closed. Immobilization blocks only movement. Morale break applies attack/defense penalties. Same-kind refresh takes the longer duration instead of multiplying duplicate effects. Dead units never regain actions. Fire/poison damage-over-time processors are future work; no decorative fire/poison status is falsely presented as implemented.

## Weapon profiles

Movement is an AP budget, not a number of hexes. Flat terrain costs 2; roads cost 1.

| Weapon | Movement | Normal range | Attack | Defense | Tactic |
| --- | ---: | --- | ---: | ---: | --- |
| Sword / basic infantry | 6 | 1 | 0.88 | 0.95 | None |
| Spear | 6 | 1 | 1.05 | 1.00 | Thrust |
| Halberd | 5 | 1 | 0.98 | 1.20 | Hook |
| Crossbow | 5 | 2–3 | 0.95 | 0.85 | Volley |
| Cavalry | 9 | 1 | 1.15 | 0.95 | Charge |

Spear counters cavalry, cavalry counters halberd, halberd counters spear: attack multiplier 1.25 in the advantageous direction, 0.90 in reverse, otherwise 1.00. Crossbows have a real adjacent blind spot; AI can withdraw to a firing cell.

`RAM`, `SIEGE_TOWER`, `WOODEN_BEAST`, `CATAPULT` are reserved enum identities. Constructing an active unit with an unsupported profile is explicitly rejected. Future equipment is not silently treated as sword infantry.

## Terrain rules

| Terrain | Foot cost | Cavalry cost | Attack | Defense |
| --- | ---: | ---: | ---: | ---: |
| Plain | 2 | 2 | 1.00 | 1.00 |
| Grass | 2 | 2 | 1.00 | 1.05 |
| Forest | 3 | 5 | 0.95 | 1.20 |
| Mountain | 4 | Impassable | 0.90 | 1.25 |
| Road | 1 | 1 | 1.00 | 0.95 |
| River | Impassable | Impassable | 0.80 | 0.80 |
| Shallow | 3 | 4 | 0.85 | 0.80 |
| City area | 2 | 2 | 1.00 | 1.35 |
| Pass area | 2 | 2 | 1.00 | 1.40 |

Terrain CITY/PASS cells are traversable regions. An actual `Stronghold` on a cell separately occupies and blocks it for all units. Displacement also respects map edges, terrain, units and objectives. River attack/defense factors reserve a consistent profile but do not permit land-unit deployment on rivers.

## Damage formula overview

```text
A = (50 + attack + 0.60*leadership + 0.40*war) / 200
    * weaponAttack * attackerTerrainAttack
    * (0.70 + 0.30*energy/100) * attackerStatus
D = (70 + defense + 0.85*leadership + 0.15*intelligence) / 250
    * weaponDefense * defenderTerrainDefense * defenderStatus
raw = 230 * sqrt(attackingTroops/1000)
      * clamp(A/D, 0.35, 2.5) * matchup * tacticMultiplier
hit = min(defendingTroops, clamp(round(raw * seededRandom[0.9,1.1)), 20, 2500))
```

Troops are bounded to 0–100,000; live deployment requires more than zero. Commander attributes are 0–100 and base attack/defense 1–300. Damage never creates troops, scales sublinearly with troop count, and cannot exceed the defender's remaining troops. Defeated units remain in result accounting but are removed from the occupancy index immediately. Basic siege uses the same square-root strength and variance with a 0.65 scale and bounded damage, not an unbounded troop-linear attack.

## Tactics and extension

| ID | Weapon | Range | Energy | Damage | Special rule |
| --- | --- | --- | ---: | ---: | --- |
| `spear-thrust` | Spear | 1 | 15 | 1.20 | Push one cell; 10–45% confusion chance based on intelligence difference |
| `halberd-hook` | Halberd | 2, straight ray | 15 | 1.10 | Pull one cell into the gap |
| `crossbow-volley` | Crossbow | 2–3 | 20 | 1.45 | Basic concentrated volley, no fabricated fire simulation |
| `cavalry-charge` | Cavalry | 1 | 20 | 1.35 | Push and follow; forest/shallow launch rejected |

Blocked displacement does not cancel already validated damage or refund energy. Pull/push never overlaps another unit. Charge follows only into a vacated, cavalry-passable cell, including when its target dies. Thrust confusion chance is `clamp(0.25 + 0.002*(attackerIntelligence-targetIntelligence), 0.10, 0.45)` and applies only to a surviving target.

Implement `Tactic`, provide its immutable `Definition`, optional pure `conditionError`, and `specialEffects` through the limited `Effects` capability. Register it in `Tactics(Collection)`. The engine handles common checks, cost, damage and action closure. Extensions must be stateless/trusted application code. A test registers a new immobilizing sword tactic without modifying the engine.

## AI, victory and strategic integration

AI chooses nearby attackable enemies, probabilistically uses eligible tactics, otherwise attacks, or follows a real shortest route to a legal attack cell. It tries a reachable farther enemy if the nearest is isolated, can detour through a distant bridge, retreats crossbows out of their blind spot, and waits when no useful route exists. `playTurn` refuses to control a PLAYER force. It can siege objectives and complete a real fight.

Eliminating all opposing units produces a winner. Capturing a critical stronghold produces an immediate winner even with opposing field units alive. Noncritical objectives change ownership without ending battle; captured walls retain only one durability point. Stalemates terminate as an explicit draw at the configurable round limit (default 200), not as an invented winner or infinite loop.

`BattleResult` includes `winner` (nullable for draw), `reason`, `completedRounds`, per-unit `survivingTroops`, `casualties`, `defeatedUnits`, `capturedCommanderCandidates`, `cityCaptured`, and `capturedCities` (city ID -> new force ID). Zero survivors remain in the map to make `initial = remaining + casualties` explicit. Candidate commanders are not automatically imprisoned.

`BattleAdapters.StrategicArmy -> toBattleUnit` and `BattleResult -> StrategicResult` are value-only boundaries. `LegacyWorldBattleAdapter` reads existing `World.Unit`/`Officer` fields, validates identity/ownership, maps weapon names explicitly rather than by ordinal, and accepts a local battle position. It never deducts troops or changes the strategic world.

Main-branch integration still needed: create local encounters from strategic contacts; choose player/AI controllers; render snapshots and command errors in Android; apply a completed result exactly once to strategic armies/cities; resolve prisoners; preserve strategic food/equipment conservation; define tactical save/replay schema. Existing `World.attack` and `World.siege` remain operational until that explicit integration. Do not run both legacy and tactical damage for the same encounter.

Not implemented: full SAN11 parity, line-of-sight/projectile blocking, elevation, zones of control, counterattacks, fire spread, naval combat, siege equipment profiles, duels, animations, multi-force alliances, Android battle screen or tactical save migration. Normal ranged attacks currently use hex distance only.

## Tests and measured results

Local environment: OpenJDK 21.0.11 compiling with `javac --release 17`, no external test dependency.

```text
bash scripts/test-core.sh
PASS: 189 existing core assertions
PASS: 282 existing scenario assertions
PASS: 725 existing domestic assertions
PASS: 71 tactical cases, 136159 assertions
PASS: 50x50 benchmark, 100 A* queries:
  p50=1.090ms p95=1.841ms max=2.987ms total=120.573ms
  maxExpanded=2125/2500; 100 bounded reachable queries=6.769ms
```

All 1,196 existing assertions continue to pass. Combined assertion count: 137,355, plus the separate benchmark checks. The 71 named tactical cases include 30 seeded weighted-map A*/Dijkstra cross-checks, 4,000 mixed-validity fuzz commands, all requested rule categories, twelve additional complete-battle seeds, and a golden ten-unit simulation. Assertion counts are not presented as 136,159 separate test methods.

Deterministic demo:

```sh
java -cp core/build/check game.sanguo.core.battle.BattleSimulation
```

Combat seed 31104, AI seed `31104 ^ 0x5DEECE66D`: force 0 wins by elimination during round 14 (13 fully completed rounds). Survivors by unit are `{0=38, 1=517, 2=3883, 3=5000, 4=0, 10=0, 11=0, 12=0, 13=0, 14=0}`. Every unit conserves its original troops against survivors plus casualties.

Performance is a warmed JVM smoke measurement on this container, not an Android device latency guarantee. The stable algorithmic guard is at most 2,500 expanded cells per 50x50 query; timing thresholds are deliberately generous on shared CI.

`./gradlew test` was attempted but the base repository contains no `gradlew` or wrapper JAR. This container also has no installed Gradle/Android SDK and cannot directly resolve GitHub. Therefore no local Gradle, lint, APK or emulator success is claimed. With the repository's Gradle 8.11.1/Android environment, run `gradle test`, `gradle lint`, `gradle assembleDebug`; the existing PR workflow runs the core script, `:core:check`, APK build/lint and emulator smoke. Consult the actual PR checks for remote verification, not this local-only result record.

`core/tactical-battle.gradle` attaches both new JavaExec runners to `test` and `check`. It also attaches existing main-based core runners to `test`, so `gradle test` does not silently omit them. No wrapper substitute or fake successful test command was added.

## Shared files and merge surface

Only two existing shared files are modified:

- `core/build.gradle`: append three lines to apply the independent tactical verification script.
- `scripts/test-core.sh`: append two runner invocations.

All other changes are new battle/adapter source files, new battle tests, `core/tactical-battle.gradle`, and this handoff. No `World`, `Domestic`, officer recruitment, save codec, resources, app Gradle, main map or Android UI files are changed. When merging with other agents, preserve both additions to the shared test wiring; no new module or `settings.gradle` edit is needed.

## Transport note

The provided runtime did not actually contain the stated clone, and native `git clone` failed on DNS. The existing GitHub source artifact for `726de636` supplied the local source; GitHub comparison confirmed the next base commit `f23d656a` changes documentation only. All production and test sources therefore match the branch base before modifications. The remote branch is based on exact `f23d656a`; unchanged newer main documentation is preserved by building the new Git tree on its original tree. Commit publication and branch push use the authorized GitHub connector rather than pretending a failed native transport succeeded. Local validation remains on the actual source files, not synthetic test stubs.

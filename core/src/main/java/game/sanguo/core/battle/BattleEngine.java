package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static game.sanguo.core.battle.CommandResult.Error.*;

/**
 * Single-writer deterministic tactical state machine. UI and AI use the same validated commands.
 * Not thread-safe: serialize commands on the game thread; immutable snapshots may be shared.
 * A unit may spend its finite movement budget in pieces, then attack/tactic once (which ends action).
 */
public final class BattleEngine {
    public enum Phase { AWAITING_TURN, ACTIVE, FINISHED }
    public enum Control { PLAYER, AI }
    public static final class Force {
        public final int id;
        public final Control control;
        public Force(int id, Control control) {
            if (id < 0) throw new IllegalArgumentException("Negative force id");
            this.id = id; this.control = Objects.requireNonNull(control);
        }
    }
    public static final class Snapshot {
        public final Phase phase;
        public final int round, activeForceId, expectedForceId;
        public final List<BattleUnit> units;
        public final List<Stronghold> strongholds;
        public final BattleResult result;
        private Snapshot(BattleEngine engine) {
            phase = engine.phase; round = engine.round; activeForceId = engine.activeForceId();
            expectedForceId = engine.expectedForceId(); units = engine.units();
            strongholds = engine.strongholds(); result = engine.result;
        }
    }
    public final Battlefield battlefield;
    public final long seed;
    private final Tactics tactics;
    private final Random random;
    private final int maxRounds;
    private final List<Force> forces;
    private final Map<Integer, BattleUnit> units = new LinkedHashMap<>();
    private final Map<Integer, Stronghold> strongholds = new LinkedHashMap<>();
    private final Map<HexPos, Integer> occupied = new HashMap<>();
    private final Set<HexPos> strongholdTiles = new HashSet<>();
    private final Map<Integer, Integer> initialTroops = new LinkedHashMap<>();
    private final Map<Integer, Integer> capturedCities = new LinkedHashMap<>();
    private Phase phase = Phase.AWAITING_TURN;
    private int forceIndex, round = 1;
    private BattleResult result;

    public BattleEngine(Battlefield field, Collection<BattleUnit> units, long seed) {
        this(field, units, defaultForces(units), Collections.emptyList(), Tactics.standard(), seed, BattleRules.MAX_ROUNDS);
    }
    public BattleEngine(Battlefield field, Collection<BattleUnit> input, List<Force> forces,
                        Collection<Stronghold> objectives, Tactics tactics, long seed, int maxRounds) {
        battlefield = Objects.requireNonNull(field); this.seed = seed; random = new Random(seed);
        this.tactics = Objects.requireNonNull(tactics);
        if (forces.size() != 2 || forces.get(0).id == forces.get(1).id)
            throw new IllegalArgumentException("v0.4 battles require exactly two distinct forces");
        if (maxRounds < 1 || maxRounds > 10000) throw new IllegalArgumentException("Invalid round limit");
        this.maxRounds = maxRounds; this.forces = Collections.unmodifiableList(new ArrayList<>(forces));
        List<Stronghold> sortedObjectives = new ArrayList<>(objectives);
        sortedObjectives.sort(Comparator.comparingInt(objective -> objective.id));
        for (Stronghold objective : sortedObjectives) {
            Terrain terrain = field.terrainAt(objective.position);
            if (!knownForce(objective.forceId) || (terrain != Terrain.CITY && terrain != Terrain.PASS)
                    || strongholds.put(objective.id, objective) != null || !strongholdTiles.add(objective.position))
                throw new IllegalArgumentException("Invalid or duplicate stronghold");
        }
        List<BattleUnit> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparingInt(unit -> unit.id));
        Set<Integer> commanders = new HashSet<>(), deployedForces = new HashSet<>();
        for (BattleUnit unit : sorted) {
            if (!unit.alive() || !knownForce(unit.forceId) || units.containsKey(unit.id)
                    || occupied.containsKey(unit.position) || strongholdTiles.contains(unit.position)
                    || !commanders.add(unit.commander.id) || !field.passable(unit.position, unit.weaponType))
                throw new IllegalArgumentException("Invalid unit deployment: " + unit.id);
            units.put(unit.id, unit); occupied.put(unit.position, unit.id);
            initialTroops.put(unit.id, unit.troopCount); deployedForces.add(unit.forceId);
        }
        if (deployedForces.size() != 2) throw new IllegalArgumentException("Both armies need living units");
    }
    private static List<Force> defaultForces(Collection<BattleUnit> units) {
        TreeSet<Integer> ids = new TreeSet<>();
        for (BattleUnit unit : units) ids.add(unit.forceId);
        if (ids.size() != 2) throw new IllegalArgumentException("Exactly two armies required");
        return Arrays.asList(new Force(ids.first(), Control.PLAYER), new Force(ids.last(), Control.AI));
    }
    private boolean knownForce(int id) {
        for (Force force : forces) if (force.id == id) return true;
        return false;
    }
    public Phase phase() { return phase; }
    public int round() { return round; }
    public int activeForceId() { return phase == Phase.ACTIVE ? forces.get(forceIndex).id : -1; }
    public int expectedForceId() { return phase == Phase.FINISHED ? -1 : forces.get(forceIndex).id; }
    public Control expectedControl() { return phase == Phase.FINISHED ? null : forces.get(forceIndex).control; }
    public BattleUnit unit(int id) { return units.get(id); }
    public BattleUnit unitAt(HexPos hex) { Integer id = occupied.get(hex); return id == null ? null : units.get(id); }
    public List<BattleUnit> units() { return Collections.unmodifiableList(new ArrayList<>(units.values())); }
    public List<Stronghold> strongholds() { return Collections.unmodifiableList(new ArrayList<>(strongholds.values())); }
    public List<Tactic> tactics() { return tactics.all(); }
    public Optional<BattleResult> result() { return Optional.ofNullable(result); }
    public Snapshot snapshot() { return new Snapshot(this); }
    /** Includes all unit and objective occupancy, but not this unit's own start tile. */
    public Set<HexPos> blockedTiles(int unitId) {
        Set<HexPos> blocked = new HashSet<>(occupied.keySet());
        BattleUnit unit = units.get(unitId);
        if (unit != null && unit.alive()) blocked.remove(unit.position);
        blocked.addAll(strongholdTiles);
        return Collections.unmodifiableSet(blocked);
    }
    public CommandResult startTurn() { return startTurn(expectedForceId()); }
    public CommandResult startTurn(int forceId) {
        if (phase != Phase.AWAITING_TURN) return CommandResult.fail(WRONG_PHASE, "Not awaiting a turn");
        if (forceId != expectedForceId()) return CommandResult.fail(WRONG_FORCE, "Unexpected force");
        phase = Phase.ACTIVE;
        for (BattleUnit unit : units()) if (unit.forceId == forceId && unit.alive()) replace(unit.startOwnerTurn());
        return CommandResult.success("Turn started");
    }
    public CommandResult endTurn() {
        if (phase != Phase.ACTIVE) return CommandResult.fail(WRONG_PHASE, "No active turn");
        for (BattleUnit unit : units()) if (unit.forceId == activeForceId() && unit.alive()) replace(unit.endOwnerTurn());
        phase = Phase.AWAITING_TURN;
        forceIndex++;
        if (forceIndex == forces.size()) {
            forceIndex = 0;
            if (round == maxRounds) { finish(null, BattleResult.Reason.TURN_LIMIT, round); return CommandResult.success("Round limit reached"); }
            round++;
        }
        return CommandResult.success("Turn ended");
    }
    private CommandResult actorError(int id) {
        if (phase != Phase.ACTIVE) return CommandResult.fail(WRONG_PHASE, "Battle is not in an active turn");
        BattleUnit unit = units.get(id);
        if (unit == null) return CommandResult.fail(UNKNOWN_UNIT, "Unknown unit");
        if (!unit.alive()) return CommandResult.fail(DEAD_UNIT, "Unit has been defeated");
        if (unit.forceId != activeForceId()) return CommandResult.fail(WRONG_FORCE, "Not this force's turn");
        if (unit.actionBlocked()) return CommandResult.fail(STATUS_BLOCKED, "Status prevents action");
        if (unit.actedThisTurn) return CommandResult.fail(ALREADY_ACTED, "Unit action has ended");
        return null;
    }
    public Map<HexPos, Integer> reachableTiles(int unitId) {
        if (actorError(unitId) != null || units.get(unitId).movementBlocked()) return Collections.emptyMap();
        BattleUnit unit = units.get(unitId);
        return Pathfinder.reachableTiles(battlefield, unit.weaponType, unit.position, blockedTiles(unitId), unit.movementRemaining);
    }
    public CommandResult move(int unitId, HexPos destination) {
        CommandResult error = actorError(unitId); if (error != null) return error;
        BattleUnit unit = units.get(unitId);
        if (unit.movementBlocked()) return CommandResult.fail(STATUS_BLOCKED, "Status prevents movement");
        if (destination == null || destination.equals(unit.position) || !battlefield.passable(destination, unit.weaponType))
            return CommandResult.fail(INVALID_DESTINATION, "Destination is not traversable or is unchanged");
        Set<HexPos> blocked = blockedTiles(unitId);
        if (blocked.contains(destination)) return CommandResult.fail(OCCUPIED, "Destination is occupied");
        Optional<Pathfinder.Path> path = Pathfinder.findPath(battlefield, unit.weaponType, unit.position,
                destination, blocked, unit.movementRemaining);
        if (!path.isPresent()) return CommandResult.fail(NO_PATH, "No path within remaining movement budget");
        replace(unit.copy(unit.troopCount, unit.energy, destination, unit.movementRemaining - path.get().totalCost,
                false, true, unit.status));
        return CommandResult.moved(path.get().tiles);
    }
    public CommandResult endUnitAction(int unitId) {
        CommandResult error = actorError(unitId); if (error != null) return error;
        closeAction(units.get(unitId), 0);
        return CommandResult.success("Unit action ended");
    }
    private CommandResult targetError(BattleUnit actor, BattleUnit target, int minRange, int maxRange) {
        if (target == null) return CommandResult.fail(UNKNOWN_UNIT, "Unknown target");
        if (!target.alive()) return CommandResult.fail(DEAD_UNIT, "Target has been defeated");
        if (target.forceId == actor.forceId) return CommandResult.fail(FRIENDLY_TARGET, "Target is friendly");
        int distance = actor.position.distance(target.position);
        if (distance < minRange || distance > maxRange) return CommandResult.fail(OUT_OF_RANGE, "Target outside attack range");
        return null;
    }
    /** Side-effect-free query suitable for Android buttons and AI planning. */
    public CommandResult canAttack(int attackerId, int targetId) {
        CommandResult error = actorError(attackerId); if (error != null) return error;
        BattleUnit actor = units.get(attackerId);
        BattleRules.WeaponProfile profile = BattleRules.weapon(actor.weaponType);
        error = targetError(actor, units.get(targetId), profile.minRange, profile.maxRange);
        return error == null ? CommandResult.success("Attack eligible") : error;
    }
    public CommandResult attack(int attackerId, int targetId) {
        CommandResult check = canAttack(attackerId, targetId); if (!check.ok) return check;
        BattleUnit actor = units.get(attackerId), target = units.get(targetId);
        int damage = rollDamage(actor, target, 1.0);
        hurt(target, damage); closeAction(actor, BattleRules.NORMAL_ATTACK_ENERGY); checkElimination();
        return CommandResult.hit(damage);
    }
    public CommandResult canUseTactic(int attackerId, int targetId, String tacticId) {
        CommandResult error = actorError(attackerId); if (error != null) return error;
        Tactic tactic = tactics.get(tacticId);
        if (tactic == null) return CommandResult.fail(UNKNOWN_TACTIC, "Unknown tactic");
        BattleUnit actor = units.get(attackerId), target = units.get(targetId);
        Tactic.Definition definition = tactic.definition();
        if (actor.weaponType != definition.weapon) return CommandResult.fail(WRONG_WEAPON, "Tactic requires a different weapon");
        if (actor.energy < definition.energyCost) return CommandResult.fail(INSUFFICIENT_ENERGY, "Insufficient energy");
        error = targetError(actor, target, definition.minRange, definition.maxRange); if (error != null) return error;
        String condition = tactic.conditionError(new Tactic.Context(actor, target, battlefield));
        return condition == null ? CommandResult.success("Tactic eligible") : CommandResult.fail(TACTIC_CONDITION, condition);
    }
    public CommandResult useTactic(int attackerId, int targetId, String tacticId) {
        CommandResult check = canUseTactic(attackerId, targetId, tacticId); if (!check.ok) return check;
        Tactic tactic = tactics.get(tacticId);
        BattleUnit actor = units.get(attackerId), target = units.get(targetId);
        int damage = rollDamage(actor, target, tactic.definition().damageMultiplier);
        hurt(target, damage); closeAction(actor, tactic.definition().energyCost);
        tactic.specialEffects(new Tactic.Context(actor, target, battlefield), new EffectSink(actor.id, target.id, target.position), random);
        checkElimination();
        return CommandResult.hit(damage);
    }
    public CommandResult canSiege(int attackerId, int strongholdId) {
        CommandResult error = actorError(attackerId); if (error != null) return error;
        Stronghold objective = strongholds.get(strongholdId);
        if (objective == null) return CommandResult.fail(UNKNOWN_STRONGHOLD, "Unknown stronghold");
        BattleUnit actor = units.get(attackerId);
        if (objective.forceId == actor.forceId) return CommandResult.fail(FRIENDLY_TARGET, "Stronghold is friendly");
        if (actor.position.distance(objective.position) != 1) return CommandResult.fail(OUT_OF_RANGE, "Siege requires adjacency");
        return CommandResult.success("Siege eligible");
    }
    /** Foundational objective interface, not full siege equipment or garrison combat yet. */
    public CommandResult siege(int attackerId, int strongholdId) {
        CommandResult check = canSiege(attackerId, strongholdId); if (!check.ok) return check;
        BattleUnit actor = units.get(attackerId); Stronghold target = strongholds.get(strongholdId);
        int damage = DamageCalculator.siegeDamage(actor, battlefield.terrainAt(actor.position), target, random);
        int remaining = target.durability - damage;
        closeAction(actor, BattleRules.NORMAL_ATTACK_ENERGY);
        if (remaining == 0) {
            // Capture leaves one point of ruined walls, not a freshly restored fortress.
            strongholds.put(target.id, new Stronghold(target.id, actor.forceId, target.position, 1, target.critical));
            capturedCities.put(target.id, actor.forceId);
            if (target.critical) finish(actor.forceId, BattleResult.Reason.CRITICAL_STRONGHOLD_CAPTURED, round - 1);
        } else strongholds.put(target.id, new Stronghold(target.id, target.forceId, target.position, remaining, target.critical));
        return CommandResult.hit(damage);
    }
    private int rollDamage(BattleUnit actor, BattleUnit target, double multiplier) {
        return DamageCalculator.damage(actor, target, battlefield.terrainAt(actor.position),
                battlefield.terrainAt(target.position), multiplier, random);
    }
    private void hurt(BattleUnit target, int damage) {
        int remaining = target.troopCount - damage;
        replace(target.copy(remaining, target.energy, target.position, remaining == 0 ? 0 : target.movementRemaining,
                remaining == 0 || target.actedThisTurn, target.movedThisTurn, target.status));
    }
    private void closeAction(BattleUnit unit, int energyCost) {
        replace(unit.copy(unit.troopCount, Math.max(0, unit.energy - energyCost), unit.position,
                0, true, unit.movedThisTurn, unit.status));
    }
    private void replace(BattleUnit next) {
        Integer occupant = occupied.get(next.position);
        if (next.alive() && occupant != null && occupant != next.id) throw new IllegalStateException("Overlapping live units");
        BattleUnit old = units.get(next.id);
        if (old != null && old.alive()) occupied.remove(old.position);
        units.put(next.id, next);
        if (next.alive()) occupied.put(next.position, next.id);
    }
    private boolean vacantAndPassable(HexPos hex, WeaponType weapon) {
        return battlefield.passable(hex, weapon) && !occupied.containsKey(hex) && !strongholdTiles.contains(hex);
    }
    private void displace(BattleUnit unit, HexPos hex) {
        replace(unit.copy(unit.troopCount, unit.energy, hex, unit.movementRemaining,
                unit.actedThisTurn, unit.movedThisTurn, unit.status));
    }
    private final class EffectSink implements Tactic.Effects {
        final int actorId, targetId;
        final HexPos originalTargetPosition;
        EffectSink(int actorId, int targetId, HexPos originalTargetPosition) {
            this.actorId = actorId; this.targetId = targetId; this.originalTargetPosition = originalTargetPosition;
        }
        @Override public void pushTarget(boolean follow) {
            BattleUnit actor = units.get(actorId), target = units.get(targetId);
            boolean vacated = !target.alive();
            Optional<HexPos.Direction> direction = actor.position.directionTo(target.position);
            if (target.alive() && direction.isPresent()) {
                HexPos destination = target.position.neighbor(direction.get());
                if (vacantAndPassable(destination, target.weaponType)) { displace(target, destination); vacated = true; }
            }
            if (follow && vacated && vacantAndPassable(originalTargetPosition, actor.weaponType))
                displace(actor, originalTargetPosition);
        }
        @Override public void pullTarget() {
            BattleUnit actor = units.get(actorId), target = units.get(targetId);
            Optional<HexPos.Direction> direction = target.position.directionTo(actor.position);
            if (target.alive() && direction.isPresent()) {
                HexPos destination = target.position.neighbor(direction.get());
                if (vacantAndPassable(destination, target.weaponType)) displace(target, destination);
            }
        }
        @Override public void applyStatus(StatusEffect effect) {
            BattleUnit target = units.get(targetId);
            if (target.alive()) replace(target.withStatus(effect));
        }
    }
    private void checkElimination() {
        Set<Integer> livingForces = new HashSet<>();
        for (BattleUnit unit : units.values()) if (unit.alive()) livingForces.add(unit.forceId);
        if (livingForces.size() == 1) finish(livingForces.iterator().next(), BattleResult.Reason.ELIMINATION, round - 1);
    }
    private void finish(Integer winner, BattleResult.Reason reason, int completedRounds) {
        result = new BattleResult(winner, reason, completedRounds, units.values(), initialTroops, capturedCities);
        phase = Phase.FINISHED;
    }
}

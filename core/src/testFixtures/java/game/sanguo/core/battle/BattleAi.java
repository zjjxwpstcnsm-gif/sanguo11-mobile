package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Basic tactical AI. Its RNG is separate from combat RNG so planning cannot change damage rolls. */
public final class BattleAi {
    public enum Action { MOVE, ATTACK, TACTIC, SIEGE, WAIT }
    public static final class Decision {
        public final Action action;
        public final int unitId, targetId;
        public final HexPos destination;
        public final String tacticId;
        private Decision(Action action, int unitId, int targetId, HexPos destination, String tacticId) {
            this.action = action; this.unitId = unitId; this.targetId = targetId;
            this.destination = destination; this.tacticId = tacticId;
        }
    }
    private final Random random;
    private final double tacticChance;
    public BattleAi(long seed) { this(seed, BattleRules.AI_TACTIC_CHANCE); }
    public BattleAi(long seed, double tacticChance) {
        if (!Double.isFinite(tacticChance) || tacticChance < 0 || tacticChance > 1)
            throw new IllegalArgumentException("Tactic chance must be 0..1");
        random = new Random(seed); this.tacticChance = tacticChance;
    }
    /** Reads engine state only. Calling decide advances only this AI's private decision RNG. */
    public Decision decide(BattleEngine engine, int unitId) {
        BattleUnit actor = engine.unit(unitId);
        if (actor == null || !actor.alive() || actor.actedThisTurn || actor.actionBlocked()
                || actor.forceId != engine.activeForceId()) return decision(Action.WAIT, unitId, -1, null, null);
        List<BattleUnit> enemies = new ArrayList<>();
        for (BattleUnit unit : engine.units()) if (unit.alive() && unit.forceId != actor.forceId) enemies.add(unit);
        enemies.sort(Comparator.comparingInt((BattleUnit unit) -> actor.position.distance(unit.position))
                .thenComparingInt(unit -> unit.id));
        Decision normal = null, tactic = null;
        for (BattleUnit enemy : enemies) {
            if (normal == null && engine.canAttack(actor.id, enemy.id).ok)
                normal = decision(Action.ATTACK, actor.id, enemy.id, null, null);
            if (tactic == null) for (Tactic candidate : engine.tactics())
                if (engine.canUseTactic(actor.id, enemy.id, candidate.definition().id).ok) {
                    tactic = decision(Action.TACTIC, actor.id, enemy.id, null, candidate.definition().id); break;
                }
        }
        if (tactic != null && random.nextDouble() < tacticChance) return tactic;
        if (normal != null) return normal;
        for (Stronghold objective : engine.strongholds()) if (engine.canSiege(actor.id, objective.id).ok)
            return decision(Action.SIEGE, actor.id, objective.id, null, null);
        if (actor.movementRemaining > 0 && !actor.movementBlocked()) {
            Set<HexPos> blocked = engine.blockedTiles(actor.id);
            BattleRules.WeaponProfile profile = BattleRules.weapon(actor.weaponType);
            for (BattleUnit enemy : enemies) {
                List<HexPos> firingPositions = new ArrayList<>();
                for (HexPos hex : enemy.position.range(profile.maxRange))
                    if (hex.distance(enemy.position) >= profile.minRange) firingPositions.add(hex);
                Decision move = toward(engine, actor, firingPositions, blocked);
                if (move != null) return move;
            }
            // If all enemies are unreachable, try an enemy objective instead of wandering.
            for (Stronghold objective : engine.strongholds()) if (objective.forceId != actor.forceId) {
                Decision move = toward(engine, actor, objective.position.neighbors(), blocked);
                if (move != null) return move;
            }
        }
        return decision(Action.WAIT, actor.id, -1, null, null);
    }
    private Decision toward(BattleEngine engine, BattleUnit actor, List<HexPos> goals, Set<HexPos> blocked) {
        Optional<Pathfinder.Path> path = Pathfinder.findPathToAny(engine.battlefield, actor.weaponType,
                actor.position, goals, blocked, Integer.MAX_VALUE);
        if (!path.isPresent() || path.get().tiles.size() <= 1) return null;
        int cost = 0; HexPos destination = actor.position;
        for (int i = 1; i < path.get().tiles.size(); i++) {
            HexPos next = path.get().tiles.get(i);
            cost += engine.battlefield.movementCost(next, actor.weaponType);
            if (cost > actor.movementRemaining) break;
            destination = next;
        }
        return destination.equals(actor.position) ? null : decision(Action.MOVE, actor.id, -1, destination, null);
    }
    private static Decision decision(Action action, int id, int target, HexPos destination, String tactic) {
        return new Decision(action, id, target, destination, tactic);
    }
    public CommandResult execute(BattleEngine engine, Decision decision) {
        switch (decision.action) {
            case MOVE: return engine.move(decision.unitId, decision.destination);
            case ATTACK: return engine.attack(decision.unitId, decision.targetId);
            case TACTIC: return engine.useTactic(decision.unitId, decision.targetId, decision.tacticId);
            case SIEGE: return engine.siege(decision.unitId, decision.targetId);
            default: return engine.endUnitAction(decision.unitId);
        }
    }
    /** Runs exactly one AI force turn (including start/end); refuses to take over a player force. */
    public int playTurn(BattleEngine engine) {
        if (engine.phase() == BattleEngine.Phase.FINISHED) return 0;
        if (engine.expectedControl() != BattleEngine.Control.AI) throw new IllegalStateException("Not an AI-controlled turn");
        if (engine.phase() == BattleEngine.Phase.AWAITING_TURN) require(engine.startTurn());
        int commands = 0;
        for (BattleUnit initial : engine.units()) {
            if (initial.forceId != engine.activeForceId()) continue;
            // Each successful move spends at least 1 AP, and the final command ends action.
            int safety = initial.movement + 1;
            while (engine.phase() == BattleEngine.Phase.ACTIVE && safety-- > 0) {
                BattleUnit actor = engine.unit(initial.id);
                if (!actor.alive() || actor.actedThisTurn || actor.actionBlocked()) break;
                require(execute(engine, decide(engine, actor.id))); commands++;
            }
            if (engine.phase() == BattleEngine.Phase.FINISHED) break;
        }
        if (engine.phase() == BattleEngine.Phase.ACTIVE) require(engine.endTurn());
        return commands;
    }
    private static void require(CommandResult result) {
        if (!result.ok) throw new IllegalStateException("AI issued invalid command: " + result.error + ": " + result.message);
    }
}

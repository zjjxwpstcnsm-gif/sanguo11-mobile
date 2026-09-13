package game.sanguo.core.battle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class BattleTestSupport {
    private BattleTestSupport() { }
    static int cases, assertions;
    static void run(String name, Runnable test) {
        try { test.run(); cases++; }
        catch (Throwable error) { throw new AssertionError("FAILED: " + name, error); }
    }
    static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    static void eq(Object expected, Object actual) {
        check(Objects.equals(expected, actual), "Expected " + expected + " but got " + actual);
    }
    static void ok(CommandResult result) { check(result.ok, "Unexpected error: " + result.error + " " + result.message); }
    static void error(CommandResult.Error expected, CommandResult result) { eq(expected, result.error); }
    static void rejects(Class<? extends Throwable> type, Runnable action) {
        assertions++;
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Wrong exception: " + error, error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    static HexPos h(int q, int r) { return new HexPos(q, r); }
    static BattleUnit u(int id, int force, WeaponType weapon, int q, int r) {
        return troops(id, force, weapon, q, r, 5000);
    }
    static BattleUnit troops(int id, int force, WeaponType weapon, int q, int r, int troops) {
        return new BattleUnit(id, force, new BattleUnit.Commander(100 + id, "C" + id, 80, 80, 80),
                weapon, h(q, r), troops, 80, 100, 100);
    }
    static List<BattleEngine.Force> aiForces() {
        return Arrays.asList(new BattleEngine.Force(0, BattleEngine.Control.AI), new BattleEngine.Force(1, BattleEngine.Control.AI));
    }
    static BattleEngine engine(BattleUnit... units) {
        return new BattleEngine(Battlefield.rectangle(12, 10, Terrain.PLAIN), Arrays.asList(units), 3);
    }
    static BattleEngine active(BattleUnit... units) { BattleEngine engine = engine(units); ok(engine.startTurn()); return engine; }
    static BattleEngine custom(Battlefield field, long seed, int rounds, BattleUnit... units) {
        return new BattleEngine(field, Arrays.asList(units), aiForces(), Collections.emptyList(), Tactics.standard(), seed, rounds);
    }
    static String fingerprint(BattleEngine engine) {
        StringBuilder out = new StringBuilder().append(engine.phase()).append('/').append(engine.round())
                .append('/').append(engine.activeForceId()).append('/').append(engine.expectedForceId());
        for (BattleUnit unit : engine.units()) {
            out.append('|').append(unit.id).append(':').append(unit.forceId).append(':').append(unit.position)
                    .append(':').append(unit.troopCount).append(':').append(unit.energy).append(':').append(unit.movementRemaining)
                    .append(':').append(unit.actedThisTurn).append(':').append(unit.movedThisTurn);
            for (StatusEffect effect : unit.status.values()) out.append(':').append(effect.kind).append('@').append(effect.remainingOwnerTurns);
        }
        for (Stronghold objective : engine.strongholds()) out.append("#").append(objective.id).append(':')
                .append(objective.forceId).append(':').append(objective.durability);
        if (engine.result().isPresent()) out.append("=").append(engine.result().get().winner).append(engine.result().get().reason);
        return out.toString();
    }
}

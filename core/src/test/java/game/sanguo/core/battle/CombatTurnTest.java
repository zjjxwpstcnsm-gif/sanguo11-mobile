package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static game.sanguo.core.battle.BattleTestSupport.*;
import static game.sanguo.core.battle.CommandResult.Error.*;

final class CombatTurnTest {
    private static BattleUnit sword(int id, int force, int q, int r) { return u(id, force, WeaponType.SWORD, q, r); }
    private static int damage(BattleUnit attacker, BattleUnit defender, Terrain attackTerrain, Terrain defenseTerrain) {
        return DamageCalculator.damage(attacker, defender, attackTerrain, defenseTerrain, 1, new Random(13));
    }
    static void runAll() {
        run("invalid unit and commander values are rejected", () -> {
            rejects(IllegalArgumentException.class, () -> troops(0, 0, WeaponType.SWORD, 1, 1, -1));
            rejects(IllegalArgumentException.class, () -> troops(0, 0, WeaponType.SWORD, 1, 1, 100001));
            rejects(IllegalArgumentException.class, () -> sword(0, 0, 1, 1).withEnergy(101));
            rejects(IllegalArgumentException.class, () -> new BattleUnit.Commander(0, "Bad", 101, 50, 50));
            rejects(IllegalArgumentException.class, () -> new StatusEffect(StatusEffect.Kind.CONFUSED, 0));
        });
        run("deployment rejects duplicate ids occupancy commander and dead army", () -> {
            rejects(IllegalArgumentException.class, () -> engine(sword(0, 0, 1, 1), sword(0, 1, 2, 1)));
            rejects(IllegalArgumentException.class, () -> engine(sword(0, 0, 1, 1), sword(1, 1, 1, 1)));
            BattleUnit first = sword(0, 0, 1, 1);
            BattleUnit sameCommander = new BattleUnit(1, 1, first.commander, WeaponType.SWORD, h(2, 1), 5000, 80, 100, 100);
            rejects(IllegalArgumentException.class, () -> engine(first, sameCommander));
            rejects(IllegalArgumentException.class, () -> engine(first, troops(1, 1, WeaponType.SWORD, 2, 1, 0)));
            rejects(IllegalArgumentException.class, () -> engine(first, sword(1, 1, 99, 1)));
            Battlefield mountain = Battlefield.rectangle(4, 4, Terrain.MOUNTAIN);
            rejects(IllegalArgumentException.class, () -> custom(mountain, 3, 10, u(0, 0, WeaponType.CAVALRY, 1, 1), sword(1, 1, 2, 1)));
        });
        run("snapshot and unit values cannot be externally mutated", () -> {
            BattleUnit input = sword(0, 0, 1, 1); BattleEngine engine = engine(input, sword(1, 1, 8, 1));
            BattleEngine.Snapshot before = engine.snapshot(); ok(engine.startTurn()); ok(engine.move(0, h(2, 1)));
            eq(h(1, 1), input.position); eq(h(1, 1), before.units.get(0).position);
            eq(BattleEngine.Phase.AWAITING_TURN, before.phase);
            rejects(UnsupportedOperationException.class, () -> before.units.clear());
            rejects(UnsupportedOperationException.class, () -> input.status.clear());
            rejects(UnsupportedOperationException.class, () -> engine.blockedTiles(0).clear());
            input.withEnergy(1); eq(86, engine.unit(0).energy);
        });
        run("explicit player AI force order and start cannot be repeated", () -> {
            BattleEngine engine = engine(sword(0, 0, 1, 1), sword(1, 1, 8, 1));
            eq(BattleEngine.Control.PLAYER, engine.expectedControl()); error(WRONG_PHASE, engine.attack(0, 1));
            error(WRONG_FORCE, engine.startTurn(1)); ok(engine.startTurn(0)); error(WRONG_PHASE, engine.startTurn(0));
            error(WRONG_FORCE, engine.move(1, h(7, 1))); ok(engine.endTurn());
            eq(-1, engine.activeForceId()); eq(BattleEngine.Control.AI, engine.expectedControl());
            error(WRONG_FORCE, engine.startTurn(0)); ok(engine.startTurn(1)); ok(engine.endTurn()); eq(2, engine.round());
        });
        run("move budget accumulates and move then attack is legal", () -> {
            BattleEngine engine = active(sword(0, 0, 1, 1), sword(1, 1, 5, 1));
            int energy = engine.unit(0).energy;
            CommandResult move = engine.move(0, h(3, 1)); ok(move); eq(3, move.path.size()); eq(2, engine.unit(0).movementRemaining);
            ok(engine.move(0, h(4, 1))); eq(0, engine.unit(0).movementRemaining); eq(energy, engine.unit(0).energy);
            error(NO_PATH, engine.move(0, h(4, 2))); ok(engine.attack(0, 1));
            check(engine.unit(0).actedThisTurn, "Attack closes action"); eq(energy - 5, engine.unit(0).energy);
            error(ALREADY_ACTED, engine.attack(0, 1)); error(ALREADY_ACTED, engine.move(0, h(3, 1)));
        });
        run("invalid movement has no side effects", () -> {
            BattleEngine engine = active(sword(0, 0, 1, 1), sword(1, 1, 4, 1)); String before = fingerprint(engine);
            error(INVALID_DESTINATION, engine.move(0, null)); error(INVALID_DESTINATION, engine.move(0, h(1, 1)));
            error(INVALID_DESTINATION, engine.move(0, h(-1, 0))); error(OCCUPIED, engine.move(0, h(4, 1)));
            error(NO_PATH, engine.move(0, h(10, 1))); error(UNKNOWN_UNIT, engine.move(99, h(2, 1)));
            eq(before, fingerprint(engine));
        });
        run("allied and enemy occupancy block engine reachable area", () -> {
            BattleEngine engine = active(sword(0, 0, 1, 1), sword(1, 1, 2, 1), sword(2, 0, 1, 2));
            check(!engine.reachableTiles(0).containsKey(h(2, 1)), "Enemy blocked");
            check(!engine.reachableTiles(0).containsKey(h(1, 2)), "Ally blocked");
            check(engine.reachableTiles(0).containsKey(h(1, 1)), "Own start included");
            check(engine.reachableTiles(1).isEmpty(), "Enemy cannot act on player turn");
        });
        run("endUnitAction closes action without double recovery", () -> {
            BattleEngine engine = active(sword(0, 0, 1, 1), sword(1, 1, 8, 1)); int energy = engine.unit(0).energy;
            ok(engine.endUnitAction(0)); error(ALREADY_ACTED, engine.endUnitAction(0));
            eq(energy, engine.unit(0).energy); check(engine.reachableTiles(0).isEmpty(), "Ended unit cannot move");
        });
        run("turn reset only resets current force and energy is capped", () -> {
            BattleEngine engine = active(sword(0, 0, 1, 1).withEnergy(100), sword(1, 1, 8, 1));
            ok(engine.endUnitAction(0)); ok(engine.endTurn()); ok(engine.startTurn());
            check(engine.unit(0).actedThisTurn, "Opponent start must not reset actor");
            ok(engine.endTurn()); ok(engine.startTurn());
            check(!engine.unit(0).actedThisTurn && !engine.unit(0).movedThisTurn, "Own turn resets action flags");
            eq(6, engine.unit(0).movementRemaining); eq(100, engine.unit(0).energy);
        });
        run("melee and ranged attacks honor minimum and maximum range", () -> {
            BattleEngine bow = active(u(0, 0, WeaponType.CROSSBOW, 1, 1), sword(1, 1, 2, 1), sword(2, 1, 3, 1), sword(3, 1, 4, 1), sword(4, 1, 5, 1));
            error(OUT_OF_RANGE, bow.canAttack(0, 1)); ok(bow.canAttack(0, 2)); ok(bow.canAttack(0, 3)); error(OUT_OF_RANGE, bow.canAttack(0, 4));
            BattleEngine melee = active(sword(0, 0, 1, 1), sword(1, 1, 3, 1)); error(OUT_OF_RANGE, melee.attack(0, 1));
            error(FRIENDLY_TARGET, melee.attack(0, 0));
        });
        run("damage seed reproducibility and variance", () -> {
            BattleUnit actor = sword(0, 0, 1, 1), target = sword(1, 1, 2, 1); List<Integer> rolls = new ArrayList<>();
            Random random = new Random(44), repeated = new Random(44);
            for (int i = 0; i < 20; i++) {
                int hit = DamageCalculator.damage(actor, target, Terrain.PLAIN, Terrain.PLAIN, 1, random);
                eq(hit, DamageCalculator.damage(actor, target, Terrain.PLAIN, Terrain.PLAIN, 1, repeated)); rolls.add(hit);
            }
            check(Collections.min(rolls) < Collections.max(rolls), "Random variation exists");
        });
        run("sqrt troop scaling is sublinear and damage is capped", () -> {
            BattleUnit target = troops(1, 1, WeaponType.SWORD, 2, 1, 100000);
            int small = damage(troops(0, 0, WeaponType.SWORD, 1, 1, 1000), target, Terrain.PLAIN, Terrain.PLAIN);
            int large = damage(troops(0, 0, WeaponType.SWORD, 1, 1, 4000), target, Terrain.PLAIN, Terrain.PLAIN);
            check(large > small && large < small * 2.1, "Four times troops gives roughly twice damage, not four times");
            BattleUnit maximum = new BattleUnit(0, 0, new BattleUnit.Commander(100, "Max", 100, 100, 100), WeaponType.CAVALRY, h(1, 1), 100000, 100, 300, 300);
            for (int seed = 0; seed < 20; seed++) {
                int hit = DamageCalculator.damage(maximum, target, Terrain.PLAIN, Terrain.SHALLOW, 3, new Random(seed));
                check(hit > 0 && hit <= BattleRules.MAX_DAMAGE, "Damage bound");
            }
            eq(1, damage(maximum, troops(1, 1, WeaponType.SWORD, 2, 1, 1), Terrain.PLAIN, Terrain.PLAIN));
            rejects(IllegalArgumentException.class, () -> DamageCalculator.damage(maximum, target, Terrain.PLAIN, Terrain.PLAIN, Double.NaN, new Random()));
        });
        run("commander leadership war and intelligence affect combat", () -> {
            BattleUnit target = sword(1, 1, 2, 1);
            BattleUnit low = new BattleUnit(0, 0, new BattleUnit.Commander(100, "Low", 0, 0, 0), WeaponType.SWORD, h(1, 1), 5000, 80, 100, 100);
            BattleUnit leader = new BattleUnit(0, 0, new BattleUnit.Commander(100, "Leader", 100, 0, 0), WeaponType.SWORD, h(1, 1), 5000, 80, 100, 100);
            BattleUnit fighter = new BattleUnit(0, 0, new BattleUnit.Commander(100, "Fighter", 0, 100, 0), WeaponType.SWORD, h(1, 1), 5000, 80, 100, 100);
            BattleUnit wise = new BattleUnit(0, 0, new BattleUnit.Commander(100, "Wise", 0, 0, 100), WeaponType.SWORD, h(1, 1), 5000, 80, 100, 100);
            check(damage(leader, target, Terrain.PLAIN, Terrain.PLAIN) > damage(low, target, Terrain.PLAIN, Terrain.PLAIN), "Leadership offense");
            check(damage(fighter, target, Terrain.PLAIN, Terrain.PLAIN) > damage(low, target, Terrain.PLAIN, Terrain.PLAIN), "War offense");
            check(damage(target, leader, Terrain.PLAIN, Terrain.PLAIN) < damage(target, low, Terrain.PLAIN, Terrain.PLAIN), "Leadership defense");
            check(damage(target, wise, Terrain.PLAIN, Terrain.PLAIN) < damage(target, low, Terrain.PLAIN, Terrain.PLAIN), "Intelligence defense");
        });
        run("terrain and attacker defender statuses modify damage", () -> {
            BattleUnit actor = sword(0, 0, 1, 1), target = sword(1, 1, 2, 1);
            int plain = damage(actor, target, Terrain.PLAIN, Terrain.PLAIN);
            check(damage(actor, target, Terrain.PLAIN, Terrain.FOREST) < plain, "Forest cover reduces incoming damage");
            check(damage(actor, target, Terrain.SHALLOW, Terrain.PLAIN) < plain, "Bad footing reduces attack");
            check(damage(actor.withStatus(new StatusEffect(StatusEffect.Kind.MORALE_BREAK, 1)), target, Terrain.PLAIN, Terrain.PLAIN) < plain, "Attacker morale status");
            check(damage(actor, target.withStatus(new StatusEffect(StatusEffect.Kind.CONFUSED, 1)), Terrain.PLAIN, Terrain.PLAIN) > plain, "Confused target defense penalty");
            check(damage(actor.withEnergy(100), target, Terrain.PLAIN, Terrain.PLAIN) > damage(actor.withEnergy(0), target, Terrain.PLAIN, Terrain.PLAIN), "Energy modifies attack");
        });
        run("ordinary attack from exhausted input consumes only normal cost", () -> {
            BattleEngine engine = active(sword(0, 0, 1, 1).withEnergy(0), sword(1, 1, 2, 1));
            eq(6, engine.unit(0).energy); ok(engine.attack(0, 1)); eq(1, engine.unit(0).energy);
        });
        run("invalid attacks and previews do not consume combat RNG", () -> {
            BattleEngine noisy = active(u(0, 0, WeaponType.SPEAR, 1, 1), sword(1, 1, 2, 1));
            BattleEngine clean = active(u(0, 0, WeaponType.SPEAR, 1, 1), sword(1, 1, 2, 1)); String before = fingerprint(noisy);
            for (int i = 0; i < 20; i++) {
                error(FRIENDLY_TARGET, noisy.attack(0, 0)); error(UNKNOWN_UNIT, noisy.attack(0, 99));
                error(WRONG_WEAPON, noisy.useTactic(0, 1, "crossbow-volley"));
                error(UNKNOWN_TACTIC, noisy.useTactic(0, 1, "missing")); ok(noisy.canUseTactic(0, 1, "spear-thrust")); ok(noisy.canAttack(0, 1));
            }
            eq(before, fingerprint(noisy)); eq(clean.attack(0, 1).damage, noisy.attack(0, 1).damage); eq(fingerprint(clean), fingerprint(noisy));
        });
        run("tactic weapon energy range and condition eligibility", () -> {
            BattleEngine low = active(u(0, 0, WeaponType.SPEAR, 1, 1).withEnergy(0), sword(1, 1, 2, 1));
            error(INSUFFICIENT_ENERGY, low.useTactic(0, 1, "spear-thrust")); eq(6, low.unit(0).energy);
            BattleEngine out = active(u(0, 0, WeaponType.SPEAR, 1, 1), sword(1, 1, 4, 1));
            error(OUT_OF_RANGE, out.useTactic(0, 1, "spear-thrust"));
            BattleEngine hook = active(u(0, 0, WeaponType.HALBERD, 1, 1), sword(1, 1, 2, 2));
            error(TACTIC_CONDITION, hook.useTactic(0, 1, "halberd-hook"));
        });
        run("exact tactic energy consumption no normal attack surcharge", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.SPEAR, 1, 1).withEnergy(9), sword(1, 1, 2, 1));
            eq(15, engine.unit(0).energy); ok(engine.useTactic(0, 1, "spear-thrust")); eq(0, engine.unit(0).energy);
            error(ALREADY_ACTED, engine.useTactic(0, 1, "spear-thrust")); check(engine.unit(0).actedThisTurn, "Tactic closes action");
        });
        run("spear thrust pushes one cell and may confuse with fixed seed", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.SPEAR, 1, 1), sword(1, 1, 2, 1));
            ok(engine.useTactic(0, 1, "spear-thrust")); eq(h(3, 1), engine.unit(1).position);
            check(engine.unitAt(h(2, 1)) == null, "Old cell freed"); eq(1, engine.unitAt(h(3, 1)).id);
            check(engine.unit(1).status.containsKey(StatusEffect.Kind.CONFUSED), "Seed 3 applies confusion");
        });
        run("blocked push still deals damage without overlapping units", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.SPEAR, 1, 1), sword(1, 1, 2, 1), sword(2, 0, 3, 1));
            ok(engine.useTactic(0, 1, "spear-thrust")); eq(h(2, 1), engine.unit(1).position); eq(2, engine.unitAt(h(3, 1)).id);
            check(engine.unit(1).troopCount < 5000, "Blocked displacement still inflicts damage");
        });
        run("push cannot enter water or force cavalry onto mountains", () -> {
            for (Terrain blocked : Arrays.asList(Terrain.RIVER, Terrain.MOUNTAIN)) {
                Battlefield field = Battlefield.rectangle(6, 4, Terrain.PLAIN).withTerrain(h(3, 1), blocked);
                BattleEngine engine = custom(field, 3, 10, u(0, 0, WeaponType.SPEAR, 1, 1), u(1, 1, WeaponType.CAVALRY, 2, 1));
                ok(engine.startTurn()); ok(engine.useTactic(0, 1, "spear-thrust")); eq(h(2, 1), engine.unit(1).position);
            }
        });
        run("push at battlefield boundary never leaves map", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.SPEAR, 10, 1), sword(1, 1, 11, 1));
            ok(engine.useTactic(0, 1, "spear-thrust")); eq(h(11, 1), engine.unit(1).position);
        });
        run("halberd hook pulls an aligned target into the gap", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.HALBERD, 1, 1), sword(1, 1, 3, 1)); int energy = engine.unit(0).energy;
            ok(engine.useTactic(0, 1, "halberd-hook")); eq(h(2, 1), engine.unit(1).position); eq(energy - 15, engine.unit(0).energy);
        });
        run("halberd hook does not pull into occupied gap", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.HALBERD, 1, 1), sword(1, 1, 3, 1), sword(2, 0, 2, 1));
            ok(engine.useTactic(0, 1, "halberd-hook")); eq(h(3, 1), engine.unit(1).position); eq(2, engine.unitAt(h(2, 1)).id);
        });
        run("crossbow volley deals enhanced damage and spends energy", () -> {
            BattleEngine normal = active(u(0, 0, WeaponType.CROSSBOW, 1, 1), sword(1, 1, 4, 1));
            BattleEngine volley = active(u(0, 0, WeaponType.CROSSBOW, 1, 1), sword(1, 1, 4, 1));
            int damage = normal.attack(0, 1).damage; CommandResult hit = volley.useTactic(0, 1, "crossbow-volley"); ok(hit);
            check(hit.damage > damage, "Volley bonus"); eq(66, volley.unit(0).energy); eq(h(4, 1), volley.unit(1).position);
        });
        run("cavalry charge pushes target and follows into vacated cell", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.CAVALRY, 1, 1), sword(1, 1, 2, 1));
            ok(engine.useTactic(0, 1, "cavalry-charge")); eq(h(2, 1), engine.unit(0).position); eq(h(3, 1), engine.unit(1).position);
            eq(0, engine.unitAt(h(2, 1)).id); eq(1, engine.unitAt(h(3, 1)).id); eq(66, engine.unit(0).energy);
        });
        run("cavalry blocked charge does not follow or overlap", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.CAVALRY, 1, 1), sword(1, 1, 2, 1), sword(2, 0, 3, 1));
            ok(engine.useTactic(0, 1, "cavalry-charge")); eq(h(1, 1), engine.unit(0).position); eq(h(2, 1), engine.unit(1).position);
        });
        run("cavalry charge condition rejects forest and shallows without costs", () -> {
            for (Terrain footing : Arrays.asList(Terrain.FOREST, Terrain.SHALLOW)) {
                Battlefield field = Battlefield.rectangle(6, 4, Terrain.PLAIN).withTerrain(h(1, 1), footing);
                BattleEngine engine = custom(field, 3, 10, u(0, 0, WeaponType.CAVALRY, 1, 1), sword(1, 1, 2, 1));
                ok(engine.startTurn()); String before = fingerprint(engine); error(TACTIC_CONDITION, engine.useTactic(0, 1, "cavalry-charge")); eq(before, fingerprint(engine));
            }
        });
        run("cavalry follows a killed target without resurrecting it", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.CAVALRY, 1, 1), troops(1, 1, WeaponType.SWORD, 2, 1, 1));
            ok(engine.useTactic(0, 1, "cavalry-charge")); eq(h(2, 1), engine.unit(0).position); eq(0, engine.unit(1).troopCount);
            eq(0, engine.unitAt(h(2, 1)).id); check(engine.result().isPresent(), "Elimination follows charge effects");
        });
        run("confusion skips exactly the next owner turn not caster endTurn", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.SPEAR, 1, 1), sword(1, 1, 2, 1));
            ok(engine.useTactic(0, 1, "spear-thrust")); ok(engine.endTurn());
            eq(1, engine.unit(1).status.get(StatusEffect.Kind.CONFUSED).remainingOwnerTurns);
            ok(engine.startTurn()); check(engine.unit(1).actedThisTurn, "Confused unit starts unable to act");
            error(STATUS_BLOCKED, engine.move(1, h(4, 1))); error(STATUS_BLOCKED, engine.attack(1, 0));
            error(STATUS_BLOCKED, engine.useTactic(1, 0, "spear-thrust")); check(engine.reachableTiles(1).isEmpty(), "No movement while confused");
            ok(engine.endTurn()); check(engine.unit(1).status.isEmpty(), "Expires at affected owner's end");
            ok(engine.startTurn()); ok(engine.endTurn()); ok(engine.startTurn()); check(!engine.unit(1).actedThisTurn, "Recovers next owner turn");
        });
        run("two-turn status duration and refresh use max not stacking", () -> {
            BattleUnit affected = sword(0, 0, 1, 1).withStatus(new StatusEffect(StatusEffect.Kind.CONFUSED, 2))
                    .withStatus(new StatusEffect(StatusEffect.Kind.CONFUSED, 1));
            eq(2, affected.status.get(StatusEffect.Kind.CONFUSED).remainingOwnerTurns); eq(0.8, affected.defenseStatusMultiplier());
            BattleEngine engine = active(affected, sword(1, 1, 8, 1)); ok(engine.endTurn()); eq(1, engine.unit(0).status.get(StatusEffect.Kind.CONFUSED).remainingOwnerTurns);
            ok(engine.startTurn()); ok(engine.endTurn()); ok(engine.startTurn()); check(engine.unit(0).actedThisTurn, "Second skipped turn");
            ok(engine.endTurn()); check(engine.unit(0).status.isEmpty(), "Status removed after two own turns");
        });
        run("immobilized unit cannot move but can attack", () -> {
            BattleEngine engine = active(sword(0, 0, 1, 1).withStatus(new StatusEffect(StatusEffect.Kind.IMMOBILIZED, 1)), sword(1, 1, 2, 1));
            eq(0, engine.unit(0).movementRemaining); error(STATUS_BLOCKED, engine.move(0, h(1, 2))); ok(engine.attack(0, 1));
        });
        run("custom tactic strategy works without engine modifications", () -> {
            Tactic custom = new Tactic() {
                private final Definition definition = new Definition("test-bind", "Bind", WeaponType.SWORD, 10, 1, 1, 1);
                public Definition definition() { return definition; }
                public void specialEffects(Context context, Effects effects, Random random) { effects.applyStatus(new StatusEffect(StatusEffect.Kind.IMMOBILIZED, 2)); }
            };
            List<Tactic> registry = new ArrayList<>(Tactics.standard().all()); registry.add(custom);
            BattleEngine engine = new BattleEngine(Battlefield.rectangle(8, 4, Terrain.PLAIN), Arrays.asList(sword(0, 0, 1, 1), sword(1, 1, 2, 1)), aiForces(), Collections.emptyList(), new Tactics(registry), 3, 10);
            ok(engine.startTurn()); ok(engine.useTactic(0, 1, "test-bind")); eq(2, engine.unit(1).status.get(StatusEffect.Kind.IMMOBILIZED).remainingOwnerTurns);
            eq(76, engine.unit(0).energy);
            rejects(IllegalArgumentException.class, () -> new Tactics(Arrays.asList(custom, custom)));
            rejects(IllegalArgumentException.class, () -> new Tactic.Definition("bad", "Bad", WeaponType.SWORD, 0, 1, 1, 1));
        });
        run("unit death frees occupancy and cannot be attacked or act again", () -> {
            BattleEngine engine = active(sword(0, 0, 1, 1), troops(1, 1, WeaponType.SWORD, 2, 1, 1), sword(2, 0, 2, 2), sword(3, 1, 9, 1));
            eq(1, engine.attack(0, 1).damage); check(engine.unitAt(h(2, 1)) == null, "Dead unit does not occupy tile");
            eq(0, engine.unit(1).troopCount); error(DEAD_UNIT, engine.attack(2, 1)); ok(engine.move(2, h(2, 1)));
            ok(engine.endTurn()); ok(engine.startTurn()); error(DEAD_UNIT, engine.endUnitAction(1));
        });
        run("elimination result conserves troops and is immutable", () -> {
            BattleEngine engine = active(sword(0, 0, 1, 1), troops(1, 1, WeaponType.SWORD, 2, 1, 1)); ok(engine.attack(0, 1));
            BattleResult result = engine.result().get(); eq(0, result.winner); eq(BattleResult.Reason.ELIMINATION, result.reason);
            eq(5000, result.survivingTroops.get(0)); eq(0, result.survivingTroops.get(1)); eq(1, result.casualties.get(1));
            eq(Collections.singletonList(1), result.defeatedUnits); eq(Collections.singletonList(101), result.capturedCommanderCandidates);
            check(!result.cityCaptured, "No objective captured"); rejects(UnsupportedOperationException.class, () -> result.casualties.clear());
            error(WRONG_PHASE, engine.startTurn()); error(WRONG_PHASE, engine.endTurn()); error(WRONG_PHASE, engine.attack(0, 1));
        });
        run("critical stronghold capture wins with enemy units still alive", () -> {
            Battlefield field = Battlefield.rectangle(12, 4, Terrain.PLAIN).withTerrain(h(2, 1), Terrain.CITY);
            BattleEngine engine = new BattleEngine(field, Arrays.asList(sword(0, 0, 1, 1), sword(1, 1, 9, 1)), aiForces(),
                    Collections.singletonList(new Stronghold(70, 1, h(2, 1), 1, true)), Tactics.standard(), 3, 10);
            ok(engine.startTurn()); error(OCCUPIED, engine.move(0, h(2, 1))); check(!engine.reachableTiles(0).containsKey(h(2, 1)), "Objective occupies cell");
            error(UNKNOWN_STRONGHOLD, engine.siege(0, 999)); ok(engine.siege(0, 70));
            BattleResult result = engine.result().get(); eq(0, result.winner); eq(BattleResult.Reason.CRITICAL_STRONGHOLD_CAPTURED, result.reason);
            check(result.cityCaptured && engine.unit(1).alive(), "Objective victory does not require enemy annihilation"); eq(0, result.capturedCities.get(70));
        });
        run("noncritical stronghold capture changes ownership without ending battle", () -> {
            Battlefield field = Battlefield.rectangle(6, 4, Terrain.PLAIN).withTerrain(h(2, 1), Terrain.PASS);
            BattleEngine engine = new BattleEngine(field, Arrays.asList(sword(0, 0, 1, 1), sword(1, 1, 3, 1)), aiForces(),
                    Collections.singletonList(new Stronghold(70, 1, h(2, 1), 1, false)), Tactics.standard(), 3, 10);
            ok(engine.startTurn()); ok(engine.siege(0, 70)); check(!engine.result().isPresent(), "Noncritical capture continues battle"); eq(0, engine.strongholds().get(0).forceId);
            ok(engine.endTurn()); ok(engine.startTurn()); ok(engine.siege(1, 70)); eq(1, engine.strongholds().get(0).forceId);
        });
        run("siege rejects friendly distant and invalid objectives", () -> {
            Battlefield field = Battlefield.rectangle(8, 4, Terrain.PLAIN).withTerrain(h(6, 1), Terrain.CITY);
            BattleEngine engine = new BattleEngine(field, Arrays.asList(sword(0, 0, 1, 1), sword(1, 1, 7, 1)), aiForces(),
                    Collections.singletonList(new Stronghold(70, 1, h(6, 1), 1000, true)), Tactics.standard(), 3, 10);
            ok(engine.startTurn()); error(OUT_OF_RANGE, engine.siege(0, 70)); ok(engine.endTurn()); ok(engine.startTurn()); error(FRIENDLY_TARGET, engine.siege(1, 70));
            rejects(IllegalArgumentException.class, () -> new BattleEngine(field, Arrays.asList(sword(0, 0, 1, 1), sword(1, 1, 7, 1)), aiForces(),
                    Collections.singletonList(new Stronghold(70, 1, h(2, 1), 1000, true)), Tactics.standard(), 3, 10));
        });
        run("round limit gives explicit draw instead of infinite loop", () -> {
            BattleEngine engine = custom(Battlefield.rectangle(12, 4, Terrain.PLAIN), 3, 1, sword(0, 0, 1, 1), sword(1, 1, 10, 1));
            ok(engine.startTurn()); ok(engine.endTurn()); ok(engine.startTurn()); ok(engine.endTurn());
            BattleResult result = engine.result().get(); eq(null, result.winner); eq(BattleResult.Reason.TURN_LIMIT, result.reason);
            eq(1, result.completedRounds); check(result.capturedCommanderCandidates.isEmpty(), "Draw does not invent captures");
        });
    }
}

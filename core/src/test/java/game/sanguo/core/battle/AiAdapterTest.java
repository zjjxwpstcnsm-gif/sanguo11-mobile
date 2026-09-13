package game.sanguo.core.battle;

import game.sanguo.core.Hex;
import game.sanguo.core.World;
import game.sanguo.core.battle.adapter.BattleAdapters;
import game.sanguo.core.battle.adapter.LegacyWorldBattleAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static game.sanguo.core.battle.BattleTestSupport.*;

final class AiAdapterTest {
    static void runAll() {
        run("AI selects closest attackable enemy and avoids pointless movement", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.SWORD, 1, 1), u(1, 1, WeaponType.SWORD, 9, 1), u(2, 1, WeaponType.SWORD, 2, 1));
            BattleAi.Decision decision = new BattleAi(1, 0).decide(engine, 0);
            eq(BattleAi.Action.ATTACK, decision.action); eq(2, decision.targetId);
        });
        run("AI uses eligible tactic according to probability and energy", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.SPEAR, 1, 1), u(1, 1, WeaponType.SWORD, 2, 1));
            BattleAi.Decision tactic = new BattleAi(1, 1).decide(engine, 0);
            eq(BattleAi.Action.TACTIC, tactic.action); eq("spear-thrust", tactic.tacticId);
            eq(BattleAi.Action.ATTACK, new BattleAi(1, 0).decide(engine, 0).action);
            BattleEngine low = active(u(0, 0, WeaponType.SPEAR, 1, 1).withEnergy(0), u(1, 1, WeaponType.SWORD, 2, 1));
            eq(BattleAi.Action.ATTACK, new BattleAi(1, 1).decide(low, 0).action);
            rejects(IllegalArgumentException.class, () -> new BattleAi(0, Double.NaN));
        });
        run("AI approaches a legal attack tile then attacks in same turn", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.SWORD, 0, 1), u(1, 1, WeaponType.SWORD, 4, 1));
            BattleAi ai = new BattleAi(3, 0); BattleAi.Decision move = ai.decide(engine, 0);
            eq(BattleAi.Action.MOVE, move.action); check(!move.destination.equals(engine.unit(1).position), "Does not path into occupied enemy");
            ok(ai.execute(engine, move)); eq(1, engine.unit(0).position.distance(engine.unit(1).position));
            BattleAi.Decision hit = ai.decide(engine, 0); eq(BattleAi.Action.ATTACK, hit.action); ok(ai.execute(engine, hit));
        });
        run("AI crossbow retreats out of blind spot instead of moving closer", () -> {
            BattleEngine engine = active(u(0, 0, WeaponType.CROSSBOW, 2, 2), u(1, 1, WeaponType.SWORD, 3, 2));
            BattleAi ai = new BattleAi(3, 0); BattleAi.Decision decision = ai.decide(engine, 0); eq(BattleAi.Action.MOVE, decision.action);
            ok(ai.execute(engine, decision)); int distance = engine.unit(0).position.distance(engine.unit(1).position);
            check(distance >= 2 && distance <= 3, "Bow seeks firing range"); eq(BattleAi.Action.ATTACK, ai.decide(engine, 0).action);
        });
        run("AI finds a detour across a distant bridge", () -> {
            Battlefield field = Battlefield.rectangle(10, 7, Terrain.PLAIN);
            for (int r = 0; r < 6; r++) field = field.withTerrain(h(4, r), Terrain.RIVER);
            field = field.withTerrain(h(4, 6), Terrain.SHALLOW);
            BattleEngine engine = custom(field, 7, 60, u(0, 0, WeaponType.SWORD, 1, 1), u(1, 1, WeaponType.SWORD, 8, 1));
            BattleResult result = BattleSimulation.run(engine, 7); check(result.winner != null, "Detour battle must reach a real winner");
            check(result.casualties.get(0) + result.casualties.get(1) > 0, "Armies met and fought");
        });
        run("AI tries accessible enemies when nearest enemy is isolated", () -> {
            Battlefield field = Battlefield.rectangle(12, 10, Terrain.PLAIN);
            for (HexPos neighbor : h(4, 2).neighbors()) field = field.withTerrain(neighbor, Terrain.RIVER);
            BattleEngine engine = custom(field, 7, 60, u(0, 0, WeaponType.SWORD, 1, 2), u(1, 1, WeaponType.SWORD, 4, 2), u(2, 1, WeaponType.SWORD, 10, 6));
            ok(engine.startTurn()); BattleAi.Decision decision = new BattleAi(1, 0).decide(engine, 0);
            eq(BattleAi.Action.MOVE, decision.action); ok(new BattleAi(1, 0).execute(engine, decision));
        });
        run("AI waits on disconnected maps and finite simulation returns draw", () -> {
            Battlefield field = Battlefield.rectangle(7, 4, Terrain.PLAIN);
            for (int r = 0; r < 4; r++) field = field.withTerrain(h(3, r), Terrain.RIVER);
            BattleEngine engine = custom(field, 3, 3, u(0, 0, WeaponType.SWORD, 1, 1), u(1, 1, WeaponType.SWORD, 5, 1));
            ok(engine.startTurn()); eq(BattleAi.Action.WAIT, new BattleAi(1).decide(engine, 0).action);
            BattleResult result = BattleSimulation.run(engine, 3); eq(BattleResult.Reason.TURN_LIMIT, result.reason); eq(null, result.winner);
            eq(h(1, 1), engine.unit(0).position); eq(h(5, 1), engine.unit(1).position); eq(0, result.casualties.get(0));
        });
        run("AI playTurn owns exactly one turn and rejects player control", () -> {
            BattleEngine engine = custom(Battlefield.rectangle(12, 4, Terrain.PLAIN), 3, 10, u(0, 0, WeaponType.SWORD, 1, 1), u(1, 1, WeaponType.SWORD, 10, 1));
            check(new BattleAi(3).playTurn(engine) > 0, "AI issued commands"); eq(1, engine.expectedForceId()); eq(BattleEngine.Phase.AWAITING_TURN, engine.phase());
            BattleEngine player = engine(u(0, 0, WeaponType.SWORD, 1, 1), u(1, 1, WeaponType.SWORD, 8, 1));
            rejects(IllegalStateException.class, () -> new BattleAi(3).playTurn(player));
        });
        run("AI can capture an adjacent critical objective", () -> {
            Battlefield field = Battlefield.rectangle(12, 4, Terrain.PLAIN).withTerrain(h(2, 1), Terrain.CITY);
            BattleEngine engine = new BattleEngine(field, Arrays.asList(u(0, 0, WeaponType.SWORD, 1, 1), u(1, 1, WeaponType.SWORD, 9, 1)), aiForces(),
                    Collections.singletonList(new Stronghold(70, 1, h(2, 1), 1, true)), Tactics.standard(), 3, 10);
            new BattleAi(3).playTurn(engine); eq(0, engine.result().get().winner); check(engine.result().get().cityCaptured, "AI objective victory");
        });
        run("complete ten-unit battle deterministic golden regression", () -> {
            BattleEngine first = BattleSimulation.demo(31104), repeated = BattleSimulation.demo(31104);
            BattleResult result = BattleSimulation.run(first, 31104L ^ 0x5DEECE66DL);
            BattleResult copy = BattleSimulation.run(repeated, 31104L ^ 0x5DEECE66DL);
            eq(0, result.winner); eq(BattleResult.Reason.ELIMINATION, result.reason); eq(13, result.completedRounds);
            eq(result.survivingTroops, copy.survivingTroops); eq(result.casualties, copy.casualties); eq(fingerprint(first), fingerprint(repeated));
            eq(38, result.survivingTroops.get(0)); eq(517, result.survivingTroops.get(1)); eq(3883, result.survivingTroops.get(2)); eq(5000, result.survivingTroops.get(3));
            for (BattleUnit unit : first.units()) {
                int initial = unit.forceId == 0 ? 5000 : 4500;
                eq(initial, result.survivingTroops.get(unit.id) + result.casualties.get(unit.id));
            }
        });
        run("simulation is independent of input collection order", () -> {
            BattleEngine original = BattleSimulation.demo(42); List<BattleUnit> reversed = new ArrayList<>(original.units()); Collections.reverse(reversed);
            BattleEngine reordered = new BattleEngine(original.battlefield, reversed, aiForces(), Collections.emptyList(), Tactics.standard(), 42, BattleRules.MAX_ROUNDS);
            BattleSimulation.run(original, 17); BattleSimulation.run(reordered, 17); eq(fingerprint(original), fingerprint(reordered));
        });
        run("seed sweep produces complete battles and conserved casualties", () -> {
            for (long seed = 0; seed < 12; seed++) {
                BattleEngine engine = BattleSimulation.demo(seed); BattleResult result = BattleSimulation.run(engine, seed + 1000);
                check(result.winner != null, "Open battlefield should not time out, seed=" + seed);
                for (BattleUnit unit : engine.units()) {
                    check(unit.troopCount >= 0, "No negative troops");
                    eq(unit.forceId == 0 ? 5000 : 4500, result.survivingTroops.get(unit.id) + result.casualties.get(unit.id));
                    if (unit.forceId != result.winner) eq(0, unit.troopCount);
                }
            }
        });
        run("random command fuzz preserves invariants and rejected-command atomicity", () -> {
            String[] tactics = {"spear-thrust", "halberd-hook", "crossbow-volley", "cavalry-charge", "missing"};
            for (int seed = 0; seed < 8; seed++) {
                Random random = new Random(seed);
                BattleEngine engine = engine(u(0, 0, WeaponType.SWORD, 1, 1), u(1, 1, WeaponType.SPEAR, 4, 1),
                        u(2, 0, WeaponType.CROSSBOW, 1, 3), u(3, 1, WeaponType.CAVALRY, 7, 3));
                for (int i = 0; i < 500; i++) {
                    String before = fingerprint(engine); CommandResult result;
                    int actor = random.nextInt(6), target = random.nextInt(6);
                    switch (random.nextInt(7)) {
                        case 0: result = engine.move(actor, h(random.nextInt(15) - 1, random.nextInt(12) - 1)); break;
                        case 1: result = engine.attack(actor, target); break;
                        case 2: result = engine.useTactic(actor, target, tactics[random.nextInt(tactics.length)]); break;
                        case 3: result = engine.endUnitAction(actor); break;
                        case 4: result = engine.startTurn(random.nextInt(3)); break;
                        case 5: result = engine.endTurn(); break;
                        default: result = engine.startTurn();
                    }
                    if (!result.ok) eq(before, fingerprint(engine));
                    Set<HexPos> occupied = new HashSet<>();
                    for (BattleUnit unit : engine.units()) {
                        check(unit.troopCount >= 0 && unit.troopCount <= 5000, "Troops bounded");
                        check(unit.energy >= 0 && unit.energy <= 100, "Energy bounded");
                        check(unit.movementRemaining >= 0 && unit.movementRemaining <= unit.movement, "Movement bounded");
                        if (unit.alive()) {
                            check(occupied.add(unit.position), "No overlapping live units"); eq(unit.id, engine.unitAt(unit.position).id);
                            check(engine.battlefield.passable(unit.position, unit.weaponType), "Unit stays on legal terrain");
                        }
                    }
                }
            }
        });
        run("strategic DTO and result adapters preserve ids and resource counts", () -> {
            BattleUnit.Commander commander = new BattleUnit.Commander(99, "Strategic officer", 91, 87, 83);
            BattleAdapters.StrategicArmy dto = new BattleAdapters.StrategicArmy(7, 0, commander, WeaponType.SPEAR, 4321, 57, 105, 102);
            BattleUnit converted = BattleAdapters.toBattleUnit(dto, h(1, 1));
            eq(7, converted.id); eq(0, converted.forceId); eq(4321, converted.troopCount); eq(57, converted.energy); eq(99, converted.commander.id);
            eq(105, converted.attack); eq(102, converted.defense); eq(h(1, 1), converted.position);
            BattleEngine engine = active(converted, troops(8, 1, WeaponType.SWORD, 2, 1, 1)); ok(engine.attack(7, 8));
            BattleAdapters.StrategicResult result = BattleAdapters.toStrategicResult(engine.result().get());
            eq(0, result.winner); eq(4321, result.survivingTroops.get(7)); eq(1, result.casualties.get(8));
            eq(Collections.singletonList(108), result.capturedCommanderCandidates); check(!result.cityCaptured, "No city invented by adapter");
        });
        run("legacy World adapter reads existing officer and army without mutation", () -> {
            World.Officer officer = new World.Officer(40, "Officer", 0, 10, 90, 88, 70, 60, 60);
            World.Unit unit = new World.Unit(20, 0, 40, World.Weapon.CAVALRY, new Hex(20, 10), 4000, 8000); unit.energy = 42;
            BattleUnit converted = LegacyWorldBattleAdapter.toBattleUnit(unit, officer, h(1, 1));
            eq(20, converted.id); eq(40, converted.commander.id); eq(90, converted.commander.leadership); eq(88, converted.commander.war);
            eq(WeaponType.CAVALRY, converted.weaponType); eq(42, converted.energy); eq(4000, converted.troopCount);
            eq(20, unit.hex.q); eq(8000, unit.food); eq(4000, unit.troops); eq(10, officer.cityId);
            World.Officer wrong = new World.Officer(41, "Wrong", 0, 10, 80, 80, 80, 80, 80);
            rejects(IllegalArgumentException.class, () -> LegacyWorldBattleAdapter.toBattleUnit(unit, wrong, h(1, 1)));
            for (World.Weapon weapon : World.Weapon.values()) {
                World.Unit source = new World.Unit(20, 0, 40, weapon, new Hex(20, 10), 4000, 8000);
                eq(weapon.name(), LegacyWorldBattleAdapter.toBattleUnit(source, officer, h(1, 1)).weaponType.name());
            }
        });
    }
}

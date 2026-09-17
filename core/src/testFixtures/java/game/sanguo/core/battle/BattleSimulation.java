package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Headless entry point: real AI-vs-AI battles, with a finite round limit enforced by the engine. */
public final class BattleSimulation {
    private BattleSimulation() { }
    public static BattleResult run(BattleEngine engine, long aiSeed) {
        BattleAi ai = new BattleAi(aiSeed);
        while (engine.phase() != BattleEngine.Phase.FINISHED) ai.playTurn(engine);
        return engine.result().get();
    }
    public static BattleEngine demo(long seed) {
        Battlefield field = Battlefield.rectangle(14, 10, Terrain.PLAIN)
                .withTerrain(new HexPos(6, 4), Terrain.FOREST)
                .withTerrain(new HexPos(7, 5), Terrain.FOREST);
        List<BattleUnit> armies = new ArrayList<>();
        WeaponType[] weapons = {WeaponType.SWORD, WeaponType.SPEAR, WeaponType.HALBERD, WeaponType.CROSSBOW, WeaponType.CAVALRY};
        for (int force = 0; force < 2; force++) for (int i = 0; i < weapons.length; i++) {
            int id = force * 10 + i;
            armies.add(new BattleUnit(id, force, new BattleUnit.Commander(id, "Commander " + id,
                    force == 0 ? 88 : 74, 80, 72), weapons[i], new HexPos(force == 0 ? 1 : 12, i + 2),
                    force == 0 ? 5000 : 4500, 80, 100, 100));
        }
        return new BattleEngine(field, armies, Arrays.asList(new BattleEngine.Force(0, BattleEngine.Control.AI),
                new BattleEngine.Force(1, BattleEngine.Control.AI)), Collections.emptyList(), Tactics.standard(), seed, BattleRules.MAX_ROUNDS);
    }
    public static void main(String[] args) {
        long seed = args.length == 0 ? 31104L : Long.parseLong(args[0]);
        BattleResult result = run(demo(seed), seed ^ 0x5DEECE66DL);
        System.out.println("winner=" + result.winner + " reason=" + result.reason + " completedRounds=" + result.completedRounds);
        System.out.println("survivingTroops=" + result.survivingTroops);
        System.out.println("casualties=" + result.casualties);
    }
}

package game.sanguo.core.battle;

/** Dependency-free JVM regression entry point, matching existing core test conventions. */
public final class TacticalBattleTest {
    public static void main(String[] args) {
        GeometryPathTest.runAll();
        CombatTurnTest.runAll();
        AiAdapterTest.runAll();
        System.out.println("PASS: " + BattleTestSupport.cases + " tactical cases, " + BattleTestSupport.assertions
                + " assertions covering hex, terrain, paths, combat, tactics, status, turns, AI, objectives, adapters and simulation.");
    }
}

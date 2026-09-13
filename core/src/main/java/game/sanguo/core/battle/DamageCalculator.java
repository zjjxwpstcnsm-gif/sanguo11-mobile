package game.sanguo.core.battle;

import java.util.Objects;
import java.util.Random;

/** Pure calculation except for the explicitly supplied RNG; validation/preview never rolls it. */
public final class DamageCalculator {
    private DamageCalculator() { }
    public static int damage(BattleUnit attacker, BattleUnit defender, Terrain attackTerrain,
                             Terrain defenseTerrain, double tacticMultiplier, Random random) {
        Objects.requireNonNull(random);
        if (!attacker.alive() || !defender.alive()) throw new IllegalArgumentException("Dead combatant");
        if (!Double.isFinite(tacticMultiplier) || tacticMultiplier <= 0 || tacticMultiplier > 3)
            throw new IllegalArgumentException("Damage multiplier must be >0 and <=3");
        double offense = offense(attacker, attackTerrain);
        double defense = (70 + defender.defense + 0.85 * defender.commander.leadership
                + 0.15 * defender.commander.intelligence) / 250.0;
        defense *= BattleRules.weapon(defender.weaponType).defense;
        defense *= BattleRules.terrain(defenseTerrain).defense * defender.defenseStatusMultiplier();
        double ratio = BattleRules.clamp(offense / defense, 0.35, 2.5);
        double raw = BattleRules.BASE_DAMAGE * StrictMath.sqrt(attacker.troopCount / BattleRules.TROOP_SCALE)
                * ratio * BattleRules.matchup(attacker.weaponType, defender.weaponType) * tacticMultiplier;
        return Math.min(defender.troopCount, boundedRoll(raw, random));
    }
    public static int siegeDamage(BattleUnit attacker, Terrain attackTerrain, Stronghold target, Random random) {
        if (!attacker.alive()) throw new IllegalArgumentException("Dead attacker");
        double raw = BattleRules.BASE_DAMAGE * StrictMath.sqrt(attacker.troopCount / BattleRules.TROOP_SCALE)
                * offense(attacker, attackTerrain) * BattleRules.SIEGE_DAMAGE_SCALE;
        return Math.min(target.durability, boundedRoll(raw, random));
    }
    private static double offense(BattleUnit attacker, Terrain terrain) {
        double offense = (50 + attacker.attack + 0.60 * attacker.commander.leadership
                + 0.40 * attacker.commander.war) / 200.0;
        offense *= BattleRules.weapon(attacker.weaponType).attack * BattleRules.terrain(terrain).attack;
        offense *= (0.70 + 0.30 * attacker.energy / BattleRules.MAX_ENERGY) * attacker.attackStatusMultiplier();
        return offense;
    }
    private static int boundedRoll(double raw, Random random) {
        double variance = 1 - BattleRules.VARIANCE + 2 * BattleRules.VARIANCE * random.nextDouble();
        return (int) Math.max(BattleRules.MIN_DAMAGE, Math.min(BattleRules.MAX_DAMAGE, Math.round(raw * variance)));
    }
}

package game.sanguo.core.strategy;

/** Pure, bounded engineering formulas. These are not claims about original SAN11 balance. */
public final class StrategyRules {
    private StrategyRules() {}
    public static int stat(int value) { return Math.max(0, Math.min(100, value)); }
    public static int searchChance(int politics, int intelligence) {
        return 5 + (45 * stat(politics) + 45 * stat(intelligence)) / 100;
    }
    /** Relationship is an optional -100..100 scenario modifier; neutral is zero. */
    public static int recruitmentChance(int charm, int politics, int loyalty,
                                        boolean unaffiliated, int relationship) {
        int relationshipBonus = Math.max(-100, Math.min(100, relationship)) / 5;
        int value = 15 + stat(charm) / 2 + stat(politics) / 4 + relationshipBonus;
        value += unaffiliated ? 10 : 15 - stat(loyalty);
        return Math.max(0, Math.min(95, value));
    }
    public static int rewardGain(int targetPolitics, int targetCharm) {
        return 6 + (100 - stat(targetPolitics)) / 25 + (100 - stat(targetCharm)) / 25;
    }
    public static int patrolGain(int politics, int charm) {
        return 5 + (stat(politics) + stat(charm)) / 20;
    }
    public static int trainingGain(int leadership, int war) {
        return 5 + stat(leadership) / 10 + stat(war) / 40;
    }
    /** At order 90 there is no penalty. Governor politics grants up to 25% extra yield. */
    public static int income(int base, int order, int governorPolitics) {
        if (base < 0 || base > 1_000_000) throw new IllegalArgumentException("base income");
        int bonus = governorPolitics < 0 ? 0 : stat(governorPolitics) / 4;
        return (int) ((long) base * (10 + stat(order)) * (100 + bonus) / 10_000);
    }
    /** Barracks determine base capacity; recruitment draws from a finite reserve. */
    public static int enlistment(int base, int order, int charm, int reserve) {
        if (base < 0 || base > 100_000 || reserve < 0 || reserve > 1_000_000)
            throw new IllegalArgumentException("recruitment capacity");
        int appeal = 60 + Math.min(80, stat(charm)) / 2;
        return Math.min(reserve, (int) ((long) base * appeal * (10 + stat(order)) / 10_000));
    }
    public static boolean succeeds(int chance, int roll) {
        if (chance < 0 || chance > 100 || roll < 0 || roll >= 100)
            throw new IllegalArgumentException("chance / roll");
        return roll < chance;
    }
}

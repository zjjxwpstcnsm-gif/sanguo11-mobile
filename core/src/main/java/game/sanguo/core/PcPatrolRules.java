package game.sanguo.core;

/** Pinned PC5cba10 arithmetic, verified by executing original instructions.
 * Inputs are current runtime leadership, not politics/charm. Full PC ability
 * modifiers and the three-officer command remain separate integration work. */
final class PcPatrolRules {
    private PcPatrolRules() {}
    static int gain(int leadershipSum,int order,boolean nearbyEnemy) {
        if(leadershipSum<0||leadershipSum>765||order<0||order>100)
            throw new IllegalArgumentException("巡察能力或治安越界");
        int gain=leadershipSum/28+2;
        if(nearbyEnemy)gain/=2;
        return Math.min(100-order,gain);
    }
}

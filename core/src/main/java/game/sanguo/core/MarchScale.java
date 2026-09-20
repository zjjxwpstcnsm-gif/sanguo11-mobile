package game.sanguo.core;

/** Engineering map-scale policy, NOT a claim about an original SAN11 movement formula.
 * Costs, attack ranges, skill/technology bonuses and ten-day ration rules are unchanged.
 * Retired 100x100 mobile base budgets were calibrated for its compressed geography.
 * Only the native national map (including its crops) uses recalibrated base budgets. */
public final class MarchScale {
    private MarchScale(){}
    public static int base(World world,int mobileBase){
        return NationalMap.ID.equals(world.mapId)&&NationalMap.REVISION==world.mapRevision?Math.multiplyExact(mobileBase,2):mobileBase;
    }
}

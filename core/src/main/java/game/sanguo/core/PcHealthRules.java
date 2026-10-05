package game.sanguo.core;

/** Original580e70 existing-injury branch under explicit lifetime mode3.
 * Original590c30 supplies the month-start gate; RNG ordering is native-ID order. */
final class PcHealthRules {
    static final class Recovery {
        final int injury,seed,draws;
        Recovery(int injury,PcMerchantRules.Random random){this.injury=injury;seed=random.state;draws=random.draws;}
    }
    static Recovery recover(int nativeId,int month,int injury,boolean active,boolean ancientExcluded,boolean statusAllowed,int seed){
        if(nativeId<0||nativeId>1099||month<1||month>12||injury< -1||injury>3)throw new IllegalArgumentException("Original recovery inputs required");
        PcMerchantRules.Random random=new PcMerchantRules.Random(seed);int next=injury;
        if(active&&!ancientExcluded&&statusAllowed&&(nativeId&1)==(month&1)&&injury!=0&&random.percent(100)){
            // Original48a8e0 accepts -1 or0..3; an attempted -2 is rejected.
            if(injury>0)next=injury-1;
        }
        return new Recovery(next,random);
    }
    private PcHealthRules(){}
}

package game.sanguo.core;

/** Pinned original merchant arithmetic. Only merit is wired into gameplay yet;
 * price state, current-ability modifiers and quantity/save integration are pending. */
final class PcMerchantRules {
    private PcMerchantRules() {}
    static final int MERIT_AWARD=50, MERIT_CAP=60000;
    static int meritGain(int before) {
        // Existing project saves allow more than the native cap. Never reduce them.
        return Math.max(0,Math.min(MERIT_AWARD,MERIT_CAP-before));
    }
    static int quote(int signedFood,int politics,int rate) {
        inputs(politics,rate);
        // Original NEG is32bit before sign extension: MIN_VALUE overflows there.
        // This preserves the oracle edge, not permission to trade invalid stock.
        int absolute=-signedFood;
        long gold=signedFood>=0?(long)signedFood*(450-politics)*10/(rate*400L)
                :-(absolute*3200L/((450-politics)*(long)rate));
        return (int)Math.max(Integer.MIN_VALUE,Math.min(Integer.MAX_VALUE,gold));
    }
    static int maximum(boolean buy,int gold,int food,int politics,int rate) {
        inputs(politics,rate);
        if(gold<0||gold>100000||food<0||food>1000000)
            throw new IllegalArgumentException("Native stock domain required");
        return (int)(buy?Math.min(gold*(long)rate*400/((450-politics)*10L),1000000-food)
                :Math.min((100000L-gold)*(450-politics)*rate/3200,Math.max(0,food-1)));
    }
    private static void inputs(int politics,int rate) {
        if(politics<0||politics>255||rate<1||rate>255)
            throw new IllegalArgumentException("Native ability/rate byte domain required");
    }
    static final class Price {
        final int rate,state,draws;
        Price(int rate,Random random){this.rate=rate;state=random.state;draws=random.draws;}
    }
    private static final int[][] MONTHS={{50,10,2},{40,10,3},{40,10,3},{40,10,2},{40,10,2},{30,10,3},
            {60,10,2},{50,10,3},{50,10,3},{50,10,3},{50,10,2},{50,10,2}};
    static Price initial(int month,int priorRate,int seed) {
        Random random=new Random(seed);
        if(month<1||month>12)return new Price(priorRate,random);
        int[] row=MONTHS[month-1];return new Price(row[0]+row[1]*random.uniform(row[2]),random);
    }
    static Price monthly(int rate,int conditionBits,int seed) {
        if(rate<0||rate>255)throw new IllegalArgumentException("Native rate byte domain required");
        Random random=new Random(seed);int next;
        if((conditionBits&3)!=0)next=30+10*random.uniform(2); // Plague or locust, before harvest.
        else if((conditionBits&4)!=0)next=60+10*random.uniform(2);
        else if(rate==30)next=random.percent(50)?30:40;
        else if(rate==70)next=random.percent(50)?60:70;
        else if(rate<=40||rate>=60){
            int direction=rate<=40?10:-10;
            next=random.percent(40)?rate+direction:random.percent(40)?rate-direction:rate;
        } else next=rate+10*random.uniform(3)-10;
        return new Price(Math.max(30,Math.min(70,next)),random);
    }
    static final class Random {
        int state,draws;
        Random(int seed){state=seed;}
        private int next(){state=state*0x6c078965+0x3039;draws++;return state>>>16;}
        int uniform(int bound){return bound<2||(bound&0xffff)==0?0:next()%(bound&0xffff);}
        boolean percent(int chance){return chance>0&&next()%100<chance;}
    }
}

package game.sanguo.core;

import java.util.Arrays;

/** Original award boundaries through ordinary orders, pure forecasts and saved turns. */
public final class PcMerchantMeritTest {
    static int checks;
    static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{
        int[][] cases={{0,50},{1,51},{49,99},{50,100},{100,150},{59900,59950},{59949,59999},
                {59950,60000},{59951,60000},{59999,60000},{60000,60000},
                {60001,60001},{65535,65535},{999999,999999},{1000000,1000000}};
        for(var op:TradePlan.Operation.values())for(int[] v:cases){
            World w=TradePlanTest.fixture();if(v[0]>0)w.government.merits.put(1,v[0]);
            byte[] before=SaveCodec.encode(w);long random=w.strategy.getRandomState();
            var p=w.campaign.previewTrade(10,1,op,1000);
            check(p.allowed()&&p.effects.meritBefore==v[0]&&p.effects.meritAfter==v[1],"native merit forecast or explicit legacy preservation");
            check(Arrays.equals(before,SaveCodec.encode(w)),"forecast has no save/RNG mutation");
            int gold=w.city(10).gold,food=w.city(10).food;
            check(w.campaign.trade(10,1,op==TradePlan.Operation.BUY,1000).ok,"ordinary trade succeeds");
            check(w.government.merit(1)==v[1]&&w.officer(1).acted&&w.actionPoints[0]==40,"one bounded award and normal action debit");
            check(w.city(10).gold==p.effects.goldAfter&&w.city(10).food==p.effects.foodAfter&&gold!=w.city(10).gold&&food!=w.city(10).food,"actual resource transaction retained");
            check(w.strategy.getRandomState()==random,"award does not consume RNG");
            byte[] after=SaveCodec.encode(w);
            check(!w.campaign.trade(10,1,op==TradePlan.Operation.BUY,1000).ok&&Arrays.equals(after,SaveCodec.encode(w)),"duplicate does not award twice");
            World restored=SaveCodec.decode(after);
            check(Arrays.equals(after,SaveCodec.encode(restored)),"save exact round trip, including legacy excess");
            for(int turn=0;turn<3;turn++){
                check(w.nextTurn().ok&&restored.nextTurn().ok,"normal global turn");
                check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"full save/RNG continuation");
            }
        }
        // Other city commands retain their separately modeled awards.
        World other=CityActionPlanTest.fixture();other.city(10).order=50;
        check(other.strategy.patrol(10,1).ok&&other.government.merit(1)==100,"merchant change does not rewrite patrol merit");
        System.out.println("PASS PcMerchantMeritTest checks="+checks);
    }
}

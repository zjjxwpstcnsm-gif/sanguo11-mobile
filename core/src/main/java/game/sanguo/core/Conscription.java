package game.sanguo.core;

/** Authored balance policy: training-weighted energy and bounded quarterly manpower. */
public final class Conscription {
    public static final int RESERVE_CAP=20000, QUARTERLY_RECOVERY=5000;
    private Conscription() {}
    public static int moraleAfter(World.City c,int recruits){
        if(recruits<=0)return c.morale;
        long veterans=Math.max(0,c.troops);
        return (int)(Math.max(0,c.morale)*veterans/(veterans+(long)recruits));
    }
    public static boolean quarterBegins(World w,int turn){
        int month=w.startMonth-1+turn/3;
        return turn>0&&turn%3==0&&month%3==0;
    }
    public static int nextRecoveryIn(World w){
        for(int offset=1;offset<=9;offset++)if(quarterBegins(w,w.turn+offset))return offset;
        throw new IllegalStateException("季度日期超出九旬");
    }
    public static int recovery(World.City c){
        return c.kind==World.SiteKind.CITY?Math.max(0,Math.min(QUARTERLY_RECOVERY,RESERVE_CAP-c.recruitReserve)):0;
    }
    public static String description(World w,World.City c){
        if(c.kind!=World.SiteKind.CITY)return "港关不征兵；兵员由所属城池运输";
        return "兵源 "+c.recruitReserve+" / "+RESERVE_CAP+"；每季首月上旬恢复最多"+QUARTERLY_RECOVERY+
            "，不超过上限；距下次恢复"+nextRecoveryIn(w)+"旬";
    }
    /** Called once from global turn settlement, never from faction resets or save loading. */
    static void settle(World w){
        if(!quarterBegins(w,w.turn))return;
        for(World.City c:w.cities){int gain=recovery(c);if(gain==0)continue;
            c.recruitReserve+=gain;
            w.note(c.name+"季度兵源恢复+"+gain+"（"+c.recruitReserve+"/"+RESERVE_CAP+"）");
        }
    }
}

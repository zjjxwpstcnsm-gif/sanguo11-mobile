package game.sanguo.core;

/** Authored balance policy: training-weighted energy and bounded quarterly manpower. */
public final class Conscription {
    public static final int RESERVE_CAP=20000, QUARTERLY_RECOVERY=5000;
    /** Per completed building, deliberately not multiplied by production/adjacency skills. */
    public static final int FARM_RECOVERY=500, MARKET_CAP=2000;
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
    public static int reserveCap(World w,World.City c){
        return c.kind==World.SiteKind.CITY?RESERVE_CAP+MARKET_CAP*w.domestic.capacity(c.id,Domestic.Kind.MARKET):0;
    }
    public static int quarterlyRecovery(World w,World.City c){return quarterlyRecovery(w,c,SiegeRules.blockaded(w,c));}
    static int quarterlyRecovery(World w,World.City c,boolean blockaded){
        int raw=c.kind==World.SiteKind.CITY?QUARTERLY_RECOVERY+FARM_RECOVERY*w.domestic.capacity(c.id,Domestic.Kind.FARM):0;
        return SiegeRules.adjusted(raw,blockaded?SiegeRules.RECRUIT_PERCENT:100);
    }
    /** Base-policy helper retained for callers without a world; gameplay uses the world-aware overload. */
    public static int recovery(World.City c){
        return c.kind==World.SiteKind.CITY?Math.max(0,Math.min(QUARTERLY_RECOVERY,RESERVE_CAP-c.recruitReserve)):0;
    }
    public static int recovery(World w,World.City c){
        return Math.max(0,Math.min(quarterlyRecovery(w,c),reserveCap(w,c)-c.recruitReserve));
    }
    /** Read-only faction totals; ports/gates have no separate recruiting population. */
    public static final class Summary {
        public final long reserve, cap, quarterlyGrowth, nextGrowth;
        private Summary(long reserve,long cap,long quarterlyGrowth,long nextGrowth){
            this.reserve=reserve;this.cap=cap;this.quarterlyGrowth=quarterlyGrowth;this.nextGrowth=nextGrowth;
        }
    }
    public static Summary summary(World w,int owner){
        long reserve=0,cap=0,growth=0,next=0;
        if(owner>=0&&owner<w.factions.length)for(World.City c:w.cities){
            if(c.owner!=owner||c.kind!=World.SiteKind.CITY)continue;
            reserve+=Math.max(0,c.recruitReserve);cap+=reserveCap(w,c);
            growth+=quarterlyRecovery(w,c);next+=recovery(w,c);
        }
        return new Summary(reserve,cap,growth,next);
    }
    public static String factionDescription(World w,int owner){
        Summary s=summary(w,owner);
        return "兵源 "+s.reserve+" / "+s.cap+" · 季增长 "+s.quarterlyGrowth+
            "（按当前余量可恢复 "+s.nextGrowth+"）";
    }
    public static String description(World w,World.City c){
        if(c.kind!=World.SiteKind.CITY)return "港关不征兵；兵员由所属城池运输";
        return "兵源 "+c.recruitReserve+" / "+reserveCap(w,c)+"；每季首月上旬恢复最多"+quarterlyRecovery(w,c)+
            "；每座农场+"+FARM_RECOVERY+"/季，每座市场上限+"+MARKET_CAP+
            "；距下次恢复"+nextRecoveryIn(w)+"旬";
    }
    /** Called once from global turn settlement, never from faction resets or save loading.
     * Removing a market stops growth above the new cap, but does not delete existing people. */
    static void settle(World w){settle(w,SiegeRules.snapshot(w));}
    static void settle(World w,java.util.Map<Integer,SiegeRules.State> states){
        if(!quarterBegins(w,w.turn))return;
        for(World.City c:w.cities){if(c.owner<0)continue;
            int gain=Math.max(0,Math.min(quarterlyRecovery(w,c,SiegeRules.blocked(states,c)),reserveCap(w,c)-c.recruitReserve));if(gain==0)continue;
            c.recruitReserve+=gain;
            w.note(c.name+"季度兵源恢复+"+gain+"（"+c.recruitReserve+"/"+reserveCap(w,c)+"）");
        }
    }
}

package game.sanguo.core;

import java.util.*;

/** One footprint-aware contract for blockade, defense and the map overlay.
 * Percentages are explicit mobile balance choices, not verified SAN11 formulas. */
public final class SiegeRules {
    public static final int RANGE=2, INCOME_PERCENT=75, RECRUIT_PERCENT=75, ATTRITION_CAP=200;
    private SiegeRules() {}

    /** The two exterior rings; cities have 12+18 cells, single-cell sites 6+12.
     * Clipped to the playable map, never including VOID or the site's own footprint. */
    public static List<Hex> cells(World w,World.City c){
        if(w==null||c==null)return Collections.emptyList();
        Set<Hex> seen=new HashSet<>(SiteFootprint.cells(c));
        Set<Hex> frontier=new HashSet<>(seen);
        List<Hex> area=new ArrayList<>();
        for(int ring=1;ring<=RANGE;ring++){
            Set<Hex> next=new HashSet<>();
            for(Hex h:frontier)for(Hex n:h.neighbors())if(seen.add(n))next.add(n);
            for(Hex h:next)if(playable(w,h))area.add(h);
            frontier=next;
        }
        area.sort(Comparator.comparingInt((Hex h)->SiteFootprint.distance(c,h))
            .thenComparingInt(h->h.r).thenComparingInt(h->h.q));
        return Collections.unmodifiableList(area);
    }
    private static boolean playable(World w,Hex h){
        return h!=null&&w.inside(h)&&w.terrain[h.q][h.r]!=World.Terrain.VOID;
    }
    /** Include any enemy still on a footprint after ownership changes, not just its center. */
    public static boolean inArea(World w,World.City c,Hex h){
        return w!=null&&c!=null&&playable(w,h)&&SiteFootprint.distance(c,h)<=RANGE;
    }
    public static boolean hostile(World w,World.City c,World.Unit u){
        return c!=null&&c.owner>=0&&u!=null&&u.owner>=0&&u.troops>0
            &&w.campaign.hostile(c.owner,u.owner)&&inArea(w,c,u.hex);
    }
    /** Immutable settlement snapshot: a last volley must not erase this turn's blockade. */
    public static final class State {
        public final int owner, enemyUnits;
        public final long enemyTroops;
        private State(int owner,int units,long troops){this.owner=owner;enemyUnits=units;enemyTroops=troops;}
        public boolean besieged(){return enemyUnits>0;}
    }
    public static State state(World w,World.City c){
        int count=0;long troops=0;
        if(w!=null&&c!=null)for(World.Unit u:w.units)
            if(!(u instanceof Domestic.Mission)&&hostile(w,c,u)){count++;troops+=u.troops;}
        return new State(c==null?-1:c.owner,count,troops);
    }
    /** Logistics units can be shot at but cannot blockade a city. No mutable cache. */
    public static boolean blockaded(World w,World.City c){return state(w,c).besieged();}
    static Map<Integer,State> snapshot(World w){
        Map<Integer,State> result=new HashMap<>();
        for(World.City c:w.cities)result.put(c.id,state(w,c));
        return Collections.unmodifiableMap(result);
    }
    static boolean blocked(Map<Integer,State> states,World.City c){
        State s=states.get(c.id);return s!=null&&s.owner==c.owner&&s.besieged();
    }
    /** Bounded mobile balance: <=1% defenders, <=1% besiegers, <=200, never the last soldier.
     * This is siege desertion, not a combat wound and not free wall damage/capture. */
    public static int attrition(World.City c,State state){
        if(c==null||state==null||c.owner!=state.owner||!state.besieged())return 0;
        return (int)Math.max(0,Math.min(Math.min(ATTRITION_CAP,c.troops-1),Math.min(c.troops/100,state.enemyTroops/100)));
    }
    static void settleAttrition(World w,Map<Integer,State> states){
        for(World.City c:w.cities){
            int loss=attrition(c,states.get(c.id));if(loss==0)continue;
            c.troops-=loss;w.note(c.name+"围城逃兵"+loss+"，剩余守军"+c.troops+"（非伤兵）");
        }
    }
    public static List<World.Unit> defendersTargets(World w,World.City c){
        List<World.Unit> result=new ArrayList<>();
        for(World.Unit u:w.fieldUnits())if(hostile(w,c,u))result.add(u);
        result.sort(Comparator.comparingInt(u->u.id));
        return result;
    }
    public static int adjusted(int amount,int percent){
        return (int)(Math.max(0,(long)amount)*Math.max(0,Math.min(100,percent))/100);
    }
    public static int income(World w,World.City c,int amount){
        return adjusted(amount,blockaded(w,c)?INCOME_PERCENT:100);
    }
    public static int recruitment(World w,World.City c,int amount){
        return adjusted(amount,blockaded(w,c)?RECRUIT_PERCENT:100);
    }
    public static String summary(World w,World.City c){
        State s=state(w,c);
        return s.besieged()?"围城中 · 敌军"+s.enemyUnits+"队 / "+s.enemyTroops+"兵 · 钱粮−25%":
            "未受围城 · 占地外两圈警戒";
    }
    public static String description(World w,World.City c){
        if(c==null)return "未选择据点";
        State s=state(w,c);
        return "围城范围：据点实际占地向外两圈（"+cells(w,c).size()+"格）\n"
            +(s.besieged()?summary(w,c)+"；征兵与季度兵源恢复−25%（不按队数叠加）。":
                "未受围城；钱粮与征兵无围城减益。")
            +(c.kind==World.SiteKind.CITY?"":"\n港口、关卡不独立征兵，兵员仍由城池运输。")
            +"\n围城暂停城防自修，手动修复为平时¼；预计本旬围城逃兵"+attrition(c,s)+"。"
            +"\n同盟、停战、运输队不封锁；敌军撤离或被击退后即时解除。"
            +"\n结算在自动射击前锁定一次围城状态，避免射杀最后一队后本旬减益被漏算。"
            +"\n图例：青色为范围，红格为敌军；地图显示与判定使用相同几何。";
    }
}

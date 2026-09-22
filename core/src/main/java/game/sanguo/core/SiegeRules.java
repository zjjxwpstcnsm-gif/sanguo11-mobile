package game.sanguo.core;

import java.util.*;

/** One footprint-aware contract for blockade, defense and the map overlay.
 * Percentages are explicit mobile balance choices, not verified SAN11 formulas. */
public final class SiegeRules {
    public static final int RANGE=2, INCOME_PERCENT=75, RECRUIT_PERCENT=75;
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
    /** Logistics units can be shot at but cannot blockade a city. No mutable cache. */
    public static boolean blockaded(World w,World.City c){
        if(w==null||c==null||c.owner<0)return false;
        for(World.Unit u:w.units)if(!(u instanceof Domestic.Mission)&&hostile(w,c,u))return true;
        return false;
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
    public static String description(World w,World.City c){
        int count=0;long troops=0;
        for(World.Unit u:w.units)if(!(u instanceof Domestic.Mission)&&hostile(w,c,u)){count++;troops+=u.troops;}
        return "围城范围：据点实际占地向外两圈（"+cells(w,c).size()+"格）\n"
            +(count==0?"未受围城；钱粮与征兵无围城减益。":
            "受围城：敌军"+count+"队 / "+troops+"兵；钱粮收入−"+(100-INCOME_PERCENT)+
            "%，征兵与季度兵源恢复−"+(100-RECRUIT_PERCENT)+"%（不按队数叠加）。")
            +"\n同盟、停战、运输队不构成封锁；敌军离开或被击退后即时解除。";
    }
}

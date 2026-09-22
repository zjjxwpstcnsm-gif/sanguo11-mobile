package game.sanguo.core;

/** Fixed administrative parents, independent of which faction currently owns a site.
 * National entries follow this repository's seven full-size scenario rosters. They are
 * authored game data, not a claim of verified original SAN11 rules. Custom maps fall
 * back to nearest CITY across ALL factions, with stable ID tie-breaking.
 */
public final class SiteAffiliation {
    private SiteAffiliation() {}
    private static final int[] NATIONAL_PARENTS = {
        5,15,17,15,17,36,37,37,37,39,
        0,4,5,11,7,7,8,10,10,6,12,15,15,17,17,18,22,22,23,24,
        25,26,26,26,26,27,28,29,30,30,30,31,32,32,35
    };
    public static World.City parent(World w,World.City site) {
        if(site==null)return null;
        if(site.kind==World.SiteKind.CITY)return site;
        if(w.siteParents.containsKey(site.id)){
            World.City parent=w.city(w.siteParents.get(site.id));
            return parent!=null&&parent.kind==World.SiteKind.CITY?parent:null;
        }
        int i=site.id-20042;
        if(i>=0&&i<NATIONAL_PARENTS.length){
            World.City parent=w.city(20000+NATIONAL_PARENTS[i]);
            if(parent!=null&&parent.kind==World.SiteKind.CITY)return parent;
        }
        World.City best=null;int distance=Integer.MAX_VALUE;
        for(World.City c:w.cities)if(c.kind==World.SiteKind.CITY){
            int d=site.hex.distance(c.hex);
            if(d<distance||d==distance&&(best==null||c.id<best.id)){best=c;distance=d;}
        }
        return best;
    }
}

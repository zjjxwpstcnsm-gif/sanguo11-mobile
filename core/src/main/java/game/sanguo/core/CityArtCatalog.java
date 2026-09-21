package game.sanguo.core;

/** Stable native-map visual identities. No legacy city-sprite fallback is allowed. */
public final class CityArtCatalog {
    /** Independent generated-art ABI; unrelated to map or save revisions. */
    public static final int ASSET_REVISION = 63;
    public enum Variant { CAPITAL, LARGE, STANDARD, MOUNTAIN, RIVER, SOUTHERN }
    private CityArtCatalog(){}
    /** Read only: first valid side in authoritative six-neighbor order, stable across reloads.
     * NON_NAVIGABLE_WATER and decorative exterior pixels are intentionally excluded. */
    public static int waterSide(World world,World.City site){
        java.util.List<Hex> neighbors=site.hex.neighbors();
        for(int d=0;d<neighbors.size();d++)if(world.army.water(neighbors.get(d)))return d;
        return -1;
    }
    /** Read only: align passage with the best opposing pair of walkable neighbors. */
    public static int gateAxis(World world,World.City site){
        java.util.List<Hex> neighbors=site.hex.neighbors();int best=0,score=-1;
        for(int d=0;d<3;d++){int candidate=0;
            for(int n:new int[]{d,d+3}){Hex h=neighbors.get(n);if(world.inside(h)&&!world.army.water(h)&&world.cost(h,World.Weapon.SPEAR)>0)candidate++;}
            if(candidate>score){score=candidate;best=d;}
        }
        return best;
    }
    public static String visualKey(World world,World.City site){
        if(site.kind==World.SiteKind.PORT){
            int d=waterSide(world,site);
            if(d<0)return "port"; // new land-side storehouse only; no invented water or pier.
            return (world.columnStaggered?PORT_COLUMN:PORT_ROW)[d];
        }
        if(site.kind==World.SiteKind.GATE){
            int d=gateAxis(world,site);
            return (world.columnStaggered?GATE_COLUMN:GATE_ROW)[d];
        }
        return key(site);
    }
    private static final String[] PORT_COLUMN={"port-s","port-sw","port-nw","port-n","port-ne","port-se"};
    private static final String[] PORT_ROW={"port-e","port-ne","port-nw","port-w","port-sw","port-se"};
    private static final String[] GATE_COLUMN={"gate-ns","gate-nesw","gate-nwse"};
    private static final String[] GATE_ROW={"gate-ew","gate-nesw","gate-nwse"};
    public static Variant variant(int cityId){
        if(cityId<20000||cityId>20041)throw new IllegalArgumentException("全国城市美术ID越界："+cityId);
        if(cityId==20015||cityId==20017)return Variant.CAPITAL;
        switch(cityId){
            case 20006:case 20008:case 20022:case 20039:return Variant.LARGE;
            case 20005:case 20018:case 20019:case 20020:case 20035:case 20036:case 20037:return Variant.MOUNTAIN;
            case 20010:case 20025:case 20026:case 20027:case 20029:case 20030:return Variant.RIVER;
            case 20023:case 20024:case 20031:case 20032:case 20033:case 20034:case 20038:case 20040:case 20041:return Variant.SOUTHERN;
            default:return Variant.STANDARD;
        }
    }
    public static String key(int cityId){
        if(cityId==20015)return "luoyang";
        if(cityId==20017)return "changan";
        return variant(cityId).name().toLowerCase(java.util.Locale.ROOT);
    }
    /** Custom test/import cities use the new STANDARD artwork, never the old atlas. */
    public static String key(World.City site){
        if(site.kind==World.SiteKind.GATE)return "gate";
        if(site.kind==World.SiteKind.PORT)return "port";
        return site.id>=20000&&site.id<=20041?key(site.id):"standard";
    }
}

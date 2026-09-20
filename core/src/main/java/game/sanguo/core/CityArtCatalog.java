package game.sanguo.core;

/** Stable native-map visual identities. No legacy city-sprite fallback is allowed. */
public final class CityArtCatalog {
    /** Asset ABI stays at 56 when terrain-only map revisions advance. */
    public static final int ASSET_REVISION = 56;
    public enum Variant { CAPITAL, LARGE, STANDARD, MOUNTAIN, RIVER, SOUTHERN }
    private CityArtCatalog(){}
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

package game.sanguo.mobile;
import game.sanguo.core.*;

/** S12 fixed national shots through the normal game Surface, all quality/span combinations. */
public final class EnvironmentInstrumentation extends TerrainMaterialInstrumentation {
    Hex site(World.SiteKind kind){for(World.City c:world.cities)if(c.kind==kind)return c.hex;throw new AssertionError("missing site "+kind);}
    @Override Hex[] shots(){return new Hex[]{site(World.SiteKind.CITY),boundary(World.Terrain.FOREST),site(World.SiteKind.GATE),site(World.SiteKind.PORT),boundary(World.Terrain.SAND)};}
    @Override String[] names(){return new String[]{"city","forest","gate","port","sand"};}
    @Override String stage(){return "S12";}
    @Override void extra(FilamentMapView spatial)throws Exception{
        check(field(spatial,"skyLight")!=null,"shared native sky irradiance loaded");
        check(field(spatial,"siteMaterial")!=null,"lit object material loaded");
        check(field(spatial,"waterMaterial")!=null,"S11 water preserved");
        runOnMainSync(()->spatial.resume(false));settle();runOnMainSync(()->spatial.resume(true));settle();ready();
    }
}

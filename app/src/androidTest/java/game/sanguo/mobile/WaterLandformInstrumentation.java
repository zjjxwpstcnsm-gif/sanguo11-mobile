package game.sanguo.mobile;
import game.sanguo.core.*;

/** Real same-source native captures; uses the existing material quality/LOD/picking assertions. */
public final class WaterLandformInstrumentation extends TerrainMaterialInstrumentation {
    Hex site(World.SiteKind kind){for(World.City c:world.cities)if(c.kind==kind)return c.hex;throw new AssertionError("Missing "+kind);}
    Hex water(boolean sea,boolean edge){
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(world);
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){
            Hex h=new Hex(q,r);if(!g.surface.water(h))continue;
            if(sea&&world.terrain[q][r]!=World.Terrain.SEA)continue;
            int land=0;boolean boundary=false;for(Hex n:h.neighbors()){if(!g.valid(n))boundary=true;else if(!g.surface.water(n))land++;}
            if(edge?boundary:land>=(sea?1:3))return h;
        }throw new AssertionError("Missing actual water shot sea="+sea+" edge="+edge);
    }
    @Override Hex[] shots(){return new Hex[]{site(World.SiteKind.GATE),site(World.SiteKind.PORT),water(false,false),water(true,false),water(false,true),boundary(World.Terrain.MOUNTAIN)};}
    @Override String[] names(){return new String[]{"mountain-gate","port","narrow-channel","coast","water-map-edge","ridge"};}
    @Override String stage(){return "S11";}
    @Override void extra(FilamentMapView spatial)throws Exception{
        check(field(spatial,"waterMaterial")!=null,"independent actual native water material loaded");
        MapSceneSnapshot snapshot=(MapSceneSnapshot)field(spatial,"snapshot");
        check(snapshot.ground.surface.at(site(World.SiteKind.PORT))==0,"port remains on original foundation");
        // The monotonic visual clock stops while the view is paused.
        runOnMainSync(()->spatial.resume(false));double t=(Double)field(spatial,"waterSeconds");settle();
        check((Double)field(spatial,"waterSeconds")==t,"background pauses water clock");
        runOnMainSync(()->spatial.resume(true));settle();ready();
    }
}

package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;

/** S13 national art routes and real toolbar grid interaction, on the production MapHost. */
public final class NationwideInstrumentation extends TerrainMaterialInstrumentation {
    Hex site(String name){for(World.City c:world.cities)if(c.name.equals(name))return c.hex;throw new AssertionError("missing national site "+name);}
    Hex edge(boolean southeast){Hex best=null;int score=southeast?Integer.MIN_VALUE:Integer.MAX_VALUE;for(int r=0;r<world.height;r++)for(int q=0;q<world.width;q++){Hex h=new Hex(q,r);if(!world.inside(h))continue;int v=q+r;if(best==null||(southeast?v>score:v<score)){best=h;score=v;}}return best;}
    @Override Hex[] shots(){return new Hex[]{boundary(World.Terrain.FOREST),boundary(World.Terrain.MOUNTAIN),boundary(World.Terrain.SAND),world.cities.get(0).hex,site("中庐港"),site("乌林港"),site("陆口港"),site("曲阿港"),site("濡须港"),site("顿丘港"),edge(false),edge(true)};}
    @Override String[] names(){return new String[]{"forest","mountain","sand","city","zhonglu","wulin","lukou","qua","ruxu","dunqiu","northwest","southeast"};}
    @Override String stage(){return "S13";}
    @Override void extra(FilamentMapView spatial)throws Exception{
        byte[] save=SaveCodec.encode(world);int territory=host.territoryMode();
        android.view.View toggle=activity.findViewById(android.R.id.content).findViewWithTag("map.grid.toggle");check(toggle!=null,"actual toolbar grid toggle");
        runOnMainSync(()->{host.setGridShown(false);spatial.camera.span=16;toggle.performClick();});settle();
        check(host.gridShown()&&(Boolean)field(spatial,"gridShown"),"toolbar enables native grid");capture("s13-grid-on-"+host.quality());
        runOnMainSync(()->toggle.performClick());settle();check(!host.gridShown()&&!(Boolean)field(spatial,"gridShown"),"toolbar hides native grid");capture("s13-grid-off-"+host.quality());
        check(host.territoryMode()==territory,"grid independent of territory");
        runOnMainSync(()->host.setGridShown(true));settle();
        check(((MapView)field(host,"flat")).gridShown(),"2D backing view retains grid state");
        runOnMainSync(()->{MapHost recreated=new MapHost(activity,h->{});check(recreated.gridShown(),"new host restores persisted grid");recreated.release();});
        check((Boolean)field(field(host,"spatial"),"gridShown"),"native recreation restores grid");
        runOnMainSync(()->host.setGridShown(false));
        check(Arrays.equals(save,SaveCodec.encode(world)),"grid/mode changes preserve full save");
    }
}

package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;

public final class Reference58ProjectionTest {
    private static long checks;
    private static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"})verify(ScenarioCatalog.load(id,0,580L));
        World axial=new World(17,9);for(World.Terrain[] r:axial.terrain)Arrays.fill(r,World.Terrain.PLAIN);verify(axial);
        World oddr=new World(21,9);oddr.sourceMapWidth=17;oddr.sourceMapHeight=9;
        for(World.Terrain[] r:oddr.terrain)Arrays.fill(r,World.Terrain.VOID);
        for(int x=0;x<17;x++)for(int y=0;y<9;y++){Hex h=MapCoordinates.axial(oddr,new SourceGridCoord(x,y));oddr.terrain[h.q][h.r]=World.Terrain.PLAIN;}verify(oddr);
        World.Terrain[] styles={World.Terrain.ROAD,World.Terrain.MOUNTAIN_PATH,World.Terrain.PLANK_ROAD};
        for(World.Terrain from:styles)for(World.Terrain to:styles)for(int d=0;d<6;d++){
            World w=new World(7,7);for(World.Terrain[] col:w.terrain)Arrays.fill(col,World.Terrain.MOUNTAIN);
            int q=3+TerrainConnections.DQ[d],r=3+TerrainConnections.DR[d];w.terrain[3][3]=from;w.terrain[q][r]=to;
            check((TerrainConnections.mask(w,3,3)&1<<d)!=0,"mixed route connection");
            check((TerrainConnections.mask(w,q,r)&1<<((d+3)%6))!=0,"mixed route reciprocal edge");
        }
        check(TerrainArt.ground(World.Terrain.NON_NAVIGABLE_WATER)==World.Terrain.WATER&&TerrainArt.connection(World.Terrain.NON_NAVIGABLE_WATER)==TerrainArt.Connection.NONE,"blocked water renders as water, never wall or bridge");
        check(TerrainConnections.water(World.Terrain.NON_NAVIGABLE_WATER),"coast joins visible water without opening pathfinding");
        System.out.println("REFERENCE58 PROJECTION PASS: "+checks);
    }
    private static void verify(World w){
        MapRaster raster=new MapRaster(w);Hex[] pixels=new Hex[raster.width*raster.height];int painted=0;
        for(int x=0;x<w.sourceColumns();x++)for(int y=0;y<w.sourceRows();y++){
            Hex h=MapCoordinates.axial(w,new SourceGridCoord(x,y));if(!w.inside(h))continue;
            int left=raster.left(h),top=raster.top(h);
            check(left>=0&&top>=0&&left+1<raster.width&&top+1<raster.height,"minimap tile in exact raster bounds");
            for(int dx=0;dx<2;dx++)for(int dy=0;dy<2;dy++){
                int idx=(top+dy)*raster.width+left+dx;check(pixels[idx]==null,"no overlapping projected source tiles");pixels[idx]=h;painted++;
                check(h.equals(raster.at(left+dx+.5f,top+dy+.5f)),"bitmap pixel -> touch -> original source tile");
            }
            check(h.equals(raster.at(raster.rasterX(raster.worldX(h)),raster.rasterY(raster.worldY(h)))),"city/unit/dot center same as terrain hit");
        }
        for(int y=0;y<raster.height;y++)for(int x=0;x<raster.width;x++)check(Objects.equals(pixels[y*raster.width+x],raster.at(x+.5f,y+.5f)),"all raster holes/padding/VOID rejected consistently");
        for(float x:new float[]{-1,raster.width,Float.NaN,Float.POSITIVE_INFINITY})check(raster.at(x,0)==null,"outside raster never navigates");
        for(float y:new float[]{-1,raster.height,Float.NaN,Float.POSITIVE_INFINITY})check(raster.at(0,y)==null,"outside raster never navigates");
        System.out.println("REFERENCE58 RASTER "+w.scenarioId+" "+raster.width+"x"+raster.height+" painted="+painted);
    }
}

package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;

/** Exact production projection, culling and artwork plan, including local crop origins. */
public final class MapTap57ProjectionTest {
    private static int checks;
    private static void check(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}) {
            World w=ScenarioCatalog.load(id,0,57L);float offset=(w.height-1)/2;
            for(int sy=0;sy<w.sourceRows();sy++)for(int sx=0;sx<w.sourceColumns();sx++) {
                SourceGridCoord s=new SourceGridCoord(sx,sy);Hex h=MapCoordinates.axial(w,s);
                float x=TileGeometry.projectedX(h.q,h.r,offset,w.columnStaggered),y=TileGeometry.projectedY(h.q,h.r,offset,w.columnStaggered);
                for(float dx:new float[]{-19.9f,0,19.9f})for(float dy:new float[]{-19.9f,0,19.9f}) {
                    int r=TileGeometry.projectedRow(x+dx,y+dy,w.columnStaggered),q=TileGeometry.projectedColumn(x+dx,y+dy,r,offset,w.columnStaggered);
                    Hex picked=new Hex(q,r);check(MapCoordinates.source(w,picked).equals(s),"source-screen-source "+id);
                    check(MapCoordinates.nationalSource(w,picked).equals(new SourceGridCoord(sx+w.sourceOriginX,sy+w.sourceOriginY)),"crop origin/parity");
                }
            }
            MapCamera camera=new MapCamera();camera.columnStaggered=w.columnStaggered;camera.columnOffset=offset;
            camera.resize(1080,2340,40*(w.sourceColumns()-1)+50,40*(w.sourceRows()-.5f)+50,25,3);
            for(float scale:new float[]{.15f,.9f,3.4f})for(int sx:new int[]{0,1,w.sourceColumns()/2,w.sourceColumns()-1})for(int sy:new int[]{0,1,w.sourceRows()/2,w.sourceRows()-1}) {
                Hex h=MapCoordinates.axial(w,new SourceGridCoord(sx,sy));float x=TileGeometry.projectedX(h.q,h.r,offset,w.columnStaggered),y=TileGeometry.projectedY(h.q,h.r,offset,w.columnStaggered);
                camera.focus(x,y);camera.zoom(scale,540,1170);camera.centerOn(x,y);
                check(camera.visible(x,y,0),"focused corner visible");
                check(h.r>=camera.firstRow(w.height,50)&&h.r<=camera.lastRow(w.height,50)&&h.q>=camera.firstColumn(h.r,w.width,50)&&h.q<=camera.lastColumn(h.r,w.width,50),"focused cell never culled");
            }
        }
        check(TerrainArt.connection(World.Terrain.ROAD)==TerrainArt.Connection.ROAD,"ROAD has independent ground-only connection");
        check(TerrainArt.connection(World.Terrain.PLANK_ROAD)==TerrainArt.Connection.PLANK,"plank retains elevated deck");
        check(TerrainArt.ground(World.Terrain.DAM)==World.Terrain.SHALLOWS,"dam physical body drawn only by entity");
        for(int mask=0;mask<64;mask++) {
            World w=new World(5,5);for(World.Terrain[] column:w.terrain)Arrays.fill(column,World.Terrain.MOUNTAIN);
            w.terrain[2][2]=World.Terrain.ROAD;
            for(int d=0;d<6;d++)if((mask&(1<<d))!=0)w.terrain[2+TerrainConnections.DQ[d]][2+TerrainConnections.DR[d]]=World.Terrain.ROAD;
            check(TerrainConnections.mask(w,2,2)==mask,"all64 connection masks");
            for(int d=0;d<6;d++) {
                int opposite=(d+3)%6;
                float nx=TileGeometry.x(TerrainConnections.DQ[d],TerrainConnections.DR[d],0),ny=TileGeometry.y(TerrainConnections.DR[d]);
                check(Math.abs(TerrainConnections.edgeX(d)-nx-TerrainConnections.edgeX(opposite))<.01f&&Math.abs(TerrainConnections.edgeY(d)-ny-TerrainConnections.edgeY(opposite))<.01f,"adjacent road endpoints join before/after transpose");
            }
        }
        System.out.println("MAP57 PROJECTION/ART PASS: "+checks);
    }
}

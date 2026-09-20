package game.sanguo.mobile;
import game.sanguo.core.*;

/** Uses the exact renderer/camera/picker functions, without a separate projection demo. */
public final class AndroidProjectionTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        World w=ScenarioCatalog.load("heroes-250",0,56L);float offset=(w.height-1)/2;
        for(int sy=0;sy<200;sy++)for(int sx=0;sx<200;sx++){
            SourceGridCoord s=new SourceGridCoord(sx,sy);Hex h=MapCoordinates.axial(w,s);
            float x=TileGeometry.projectedX(h.q,h.r,offset,true),y=TileGeometry.projectedY(h.q,h.r,offset,true);
            check(x==sx*40&&y==(sy+.5f*(sx&1))*40,"native geographic axes, not transposed screen");
            for(float dx:new float[]{-19.99f,0,19.99f})for(float dy:new float[]{-19.99f,0,19.99f}){
                int r=TileGeometry.projectedRow(x+dx,y+dy,true),q=TileGeometry.projectedColumn(x+dx,y+dy,r,offset,true);
                check(MapCoordinates.source(w,new Hex(q,r)).equals(s),"source-screen-source all corners and parity");
            }
        }
        MapCamera camera=new MapCamera();camera.columnStaggered=true;camera.columnOffset=offset;
        camera.resize(1080,2340,40*199+50,40*199.5f+50,25,3);
        for(int sx:new int[]{0,1,99,198,199})for(int sy:new int[]{0,1,99,198,199}){
            Hex h=MapCoordinates.axial(w,new SourceGridCoord(sx,sy));float x=TileGeometry.projectedX(h.q,h.r,offset,true),y=TileGeometry.projectedY(h.q,h.r,offset,true);
            camera.focus(x,y);check(camera.visible(x,y,0),"camera clamps all corner cells inside tappable viewport");
            check(h.r>=camera.firstRow(w.height,0)&&h.r<=camera.lastRow(w.height,0),"culling includes focused source column");
            check(h.q>=camera.firstColumn(h.r,w.width,0)&&h.q<=camera.lastColumn(h.r,w.width,0),"culling includes focused source row");
            float screenX=x*camera.scale+camera.x,screenY=y*camera.scale+camera.y;
            float wx=(screenX-camera.x)/camera.scale,wy=(screenY-camera.y)/camera.scale;
            int r=TileGeometry.projectedRow(wx,wy,true);check(new Hex(TileGeometry.projectedColumn(wx,wy,r,offset,true),r).equals(h),"touch uses inverse camera plus native projection");
        }
        camera.focus(4000,4000);int visited=0;
        for(int r=camera.firstRow(w.height,120);r<=camera.lastRow(w.height,120);r++)visited+=Math.max(0,camera.lastColumn(r,w.width,120)-camera.firstColumn(r,w.width,120)+1);
        check(visited<1500,"near view culls the 40,000 grid, visits="+visited);
        System.out.println("ANDROID PROJECTION PASS: "+checks+"; focused viewport candidates="+visited);
    }
}

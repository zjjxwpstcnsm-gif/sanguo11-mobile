package game.sanguo.mobile;

import game.sanguo.core.Hex;
import game.sanguo.core.MapCoordinates;
import java.util.Random;

/** No Android dependency: clipping, picking, road seams and viewport use this same geometry. */
public final class TileGeometryTest {
    private static int checks;
    private static void check(boolean test,String message){checks++;if(!test)throw new AssertionError(message);}
    private static Hex pick(float x,float y,float offset){int r=TileGeometry.row(y);return new Hex(TileGeometry.column(x,r,offset),r);}
    public static void main(String[] args){
        check(TileGeometry.DX==TileGeometry.DY,"cells are squares, not honeycomb hexagons");
        Random random=new Random(530);
        for(int size:new int[]{7,100,200}){
            float offset=(size-1)/2;
            for(int y=0;y<size;y++)for(int x=0;x<size;x++){
                Hex h=MapCoordinates.axial(x,y,size);
                float cx=TileGeometry.x(h.q,h.r,offset),cy=TileGeometry.y(h.r);
                check(pick(cx,cy,offset).equals(h),"all map centers round-trip including odd/even rows and edges");
                for(int i=0;i<4;i++){
                    float dx=(i%2==0?-1:1)*19.99f,dy=(i<2?-1:1)*19.99f;
                    check(pick(cx+dx,cy+dy,offset).equals(h),"all square corners belong to their tile, not the old cube-rounded hex");
                }
                float dx=random.nextFloat()*39.9f-19.95f,dy=random.nextFloat()*39.9f-19.95f;
                check(pick(cx+dx,cy+dy,offset).equals(h),"interior pixel picking");
                if(x>2&&y>2&&x<size-3&&y<size-3)for(int d=0;d<6;d++){
                    Hex n=h.neighbors().get(d);float ex=TileGeometry.edgeX(d),ey=TileGeometry.edgeY(d);
                    float nx=TileGeometry.x(n.q,n.r,offset),ny=TileGeometry.y(n.r);
                    check(Math.abs(cx+ex-(nx+TileGeometry.edgeX((d+3)%6)))<.001f && Math.abs(cy+ey-(ny+TileGeometry.edgeY((d+3)%6)))<.001f,"both road branches meet at identical world pixels");
                    float normalX=d==0?1:d==3?-1:0,normalY=d==1||d==2?-1:d==4||d==5?1:0;
                    check(pick(cx+ex-normalX*.01f,cy+ey-normalY*.01f,offset).equals(h),"inside each shared edge belongs to source");
                    check(pick(cx+ex+normalX*.01f,cy+ey+normalY*.01f,offset).equals(n),"crossing edge picks exact logical neighbor; never a seventh/eighth move");
                }
            }
        }
        // Negative cells matter around the padding in overview rasters.
        check(pick(-20.001f,0,0).equals(new Hex(-1,0)),"negative coordinate uses floor, not truncation");
        check(pick(20,0,0).equals(new Hex(1,0)),"deterministic right-open boundary");
        System.out.println("PASS: "+checks+" rectangular six-neighbor geometry assertions.");
    }
}

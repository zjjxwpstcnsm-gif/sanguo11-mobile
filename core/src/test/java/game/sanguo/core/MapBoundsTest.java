package game.sanguo.core;
import static game.sanguo.core.Native56Checks.*;
public final class MapBoundsTest {
 public static void main(String[] args)throws Exception {
  World w=world();check(w.sourceColumns()==200&&w.sourceRows()==200,"native source bounds");int valid=0;
  for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
   Hex h=new Hex(q,r);if(w.sourceInside(h))valid++;else check(w.terrain[q][r]==World.Terrain.VOID&&!w.inside(h),"axial padding never playable");
  }
  check(valid==40000,"independent source slots not display scaling");
  for(int[] p:new int[][]{{0,0},{0,199},{199,0},{199,199}})check(w.sourceInside(MapCoordinates.axial(w,new SourceGridCoord(p[0],p[1]))),"corner source exists");
  for(int[] p:new int[][]{{-1,0},{0,-1},{200,0},{0,200}})check(!w.sourceInside(MapCoordinates.axial(w,new SourceGridCoord(p[0],p[1]))),"outside rejected");
  pass("MapBoundsTest");
 }
}

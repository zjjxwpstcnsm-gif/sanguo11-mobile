package game.sanguo.mobile;
import game.sanguo.core.*;
/** Actual source/crop geometry and visual adjacency, without granting navigation. */
public final class Reference61ProjectionTest {
 static long checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception {
  int seaEdges=0;
  for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}){
   World w=ScenarioCatalog.load(id,0,610L);MapRaster raster=new MapRaster(w);int ext=0;
   for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
    Hex h=new Hex(q,r);World.Terrain real=w.terrain[q][r],appearance=TerrainConnections.appearance(w,q,r);
    NationalExterior.Surface s=NationalExterior.surface(w,h);
    if(s==null){check(appearance==real,"unknown/crop/padding not reinterpreted");continue;}
    ext++;check(w.sourceInside(h)&&!w.inside(h)&&real==World.Terrain.VOID,"visual layer does not mutate validity");
    float px=raster.rasterX(raster.worldX(h)),py=raster.rasterY(raster.worldY(h));
    check(raster.at(px,py)==null,"mini/preview tap rejects exterior instead of nearest city");
    for(int d=0;d<6;d++){
     Hex n=new Hex(q+TerrainConnections.DQ[d],r+TerrainConnections.DR[d]);
     if(!w.sourceInside(n))continue;
     if(TerrainConnections.water(appearance)&&TerrainConnections.water(TerrainConnections.appearance(w,n.q,n.r))){
      check((TerrainConnections.mask(w,q,r)&1<<d)!=0,"scenic sea joins Q/W visually without fake coastline");
      check((TerrainConnections.mask(w,n.q,n.r)&1<<((d+3)%6))!=0,"reciprocal scenic-water edge");seaEdges++;
     }
    }
   }
   if(w.sourceColumns()==200)check(ext==1051,"whole national mask visible to all LOD consumers");else check(ext==0,"cropped edge is not national exterior");
   System.out.println("REFERENCE61 PROJECTION "+id+" exterior="+ext);
  }
  check(seaEdges>8000,"whole connected sea, not one representative tile");
  Reference60ProjectionTest.main(new String[0]);
  System.out.println("REFERENCE61 PROJECTION PASS: "+checks+" seaEdges="+seaEdges);
 }
}

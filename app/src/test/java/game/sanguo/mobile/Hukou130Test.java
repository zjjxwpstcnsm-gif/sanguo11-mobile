package game.sanguo.mobile;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.nio.file.*;
import java.util.*;
/** Actual decoded/baked Hukou silhouette, fitted to the unchanged national bank. */
public final class Hukou130Test {
 static int checks;
 static void check(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  World w=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(w);
  var snap=new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);var g=snap.ground;
  Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(66,58));
  check(g.terrain[h.r*g.width+h.q]==World.Terrain.NON_NAVIGABLE_WATER.ordinal(),"unchanged authoritative Hukou source cell");
  var exclusions=Vegetation.exclusions(snap);
  var assets=new FieldAssets(n->Files.newInputStream(Path.of("app/src/main/assets/3d/field",n)));
  Class<?> type=Class.forName("game.sanguo.mobile.Vegetation$Batch");
  var ctor=type.getDeclaredConstructor(MapSceneSnapshot.Ground.class);ctor.setAccessible(true);
  var emit=type.getDeclaredMethod("cascade",Hex.class,Set.class,SceneMesh.class,float.class);emit.setAccessible(true);
  var finish=type.getDeclaredMethod("mesh",float.class,float.class,float.class);finish.setAccessible(true);
  for(int lod=0;lod<2;lod++){
   SceneMesh model=assets.mesh("fall-hukou-lod"+lod);Object batch=ctor.newInstance(g);
   emit.invoke(batch,h,Vegetation.exclusions(snap),model,1.15f);SceneMesh mesh=(SceneMesh)finish.invoke(batch,0f,0f,4f);
   check(mesh.vertices.length==model.vertices.length,"full authored Hukou emits without fallback LOD"+lod);
   check(model.indices.length/3<=(lod==0?4000:850),"reasoned Hukou silhouette budget LOD"+lod);
   float minX=Float.POSITIVE_INFINITY,maxX=-minX,maxY=-minX,bed=-minX,lip=minX,foot=-minX;
   for(int k=0;k<mesh.vertices.length;k+=7){
    float x=mesh.vertices[k],y=mesh.vertices[k+1],z=mesh.vertices[k+2];
    minX=Math.min(minX,x);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);bed=Math.max(bed,g.surface.meshHeight(x,z));
    check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"finite fitted vertices");
    check(y>=g.surface.meshHeight(x,z)+.029f,"all vertices grounded above canonical bank");
    int uv=k/7*2;float u=mesh.uv[uv],v=mesh.uv[uv+1];
    if(u>.375f&&u<.5f){if(Math.abs(v-(.06f+.88f*.34f))<.0001f)lip=Math.min(lip,y);if(Math.abs(v-(.06f+.88f*.50f))<.0001f)foot=Math.max(foot,y);}
   }
   for(int k=0;k<mesh.indices.length;k+=3)for(int a=0;a<=8;a++)for(int b=0;b<=8-a;b++){
    float u=a/8f,v=b/8f,t=1-u-v;int A=mesh.indices[k]*7,B=mesh.indices[k+1]*7,C=mesh.indices[k+2]*7;
    float x=mesh.vertices[A]*u+mesh.vertices[B]*v+mesh.vertices[C]*t,z=mesh.vertices[A+2]*u+mesh.vertices[B+2]*v+mesh.vertices[C+2]*t,y=mesh.vertices[A+1]*u+mesh.vertices[B+1]*v+mesh.vertices[C+1]*t;
    Hex cell=g.grid.cell(x,z);
    check(g.valid(cell)&&(cell.equals(h)||g.terrain[cell.r*g.width+cell.q]==World.Terrain.MOUNTAIN.ordinal()),"entire broader face remains in original blocked bank/water corridor");
    check(!exclusions.contains(cell)&&!g.bases.contains(cell),"no path or facility obscured by broad rock shoulders");
    check(y>=g.surface.meshHeight(x,z)+.028f,"full broad shoulder face clears unchanged canonical terrain: lod="+lod+" tri="+(k/3)+" clearance="+(y-g.surface.meshHeight(x,z))+" at="+x+","+z);
   }
   System.out.println("HUKOU130 lod="+lod+" triangles="+model.indices.length/3+" width="+(maxX-minX)+" highest="+maxY+" bank="+bed+" lip="+lip+" foot="+foot);
   check(maxX-minX>=.72f,"Hukou has broad fitted ledge, not a narrow ornament");
   check(maxY<=bed+.18f,"Hukou follows the actual bank instead of raised alpine cap");
   check(maxY<.82f,"source-map Hukou silhouette has no artificial tall pillar");
   check(Float.isFinite(lip)&&Float.isFinite(foot)&&lip-foot>.25f,"strict visible fall drop retained");
  }
  check(Arrays.equals(before,SaveCodec.encode(w)),"all source heights/cells/gameplay/save/RNG unchanged");
  System.out.println("PASS HUKOU130 "+checks+" bank-relative silhouette, strict drop, grounded near/far and complete authority");
 }
}

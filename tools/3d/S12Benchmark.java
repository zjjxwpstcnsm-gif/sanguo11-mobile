package game.sanguo.mobile;
import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;
public final class S12Benchmark {
 static long bytes(SceneMesh m)throws Exception{
  long n=4L*(m.vertices.length+m.indices.length+(m.uv==null?0:m.uv.length));
  try{float[] t=(float[])SceneMesh.class.getDeclaredField("tangents").get(m);if(t!=null)n+=t.length*4L;}catch(NoSuchFieldException e){}
  return n;
 }
 static List<SceneMesh> build(MapSceneSnapshot.Ground g,Set<Hex> e,List<SceneMesh> previous,FieldAssets a)throws Exception{
  try{return (List<SceneMesh>)Vegetation.class.getDeclaredMethod("build",MapSceneSnapshot.Ground.class,Set.class,List.class,SceneMesh.class,SceneMesh.class,SceneMesh.class,SceneMesh.class).invoke(null,g,e,previous,a.mesh("tree-lod0"),a.mesh("tree-lod1"),a.mesh("tree-upland-lod0"),a.mesh("tree-upland-lod1"));}
  catch(NoSuchMethodException ignored){return Vegetation.build(g,e,previous,a.mesh("tree-lod0"),a.mesh("tree-lod1"));}
 }
 public static void main(String[] args)throws Exception{
  World w=ScenarioCatalog.all().get(0);MapSceneSnapshot snap=new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);
  FieldAssets a=new FieldAssets(name->Files.newInputStream(Path.of("app/src/main/assets/3d/field",name)));
  Set<Hex> excluded=Vegetation.exclusions(snap);
  for(int run=0;run<3;run++){
   long start=System.nanoTime();List<SceneMesh> wood=build(snap.ground,excluded,List.of(),a);double cold=(System.nanoTime()-start)/1e6;
   start=System.nanoTime();List<SceneMesh> reused=build(snap.ground,excluded,wood,a);double hot=(System.nanoTime()-start)/1e6;
   long bytes=0,near=0,far=0;for(SceneMesh m:wood){bytes+=bytes(m)+bytes(m.distant);near+=m.indices.length/3;far+=m.distant.indices.length/3;}
   if(!wood.equals(reused))throw new AssertionError("cache reuse");
   System.out.printf(java.util.Locale.ROOT,"run=%d cold_ms=%.2f cached_ms=%.2f chunks=%d cpu_mesh_bytes=%d near_triangles=%d far_triangles=%d%n",run,cold,hot,wood.size(),bytes,near,far);
  }
 }
}

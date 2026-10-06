package game.sanguo.mobile;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.security.MessageDigest;
import java.util.*;
/** Expand indexed triangles; compare actual shader-consumed source attributes with baseline. */
public final class MeshParityProbe {
 static void integer(MessageDigest d,int n){for(int i=0;i<4;i++)d.update((byte)(n>>>(8*i)));}
 static void number(MessageDigest d,float n){integer(d,Float.floatToRawIntBits(n));}
 static void mesh(MessageDigest d,SceneMesh m){
  integer(d,m==null?0:1);if(m==null)return;integer(d,m.indices.length);integer(d,m.landIndexCount);integer(d,m.terrainLod);number(d,m.x);number(d,m.z);number(d,m.radius);
  int stride=m.surfaceData==null||m.vertices.length==0?0:m.surfaceData.length/(m.vertices.length/7);
  for(int index:m.indices){
   for(int a=0;a<(m.pcGround?3:7);a++)number(d,m.vertices[index*7+a]);
   integer(d,stride==0?0:m.pcGround?2:stride);for(int a=0;a<(m.pcGround?2:stride);a++)number(d,m.surfaceData[index*stride+a]);
   for(float[] stream:new float[][]{m.uv,m.tangents}){int size=stream==null?0:stream.length/(m.vertices.length/7);integer(d,size);for(int a=0;a<size;a++)number(d,stream[index*size+a]);}
  }
  mesh(d,m.grid);mesh(d,m.sourceWater);
 }
 static String digest(List<SceneMesh> list)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");for(SceneMesh m:list)mesh(d,m);return HexFormat.of().formatHex(d.digest());}
 public static void main(String[] args)throws Exception{
  World initial=ScenarioCatalog.load("heroes-250",0);byte[] authority=SaveCodec.encode(initial);
  for(boolean legacy:new boolean[]{false,true}){
   World w;if(legacy){try(var in=new java.util.zip.GZIPInputStream(MeshParityProbe.class.getResourceAsStream("/pc-map-v063.sg11.gz"))){w=SaveCodec.decode(in.readAllBytes());}}else w=SaveCodec.decode(authority);byte[] save=SaveCodec.encode(w);var g=new MapSceneSnapshot.Ground(w);
   for(int[] point:new int[][]{{81,79},{108,119},{176,21},{40,82}}){Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(point[0],point[1]));float x=g.grid.x(h),z=g.grid.z(h);
    for(float span:new float[]{8,50}){
     var window=new SceneMesh.TerrainWindow(x,z,2,2,span);List<SceneMesh> list=SceneMesh.ground(g,List.of(),window);
     String hash=digest(list);List<SceneMesh> reused=SceneMesh.ground(g,list,window);if(!hash.equals(digest(reused)))throw new AssertionError("cache changed");
     SceneMesh.ground(g,list,new SceneMesh.TerrainWindow(x+10,z+10,2,2,span));if(!hash.equals(digest(list)))throw new AssertionError("published arrays mutated");
     System.out.println((legacy?"legacy":"PC")+" source="+point[0]+","+point[1]+" span="+span+" chunks="+list.size()+" indexedShaderTrianglesSha="+hash);
    }
   }
   if(!Arrays.equals(save,SaveCodec.encode(w)))throw new AssertionError("terrain changed Save/RNG");
  }
 }
}

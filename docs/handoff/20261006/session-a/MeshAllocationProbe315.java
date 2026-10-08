package game.sanguo.mobile;
import game.sanguo.core.*;
import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.util.*;
/** Exact raw producer attributes/cache/fingerprint parity; no installed-flow claim. */
public final class MeshAllocationProbe315 {
 static void add(MessageDigest d,int x){for(int i=0;i<4;i++)d.update((byte)(x>>>(8*i)));}
 static void floats(MessageDigest d,float[] a){add(d,a==null?-1:a.length);if(a!=null)for(float x:a)add(d,Float.floatToRawIntBits(x));}
 static void mesh(MessageDigest d,SceneMesh m){
  add(d,m==null?0:1);if(m==null)return;
  floats(d,m.vertices);floats(d,m.surfaceData);floats(d,m.tangents);floats(d,m.uv);
  add(d,m.indices.length);for(int i:m.indices)add(d,i);
  floats(d,new float[]{m.x,m.z,m.radius,m.minY,m.maxY});add(d,m.landIndexCount);add(d,m.chunkQ);add(d,m.chunkR);add(d,m.terrainLod);add(d,(int)m.fingerprint);add(d,(int)(m.fingerprint>>>32));
  add(d,m.pcGround?1:0);add(d,m.pcWater?1:0);add(d,m.gridDifficultMarch?1:0);
  mesh(d,m.grid);mesh(d,m.sourceWater);mesh(d,m.distant);
 }
 static String digest(List<SceneMesh> list)throws Exception{var d=MessageDigest.getInstance("SHA-256");for(var m:list)mesh(d,m);return HexFormat.of().formatHex(d.digest());}
 public static void main(String[] args)throws Exception{
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long tid=Thread.currentThread().getId();
  World w=PcScenarioCatalog.preview(PcScenarioCatalog.all().get(14).identity.scenarioId);
  for(boolean legacy:new boolean[]{false,true}){
   World world=w;if(legacy)try(var in=new java.util.zip.GZIPInputStream(MeshAllocationProbe315.class.getResourceAsStream("/pc-map-v063.sg11.gz"))){world=SaveCodec.decode(in.readAllBytes());}
   byte[] saved=SaveCodec.encode(world);var g=new MapSceneSnapshot.Ground(world);
   List<SceneMesh> previous=List.of();
   for(int i=0;i<5;i++){
    float span=i%2==0?87.153015f:8;float x=i==0?(g.minX+g.maxX)/2:g.grid.x(81+i*10,79),z=i==0?(g.minZ+g.maxZ)/2:g.grid.z(81+i*10,79);
    var window=new SceneMesh.TerrainWindow(x,z,i==0?120:3,i==0?120:3,span);
    long start=bean.getThreadAllocatedBytes(tid);var list=SceneMesh.ground(g,previous,window);
    long allocated=bean.getThreadAllocatedBytes(tid)-start;String hash=digest(list);
    var reused=SceneMesh.ground(g,list,window);if(!hash.equals(digest(reused)))throw new AssertionError("cache output changed");
    SceneMesh.ground(g,list,new SceneMesh.TerrainWindow(x+8,z+8,3,3,span));if(!hash.equals(digest(list)))throw new AssertionError("scratch escaped or published arrays mutated");
    System.out.println((legacy?"legacy":"PC")+"\t"+i+"\t"+list.size()+"\t"+SceneMesh.payloadBytes(list)+"\t"+hash+"\t"+allocated);previous=list;
   }
   if(!Arrays.equals(saved,SaveCodec.encode(world)))throw new AssertionError("full Save/RNG changed");
  }
 }
}

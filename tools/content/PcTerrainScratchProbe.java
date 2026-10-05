package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.util.*;
import java.security.MessageDigest;
import java.lang.management.ManagementFactory;

/** Real source terrain output and worker allocation probe. Run in separate
 * JVMs with the archived and current SceneMesh, then compare buffer digests.
 * Desktop allocation evidence only; this does not measure Android frame time. */
public final class PcTerrainScratchProbe {
    private static void integer(MessageDigest d,int n){for(int i=0;i<4;i++)d.update((byte)(n>>>(i*8)));}
    private static void floats(MessageDigest d,float[] a){integer(d,a==null?-1:a.length);if(a!=null)for(float f:a)integer(d,Float.floatToRawIntBits(f));}
    private static void mesh(MessageDigest d,SceneMesh m){
        integer(d,m.chunkQ);integer(d,m.chunkR);integer(d,m.landIndexCount);integer(d,m.terrainLod);
        floats(d,m.vertices);floats(d,m.surfaceData);floats(d,m.tangents);floats(d,m.uv);
        integer(d,m.indices.length);for(int i:m.indices)integer(d,i);
        floats(d,new float[]{m.x,m.z,m.radius});
        integer(d,m.grid==null?0:1);if(m.grid!=null)mesh(d,m.grid);
    }
    private static String digest(List<SceneMesh> chunks)throws Exception{
        MessageDigest d=MessageDigest.getInstance("SHA-256");integer(d,chunks.size());for(SceneMesh m:chunks)mesh(d,m);
        return HexFormat.of().formatHex(d.digest());
    }
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.load("heroes-250",0);byte[] authority=SaveCodec.encode(w);
        var meter=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();meter.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().getId();
        for(boolean legacy:new boolean[]{false,true}){
            World review=SaveCodec.decode(authority);if(legacy)review.mapRevision=63;
            var ground=new MapSceneSnapshot.Ground(review);
            for(int[] point:new int[][]{{81,79},{108,119},{176,21},{40,82}}){
                Hex h=MapCoordinates.fromNationalSource(review,new SourceGridCoord(point[0],point[1]));
                float x=ground.grid.x(h),z=ground.grid.z(h);
                for(float span:new float[]{8,50}){
                    SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow(x,z,2,2,span);
                    long before=meter.getThreadAllocatedBytes(thread);
                    List<SceneMesh> chunks=SceneMesh.ground(ground,List.of(),window);
                    long allocated=meter.getThreadAllocatedBytes(thread)-before;
                    String original=digest(chunks);
                    List<SceneMesh> retained=SceneMesh.ground(ground,chunks,window);
                    if(!original.equals(digest(retained)))throw new AssertionError("cached output changed");
                    for(int i=0;i<chunks.size();i++)if(chunks.get(i)!=retained.get(i))throw new AssertionError("matching cache identity lost");
                    // Another build resets scratch repeatedly. Published old arrays
                    // must remain exactly as delivered, including clipped water.
                    List<SceneMesh> next=SceneMesh.ground(ground,chunks,new SceneMesh.TerrainWindow(x+10,z+10,2,2,span));
                    if(!original.equals(digest(chunks)))throw new AssertionError("later chunks modified published buffers");
                    long wet=chunks.stream().filter(m->m.landIndexCount<m.indices.length).count();
                    System.out.println((legacy?"legacy":"PC")+" "+point[0]+","+point[1]+" span="+span+" chunks="+chunks.size()+" waterChunks="+wet+" sha256="+original+" next="+digest(next)+" allocated="+allocated);
                }
            }
            if(!Arrays.equals(authority,SaveCodec.encode(w)))throw new AssertionError("authority/RNG/save mutated");
        }
    }
}

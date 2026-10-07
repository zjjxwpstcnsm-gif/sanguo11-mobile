package game.sanguo.mobile;

import game.sanguo.core.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.*;

/** Host producer proof only. Declared geometry regions do not replace normal Android entry. */
public final class SourceWaterAllocationProbe {
    private static void integer(MessageDigest hash,int value){for(int n=0;n<4;n++)hash.update((byte)(value>>>(n*8)));}
    private static void number(MessageDigest hash,float value){integer(hash,Float.floatToRawIntBits(value));}
    private static void digest(MessageDigest hash,SceneMesh mesh){
        integer(hash,mesh==null?0:1);if(mesh==null)return;
        number(hash,mesh.x);number(hash,mesh.z);number(hash,mesh.radius);number(hash,mesh.minY);number(hash,mesh.maxY);
        integer(hash,mesh.chunkQ);integer(hash,mesh.chunkR);integer(hash,mesh.pcWater?1:0);integer(hash,mesh.landIndexCount);
        integer(hash,mesh.vertices.length);for(float v:mesh.vertices)number(hash,v);
        integer(hash,mesh.indices.length);for(int v:mesh.indices)integer(hash,v);
        integer(hash,mesh.surfaceData.length);for(float v:mesh.surfaceData)number(hash,v);
    }
    public static void main(String[] args)throws Exception{
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isThreadAllocatedMemorySupported())throw new AssertionError("Actual host allocation measurement unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().getId();
        Method build=SceneMesh.class.getDeclaredMethod("sourceWater",MapSceneSnapshot.Ground.class,int.class,int.class,SceneMesh.class);build.setAccessible(true);
        int sourceIndex=0;
        for(var source:PcScenarioCatalog.all()){
            World world=PcScenarioCatalog.preview(source.identity.scenarioId);byte[] saved=SaveCodec.encode(world);
            var ground=new MapSceneSnapshot.Ground(world);MessageDigest hash=MessageDigest.getInstance("SHA-256");
            long allocated=0,payload=0;int batches=0,nonempty=0;
            for(int r=-32;r<ground.height+32;r+=16)for(int q=-32;q<ground.width+32;q+=16){
                SceneMesh owner=new SceneMesh(new float[0],new int[0],ground.grid.x(q+8,r+8),ground.grid.z(q+8,r+8),13);
                long start=bean.getThreadAllocatedBytes(thread);SceneMesh mesh=(SceneMesh)build.invoke(null,ground,q,r,owner);allocated+=bean.getThreadAllocatedBytes(thread)-start;
                digest(hash,mesh);batches++;if(mesh!=null){nonempty++;payload+=4L*(mesh.vertices.length+mesh.indices.length+mesh.surfaceData.length);}
            }
            SceneMesh owner=new SceneMesh(new float[0],new int[0],(ground.minX+ground.maxX)/2,(ground.minZ+ground.maxZ)/2,300);
            long start=bean.getThreadAllocatedBytes(thread);SceneMesh exterior=(SceneMesh)build.invoke(null,ground,-1,-1,owner);allocated+=bean.getThreadAllocatedBytes(thread)-start;
            digest(hash,exterior);batches++;if(exterior!=null){nonempty++;payload+=4L*(exterior.vertices.length+exterior.indices.length+exterior.surfaceData.length);}
            if(!Arrays.equals(saved,SaveCodec.encode(world)))throw new AssertionError("Water producer changed complete Save/RNG");
            System.out.println(sourceIndex+++"\t"+batches+"\t"+nonempty+"\t"+payload+"\t"+allocated+"\t"+HexFormat.of().formatHex(hash.digest()));
        }
    }
}

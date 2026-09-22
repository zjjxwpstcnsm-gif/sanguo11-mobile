package game.sanguo.mobile;
import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
/** Host allocation/CPU comparison only. Never interpreted as device or GPU timing. */
public final class S08Benchmark {
    static volatile long sink;
    public static void main(String[] args)throws Exception{
        FieldAssets assets=new FieldAssets(name->Files.newInputStream(Path.of("app/src/main/assets/3d/field",name)));
        com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().getId();
        long[] timings=new long[7],allocations=new long[7];long digest=0;
        for(int round=-3;round<7;round++){
            long bytes=bean.getThreadAllocatedBytes(id),start=System.nanoTime();long hash=1;
            for(int i=0;i<600;i++){
                SceneMesh m=assets.pose("unit-SPEAR-lod0","idle",i%8,8);
                hash=31*hash+Arrays.hashCode(m.vertices);hash=31*hash+Arrays.hashCode(m.indices);hash=31*hash+Arrays.hashCode(m.uv);
            }
            long elapsed=System.nanoTime()-start,allocated=bean.getThreadAllocatedBytes(id)-bytes;
            sink=hash;digest=hash;
            if(round>=0){timings[round]=elapsed;allocations[round]=allocated;}
        }
        Arrays.sort(timings);Arrays.sort(allocations);
        System.out.println("{\"poses\":600,\"median_cpu_ms\":"+timings[3]/1e6+",\"median_allocated_bytes\":"+allocations[3]+",\"geometry_hash\":"+digest+"}");
    }
}

package game.sanguo.core;
import java.nio.file.*;import java.security.*;import java.util.*;import java.lang.management.*;
/** Same actual fresh installed save/normal turns; no fixture arithmetic or disabled AI. */
public final class PcScenarioPeopleReplayProbe {
    public static void main(String[] args)throws Exception {
        World w=SaveCodec.decode(Files.readAllBytes(Path.of(args[0])));
        com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);
        for(int i=0;i<=3;i++){
            byte[] saved=SaveCodec.encode(w);System.out.println(i+"\t"+PcCommandCapacityPolicy.hex(MessageDigest.getInstance("SHA-256").digest(saved)));
            if(!Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))))throw new AssertionError("normal completeWorld/bothRNG reload mismatch "+i);
            if(i<3){long bytes=bean.getThreadAllocatedBytes(Thread.currentThread().getId()),start=System.nanoTime();var result=w.nextTurn();if(!result.ok)throw new AssertionError(result.message);System.err.println("normalTurn="+i+" ms="+(System.nanoTime()-start)/1000000+" threadAllocatedBytes="+(bean.getThreadAllocatedBytes(Thread.currentThread().getId())-bytes));}
        }
    }
}

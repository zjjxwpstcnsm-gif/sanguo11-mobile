package game.sanguo.mobile;
import android.os.Debug;
import android.os.SystemClock;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
/** Instrumentation-only allocator observation. No GC/dump/View/core/save/RNG access. */
final class SessionAMemorySampler implements AutoCloseable {
 private volatile boolean stopped;
 private volatile Throwable failure;
 private final Thread thread;
 SessionAMemorySampler(File output)throws IOException {
  if(output.exists())throw new IOException("Preserve existing allocator evidence");
  BufferedWriter writer=Files.newBufferedWriter(output.toPath(),StandardCharsets.UTF_8);
  thread=new Thread(()->{
   try(BufferedWriter sink=writer){
    sink.write("sample,elapsedRealtimeMillis,javaUsed,javaTotal,javaLimit,nativeAllocated,consistent,freeBytes,totalAfter\n");
    Runtime runtime=Runtime.getRuntime();int count=0;
    while(!stopped){
     if(count>=20000)throw new IOException("Allocator sample cap reached before lifecycle end");
     long total=runtime.totalMemory(),free=runtime.freeMemory(),limit=runtime.maxMemory(),after=runtime.totalMemory();
     boolean consistent=total==after&&free>=0&&free<=total&&total<=limit;
     long used=consistent?total-free:-1;
     sink.write(Integer.toString(count++));sink.write(',');sink.write(Long.toString(SystemClock.elapsedRealtime()));
     for(long value:new long[]{used,total,limit,Debug.getNativeHeapAllocatedSize()}){sink.write(',');sink.write(Long.toString(value));}
     sink.write(',');sink.write(consistent?"1":"0");sink.write(',');sink.write(Long.toString(free));sink.write(',');sink.write(Long.toString(after));
     sink.write('\n');if(count%20==0)sink.flush();SystemClock.sleep(250);
    }
   }catch(Throwable error){failure=error;}
  },"Session A allocator evidence");
  thread.start();
 }
 @Override public void close()throws IOException,InterruptedException {
  stopped=true;thread.join(3000);
  if(thread.isAlive())throw new IOException("Allocator observer still alive");
  if(failure!=null)throw new IOException("Allocator observer failed",failure);
 }
}

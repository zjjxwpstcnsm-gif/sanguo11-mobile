#!/usr/bin/env python3
"""Stage bounded allocator sampling for later actual fire APK; no live mutation."""
from pathlib import Path
import json,hashlib
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/fire-memory-sampler300';BASE=ROOT/'out/session-a/fire-cache-apk296/source'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);name='app/src/androidTest/java/game/sanguo/mobile/SessionAFireFlowInstrumentation.java';source=BASE/name;before=source.read_text();s=before
 needle='public final class SessionAFireFlowInstrumentation extends SessionAScenePresentationInstrumentation {';assert s.count(needle)==1;s=s.replace(needle,needle+'\n private SessionAMemorySampler memorySampler;')
 needle='evidence.mkdirs();put("output",evidence);';assert s.count(needle)==1;s=s.replace(needle,needle+'memorySampler=new SessionAMemorySampler(new File(evidence,"allocator-samples.csv"));')
 needle='try{Files.write(new File(evidence,"result.txt").toPath(),result.getString("stream","").getBytes("UTF-8"));}'
 assert s.count(needle)==1;s=s.replace(needle,'try{if(memorySampler!=null)memorySampler.close();}catch(Throwable failure){result.putString("stream",result.getString("stream","")+"\\nFAIL memory sampling "+android.util.Log.getStackTraceString(failure));}\n '+needle)
 target=OUT/name;target.parent.mkdir(parents=True);target.write_text(s)
 sampler='''package game.sanguo.mobile;
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
    sink.write("sample,elapsedRealtimeMillis,javaUsed,javaTotal,javaLimit,nativeAllocated,consistent,freeBytes,totalAfter\\n");
    Runtime runtime=Runtime.getRuntime();int count=0;
    while(!stopped){
     if(count>=20000)throw new IOException("Allocator sample cap reached before lifecycle end");
     long total=runtime.totalMemory(),free=runtime.freeMemory(),limit=runtime.maxMemory(),after=runtime.totalMemory();
     boolean consistent=total==after&&free>=0&&free<=total&&total<=limit;
     long used=consistent?total-free:-1;
     sink.write(Integer.toString(count++));sink.write(',');sink.write(Long.toString(SystemClock.elapsedRealtime()));
     for(long value:new long[]{used,total,limit,Debug.getNativeHeapAllocatedSize()}){sink.write(',');sink.write(Long.toString(value));}
     sink.write(',');sink.write(consistent?"1":"0");sink.write(',');sink.write(Long.toString(free));sink.write(',');sink.write(Long.toString(after));
     sink.write('\\n');if(count%20==0)sink.flush();SystemClock.sleep(250);
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
'''
 # Preserve raw total/free/after values and mark inconsistent rows used=-1;
 # no GC/retry/fabricated peak or false case failure on concurrent heap resize.
 path=OUT/'app/src/androidTest/java/game/sanguo/mobile/SessionAMemorySampler.java';path.write_text(sampler)
 assert source.read_text()==before and not (ROOT/path.relative_to(OUT)).exists()
 report={'paths':[{'path':name,'beforeSha256':sha(source),'afterSha256':sha(target),'stagedPath':str(target)},{'path':str(path.relative_to(OUT)),'beforeSha256':None,'afterSha256':sha(path),'stagedPath':str(path)}],'samplePeriodMillis':250,'maximumSamplesPerProcess':20000,'requestsGcOrHeapDump':False,'recordsProcessPssOrGpu':False,'canonicalOrCurrent297Changed':False,'compiled':False,'actualInstalled':False,'scope':'Future independent test-only staged delta. Sampled Runtime used/total/limit and native allocator every250ms through fire normal lifecycle and Activity.finish; cold separate process. Bounded20000 rows and observer3s join; failure fails case instead of accepting missing evidence. Adds measurement allocation/scheduling overhead; no capture of instantaneous peak/allocating stack, GPU, source child RSS or ARM; actual separate APK/pair/normal SaveRNGToken/restoration required.','wholeGoalComplete':False}
 (DOC/'FIRE_MEMORY_SAMPLER300.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'paths':report['paths'],'actualInstalled':False}),flush=True)
if __name__=='__main__':main()

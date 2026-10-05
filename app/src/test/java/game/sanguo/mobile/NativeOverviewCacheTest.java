package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerArray;

/** Saturation regression uses the actual national ground, not a reduced demo. */
public final class NativeOverviewCacheTest {
 public static void main(String[] args)throws Exception {
  World w=ScenarioCatalog.load("coalition-190",0,20260924L);
  byte[] before=SaveCodec.encode(w);
  TerrainSurface s=new MapSceneSnapshot.Ground(w).surface;
  var field=TerrainSurface.class.getDeclaredField("lattice");field.setAccessible(true);
  float[] samples=new float[400*400];long started=System.nanoTime();
  for(int z=0;z<400;z++)for(int x=0;x<400;x++)samples[z*400+x]=s.sample(x*.5f,z*.5f);
  Object cache=field.get(s);
  // The original bounded map drops the beginning of a full sweep. This assertion
  // also runs against the unchanged input to retain the actual failure.
  if(cache instanceof Map && !((Map<?,?>)cache).containsKey(0L))throw new AssertionError("national sweep evicts first sample by clearing entire cache");
  if(cache instanceof AtomicIntegerArray){
   AtomicIntegerArray a=(AtomicIntegerArray)cache;
   if(a.length()>262144)throw new AssertionError("surface cache exceeds 1 MiB budget");
   System.out.println("cache_bytes="+4L*a.length());
  }
  long first=System.nanoTime()-started;
  Thread worker=new Thread(()->{for(int i=samples.length-1;i>=0;i--)if(samples[i]!=s.sample((i%400)*.5f,(i/400)*.5f))throw new AssertionError("worker height mismatch");});
  final Throwable[] failure={null};worker.setUncaughtExceptionHandler((t,e)->failure[0]=e);worker.start();
  started=System.nanoTime();
  for(int i=0;i<samples.length;i++)if(samples[i]!=s.sample((i%400)*.5f,(i/400)*.5f))throw new AssertionError("owner height mismatch");
  worker.join();if(failure[0]!=null)throw new AssertionError(failure[0]);
  if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("cache changed authority/RNG");
  // A new Ground always owns a separate cache, including unchanged map revisions.
  if(field.get(new MapSceneSnapshot.Ground(w).surface)==cache)throw new AssertionError("ground caches shared across worlds");
  System.out.println("PASS national cache saturation, concurrent exact samples, byte-identical authority; cold_ms="+first/1e6+" warm_ms="+(System.nanoTime()-started)/1e6+" (host only)");
 }
}

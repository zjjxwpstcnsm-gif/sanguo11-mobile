package game.sanguo.mobile;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
public final class CaptureQueueRegression362 {
 static int checks;
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);checks++;}
 public static void main(String[] args)throws Exception {
  ByteArrayOutputStream actual=new ByteArrayOutputStream(),expected=new ByteArrayOutputStream();
  SessionAPcmCaptureWriter q=new SessionAPcmCaptureWriter(actual,64,8192);
  Random r=new Random(362);byte[] block=new byte[8192];
  for(int i=0;i<48;i++){r.nextBytes(block);int n=(i%19+1)*4;expected.write(block,0,n);q.append(block,n);Arrays.fill(block,(byte)0);}
  q.finish();check(Arrays.equals(expected.toByteArray(),actual.toByteArray()),"FIFO with caller buffer reuse exact");check(q.writtenBytes()==expected.size(),"All bytes flushed");check(q.highWater()>0&&q.highWater()<=64,"Bounded actual queue");
  boolean closed=false;try{q.append(block,4);}catch(IOException e){closed=true;}check(closed,"closed append rejected");
  CountDownLatch writing=new CountDownLatch(1),release=new CountDownLatch(1);
  OutputStream stall=new OutputStream(){public void write(int b){}public void write(byte[] b,int o,int n)throws IOException{writing.countDown();try{release.await();}catch(InterruptedException e){throw new IOException(e);}}};
  q=new SessionAPcmCaptureWriter(stall,2,8);q.append(new byte[8],8);check(writing.await(2,TimeUnit.SECONDS),"Writer actually blocked on simulated slow disk");q.append(new byte[8],8);boolean overflow=false;try{q.append(new byte[8],8);}catch(IOException e){overflow=e.getMessage().contains("overflow");}check(overflow,"Overflow invalidates capture without blocking reader");release.countDown();boolean failed=false;try{q.finish();}catch(IOException e){failed=true;}check(failed,"Overflow cannot produce accepted terminal capture");
  q=new SessionAPcmCaptureWriter(new OutputStream(){public void write(int b)throws IOException{throw new IOException("real-write-failure");}},4,8);q.append(new byte[8],8);failed=false;try{q.finish();}catch(IOException e){failed=e.getMessage().contains("real-write-failure");}check(failed,"Disk failure propagated");
  q=new SessionAPcmCaptureWriter(new ByteArrayOutputStream(),4,8);failed=false;try{q.append(new byte[8],3);}catch(IOException e){failed=true;}check(failed,"Partial stereo frame rejected");q.finish();
  for(Thread t:Thread.getAllStackTraces().keySet())check(!t.getName().equals("session-a-pcm-disk")||!t.isAlive(),"All measurement writers exited");
  System.out.println("PASS "+checks+" exact bounded capture-writer checks");
 }
}

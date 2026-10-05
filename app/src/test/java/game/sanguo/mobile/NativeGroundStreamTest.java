package game.sanguo.mobile;

import game.sanguo.core.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Host checks of the actual bounded queue and national mesh producer. Not a device lifecycle test. */
public final class NativeGroundStreamTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void drainUntil(SceneWorkQueue<Integer> q,AtomicInteger last,int target)throws Exception{
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(last.get()!=target&&System.nanoTime()<until){q.drain(last::set,e->{throw new AssertionError(e);});Thread.sleep(1);}
        check(last.get()==target,"owner receives expected epoch result");
    }
    static void mailbox()throws Exception{
        try(SceneWorkQueue<Integer> q=new SceneWorkQueue<>()){
            CountDownLatch first=new CountDownLatch(1),second=new CountDownLatch(1);
            q.submitPhased(publish->{publish.accept(10);first.countDown();publish.accept(20);second.countDown();return 30;});
            check(first.await(2,TimeUnit.SECONDS),"first phase ready");
            check(!second.await(60,TimeUnit.MILLISECONDS),"paused owner applies bounded backpressure");
            check(q.pending()==1&&q.waiting()==0,"partial CPU job stays pending");
            AtomicInteger last=new AtomicInteger(-1);q.drain(last::set,e->{throw new AssertionError(e);});
            check(last.get()==10&&q.pending()==1,"draining first phase cannot signal complete");
            check(second.await(2,TimeUnit.SECONDS),"producer resumes after owner drain");
            q.drain(last::set,e->{throw new AssertionError(e);});
            check(last.get()==20&&q.pending()==1,"second phase cannot signal complete");
            drainUntil(q,last,30);check(q.pending()==0&&q.delivered()==3,"only final drain closes task");
            check(q.backpressureNanos()>0,"backpressure time observable");
            q.submitPhased(publish->{publish.accept(40);throw new IllegalStateException("after phase");});
            AtomicBoolean failed=new AtomicBoolean();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while(!failed.get()&&System.nanoTime()<until){q.drain(v->{check(v==40,"only partial before failure");check(q.pending()==1,"failure still pending");},e->failed.set(e instanceof IllegalStateException));Thread.sleep(1);}
            check(failed.get()&&q.pending()==0,"failure delivered after phase without READY spoof");
        }
        SceneWorkQueue<Integer> closed=new SceneWorkQueue<>();CountDownLatch first=new CountDownLatch(1),done=new CountDownLatch(1);
        closed.submitPhased(p->{try{p.accept(1);first.countDown();p.accept(2);return 3;}finally{done.countDown();}});
        check(first.await(2,TimeUnit.SECONDS),"close test ready");closed.close();
        check(done.await(2,TimeUnit.SECONDS)&&closed.pending()==0,"close unblocks paused producer");
        try(SceneWorkQueue<Integer> q=new SceneWorkQueue<>()){
            CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
            q.submitPhased(p->{started.countDown();while(true)try{release.await();break;}catch(InterruptedException ignored){}p.accept(-5);return -6;});
            check(started.await(2,TimeUnit.SECONDS),"stale phased worker entered");
            for(int i=0;i<100;i++){int n=i;q.submitPhased(p->{p.accept(n);return n+1000;});check(q.waiting()<=1,"one waiting request");}
            release.countDown();AtomicInteger last=new AtomicInteger(-1);
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(q.pending()!=0&&System.nanoTime()<until){q.drain(v->{check(v==99||v==1099,"never deliver stale phase");last.set(v);},e->{throw new AssertionError(e);});Thread.sleep(1);}
            check(last.get()==1099&&q.discarded()>0,"rapid replacement rejects uncooperative obsolete publisher");
        }
    }
    static void national()throws Exception{
        World world=ScenarioCatalog.load("huangjin-184",0,20260925L);byte[] authority=SaveCodec.encode(world);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(world);
        for(int r=-1;r<=g.height;r++)for(int q=-1;q<=g.width;q++)
            check(g.isBase(q,r)==g.bases.contains(new Hex(q,r)),"exact footprint membership including bounds");
        SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow((g.minX+g.maxX)/2,(g.minZ+g.maxZ)/2,200,200,86.256714f);
        AtomicInteger built=new AtomicInteger();CountDownLatch first=new CountDownLatch(1),hold=new CountDownLatch(1);
        try(SceneWorkQueue<List<SceneMesh>> queue=new SceneWorkQueue<>()){
            queue.submitPhased(publish->{
                SceneMesh.BuildStats stats=new SceneMesh.BuildStats();
                stats.firstLoadBatch=meshes->{
                    built.set(meshes.size());publish.accept(meshes);
                    if(meshes.size()==8){first.countDown();try{hold.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}}
                };
                return SceneMesh.ground(g,List.of(),window,stats);
            });
            check(first.await(10,TimeUnit.SECONDS),"real first eight meshes delivered before whole national ground");
            AtomicReference<List<SceneMesh>> seen=new AtomicReference<>(List.of());
            queue.drain(seen::set,e->{throw new AssertionError(e);});
            check(seen.get().size()==8&&built.get()==8&&queue.pending()==1,"first-screen meshes available while work remains");
            try{seen.get().clear();throw new AssertionError("mutable partial");}catch(UnsupportedOperationException expected){}
            hold.countDown();int count=8;long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(45);
            while(queue.pending()!=0&&System.nanoTime()<until){
                queue.drain(seen::set,e->{throw new AssertionError(e);});
                check(seen.get().size()>=count,"phase coverage never shrinks within same request");count=seen.get().size();Thread.sleep(1);
            }
            check(queue.pending()==0&&count==177,"all national chunks delivered without a new epoch");
            MessageDigest md=MessageDigest.getInstance("SHA-256");ByteBuffer n=ByteBuffer.allocate(4);
            for(SceneMesh m:seen.get()){
                for(float[] a:new float[][]{m.vertices,m.surfaceData})for(float f:a){n.clear();md.update(n.putFloat(f).array());}
                for(int i:m.indices){n.clear();md.update(n.putInt(i).array());}
            }
            check(HexFormat.of().formatHex(md.digest()).equals("e716c21a20f7043eed4ef2acaf6d77bc8c56a522a7fa61db65b89f6ab0a70b48"),"all streamed national bytes equal original v105");
            check(Arrays.equals(authority,SaveCodec.encode(world)),"complete authority/RNG unchanged");
            System.out.println("national chunks="+count+" deliveries="+queue.delivered()+" backpressureWallMs="+queue.backpressureNanos()/1e6+" HOST_ONLY");
        }finally{hold.countDown();}
    }
    public static void main(String[] args)throws Exception{
        mailbox();national();
        System.out.println("PASS ground phases: immutable exact geometry, bounded backpressure, final-only completion, errors, cancellation, stale epochs and close");
    }
}

package game.sanguo.mobile;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public final class SceneWorkQueueTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args)throws Exception{
        SceneWorkQueue<Integer> q=new SceneWorkQueue<>();
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        q.submit(()->{entered.countDown();while(true)try{release.await();break;}catch(InterruptedException ignored){}return -1;});
        check(entered.await(2,TimeUnit.SECONDS));
        for(int i=0;i<200;i++){int value=i;q.submit(()->value);check(q.waiting()<=1);}
        release.countDown();AtomicInteger result=new AtomicInteger(-2);
        long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(result.get()==-2&&System.nanoTime()<limit){q.drain(result::set,e->{throw new AssertionError(e);});Thread.yield();}
        check(result.get()==199);check(q.discarded()>=1);
        q.submit(()->{throw new IllegalArgumentException("decode");});AtomicBoolean failed=new AtomicBoolean();
        limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(!failed.get()&&System.nanoTime()<limit){q.drain(v->{throw new AssertionError();},e->failed.set(e instanceof IllegalArgumentException));Thread.yield();}
        check(failed.get());
        q.submit(()->5);q.invalidate();q.drain(v->{throw new AssertionError();},e->{throw new AssertionError();});
        AtomicBoolean rejected=new AtomicBoolean();Thread wrong=new Thread(()->{try{q.invalidate();}catch(IllegalStateException e){rejected.set(true);}});wrong.start();wrong.join();check(rejected.get());
        q.close();q.close();check(q.pending()==0&&q.waiting()==0);
        System.out.println("PASS bounded latest-wins, uncooperative stale worker, failure delivery, invalidation, owner thread, close");
    }
}

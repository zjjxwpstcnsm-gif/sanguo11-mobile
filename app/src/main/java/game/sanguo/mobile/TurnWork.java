package game.sanguo.mobile;

import android.os.Handler;
import android.os.Looper;
import game.sanguo.core.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/** Retained single-writer turn job. One finished faction at a time crosses into presentation. */
final class TurnWork {
    final World before;
    World after,visual;
    List<TurnJournal.Event> events=Collections.emptyList();
    int cursor,speed=1,playedEvents,publishedBatches,batchOwner=-1;
    float fraction;
    boolean paused,savedFinal,batchReady,skipAnimations;
    int visibleCount;
    final StringBuilder actionReport=new StringBuilder();
    String summary;
    Exception error;
    volatile boolean done,waitingForPlayback,cancelled;
    volatile World working;
    volatile long cloneMillis,computeMillis;
    private long waitNanos;
    private volatile World.TurnProgress progress;
    private Runnable observer;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Semaphore consumed=new Semaphore(0);
    private Thread worker;
    TurnWork(World before){this.before=before;}
    void observe(Runnable observer){this.observer=observer;if(observer!=null&&(visual!=null||done))observer.run();}
    private void notifyObserver(){if(!cancelled&&observer!=null)observer.run();}
    void start(){
        worker=new Thread(()->{
            World computed=null;TurnJournal journal=null;String report=null;Exception failure=null;
            long started=System.nanoTime();
            try{
                byte[] initial=SaveCodec.encode(before);
                computed=SaveCodec.decode(initial);final World render=SaveCodec.decode(initial);
                cloneMillis=(System.nanoTime()-started)/1000000L;working=computed;
                main.post(()->{if(cancelled)return;visual=render;notifyObserver();});
                journal=new TurnJournal(computed);final TurnJournal recording=journal;
                World.Result result=computed.nextTurn(p->{
                    recording.checkpoint(p.phase);progress=p;
                    if(p.boundary)publish(recording.drainEvents(),p.owner);
                });
                if(!result.ok)throw new IllegalStateException(result.message);
                journal.close();publish(journal.drainEvents(),computed.player);
                SaveCodec.validate(computed);report=UiModels.turnSummary(before,computed);
            }catch(Exception e){failure=e;}
            computeMillis=Math.max(0,(System.nanoTime()-started-waitNanos)/1000000L);
            final World result=computed;final String text=report;final Exception problem=failure;
            main.post(()->{if(cancelled)return;after=result;summary=text;error=problem;done=true;working=null;notifyObserver();});
        },"strategy-turn");worker.start();
    }
    private void publish(List<TurnJournal.Event> batch,int owner){
        if(cancelled)throw new IllegalStateException("Turn cancelled");
        if(batch.isEmpty())return;
        long start=System.nanoTime();waitingForPlayback=true;
        main.post(()->{if(cancelled)return;events=batch;cursor=0;fraction=0;batchOwner=owner;batchReady=true;publishedBatches++;notifyObserver();});
        try{consumed.acquire();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Turn cancelled",e);}
        finally{waitingForPlayback=false;waitNanos+=System.nanoTime()-start;}
    }
    /** UI thread acknowledges a complete immutable batch; the worker can now decide the next side. */
    void consumeBatch(){
        if(!batchReady)return;
        playedEvents+=cursor;events=Collections.emptyList();cursor=0;fraction=0;batchReady=false;consumed.release();
    }
    void cancel(){cancelled=true;observer=null;if(worker!=null)worker.interrupt();}
    String status(){
        if(batchReady||done&&!events.isEmpty()){
            String owner=batchOwner<0?"全局设施与后勤":before.faction(batchOwner);
            return owner+" · "+(paused?"已暂停":skipAnimations?"快速结算":"行动演示")+" · "+cursor+"/"+events.size()+" · "+speed+"×";
        }
        if(done)return "结算完成 · 实际运算 "+String.format(Locale.ROOT,"%.2f",computeMillis/1000.0)+" 秒";
        World.TurnProgress p=progress;if(p==null)return "正在复制局面 · 实际运算中…";
        return percent()+"% · "+(p.owner<0?"全局结算":before.faction(p.owner))+" · "+p.phase+" · 运算中";
    }
    int percent(){World.TurnProgress p=progress;return done?100:p==null?0:Math.min(100,p.completed*100/Math.max(1,p.total));}
}

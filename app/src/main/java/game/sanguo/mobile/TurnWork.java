package game.sanguo.mobile;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import game.sanguo.core.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Retained single writer; immutable faction batches are streamed without waiting for animation.
 * UI owns the queue/render world. The worker exclusively owns computed until its final handoff. */
final class TurnWork {
    static final long PRESENTATION_BUDGET_MS=8000;
    private static final int MAX_QUEUED_EVENTS=1600;
    final World before;
    final long startedAt=SystemClock.elapsedRealtime();
    World after,visual;
    List<TurnJournal.Event> events=Collections.emptyList();
    int cursor,speed=1,playedEvents,publishedBatches,batchOwner=-1;
    float fraction,criticalElapsed;
    boolean paused,savedFinal,batchReady,fullReplay;
    volatile boolean skipAnimations;
    int visibleCount,criticalsShown;
    long pauseStarted,pausedMillis,saveMillis,totalMillis;
    final StringBuilder actionReport=new StringBuilder();
    String summary,timings="";
    Exception error;
    volatile boolean done,waitingForPlayback,cancelled,compacted;
    volatile World working;
    volatile long cloneMillis,computeMillis;
    private volatile World.TurnProgress progress;
    private Runnable observer;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final AtomicInteger queuedEvents=new AtomicInteger();
    private final ArrayDeque<Batch> pending=new ArrayDeque<>();
    private Thread worker;
    private static final class Batch {
        final List<TurnJournal.Event> events;final int owner;
        Batch(List<TurnJournal.Event> events,int owner){this.events=events;this.owner=owner;}
    }
    TurnWork(World before){this.before=before;}
    void observe(Runnable observer){this.observer=observer;if(observer!=null&&(visual!=null||done))observer.run();}
    private void notifyObserver(){if(!cancelled&&observer!=null)observer.run();}
    long activeMillis(){return Math.max(0,SystemClock.elapsedRealtime()-startedAt-pausedMillis-(paused?SystemClock.elapsedRealtime()-pauseStarted:0));}
    boolean budgetExpired(){return !fullReplay&&activeMillis()>=PRESENTATION_BUDGET_MS;}
    boolean fastForward(){return skipAnimations||compacted||budgetExpired();}
    void pause(boolean value){
        if(paused==value)return;
        if(value)pauseStarted=SystemClock.elapsedRealtime();else pausedMillis+=SystemClock.elapsedRealtime()-pauseStarted;
        paused=value;
    }
    void start(){
        worker=new Thread(()->{
            World computed=null;TurnJournal journal=null;String report=null;Exception failure=null;
            StringBuilder profile=new StringBuilder();long started=System.nanoTime();final long[] stage={started};
            try{
                byte[] initial=SaveCodec.encode(before);
                computed=SaveCodec.decode(initial);final World render=SaveCodec.decode(initial);
                if(before.visualMap!=null){computed.visualMap=before.visualMap.copy();render.visualMap=before.visualMap.copy();}
                cloneMillis=(System.nanoTime()-started)/1000000L;working=computed;
                main.post(()->{if(cancelled)return;visual=render;notifyObserver();});
                journal=new TurnJournal(computed);final TurnJournal recording=journal;
                World.Result result=computed.nextTurn(p->{
                    if(cancelled)throw new IllegalStateException("Turn cancelled");
                    recording.checkpoint(p.phase);progress=p;
                    if(p.boundary){long now=System.nanoTime();profile.append(before.faction(p.owner)).append(" · ").append(p.phase).append(" ").append((now-stage[0])/1000000L).append("ms\n");android.util.Log.d("Turn52","PHASE side="+p.owner+" ms="+(now-stage[0])/1000000L+" "+p.phase);stage[0]=now;publish(recording.drainEvents(),p.owner);}
                });
                if(!result.ok)throw new IllegalStateException(result.message);
                journal.close();publish(journal.drainEvents(),computed.player);journal=null;
                SaveCodec.validate(computed);report=UiModels.turnSummary(before,computed);
            }catch(Exception e){failure=e;}
            finally{if(journal!=null)try{journal.close();}catch(Exception closing){if(failure==null)failure=closing;else failure.addSuppressed(closing);}}
            computeMillis=Math.max(0,(System.nanoTime()-started)/1000000L);
            final World result=computed;final String text=report,profileText=profile.toString();final Exception problem=failure;
            main.post(()->{if(cancelled)return;after=result;summary=text;error=problem;timings=profileText;done=true;working=null;notifyObserver();});
        },"strategy-turn");worker.start();
    }
    private void publish(List<TurnJournal.Event> batch,int owner){
        if(cancelled)throw new IllegalStateException("Turn cancelled");
        if(batch.isEmpty()||compacted)return;
        // Never let an unlimited replay queue exhaust memory during a long campaign or manual pause.
        if(queuedEvents.addAndGet(batch.size())>MAX_QUEUED_EVENTS){compacted=true;return;}
        Batch immutable=new Batch(batch,owner);
        main.post(()->{if(cancelled)return;pending.add(immutable);publishedBatches++;selectBatch();notifyObserver();});
    }
    void selectBatch(){
        if(batchReady||pending.isEmpty())return;Batch batch=pending.remove();
        events=batch.events;cursor=0;fraction=0;criticalElapsed=0;batchOwner=batch.owner;batchReady=true;
    }
    void consumeBatch(){
        if(!batchReady)return;
        playedEvents+=cursor;queuedEvents.addAndGet(-events.size());events=Collections.emptyList();cursor=0;fraction=0;criticalElapsed=0;batchReady=false;selectBatch();
    }
    void clearPresentation(){pending.clear();events=Collections.emptyList();cursor=0;batchReady=false;queuedEvents.set(0);}
    void cancel(){cancelled=true;observer=null;clearPresentation();if(worker!=null)worker.interrupt();}
    String status(){
        if(batchReady&&!fastForward()){
            String owner=batchOwner<0?"全局设施与后勤":before.faction(batchOwner);
            return owner+" · "+(paused?"已暂停":"行动演示")+" · "+cursor+"/"+events.size()+" · "+speed+"×"+(done?" · 计算完成":" · 后台计算中");
        }
        if(done)return "结算完成 · 实际运算 "+String.format(Locale.ROOT,"%.2f",computeMillis/1000.0)+" 秒";
        World.TurnProgress p=progress;if(p==null)return "正在复制局面 · 实际运算中…";
        return percent()+"% · "+(p.owner<0?"全局结算":before.faction(p.owner))+" · "+p.phase+(fastForward()?" · 已压缩演示":" · 运算中");
    }
    int percent(){World.TurnProgress p=progress;return done?100:p==null?0:Math.min(100,p.completed*100/Math.max(1,p.total));}
}

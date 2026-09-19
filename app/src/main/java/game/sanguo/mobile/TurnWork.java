package game.sanguo.mobile;

import android.os.Handler;
import android.os.Looper;
import game.sanguo.core.*;

/** Retained turn job; never retains a destroyed Activity or mutates its live World. */
final class TurnWork {
    final World before;
    World after,visual;
    java.util.List<TurnJournal.Event> events=java.util.Collections.emptyList();
    int cursor,speed=1;
    float fraction;
    boolean paused,savedFinal;
    int visibleCount;
    final StringBuilder actionReport=new StringBuilder();
    String summary;
    Exception error;
    volatile boolean done;
    volatile World working;
    volatile long cloneMillis;
    private volatile World.TurnProgress progress;
    private Runnable observer;
    private final Handler main = new Handler(Looper.getMainLooper());
    TurnWork(World before) {this.before=before;}
    void observe(Runnable observer) {this.observer=observer;if(done && observer!=null) observer.run();}
    void start() {
        new Thread(() -> {
            World computed=null,render=null;String report=null;Exception failure=null;
            TurnJournal journal=null;
            try {
                long cloneStart=System.nanoTime();byte[] initial=SaveCodec.encode(before);
                computed=SaveCodec.decode(initial);render=SaveCodec.decode(initial);
                cloneMillis=(System.nanoTime()-cloneStart)/1000000L;working=computed;
                journal=new TurnJournal(computed);final TurnJournal recording=journal;
                computed.nextTurn(p->{recording.checkpoint(p.phase);progress=p;});journal.close();
                SaveCodec.validate(computed);report=UiModels.turnSummary(before,computed);
            }
            catch(Exception e) {failure=e;}
            final World result=computed,renderResult=render;final String text=report;final Exception problem=failure;
            final java.util.List<TurnJournal.Event> recorded=journal==null?java.util.Collections.emptyList():journal.events();
            main.post(() -> {after=result;visual=renderResult;events=recorded;summary=text;error=problem;done=true;working=null;if(observer!=null)observer.run();});
        },"strategy-turn").start();
    }
    String status() {
        if(done)return (paused?"已暂停":"行动演示")+" · "+cursor+"/"+events.size()+" · "+speed+"× · 点此控制";
        World.TurnProgress p=progress;
        if(p==null)return "正在准备旬结算…";
        return percent()+"% · "+(p.owner<0?"全局结算":before.faction(p.owner))+" · "+p.phase;
    }
    int percent(){World.TurnProgress p=progress;return done?100:p==null?0:Math.min(100,p.completed*100/Math.max(1,p.total));}
}

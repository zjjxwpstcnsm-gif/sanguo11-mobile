package game.sanguo.mobile;

import android.os.Handler;
import android.os.Looper;
import game.sanguo.core.*;

/** Retained turn job; never retains a destroyed Activity or mutates its live World. */
final class TurnWork {
    final World before;
    World after;
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
            World computed=null;String report=null;Exception failure=null;
            try {long cloneStart=System.nanoTime();computed=SaveCodec.decode(SaveCodec.encode(before));cloneMillis=(System.nanoTime()-cloneStart)/1000000L;working=computed;computed.nextTurn(p->progress=p);SaveCodec.validate(computed);report=UiModels.turnSummary(before,computed);}
            catch(Exception e) {failure=e;}
            final World result=computed; final String text=report; final Exception problem=failure;
            main.post(() -> {after=result;summary=text;error=problem;done=true;if(observer!=null)observer.run();});
        },"strategy-turn").start();
    }
    String status() {
        if(done)return "";
        World.TurnProgress p=progress;
        if(p==null)return "正在准备旬结算…";
        return percent()+"% · "+(p.owner<0?"全局结算":before.faction(p.owner))+" · "+p.phase;
    }
    int percent(){World.TurnProgress p=progress;return done?100:p==null?0:Math.min(100,p.completed*100/Math.max(1,p.total));}
}

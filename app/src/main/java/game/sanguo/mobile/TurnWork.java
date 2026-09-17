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
    private Runnable observer;
    private final Handler main = new Handler(Looper.getMainLooper());
    TurnWork(World before) {this.before=before;}
    void observe(Runnable observer) {this.observer=observer;if(done && observer!=null) observer.run();}
    void start() {
        new Thread(() -> {
            World computed=null;String report=null;Exception failure=null;
            try {long cloneStart=System.nanoTime();computed=SaveCodec.decode(SaveCodec.encode(before));cloneMillis=(System.nanoTime()-cloneStart)/1000000L;working=computed;computed.nextTurn();SaveCodec.validate(computed);report=UiModels.turnSummary(before,computed);}
            catch(Exception e) {failure=e;}
            final World result=computed; final String text=report; final Exception problem=failure;
            main.post(() -> {after=result;summary=text;error=problem;done=true;if(observer!=null)observer.run();});
        },"strategy-turn").start();
    }
    String status() {
        if(done)return "";
        World w=working;
        if(w==null)return "正在准备旬结算…";
        int owner=w.active;
        if(owner==w.player)return "正在执行全局旬结算 · 局面复制 "+cloneMillis+"ms";
        return "电脑行动中 · "+w.faction(owner)+" · "+w.date();
    }
}

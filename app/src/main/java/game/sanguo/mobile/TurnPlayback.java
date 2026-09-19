package game.sanguo.mobile;

import android.os.SystemClock;
import game.sanguo.core.*;

/** Main-thread presentation only. Retained cursor/fraction live in TurnWork, never in the Activity. */
final class TurnPlayback {
    private final MapView map;
    private final TurnWork work;
    private final Runnable changed,finished;
    private boolean detached;
    private long last;
    private TurnJournal.Event current;
    TurnPlayback(MapView map,TurnWork work,Runnable changed,Runnable finished){this.map=map;this.work=work;this.changed=changed;this.finished=finished;}
    void start(){last=SystemClock.uptimeMillis();map.invalidateScene();map.setWorld(work.visual,null,-1);if(work.paused&&work.cursor<work.events.size())map.replayFrame(work.events.get(work.cursor),work.fraction);map.postOnAnimation(tick);}
    void detach(){detached=true;map.removeCallbacks(tick);}
    void pause(boolean paused){work.paused=paused;last=SystemClock.uptimeMillis();}
    void skip(){work.skipAnimations=true;work.paused=false;}
    private final Runnable tick=this::step;
    private void step(){
        if(detached)return;
        long now=SystemClock.uptimeMillis(),elapsed=Math.max(0,now-last);last=now;
        if(work.paused){map.postDelayed(tick,60);return;}
        boolean applied=false;long deadline=now+7; // Off-screen bookkeeping is also frame-budgeted.
        while(work.cursor<work.events.size()){
            TurnJournal.Event event=work.events.get(work.cursor);
            if(current!=event){current=event;elapsed=0;}
            boolean visible=!work.skipAnimations&&UiMotion.enabled()&&event.visibleAction()&&map.replayVisible(event);
            if(visible){
                if(work.fraction==0){work.visibleCount++;if(work.actionReport.length()<20000)work.actionReport.append(work.before.faction(event.owner)).append(" · ").append(event.message).append('\n');}
                // Preserve readable beats after GC, backgrounding or a slow frame; never jump a whole action.
                work.fraction=Math.min(1,work.fraction+Math.max(1,Math.min(50,elapsed))*Math.max(1,work.speed)/(float)event.durationMillis());
                map.replayFrame(event,work.fraction);
                if(work.fraction<1){if(applied){map.invalidateScene();map.setWorld(work.visual,null,-1);}map.postOnAnimation(tick);return;}
            }
            event.applyVisual(work.visual);work.cursor++;work.fraction=0;current=null;applied=true;map.replayFrame(null,0);
            if(SystemClock.uptimeMillis()>=deadline)break;
        }
        if(applied){map.invalidateScene();map.setWorld(work.visual,null,-1);changed.run();}
        if(work.cursor>=work.events.size()){
            if(work.batchReady){work.consumeBatch();changed.run();}
            if(work.done){finished.run();return;}
            // A faction is computing on the worker, not a fake loading delay.
            map.postDelayed(tick,24);return;
        }
        map.postOnAnimation(tick);
    }
}

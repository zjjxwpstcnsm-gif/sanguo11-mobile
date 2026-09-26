package game.sanguo.mobile;

import android.os.SystemClock;
import game.sanguo.core.*;

/** Presentation alone is budgeted; no gameplay, resource, AI or RNG step is ever skipped. */
final class TurnPlayback {
    private final MapHost map;
    private final TurnWork work;
    private final Runnable changed,finished;
    private boolean detached,finishedOnce;
    private void finish(){if(finishedOnce)return;finishedOnce=true;map.pauseEffects(false);map.removeCallbacks(tick);finished.run();}
    private long last;
    private TurnJournal.Event current;
    TurnPlayback(MapHost map,TurnWork work,Runnable changed,Runnable finished){this.map=map;this.work=work;this.changed=changed;this.finished=finished;}
    void start(){map.setCriticalSkip(()->{work.criticalElapsed=CriticalScene.DURATION;map.criticalFrame(null,0);});last=SystemClock.uptimeMillis();map.invalidateScene();map.setWorld(work.visual,null,-1);map.postOnAnimation(tick);}
    void detach(){detached=true;map.removeCallbacks(tick);map.setCriticalSkip(null);map.criticalFrame(null,0);map.replayFrame(null,0);map.pauseEffects(false);}
    void pause(boolean paused){work.pause(paused);map.pauseEffects(paused);last=SystemClock.uptimeMillis();}
    void skip(){work.skipAnimations=true;work.pause(false);map.pauseEffects(false);map.replayFrame(null,0);map.criticalFrame(null,0);map.removeCallbacks(tick);step();}
    private final Runnable tick=this::step;
    private void step(){
        if(detached||finishedOnce)return;
        long now=SystemClock.uptimeMillis(),elapsed=Math.max(0,now-last);last=now;
        if(work.paused){map.postDelayed(tick,60);return;}
        if(work.fastForward()){
            map.criticalFrame(null,0);map.replayFrame(null,0);
            // Completed authoritative state replaces presentation directly, never replaying commands.
            if(work.done){work.clearPresentation();finish();return;}
            map.postDelayed(tick,24);return;
        }
        work.selectBatch();boolean applied=false;long deadline=now+6;
        while(work.cursor<work.events.size()){
            TurnJournal.Event event=work.events.get(work.cursor);
            if(current!=event){current=event;elapsed=0;}
            boolean visible=UiMotion.enabled()&&event.visibleAction()&&map.replayVisible(event);
            if(visible){
                if(event.critical!=null&&work.criticalElapsed<CriticalScene.DURATION){
                    if(!event.id.equals(work.announcedCritical)){work.announcedCritical=event.id;work.criticalsShown++;}
                    work.criticalElapsed+=Math.max(1,Math.min(50,elapsed))*Math.max(1,work.speed);
                    map.replayFrame(event,0);map.criticalFrame(event.critical,work.criticalElapsed/CriticalScene.DURATION);
                    if(applied){map.invalidateScene();map.setWorld(work.visual,null,-1);}
                    map.postOnAnimation(tick);return;
                }
                map.criticalFrame(null,0);
                if(!event.id.equals(work.announcedEvent)){work.announcedEvent=event.id;work.visibleCount++;if(work.actionReport.length()<20000)work.actionReport.append(work.before.faction(event.owner)).append(" · ").append(event.message).append('\n');}
                work.fraction=Math.min(1,work.fraction+Math.max(1,Math.min(50,elapsed))*Math.max(1,work.speed)/(float)event.durationMillis());
                map.replayFrame(event,work.fraction);
                if(work.fraction<1){if(applied){map.invalidateScene();map.setWorld(work.visual,null,-1);}map.postOnAnimation(tick);return;}
            }
            map.finishReplay(event);event.applyVisual(work.visual);work.cursor++;work.fraction=0;work.criticalElapsed=0;current=null;applied=true;map.replayFrame(null,0);
            if(SystemClock.uptimeMillis()>=deadline||work.budgetExpired())break;
        }
        if(applied){map.invalidateScene();map.setWorld(work.visual,null,-1);changed.run();}
        if(work.cursor>=work.events.size()){
            if(work.batchReady){work.consumeBatch();changed.run();}
            if(work.done&&!work.batchReady){finish();return;}
            map.postDelayed(tick,16);return;
        }
        map.postOnAnimation(tick);
    }
}

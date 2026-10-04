package game.sanguo.mobile;

import android.animation.ValueAnimator;
import android.widget.TextView;
import game.sanguo.api.StateToken;
import game.sanguo.api.GameEvent;
import game.sanguo.api.TechniquePointsFact;

/** Presentation of committed scalar values only; no authority, rules or RNG. */
final class TechniquePointsHud {
    private final TextView badge;
    private final java.util.function.BiConsumer<String,Boolean> sound;
    private StateToken state;
    private int player=-1,target,displayed;
    private boolean foreground,pending;
    private String identity;
    private ValueAnimator animator;
    private Runnable startFrame;
    private final TechniqueFactQueue facts=new TechniqueFactQueue();
    private boolean factsMode;
    private int committedPoints;
    int rolls;
    TechniquePointsHud(TextView badge,java.util.function.BiConsumer<String,Boolean> sound){this.badge=badge;this.sound=sound;}
    private void show(int value){displayed=value;badge.setText("技巧\n"+value);}
    private void cancelMotion(){if(startFrame!=null){badge.removeCallbacks(startFrame);startFrame=null;}if(animator!=null){ValueAnimator old=animator;animator=null;old.cancel();}}
    private void settle(){cancelMotion();pending=false;show(target);}
    void foreground(boolean value){foreground=value;facts.foreground(value);if(!value)discardPending();}
    void discardPending(){facts.discard();if(factsMode)target=committedPoints;settle();}
    void update(StateToken next,int side,int points,boolean defer){
        if(factsMode){syncFactsBaseline(next,side,points);return;}
        if(state==null||!state.sessionId.equals(next.sessionId)||state.generation!=next.generation||player!=side){
            target=points;settle();state=next;player=side;return;
        }
        if(next.revision<=state.revision)return;
        state=next;if(points==target)return;
        cancelMotion();
        // Complete an interrupted old roll before presenting the next committed
        // delta. Sound direction must follow authority, not an intermediate digit.
        show(target);
        target=points;identity="technique:"+next.sessionId+":"+next.generation+":"+next.revision+":"+side;
        pending=true;if(!defer)releasePending();
    }
    void releasePending(){
        if(factsMode){pumpFacts();return;}
        if(!pending)return;pending=false;
        if(!foreground){settle();return;}
        int from=displayed,to=target;if(from==to)return;
        badge.setContentDescription("本势力技巧点 "+to+" 点，变化 "+(to>from?"+":"")+(to-from));
        String cue=identity;
        if(!UiMotion.enabled()){sound.accept(cue,to>from);rolls++;show(to);return;}
        startFrame=()->{
            startFrame=null;if(!foreground)return;
            sound.accept(cue,to>from);rolls++;
            animator=ValueAnimator.ofInt(from,to);animator.setDuration(550);
            animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            animator.addUpdateListener(a->show((Integer)a.getAnimatedValue()));animator.start();
        };
        badge.postOnAnimation(startFrame);
    }
    /** The sequential host patch calls this instead of enabling the legacy NET route. */
    void syncFactsBaseline(StateToken next,int side,int points){
        boolean changed=state==null||!state.sessionId.equals(next.sessionId)||state.generation!=next.generation||player!=side;
        if(staleBaseline(next))return;
        if(!factsMode||changed){cancelMotion();pending=false;}
        factsMode=true;facts.baseline(next,side);state=next;player=side;committedPoints=points;
        if(changed||animator==null&&startFrame==null&&facts.size()==0){target=points;show(points);}
        pumpFacts();
    }
    TechniqueFactQueue.Result committedFacts(GameEvent event,int side){
        if(!factsMode)return TechniqueFactQueue.Result.IGNORED;
        TechniqueFactQueue.Result result=facts.committed(event,side);
        if(result==TechniqueFactQueue.Result.RESET||result==TechniqueFactQueue.Result.RESYNC)discardPending();
        else pumpFacts();
        return result;
    }
    boolean mediaNeedsResync(){return facts.needsResync();}
    private boolean staleBaseline(StateToken next){return state!=null&&state.sessionId.equals(next.sessionId)
        &&(next.generation<state.generation||next.generation==state.generation&&next.revision<state.revision);}
    void resynchronizeFacts(StateToken next,int side,int points){if(staleBaseline(next))return;discardPending();facts.resynchronize(next,side);syncFactsBaseline(next,side,points);}
    void releasePresentation(String parent){facts.releasePresentation(parent);pumpFacts();}
    void skipPresentation(String parent){facts.skipPresentation(parent);pumpFacts();}
    private void pumpFacts(){
        if(!factsMode||!foreground||animator!=null||startFrame!=null)return;
        TechniquePointsFact fact=facts.poll();if(fact==null)return;
        show(fact.before);target=fact.after;
        startFrame=()->{
            startFrame=null;if(!foreground)return;
            sound.accept(fact.id,fact.delta>0);rolls++;
            badge.setContentDescription("本势力技巧点 "+fact.after+" 点，变化 "+(fact.delta>0?"+":"")+fact.delta);
            if(!UiMotion.enabled()){show(fact.after);pumpFacts();return;}
            ValueAnimator motion=ValueAnimator.ofInt(fact.before,fact.after);animator=motion;
            motion.setDuration(550);motion.setInterpolator(new android.view.animation.DecelerateInterpolator());
            motion.addUpdateListener(a->show((Integer)a.getAnimatedValue()));
            motion.addListener(new android.animation.AnimatorListenerAdapter(){
                @Override public void onAnimationEnd(android.animation.Animator animation){if(animator==motion){animator=null;pumpFacts();}}
            });motion.start();
        };
        badge.postOnAnimation(startFrame);
    }
    void close(){foreground=false;discardPending();facts.close();}
}

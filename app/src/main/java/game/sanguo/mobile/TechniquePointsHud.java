package game.sanguo.mobile;

import android.animation.ValueAnimator;
import android.widget.TextView;
import game.sanguo.api.StateToken;

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
    int rolls;
    TechniquePointsHud(TextView badge,java.util.function.BiConsumer<String,Boolean> sound){this.badge=badge;this.sound=sound;}
    private void show(int value){displayed=value;badge.setText("技巧\n"+value);}
    private void cancelMotion(){if(startFrame!=null){badge.removeCallbacks(startFrame);startFrame=null;}if(animator!=null){animator.cancel();animator=null;}}
    private void settle(){cancelMotion();pending=false;show(target);}
    void foreground(boolean value){foreground=value;if(!value)settle();}
    void discardPending(){settle();}
    void update(StateToken next,int side,int points,boolean defer){
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
    void close(){foreground=false;settle();}
}

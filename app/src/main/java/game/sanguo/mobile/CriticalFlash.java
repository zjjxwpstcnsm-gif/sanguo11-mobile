package game.sanguo.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.*;
import game.sanguo.core.*;

/** A brief portrait freeze, timed from its first rendered frame rather than command execution. */
final class CriticalFlash extends View {
    private final CriticalScene scene;
    private long began;
    private final Runnable finished;
    private final Runnable timeout=this::dismiss;
    private boolean ended;
    CriticalFlash(Context context,World world,CriticalHit hit,Runnable finished){
        super(context);this.finished=finished;scene=new CriticalScene(context);scene.set(world,hit);
        setWillNotDraw(false);setAlpha(1f);setElevation(48*getResources().getDisplayMetrics().density);
        setClickable(true);setFocusable(true);setContentDescription("战法暴击 · "+hit.name+" · "+hit.tactic+" · 点击跳过");
        setOnClickListener(v->dismiss());
    }
    void dismiss(){if(ended)return;ended=true;removeCallbacks(timeout);finished.run();}
    @Override protected void onDraw(Canvas c){
        if(ended)return;
        // The first frame is already opaque. A short freeze should not spend its entire
        // lifetime on transparent enter frames while the map is rebuilding its layers.
        scene.draw(c,getWidth(),getHeight(),.45f);
        if(began==0){began=SystemClock.uptimeMillis();postDelayed(timeout,(long)CriticalScene.DURATION);}
    }
    @Override protected void onDetachedFromWindow(){removeCallbacks(timeout);super.onDetachedFromWindow();}
}

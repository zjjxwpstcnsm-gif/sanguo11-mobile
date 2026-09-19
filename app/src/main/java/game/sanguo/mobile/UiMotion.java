package game.sanguo.mobile;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.PathInterpolator;

/** Short, interruptible motion. Never schedules work while a surface is idle. */
final class UiMotion {
    static final PathInterpolator EASE = new PathInterpolator(.2f, 0f, 0f, 1f);
    private UiMotion() {}
    static boolean enabled() { return ValueAnimator.areAnimatorsEnabled(); }
    static void enter(View view, boolean vertical) {
        view.animate().cancel();
        view.setAlpha(1f); view.setTranslationX(0); view.setTranslationY(0);
        if (!enabled() || !view.isAttachedToWindow()) return;
        float offset=12*view.getResources().getDisplayMetrics().density;
        view.setAlpha(.78f);
        if(vertical)view.setTranslationY(offset);else view.setTranslationX(offset);
        view.animate().alpha(1f).translationX(0).translationY(0)
            .setDuration(180).setInterpolator(EASE).start();
    }
    static void surface(View view, boolean visible, boolean vertical) {
        boolean requested=Boolean.TRUE.equals(view.getTag());
        if(requested==visible && view.getVisibility()==(visible?View.VISIBLE:View.GONE))return;
        if(requested==visible)return;
        view.setTag(visible);view.animate().cancel();
        if(visible){view.setVisibility(View.VISIBLE);enter(view,vertical);return;}
        if(!enabled()||!view.isAttachedToWindow()){
            view.setAlpha(1);view.setTranslationX(0);view.setTranslationY(0);view.setVisibility(View.GONE);return;
        }
        view.animate().alpha(0).translationX(vertical?0:12*view.getResources().getDisplayMetrics().density)
            .translationY(vertical?12*view.getResources().getDisplayMetrics().density:0)
            .setDuration(120).setInterpolator(EASE).withEndAction(()->{
                if(!Boolean.TRUE.equals(view.getTag()))view.setVisibility(View.GONE);
                view.setAlpha(1);view.setTranslationX(0);view.setTranslationY(0);
            }).start();
    }
}

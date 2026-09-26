package game.sanguo.mobile;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

/** Recover a confirmed lost HWUI backing Surface, not a stale model or a slow 3D frame.
 * API29 can release Window's Java Surface after dequeueBuffer fails while the separate
 * Filament Surface still presents. Invalidating a child then never relayouts the Window.
 * A single public Window attribute reapply reacquires it without replacing any page,
 * Activity, engine or saved state. A successful Window copy must rearm the loss latch.
 */
final class WindowSurfaceRecovery implements AutoCloseable {
    private static final long QUIET_MS=1000;
    private final Window window;
    private final View decor;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final WindowSurfaceRecoveryState state=new WindowSurfaceRecoveryState();
    private boolean closed,armed,copyPending,wanted;
    private long lastFrame,lastRequest,lastProbe,frames,copies,probeErrors;
    private ViewTreeObserver observed;
    private final Runnable check=this::check;
    private final ViewTreeObserver.OnDrawListener drawing=this::request;
    private final ViewTreeObserver.OnWindowFocusChangeListener focus=focused->{if(focused)request();else pause();};
    private final Window.OnFrameMetricsAvailableListener metrics=(w,m,d)->{if(!closed){frames++;lastFrame=SystemClock.uptimeMillis();}};
    private final View.OnAttachStateChangeListener attach=new View.OnAttachStateChangeListener(){
        @Override public void onViewAttachedToWindow(View v){listen();request();}
        @Override public void onViewDetachedFromWindow(View v){pause();unlisten();}
    };
    WindowSurfaceRecovery(Window window){
        owner();this.window=window;decor=window.getDecorView();
        if(decor.getTag(R.id.window_surface_recovery)!=null)throw new IllegalStateException("duplicate Window recovery owner");
        decor.setTag(R.id.window_surface_recovery,this);decor.addOnAttachStateChangeListener(attach);
        window.addOnFrameMetricsAvailableListener(metrics,handler);listen();request();
    }
    static void changed(View child){
        Object value=child.getRootView().getTag(R.id.window_surface_recovery);
        if(value instanceof WindowSurfaceRecovery)((WindowSurfaceRecovery)value).request();
    }
    static String report(View child){
        Object value=child.getRootView().getTag(R.id.window_surface_recovery);
        if(!(value instanceof WindowSurfaceRecovery))return "windowRecovery=unattached";
        WindowSurfaceRecovery s=(WindowSurfaceRecovery)value;
        return "windowRecovery="+(s.state.latched?"AWAITING_BACKING":"OBSERVING")+" windowFrames="+s.frames
            +" backingCopies="+s.copies+" confirmedLosses="+s.state.losses+" relayouts="+s.state.relayouts
            +" copyPending="+s.copyPending+" probeErrors="+s.probeErrors;
    }
    private static void owner(){if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Window owner thread required");}
    private void listen(){
        if(closed)return;ViewTreeObserver next=decor.getViewTreeObserver();if(observed==next)return;
        unlisten();observed=next;observed.addOnDrawListener(drawing);observed.addOnWindowFocusChangeListener(focus);
    }
    private void unlisten(){
        if(observed!=null&&observed.isAlive()){observed.removeOnDrawListener(drawing);observed.removeOnWindowFocusChangeListener(focus);}observed=null;
    }
    private boolean interactive(){return !closed&&decor.isAttachedToWindow()&&decor.isShown()&&decor.hasWindowFocus()
        &&decor.getWidth()>0&&decor.getHeight()>0;}
    void request(){owner();if(!interactive())return;wanted=true;lastRequest=SystemClock.uptimeMillis();arm(QUIET_MS);}
    private void arm(long delay){if(!armed&&!copyPending&&!closed&&wanted){armed=true;handler.postDelayed(check,Math.max(1,delay));}}
    private void pause(){owner();handler.removeCallbacks(check);armed=false;wanted=false;state.invalidateCallbacks();}
    private void check(){
        owner();armed=false;if(!wanted||!interactive())return;
        long now=SystemClock.uptimeMillis();
        // Healthy HWUI progress needs neither readback nor relayout. An idle healthy
        // window performs at most one tiny confirmation, then has no timer.
        if(!state.latched&&lastFrame>=lastRequest){wanted=false;return;}
        if(!state.latched&&now-lastFrame<QUIET_MS){arm(QUIET_MS-(now-lastFrame));return;}
        if(now-lastProbe<QUIET_MS){arm(QUIET_MS-(now-lastProbe));return;}
        if(copyPending)return;lastProbe=now;copyPending=true;
        final long ticket=state.epoch;final Bitmap sample=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);
        try{
            PixelCopy.request(window,new Rect(0,0,1,1),sample,result->{
                owner();copyPending=false;
                try{
                    if(!state.accepts(ticket)||!interactive())return;
                    if(result==PixelCopy.SUCCESS){copies++;boolean recovered=state.latched;state.copied();
                        if(recovered)Log.i("SanguoWindow","backing reacquired without page replacement "+report(decor));
                        wanted=lastRequest>lastProbe;
                    }else{probeErrors++;} // TIMEOUT/no-data is not proof of a missing backing Surface.
                }finally{sample.recycle();arm(QUIET_MS);}
            },handler);
        }catch(IllegalArgumentException error){
            copyPending=false;sample.recycle();
            if(state.accepts(ticket)&&interactive()&&WindowSurfaceRecoveryState.missingBacking(error.getMessage())){
                if(state.missing()){
                    Log.w("SanguoWindow","confirmed missing Window backing Surface; same-Window relayout "+report(decor),error);
                    try{window.setAttributes(window.getAttributes());}
                    catch(RuntimeException failure){probeErrors++;Log.e("SanguoWindow","Window relayout rejected; no Activity restart",failure);}
                }
            }else{probeErrors++;wanted=false;}
            arm(QUIET_MS);
        }
    }
    @Override public void close(){
        owner();if(closed)return;pause();closed=true;unlisten();decor.removeOnAttachStateChangeListener(attach);
        window.removeOnFrameMetricsAvailableListener(metrics);
        if(decor.getTag(R.id.window_surface_recovery)==this)decor.setTag(R.id.window_surface_recovery,null);
        // An in-flight PixelCopy retains its own four-byte bitmap until its callback;
        // the epoch above prevents it from touching a detached/replaced Window.
    }
}

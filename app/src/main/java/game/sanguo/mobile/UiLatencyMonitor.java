package game.sanguo.mobile;
import android.os.*;
import android.util.Log;
/** Real main-looper scheduling delay. Includes host contention; not GPU time. */
final class UiLatencyMonitor implements Runnable {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active;
    private long due,count,max,total;
    void start(){if(active)return;active=true;due=SystemClock.elapsedRealtime()+50;handler.postDelayed(this,50);}
    void stop(){active=false;handler.removeCallbacks(this);Log.i("SceneTiming",summary());}
    public void run(){if(!active)return;long now=SystemClock.elapsedRealtime(),delay=Math.max(0,now-due);count++;total+=delay;max=Math.max(max,delay);if(delay>=100)Log.w("SceneTiming","main_looper_delay_ms="+delay);due=now+50;handler.postDelayed(this,50);}
    String summary(){return "main_looper_samples="+count+" main_looper_delay_max_ms="+max+" main_looper_delay_total_ms="+total+" wall_clock_includes_scheduling=true";}
}

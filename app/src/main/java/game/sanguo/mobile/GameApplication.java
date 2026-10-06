package game.sanguo.mobile;

import android.app.Application;

/** Process lifecycle owner. Configuration/page/renderer changes only bind and unbind views. */
public final class GameApplication extends Application {
    private NativeGameHost host;
    private SoundEffects sounds;
    @Override public void onCreate(){super.onCreate();
        android.app.ActivityManager manager=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo memory=new android.app.ActivityManager.MemoryInfo();manager.getMemoryInfo(memory);
        android.util.Log.i("SanguoMemory","heapBudget normalMiB="+manager.getMemoryClass()+" largeMiB="+manager.getLargeMemoryClass()+" actualBytes="+Runtime.getRuntime().maxMemory()+" deviceRamBytes="+memory.totalMem+" largeHeap="+((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP)!=0));
        host=new NativeGameHost(this);}
    NativeGameHost host(){return host;}
    SoundEffects sounds(){if(sounds==null)sounds=new SoundEffects(this);return sounds;}
}

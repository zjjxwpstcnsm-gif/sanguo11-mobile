package game.sanguo.mobile;

import android.app.Application;

/** Process lifecycle owner. Configuration/page/renderer changes only bind and unbind views. */
public final class GameApplication extends Application {
    private NativeGameHost host;
    @Override public void onCreate(){super.onCreate();host=new NativeGameHost(this);}
    NativeGameHost host(){return host;}
}

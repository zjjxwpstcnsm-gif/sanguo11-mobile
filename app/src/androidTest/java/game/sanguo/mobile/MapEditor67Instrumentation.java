package game.sanguo.mobile;

import android.app.*;
import android.os.*;
import java.io.*;

/** Standalone installed probe; retains all historical smoke runners unchanged. */
public final class MapEditor67Instrumentation extends Instrumentation {
    volatile Activity current;
    @Override public void callActivityOnResume(Activity activity){super.callActivityOnResume(activity);current=activity;}
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{new MapEditor67Probe(this).run();result.putString("stream","MAP_EDITOR67 ANDROID PASS: actual editor controls, gestures, immutable library, backup recovery and custom new game.\n");finish(Activity.RESULT_OK,result);}catch(Throwable failure){StringWriter trace=new StringWriter();failure.printStackTrace(new PrintWriter(trace));result.putString("stream","MAP_EDITOR67 ANDROID FAIL: "+trace+"\n");finish(Activity.RESULT_CANCELED,result);}}
}

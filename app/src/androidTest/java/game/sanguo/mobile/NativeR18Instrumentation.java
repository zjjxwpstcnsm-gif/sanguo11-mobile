package game.sanguo.mobile;

import android.app.*;
import android.content.*;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Recovery on the real host; small legal fixture, not nationwide or native-abort acceptance. */
public final class NativeR18Instrumentation extends SceneInstrumentation {
    private String expected;
    @Override public void onCreate(Bundle b){expected=b==null?null:b.getString("source");super.onCreate(b);}
    private SharedPreferences prefs(){return getTargetContext().getSharedPreferences("map-renderer",0);}
    private byte[] authority(){byte[][] b={null};runOnMainSync(()->{try{b[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return b[0];}
    private void fault(){try{var m=MapHost.class.getDeclaredMethod("fallback",Throwable.class);m.setAccessible(true);m.invoke(host,new IOException("R18 controlled renderer failure"));}catch(Exception e){throw new RuntimeException(e);}}
    private void stale(FilamentMapView old){try{var m=MapHost.class.getDeclaredMethod("completeNativeRetry",FilamentMapView.class);m.setAccessible(true);m.invoke(host,old);}catch(Exception e){throw new RuntimeException(e);}}
    @Override public void onStart(){Bundle result=new Bundle();try{
        check(expected!=null&&expected.matches("[0-9a-f]{40}")&&expected.equals(BuildConfig.SOURCE_REVISION),"installed source matches exact candidate");
        check(getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).versionCode==116,"installed v116");
        File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        World seed=NativeR11Fixture.world("counter");try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");check(host!=null,"normal restored host");
        byte[] saved=authority();FilamentMapView[] retired={null};
        runOnMainSync(()->{
            fault();host.resume(false);host.switchMode(true);
            try{retired[0]=(FilamentMapView)field(host,"spatial");}catch(Exception e){throw new RuntimeException(e);}
            check(host.is3D(),"manual retry constructed native renderer");
            check(prefs().getBoolean("nativeFailure",false),"construction does not clear failure");
            host.switchMode(false);stale(retired[0]);
            check(prefs().getBoolean("nativeFailure",false),"abandoned retry and retired callback cannot clear failure");
            host.switchMode(true);stale(retired[0]);
            check(prefs().getBoolean("nativeFailure",false),"retired callback cannot certify replacement renderer");
            host.resume(true);
        });
        ready();long deadline=SystemClock.uptimeMillis()+120000;
        while(prefs().getBoolean("nativeFailure",false)&&SystemClock.uptimeMillis()<deadline)settle();
        check(!prefs().getBoolean("nativeFailure",true),"real current Surface content clears persistent failure");
        check(!(Boolean)field(host,"safeMode"),"verified output clears in-process protection");
        check(prefs().getBoolean("nativeSession",false),"live native session remains marked for process-death protection");
        check(Arrays.equals(saved,authority()),"recovery preserves entire authority/RNG");
        capture("r18-recovered-ui");surfaceCapture();
        Files.write(new File(dir,"r18-identity.txt").toPath(),("SOURCE="+BuildConfig.SOURCE_REVISION+"\n"+host.report()).getBytes("UTF-8"));
        Files.write(new File(dir,"r18-expected.sg11").toPath(),saved);Files.write(new File(dir,"r18-observed.sg11").toPath(),authority());
        runOnMainSync(()->{fault();stale(retired[0]);});
        check(!host.is3D()&&prefs().getBoolean("nativeFailure",false),"later failure reinstates protection");
        check(Arrays.equals(saved,authority()),"later failure leaves authority unchanged");capture("r18-fallback-ui");
        result.putString("stream","PASS R18 recovery checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL R18 "+android.util.Log.getStackTraceString(e));try{capture("r18-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

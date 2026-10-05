package game.sanguo.mobile;
import android.app.*;
import android.content.*;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Upgrade and controlled failure tests; not a claim of full touch gameplay or native-abort recovery. */
public final class NativeR17Instrumentation extends SceneInstrumentation {
    private String mode="verify";
    @Override public void onCreate(Bundle b){if(b!=null)mode=b.getString("mode",mode);super.onCreate(b);}
    private byte[] authority(){byte[][] result={null};runOnMainSync(()->{try{result[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return result[0];}
    @Override public void onStart(){Bundle result=new Bundle();try{
        File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        if(mode.equals("seed")){
            check(getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).versionCode==114,"installed input v114");
            runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",0));
        }
        long end=SystemClock.uptimeMillis()+120000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<end)settle();
        host=(MapHost)field(activity,"map");check(host!=null,"normal campaign restored/opened");
        File expected=new File(dir,"r17-upgrade.sg11");
        if(mode.equals("seed")){
            byte[] bytes=authority();Files.write(expected.toPath(),bytes);
            runOnMainSync(()->{try{((GameApplication)activity.getApplication()).host().store().write("manual",bytes);}catch(IOException e){throw new RuntimeException(e);}invoke("save",new Class<?>[]{String.class,boolean.class},"auto",false);});
            getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("gridShown",true).commit();
        }else{
            check(getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).versionCode==115,"installed candidate v115");
            byte[] saved=Files.readAllBytes(expected.toPath());
            check(Arrays.equals(saved,authority()),"upgrade and process restart retain complete authority/RNG");
            check(Arrays.equals(saved,Files.readAllBytes(new File(getTargetContext().getFilesDir(),"manual.sg11").toPath())),"manual slot bytes preserved");
            check(host.gridShown(),"visual preferences preserved");
            failureChecks(saved);
            capture("r17-2d-after-failure");
        }
        result.putString("stream","PASS R17 "+mode+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL R17 "+mode+" "+android.util.Log.getStackTraceString(e));}
        finish(Activity.RESULT_OK,result);
    }
    private void failureChecks(byte[] saved)throws Exception {
        try{VerifiedMaterial.read("3d/terrain.filamat",new ByteArrayInputStream(new byte[]{1,2,3}));throw new AssertionError("corruption accepted");}catch(IOException expected){check(expected.getMessage().contains("3d/terrain.filamat"),"bad resource diagnosis identifies path");}
        runOnMainSync(()->{
            try{
                java.lang.reflect.Method method=MapHost.class.getDeclaredMethod("fallback",Throwable.class);method.setAccessible(true);method.invoke(host,new IOException("3d/terrain.filamat checksum mismatch (injected)"));
            }catch(Exception e){throw new RuntimeException(e);}
        });
        check(!host.is3D(),"caught failure leaves usable 2D");
        check(getTargetContext().getSharedPreferences("map-renderer",0).getBoolean("nativeFailure",false),"failure survives normal native session release");
        runOnMainSync(()->{try{java.lang.reflect.Field latch=MapHost.class.getDeclaredField("interruptedSession");latch.setAccessible(true);latch.set(null,null);}catch(Exception e){throw new RuntimeException(e);}});
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);runOnMainSync(activity::recreate);
        Activity next=monitor.waitForActivityWithTimeout(30000);removeMonitor(monitor);check(next instanceof MainActivity,"activity recreated");activity=(MainActivity)next;settle();host=(MapHost)field(activity,"map");
        check((Boolean)field(host,"safeMode"),"persistent failure survives fresh process-latch simulation");
        check(!host.is3D(),"recreation remains 2D");check(Arrays.equals(saved,authority()),"failure/recreation preserve entire authority/RNG");
    }
}

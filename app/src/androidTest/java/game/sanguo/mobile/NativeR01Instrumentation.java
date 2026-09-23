package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.World;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Runs the normal game host, not a second renderer Activity. */
public final class NativeR01Instrumentation extends SceneInstrumentation {
    private byte[] authority(){final byte[][] b={null};runOnMainSync(()->{try{b[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return b[0];}
    private void shell(String command)throws Exception{try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(command))){while(in.read()!=-1){}}}
    private void log(String line)throws Exception{android.util.Log.i("NativeR01",line);try(FileWriter out=new FileWriter(new File(getTargetContext().getExternalFilesDir("s01"),"r01-cycles.txt"),true)){out.write(line+"\n");}}
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",0));
        long limit=SystemClock.uptimeMillis()+90000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<limit)settle();
        host=(MapHost)field(activity,"map");world=(World)field(activity,"world");check(host!=null,"normal campaign opened");byte[] saved=authority();
        runOnMainSync(()->{
            boolean failed=false;
            try{new FilamentMapView(new android.content.ContextWrapper(activity){
                @Override public android.content.res.AssetManager getAssets(){return null;}
            },h->{},e->{throw new AssertionError(e);});}catch(Exception e){failed=true;}
            check(failed,"missing asset provider fails after partial engine construction");
        });
        for(int i=0;i<20;i++){
            runOnMainSync(()->{host.switchMode(true);activity.selectAndFocus(world.home().hex);});ready();
            FilamentMapView spatial=(FilamentMapView)field(host,"spatial");
            check((Integer)field(host,"activeNativeHosts")==1,"one native host");
            if(i==0||i==19){surfaceCapture();capture("r01-cycle-"+i);}
            log("switch="+i+" "+host.report());
            runOnMainSync(()->host.switchMode(false));
            check((Boolean)field(spatial,"released"),"retired view released");check(!(Boolean)field(spatial,"queued"),"no retired frame callback");
            check(field(spatial,"engine")==null,"retired engine gone");check((Integer)field(host,"activeNativeHosts")==0,"no background native host");
            check(Arrays.equals(saved,authority()),"switch preserves authority and RNG");
        }
        runOnMainSync(()->{host.switchMode(true);activity.selectAndFocus(world.home().hex);});ready();
        for(int i=0;i<20;i++){
            shell("input keyevent KEYCODE_HOME");settle();
            FilamentMapView spatial=(FilamentMapView)field(host,"spatial");check(!(Boolean)field(spatial,"queued"),"background frame loop stopped");
            shell("am start -W -f 0x10020000 -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity");settle();ready();
            check((Integer)field(host,"activeNativeHosts")==1,"resume one native host");log("background="+i+" "+host.report());
        }
        // An intentionally uncooperative old job must not survive a real session replacement.
        FilamentMapView spatial=(FilamentMapView)field(host,"spatial");
        @SuppressWarnings("unchecked") SceneWorkQueue<Object> queue=(SceneWorkQueue<Object>)field(spatial,"meshWork");
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        runOnMainSync(()->queue.submit(()->{entered.countDown();while(true)try{release.await();break;}catch(InterruptedException ignored){}return new Object();}));
        check(entered.await(5,TimeUnit.SECONDS),"delayed worker entered");
        try{runOnMainSync(()->{invoke("activateWorld",new Class<?>[]{World.class},world);activity.refresh();});}finally{release.countDown();}
        ready();check(queue.discarded()>0,"old session worker discarded");check(Arrays.equals(saved,authority()),"replacement exact authority");
        surfaceCapture();capture("r01-after-replacement");
        // Worker errors are delivered once on the owner and return to playable Canvas.
        runOnMainSync(()->queue.submit(()->{throw new IOException("R01 injected decode failure");}));
        limit=SystemClock.uptimeMillis()+10000;while(host.is3D()&&SystemClock.uptimeMillis()<limit)settle();
        check(!host.is3D(),"decode failure returns to 2D");check(Arrays.equals(saved,authority()),"failure preserves authority");capture("r01-fallback");
        runOnMainSync(()->host.switchMode(true));ready();surfaceCapture();capture("r01-manual-retry");
        result.putString("stream","PASS R01 "+checks+" checks; emulator only\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{capture("r01-failure");}catch(Throwable ignored){}result.putString("stream","FAIL R01 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

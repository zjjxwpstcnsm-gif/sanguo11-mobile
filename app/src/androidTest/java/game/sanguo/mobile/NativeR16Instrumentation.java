package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Controlled profiling through MainActivity. Camera/commands use APIs, not a pure-touch claim. */
public final class NativeR16Instrumentation extends SceneInstrumentation {
    private String mode="local";private File dir;private long started,lastSample;private boolean listOnly;
    @Override public void onCreate(Bundle b){if(b!=null){mode=b.getString("mode",mode);listOnly="true".equals(b.getString("log"));}super.onCreate(b);}
    private Bundle status(){Bundle b=new Bundle();b.putString("id","InstrumentationTestRunner");b.putString("class",getClass().getName());b.putString("test",mode);b.putInt("numtests",1);b.putInt("current",1);return b;}
    private void log(String s)throws Exception{Files.write(new File(dir,"r16-"+mode+".txt").toPath(),(s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private FilamentMapView view()throws Exception{return (FilamentMapView)field(host,"spatial");}
    private void sample(String label)throws Exception{
        long now=SystemClock.elapsedRealtime();Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);
        String[] report={""},raw={"NOT_AVAILABLE on baseline"};
        runOnMainSync(()->{report[0]=host.report();try{java.lang.reflect.Method m=FilamentMapView.class.getDeclaredMethod("frameSamples");m.setAccessible(true);raw[0]=(String)m.invoke(view());}catch(NoSuchMethodException ignored){}catch(Exception e){throw new RuntimeException(e);}});
        log("SAMPLE "+label+" elapsed_ms="+(now-started)+" pss_kib="+memory.getTotalPss()+" java_used_bytes="+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())+" native_heap_allocated_bytes="+Debug.getNativeHeapAllocatedSize()+"\n"+report[0]);
        Files.write(new File(dir,"r16-"+mode+"-frames-"+(now-started)+".csv").toPath(),raw[0].getBytes("UTF-8"));lastSample=now;
    }
    @Override void observeLoading(FilamentMapView v)throws Exception{if(SystemClock.elapsedRealtime()-lastSample>=5000)sample("loading");}
    private void camera(Hex h,float span,float yaw)throws Exception{runOnMainSync(()->{try{FilamentMapView v=view();v.center(h);v.camera.span=span;v.camera.yaw=yaw;v.camera.tilt=48;}catch(Exception e){throw new RuntimeException(e);}});settle();ready();}
    private void shot(String label)throws Exception{surfaceCapture();capture("r16-"+mode+"-"+label);Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"r16-"+mode+"-"+label+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);sample(label);}
    private void profile()throws Exception{
        runOnMainSync(()->{world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.MEDIUM);host.setGridShown(false);host.setTerritoryMode(0);invoke("closePanel",new Class<?>[0]);
            if(mode.equals("national"))host.fit();else{host.center(world.cities.get(0).hex);try{view().camera.span=8;}catch(Exception e){throw new RuntimeException(e);}}});
        settle();ready();log("FIRST_READY_MS="+(SystemClock.elapsedRealtime()-started));shot("first");
        byte[] before=SaveCodec.encode(world);long idleEnd=SystemClock.elapsedRealtime()+15000;
        while(SystemClock.elapsedRealtime()<idleEnd){sample("idle");SystemClock.sleep(1000);}
        for(float span:new float[]{8,14,30})for(float yaw:new float[]{0,90}){camera(world.cities.get(0).hex,span,yaw);shot("span"+(int)span+"-yaw"+(int)yaw);}
        runOnMainSync(()->world=SessionProbe.view(activity));check(Arrays.equals(before,SaveCodec.encode(world)),"camera and profiling preserve complete save/RNG");
        if(!mode.equals("soak"))return;
        // The 30-minute clock starts only after initial readiness. Never count a loading hang as soak.
        long soakStart=SystemClock.elapsedRealtime();List<R15TourPlan.Stop> stops=R15TourPlan.regions(world);
        for(int cycle=0;cycle<20;cycle++){
            R15TourPlan.Stop stop=stops.get(cycle%stops.size());camera(stop.hex,cycle%2==0?8:30,(cycle%4)*90);shot("cycle"+cycle);
            if(cycle%5==0)commandFlow();
            runOnMainSync(()->world=SessionProbe.view(activity));byte[] exact=SaveCodec.encode(world);
            runOnMainSync(()->host.switchMode(false));settle();runOnMainSync(()->host.switchMode(true));settle();ready();
            getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);SystemClock.sleep(1000);
            runOnMainSync(()->{try{FilamentMapView v=view();check(!(Boolean)field(v,"queued"),"background frame stopped");}catch(Exception e){throw new RuntimeException(e);}});
            Intent resume=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);getTargetContext().startActivity(resume);settle();ready();
            World restored=SaveCodec.decode(exact);runOnMainSync(()->{invoke("activateWorld",new Class<?>[]{World.class},restored);activity.refresh();world=SessionProbe.view(activity);});settle();ready();
            check(Arrays.equals(exact,SaveCodec.encode(world)),"cycle/load preserves complete save/RNG");sample("cycle"+cycle+"-complete");
            long until=soakStart+(cycle+1)*90000L;
            while(SystemClock.elapsedRealtime()<until){sample("soak-idle");SystemClock.sleep(5000);}
        }
        check(SystemClock.elapsedRealtime()-soakStart>=1800000,"30 minutes after warmup");log("SOAK_COMPLETE 20 mode/home/load cycles; API commands and camera route, not full touch or all editor/effect cases");
    }
    @Override public void onStart(){Bundle result=new Bundle();sendStatus(1,status());if(listOnly){sendStatus(0,status());finish(Activity.RESULT_OK,result);return;}
        boolean pass=false;try{
            dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();started=SystemClock.elapsedRealtime();
            log("SOURCE="+BuildConfig.SOURCE_REVISION+" mode="+mode+" quality=MEDIUM cap=30 scale=.85; presentation/GPU timings require external collector; charging/ambient not controlled here");
            getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",5));
            long end=SystemClock.uptimeMillis()+120000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<end)settle();host=(MapHost)field(activity,"map");check(host!=null,"normal host exists");
            profile();pass=true;result.putString("stream","PASS R16 "+mode+" checks="+checks+"; physical/visual acceptance separate\n");
        }catch(Throwable e){String stack=android.util.Log.getStackTraceString(e);result.putString("stream","FAIL R16 "+mode+" "+stack);try{log(stack);if(host!=null)sample("FAIL");capture("r16-"+mode+"-failed");}catch(Exception ignored){}}
        Bundle done=status();done.putString("stream",result.getString("stream"));if(!pass)done.putString("stack",result.getString("stream"));sendStatus(pass?0:-2,done);finish(Activity.RESULT_OK,result);
    }
}

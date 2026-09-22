package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.MotionEvent;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Installed production activity, real national scenario, native Surface and lifecycle checks. */
public final class SceneInstrumentation extends Instrumentation {
    MainActivity activity;MapHost host;World world;int checks;
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    void settle(){waitForIdleSync();SystemClock.sleep(500);}
    void capture(String name)throws Exception{android.graphics.Bitmap b=getUiAutomation().takeScreenshot();if(b==null)throw new AssertionError("screenshot unavailable");File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        World w=ScenarioCatalog.all().get(0);try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(w));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");world=(World)field(activity,"world");byte[] initial=SaveCodec.encode(world);
        World.City city=world.cities.get(0);runOnMainSync(()->activity.selectAndFocus(city.hex));settle();capture("01-baseline-2d");
        for(int i=0;i<10;i++){
            runOnMainSync(()->host.switchMode(true));settle();check(host.is3D(),"Filament active, no fallback cycle "+i);
            FilamentMapView spatial=(FilamentMapView)field(host,"spatial");
            check(field(spatial,"swap")!=null,"native swapchain exists");check((Long)field(spatial,"lastFrame")>0,"native frame loop running");
            if(i==0){SystemClock.sleep(2000);runOnMainSync(()->host.focus(city.hex));settle();capture("02-3d-city");}
            runOnMainSync(()->host.switchMode(false));settle();check(!host.is3D(),"returned to 2D");check((Boolean)field(spatial,"released"),"old engine released");
        }
        check(Arrays.equals(initial,SaveCodec.encode(world)),"ten switches leave authoritative state identical");
        runOnMainSync(()->host.switchMode(true));settle();
        for(int i=0;i<10;i++){
            runOnMainSync(()->callActivityOnPause(activity));check(!(Boolean)field(field(host,"spatial"),"queued"),"pause removes frame callback");
            runOnMainSync(()->callActivityOnResume(activity));settle();check((Boolean)field(field(host,"spatial"),"queued"),"resume restarts single loop");
        }
        // Exact real Surface tap is routed through the same MainActivity onTile command entry.
        for(Hex cell:SiteFootprint.cells(city))if(world.inside(cell)){
            runOnMainSync(()->{
                try{FilamentMapView spatial=(FilamentMapView)field(host,"spatial");host.focus(cell);MapSceneSnapshot snap=(MapSceneSnapshot)field(spatial,"snapshot");float x=spatial.camera.screenX(snap.ground.grid.x(cell)),y=spatial.camera.screenY(snap.ground.grid.z(cell),0);long t=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(t,t,0,x,y,0),up=MotionEvent.obtain(t,t+50,1,x,y,0);spatial.onTouchEvent(down);spatial.onTouchEvent(up);down.recycle();up.recycle();}catch(Exception e){throw new RuntimeException(e);}
            });settle();check(world.cityAt((Hex)field(activity,"selected"))==city,"seven-cell tap selects same city");
        }
        runOnMainSync(()->{host.fit();host.toggleDiagnostics();});SystemClock.sleep(3000);capture("03-3d-national");
        try(OutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir("s01"),"report.txt"))){out.write(("PASS "+checks+" installed scene/lifecycle assertions\n"+host.report()+"\nLifecycle uses activity pause/resume callbacks; not physical device background/process death performance. Deployment/movement/next-turn UI end-to-end remains manual acceptance.\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        runOnMainSync(()->host.switchMode(false));result.putString("stream","PASS S01 "+checks+" installed checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL S01 "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

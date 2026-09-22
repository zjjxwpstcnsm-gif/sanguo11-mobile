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
    void invoke(String name,Class<?>[] types,Object... args){try{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception e){throw new RuntimeException(e);}}
    void commandFlow()throws Exception{
        World reference=SaveCodec.decode(SaveCodec.encode(world));int cityId=-1,officerId=-1;
        for(World.City site:world.cities)if(site.owner==world.player&&!world.idle(site).isEmpty()){
            int officer=world.idle(site).get(0).id;World probe=SaveCodec.decode(SaveCodec.encode(world));
            if(probe.deploy(site.id,officer,World.Weapon.SWORD,1000).ok){cityId=site.id;officerId=officer;break;}
        }
        check(cityId>=0,"real national scenario deployment available");final int c=cityId,o=officerId;
        runOnMainSync(()->{World.Result r=world.deploy(c,o,World.Weapon.SWORD,1000);check(r.ok,"3D command deployment");activity.apply(r);});
        check(reference.deploy(c,o,World.Weapon.SWORD,1000).ok,"reference deployment");
        World.Unit unit=world.units.get(world.units.size()-1);runOnMainSync(()->activity.selectUnitAndFocus(unit.id));settle();
        Hex destination=null;for(Hex target:world.orders.marchReachable(unit).keySet())if(!target.equals(unit.hex)&&world.orders.previewMove(unit.id,target).valid()){destination=target;break;}
        check(destination!=null,"legal move available");final Hex target=destination;
        runOnMainSync(()->{World.Result r=world.move(unit.id,target);check(r.ok,"3D command movement");activity.apply(r);});
        check(reference.move(unit.id,target).ok,"reference movement");
        check(Arrays.equals(SaveCodec.encode(world),SaveCodec.encode(reference)),"3D deployment/movement equal headless commands");
        runOnMainSync(()->invoke("advanceTurn",new Class<?>[0]));
        check(reference.nextTurn().ok,"reference next turn");long deadline=SystemClock.uptimeMillis()+120000;
        while((Boolean)field(activity,"aiRunning")&&SystemClock.uptimeMillis()<deadline)settle();
        check(!(Boolean)field(activity,"aiRunning"),"installed next turn finished");world=(World)field(activity,"world");check(host.is3D(),"next turn remains in 3D");
        check(Arrays.equals(SaveCodec.encode(world),SaveCodec.encode(reference)),"3D full playback and headless next-turn state equivalent");
        World loaded;try(InputStream in=getTargetContext().openFileInput("auto.sg11")){loaded=SaveCodec.read(in);}
        check(Arrays.equals(SaveCodec.encode(world),SaveCodec.encode(loaded)),"actual autosave reload equal");
        runOnMainSync(()->{invoke("activateWorld",new Class<?>[]{World.class},loaded);activity.refresh();});
        world=(World)field(activity,"world");check(host.is3D(),"load uses same 3D host");capture("04-3d-after-turn-load");
    }
    void settle(){waitForIdleSync();SystemClock.sleep(500);}
    void surfaceCapture()throws Exception{
        FilamentMapView spatial=(FilamentMapView)field(host,"spatial");
        android.view.SurfaceView surface=(android.view.SurfaceView)field(spatial,"surface");
        android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(surface.getWidth(),surface.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
        java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1);int[] status={-1};
        runOnMainSync(()->android.view.PixelCopy.request(surface,b,r->{status[0]=r;ready.countDown();},new Handler(Looper.getMainLooper())));
        check(ready.await(20,java.util.concurrent.TimeUnit.SECONDS)&&status[0]==android.view.PixelCopy.SUCCESS,"read actual rendered Surface");
        File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,"surface.png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
        int bright=0,total=0;for(int y=0;y<b.getHeight();y+=8)for(int x=0;x<b.getWidth();x+=8){int color=b.getPixel(x,y);total++;if(((color>>8)&255)>65)bright++;}
        android.util.Log.i("SceneAcceptance",host.report()+" surface bright="+bright+"/"+total);
        b.recycle();check(bright>total/50,"Surface contains actual terrain pixels, not a black/clear-only frame");
    }
    void capture(String name)throws Exception{android.graphics.Bitmap b=getUiAutomation().takeScreenshot();if(b==null)throw new AssertionError("screenshot unavailable");File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        android.util.Log.i("SceneAcceptance","maxJavaHeapBytes="+Runtime.getRuntime().maxMemory());
        World w=ScenarioCatalog.all().get(0);try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(w));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");world=(World)field(activity,"world");byte[] initial=SaveCodec.encode(world);
        World.City city=world.cities.get(0);runOnMainSync(()->activity.selectAndFocus(city.hex));settle();capture("01-baseline-2d");
        for(int i=0;i<10;i++){
            runOnMainSync(()->host.switchMode(true));settle();check(host.is3D(),"Filament active, no fallback cycle "+i);
            FilamentMapView spatial=(FilamentMapView)field(host,"spatial");
            check(field(spatial,"swap")!=null,"native swapchain exists");check((Long)field(spatial,"lastFrame")>0,"native frame loop running");
            if(i==0){SystemClock.sleep(2000);runOnMainSync(()->host.focus(city.hex));settle();SystemClock.sleep(5000);capture("02-3d-city");surfaceCapture();}
            runOnMainSync(()->host.switchMode(false));settle();check(!host.is3D(),"returned to 2D");check((Boolean)field(spatial,"released"),"old engine released");
        }
        check(Arrays.equals(initial,SaveCodec.encode(world)),"ten switches leave authoritative state identical");
        runOnMainSync(()->host.switchMode(true));settle();
        for(int i=0;i<10;i++){
            try(InputStream shell=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME"))){while(shell.read()!=-1){}}settle();check(!(Boolean)field(field(host,"spatial"),"queued"),"background removes frame callback");
            try(InputStream shell=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("am start -W -f 0x10020000 -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity"))){while(shell.read()!=-1){}}settle();long readyBy=SystemClock.uptimeMillis()+10000;while((Long)field(field(host,"spatial"),"lastFrame")==0&&SystemClock.uptimeMillis()<readyBy)settle();check((Long)field(field(host,"spatial"),"lastFrame")>0,"foreground produces new rendered frame callback");
        }
        // Exact real Surface tap is routed through the same MainActivity onTile command entry.
        for(Hex cell:SiteFootprint.cells(city))if(world.inside(cell)){
            runOnMainSync(()->{
                try{FilamentMapView spatial=(FilamentMapView)field(host,"spatial");host.focus(cell);MapSceneSnapshot snap=(MapSceneSnapshot)field(spatial,"snapshot");float x=spatial.camera.screenX(snap.ground.grid.x(cell)),y=spatial.camera.screenY(snap.ground.grid.z(cell),0);long t=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(t,t,0,x,y,0),up=MotionEvent.obtain(t,t+50,1,x,y,0);spatial.onTouchEvent(down);spatial.onTouchEvent(up);down.recycle();up.recycle();}catch(Exception e){throw new RuntimeException(e);}
            });settle();check(world.cityAt((Hex)field(activity,"selected"))==city,"seven-cell tap selects same city");
        }
        runOnMainSync(()->{host.fit();host.toggleDiagnostics();});settle();long terrainDeadline=SystemClock.uptimeMillis()+30000;while((Integer)field(field(host,"spatial"),"pending")>0&&SystemClock.uptimeMillis()<terrainDeadline)settle();check((Integer)field(field(host,"spatial"),"pending")==0,"national terrain chunks uploaded");SystemClock.sleep(1500);capture("03-3d-national");
        commandFlow();
        try(OutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir("s01"),"report.txt"))){out.write(("PASS "+checks+" installed scene/lifecycle assertions\n"+host.report()+"\nLifecycle uses HOME and task foreground ten times on emulator; not physical-device or process-death performance. Deployment/movement use actual command APIs and MainActivity.apply; next turn runs actual TurnWork/TurnPlayback; deployment wizard touches remain manual acceptance.\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        runOnMainSync(()->host.switchMode(false));result.putString("stream","PASS S01 "+checks+" installed checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL S01 "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

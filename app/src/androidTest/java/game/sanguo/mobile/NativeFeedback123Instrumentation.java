package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.MotionEvent;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Normal MainActivity, fixed small input + real national gate. Camera operations
 * are API driven. Original 120s readiness is preserved; no phone-FPS claim. */
public final class NativeFeedback123Instrumentation extends SceneInstrumentation {
    private String phase="candidate",mode="fixture";private File dir;private FilamentMapView view;private int launcherDialogs;private boolean focused;
    @Override public void onCreate(Bundle b){if(b!=null){phase=b.getString("phase",phase);mode=b.getString("mode",mode);focused="true".equals(b.getString("focused","false"));}super.onCreate(b);}
    private void log(String s)throws Exception{Files.write(new File(dir,"feedback123.txt").toPath(),(s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private byte[] authority(){byte[][] out={null};runOnMainSync(()->{try{out[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return out[0];}
    @Override void observeLoading(FilamentMapView renderer)throws Exception {
        // Initial API35 captures show a Quickstep ANR stealing focus before any
        // game frame. Preserve that evidence and close only this exact external
        // dialog through real input; never bypass the game's window/render gate.
        android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
        if(root==null)return;
        try {
            List<android.view.accessibility.AccessibilityNodeInfo> titles=root.findAccessibilityNodeInfosByText("Quickstep isn't responding");
            boolean launcher=titles.stream().anyMatch(n->"android".contentEquals(n.getPackageName()==null?"":n.getPackageName())&&"Quickstep isn't responding".contentEquals(n.getText()==null?"":n.getText()));
            if(!launcher)return;
            check(launcherDialogs<2,"bounded Quickstep environment recovery");
            capture(phase+"-quickstep-anr-"+launcherDialogs);
            for(android.view.accessibility.AccessibilityNodeInfo button:root.findAccessibilityNodeInfosByText("Close app")) {
                if(!"Close app".contentEquals(button.getText()==null?"":button.getText())||!button.isVisibleToUser())continue;
                android.graphics.Rect bounds=new android.graphics.Rect();button.getBoundsInScreen(bounds);
                float[] point={bounds.exactCenterX(),bounds.exactCenterY()};long time=SystemClock.uptimeMillis();
                log("ENVIRONMENT: observed Quickstep ANR; close launcher via actual pointer, original readiness deadline unchanged");
                event(time,MotionEvent.ACTION_DOWN,point);event(time,MotionEvent.ACTION_UP,point);launcherDialogs++;return;
            }
            throw new AssertionError("Quickstep ANR visible but Close app control unavailable");
        }finally{root.recycle();}
    }
    private void event(long down,int action,float[] p){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,p[0],p[1],0);e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(e);e.recycle();}
    private long frames()throws Exception{long[] n={0};runOnMainSync(()->{try{n[0]=(Long)field(view,"surfaceFrames");}catch(Exception e){throw new RuntimeException(e);}});return n[0];}
    private void advance(long from)throws Exception{
        long deadline=SystemClock.uptimeMillis()+120000;while(frames()<from+3&&SystemClock.uptimeMillis()<deadline)settle();
        check(frames()>=from+3,"three fresh submissions after view change");ready();
    }
    private void shot(String name)throws Exception{
        long prior=frames();advance(prior);surfaceCapture();
        Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);
        capture(name+"-ui");log(name+"\n"+host.report());
    }
    private long lastAttempt(String csv){String[] lines=csv.trim().split("\n");return lines.length<2?0:Long.parseLong(lines[lines.length-1].split(",")[0]);}
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        boolean national=!mode.equals("fixture");
        World seed=national?ScenarioCatalog.all().get(0):NativeR11Fixture.world("critical-fire");
        Hex focus;
        if(national){
            World.City site=mode.equals("port")?seed.cities.stream().filter(c->c.kind==World.SiteKind.PORT).findFirst().get():seed.city(20010);
            check(site!=null,"real national site exists");focus=site.hex;
        }else{
            for(int q=9;q<=15;q++)for(int r=4;r<=6;r++)seed.terrain[q][r]=q==12?World.Terrain.MOUNTAIN_PATH:World.Terrain.MOUNTAIN;
            World.City gate=new World.City(2,"v123 gate fixture",new Hex(12,5),0);gate.kind=World.SiteKind.GATE;seed.cities.add(gate);
            for(int q=10;q<=13;q++)for(int r=10;r<=12;r++)seed.terrain[q][r]=World.Terrain.SWAMP;
            seed.terrain[10][7]=World.Terrain.PLANK_ROAD;seed.terrain[11][7]=World.Terrain.MOUNTAIN_PATH;seed.terrain[12][7]=World.Terrain.ROAD;
            focus=new Hex(11,8);seed.scenarioName="v123 explicit terrain and army fixture";
        }
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).putString("quality","MEDIUM").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");check(host!=null,"normal save restore");byte[] before=authority();
        runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.setGridShown(false);host.setTerritoryMode(0);activity.selectAndFocus(focus);host.switchMode(true);});settle();
        view=(FilamentMapView)field(host,"spatial");runOnMainSync(()->{view.center(focus);view.camera.span=6;view.camera.tilt=55;view.camera.yaw=0;});ready();
        log("phase="+phase+" mode="+mode+" original120sReady; MEDIUM; fixed camera API; not full touch flow or phone performance; focused="+focused+"");
        for(float span:new float[]{5,10}){
            runOnMainSync(()->{view.center(focus);view.camera.span=span;view.camera.yaw=0;});settle();ready();
            for(boolean grid:new boolean[]{false,true}){
                runOnMainSync(()->host.setGridShown(grid));settle();ready();
                shot(phase+"-"+mode+"-span"+(int)span+"-grid"+grid);
            }
        }
        // Fixed static view: record all attempts, not just successful frames.
        long started=SystemClock.uptimeMillis();while(SystemClock.uptimeMillis()-started<10000)settle();
        String[] samples={null};runOnMainSync(()->samples[0]=view.frameSamples());
        Files.write(new File(dir,"frame-samples.csv").toPath(),samples[0].getBytes("UTF-8"));
        if(phase.equals("candidate")){
            long uploads=0,skips=0;
            for(Object proxy:((Map<?,?>)field(view,"objects")).values()){
                uploads+=(Long)field(proxy,"positionUploads");skips+=(Long)field(proxy,"positionSkips");
            }
            log("POSITION_CACHE uploads="+uploads+" skips="+skips);
            check(skips>0,"normal visible-unit path actually reuses contact/transform");
        }
        check(Arrays.equals(before,authority()),"all views/grid/animations preserve entire authority and RNG");
        Files.write(new File(dir,"expected.sg11").toPath(),before);Files.write(new File(dir,"observed.sg11").toPath(),authority());
        check(((Set<?>)field(view,"missingAssets")).isEmpty(),"no asset fallback");
        if(phase.equals("candidate")){
            Map<?,?> shapes=(Map<?,?>)field(view,"shapes");check(!shapes.isEmpty(),"production asset queue reached GPU meshes");
            int actual=0;
            for(Map.Entry<?,?> entry:shapes.entrySet()){
                String key=entry.getKey().toString();
                if(key.startsWith("city")||key.startsWith("gate:")||key.startsWith("port:")){
                    SceneMesh loaded=(SceneMesh)field(entry.getValue(),"source");
                    log("ACTUAL_GPU_SHAPE "+key+" triangles="+loaded.indices.length/3);actual++;
                }
            }
            check(actual>0,"actual city/port/gate meshes resident after normal loading");
        }
        log("PASS FEEDBACK123 checks="+checks);result.putString("stream","PASS FEEDBACK123 "+phase+" "+mode+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL FEEDBACK123 "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture(phase+"-"+mode+"-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

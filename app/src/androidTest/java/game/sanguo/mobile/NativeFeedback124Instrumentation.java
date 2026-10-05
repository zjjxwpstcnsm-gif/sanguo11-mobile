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
public final class NativeFeedback124Instrumentation extends SceneInstrumentation {
    private String phase="candidate",mode="fixture";private File dir;private FilamentMapView view;private int launcherDialogs;private boolean focused;
    @Override public void onCreate(Bundle b){if(b!=null){phase=b.getString("phase",phase);mode=b.getString("mode",mode);focused="true".equals(b.getString("focused","false"));}super.onCreate(b);}
    private void log(String s)throws Exception{Files.write(new File(dir,"feedback124.txt").toPath(),(s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
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
    private void touchView()throws Exception{
        float[] state={0,0,0};int[] location=new int[2];float[] dimensions={0,0};
        runOnMainSync(()->{view.getLocationOnScreen(location);dimensions[0]=view.getWidth();dimensions[1]=view.getHeight();state[0]=view.camera.x;state[1]=view.camera.z;state[2]=view.camera.span;});
        float x=location[0]+dimensions[0]*.50f,y=location[1]+dimensions[1]*.50f;
        long down=SystemClock.uptimeMillis();event(down,MotionEvent.ACTION_DOWN,new float[]{x,y});
        for(int i=1;i<=6;i++){SystemClock.sleep(30);event(down,MotionEvent.ACTION_MOVE,new float[]{x+dimensions[0]*.015f*i,y});}
        event(down,MotionEvent.ACTION_UP,new float[]{x+dimensions[0]*.09f,y});settle();
        float[] after={0,0};runOnMainSync(()->{after[0]=view.camera.x;after[1]=view.camera.z;});
        check(Math.abs(after[0]-state[0])+Math.abs(after[1]-state[1])>.01,"real pointer scroll moves normal camera");
        int minimum=Build.VERSION.SDK_INT>=29?android.view.ViewConfiguration.get(getTargetContext()).getScaledMinimumScalingSpan():0;
        float radius=Math.max(40,minimum*.55f),end=Math.min(dimensions[0]*.35f,radius*1.65f);
        check(end>radius+16,"unobstructed viewport supports a platform-sized pinch");
        log("GESTURE minimumScalingSpan="+minimum+" actualSpan="+(radius*2)+"->"+(end*2)+"; original camera assertion retained");
        down=SystemClock.uptimeMillis();event(down,MotionEvent.ACTION_DOWN,new float[]{x-radius,y});
        pinch(down,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),x,y,radius);
        for(int i=1;i<=6;i++){SystemClock.sleep(30);pinch(down,MotionEvent.ACTION_MOVE,x,y,radius+(end-radius)*i/6);}
        pinch(down,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),x,y,end);
        event(down,MotionEvent.ACTION_UP,new float[]{x-end,y});settle();
        float[] span={0};runOnMainSync(()->span[0]=view.camera.span);check(Math.abs(span[0]-state[2])>.01,"real two-finger pinch changes normal zoom");
        ready();shot(phase+"-fixture-real-scroll-pinch");log("Actual pointer scroll and pinch; remaining fixed views API driven; not complete new-game touch chain");
    }
    private void pinch(long down,int action,float x,float y,float radius){
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[2];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[2];
        for(int i=0;i<2;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=i;properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=x+(i==0?-radius:radius);coords[i].y=y;coords[i].pressure=1;coords[i].size=1;}
        MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,2,properties,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);sendPointerSync(event);event.recycle();
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
            World.City gate=new World.City(2,"v124 gate fixture",new Hex(12,5),0);gate.kind=World.SiteKind.GATE;seed.cities.add(gate);
            for(int q=10;q<=13;q++)for(int r=10;r<=12;r++)seed.terrain[q][r]=World.Terrain.SWAMP;
            seed.terrain[10][7]=World.Terrain.PLANK_ROAD;seed.terrain[11][7]=World.Terrain.MOUNTAIN_PATH;seed.terrain[12][7]=World.Terrain.ROAD;
            focus=new Hex(11,8);seed.scenarioName="v124 explicit terrain and army fixture";
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
        if(mode.equals("fixture"))touchView();
        // Fixed static view: record all attempts, not just successful frames.
        long first=frames();String[] initial={null};runOnMainSync(()->initial[0]=view.frameSamples());
        Files.write(new File(dir,"frame-samples-before.csv").toPath(),initial[0].getBytes("UTF-8"));
        long started=SystemClock.uptimeMillis();while(SystemClock.uptimeMillis()-started<10000)settle();
        String[] samples={null};runOnMainSync(()->samples[0]=view.frameSamples());
        Files.write(new File(dir,"frame-samples.csv").toPath(),samples[0].getBytes("UTF-8"));
        log("STATIC_WINDOW uptimeMs="+(SystemClock.uptimeMillis()-started)+" submitted="+(frames()-first)+" attempts="+(lastAttempt(samples[0])-lastAttempt(initial[0]))+"; submitted counts are not presented FPS");
        if(phase.equals("candidate")){
            long uploads=0,skips=0;int visibleUnits=0;
            for(Object proxy:((Map<?,?>)field(view,"objects")).values()){
                uploads+=(Long)field(proxy,"positionUploads");skips+=(Long)field(proxy,"positionSkips");
                MapSceneSnapshot.Item item=(MapSceneSnapshot.Item)field(proxy,"item");
                if(item.unit!=null&&(Boolean)field(proxy,"shown"))visibleUnits++;
            }
            log("POSITION_CACHE uploads="+uploads+" skips="+skips+" visibleUnits="+visibleUnits);
            check(!mode.equals("fixture")||visibleUnits>0,"army fixture has visible troops");
            if(visibleUnits>0)check(skips>0,"normal visible-unit path actually reuses contact/transform");
            else log("POSITION_CACHE NOT_APPLICABLE: this official city/port view has no visible unit; all scene, asset and authority assertions remain active");
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
        log("PASS FEEDBACK124 checks="+checks);result.putString("stream","PASS FEEDBACK124 "+phase+" "+mode+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL FEEDBACK124 "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture(phase+"-"+mode+"-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

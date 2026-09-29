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
public final class NativeFeedback121Instrumentation extends SceneInstrumentation {
    private String phase="candidate",mode="fixture";private File dir;private FilamentMapView view;private int launcherDialogs;private boolean focused;
    @Override public void onCreate(Bundle b){if(b!=null){phase=b.getString("phase",phase);mode=b.getString("mode",mode);focused="true".equals(b.getString("focused","false"));}super.onCreate(b);}
    private void log(String s)throws Exception{Files.write(new File(dir,"feedback121.txt").toPath(),(s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
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
        World seed=mode.equals("national")?ScenarioCatalog.all().get(0):NativeR11Fixture.world("critical-fire");
        Hex focus;
        if(mode.equals("national"))focus=seed.cities.stream().filter(c->c.name.contains("虎牢")).findFirst().orElseGet(()->seed.cities.stream().filter(c->c.kind==World.SiteKind.GATE).findFirst().get()).hex;
        else{
            for(int q=5;q<=17;q++)for(int r=2;r<=6;r++)seed.terrain[q][r]=q==12?World.Terrain.MOUNTAIN_PATH:World.Terrain.MOUNTAIN;
            World.City gate=new World.City(2,"山口关（测试场景）",new Hex(12,5),0);gate.kind=World.SiteKind.GATE;seed.cities.add(gate);
            for(int q=12;q<=17;q++)for(int r=10;r<=12;r++)seed.terrain[q][r]=World.Terrain.FOREST;
            check(NativeR11Fixture.command(seed,"critical-fire").ok,"actual fire command fixture");check(!seed.war.fires().isEmpty(),"actual persistent fire exists");
            focus=new Hex(11,7);seed.scenarioName="v121 explicit mountain/gate/fire fixture";
        }
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).putString("quality","MEDIUM").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");check(host!=null,"normal save restore");byte[] before=authority();
        runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.setGridShown(false);host.setTerritoryMode(0);activity.selectAndFocus(focus);host.switchMode(true);});settle();
        view=(FilamentMapView)field(host,"spatial");runOnMainSync(()->{view.center(focus);view.camera.span=6;view.camera.tilt=55;view.camera.yaw=0;});ready();
        log("phase="+phase+" mode="+mode+" original120sReady; MEDIUM; fixed camera API; not full touch flow or phone performance; focused="+focused+"");
        for(float span:(focused?new float[]{6}:new float[]{4,8,16}))for(float yaw:(focused?new float[]{0}:new float[]{0,90})){
            runOnMainSync(()->{view.center(focus);view.camera.span=span;view.camera.yaw=yaw;});settle();ready();
            for(boolean grid:new boolean[]{false,true}){
                runOnMainSync(()->host.setGridShown(grid));settle();ready();
                shot(phase+"-"+mode+"-span"+(int)span+"-yaw"+(int)yaw+"-grid"+grid);
            }
        }
        for(boolean grid:new boolean[]{false,true}){
            runOnMainSync(()->{host.setGridShown(grid);view.camera.span=8;view.camera.yaw=0;});ready();
            String label=phase+"-"+mode+"-grid"+grid;
            String[] startSamples={null};runOnMainSync(()->{view.resetMetrics();startSamples[0]=view.frameSamples();});
            long startAttempt=lastAttempt(startSamples[0]);
            for(int i=0;i<20;i++){final int at=i;runOnMainSync(()->view.camera.yaw=at*3);settle();}
            log(label+" orbit: "+host.report());String[] samples={null};runOnMainSync(()->samples[0]=view.frameSamples());
            StringBuilder measured=new StringBuilder(samples[0].split("\n")[0]).append("\n");
            for(String row:samples[0].split("\n"))if(Character.isDigit(row.charAt(0))&&Long.parseLong(row.substring(0,row.indexOf(',')))>startAttempt)measured.append(row).append("\n");
            Files.write(new File(dir,label+"-owner-frames.csv").toPath(),measured.toString().getBytes("UTF-8"));
        }
        check(Arrays.equals(before,authority()),"all views/grid/animations preserve entire authority and RNG");
        Files.write(new File(dir,"expected.sg11").toPath(),before);Files.write(new File(dir,"observed.sg11").toPath(),authority());
        check(((Set<?>)field(view,"missingAssets")).isEmpty(),"no asset fallback");
        if(phase.equals("candidate")){
            check((Long)field(view,"gridUploads")>0,"actual GPU grid batch uploads");
            if(mode.equals("fixture")){Object[] effects=(Object[])field(view,"effectMeshes");check(effects[2]!=null,"Blender fire reaches normal effect renderer");SceneMesh mesh=(SceneMesh)field(effects[2],"source");check(mesh.indices.length/3>1000,"loaded curved flame geometry");}
        }
        log("PASS FEEDBACK121 checks="+checks);result.putString("stream","PASS FEEDBACK121 "+phase+" "+mode+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL FEEDBACK121 "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture(phase+"-"+mode+"-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

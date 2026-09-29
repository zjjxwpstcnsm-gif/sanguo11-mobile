package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicit saved fixture, normal MainActivity and actual injected pointer gestures.
 * Does not claim a full nationwide opening or physical-device acceptance. */
public final class NativeFeedback120Instrumentation extends SceneInstrumentation {
    private String phase="candidate";private File dir;private FilamentMapView view;
    @Override public void onCreate(Bundle b){if(b!=null)phase=b.getString("phase",phase);super.onCreate(b);}
    private byte[] authority(){byte[][] out={null};runOnMainSync(()->{try{out[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return out[0];}
    private void log(String s)throws Exception{Files.write(new File(dir,"feedback120.txt").toPath(),(s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private float[] point(Hex h)throws Exception{
        float[][] out={null};runOnMainSync(()->{try{MapSceneSnapshot snap=(MapSceneSnapshot)field(view,"snapshot");int[] at=new int[2];view.getLocationOnScreen(at);float x=snap.ground.grid.x(h),z=snap.ground.grid.z(h);out[0]=new float[]{at[0]+view.camera.screenX(x,z),at[1]+view.camera.screenY(x,z,snap.ground.surface.at(h))};}catch(Exception e){throw new RuntimeException(e);}});return out[0];
    }
    private void event(long down,int action,float[] p){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,p[0],p[1],0);e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(e);e.recycle();}
    private long hold()throws Exception{
        long t=SystemClock.uptimeMillis();event(t,MotionEvent.ACTION_DOWN,point(new Hex(8,8)));SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+250);runOnMainSync(()->{});
        check((Boolean)field(view,"draggingUnit"),"long press selected unit begins real drag");return t;
    }
    private void multi(long t,float[] p){
        MotionEvent.PointerProperties[] props={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};
        MotionEvent.PointerCoords[] coords={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};
        for(int i=0;i<2;i++){props[i].id=i;props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i].x=p[0]+i*40;coords[i].y=p[1];coords[i].pressure=1;coords[i].size=1;}
        MotionEvent e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,props,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);sendPointerSync(e);e.recycle();event(t,MotionEvent.ACTION_CANCEL,p);
    }
    private void shot(String name)throws Exception{ready();surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(name+"-ui");log(name+"\n"+host.report());}
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        World seed=CombatSceneFixture.world("counter");seed.scenarioName="v120 explicit forest occupancy and drag fixture";
        for(int q=5;q<=12;q++)for(int r=4;r<=11;r++)seed.terrain[q][r]=World.Terrain.FOREST;
        seed.terrain[13][6]=World.Terrain.ROAD;
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).putString("quality","MEDIUM").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
        runOnMainSync(()->{activity.selectUnitAndFocus(1);invoke("closePanel",new Class<?>[0]);host.setTerritoryMode(0);host.setGridShown(true);host.switchMode(true);});settle();
        view=(FilamentMapView)field(host,"spatial");check(view!=null,"normal native renderer active");
        runOnMainSync(()->{view.center(new Hex(8,8));view.camera.span=5;view.camera.tilt=55;view.camera.yaw=0;});settle();ready();
        shot(phase+"-selected-forest-before");
        MapSceneSnapshot initial=(MapSceneSnapshot)field(view,"snapshot");
        Set<Hex> oldMask=Vegetation.exclusions(initial);
        List<?> oldWoods=new ArrayList<>((List<?>)field(view,"woods"));
        long startGeneration=((Number)field(view,"generation")).longValue();
        if(phase.equals("candidate")){
            Object overlay=field(view,"overlay");check(((Number)field(overlay,"selectionDraws")).longValue()>0,"multilayer selection actually drawn");
            check(((Number)field(overlay,"forestSilhouetteDraws")).longValue()>0,"selected formation rendered through retained forest");
            check(Vegetation.placements(initial.ground,oldMask,new Hex(8,8)).size()>0,"selected occupied cell still has vegetation");
        }
        byte[] before=authority();log("phase="+phase+" normal MainActivity fixture; real injected pointer events; original 120s ready");
        if(phase.equals("candidate")){
            for(String cancel:new String[]{"cancel","enemy","two-finger","snapshot","disabled","panel"}){
                long t=hold();float[] dest=point(new Hex(7,8));event(t,MotionEvent.ACTION_MOVE,dest);
                if(cancel.equals("two-finger"))multi(t,dest);
                else if(cancel.equals("cancel"))event(t,MotionEvent.ACTION_CANCEL,dest);
                else {
                    if(cancel.equals("enemy")){dest=point(new Hex(9,8));event(t,MotionEvent.ACTION_MOVE,dest);settle();check(field(view,"dragPlan")==null,"occupied enemy drop invalid");capture(phase+"-invalid-ghost");}
                    if(cancel.equals("snapshot"))runOnMainSync(()->{host.invalidateScene();activity.refresh();});
                    if(cancel.equals("disabled"))runOnMainSync(()->host.setEnabled(false));
                    if(cancel.equals("panel"))runOnMainSync(()->host.setPanelOcclusion(view.getWidth(),0));
                    event(t,MotionEvent.ACTION_UP,dest);
                    runOnMainSync(()->{host.setEnabled(true);host.setPanelOcclusion(0,0);});
                }
                settle();check(Arrays.equals(before,authority()),"cancel path preserves all authority/RNG: "+cancel);check(!(Boolean)field(view,"draggingUnit"),"drag ended: "+cancel);
                check(((android.graphics.Path)field(field(field(view,"overlay"),"ghost"),"path")).isEmpty(),"ghost cleared on "+cancel);
            }
        }
        float x=view.camera.x,z=view.camera.z;long t=hold();float[] destination=point(new Hex(7,8));event(t,MotionEvent.ACTION_MOVE,destination);settle();capture(phase+"-drag-preview");
        check(Arrays.equals(before,authority()),"preview before drop is read only");
        if(phase.equals("candidate")){
            Object overlay=field(view,"overlay"),ghost=field(overlay,"ghost");
            check(((Number)field(overlay,"ghostDraws")).longValue()>0,"actual mesh ghost drawn at target");
            check(!((android.graphics.Path)field(ghost,"path")).isEmpty(),"projected ghost has actual model silhouette");
            long builds=((Number)field(ghost,"builds")).longValue();settle();
            check(((Number)field(ghost,"builds")).longValue()==builds,"stationary preview reuses cached silhouette");
            check(new Hex(7,8).equals(field(view,"dragTarget")),"ghost target equals exact picked drop cell");
        }
        event(t,MotionEvent.ACTION_UP,destination);settle();
        {
            World expected=SaveCodec.decode(before);MarchOrders.Plan plan=expected.marches.previewMove(1,new Hex(7,8));check(expected.marches.execute(plan).ok,"reference same ordinary move accepted");
            byte[] actual=authority();Files.write(new File(dir,"move-expected.sg11").toPath(),SaveCodec.encode(expected));Files.write(new File(dir,"move-actual.sg11").toPath(),actual);
            check(Arrays.equals(SaveCodec.encode(expected),actual),"one dropped command equals full headless save/RNG");
            check(view.camera.x==x&&view.camera.z==z,"drag does not pan camera");
            event(t,MotionEvent.ACTION_UP,destination);settle();check(Arrays.equals(actual,authority()),"duplicate UP cannot execute again");
        }
        shot(phase+"-after-drop");
        MapSceneSnapshot occupied=(MapSceneSnapshot)field(view,"snapshot");
        Set<Hex> newMask=Vegetation.exclusions(occupied);
        if(phase.equals("candidate")){
            check(oldMask.equals(newMask),"forest exclusion unchanged after real drop");
            check(oldWoods.equals((List<?>)field(view,"woods")),"same native forest meshes survive real movement");
            check(((Number)field(view,"generation")).longValue()==startGeneration,"move does not regenerate forest or ground");
            check(Vegetation.placements(occupied.ground,newMask,new Hex(7,8)).size()>0,"destination forest not erased");
        }else check(!oldMask.equals(newMask),"baseline reproduces forest cleared by unit move");
        byte[] forestBefore=authority();
        for(float span:new float[]{4,8})for(float yaw:new float[]{0,90}){
            runOnMainSync(()->{view.center(new Hex(7,8));view.camera.span=span;view.camera.yaw=yaw;host.setGridShown(false);});settle();shot(phase+"-forest-span"+(int)span+"-yaw"+(int)yaw);
        }
        check(Arrays.equals(forestBefore,authority()),"forest/camera leave authority unchanged");
        log("PASS FEEDBACK120 checks="+checks);result.putString("stream","PASS FEEDBACK120 phase="+phase+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL FEEDBACK120 "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture("feedback120-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

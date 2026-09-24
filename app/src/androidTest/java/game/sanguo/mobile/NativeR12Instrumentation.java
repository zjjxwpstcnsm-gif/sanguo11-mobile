package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed checks use actual host UI callbacks and Surface. Synthetic combat is labelled separately. */
public final class NativeR12Instrumentation extends SceneInstrumentation {
    private File dir;
    private FilamentMapView spatial()throws Exception{return (FilamentMapView)field(host,"spatial");}
    private byte[] authority()throws Exception{byte[][] bytes={null};runOnMainSync(()->{try{bytes[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception e){throw new RuntimeException(e);}});return bytes[0];}
    private void note(String text)throws Exception{Files.write(new File(dir,"r12-runtime.txt").toPath(),(text+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private void shot(String name)throws Exception{ready();surfaceCapture();capture(name+"-ui");Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);note(name+" source="+BuildConfig.SOURCE_REVISION+" grid="+host.gridShown()+" territory="+host.territoryMode()+"\n"+host.report());}
    private void recreate()throws Exception{
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);runOnMainSync(activity::recreate);
        Activity next=monitor.waitForActivityWithTimeout(30000);removeMonitor(monitor);check(next instanceof MainActivity,"actual Activity recreated");activity=(MainActivity)next;settle();host=(MapHost)field(activity,"map");runOnMainSync(()->world=SessionProbe.view(activity));ready();
    }
    private void click(View v,float x,float y){long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(now,now+60,MotionEvent.ACTION_UP,x,y,0);v.dispatchTouchEvent(down);v.dispatchTouchEvent(up);down.recycle();up.recycle();}
    private void combinations()throws Exception{
        byte[] before=authority();
        for(boolean grid:new boolean[]{false,true})for(int mode:new int[]{0,1}){
            runOnMainSync(()->{host.setGridShown(grid);host.setTerritoryMode(mode);invoke("closePanel",new Class<?>[0]);host.focus(world.home().hex);});ready();
            check((Boolean)field(spatial(),"gridShown")==grid,"3D grid independent");check((Integer)field(spatial(),"territoryMode")==mode,"3D territory independent");
            shot("r12-grid-"+grid+"-territory-"+mode);
            runOnMainSync(()->{host.switchMode(false);check(host.gridShown()==grid&&host.territoryMode()==mode,"2D parity");host.switchMode(true);});ready();
            recreate();check(host.gridShown()==grid&&host.territoryMode()==mode,"activity preference parity");
            check(Arrays.equals(before,authority()),"four combinations preserve full authority/RNG");
        }
        runOnMainSync(()->host.setTerritoryMode(2));shot("r12-site-boundaries");
        int[] actual=(int[])field(spatial(),"territoryBorders");Territory t=new Territory(world);
        for(int r=0;r<world.height;r++)for(int q=0;q<world.width;q++)check(actual[r*world.width+q]==t.boundary(q,r,true),"installed site boundaries");
        // Editor's temporary layer must not change the user's persisted grid setting.
        runOnMainSync(()->{host.setGridShown(false);host.editorLayers(true,false,false,false);});shot("r12-editor-grid-override");
        check(!host.gridShown(),"editor override does not persist");runOnMainSync(()->host.editorLayers(false,false,false,false));
        note("PASS four-combination / 2D / Activity recreation / temporary editor layer; full-save equality");
    }
    private void navigatorAndPanels()throws Exception{
        byte[] before=authority();FilamentMapView v=spatial();
        runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);if(!((MapView)unchecked(host,"flat")).navigatorShown())host.toggleNavigator();v.camera.yaw=67;v.camera.span=9;});settle();
        Object overlay=field(v,"overlay");android.graphics.RectF rect=new android.graphics.RectF((android.graphics.RectF)field(overlay,"miniRect"));check(!rect.isEmpty(),"native minimap has visible viewport");
        MapSceneSnapshot snapshot=(MapSceneSnapshot)field(v,"snapshot");NavigatorTransform transform=new NavigatorTransform(snapshot.ground.minX,snapshot.ground.minZ,snapshot.ground.maxX,snapshot.ground.maxZ);
        runOnMainSync(()->click(v,rect.left+rect.width()*.6f,rect.top+rect.height()*.4f));settle();
        check(Math.abs(v.camera.x-transform.x(.6f))<.01f&&Math.abs(v.camera.z-transform.z(.4f))<.01f,"north-up click aligns rotated camera");
        check(Arrays.equals(before,authority()),"minimap click never issues command");shot("r12-minimap-yaw67");
        float x=v.camera.x,z=v.camera.z,span=v.camera.span;runOnMainSync(host::toggleNavigator);check(!(Boolean)field(v,"navigatorShown"),"toolbar hides navigator");check(v.camera.x==x&&v.camera.z==z&&v.camera.span==span,"navigator toggle does not fit camera");runOnMainSync(host::toggleNavigator);
        runOnMainSync(()->activity.selectAndFocus(world.home().hex));settle();
        View panel=(View)field(activity,"panelShell");check(panel.isClickable(),"empty modal panel consumes touch");
        Hex selected=(Hex)field(activity,"selected");runOnMainSync(()->click(panel,8,panel.getHeight()-8));settle();check(Objects.equals(selected,field(activity,"selected")),"panel background cannot tap map");shot("r12-panel");
        runOnMainSync(()->invoke("closePanel",new Class<?>[0]));
        TextView date=(TextView)field(activity,"dateBanner");check(date.getWidth()>120*activity.getResources().getDisplayMetrics().density,"date has reserved width");
        note("PASS north-up minimap, rotated camera, no command leakage, panel interception, date width");
    }
    private Object unchecked(Object o,String n){try{return field(o,n);}catch(Exception e){throw new RuntimeException(e);}}
    private void preview()throws Exception{
        World template=ScenarioCatalog.load("coalition-190",0,20260924L);byte[] before=SaveCodec.encode(template);ScenarioFactionPicker[] picker={null};int[] chosen={-1};
        runOnMainSync(()->{picker[0]=new ScenarioFactionPicker(activity,template,side->{chosen[0]=side;invoke("startScenario",new Class<?>[]{String.class,int.class},template.scenarioId,side);});picker[0].show();});settle();
        MapHost preview=(MapHost)field(picker[0],"map");check(preview.is3D(),"new scenario preview inherits native 3D");
        MapHost original=host;host=preview;shot("r12-opening-preview");host=original;
        runOnMainSync(()->((Button)unchecked(picker[0],"start")).performClick());settle();check(chosen[0]>=0,"real faction confirmation callback");check(Arrays.equals(before,SaveCodec.encode(template)),"preview does not mutate scenario");
        check(host.territoryMode()==2,"opening preview cannot overwrite gameplay territory preference");
        World previous=world;long deadline=SystemClock.uptimeMillis()+90000;
        while(SystemClock.uptimeMillis()<deadline){runOnMainSync(()->world=SessionProbe.view(activity));if(world!=previous)break;settle();}
        check(world!=previous,"actual background scenario start installs session");check(host.is3D(),"scenario start preserves native mode");ready();
        note("PASS real ScenarioFactionPicker native rendering, confirmation and actual startScenario session installation");
    }
    private void combatAndSelection()throws Exception{
        World fixture=NativeR11Fixture.world("counter");World reference=SaveCodec.decode(SaveCodec.encode(fixture));
        runOnMainSync(()->{SessionProbe.install(activity,fixture);activity.refresh();world=SessionProbe.view(activity);host.switchMode(true);});ready();
        World.Unit unit=world.fieldUnits().stream().filter(u->u.owner==world.player).findFirst().orElseThrow();byte[] before=authority();
        runOnMainSync(()->invoke("onUnitTile",new Class<?>[]{int.class,Hex.class},unit.id,unit.hex));settle();
        ClientState ui=(ClientState)field(activity,"ui");check(!ui.panelVisible,"unit selects without blocking details");check("select".equals(field(activity,"unitCommand")),"selection not command");check(!(Boolean)field(spatial(),"commandTargeting"),"reachable selection does not disable stable-ID picking");check(Arrays.equals(before,authority()),"selection keeps full authority");
        MapSceneSnapshot snap=(MapSceneSnapshot)field(spatial(),"snapshot");check(snap.reachable.equals(world.orders.marchReachable(world.unit(unit.id)).keySet()),"installed move preview authority set");
        check(!snap.attackTargets.isEmpty(),"counter fixture has highlighted ordinary attack targets");
        runOnMainSync(()->((MapView)unchecked(host,"flat")).setWorld(world,unit.hex,unit.id));
        check(snap.attackTargets.equals(field(field(host,"flat"),"attackTargets")),"installed 2D/3D exact attack target equality");
        Set<Hex> targets=new HashSet<>();targets.add(unit.hex);runOnMainSync(()->host.setPickTargets(targets));targets.clear();check(((Set<?>)field(spatial(),"targets")).size()==1,"input target snapshot detached");runOnMainSync(()->host.setPickTargets(Collections.emptySet()));
        check(NativeR11Fixture.command(reference,"counter").ok,"reference attack");runOnMainSync(()->{check(SessionProbe.command(activity,w->NativeR11Fixture.command(w,"counter")).ok,"normal Activity attack");host.cancelCommandEffects();});
        check(Arrays.equals(SaveCodec.encode(reference),authority()),"attack full authority matches");shot("r12-explicit-fixture-attack");note("PASS explicit combat fixture selection/no auto command, move preview, attack full-save parity");
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        World initial=ScenarioCatalog.load("coalition-190",0,20260924L);try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(initial));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");world=SessionProbe.view(activity);dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        runOnMainSync(()->{host.switchMode(true);invoke("closePanel",new Class<?>[0]);});ready();
        note("SOURCE="+BuildConfig.SOURCE_REVISION+" emulator API29 x86_64 SwANGLE; no physical-device/PC-art claim");
        combinations();navigatorAndPanels();preview();
        commandFlow();note("PASS official deployment/movement/next-turn/autosave/load parity; invoked normal Activity transaction, not full manual walkthrough");
        combatAndSelection();note("PASS R12 checks="+checks+"; full manual touch walkthrough, orientation matrix, Adreno/Mali NOT_RUN");
        result.putString("stream","PASS R12 checks="+checks+"\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{note("FAIL "+android.util.Log.getStackTraceString(e));}catch(Exception ignored){}result.putString("stream","FAIL R12 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

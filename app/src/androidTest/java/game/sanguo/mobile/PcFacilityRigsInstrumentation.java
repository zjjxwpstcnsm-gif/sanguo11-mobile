package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Actual next-turn worker/journal/playback fires source platform skin; no synthetic event or fraction. */
public final class PcFacilityRigsInstrumentation extends SceneInstrumentation {
    private File dir,report;private FilamentMapView renderer;
    private boolean resourceEvidence;
    @Override public void onCreate(Bundle arguments){resourceEvidence=arguments!=null&&"true".equals(arguments.getString("resourceEvidence"));super.onCreate(arguments);}
    @Override public void onStart(){
        Bundle result=new Bundle();dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();report=new File(dir,"pc-platform-rig-report.txt");
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");boolean existed=auto.isFile(),backed=false;byte[] original=null;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);Map<String,?> old=new HashMap<>(prefs.getAll());var client=getTargetContext().getSharedPreferences("MainActivity",0);Map<String,?> oldClient=new HashMap<>(client.getAll());
        try{
            Files.deleteIfExists(report.toPath());if(existed)original=Files.readAllBytes(auto.toPath());backed=true;Files.write(auto.toPath(),SaveCodec.encode(ScenarioCatalog.load("heroes-250",0)));prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
            checkedMain(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);});renderer=(FilamentMapView)field(host,"spatial");
            for(boolean damaged:new boolean[]{false,true}){
                var setup=PcFacilityRigsFixture.prepare(damaged);World expected=SaveCodec.decode(SaveCodec.encode(setup.world()));check(setup.command(expected).ok,"independent normal next turn");byte[] expectedBytes=SaveCodec.encode(expected);String label=damaged?"damaged":"normal";
                checkedMain(()->{SessionProbe.install(activity,setup.world());activity.refresh();host.switchMode(true);host.focus(setup.focus());renderer.camera.span=3;renderer.camera.yaw=0;renderer.camera.tilt=55;});settle();ready();assetsReady(setup.tower());inspect(setup.tower(),label+"-before");shot(label+"-before");
                checkedMain(()->invoke("advanceTurn",new Class<?>[0]));TurnWork work=(TurnWork)field(activity,"turnWork");check(work!=null,"normal production single-writer turn worker");checkedMain(()->{work.fullReplay=true;work.speed=1;});
                for(float desired:new float[]{.55f,.74f,.94f}){
                    // Default performance window stays 240s. The explicitly named
                    // resource-evidence run separates slow full national AI from
                    // pose verification; it must not be counted as performance PASS.
                    long end=SystemClock.uptimeMillis()+(resourceEvidence&&desired==.55f?600000:240000);boolean[] found={false};boolean[] firingObserved={false};
                    while(SystemClock.uptimeMillis()<end){checkedMain(()->{try{
                        var event=(TurnJournal.Event)field(renderer,"replay");float fraction=(Float)field(renderer,"replayFraction");
                        if(PcFacilityRigs.firing(event)&&event.sourceKey.equals("s"+setup.tower()))firingObserved[0]=true;
                        if(PcFacilityRigs.firing(event)&&event.sourceKey.equals("s"+setup.tower())&&fraction>=desired){var playback=(TurnPlayback)field(activity,"playback");if(playback!=null){playback.pause(true);found[0]=true;}}
                    }catch(Exception e){throw new RuntimeException(e);}});if(found[0])break;if(!(Boolean)field(activity,"aiRunning"))break;SystemClock.sleep(15);}
                    record(label+" phase-request="+desired+" firing-observed="+firingObserved[0]+" compute-done="+work.done+" compute-ms="+work.computeMillis+" mode="+(resourceEvidence?"resource-evidence-no-performance-acceptance":"240s-performance-window"));
                    check(found[0],"normal firing source phase observed "+desired);assetsReady(setup.tower());inspect(setup.tower(),label+"-phase-"+desired);shot(label+"-phase-"+desired);
                    float paused=(Float)field(renderer,"replayFraction");SystemClock.sleep(250);check(paused==(Float)field(renderer,"replayFraction"),"player pause preserves source clock");
                    checkedMain(()->{try{((TurnPlayback)field(activity,"playback")).pause(false);}catch(Exception e){throw new RuntimeException(e);}});
                }
                checkedMain(()->work.speed=4);long end=SystemClock.uptimeMillis()+120000;while((Boolean)field(activity,"aiRunning")&&SystemClock.uptimeMillis()<end)SystemClock.sleep(50);
                check(!(Boolean)field(activity,"aiRunning"),"normal accelerated full turn completes");byte[][] actual={null};checkedMain(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
                check(Arrays.equals(expectedBytes,actual[0]),"complete normal-turn authority and RNG equal independent reference");check(Arrays.equals(actual[0],Files.readAllBytes(auto.toPath())),"normal turn actual autosave exact");
                settle();ready();assetsReady(setup.tower());inspect(setup.tower(),label+"-after");shot(label+"-after");record("NORMAL_TURN "+label+" visible="+work.visibleCount+" batches="+work.publishedBatches+" authority=exact\n"+host.report());
            }
            checkedMain(()->callActivityOnPause(activity));check(!(Boolean)field(renderer,"resumed"),"pause releases frame gate");checkedMain(()->callActivityOnResume(activity));settle();ready();
            checkedMain(()->host.switchMode(false));check(field(renderer,"pcFacilityRigs")==null&&field(renderer,"pcUnits")==null,"scene exit releases both source skin libraries");check(((Map<?,?>)field(renderer,"pcTextureSizes")).isEmpty(),"all source GPU texture owners released");
            result.putString("stream","PASS PC PLATFORM RIG installed checks="+checks+"; normal/damaged actual next-turn firing, pause/4x, exact full save/RNG; emulator only; source projectile and PC rendered comparison pending\n");
        }catch(Throwable e){result.putString("stream","FAIL PC PLATFORM RIG "+android.util.Log.getStackTraceString(e));try{capture("pc-platform-rig-failed");}catch(Exception ignored){}}
        finally{
            if(activity!=null)checkedMain(()->{try{TurnWork work=(TurnWork)field(activity,"turnWork");if(work!=null){work.cancel();((GameApplication)activity.getApplication()).host().playbackFinished(work);}TurnPlayback playback=(TurnPlayback)field(activity,"playback");if(playback!=null)playback.detach();activity.finish();}catch(Exception e){throw new RuntimeException(e);}});
            try{finishActivityForRestore();}catch(Throwable e){result.putString("stream",result.getString("stream")+"FAIL lifecycle restoration barrier "+e);}
            try{if(backed){if(existed){Files.write(auto.toPath(),original);if(!Arrays.equals(original,Files.readAllBytes(auto.toPath())))throw new IOException("restored save differs");}else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"FAIL restoration "+e);}
            restore(prefs,old);restore(client,oldClient);
        }
        try{record(result.getString("stream"));}catch(IOException ignored){}finish(Activity.RESULT_OK,result);
    }
    private void checkedMain(Runnable action){Throwable[] failure={null};super.runOnMainSync(()->{try{action.run();}catch(Throwable e){failure[0]=e;}});if(failure[0]!=null)throw new IllegalStateException("Main platform verification",failure[0]);}
    private void assetsReady(int id)throws Exception{
        long end=SystemClock.uptimeMillis()+30000;boolean[] ready={false};
        while(SystemClock.uptimeMillis()<end){checkedMain(()->{try{Object p=((Map<?,?>)field(renderer,"objects")).get("structure:"+id);var item=p==null?null:(MapSceneSnapshot.Item)field(p,"item");var event=(TurnJournal.Event)field(renderer,"replay");float f=(Float)field(renderer,"replayFraction");
            ready[0]=p!=null&&PcFacilityRigs.key(item,((MapSceneSnapshot)field(renderer,"snapshot")).month,PcFacilityRigs.frame(item,event,f)).equals(field(p,"poseKey"))&&!(Boolean)field(renderer,"assetSyncPending")&&((SceneAssetQueue)field(renderer,"assetWork")).pending()==0;
        }catch(Exception e){throw new RuntimeException(e);}});if(ready[0])break;settle();}check(ready[0],"actual event source pose decode/upload complete");check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no source platform fallback");
    }
    private void inspect(int id,String name)throws Exception{String[] line={null};checkedMain(()->{try{
        Object p=((Map<?,?>)field(renderer,"objects")).get("structure:"+id);var item=(MapSceneSnapshot.Item)field(p,"item");SceneMesh mesh=(SceneMesh)field(field(p,"shape"),"source");var snapshot=(MapSceneSnapshot)field(renderer,"snapshot");var e=(TurnJournal.Event)field(renderer,"replay");float f=(Float)field(renderer,"replayFraction");int frame=PcFacilityRigs.frame(item,e,f);
        SceneMesh expected=((PcFacilityRigs)field(renderer,"pcFacilityRigs")).mesh(item,snapshot.month,frame,(PcFacilities)field(renderer,"pcFacilities"));
        check(mesh.pcFacilityRig&&mesh.pcFacility&&!mesh.pcUnit,"actual original platform rig body");check(Arrays.equals(mesh.vertices,expected.vertices)&&Arrays.equals(mesh.indices,expected.indices)&&Arrays.equals(mesh.uv,expected.uv)&&Arrays.equals(mesh.tangents,expected.tangents),"actual GPU original full source pose and seasonal UV");
        check(field(p,"alphaInstance")!=null&&mesh.pcUnitOpaqueIndices==0,"original all-alpha platform pass bound");check(frame==0||e!=null&&e.kind==TurnJournal.Kind.FACILITY_ATTACK,"only normal facility event moves source rig");
        check((Integer)field(p,"flag")==0&&(Integer)field(p,"state")==0,"source platform has no compatibility flag/fire scaffold");
        check(((CombatVisual)field(renderer,"combat")).count==0,"source platform event does not sample approximate projectiles");
        for(boolean shown:(boolean[])field(renderer,"effectShown"))check(!shown,"source platform has no compatibility effect entity");
        line[0]=name+" pose="+field(p,"poseKey")+" frame="+frame+" fraction="+f+" vertices="+mesh.vertices.length/7+" triangles="+mesh.indices.length/3;
    }catch(Exception e){throw new RuntimeException(e);}});record(line[0]);}
    private void shot(String name)throws Exception{surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"pc-platform-"+name+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture("pc-platform-"+name+"-ui");}
    private void record(String line)throws IOException{Files.write(report.toPath(),(line+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private static void restore(android.content.SharedPreferences p,Map<String,?> old){var e=p.edit().clear();for(var r:old.entrySet()){Object v=r.getValue();String k=r.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));else throw new IllegalArgumentException(k);}e.commit();}
}

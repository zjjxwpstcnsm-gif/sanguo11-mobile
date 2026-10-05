package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Original unit GPU inputs driven by normal GameSession commands and production pause/speed controls. */
public final class PcUnitsInstrumentation extends SceneInstrumentation {
    private String kind="SWORD",mode="both";private boolean waterProbe;private File dir;private FilamentMapView renderer;
    @Override public void onCreate(Bundle args){if(args!=null){kind=args.getString("unitKind","SWORD");mode=args.getString("commandMode","both");waterProbe=Boolean.parseBoolean(args.getString("waterProbe","false"));}super.onCreate(args);}
    @Override public void onStart(){
        Bundle result=new Bundle();dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();File report=new File(dir,"pc-units-"+kind+"-"+mode+"-report.txt");
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");byte[] backup=null;boolean existed=auto.isFile(),backed=false;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);Map<String,?> old=new HashMap<>(prefs.getAll());
        var client=getTargetContext().getSharedPreferences("MainActivity",0);Map<String,?> oldClient=new HashMap<>(client.getAll());
        try{
            check(kind.equals("all")||Arrays.asList(PcUnitsFixture.KINDS).contains(kind),"explicit source unit kind");check(Arrays.asList("both","move","attack").contains(mode),"explicit normal command scope");Files.deleteIfExists(report.toPath());
            if(existed)backup=Files.readAllBytes(auto.toPath());backed=true;Files.write(auto.toPath(),SaveCodec.encode(ScenarioCatalog.load("heroes-250",0)));prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
            checkedMain(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);});renderer=(FilamentMapView)field(host,"spatial");
            for(String k:kind.equals("all")?PcUnitsFixture.KINDS:new String[]{kind})for(String m:mode.equals("both")?new String[]{"move","attack"}:new String[]{mode}){
                if(k.equals("transport")&&m.equals("attack"))continue;
                var prepared=PcUnitsFixture.prepare(k,m);World expected=SaveCodec.decode(SaveCodec.encode(prepared.world()));World.Result reference=prepared.command(expected);check(reference.ok,"independent legal source unit command "+k+"/"+m);
                checkedMain(()->{SessionProbe.install(activity,prepared.world());activity.refresh();host.switchMode(true);host.focus(prepared.focus());renderer.camera.span=2;renderer.camera.yaw=0;renderer.camera.tilt=55;host.pauseCommandEffects(true);});settle();ready();assetsReady();
                inspect(prepared.actor(),report,k+"-"+m+"-before");shot(k+"-"+m+"-before");
                if(waterProbe&&k.equals("WARSHIP"))probeOriginalWater(report,k+"-"+m);
                World.Result[] command={null};checkedMain(()->{command[0]=SessionProbe.command(activity,prepared::command);host.pauseCommandEffects(true);});check(command[0].ok,"normal production unit command "+k+"/"+m);settle();
                check(host.commandEffectsActive()&&host.commandEffectsPaused(),"normal command owns paused production sequence");
                assetsReady();inspect(prepared.actor(),report,k+"-"+m+"-prepare");shot(k+"-"+m+"-prepare");
                // Real player controls advance the actual journal. No fabricated
                // event or synthetic fraction is installed into the renderer.
                for(int step=0;step<3&&host.commandEffectsActive();step++){
                    checkedMain(()->host.pauseCommandEffects(false));SystemClock.sleep(220);checkedMain(()->host.pauseCommandEffects(true));settle();assetsReady();
                    inspect(prepared.actor(),report,k+"-"+m+"-step"+step);shot(k+"-"+m+"-step"+step);
                }
                checkedMain(()->{host.commandEffectSpeed(4);host.pauseCommandEffects(false);});long end=SystemClock.uptimeMillis()+30000;while(host.commandEffectsActive()&&SystemClock.uptimeMillis()<end)settle();check(!host.commandEffectsActive(),"normal accelerated source unit sequence finishes");
                checkedMain(()->host.pauseEffects(true));settle();ready();assetsReady();inspect(prepared.actor(),report,k+"-"+m+"-after");shot(k+"-"+m+"-after");
                byte[][] actual={null};checkedMain(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
                check(Arrays.equals(SaveCodec.encode(expected),actual[0]),"complete unit authority/RNG/save matches independent normal command");check(Arrays.equals(actual[0],SaveCodec.encode(SaveCodec.decode(actual[0]))),"post-command exact save roundtrip");
                Files.write(report.toPath(),("NORMAL_UNIT "+k+"/"+m+" "+command[0].message+"\n"+host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            checkedMain(()->callActivityOnPause(activity));check(!(Boolean)field(renderer,"resumed"),"pause stops source unit frame gate");checkedMain(()->callActivityOnResume(activity));settle();ready();
            checkedMain(()->host.switchMode(false));check(field(renderer,"pcUnits")==null&&field(renderer,"pcUnitMaterial")==null&&field(renderer,"pcUnitAlphaMaterial")==null,"scene exit releases source unit CPU/material owners");
            if(waterProbe){check(field(renderer,"pcWaterMaterial")==null&&field(renderer,"pcWaterSheet0")==null&&field(renderer,"pcWaterSheet1")==null,"scene exit releases original water material/textures");check(((Map<?,?>)field(renderer,"pcWaterClocks")).isEmpty(),"scene exit releases presentation water clocks");}
            for(Object sheet:(Object[])field(renderer,"pcUnitSheets"))check(sheet==null,"original unit texture released");check(((Map<?,?>)field(renderer,"pcTextureSizes")).isEmpty(),"tracked source textures all released");
            result.putString("stream","PASS PC UNITS installed checks="+checks+" kind="+kind+" mode="+mode+"; source GPU geometry, normal commands, pause/resume/4x, exact authority; emulator only; original PC timing/flags/reference pending\n");
        }catch(Throwable e){result.putString("stream","FAIL PC UNITS "+android.util.Log.getStackTraceString(e));try{capture("pc-units-"+kind+"-failed");}catch(Exception ignored){}}
        finally{
            try{finishActivityForRestore();}catch(Throwable e){result.putString("stream",result.getString("stream")+"FAIL lifecycle restoration barrier "+e);}
            try{if(backed){if(existed){Files.write(auto.toPath(),backup);if(!Arrays.equals(backup,Files.readAllBytes(auto.toPath())))throw new IOException("restored autosave differs");}else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"FAIL autosave restoration "+e);}
            restore(prefs,old);restore(client,oldClient);
        }
        try{Files.write(report.toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}finish(Activity.RESULT_OK,result);
    }
    private void checkedMain(Runnable action){
        Throwable[] error={null};super.runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});
        if(error[0]!=null)throw new IllegalStateException("Main-thread source-unit verification failed",error[0]);
    }
    private Map<String,Long> waterClocks(){
        Map<String,Long> copy=new HashMap<>();checkedMain(()->{try{copy.putAll((Map<String,Long>)field(renderer,"pcWaterClocks"));}catch(Exception e){throw new IllegalStateException(e);}});return copy;
    }
    private void probeOriginalWater(File report,String label)throws Exception {
        checkedMain(()->{
            try{
            check(field(renderer,"pcWaterMaterial")!=null&&field(renderer,"pcWaterSheet0")!=null&&field(renderer,"pcWaterSheet1")!=null,"original4844 water material and both source sheets are bound");
            int visible=0;for(Object parent:((Map<?,?>)field(renderer,"terrain")).values()){
                SceneMesh mesh=(SceneMesh)field(parent,"source");check(mesh.landIndexCount==mesh.indices.length,"PC parent never draws legacy water indices");
                Object child=field(parent,"waterChild");if(child!=null&&(Boolean)field(child,"shown")){
                    visible++;check(((SceneMesh)field(child,"source")).pcWater&&field(child,"waterInstance")!=null,"visible native water quad has its own GPU material");
                }
            }
            check(visible>0,"WARSHIP normal scene includes original coarse water");host.pauseEffects(true);
            }catch(Exception e){throw new IllegalStateException(e);}
        });
        Map<String,Long> paused=waterClocks();SystemClock.sleep(350);settle();check(paused.equals(waterClocks()),"paused water clock remains exactly unchanged");
        checkedMain(()->host.pauseEffects(false));long deadline=SystemClock.uptimeMillis()+10000;boolean advanced=false;
        while(SystemClock.uptimeMillis()<deadline){SystemClock.sleep(100);Map<String,Long> now=waterClocks();for(var e:now.entrySet())if(e.getValue()>paused.getOrDefault(e.getKey(),0L)){advanced=true;break;}if(advanced)break;}
        check(advanced,"normal unpaused game frames advance source water animation");
        shot(label+"-source-water-playing");SystemClock.sleep(600);shot(label+"-source-water-later");
        checkedMain(()->host.pauseEffects(true));Map<String,Long> end=waterClocks();
        Files.write(report.toPath(),("SOURCE_WATER "+label+" paused="+paused+" resumed="+end+"; native coarse topology/4844 sheets; PC visual/time comparison pending\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private void assetsReady()throws Exception{
        long end=SystemClock.uptimeMillis()+30000;boolean[] ready={false};
        while(SystemClock.uptimeMillis()<end){checkedMain(()->{try{
            ready[0]=!(Boolean)field(renderer,"assetSyncPending")&&((SceneAssetQueue)field(renderer,"assetWork")).pending()==0;
            var snapshot=(MapSceneSnapshot)field(renderer,"snapshot");var replay=(TurnJournal.Event)field(renderer,"replay");float fraction=(Float)field(renderer,"replayFraction");
            long clock=(Long)field(renderer,(Boolean)field(renderer,"effectsPaused")?"pausedEffectTick":"animationTick");
            for(Object proxy:((Map<?,?>)field(renderer,"objects")).values()){
                var item=(MapSceneSnapshot.Item)field(proxy,"item");if(item.unit==null||!(Boolean)field(proxy,"shown"))continue;
                UnitAnimation animation=new UnitAnimation();animation.sample(item,snapshot.ground,replay,fraction,clock,(Integer)field(renderer,"unitLod"));
                String expected=((PcUnits)field(renderer,"pcUnits")).animation(item.unit,animation,clock,replay,fraction).key();
                if(!expected.equals(field(proxy,"poseKey")))ready[0]=false;
            }
        }catch(Exception e){throw new RuntimeException(e);}});if(ready[0])break;settle();}
        check(ready[0],"bounded original unit pose decode/upload completes");check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no original unit placeholder or decode fallback");
    }
    private void inspect(int actor,File report,String name)throws Exception{
        String[] text={null};checkedMain(()->{try{
            check(((MapSceneSnapshot)field(renderer,"snapshot")).ground.pcMap!=null,"actual original national terrain");Object proxy=((Map<?,?>)field(renderer,"objects")).get("unit:"+actor);check(proxy!=null,"normal unit proxy exists");
            MapSceneSnapshot.Item item=(MapSceneSnapshot.Item)field(proxy,"item");SceneMesh mesh=(SceneMesh)field(field(proxy,"shape"),"source");String key=(String)field(proxy,"poseKey");check(mesh.pcUnit&&key!=null&&key.startsWith("pc-unit:"),"GPU source unit has original FCVD pose");
            String[] parts=key.split(":");SceneMesh expected=((PcUnits)field(renderer,"pcUnits")).pose(Integer.parseInt(parts[1]),Integer.parseInt(parts[2]),Integer.parseInt(parts[3]));
            check(Arrays.equals(mesh.vertices,expected.vertices)&&Arrays.equals(mesh.indices,expected.indices)&&Arrays.equals(mesh.uv,expected.uv)&&Arrays.equals(mesh.tangents,expected.tangents),"actual GPU inputs equal complete original source skin pose");
            check(mesh.pcUnitModel==expected.pcUnitModel&&mesh.pcUnitOpaqueIndices==expected.pcUnitOpaqueIndices,"source model and original alpha split retained");
            check(((CombatVisual)field(renderer,"combat")).count==0,"source map does not sample compatibility particles");
            for(boolean shown:(boolean[])field(renderer,"effectShown"))check(!shown,"no compatibility effect entity displayed in source map");
            for(Object other:((Map<?,?>)field(renderer,"objects")).values()){
                check((Integer)field(other,"flag")==0,"source object has no approximate flag");
                check((Integer)field(other,"state")==0,"source object has no approximate scaffold/fire overlay");
            }
            var formation=(PcUnitFormation)field(proxy,"pcFormation");var animation=(UnitAnimation)field(proxy,"animation");int troops=UnitAnimation.shownTroops(item.unit,(TurnJournal.Event)field(renderer,"replay"),(Float)field(renderer,"replayFraction"));
            check((Integer)field(proxy,"memberCount")==formation.count&&formation.count==PcUnits.members(troops,item.unit.singleModel(animation.naval)),"all original formation members drawn");check((Integer)field(proxy,"flag")==0,"no legacy substitute unit flag");
            check(field(proxy,"alphaInstance")!=null&&((Object[])field(renderer,"pcUnitSheets"))[mesh.pcUnitModel]!=null,"independent original sheet and source alpha material bound");
            var engine=(com.google.android.filament.Engine)field(renderer,"engine");var manager=engine.getRenderableManager();int instance=manager.getInstance((Integer)field(proxy,"entity"));
            check(manager.getPrimitiveCount(instance)==(mesh.pcUnitOpaqueIndices>0&&mesh.pcUnitOpaqueIndices<mesh.indices.length?2:1),"actual GPU preserves both source draw passes");
            text[0]=name+" pose="+key+" members="+formation.count+" alpha_indices="+(mesh.indices.length-mesh.pcUnitOpaqueIndices)+" replay_fraction="+field(renderer,"replayFraction")+"\n";
        }catch(Throwable e){throw new RuntimeException(e);}});Files.write(report.toPath(),text[0].getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private void shot(String name)throws Exception{surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"pc-unit-"+name+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture("pc-unit-"+name+"-ui");}
    private static void restore(android.content.SharedPreferences prefs,Map<String,?> old){var e=prefs.edit().clear();for(var r:old.entrySet()){Object v=r.getValue();String k=r.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));else throw new IllegalArgumentException(k);}e.commit();}
}

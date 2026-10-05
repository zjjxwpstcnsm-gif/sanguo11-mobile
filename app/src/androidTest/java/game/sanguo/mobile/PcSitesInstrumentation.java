package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed source cities, original walls, gates and ports, seasonal textures and release.
 * This is renderer integration evidence; it is not a PC image comparison. */
public final class PcSitesInstrumentation extends SceneInstrumentation {
    private boolean commandsOnly,siegesOnly;
    @Override public void onCreate(Bundle args){siegesOnly=args!=null&&"true".equals(args.getString("siegesOnly"));commandsOnly=siegesOnly||args!=null&&"true".equals(args.getString("commandsOnly"));super.onCreate(args);}
    @Override public void onStart(){
        Bundle result=new Bundle();File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");
        byte[] backup=null;boolean existed=auto.isFile(),backupReady=false;
        android.content.SharedPreferences prefs=getTargetContext().getSharedPreferences("map-renderer",0);
        Map<String,?> oldPrefs=new HashMap<>(prefs.getAll());
        android.content.SharedPreferences client=activityPreferences();
        Map<String,?> oldClient=new HashMap<>(client.getAll());
        try{
            Files.deleteIfExists(new File(dir,"pc-sites-report.txt").toPath());
            if(existed)backup=Files.readAllBytes(auto.toPath());backupReady=true;
            World seed=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(seed);
            Files.write(auto.toPath(),before);prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            host=(MapHost)field(activity,"map");world=SessionProbe.view(activity);
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);if(siegesOnly)host.focusNative(world.cities.get(0).hex);else host.switchMode(true);host.setGridShown(false);});settle();
            check(host.is3D(),"production 3D initialized with source RGBA material");
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");
            PcSites nativeSites=(PcSites)field(renderer,"pcSites");
            check(nativeSites.placementCount()==87,"all source placement records loaded");
            World review=SaveCodec.decode(before);MapSceneSnapshot initial=(MapSceneSnapshot)field(renderer,"snapshot");
            List<MapSceneSnapshot.Item> examples=new ArrayList<>();Set<String> kinds=new HashSet<>();
            for(var item:initial.items)if(item.site!=null){var p=nativeSites.placement(initial.ground,item);check(p!=null,"all87 source anchors bound");if(kinds.add(p.kind()+":"+PcSites.textureDelta(p.kind(),10,p.climate())))examples.add(item);}
            check(examples.size()==10,"six city styles and cold/warm gate/port variants represented");
            if(!commandsOnly)for(var example:examples)for(int month:new int[]{1,10}){
                review.startMonth=month;MapSceneSnapshot snapshot=new MapSceneSnapshot(initial.ground,review,example.hex,-1);
                runOnMainSync(()->{renderer.focus(example.hex);renderer.camera.span=6;renderer.camera.yaw=0;renderer.camera.tilt=55;renderer.camera.facing=1;renderer.snapshot(snapshot);});settle();ready();
                long deadline=android.os.SystemClock.uptimeMillis()+30000;
                while((Boolean)field(renderer,"assetSyncPending")&&android.os.SystemClock.uptimeMillis()<deadline)settle();
                Map<?,?> objects=(Map<?,?>)field(renderer,"objects");int nativeCount=0;
                for(Object proxy:objects.values()){
                    MapSceneSnapshot.Item item=(MapSceneSnapshot.Item)field(proxy,"item");if(item.site==null)continue;
                    SceneMesh mesh=(SceneMesh)field(field(proxy,"shape"),"source");check(mesh.pcSite,"source site body+wall GPU mesh "+item.key);nativeCount++;
                }
                check(nativeCount==87,"all87 sites uploaded through production path");
                check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no missing source assets");
                var placement=nativeSites.placement(initial.ground,example);
                surfaceCapture();String name="pc-site-"+placement.kind()+"-climate"+placement.climate()+"-month"+month;
                Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(name+"-ui");
                Files.write(new File(dir,"pc-sites-report.txt").toPath(),(name+"\n"+host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            // Separate command evidence can proceed while national readiness fails.
            // The default and existing commandsOnly modes retain their original far assertion.
            if(!siegesOnly){runOnMainSync(()->{renderer.camera.span=50;renderer.camera.yaw=35;});settle();ready();surfaceCapture();capture("pc-sites-far");}else{settle();ready();}
            runOnMainSync(()->callActivityOnPause(activity));
            check(!(Boolean)field(renderer,"resumed"),"activity pause closes production frame gate");
            runOnMainSync(()->callActivityOnResume(activity));settle();ready();
            check((Boolean)field(renderer,"resumed"),"activity resume restores production frame gate");
            check(field(renderer,"pcSitesAtlas")!=null,"source site texture survives pause/resume");
            byte[][] after={null};runOnMainSync(()->{try{after[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
            check(Arrays.equals(before,after[0]),"season/LOD/camera review leaves complete authority and RNG unchanged");
            for(int kind:new int[]{0,2,1}){
                PcSitesFixture.Case prepared=PcSitesFixture.prepare(kind);
                World expected=SaveCodec.decode(SaveCodec.encode(prepared.world()));
                World.Result expectedResult=expected.siege(prepared.attacker(),prepared.target());check(expectedResult.ok,"reference normal siege");
                runOnMainSync(()->{SessionProbe.install(activity,prepared.world());activity.refresh();host.switchMode(true);host.focus(prepared.world().city(prepared.target()).hex);renderer.camera.span=5;renderer.camera.yaw=0;});settle();ready();
                check(((MapSceneSnapshot)field(renderer,"snapshot")).ground.pcMap!=null,"battle fixture uses actual source map");
                Object beforeProxy=((Map<?,?>)field(renderer,"objects")).get("site:"+prepared.target());
                SceneMesh beforeMesh=(SceneMesh)field(field(beforeProxy,"shape"),"source");
                surfaceCapture();String name="pc-site-command-kind"+kind;
                Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-before.png").toPath(),StandardCopyOption.REPLACE_EXISTING);
                android.util.Log.i("SceneAcceptance","NORMAL_SIEGE_START kind="+kind);
                World.Result[] action={null};runOnMainSync(()->{action[0]=SessionProbe.command(activity,w->w.siege(prepared.attacker(),prepared.target()));activity.refresh();});
                check(action[0].ok,"installed normal siege transaction: "+action[0].message);
                long playbackDeadline=android.os.SystemClock.uptimeMillis()+15000;
                while(host.commandEffectsActive()&&android.os.SystemClock.uptimeMillis()<playbackDeadline)settle();
                check(!host.commandEffectsActive(),"normal command playback finishes before damage model acceptance");
                android.util.Log.i("SceneAcceptance","NORMAL_SIEGE_PLAYBACK_DONE kind="+kind);
                settle();ready();
                long deadline=android.os.SystemClock.uptimeMillis()+15000;
                while((Boolean)field(renderer,"assetSyncPending")&&android.os.SystemClock.uptimeMillis()<deadline)settle();
                byte[][] actual={null};runOnMainSync(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
                check(Arrays.equals(SaveCodec.encode(expected),actual[0]),"native damage rendering preserves exact normal siege authority/RNG/save");
                Object proxy=((Map<?,?>)field(renderer,"objects")).get("site:"+prepared.target());
                MapSceneSnapshot.Item item=(MapSceneSnapshot.Item)field(proxy,"item");
                check(kind==0?item.site.sourceWalls==1:item.site.sourceBuildings==1,"installed source model threshold follows normal command");
                SceneMesh afterMesh=(SceneMesh)field(field(proxy,"shape"),"source");
                check(afterMesh.pcSite&&afterMesh!=beforeMesh,"normal command swaps actual original geometry on GPU");
                surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-after.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(name+"-after-ui");
                Files.write(new File(dir,"pc-sites-report.txt").toPath(),("NORMAL_COMMAND kind="+kind+" "+action[0].message+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            runOnMainSync(()->host.switchMode(false));
            check(field(renderer,"pcSites")==null&&field(renderer,"pcSitesAtlas")==null,"source site GPU resources released on scene exit");
            check(((Map<?,?>)field(renderer,"pcTextureSizes")).isEmpty(),"all tracked source textures released");
            result.putString("stream","PASS PC SITES mode="+(siegesOnly?"sieges-only (national far excluded)":"original")+" installed checks="+checks+"; emulator renderer evidence only\n");
        }catch(Throwable e){result.putString("stream","FAIL PC SITES "+android.util.Log.getStackTraceString(e));try{capture("pc-sites-failed");}catch(Exception ignored){}}
        finally{
            try{finishActivityForRestore();}catch(Throwable e){result.putString("stream",result.getString("stream")+"\nFAIL lifecycle restoration barrier "+e);}
            try{if(backupReady){if(existed)Files.write(auto.toPath(),backup);else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"\nFAIL autosave restoration "+e);}
            restorePreferences(prefs,oldPrefs);restorePreferences(client,oldClient);
        }
        try{Files.write(new File(dir,"pc-sites-report.txt").toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}
        finish(Activity.RESULT_OK,result);
    }
    private android.content.SharedPreferences activityPreferences(){return getTargetContext().getSharedPreferences("MainActivity",0);}
    private static void restorePreferences(android.content.SharedPreferences prefs,Map<String,?> saved){
        android.content.SharedPreferences.Editor e=prefs.edit().clear();
        for(var row:saved.entrySet()){
            String key=row.getKey();Object value=row.getValue();
            if(value instanceof String)e.putString(key,(String)value);
            else if(value instanceof Boolean)e.putBoolean(key,(Boolean)value);
            else if(value instanceof Integer)e.putInt(key,(Integer)value);
            else if(value instanceof Long)e.putLong(key,(Long)value);
            else if(value instanceof Float)e.putFloat(key,(Float)value);
            else if(value instanceof Set)e.putStringSet(key,new HashSet<>((Set<String>)value));
            else throw new IllegalArgumentException("Unsupported saved preference "+key);
        }
        e.commit();
    }
}

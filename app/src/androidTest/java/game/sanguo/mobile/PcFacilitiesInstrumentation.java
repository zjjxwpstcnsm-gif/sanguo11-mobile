package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed normal facility commands. No direct HP changes after fixture installation. */
public final class PcFacilitiesInstrumentation extends SceneInstrumentation {
    @Override public void onStart(){
        Bundle result=new Bundle();File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");byte[] backup=null;
        boolean existed=auto.isFile(),backupReady=false;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);Map<String,?> saved=new HashMap<>(prefs.getAll());
        var client=getTargetContext().getSharedPreferences("MainActivity",0);Map<String,?> savedClient=new HashMap<>(client.getAll());
        try{
            Files.deleteIfExists(new File(dir,"pc-facilities-report.txt").toPath());
            if(existed)backup=Files.readAllBytes(auto.toPath());backupReady=true;
            Files.write(auto.toPath(),SaveCodec.encode(ScenarioCatalog.load("heroes-250",0)));
            prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);});settle();
            check(host.is3D(),"production native renderer active");
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");
            check(field(renderer,"pcFacilities")!=null&&field(renderer,"pcFacilitiesAtlas")!=null,"original facility catalog and exact RGBA sheets loaded");
            for(String mode:new String[]{"domestic-build","military-build","domestic-damage","military-damage","domestic-destroy","military-destroy"}){
                PcFacilitiesFixture.Case prepared=PcFacilitiesFixture.prepare(mode);
                World expected=SaveCodec.decode(SaveCodec.encode(prepared.world()));World.Result expectedResult=prepared.command(expected);check(expectedResult.ok,"reference normal command "+mode);
                runOnMainSync(()->{SessionProbe.install(activity,prepared.world());activity.refresh();host.switchMode(true);host.focus(prepared.target());renderer.camera.span=5;renderer.camera.yaw=0;renderer.camera.tilt=55;});settle();ready();awaitAssets(renderer);
                check(((MapSceneSnapshot)field(renderer,"snapshot")).ground.pcMap!=null,"source map remains bound in normal command fixture");
                Object prior=facilityProxy(renderer,prepared.target());SceneMesh priorMesh=prior==null?null:(SceneMesh)field(field(prior,"shape"),"source");
                check(mode.endsWith("build")?prior==null:priorMesh!=null&&priorMesh.pcFacility,"command precondition uses original body or empty build parcel");
                saveSurface(dir,mode+"-before");
                World.Result[] action={null};runOnMainSync(()->{action[0]=SessionProbe.command(activity,prepared::command);activity.refresh();});
                check(action[0].ok,"production transaction "+mode+": "+action[0].message);
                long deadline=android.os.SystemClock.uptimeMillis()+20000;
                while(host.commandEffectsActive()&&android.os.SystemClock.uptimeMillis()<deadline)settle();
                check(!host.commandEffectsActive(),"normal facility command playback finishes");settle();ready();awaitAssets(renderer);
                byte[][] actual={null};runOnMainSync(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
                check(Arrays.equals(SaveCodec.encode(expected),actual[0]),"source visual command preserves exact authority/RNG/save "+mode);
                Object after=facilityProxy(renderer,prepared.target());
                if(mode.endsWith("destroy"))check(after==null,"normal destruction releases render entity");
                else{
                    check(after!=null,"normal build/damage produces live facility entity");
                    SceneMesh mesh=(SceneMesh)field(field(after,"shape"),"source");check(mesh.pcFacility&&mesh!=priorMesh,"normal command selects actual original GPU geometry");
                    MapSceneSnapshot.Item item=(MapSceneSnapshot.Item)field(after,"item");
                    check(PcFacilities.state(item.facility)==(mode.endsWith("build")?item.facility.type.startsWith("domestic/")?2:item.facility.hp<item.facility.maxHp*.3f?3:2:1),"normal command source construction/damaged state");
                    if(mode.endsWith("build"))check((Integer)field(after,"state")==0,"original construction body replaces legacy scaffold overlay");
                }
                check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"source command has no missing/placeholder model");
                saveSurface(dir,mode+"-after");capture("pc-facility-"+mode+"-after-ui");
                Files.write(new File(dir,"pc-facilities-report.txt").toPath(),("NORMAL_COMMAND "+mode+" "+action[0].message+"\n"+host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            runOnMainSync(()->{renderer.camera.span=50;renderer.camera.yaw=35;});settle();ready();capture("pc-facilities-far");
            runOnMainSync(()->callActivityOnPause(activity));check(!(Boolean)field(renderer,"resumed"),"pause frame gate");
            runOnMainSync(()->callActivityOnResume(activity));settle();ready();check(field(renderer,"pcFacilitiesAtlas")!=null,"resume source texture retained");
            runOnMainSync(()->host.switchMode(false));check(field(renderer,"pcFacilities")==null&&field(renderer,"pcFacilitiesAtlas")==null,"scene exit releases source facility CPU/GPU catalog");
            check(((Map<?,?>)field(renderer,"pcTextureSizes")).isEmpty(),"tracked native textures all released");
            result.putString("stream","PASS PC FACILITIES installed checks="+checks+"; six normal commands; emulator evidence only\n");
        }catch(Throwable e){result.putString("stream","FAIL PC FACILITIES "+android.util.Log.getStackTraceString(e));try{capture("pc-facilities-failed");}catch(Exception ignored){}}
        finally{
            try{finishActivityForRestore();}catch(Throwable e){result.putString("stream",result.getString("stream")+"\nFAIL lifecycle restoration barrier "+e);}
            try{if(backupReady){if(existed)Files.write(auto.toPath(),backup);else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"\nFAIL autosave restoration "+e);}
            restore(prefs,saved);restore(client,savedClient);
        }
        try{Files.write(new File(dir,"pc-facilities-report.txt").toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}
        finish(Activity.RESULT_OK,result);
    }
    private Object facilityProxy(FilamentMapView renderer,Hex target)throws Exception{
        for(Object proxy:((Map<?,?>)field(renderer,"objects")).values()){
            var item=(MapSceneSnapshot.Item)field(proxy,"item");if(item.facility!=null&&item.hex.equals(target))return proxy;
        }return null;
    }
    private void awaitAssets(FilamentMapView renderer)throws Exception{
        long deadline=android.os.SystemClock.uptimeMillis()+30000;
        while((Boolean)field(renderer,"assetSyncPending")&&android.os.SystemClock.uptimeMillis()<deadline)settle();
        check(!(Boolean)field(renderer,"assetSyncPending"),"facility source GPU decode/upload completed");
    }
    private void saveSurface(File dir,String name)throws Exception{
        surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"pc-facility-"+name+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);
    }
    private static void restore(android.content.SharedPreferences prefs,Map<String,?> saved){
        var e=prefs.edit().clear();for(var row:saved.entrySet()){
            String k=row.getKey();Object v=row.getValue();
            if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
            else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);
            else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));
            else throw new IllegalArgumentException("Unsupported saved preference "+k);
        }e.commit();
    }
}

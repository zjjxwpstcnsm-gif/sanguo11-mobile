package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed real transactions, source connected GPU bodies and exact save/RNG. */
public final class PcConstructibleWallsInstrumentation extends SceneInstrumentation {
    private String kind="earth",commandMode="all";
    @Override public void onCreate(Bundle args){if(args!=null){kind=args.getString("wallKind","earth");commandMode=args.getString("commandMode","all");}super.onCreate(args);}
    @Override public void onStart(){
        Bundle result=new Bundle();File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();File report=new File(dir,"pc-live-walls-"+kind+"-report.txt");
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");byte[] backup=null;boolean existed=auto.isFile(),backed=false;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);Map<String,?> old=new HashMap<>(prefs.getAll());
        var client=getTargetContext().getSharedPreferences("MainActivity",0);Map<String,?> oldClient=new HashMap<>(client.getAll());
        try{
            check(kind.equals("earth")||kind.equals("stone"),"explicit supported wall fixture");Files.deleteIfExists(report.toPath());
            check(Arrays.asList("all","build","complete","damage","destroy").contains(commandMode),"explicit normal command filter");
            if(existed)backup=Files.readAllBytes(auto.toPath());backed=true;Files.write(auto.toPath(),SaveCodec.encode(ScenarioCatalog.load("heroes-250",0)));prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);});settle();check(host.is3D(),"normal production renderer");FilamentMapView renderer=(FilamentMapView)field(host,"spatial");
            for(String mode:new String[]{"build","complete","damage","destroy"}){
                if(!commandMode.equals("all")&&!mode.equals(commandMode))continue;
                PcWallsFixture.Case prepared=PcWallsFixture.prepare(kind+"-"+mode+"-odd");World expected=SaveCodec.decode(SaveCodec.encode(prepared.world()));World.Result expectedResult=prepared.command(expected);check(expectedResult.ok,"independent legal reference command");
                runOnMainSync(()->{SessionProbe.install(activity,prepared.world());activity.refresh();host.switchMode(true);host.focus(prepared.target());renderer.camera.span=3;renderer.camera.yaw=0;renderer.camera.tilt=55;});settle();ready();awaitAssets(renderer);
                MapSceneSnapshot prior=(MapSceneSnapshot)field(renderer,"snapshot");check(edges(prior)==(mode.equals("damage")||mode.equals("destroy")?2:0),"source graph before real command "+mode);verifyGPU(renderer,prior);save(dir,kind+"-"+mode+"-before");
                World.Result[] command={null};runOnMainSync(()->{command[0]=SessionProbe.command(activity,prepared::command);activity.refresh();});check(command[0].ok,"normal production transaction "+mode);
                long end=android.os.SystemClock.uptimeMillis()+20000;while(host.commandEffectsActive()&&android.os.SystemClock.uptimeMillis()<end)settle();check(!host.commandEffectsActive(),"normal command playback finished");settle();ready();awaitAssets(renderer);
                MapSceneSnapshot next=(MapSceneSnapshot)field(renderer,"snapshot");check(edges(next)==(mode.equals("complete")?2:0),"all obsolete GPU connections disappear or source completion creates them");verifyGPU(renderer,next);
                byte[][] actual={null};runOnMainSync(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});check(Arrays.equals(SaveCodec.encode(expected),actual[0]),"authority/RNG/save exact after normal wall command");
                check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no missing/placeholder source wall asset");save(dir,kind+"-"+mode+"-after");capture("pc-live-wall-"+kind+"-"+mode+"-after-ui");
                Files.write(report.toPath(),("NORMAL_COMMAND "+prepared.mode()+" "+command[0].message+"\n"+host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            runOnMainSync(()->{renderer.camera.span=50;renderer.camera.yaw=35;});settle();ready();capture("pc-live-wall-"+kind+"-far");
            runOnMainSync(()->callActivityOnPause(activity));check(!(Boolean)field(renderer,"resumed"),"pause frame gate");runOnMainSync(()->callActivityOnResume(activity));settle();ready();
            runOnMainSync(()->host.switchMode(false));check(field(renderer,"pcFacilities")==null&&field(renderer,"pcFacilitiesInstance")==null,"source wall CPU/material released");check(((Map<?,?>)field(renderer,"pcTextureSizes")).isEmpty(),"all native textures released");
            result.putString("stream","PASS PC LIVE WALLS "+kind+" mode="+commandMode+" installed checks="+checks+"; "+(commandMode.equals("all")?4:1)+" normal commands; emulator only\n");
        }catch(Throwable e){result.putString("stream","FAIL PC LIVE WALLS "+kind+" "+android.util.Log.getStackTraceString(e));try{capture("pc-live-wall-"+kind+"-failed");}catch(Exception ignored){}}
        finally{
            if(activity!=null)runOnMainSync(()->activity.finish());
            try{if(backed){if(existed){Files.write(auto.toPath(),backup);if(!Arrays.equals(backup,Files.readAllBytes(auto.toPath())))throw new IOException("restored bytes differ");}else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"FAIL autosave restoration "+e);}
            restore(prefs,old);restore(client,oldClient);
        }
        try{Files.write(report.toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}finish(Activity.RESULT_OK,result);
    }
    private static int edges(MapSceneSnapshot s){int n=0;for(int mask:s.wallConnections.values())n+=Integer.bitCount(mask&56);return n;}
    private void verifyGPU(FilamentMapView renderer,MapSceneSnapshot s)throws Exception {
        int count=0;PcFacilities library=(PcFacilities)field(renderer,"pcFacilities");
        for(Object proxy:((Map<?,?>)field(renderer,"objects")).values()){
            var item=(MapSceneSnapshot.Item)field(proxy,"item");var p=PcConstructibleWalls.placement(s.ground,item);if(p==null)continue;count++;
            SceneMesh actual=(SceneMesh)field(field(proxy,"shape"),"source");SceneMesh expected=PcConstructibleWalls.mesh(library,s.ground,item,s.month,(Integer)field(renderer,"siteLod"),p,s.wallConnections.getOrDefault(item.hex,0));
            check(actual.pcWall&&actual.pcFacility&&!actual.pcCliffWall,"dynamic source wall uses original facility GPU material");check(Arrays.equals(actual.vertices,expected.vertices)&&Arrays.equals(actual.indices,expected.indices)&&Arrays.equals(actual.uv,expected.uv)&&Arrays.equals(actual.tangents,expected.tangents),"actual GPU source geometry matches updated connected state");
            check((Float)field(proxy,"y")==p.y,"source quarter-grid vertical placement");check((Float)field(field(proxy,"motion"),"x")==p.x&&(Float)field(field(proxy,"motion"),"z")==p.z,"source facility creation horizontal placement");
        }
        check(count==s.items.stream().filter(i->PcConstructibleWalls.placement(s.ground,i)!=null).count(),"no obsolete destroyed wall proxy or omitted live endpoint");
    }
    private void awaitAssets(FilamentMapView r)throws Exception{long end=android.os.SystemClock.uptimeMillis()+30000;while((Boolean)field(r,"assetSyncPending")&&android.os.SystemClock.uptimeMillis()<end)settle();check(!(Boolean)field(r,"assetSyncPending"),"source wall decode and GPU upload finished");}
    private void save(File dir,String name)throws Exception{surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"pc-live-wall-"+name+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);}
    private static void restore(android.content.SharedPreferences prefs,Map<String,?> old){var e=prefs.edit().clear();for(var r:old.entrySet()){Object v=r.getValue();String k=r.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));else throw new IllegalArgumentException(k);}e.commit();}
}

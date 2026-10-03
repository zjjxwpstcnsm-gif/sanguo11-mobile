package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Real production streaming of immutable original walls, not a debug renderer. */
public final class PcCliffWallsInstrumentation extends SceneInstrumentation {
    @Override public void onStart(){
        Bundle result=new Bundle();File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");byte[] backup=null;boolean existed=auto.isFile(),backed=false;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);Map<String,?> saved=new HashMap<>(prefs.getAll());
        var client=getTargetContext().getSharedPreferences("MainActivity",0);Map<String,?> savedClient=new HashMap<>(client.getAll());
        try{
            Files.deleteIfExists(new File(dir,"pc-cliff-walls-report.txt").toPath());if(existed)backup=Files.readAllBytes(auto.toPath());backed=true;
            World seed=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(seed);Files.write(auto.toPath(),before);prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);});settle();check(host.is3D(),"normal native renderer active");
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");PcCliffWalls library=(PcCliffWalls)field(renderer,"pcCliffWalls");
            check(library!=null&&library.placementCount()==161,"all original source map walls loaded");check(field(renderer,"pcFacilitiesInstance")!=null,"separate source wall texture instance loaded");
            check(library.connectionCount()==154,"source six-neighbor topology emits each connected edge once");
            MapSceneSnapshot initial=(MapSceneSnapshot)field(renderer,"snapshot");List<Hex> anchors=library.anchors(initial.ground);check(anchors.size()==161,"all original map transforms eligible");
            for(int n:new int[]{0,80,160})for(int month:new int[]{1,10}){
                World review=SaveCodec.decode(before);review.startMonth=month;Hex target=anchors.get(n);MapSceneSnapshot next=new MapSceneSnapshot(initial.ground,review,target,-1);
                runOnMainSync(()->{renderer.focus(target);renderer.camera.span=5;renderer.camera.yaw=0;renderer.camera.tilt=55;renderer.snapshot(next);});settle();ready();
                int cpu=0,gpu=0;for(SceneMesh m:(List<SceneMesh>)field(renderer,"woods")){
                    check(m.pcCliffWall||m.pcScenery,"source mode never introduces old approximation");
                    if(m.pcCliffWall){cpu++;check(m.pcFacility&&m.uv!=null&&m.distant.pcCliffWall,"original wall LOD/texture streams retained");}
                }
                for(Object key:((Map<?,?>)field(renderer,"vegetation")).keySet())if(((SceneMesh)key).pcCliffWall)gpu++;
                check(cpu>0&&gpu>0,"original map wall uploaded at normal camera anchor "+n);check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no missing source model fallback");
                surfaceCapture();String name="pc-cliff-walls-anchor"+n+"-month"+month;Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(name+"-ui");
                Files.write(new File(dir,"pc-cliff-walls-report.txt").toPath(),(name+" cpu="+cpu+" gpu="+gpu+"\n"+host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            runOnMainSync(()->{renderer.camera.x=99.5f;renderer.camera.z=99.75f;renderer.camera.span=50;renderer.camera.yaw=35;});settle();ready();capture("pc-cliff-walls-national-far");
            runOnMainSync(()->callActivityOnPause(activity));check(!(Boolean)field(renderer,"resumed"),"pause frame gate");runOnMainSync(()->callActivityOnResume(activity));settle();ready();
            byte[][] actual={null};runOnMainSync(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});check(Arrays.equals(before,actual[0]),"wall/quarter/LOD/camera streaming preserves entire authority/RNG/save");
            runOnMainSync(()->host.switchMode(false));check(field(renderer,"pcCliffWalls")==null&&field(renderer,"pcFacilitiesInstance")==null,"wall CPU and source material instance released on exit");check(((Map<?,?>)field(renderer,"pcTextureSizes")).isEmpty(),"all source textures released");
            result.putString("stream","PASS PC CLIFF WALLS installed checks="+checks+"; original static placement integration; emulator evidence only\n");
        }catch(Throwable e){result.putString("stream","FAIL PC CLIFF WALLS "+android.util.Log.getStackTraceString(e));try{capture("pc-cliff-walls-failed");}catch(Exception ignored){}}
        finally{
            if(activity!=null)runOnMainSync(()->activity.finish());
            try{if(backed){if(existed){Files.write(auto.toPath(),backup);if(!Arrays.equals(backup,Files.readAllBytes(auto.toPath())))throw new IOException("restored bytes differ");}else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"FAIL autosave restoration "+e);}
            restore(prefs,saved);restore(client,savedClient);
        }
        try{Files.write(new File(dir,"pc-cliff-walls-report.txt").toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}
        finish(Activity.RESULT_OK,result);
    }
    private static void restore(android.content.SharedPreferences prefs,Map<String,?> saved){
        var e=prefs.edit().clear();for(var row:saved.entrySet()){String k=row.getKey();Object v=row.getValue();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));else throw new IllegalArgumentException("Unsupported preference "+k);}e.commit();
    }
}

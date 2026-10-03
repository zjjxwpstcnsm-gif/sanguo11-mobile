package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed source forests, seasonal UV/mesh changes, LODs and scene release.
 * This is renderer integration evidence; it is not a PC image comparison. */
public final class PcSceneryInstrumentation extends SceneInstrumentation {
    private boolean farOnly;
    private long lastLoadingLog;
    @Override public void onCreate(Bundle args){farOnly=args!=null&&"true".equals(args.getString("farOnly"));super.onCreate(args);}
    @Override void observeLoading(FilamentMapView view)throws Exception {
        long now=android.os.SystemClock.uptimeMillis();
        if(now-lastLoadingLog<1000)return;lastLoadingLog=now;
        android.util.Log.i("SceneAcceptance","SCENERY_LOADING "+view.startupReport());
    }
    @Override public void onStart(){
        Bundle result=new Bundle();File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");
        byte[] backup=null;boolean existed=auto.isFile(),backupReady=false;
        android.content.SharedPreferences prefs=getTargetContext().getSharedPreferences("map-renderer",0);
        Map<String,?> oldPrefs=new HashMap<>(prefs.getAll());
        android.content.SharedPreferences client=activityPreferences();
        Map<String,?> oldClient=new HashMap<>(client.getAll());
        try{
            Files.deleteIfExists(new File(dir,"pc-scenery-report.txt").toPath());
            if(existed)backup=Files.readAllBytes(auto.toPath());backupReady=true;
            World seed=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(seed);
            Files.write(auto.toPath(),before);prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            host=(MapHost)field(activity,"map");world=SessionProbe.view(activity);
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);});settle();
            check(host.is3D(),"production 3D initialized with source RGBA material");
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");
            int[][] places={{81,79},{177,18},{108,119}};String[] names={"luoyang-forest","northeast-forest","jingzhou-forest"};
            for(int i=0;i<places.length;i++){
                if(farOnly&&i!=places.length-1)continue;
                Hex h=MapCoordinates.fromNationalSource(world,new SourceGridCoord(places[i][0],places[i][1]));
                for(int month:new int[]{1,4,7,10}){
                    if(farOnly&&month!=10)continue;
                    World review=SaveCodec.decode(before);review.startMonth=month;
                    MapSceneSnapshot old=(MapSceneSnapshot)field(renderer,"snapshot");
                    MapSceneSnapshot snapshot=new MapSceneSnapshot(old.ground,review,h,-1);
                    runOnMainSync(()->{renderer.focus(h);renderer.camera.span=8;renderer.camera.yaw=0;renderer.camera.tilt=55;renderer.camera.facing=1;renderer.snapshot(snapshot);});settle();ready();
                    check(field(renderer,"pcSceneryMaterial")!=null&&field(renderer,"pcSceneryAtlas")!=null,"source material and RGBA atlas resident");
                    check(field(renderer,"pcPaint")!=null&&field(renderer,"pcEnvironment")!=null,"original resource4806 painting and SENV are resident");
                    check((Integer)field(renderer,"pcLightMonth")==month,"source SENV light follows the displayed month");
                    check(field(renderer,"pcPaletteSizes")!=null,"original ground dimension table resident");
                    check(field(renderer,"pcGroundNormal")!=null&&field(renderer,"pcGroundPaint")!=null,"original raw normal and RGBA ground paint resident");
                    check(field(renderer,"pcGroundOutlineMaterial")!=null&&field(renderer,"pcGroundOutline")!=null,"original shared-geometry outline resident");
                    check((Integer)field(renderer,"pcFogMonth")==month,"source c26/c27 tracks displayed month");
                    int quarter=month==1?1:month==4?2:month==7?0:3;
                    check((Integer)field(renderer,"pcSeason")==quarter,"ground palette and numeric dimensions use source archive quarter");
                    List<SceneMesh> woods=(List<SceneMesh>)field(renderer,"woods");
                    check(!woods.isEmpty(),"source forest streamed at "+names[i]);
                    for(SceneMesh mesh:woods){check(mesh.pcScenery||mesh.pcCliffWall,"no legacy scenery mixed into PC chunks");check(mesh.distant.pcScenery||mesh.distant.pcCliffWall,"far LOD also source geometry");}
                    check(((Map<?,?>)field(renderer,"vegetation")).size()>0,"source scenery actually uploaded to GPU");
                    check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no missing source or compatibility assets");
                    surfaceCapture();String name="pc-source-"+names[i]+"-month"+month;
                    Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(name+"-ui");
                    Files.write(new File(dir,"pc-scenery-report.txt").toPath(),(name+"\n"+host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                }
            }
            runOnMainSync(()->{renderer.camera.span=50;renderer.camera.yaw=35;});settle();ready();surfaceCapture();capture("pc-source-forest-far");
            runOnMainSync(()->callActivityOnPause(activity));
            check(!(Boolean)field(renderer,"resumed"),"activity pause closes production frame gate");
            runOnMainSync(()->callActivityOnResume(activity));settle();ready();
            check((Boolean)field(renderer,"resumed"),"activity resume restores production frame gate");
            check(field(renderer,"pcSceneryAtlas")!=null,"source texture survives pause/resume");
            byte[][] after={null};runOnMainSync(()->{try{after[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
            check(Arrays.equals(before,after[0]),"season/LOD/camera review leaves complete authority and RNG unchanged");
            runOnMainSync(()->host.switchMode(false));
            check(field(renderer,"pcSceneryMaterial")==null&&field(renderer,"pcSceneryAtlas")==null,"source scenery GPU resources released on scene exit");
            check(field(renderer,"pcPaint")==null&&field(renderer,"pcEnvironment")==null,"painting texture and SENV released on scene exit");
            check(field(renderer,"pcGroundNormal")==null&&field(renderer,"pcGroundPaint")==null&&field(renderer,"pcGroundOutline")==null&&field(renderer,"pcGroundOutlineMaterial")==null,"original ground pass owners released");
            check(field(renderer,"pcPaletteSizes")==null,"ground dimension texture released on scene exit");
            check(((Map<?,?>)field(renderer,"pcTextureSizes")).isEmpty(),"all tracked source textures released");
            result.putString("stream","PASS PC SCENERY installed checks="+checks+" mode="+(farOnly?"far-diagnostic":"all12-seasons")+"; emulator renderer evidence only\n");
        }catch(Throwable e){result.putString("stream","FAIL PC SCENERY "+android.util.Log.getStackTraceString(e));try{capture("pc-scenery-failed");}catch(Exception ignored){}}
        finally{
            try{finishActivityForRestore();}catch(Throwable e){result.putString("stream",result.getString("stream")+"\nFAIL lifecycle restoration barrier "+e);}
            try{if(backupReady){if(existed)Files.write(auto.toPath(),backup);else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"\nFAIL autosave restoration "+e);}
            restorePreferences(prefs,oldPrefs);restorePreferences(client,oldClient);
        }
        try{Files.write(new File(dir,"pc-scenery-report.txt").toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}
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

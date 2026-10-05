package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed source transforms, real attacks/flood rules, exact authority and lifecycle. */
public final class PcDamsInstrumentation extends SceneInstrumentation {
    private int index=-1;private String mode="all";
    @Override public void onCreate(Bundle args){if(args!=null){index=Integer.parseInt(args.getString("damIndex","-1"));mode=args.getString("commandMode","all");}super.onCreate(args);}
    @Override public void onStart(){
        Bundle result=new Bundle();File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();File report=new File(dir,"pc-dams-"+index+"-"+mode+"-report.txt");
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");byte[] backup=null;boolean existed=auto.isFile(),backed=false;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);Map<String,?> old=new HashMap<>(prefs.getAll());
        var client=getTargetContext().getSharedPreferences("MainActivity",0);Map<String,?> oldClient=new HashMap<>(client.getAll());
        try{
            check(index>=-1&&index<4&&Arrays.asList("all","damage","destroy").contains(mode),"explicit source dam command scope");Files.deleteIfExists(report.toPath());
            if(existed){backup=Files.readAllBytes(auto.toPath());World restored=SaveCodec.decode(backup);check(Arrays.equals(backup,SaveCodec.encode(restored)),"actual Android autosave byte-exact load with no source dam seeding");if(restored.mapRevision==64)check(new MapSceneSnapshot.Ground(restored).pcMap!=null,"real revision64 autosave keeps original PC ground");}
            backed=true;Files.write(auto.toPath(),SaveCodec.encode(ScenarioCatalog.load("heroes-250",0)));prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);});settle();check(host.is3D(),"production renderer");FilamentMapView renderer=(FilamentMapView)field(host,"spatial");
            for(int n=0;n<4;n++)for(String commandMode:new String[]{"damage","destroy"}){
                if(index>=0&&index!=n||!mode.equals("all")&&!mode.equals(commandMode))continue;
                PcDamsFixture.Case prepared=PcDamsFixture.prepare(n,commandMode);World expected=SaveCodec.decode(SaveCodec.encode(prepared.world()));World.Result expectedResult=prepared.command(expected);check(expectedResult.ok,"independent legal dam reference command");
                runOnMainSync(()->{SessionProbe.install(activity,prepared.world());activity.refresh();host.switchMode(true);host.focus(prepared.target());renderer.camera.span=3;renderer.camera.yaw=0;renderer.camera.tilt=55;});settle();ready();awaitAssets(renderer);
                MapSceneSnapshot prior=(MapSceneSnapshot)field(renderer,"snapshot");verifyGPU(renderer,prior,4);save(dir,n+"-"+commandMode+"-before");
                World.Result[] command={null};runOnMainSync(()->{command[0]=SessionProbe.command(activity,prepared::command);activity.refresh();});check(command[0].ok,"normal production dam attack "+n+"/"+commandMode);
                long end=android.os.SystemClock.uptimeMillis()+20000;while(host.commandEffectsActive()&&android.os.SystemClock.uptimeMillis()<end)settle();check(!host.commandEffectsActive(),"normal dam command replay finished");settle();ready();awaitAssets(renderer);
                MapSceneSnapshot next=(MapSceneSnapshot)field(renderer,"snapshot");verifyGPU(renderer,next,commandMode.equals("destroy")?3:4);
                byte[][] actual={null};runOnMainSync(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});check(Arrays.equals(SaveCodec.encode(expected),actual[0]),"entire dam attack authority/RNG/save matches independent normal command");
                World saved=SaveCodec.decode(actual[0]);check(Arrays.equals(actual[0],SaveCodec.encode(saved)),"actual post-attack save byte-exact roundtrip");
                if(commandMode.equals("destroy")){check(saved.war.at(prepared.target())==null,"source dam does not respawn when actual save is decoded");check(saved.unit(prepared.ally()).troops==4400&&saved.unit(prepared.enemy()).troops==4400,"normal flood applies exact600 to both allegiances");}
                else check(saved.war.at(prepared.target()).hp>0&&saved.war.at(prepared.target()).hp<500,"normal attack binds original damaged body state");
                check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no placeholder/missing native dam assets");save(dir,n+"-"+commandMode+"-after");capture("pc-dam-"+n+"-"+commandMode+"-after-ui");
                Files.write(report.toPath(),("NORMAL_DAM index="+n+" mode="+commandMode+" "+command[0].message+"\n"+host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            runOnMainSync(()->{renderer.camera.span=50;renderer.camera.yaw=35;});settle();ready();capture("pc-dams-"+index+"-far");
            MapSceneSnapshot far=(MapSceneSnapshot)field(renderer,"snapshot");verifyGPU(renderer,far,mode.equals("damage")?4:3);
            runOnMainSync(()->callActivityOnPause(activity));check(!(Boolean)field(renderer,"resumed"),"pause stops frame gate");runOnMainSync(()->callActivityOnResume(activity));settle();ready();
            runOnMainSync(()->host.switchMode(false));check(field(renderer,"pcDams")==null&&field(renderer,"pcFacilitiesInstance")==null,"source placement and GPU material released");check(((Map<?,?>)field(renderer,"pcTextureSizes")).isEmpty(),"native textures released");
            result.putString("stream","PASS PC DAMS installed checks="+checks+" index="+index+" mode="+mode+"; normal damage/destruction/flood rules; emulator only; original flood/collapse FX pending\n");
        }catch(Throwable e){result.putString("stream","FAIL PC DAMS "+android.util.Log.getStackTraceString(e));try{capture("pc-dams-failed");}catch(Exception ignored){}}
        finally{
            if(activity!=null)runOnMainSync(()->activity.finish());
            try{if(backed){if(existed){Files.write(auto.toPath(),backup);if(!Arrays.equals(backup,Files.readAllBytes(auto.toPath())))throw new IOException("restored bytes differ");}else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"FAIL autosave restoration "+e);}
            restore(prefs,old);restore(client,oldClient);
        }
        try{Files.write(report.toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}finish(Activity.RESULT_OK,result);
    }
    private void verifyGPU(FilamentMapView renderer,MapSceneSnapshot s,int expectedCount)throws Exception{
        int count=0;PcDams placements=(PcDams)field(renderer,"pcDams");PcFacilities library=(PcFacilities)field(renderer,"pcFacilities");
        for(Object proxy:((Map<?,?>)field(renderer,"objects")).values()){
            var item=(MapSceneSnapshot.Item)field(proxy,"item");var p=placements.placement(s.ground,item);if(p==null)continue;count++;
            SceneMesh actual=(SceneMesh)field(field(proxy,"shape"),"source");SceneMesh expected=library.mesh(item,s.month,(Integer)field(renderer,"siteLod"));
            check(actual.pcDam&&actual.pcFacility&&!actual.pcWall&&!actual.pcCliffWall,"authored live dam source material/tag");
            if(item.facility.owner<0)check((Integer)field(proxy,"flag")==0,"neutral source dam carries no invented faction flag");
            check(Arrays.equals(actual.vertices,expected.vertices)&&Arrays.equals(actual.indices,expected.indices)&&Arrays.equals(actual.uv,expected.uv)&&Arrays.equals(actual.tangents,expected.tangents),"actual GPU buffers retain exact source dam state/LOD geometry");
            check((Float)field(proxy,"y")==p.y,"original authored dam height rather than inferred terrain height");
            check((Float)field(field(proxy,"motion"),"x")==p.x-s.ground.sourceOriginX&&(Float)field(field(proxy,"motion"),"z")==p.z-s.ground.sourceOriginY,"source half-grid horizontal placement");
            check((Float)field(proxy,"positionedYaw")==p.yaw,"original source yaw used by production root transform");
            float[][] transform={null};com.google.android.filament.Engine engine=(com.google.android.filament.Engine)field(renderer,"engine");int entity=(Integer)field(proxy,"entity");runOnMainSync(()->{
                var tm=engine.getTransformManager();transform[0]=tm.getTransform(tm.getInstance(entity),new float[16]);
            });
            float c=(float)Math.cos(p.yaw),sin=(float)Math.sin(p.yaw);float[] authored={c,0,-sin,0,0,1,0,0,sin,0,c,0,p.x-s.ground.sourceOriginX,p.y,p.z-s.ground.sourceOriginY,1};
            for(int j=0;j<16;j++)check(Math.abs(transform[0][j]-authored[j])<1e-6f,"actual GPU model matrix matches original yaw/scale/position element"+j);
        }
        check(count==expectedCount&&count==s.items.stream().filter(i->placements.placement(s.ground,i)!=null).count(),"all live dams exactly once; no destroyed scenery duplicate");
    }
    private void awaitAssets(FilamentMapView r)throws Exception{long end=android.os.SystemClock.uptimeMillis()+30000;while((Boolean)field(r,"assetSyncPending")&&android.os.SystemClock.uptimeMillis()<end)settle();check(!(Boolean)field(r,"assetSyncPending"),"source dam asset upload complete");}
    private void save(File dir,String name)throws Exception{surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"pc-dam-"+name+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);}
    private static void restore(android.content.SharedPreferences prefs,Map<String,?> old){var e=prefs.edit().clear();for(var r:old.entrySet()){Object v=r.getValue();String k=r.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));else throw new IllegalArgumentException(k);}e.commit();}
}

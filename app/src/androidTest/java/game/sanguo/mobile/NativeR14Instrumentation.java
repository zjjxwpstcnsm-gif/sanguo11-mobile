package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed game path. Calendar variants are labelled saved-date fixtures, not played years. */
public final class NativeR14Instrumentation extends SceneInstrumentation {
    private File dir;private byte[] original;
    interface Ui {void run()throws Exception;}
    void ui(Ui task){runOnMainSync(()->{try{task.run();}catch(Exception e){throw new RuntimeException(e);}});}
    FilamentMapView view()throws Exception{return (FilamentMapView)field(host,"spatial");}
    void text(String name,String content)throws Exception{Files.write(new File(dir,name).toPath(),content.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    void queues()throws Exception{
        ready();long end=SystemClock.uptimeMillis()+120000;boolean[] done={false};
        while(SystemClock.uptimeMillis()<end){ui(()->{FilamentMapView v=view();done[0]=!(Boolean)field(v,"assetSyncPending")&&((SceneAssetQueue)field(v,"assetWork")).pending()==0&&((SceneWorkQueue<?>)field(v,"meshWork")).pending()==0;});if(done[0])break;settle();}
        check(done[0],"all visible scene work settles");
        ui(()->check(((Set<?>)field(view(),"missingAssets")).isEmpty(),"no missing assets"));
    }
    void camera(float span,float yaw)throws Exception{ui(()->{FilamentMapView v=view();v.center(new Hex(134,82));v.camera.span=span;v.camera.yaw=yaw;v.camera.tilt=48;});settle();queues();}
    void shot(String name)throws Exception{
        queues();surfaceCapture();capture(name+"-ui");
        Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);
        text(name+".txt","source="+BuildConfig.SOURCE_REVISION+"\ncalendar=saved-date fixture on official coalition-190 map\nPC exact season reference=REFERENCE_MISSING\nquality="+host.quality()+" grid="+host.gridShown()+"\n"+host.report());
    }
    void loadMonth(int month)throws Exception{
        World fixture=SaveCodec.decode(original);fixture.startMonth=month;
        byte[] saved=SaveCodec.encode(fixture);World loaded=SaveCodec.decode(saved);
        ui(()->{SessionProbe.install(activity,loaded);activity.refresh();world=SessionProbe.view(activity);});
        queues();
        ui(()->{check(field(view(),"season")==SeasonStyle.forMonth(month),"normal activate/load refresh selects correct GPU season");check(((MapSceneSnapshot)field(view(),"snapshot")).month==month,"projected calendar");});
        check(Arrays.equals(saved,SaveCodec.encode(world)),"installed load/projection leaves complete save/RNG unchanged");
    }
    void uniformCycles()throws Exception{
        queues();FilamentMapView v=view();Object[] ground={null},textures={null},meshes={null};long[] bytes={0};int[] generation={0},count={0};
        ui(()->{ground[0]=field(v,"snapshot");textures[0]=new ArrayList<>((List<?>)field(v,"groundTextures"));meshes[0]=new HashMap<>((Map<?,?>)field(v,"terrain"));bytes[0]=(Long)field(v,"textureBytes");generation[0]=(Integer)field(v,"generation");count[0]=(Integer)field(v,"seasonUpdates");});
        byte[] authority=SaveCodec.encode(world);MapSceneSnapshot current=(MapSceneSnapshot)ground[0];
        // Display-only projection fixtures. Never install these dates into GameSession.
        for(int i=0;i<20;i++){
            World detached=SaveCodec.decode(authority);detached.startMonth=1+(i%4)*3;
            MapSceneSnapshot next=new MapSceneSnapshot(current.ground,detached,current.selected,-1);
            ui(()->v.snapshot(next));settle();
        }
        ui(()->{v.snapshot(current);check(((List<?>)textures[0]).equals(field(v,"groundTextures")),"season keeps exact texture handles");check(((Map<?,?>)meshes[0]).equals(field(v,"terrain")),"season keeps exact terrain GPU handles");check(bytes[0]==(Long)field(v,"textureBytes"),"no repeated texture upload bytes");check(generation[0]==(Integer)field(v,"generation"),"season does not rebuild ground/vegetation");check((Integer)field(v,"seasonUpdates")>count[0]+15,"actual repeated season application");});
        check(Arrays.equals(authority,SaveCodec.encode(SessionProbe.view(activity))),"display test never writes authority");
        text("r14-resource-cycles.txt",host.report());
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        ui(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",5));
        long end=SystemClock.uptimeMillis()+120000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<end)settle();
        host=(MapHost)field(activity,"map");ui(()->{world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.MEDIUM);host.setGridShown(false);host.setTerritoryMode(0);invoke("closePanel",new Class<?>[0]);});
        original=SaveCodec.encode(world);camera(12,0);
        for(int month:new int[]{1,4,7,10}){
            loadMonth(month);
            for(float span:new float[]{5,12,22})for(boolean grid:new boolean[]{false,true}){
                ui(()->host.setGridShown(grid));camera(span,0);shot("r14-m"+month+"-span"+(int)span+"-grid"+grid);
            }
            ui(()->host.setGridShown(false));camera(12,90);shot("r14-m"+month+"-yaw90");
        }
        camera(12,0);uniformCycles();
        for(SceneQuality quality:new SceneQuality[]{SceneQuality.LOW,SceneQuality.HIGH}){
            ui(()->host.quality(quality));camera(12,0);
            for(int month:new int[]{1,4,7,10}){loadMonth(month);shot("r14-"+quality+"-m"+month);}
        }
        ui(()->host.quality(SceneQuality.MEDIUM));camera(12,0);
        World edge=SaveCodec.decode(original);edge.turn=8;ui(()->{SessionProbe.install(activity,edge);activity.refresh();world=SessionProbe.view(activity);});
        check(SeasonStyle.fromDate(world.startMonth,world.turn)==SeasonStyle.SPRING,"March lower period fixture");
        commandFlow();
        ui(()->{world=SessionProbe.view(activity);check(world.turn==9,"real normal turn crosses quarter");check(field(view(),"season")==SeasonStyle.SUMMER,"real turn and autosave load apply summer");});
        shot("r14-real-quarter-turn-autosave-load");text("r14-final.txt",host.report());
        result.putString("stream","PASS R14 installed checks="+checks+"; saved-date fixtures labelled; physical/PC art NOT_RUN\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{text("r14-failure.txt",android.util.Log.getStackTraceString(e));}catch(Exception ignored){}result.putString("stream","FAIL R14 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

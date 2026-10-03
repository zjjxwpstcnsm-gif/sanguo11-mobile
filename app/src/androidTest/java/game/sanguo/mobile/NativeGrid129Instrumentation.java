package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Actual MainActivity/session, research completion, retained host and GPU grids.
 * Explicit small fixture, real Surface captures; not physical-device performance evidence. */
public final class NativeGrid129Instrumentation extends SceneInstrumentation {
    private File dir;
    private long transitionFrame=Long.MAX_VALUE;
    private MapSceneSnapshot.Ground observedGround;
    private void log(String s)throws Exception{Files.write(new File(dir,"grid129.txt").toPath(),(s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private byte[] authority(){byte[][] value={null};runOnMainSync(()->{try{value[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return value[0];}
    private void gpuGrid(FilamentMapView view)throws Exception{
        MapSceneSnapshot current=(MapSceneSnapshot)field(view,"snapshot");
        int shown=0;for(var entry:((Map<?,?>)field(view,"gridMeshes")).entrySet())if((Boolean)field(entry.getValue(),"shown")){
            shown++;check(((SceneMesh)entry.getKey()).gridMatches(current.ground),"admitted GPU never displays obsolete research grid");
        }
    }
    @Override void observeLoading(FilamentMapView view)throws Exception{
        runOnMainSync(()->{try{
            MapSceneSnapshot.Ground ground=((MapSceneSnapshot)field(view,"snapshot")).ground;
            long frames=(Long)field(view,"surfaceFrames");
            if(observedGround!=ground){observedGround=ground;transitionFrame=frames;}
            else if(frames>transitionFrame)gpuGrid(view);
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private void phase(String name,int force,boolean researched)throws Exception{
        ready();FilamentMapView view=(FilamentMapView)field(host,"spatial");byte[] before=authority();
        runOnMainSync(()->{try{
            MapSceneSnapshot snapshot=(MapSceneSnapshot)field(view,"snapshot");
            check(snapshot.ground.gridForce==force&&snapshot.ground.gridDifficultMarch==researched,"installed force/research projection: "+name);
            for(Hex h:new Hex[]{new Hex(6,7),new Hex(8,7),new Hex(10,7)})check(snapshot.ground.gridCell(h,false)==researched,"installed hard-terrain grid: "+name+" "+h);
            for(Hex h:new Hex[]{new Hex(12,7),new Hex(14,7)})check(!snapshot.ground.gridCell(h,false),"permanent blockers remain grid-free");
            check(snapshot.ground.gridCell(new Hex(6,9),false)&&snapshot.ground.gridCell(new Hex(8,9),false),"poison/naval potential cells remain gridded");
            for(Hex h:new Hex[]{new Hex(6,7),new Hex(8,7),new Hex(10,7),new Hex(12,7),new Hex(14,7)})check(snapshot.ground.gridCell(h,true),"editor inspection still exposes all valid terrain");
            gpuGrid(view);check(!((Map<?,?>)field(view,"gridMeshes")).isEmpty(),"real grid GPU batches uploaded");
        }catch(Exception e){throw new RuntimeException(e);}});
        surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(name+"-ui");
        check(Arrays.equals(before,authority()),"grid observation/capture preserves complete authority/RNG");log(name+"\n"+host.report());
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();World seed=NativeGrid129Fixture.researchReadyWorld();
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).putString("quality","MEDIUM").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
        runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.setGridShown(true);host.setTerritoryMode(0);host.focusNative(new Hex(10,8));});settle();ready();
        FilamentMapView view=(FilamentMapView)field(host,"spatial");MapSceneSnapshot.Ground old=((MapSceneSnapshot)field(view,"snapshot")).ground;
        phase("01-before-research",0,false);
        World expected=SaveCodec.decode(authority());check(expected.nextTurn().ok,"headless real next-turn completes staged research");
        check(expected.campaign.has(0,Campaign.Tech.DIFFICULT_MARCH),"headless research completion reached");
        runOnMainSync(()->{try{transitionFrame=(Long)field(view,"surfaceFrames");}catch(Exception e){throw new RuntimeException(e);}invoke("advanceTurn",new Class<?>[0]);});
        long deadline=SystemClock.uptimeMillis()+120000;while((Boolean)field(activity,"aiRunning")&&SystemClock.uptimeMillis()<deadline)settle();
        check(!(Boolean)field(activity,"aiRunning"),"installed real turn/research playback completed");runOnMainSync(()->{});ready();
        check(Arrays.equals(SaveCodec.encode(expected),authority()),"installed actual turn/research result equals complete headless state/RNG");
        check(old.surface==((MapSceneSnapshot)field(view,"snapshot")).ground.surface,"research reuses unchanged terrain surface through MapHost");
        phase("02-after-research",0,true);
        World switched=SaveCodec.decode(authority());switched.player=1;switched.active=1;
        runOnMainSync(()->{try{transitionFrame=(Long)field(view,"surfaceFrames");}catch(Exception e){throw new RuntimeException(e);}SessionProbe.install(activity,switched);activity.refresh();host.center(new Hex(10,8));});
        check(host.is3D()&&view==field(host,"spatial"),"player replacement retains same native host");phase("03-unresearched-player",1,false);
        byte[] authorityBeforePreview=authority();
        runOnMainSync(()->{try{transitionFrame=(Long)field(view,"surfaceFrames");}catch(Exception e){throw new RuntimeException(e);}host.previewMode();host.setPreviewFaction(0);});phase("04-researched-preview",0,true);
        runOnMainSync(()->{try{transitionFrame=(Long)field(view,"surfaceFrames");}catch(Exception e){throw new RuntimeException(e);}host.setPreviewFaction(1);});phase("05-unresearched-preview",1,false);
        check(Arrays.equals(authorityBeforePreview,authority()),"preview-force switching cannot change player, technology or RNG");
        runOnMainSync(()->host.setGridShown(false));settle();ready();
        runOnMainSync(()->{try{for(Object gpu:((Map<?,?>)field(view,"gridMeshes")).values())check(!(Boolean)field(gpu,"shown"),"grid toggle removes all actual GPU grids");}catch(Exception e){throw new RuntimeException(e);}});
        runOnMainSync(()->host.setGridShown(true));phase("06-grid-toggle-restored",1,false);
        Files.write(new File(dir,"grid129-final.sg11").toPath(),authority());
        log("PASS GRID129 checks="+checks+" actual next-turn research, player and preview switches, retained terrain, GPU stale rejection, exact save/RNG");
        result.putString("stream","PASS GRID129 checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL GRID129 "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture("grid129-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

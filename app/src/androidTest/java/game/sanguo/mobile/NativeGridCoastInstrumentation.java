package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Small explicit coast fixture restored by normal MainActivity/MapHost. This is
 * local visual evidence, never a nationwide, full-touch or performance claim. */
public final class NativeGridCoastInstrumentation extends SceneInstrumentation {
    private String phase="candidate",source="",mode="fixture";
    private File dir;
    @Override public void onCreate(Bundle b){if(b!=null){phase=b.getString("phase",phase);source=b.getString("source",source);mode=b.getString("mode",mode);}super.onCreate(b);}
    private byte[] authority(){byte[][] out={null};runOnMainSync(()->{try{out[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return out[0];}
    private void log(String line)throws Exception{Files.write(new File(dir,"grid-coast.txt").toPath(),(line+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private void shot(FilamentMapView view,String name)throws Exception{
        ready();runOnMainSync(()->view.resume(false));
        try{surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(name+"-ui");}
        finally{runOnMainSync(()->view.resume(true));}
        log(name+"\n"+host.report());
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        World seed=mode.equals("national")?ScenarioCatalog.all().get(0):NativeR11Fixture.world("counter");
        if(!mode.equals("national")){
            seed.scenarioName="Grid/coast explicit 22x16 visual fixture";
            for(int r=2;r<=10;r++)for(int q=11;q<seed.width;q++){
                if(q>=13+(r/3%2)||r==5)seed.terrain[q][r]=World.Terrain.SEA;
            }
            seed.terrain[17][6]=World.Terrain.PLAIN;seed.terrain[20][2]=World.Terrain.NON_NAVIGABLE_WATER;
            for(int q=10;q<=14;q++)for(int r=11;r<=13;r++)seed.terrain[q][r]=World.Terrain.MOUNTAIN;
            seed.terrain[12][12]=World.Terrain.MOUNTAIN_PATH;
        }
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).putString("quality","MEDIUM").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");check(host!=null,"normal save restore creates host");
        byte[] before=authority();runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.setGridShown(false);host.setTerritoryMode(0);host.switchMode(true);});settle();ready();
        FilamentMapView view=(FilamentMapView)field(host,"spatial");
        Hex focus=mode.equals("national")?seed.cities.stream().filter(c->c.kind==World.SiteKind.PORT).findFirst().get().hex:new Hex(13,8);
        log("phase="+phase+" source="+source+" mode="+mode+" quality=MEDIUM normal MainActivity; camera API; original 120s ready");
        for(float span:new float[]{4,8,16})for(float yaw:new float[]{0,90}){
            runOnMainSync(()->{view.center(focus);view.camera.span=span;view.camera.yaw=yaw;view.camera.tilt=55;});settle();ready();
            for(boolean grid:new boolean[]{false,true}){
                runOnMainSync(()->host.setGridShown(grid));settle();
                shot(view,"grid-coast-"+phase+"-s"+(int)span+"-yaw"+(int)yaw+"-grid"+grid);
            }
        }
        MapSceneSnapshot snap=(MapSceneSnapshot)field(view,"snapshot");
        if(phase.equals("candidate")){
            for(int r=0;r<seed.height;r++)for(int q=0;q<seed.width;q++){
                Hex h=new Hex(q,r);if(!snap.ground.valid(h))continue;
                boolean expected=seed.terrain[q][r]!=World.Terrain.MOUNTAIN&&seed.terrain[q][r]!=World.Terrain.NON_NAVIGABLE_WATER&&!NationalMap.restricted(seed,h)&&(seed.campaign.has(seed.player,Campaign.Tech.DIFFICULT_MARCH)||!Fieldworks.requiresDifficultMarch(seed.terrain[q][r]));
                check(snap.ground.gridCell(h,false)==expected,"installed grid membership follows permanent and current-force technology restrictions");
                check(snap.ground.gridCell(h,true),"editor still sees blocked valid cells");
            }
        }
        check(Arrays.equals(before,authority()),"all camera/grid operations preserve complete authority/RNG");
        Files.write(new File(dir,"expected.sg11").toPath(),before);Files.write(new File(dir,"observed.sg11").toPath(),authority());
        log("PASS GRID_COAST checks="+checks);result.putString("stream","PASS GRID_COAST phase="+phase+" mode="+mode+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL GRID_COAST "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture("grid-coast-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.MotionEvent;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Actual normal-game material comparison: official map, no replaced map or preview Activity. */
public final class NativeR05Instrumentation extends SceneInstrumentation {
    private FilamentMapView spatial()throws Exception{return (FilamentMapView)field(host,"spatial");}
    private void shot(String name)throws Exception{
        // ready() alone can see an old window before the paced frame requests its replacement.
        settle();settle();ready();
        long end=SystemClock.uptimeMillis()+120000;
        while(host.report().contains("worker_pending=1")&&SystemClock.uptimeMillis()<end){settle();}
        ready();surfaceCapture();capture(name+"-ui");
        File dir=getTargetContext().getExternalFilesDir("s01");
        java.nio.file.Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        FilamentMapView v=spatial();
        check(field(v,"waterMaterial")!=null,"dedicated opaque water material exists");
        check(field(v,"groundMaterial")!=null,"dedicated lit ground material exists; no unlit fallback");
        try(FileWriter out=new FileWriter(new File(dir,name+".txt"))){out.write("source="+BuildConfig.SOURCE_REVISION+"\nscenario=coalition-190\ndate="+world.date()+"\ncamera="+v.camera.x+","+v.camera.z+","+v.camera.span+","+v.camera.yaw+","+v.camera.tilt+"\n"+host.report());}
    }
 private static World boatFixture(){
 World w=new World(24,14,"甲军","乙军");
 w.cities.add(new World.City(10,"甲城",new Hex(1,1),0));w.cities.add(new World.City(20,"乙城",new Hex(20,10),1));
 for(int[] a:new int[][]{{30,8,7},{31,11,5}}){World.City p=new World.City(a[0],"测试港"+a[0],new Hex(a[1],a[2]),0);p.kind=World.SiteKind.PORT;w.cities.add(p);}
 for(int q=9;q<=10;q++)for(int r=0;r<14;r++)w.terrain[q][r]=World.Terrain.WATER;
 World.Officer a=new World.Officer(0,"我将",0,-1,90,90,90,90,90);a.role=Strategy.Role.RULER;a.loyalty=100;a.unitId=1;Arrays.fill(a.aptitude,3);w.officers.add(a);
 World.Officer b=new World.Officer(1,"敌将",1,20,50,50,60,50,50);b.role=Strategy.Role.RULER;b.loyalty=100;w.officers.add(b);
 w.units.add(new World.Unit(1,0,0,World.Weapon.SPEAR,new Hex(8,6),8000,30000));w.nextUnitId=2;return w;
 }
    private void boatCommands()throws Exception{
        // Explicit synthetic navigation fixture, separate from official-map art captures.
        // Only the save setup is synthetic; movement/embark/enter use real GameSession commands.
        World prepared=SaveCodec.decode(SaveCodec.encode(boatFixture()));
        runOnMainSync(()->{SessionProbe.install(activity,prepared);activity.refresh();world=SessionProbe.view(activity);});
        World reference=SaveCodec.decode(SaveCodec.encode(world));FilamentMapView v=spatial();
        runOnMainSync(()->{v.center(new Hex(9,6));v.camera.span=5;v.camera.yaw=0;v.setGridShown(true);});shot("r05-fixture-boat-land-start");
        for(Hex h:new Hex[]{new Hex(9,6),new Hex(10,5)}){
            check(reference.move(1,h).ok,"reference legal embark/bend");
            runOnMainSync(()->{check(SessionProbe.command(activity,w->w.move(1,h)).ok,"session legal embark/bend");world=SessionProbe.view(activity);});
            check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(world)),"boat command exact save parity");
            check(world.army.water(world.unit(1).hex),"actual unit is naval on authorized water");
            shot("r05-fixture-boat-water-"+h.q+"-"+h.r);
        }
        World loaded=SaveCodec.decode(SaveCodec.encode(world));
        runOnMainSync(()->{SessionProbe.install(activity,loaded);activity.refresh();world=SessionProbe.view(activity);});
        check(reference.enter(1,31).ok,"reference dock entry after save");
        runOnMainSync(()->{check(SessionProbe.command(activity,w->w.enter(1,31)).ok,"session dock entry after save");world=SessionProbe.view(activity);});
        check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(world)),"port entry exact save parity");
        shot("r05-fixture-boat-docked-after-load");
        try(FileWriter out=new FileWriter(new File(getTargetContext().getExternalFilesDir("s01"),"r05-boat-fixture.txt"))){out.write("Explicit 24x14 synthetic save, not national art reference. Actual commands through GameSession: land(8,6) -> water(9,6) -> bend(10,5) -> SaveCodec reload -> enter port31(11,5). Every result/save compared to independent core replay. Captures/recording show committed states, not proof of interpolated animation.\n");}
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",0));
        long end=SystemClock.uptimeMillis()+90000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<end)settle();
        host=(MapHost)field(activity,"map");runOnMainSync(()->world=SessionProbe.view(activity));
        check(world!=null,"official scenario loaded");byte[] before=SaveCodec.encode(world);
        runOnMainSync(()->{host.switchMode(true);host.quality(SceneQuality.MEDIUM);activity.selectAndFocus(world.home().hex);});settle();ready();
        FilamentMapView v=spatial();
        runOnMainSync(()->{v.camera.span=10;v.camera.tilt=55;v.camera.yaw=0;v.setGridShown(false);});shot("r05-home-baseline-comparable");
        MapSceneSnapshot.Ground g=((MapSceneSnapshot)field(v,"snapshot")).ground;
        WaterLandformInstrumentation locate=new WaterLandformInstrumentation();locate.world=world;
        Hex port=locate.site(World.SiteKind.PORT),narrow=locate.water(false,false),broad=locate.water(true,false);
        Hex[] targets={port,narrow,broad};String[] names={"port","narrow-channel","broad-shore"};
        for(int j=0;j<targets.length;j++)for(float span:new float[]{8,24,70})for(float yaw:new float[]{0,90}){
            final Hex target=targets[j];runOnMainSync(()->{v.center(target);v.camera.span=span;v.camera.yaw=yaw;v.setGridShown(false);});
            shot("r05-"+names[j]+"-"+(int)span+"-"+(int)yaw+"-grid-off");
        }
        runOnMainSync(()->{v.center(narrow);v.camera.span=8;v.camera.yaw=0;v.setGridShown(true);});shot("r05-narrow-grid-on");
        runOnMainSync(()->v.setGridShown(false));
        for(int step=0;step<4;step++){
            long t=SystemClock.uptimeMillis();float x=v.getWidth()*.4f,y=v.getHeight()*.4f;
            for(int i=0;i<=12;i++){final int point=i;runOnMainSync(()->{MotionEvent e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),point==0?MotionEvent.ACTION_DOWN:point==12?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE,x+point*15,y+point*5,0);v.onTouchEvent(e);e.recycle();});SystemClock.sleep(40);}
            shot("r05-drag-"+step);
        }
        for(SceneQuality quality:SceneQuality.values()){
            runOnMainSync(()->host.quality(quality));settle();ready();FilamentMapView qv=spatial();
            runOnMainSync(()->{qv.center(narrow);qv.camera.span=10;qv.camera.yaw=0;qv.camera.tilt=55;qv.setGridShown(false);});shot("r05-quality-"+quality);
        }
        runOnMainSync(()->world=SessionProbe.view(activity));check(Arrays.equals(before,SaveCodec.encode(world)),"all render qualities and cameras preserve full authority bytes");
        commandFlow();shot("r05-command-result");
        boatCommands();
        result.putString("stream","PASS R05 "+checks+" installed checks; water visual acceptance needs manual review; software emulator only\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{capture("r05-failure");}catch(Throwable ignored){}result.putString("stream","FAIL R05 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

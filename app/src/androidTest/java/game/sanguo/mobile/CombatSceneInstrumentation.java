package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Installed production renderer and shared TurnWork/TurnPlayback, never synthetic damage. */
public final class CombatSceneInstrumentation extends SceneInstrumentation {
    void activate(World w)throws Exception{
        world=w;runOnMainSync(()->{invoke("activateWorld",new Class<?>[]{World.class},w);activity.refresh();host.switchMode(true);host.focus(new Hex(9,8));});
        runOnMainSync(()->{try{((FilamentMapView)field(host,"spatial")).camera.span=4;}catch(Exception e){throw new RuntimeException(e);}});settle();ready();
    }
    void sequence(String kind)throws Exception{
        World w=SaveCodec.decode(SaveCodec.encode(CombatSceneFixture.world(kind))),reference=SaveCodec.decode(SaveCodec.encode(w)),visual=SaveCodec.decode(SaveCodec.encode(w));
        CombatSceneFixture.action(reference,kind);TurnJournal journal=new TurnJournal(w);CombatSceneFixture.action(w,kind);journal.close();activate(visual);
        FilamentMapView view=(FilamentMapView)field(host,"spatial");boolean drawn=false;
        for(TurnJournal.Event e:journal.events()){
            if(e.critical!=null){runOnMainSync(()->host.criticalFrame(e.critical,.5f));settle();capture("s06-critical");runOnMainSync(()->host.criticalFrame(null,0));}
            for(int i=0;i<=20;i++){
                final float f=i/20f;runOnMainSync(()->host.replayFrame(e,f));SystemClock.sleep(60);
                if(i==8||i==16){settle();check(host.is3D(),"no native fallback "+kind);CombatVisual pool=(CombatVisual)field(view,"combat");drawn|=pool.count>0;capture("s06-"+kind+"-"+i);}
            }
            e.applyVisual(visual);runOnMainSync(()->{host.replayFrame(null,0);host.invalidateScene();host.setWorld(visual,null,-1);});
        }
        check(drawn,"native event effects visible "+kind);
        check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(w)),"native sampling retains exact command result "+kind);
        runOnMainSync(()->host.resume(false));check(((CombatVisual)field(view,"combat")).count==0,"background clears pool");runOnMainSync(()->host.resume(true));
        surfaceCapture();
        System.out.println("S06_NATIVE case="+kind+" events="+journal.events().size());
    }
    void reducedMotion()throws Exception{
        World initial=SaveCodec.decode(SaveCodec.encode(CombatSceneFixture.world("critical")));
        World computed=SaveCodec.decode(SaveCodec.encode(initial));TurnJournal journal=new TurnJournal(computed);CombatSceneFixture.action(computed,"critical");journal.close();activate(initial);
        try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("settings put global animator_duration_scale 0"))){while(input.read()!=-1){}}
        SystemClock.sleep(1000);check(!UiMotion.enabled(),"system reduced motion enabled");
        TurnJournal.Event event=journal.events().get(0);runOnMainSync(()->host.replayFrame(event,.4f));settle();
        FilamentMapView view=(FilamentMapView)field(host,"spatial");check(((CombatVisual)field(view,"combat")).count==0,"reduced motion omits transient combat");
        runOnMainSync(()->host.replayFrame(null,0));
        try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("settings put global animator_duration_scale 1"))){while(input.read()!=-1){}}
        SystemClock.sleep(1000);check(UiMotion.enabled(),"restore motion for actual playback tests");
    }
    void turn(boolean spatial,int speed,boolean skip,boolean interrupt)throws Exception{
        World before=SaveCodec.decode(SaveCodec.encode(Turn48Fixture.world()));World reference=SaveCodec.decode(SaveCodec.encode(before));
        long begin=SystemClock.elapsedRealtime();check(reference.nextTurn().ok,"reference next turn");long referenceMs=SystemClock.elapsedRealtime()-begin;
        activate(before);runOnMainSync(()->{if(!spatial)host.switchMode(false);invoke("advanceTurn",new Class<?>[0]);});
        TurnWork work=(TurnWork)field(activity,"turnWork");runOnMainSync(()->{work.speed=speed;work.fullReplay=true;work.skipAnimations=skip;});
        if(interrupt){
            runOnMainSync(()->{work.pause(true);host.resume(false);});SystemClock.sleep(400);
            runOnMainSync(()->{try{TurnPlayback playback=(TurnPlayback)field(activity,"playback");if(playback!=null){playback.detach();TurnPlayback replacement=new TurnPlayback(host,work,()->{},()->invoke("completeTurnPlayback",new Class<?>[0]));java.lang.reflect.Field f=MainActivity.class.getDeclaredField("playback");f.setAccessible(true);f.set(activity,replacement);replacement.start();}work.pause(false);host.resume(true);}catch(Exception e){throw new RuntimeException(e);}});
        }
        long deadline=SystemClock.uptimeMillis()+120000;
        while((Boolean)field(activity,"aiRunning")&&SystemClock.uptimeMillis()<deadline)settle();
        check(!(Boolean)field(activity,"aiRunning"),"shared playback completes");
        World after=(World)field(activity,"world");check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(after)),"2D/3D/speed/skip/interruption exact full save equality");
        System.out.println("S06_TURN spatial="+spatial+" speed="+speed+" skip="+skip+" interrupt="+interrupt+" referenceMs="+referenceMs+" computeMs="+work.computeMillis+" totalMs="+work.totalMillis+" visible="+work.visibleCount+" batches="+work.publishedBatches);
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        check(UiMotion.enabled(),"combat acceptance requires animator_duration_scale=1; reduced mode tested separately");
        World initial=CombatSceneFixture.world("critical");try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(initial));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
        ParcelFileDescriptor recording=getUiAutomation().executeShellCommand("screenrecord --time-limit 150 /sdcard/combat-scene.mp4");
        for(String kind:new String[]{"critical","arrow","stone","charge","fire","trap-ball","lightning","counter","defeat","enemy","facilities","site"})sequence(kind);
        recording.close();reducedMotion();
        turn(false,1,false,false);turn(true,4,false,false);turn(true,2,false,true);turn(true,1,true,false);
        result.putString("stream","PASS S06 native combat "+checks+" checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){e.printStackTrace();result.putString("stream","FAIL S06 "+e+"\n");finish(Activity.RESULT_CANCELED,result);}}
}

package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Real MainActivity.apply -> GameSession transaction -> production sequence/Filament.
 * Fixtures are explicit and saveable. Screens are installed Surface pixels, never renders off device. */
public final class NativeR11Instrumentation extends SceneInstrumentation {
    private File dir;private int failures;
    private final StringBuilder evidence=new StringBuilder();
    private FilamentMapView view()throws Exception{return (FilamentMapView)field(host,"spatial");}
    private CombatSequence sequence()throws Exception{return (CombatSequence)field(host,"commandEffects");}
    private World copy(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    private void record(String text)throws Exception{evidence.append(text).append('\n');Files.write(new File(dir,"r11-runtime.txt").toPath(),evidence.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));android.util.Log.i("R11Evidence",text);}
    private void assetsReady()throws Exception{
        ready();long end=SystemClock.uptimeMillis()+90000;boolean[] done={false};
        do {runOnMainSync(()->{try{done[0]=!(Boolean)field(view(),"assetSyncPending")&&((SceneAssetQueue)field(view(),"assetWork")).pending()==0;}catch(Exception e){throw new RuntimeException(e);}});if(done[0])break;settle();}while(SystemClock.uptimeMillis()<end);
        check(done[0],"asset uploads settled");check(host.is3D(),"Filament active");
    }
    private void activate(World w,SceneQuality quality)throws Exception{
        runOnMainSync(()->{SessionProbe.install(activity,w);activity.refresh();world=SessionProbe.view(activity);host.switchMode(true);host.quality(quality);host.center(new Hex(9,8));host.setGridShown(false);try{view().camera.span=3.4f;}catch(Exception e){throw new RuntimeException(e);}});
        assetsReady();
    }
    private byte[] authority()throws Exception{
        byte[][] data={null};runOnMainSync(()->{try{data[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception e){throw new RuntimeException(e);}});return data[0];
    }
    private void exact(byte[] expected,String label)throws Exception{
        check(Arrays.equals(expected,authority()),"authority parity "+label);
        try(InputStream in=getTargetContext().openFileInput("auto.sg11")){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);check(Arrays.equals(expected,bytes.toByteArray()),"autosave parity "+label);}
    }
    private void shot(String name,TurnJournal.Event event)throws Exception{
        assetsReady();surfaceCapture();capture(name+"-ui");
        Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);
        String[] report={""};runOnMainSync(()->{try{
            var v=view();var pool=(CombatVisual)field(v,"combat");check(pool.count<=CombatVisual.CAPACITY,"native effect capacity");
            report[0]="source="+BuildConfig.SOURCE_REVISION+" event="+(event==null?"none":event.id+" "+event.kind+" "+CombatVisual.style(event))+" fraction="+field(v,"replayFraction")+" effects="+pool.count+"\n"+host.report();
        }catch(Exception e){throw new RuntimeException(e);}});
        Files.write(new File(dir,name+".txt").toPath(),report[0].getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private void freeze(CombatSequence sequence,TurnJournal.Event event,float target)throws Exception{
        runOnMainSync(()->host.pauseCommandEffects(false));long deadline=SystemClock.uptimeMillis()+45000;boolean[] reached={false};
        while(SystemClock.uptimeMillis()<deadline){
            runOnMainSync(()->{if(sequence.current()==event&&sequence.fraction()>=target){host.pauseCommandEffects(true);reached[0]=true;}});
            if(reached[0])return;if(sequence.current()!=event)break;SystemClock.sleep(15);
        }
        throw new AssertionError("event phase not observed "+event.id+" wanted="+target+" actual="+sequence.fraction());
    }
    private void commandCase(String kind,SceneQuality quality)throws Exception{
        World initial=copy(NativeR11Fixture.world(kind)),reference=copy(initial);
        check(NativeR11Fixture.command(reference,kind).ok,"reference command valid "+kind);byte[] expected=SaveCodec.encode(reference);
        Files.write(new File(dir,"r11-"+kind+"-fixture.sg11").toPath(),SaveCodec.encode(initial));
        activate(initial,quality);
        runOnMainSync(()->{check(SessionProbe.command(activity,w->NativeR11Fixture.command(w,kind)).ok,"normal Activity command "+kind);host.pauseCommandEffects(true);});
        exact(expected,"committed before effects "+kind);
        CombatSequence seq=sequence();check(seq!=null&&!seq.done(),"normal command connected to live timeline "+kind);
        int eventCount=0,images=0;
        while(!seq.done()){
            TurnJournal.Event event=seq.current();eventCount++;int hits=Math.max(1,event.strikes.size());
            record("EVENT case="+kind+" id="+event.id+" kind="+event.kind+" style="+CombatVisual.style(event)+" hits="+event.strikes.size()+" impacts="+event.impacts.stream().map(i->i.metric+":"+i.amount+":"+i.entityKey).collect(java.util.stream.Collectors.toList()));
            if(CombatVisual.style(event)!=CombatVisual.Style.NONE){
                if(CombatVisual.style(event)!=CombatVisual.Style.STATUS&&CombatVisual.style(event)!=CombatVisual.Style.RECOVER){
                    freeze(seq,event,.43f/hits);shot("r11-"+kind+"-"+quality+"-"+eventCount+"-launch",event);images++;
                }
                freeze(seq,event,.78f/hits);float stopped=seq.fraction();SystemClock.sleep(250);check(stopped==seq.fraction(),"real pause retains phase");
                shot("r11-"+kind+"-"+quality+"-"+eventCount+"-hit",event);images++;
                if(event.strikes.size()>1){freeze(seq,event,(hits-1+.78f)/hits);shot("r11-"+kind+"-"+quality+"-"+eventCount+"-counter",event);images++;}
            }
            runOnMainSync(()->host.pauseCommandEffects(false));long end=SystemClock.uptimeMillis()+45000;
            while(seq.current()==event&&SystemClock.uptimeMillis()<end)SystemClock.sleep(15);
            runOnMainSync(()->host.pauseCommandEffects(true));check(seq.current()!=event,"event completes without input lock");
        }
        runOnMainSync(()->host.pauseCommandEffects(false));settle();exact(expected,"completed "+kind);
        check(sequence()==null,"finished timeline removes callbacks "+kind);
        check(images>0,"real runtime images produced "+kind);
        record("PASS CASE "+kind+" quality="+quality+" events="+eventCount+" images="+images+" authority/autosave=exact");
    }
    private void modes()throws Exception{
        for(String mode:new String[]{"2x","4x","skip","offscreen","background","rebuild"}){
            World initial=copy(NativeR11Fixture.world("counter")),reference=copy(initial);NativeR11Fixture.command(reference,"counter");byte[] expected=SaveCodec.encode(reference);activate(initial,SceneQuality.MEDIUM);
            runOnMainSync(()->{if(mode.equals("offscreen"))host.center(new Hex(1,1));check(SessionProbe.command(activity,w->NativeR11Fixture.command(w,"counter")).ok,"mode command");host.commandEffectSpeed(mode.equals("2x")?2:4);});
            FilamentMapView old=view();
            if(mode.equals("skip"))runOnMainSync(()->host.cancelCommandEffects());
            if(mode.equals("background")){runOnMainSync(()->host.resume(false));check(((CombatVisual)field(old,"combat")).count==0,"background clear");runOnMainSync(()->host.resume(true));}
            if(mode.equals("rebuild")){runOnMainSync(()->{host.switchMode(false);host.switchMode(true);});assetsReady();check((Boolean)field(old,"released"),"old native engine released");}
            long end=SystemClock.uptimeMillis()+30000;while(sequence()!=null&&SystemClock.uptimeMillis()<end)SystemClock.sleep(25);
            check(sequence()==null,"mode reaches final snapshot "+mode);exact(expected,mode);
            runOnMainSync(()->host.center(new Hex(9,8)));settle();check(field(view(),"replay")==null,"no re-entry/rebuild duplicate "+mode);
            record("PASS MODE "+mode+" authority/autosave=exact; replay=null");
        }
        World fire=copy(NativeR11Fixture.world("fire"));NativeR11Fixture.command(fire,"fire");activate(copy(fire),SceneQuality.MEDIUM);
        check(!((MapSceneSnapshot)field(view(),"snapshot")).fires.isEmpty(),"saved fire rehydrated");shot("r11-fire-reload",null);
        activate(copy(NativeR11Fixture.world("melee")),SceneQuality.MEDIUM);
        check(((MapSceneSnapshot)field(view(),"snapshot")).fires.isEmpty(),"replacement removes old fires");check(field(view(),"replay")==null,"replacement removes old event");shot("r11-fire-replaced",null);
        record("PASS replacement/save-load fire snapshot lifecycle");
    }
    private void turn(boolean skip)throws Exception{
        World initial=copy(Turn48Fixture.world()),reference=copy(initial);check(reference.nextTurn().ok,"reference real turn");byte[] expected=SaveCodec.encode(reference);activate(initial,SceneQuality.MEDIUM);
        runOnMainSync(()->invoke("advanceTurn",new Class<?>[0]));TurnWork work=(TurnWork)field(activity,"turnWork");runOnMainSync(()->{work.fullReplay=true;work.speed=4;work.skipAnimations=skip;});
        Set<String> photographed=new HashSet<>();long deadline=SystemClock.uptimeMillis()+240000;
        while((Boolean)field(activity,"aiRunning")&&SystemClock.uptimeMillis()<deadline){
            TurnJournal.Event event=(TurnJournal.Event)field(view(),"replay");float f=(Float)field(view(),"replayFraction");
            if(!skip&&event!=null&&(event.kind==TurnJournal.Kind.RECOVER||event.kind==TurnJournal.Kind.FACILITY_ATTACK)&&CombatVisual.phase(event,f)>=CombatVisual.FEEDBACK&&!photographed.contains(event.kind.name())){
                runOnMainSync(()->{try{TurnPlayback p=(TurnPlayback)field(activity,"playback");if(p!=null)p.pause(true);}catch(Exception e){throw new RuntimeException(e);}});
                shot("r11-turn-"+event.kind,event);photographed.add(event.kind.name());
                runOnMainSync(()->{try{TurnPlayback p=(TurnPlayback)field(activity,"playback");if(p!=null)p.pause(false);}catch(Exception e){throw new RuntimeException(e);}});
            }
            SystemClock.sleep(15);
        }
        check(!(Boolean)field(activity,"aiRunning"),"real turn completes");exact(expected,"turn skip="+skip);
        record("PASS TURN skip="+skip+" batches="+work.publishedBatches+" visible="+work.visibleCount+" critical="+work.criticalsShown+" photographed="+photographed+" full-save=exact");
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        World initial=NativeR11Fixture.world("melee");try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(initial));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        record("SOURCE="+BuildConfig.SOURCE_REVISION+" BACKEND=Filament1.56/OpenGL DEVICE=emulator; explicit fixture commands, not manual PC art acceptance");
        for(String kind:new String[]{"melee","arrow","stone","charge","fire","fire-arrow","critical-fire","trap-seed","trap-ball","trap-ship","lightning","critical","counter","defeat","enemy","facility-counter","site","status","equipment-RAM","equipment-WOODEN_BEAST","equipment-TOWER_SHIP"}){
            try{commandCase(kind,SceneQuality.MEDIUM);}catch(Throwable e){failures++;record("FAIL CASE "+kind+"\n"+android.util.Log.getStackTraceString(e));runOnMainSync(()->host.cancelCommandEffects());}
        }
        for(SceneQuality q:new SceneQuality[]{SceneQuality.LOW,SceneQuality.HIGH})try{commandCase("arrow",q);}catch(Throwable e){failures++;record("FAIL QUALITY "+q+" "+android.util.Log.getStackTraceString(e));}
        try{modes();}catch(Throwable e){failures++;record("FAIL MODES "+android.util.Log.getStackTraceString(e));}
        for(boolean skip:new boolean[]{false,true})try{turn(skip);}catch(Throwable e){failures++;record("FAIL TURN skip="+skip+" "+android.util.Log.getStackTraceString(e));}
        record((failures==0?"PASS":"FAIL")+" R11 installed checks="+checks+" failures="+failures+"; physical ARM64/PC art/thermal/manual matrix NOT_RUN");
        result.putString("stream",(failures==0?"PASS":"FAIL")+" R11 checks="+checks+" failures="+failures+"\n");finish(failures==0?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }catch(Throwable e){result.putString("stream","FAIL R11 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

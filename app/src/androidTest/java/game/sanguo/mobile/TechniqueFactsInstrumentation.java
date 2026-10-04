package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import game.sanguo.api.GameEvent;
import game.sanguo.api.GameApi;
import game.sanguo.core.SaveCodec;
import game.sanguo.core.ScenarioCatalog;
import game.sanguo.core.World;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Installed HUD/media adapter proof. Public host event/phase patch remains a separate integration gate. */
public final class TechniqueFactsInstrumentation extends SceneInstrumentation {
    private TechniquePointsHud hud;
    private SoundEffects sounds;
    private TextView badge;
    private GameApi.Subscription subscription;
    private final List<GameEvent> observed=new ArrayList<>();
    private String shell(String command)throws Exception{try(var fd=getUiAutomation().executeShellCommand(command);var in=new java.io.FileInputStream(fd.getFileDescriptor());var out=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[4096];for(int n;(n=in.read(buffer))!=-1;)out.write(buffer,0,n);return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8).trim();}}
    private byte[] capture()throws Exception{byte[][] value={null};runOnMainSync(()->{try{value[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception e){throw new RuntimeException(e);}});return value[0];}
    private int shown(){int[] value={0};runOnMainSync(()->value[0]=Integer.parseInt(badge.getText().toString().split("\n")[1]));return value[0];}
    private boolean heard(String id)throws Exception{boolean[] value={false};runOnMainSync(()->{try{value[0]=((Set<?>)field(sounds,"heard")).contains(id);}catch(Exception e){throw new RuntimeException(e);}});return value[0];}
    private void zeroNet(){runOnMainSync(()->check(SessionProbe.command(activity,w->{
        int points=w.campaign.points(w.player);var gain=w.editor.apply(w.editor.faction(w.player,w.actionPoints[w.player],points+20));
        return gain.ok?w.editor.apply(w.editor.faction(w.player,w.actionPoints[w.player],points)):gain;
    }).ok,"actual normal Activity command commits two opposite writes"));}
    @Override public void onStart(){Bundle result=new Bundle();String originalScale=null;try{
        originalScale=shell("settings get global animator_duration_scale");
        shell("settings put global animator_duration_scale 1");
        World seed=ScenarioCatalog.load("heroes-250",0);check(seed.editor.apply(seed.editor.faction(seed.player,60,2000)).ok,"known source fixture points");
        try(var out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");ready();hud=(TechniquePointsHud)field(activity,"techniqueHud");sounds=((GameApplication)activity.getApplication()).sounds();badge=(TextView)field(activity,"actionPointsBadge");
        long motionDeadline=SystemClock.uptimeMillis()+10000;while(!UiMotion.enabled()&&SystemClock.uptimeMillis()<motionDeadline)SystemClock.sleep(25);
        check(UiMotion.enabled(),"actual motion setting enables distinct fact starts for PCM proof");
        long deadline=SystemClock.uptimeMillis()+10000;while(!sounds.loaded()&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(25);
        check(sounds.loaded()&&sounds.active(),"installed audio ready in normal 3D host");
        runOnMainSync(()->{
            var session=((GameApplication)activity.getApplication()).host().session();World current=SessionProbe.view(activity);
            hud.syncFactsBaseline(session.state(),current.player,current.campaign.points(current.player));
            // Temporary test adapter represents the reserved sequential MainActivity patch.
            subscription=session.subscribe(event->{observed.add(event);hud.committedFacts(event,current.player);});
        });
        int initialRolls=hud.rolls;byte[] before=capture();World control=SaveCodec.decode(before);
        check(control.editor.apply(control.editor.faction(control.player,control.actionPoints[control.player],2020)).ok,"reference gain");
        check(control.editor.apply(control.editor.faction(control.player,control.actionPoints[control.player],2000)).ok,"reference loss");
        zeroNet();check(observed.size()==1&&observed.get(0).techniquePointsFacts.size()==2&&observed.get(0).techniquePointsChanges.isEmpty(),"real zero-net commit retains two facts");
        GameEvent event=observed.get(0);String phase=event.techniquePointsFacts.get(0).presentationParentId;
        check(!phase.isEmpty()&&phase.equals(event.techniquePointsFacts.get(1).presentationParentId),"original journal phase retained");
        SystemClock.sleep(250);check(hud.rolls==initialRolls,"pending exact phase has no premature sound");
        runOnMainSync(()->hud.releasePresentation("unrelated"));SystemClock.sleep(120);check(hud.rolls==initialRolls,"unrelated renderer phase silent");
        runOnMainSync(()->hud.releasePresentation(phase));SystemClock.sleep(1900);
        check(hud.rolls==initialRolls+2&&shown()==2000,"two opposite actual facts animate despite unchanged NET");
        check(heard(event.techniquePointsFacts.get(0).id)&&heard(event.techniquePointsFacts.get(1).id),"each sound owns exact fact.id");
        check(Arrays.equals(SaveCodec.encode(control),capture()),"fact presentation preserves entire control Save/RNG");
        runOnMainSync(()->{check(hud.committedFacts(event,0)==TechniqueFactQueue.Result.IGNORED,"duplicate committed event ignored");hud.releasePresentation(phase);activity.refresh();});
        SystemClock.sleep(700);check(hud.rolls==initialRolls+2&&shown()==2000,"duplicate phase and normal refresh never play NET or repeat");
        runOnMainSync(()->check(!SessionProbe.command(activity,w->w.patrol(-1,-1)).ok,"normal failed command"));
        check(observed.size()==1&&Arrays.equals(SaveCodec.encode(control),capture()),"failed command has no facts and no Save/RNG changes");
        zeroNet();GameEvent second=observed.get(1);String secondPhase=second.techniquePointsFacts.get(0).presentationParentId;
        runOnMainSync(()->{hud.foreground(false);hud.foreground(true);hud.releasePresentation(secondPhase);activity.refresh();});
        SystemClock.sleep(700);check(hud.rolls==initialRolls+2&&!heard(second.techniquePointsFacts.get(0).id),"background discards pending fact without later replay");
        check(Arrays.equals(SaveCodec.encode(control),capture()),"second zero-net editor commit and background preserve full Save/RNG");
        zeroNet();GameEvent third=observed.get(2);String thirdPhase=third.techniquePointsFacts.get(0).presentationParentId;
        runOnMainSync(()->{hud.pauseFacts(true);sounds.pauseEffects(true);hud.releasePresentation(thirdPhase);});
        SystemClock.sleep(650);check(hud.rolls==initialRolls+2&&!heard(third.techniquePointsFacts.get(0).id),"paused exact phase retains facts without sound");
        runOnMainSync(()->{sounds.pauseEffects(false);hud.pauseFacts(false);});SystemClock.sleep(1900);
        check(hud.rolls==initialRolls+4&&shown()==2000&&heard(third.techniquePointsFacts.get(0).id)&&heard(third.techniquePointsFacts.get(1).id),"resume consumes each retained fact once");
        runOnMainSync(()->{hud.pauseFacts(true);hud.pauseFacts(false);hud.releasePresentation(thirdPhase);});SystemClock.sleep(500);
        check(hud.rolls==initialRolls+4&&Arrays.equals(SaveCodec.encode(control),capture()),"repeated pause/resume never replays; full Save/RNG unchanged");
        zeroNet();GameEvent fourth=observed.get(3);String fourthPhase=fourth.techniquePointsFacts.get(0).presentationParentId;
        runOnMainSync(()->{hud.releasePresentation(fourthPhase);hud.skipPresentation(fourthPhase);});SystemClock.sleep(650);
        check(hud.rolls==initialRolls+4&&!heard(fourth.techniquePointsFacts.get(0).id)&&!heard(fourth.techniquePointsFacts.get(1).id),"skip before scheduled frame cancels current and pending fact without sound");
        check(Arrays.equals(SaveCodec.encode(control),capture()),"skip changes presentation only, entire Save/RNG unchanged");
        runOnMainSync(()->{SessionProbe.install(activity,control);activity.refresh();hud.releasePresentation(phase);});SystemClock.sleep(500);
        check(hud.rolls==initialRolls+4&&shown()==2000&&Arrays.equals(SaveCodec.encode(control),capture()),"real restore resets generation without replay");
        result.putString("stream","PASS TECHNIQUE FACTS "+checks+" checks; installed normal3D HUD with temporary readonly event/phase test adapter; shared host integration pending\n");
    }catch(Throwable e){result.putString("stream","FAIL TECHNIQUE FACTS "+android.util.Log.getStackTraceString(e));}
    finally{try{
        runOnMainSync(()->{if(subscription!=null)subscription.close();});finishActivityForRestore();
        if(originalScale!=null){
            if(originalScale.equals("null"))shell("settings delete global animator_duration_scale");
            else{Double.parseDouble(originalScale);shell("settings put global animator_duration_scale "+originalScale);}
            check(originalScale.equals(shell("settings get global animator_duration_scale")),"original animation setting restored exactly");
        }
    }catch(Throwable e){result.putString("stream","FAIL teardown/setting restore "+e);}}
    finish(Activity.RESULT_OK,result);}
}

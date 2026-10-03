package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import game.sanguo.core.*;
import java.util.*;

/** Actual app command commits, scalar HUD animation and complete rule/RNG comparison. */
public final class TechniquePointsInstrumentation extends SceneInstrumentation {
    private TechniquePointsHud hud;private SoundEffects sounds;private TextView badge;
    private byte[] capture()throws Exception{byte[][] b={null};runOnMainSync(()->{try{b[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception e){throw new RuntimeException(e);}});return b[0];}
    private int heard()throws Exception{int[] n={0};runOnMainSync(()->{try{for(Object key:(Set<?>)field(sounds,"heard"))if(key.toString().startsWith("technique:"))n[0]++;}catch(Exception e){throw new RuntimeException(e);}});return n[0];}
    private int shown()throws Exception{int[] n={0};runOnMainSync(()->n[0]=Integer.parseInt(badge.getText().toString().split("\n")[1]));return n[0];}
    private String shell(String command)throws Exception{try(var fd=getUiAutomation().executeShellCommand(command);var in=new java.io.FileInputStream(fd.getFileDescriptor());var out=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[4096];for(int n;(n=in.read(buffer))!=-1;)out.write(buffer,0,n);return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8).trim();}}
    private void motion(boolean enabled)throws Exception{shell("settings put global animator_duration_scale "+(enabled?"1":"0"));long end=SystemClock.uptimeMillis()+10000;while(UiMotion.enabled()!=enabled&&SystemClock.uptimeMillis()<end)SystemClock.sleep(30);check(UiMotion.enabled()==enabled,"actual system motion setting applied");}
    @Override public void onStart(){Bundle result=new Bundle();String scale=null;boolean motionBefore=UiMotion.enabled();try{
        scale=shell("settings get global animator_duration_scale");
        World seed=ScenarioCatalog.load("heroes-250",0);check(seed.editor.apply(seed.editor.faction(seed.player,60,2000)).ok,"authoring fixture has known points");
        World.City city=seed.home();city.defense=city.baseDefense-400;
        try(var out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");ready();world=SessionProbe.view(activity);hud=(TechniquePointsHud)field(activity,"techniqueHud");sounds=((GameApplication)activity.getApplication()).sounds();badge=(TextView)field(activity,"actionPointsBadge");
        motionBefore=UiMotion.enabled();motion(true);
        long end=SystemClock.uptimeMillis()+10000;while(!sounds.loaded()&&SystemClock.uptimeMillis()<end)SystemClock.sleep(25);
        check(sounds.loaded()&&shown()==2000&&hud.rolls==0,"initial left HUD shows own technique points without an opening roll");int initialHeard=heard();byte[] initial=capture();
        runOnMainSync(()->{world.editor.faction(world.player,60,9000);activity.refresh();activity.refresh();});
        check(Arrays.equals(initial,capture())&&hud.rolls==0&&heard()==initialHeard,"preview and repeated refresh preserve save/RNG and produce no technique feedback");
        World reference=SaveCodec.decode(initial);int officer=world.idle(city).get(0).id;check(reference.campaign.repair(city.id,officer).ok,"reference real repair succeeds");World stale=world;
        runOnMainSync(()->check(SessionProbe.command(activity,w->w.campaign.repair(city.id,officer)).ok,"actual app repair commit"));world=SessionProbe.view(activity);
        long rollingBegan=SystemClock.uptimeMillis(),rollingDeadline=rollingBegan+2000;int mid;
        do{SystemClock.sleep(20);mid=shown();}while(mid==2000&&SystemClock.uptimeMillis()<rollingDeadline);
        check(mid>2000&&mid<2020,"actual displayed intermediate integer proves rolling animation: value="+mid+" wait_ms="+(SystemClock.uptimeMillis()-rollingBegan)+" rolls="+hud.rolls+" foreground="+field(hud,"foreground")+" motion="+UiMotion.enabled()+" authority="+world.campaign.points(world.player));
        SystemClock.sleep(650);check(shown()==2020&&hud.rolls==1&&heard()==initialHeard+1,"actual positive commit rolls to +20 with one sound identity");capture("technique-gain");
        check(Arrays.equals(SaveCodec.encode(reference),capture()),"positive points command preserves complete reference authority/RNG");
        byte[] afterGain=capture();runOnMainSync(()->{check(!activity.applyResult(stale,()->stale.campaign.repair(city.id,officer)).ok,"stale duplicate rejected");activity.refresh();});
        check(Arrays.equals(afterGain,capture())&&hud.rolls==1&&heard()==initialHeard+1,"stale submission and repeated display never replay feedback");
        final Campaign.Tech tech=Campaign.Tech.SPEAR_DRILL;int researcher=-1;
        for(World.Officer o:world.idle(world.city(city.id)))if(world.campaign.researchError(city.id,o.id,tech)==null){researcher=o.id;break;}
        check(researcher>=0,"real research spending available");final int actor=researcher;
        check(reference.campaign.research(city.id,actor,tech).ok,"reference research spending succeeds");
        runOnMainSync(()->check(SessionProbe.command(activity,w->w.campaign.research(city.id,actor,tech)).ok,"actual app research commit"));
        check(Arrays.equals(SaveCodec.encode(reference),capture()),"negative points command preserves complete reference authority/RNG");
        SystemClock.sleep(700);check(shown()==reference.campaign.points(reference.player)&&hud.rolls==2&&heard()==initialHeard+2,"negative commit rolls downward and plays one identity");capture("technique-loss");
        byte[] finalSave=capture();runOnMainSync(()->{hud.foreground(false);activity.refresh();hud.foreground(true);activity.refresh();});
        check(Arrays.equals(finalSave,capture())&&hud.rolls==2&&heard()==initialHeard+2,"background/resume and unchanged revision do not replay point sounds");
        World reduced=SaveCodec.decode(finalSave);reduced.city(city.id).defense-=400;check(reduced.editor.apply(reduced.editor.faction(reduced.player,60,3000)).ok,"authoring reduced-motion fixture");
        runOnMainSync(()->{SessionProbe.install(activity,reduced);activity.refresh();});check(shown()==3000&&hud.rolls==2&&heard()==initialHeard+2,"normal session replacement resets point display without replaying old gains");
        motion(false);World reducedReference=SaveCodec.decode(capture());final int other=SessionProbe.view(activity).idle(SessionProbe.view(activity).city(city.id)).get(0).id;
        check(reducedReference.campaign.repair(city.id,other).ok,"reduced-motion reference repair");runOnMainSync(()->check(SessionProbe.command(activity,w->w.campaign.repair(city.id,other)).ok,"actual reduced-motion commit"));
        check(shown()==3020&&hud.rolls==3&&heard()==initialHeard+3&&Arrays.equals(SaveCodec.encode(reducedReference),capture()),"disabled animation shows final points with one sound and exact authority/RNG");
        result.putString("stream","PASS TECHNIQUE HUD "+checks+" checks\n");
    }catch(Throwable e){result.putString("stream","FAIL TECHNIQUE HUD "+android.util.Log.getStackTraceString(e));}
    finally{try{if(scale!=null){if(scale.equals("null")){motion(motionBefore);shell("settings delete global animator_duration_scale");}else{Double.parseDouble(scale);shell("settings put global animator_duration_scale "+scale);}check(scale.equals(shell("settings get global animator_duration_scale")),"original animation setting restored exactly");}}catch(Throwable e){result.putString("stream","FAIL motion restore "+e);}}
    try{finishActivityForRestore();}catch(Throwable e){result.putString("stream","FAIL teardown "+e);}finish(Activity.RESULT_OK,result);}
}

package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Real custom-map commands verify that source-only restrictions keep compatibility usable. */
public final class PcVisualPolicyInstrumentation extends SceneInstrumentation {
    private File dir,report;
    private FilamentMapView renderer;
    @Override public void onStart(){
        Bundle result=new Bundle();File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");
        boolean existed=auto.exists(),backed=false;byte[] backup=null;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);var old=new HashMap<>(prefs.getAll());
        var client=getTargetContext().getSharedPreferences("MainActivity",0);var oldClient=new HashMap<>(client.getAll());
        try{
            if(existed)backup=Files.readAllBytes(auto.toPath());backed=true;
            dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();report=new File(dir,"pc-visual-policy-report.txt");Files.deleteIfExists(report.toPath());
            Files.write(auto.toPath(),SaveCodec.encode(NativeR11Fixture.world("arrow")));prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            settle();host=(MapHost)field(activity,"map");
            for(String kind:new String[]{"arrow","critical"}){
                World fixture=NativeR11Fixture.world(kind),expected=SaveCodec.decode(SaveCodec.encode(fixture));
                World.Result expectedResult=NativeR11Fixture.command(expected,kind);check(expectedResult.ok,"independent compatibility command");
                checkedMain(()->{SessionProbe.install(activity,fixture);activity.refresh();invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.center(new Hex(9,8));});
                renderer=(FilamentMapView)field(host,"spatial");checkedMain(()->renderer.camera.span=3.4f);settle();ready();assetsReady();
                check(((MapSceneSnapshot)field(renderer,"snapshot")).ground.pcMap==null,"custom map has explicit compatibility surface");
                check(!host.sourceVisuals(),"source-only policy excludes custom map");
                World.Result[] command={null};checkedMain(()->{command[0]=SessionProbe.command(activity,w->NativeR11Fixture.command(w,kind));host.pauseCommandEffects(true);});
                check(command[0].ok,"normal compatibility command");
                if(kind.equals("critical"))check(command[0].critical!=null,"normal tactic creates real critical result");
                long end=SystemClock.uptimeMillis()+30000;boolean[] shown={false};
                checkedMain(()->host.pauseCommandEffects(false));
                while(host.commandEffectsActive()&&SystemClock.uptimeMillis()<end){
                    checkedMain(()->{try{
                        boolean visible=false;for(boolean v:(boolean[])field(renderer,"effectShown"))visible|=v;
                        if(((CombatVisual)field(renderer,"combat")).count>0&&visible){host.pauseCommandEffects(true);shown[0]=true;}
                    }catch(Exception e){throw new RuntimeException(e);}});
                    if(shown[0])break;SystemClock.sleep(15);
                }
                check(shown[0],"compatibility effects reach actual displayed GPU entities");assetsReady();
                boolean flag=false;for(Object proxy:((Map<?,?>)field(renderer,"objects")).values())flag|=(Integer)field(proxy,"flag")!=0;
                check(flag,"compatibility map retains authored legacy flag path");
                surfaceCapture();capture("pc-policy-compat-"+kind+"-ui");Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"pc-policy-compat-"+kind+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);
                record(kind+" effects="+((CombatVisual)field(renderer,"combat")).count+" source=false\n"+host.report());
                checkedMain(()->{host.commandEffectSpeed(4);host.pauseCommandEffects(false);});end=SystemClock.uptimeMillis()+30000;
                while(host.commandEffectsActive()&&SystemClock.uptimeMillis()<end)settle();check(!host.commandEffectsActive(),"compatibility normal sequence completes");
                byte[][] actual={null};checkedMain(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
                check(Arrays.equals(SaveCodec.encode(expected),actual[0]),"compatibility full authority/RNG exact");
                check(Arrays.equals(actual[0],Files.readAllBytes(auto.toPath())),"compatibility actual autosave exact");
            }
            checkedMain(()->{host.switchMode(false);host.switchMode(true);});settle();ready();
            check(!host.sourceVisuals(),"compatibility policy survives scene rebuild");
            checkedMain(()->{callActivityOnPause(activity);callActivityOnResume(activity);});settle();ready();
            result.putString("stream","PASS PC VISUAL POLICY compatibility installed checks="+checks+"; normal arrow/critical commands, actual legacy GPU particles/flags, exact save/RNG, rebuild/pause; emulator only\n");
        }catch(Throwable e){result.putString("stream","FAIL PC VISUAL POLICY "+android.util.Log.getStackTraceString(e));}
        finally{
            if(activity!=null)checkedMain(()->host.cancelCommandEffects());
            try{finishActivityForRestore();}catch(Throwable e){result.putString("stream",result.getString("stream")+"FAIL lifecycle restoration barrier "+e);}
            try{if(backed){if(existed){Files.write(auto.toPath(),backup);if(!Arrays.equals(backup,Files.readAllBytes(auto.toPath())))throw new IOException("restored autosave differs");}else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"FAIL restoration "+e);}
            restore(prefs,old);restore(client,oldClient);
        }
        try{if(report!=null)record(result.getString("stream"));}catch(IOException ignored){}
        finish(Activity.RESULT_OK,result);
    }
    private void checkedMain(Runnable action){Throwable[] error={null};super.runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new IllegalStateException("Compatibility verification main thread",error[0]);}
    private void assetsReady()throws Exception{
        long end=SystemClock.uptimeMillis()+30000;boolean[] ready={false};
        while(SystemClock.uptimeMillis()<end){checkedMain(()->{try{ready[0]=!(Boolean)field(renderer,"assetSyncPending")&&((SceneAssetQueue)field(renderer,"assetWork")).pending()==0;}catch(Exception e){throw new RuntimeException(e);}});if(ready[0])break;settle();}
        check(ready[0],"compatibility asset uploads complete");check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"compatibility assets present");
    }
    private void record(String line)throws IOException{Files.write(report.toPath(),(line+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private static void restore(android.content.SharedPreferences p,Map<String,?> old){var e=p.edit().clear();for(var r:old.entrySet()){Object v=r.getValue();String k=r.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));else throw new IllegalArgumentException(k);}e.commit();}
}

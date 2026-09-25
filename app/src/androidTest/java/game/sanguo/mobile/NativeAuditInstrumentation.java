package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.Window;
import android.widget.TextView;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Reproduces the missing modal-window gate on the unchanged R14 production tree.
 * The same test is compiled against baseline and candidate; no new API is required. */
public final class NativeAuditInstrumentation extends SceneInstrumentation {
    private File dir;
    private void note(String text)throws Exception{Files.write(new File(dir,"audit-runtime.txt").toPath(),(text+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private FilamentMapView view()throws Exception{return (FilamentMapView)field(host,"spatial");}
    private void shot(String name)throws Exception{surfaceCapture();capture(name+"-ui");Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);}
    private void modalCycles()throws Exception{
        byte[] saved=SaveCodec.encode(SessionProbe.view(activity));
        FilamentMapView original=view();Dialog[] dialog={null};
        for(int i=0;i<3;i++){
            runOnMainSync(()->{dialog[0]=new Dialog(activity);dialog[0].requestWindowFeature(Window.FEATURE_NO_TITLE);TextView label=new TextView(activity);label.setText("R00–R14 audit: real modal window");dialog[0].setContentView(label);dialog[0].show();dialog[0].getWindow().setLayout(-1,-1);});settle();
            boolean[] stopped={false};long[] frames={0};
            runOnMainSync(()->{try{stopped[0]=!(Boolean)field(original,"queued");frames[0]=(Long)field(original,"surfaceFrames");}catch(Exception e){throw new RuntimeException(e);}});
            note("modal="+i+" coveredCallbackStopped="+stopped[0]+" frames="+frames[0]);
            check(stopped[0],"covered map stops native frame callback");
            settle();runOnMainSync(()->{try{check(frames[0]==(Long)field(original,"surfaceFrames"),"covered window submits no additional native frame");host.resume(false);host.resume(true);check(!(Boolean)field(original,"queued"),"resume cannot bypass modal focus gate");host.resume(false);}catch(Exception e){throw new RuntimeException(e);}});
            runOnMainSync(()->dialog[0].dismiss());settle();
            runOnMainSync(()->{try{check(!(Boolean)field(original,"queued"),"dismiss while owner paused does not restart renderer");host.resume(true);}catch(Exception e){throw new RuntimeException(e);}});ready();
            check(view()==original,"same renderer/resources retained after modal dismissal");
        }
        check(Arrays.equals(saved,SaveCodec.encode(SessionProbe.view(activity))),"modal lifecycle never changes complete authority/RNG/save");
        note("PASS actual modal windows: focus loss, background/resume ordering, resource identity, full save equality");
    }
    private void calendar()throws Exception{
        byte[] saved=SaveCodec.encode(SessionProbe.view(activity));
        for(int month:new int[]{1,7,4}){
            World fixture=SaveCodec.decode(saved);fixture.startMonth=month;fixture.turn=0;
            runOnMainSync(()->{SessionProbe.install(activity,fixture);activity.refresh();world=SessionProbe.view(activity);});ready();
            String[] value={null};runOnMainSync(()->{try{value[0]=((TextView)field(activity,"dateBanner")).getText().toString();check(value[0].equals(world.date().replace(" ","")),"date widget equals active session");check(((MapSceneSnapshot)field(view(),"snapshot")).month==month,"native month equals active session");}catch(Exception e){throw new RuntimeException(e);}});
            note("calendar_fixture="+month+" widget="+value[0]+"; pixels require separate review");shot("audit-calendar-m"+month);
        }
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        World initial=ScenarioCatalog.load("coalition-190",5,20260925L);try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(initial));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
        runOnMainSync(()->{world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.MEDIUM);host.setGridShown(false);host.setTerritoryMode(0);invoke("closePanel",new Class<?>[0]);});ready();
        note("SOURCE="+BuildConfig.SOURCE_REVISION+"; API29 x86_64 software renderer; physical NOT_RUN");modalCycles();calendar();
        note("PASS AUDIT installed checks="+checks+" (includes readiness polls; not independent user actions)");result.putString("stream","PASS AUDIT installed checks="+checks+"\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{note("FAIL "+android.util.Log.getStackTraceString(e));}catch(Exception ignored){}result.putString("stream","FAIL AUDIT "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

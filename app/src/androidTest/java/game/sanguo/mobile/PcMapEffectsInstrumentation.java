package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Normal PC map loading/camera and production visual pause, never injects FX.
 * Pixel/video comparison to PC remains a separate acceptance requirement. */
public final class PcMapEffectsInstrumentation extends SceneInstrumentation {
    private File dir;
    @Override public void onStart() {
        Bundle result=new Bundle();File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");byte[] backup=null;boolean existed=auto.isFile(),backed=false;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);Map<String,?> old=new HashMap<>(prefs.getAll());
        var client=getTargetContext().getSharedPreferences("MainActivity",0);Map<String,?> oldClient=new HashMap<>(client.getAll());
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();File report=new File(dir,"pc-map-effects-report.txt");
        try {
            Files.deleteIfExists(report.toPath());
            Files.deleteIfExists(new File(dir,"pc-map-effects-source-commands.bin").toPath());
            if(existed)backup=Files.readAllBytes(auto.toPath());backed=true;
            World initial=ScenarioCatalog.load("heroes-250",0);byte[] authority=SaveCodec.encode(initial);Files.write(auto.toPath(),authority);
            prefs.edit().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);host.pauseEffects(false);});
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");
            float[][] targets={{878.7166137695312f,3902.0146484375f},{3543.083740234375f,1967.943359375f},{2194.009033203125f,2785.2197265625f}};
            for(int target=0;target<targets.length;target++) {
                int index=target;runOnMainSync(()->{try{MapSceneSnapshot s=(MapSceneSnapshot)field(renderer,"snapshot");renderer.camera.x=targets[index][0]*.05f-28.5f-s.ground.sourceOriginX;renderer.camera.z=targets[index][1]*.05f-28.5f-s.ground.sourceOriginY;renderer.camera.span=8;renderer.camera.yaw=35;renderer.camera.tilt=55;}catch(Exception e){throw new RuntimeException(e);}});
                settle();ready();awaitFx(renderer);
                shot("pc-map-effects-"+target);Files.write(report.toPath(),(host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                PcMapEffects effects=(PcMapEffects)field(renderer,"pcMapEffects");
                long prior=(Long)field(effects,"frames");float clock=(Float)field(effects,"sourceElapsed");SystemClock.sleep(1000);settle();
                check((Long)field(effects,"frames")>prior,"source native updates reach normal GPU map frame");
                check((Float)field(effects,"sourceElapsed")>clock,"normal map source clock advances");
                check((Integer)field(effects,"shown")>0,"original source quads uploaded to production scene");
                check(((String)field(effects,"error")).isEmpty(),"no missing/unsupported source worker/material input");
            }
            PcMapEffects effects=(PcMapEffects)field(renderer,"pcMapEffects");
            runOnMainSync(()->host.pauseEffects(true));SystemClock.sleep(1000);settle();float paused=(Float)field(effects,"sourceElapsed");
            SystemClock.sleep(1000);settle();check(Float.floatToRawIntBits(paused)==Float.floatToRawIntBits((Float)field(effects,"sourceElapsed")),"production pause keeps native source time fixed");
            shot("pc-map-effects-paused");pixelContribution(renderer,effects,report);
            runOnMainSync(()->host.pauseEffects(false));SystemClock.sleep(1000);settle();check((Float)field(effects,"sourceElapsed")>paused,"production resume restarts native clock");
            runOnMainSync(()->callActivityOnPause(activity));check(!(Boolean)field(renderer,"resumed"),"activity pause cancels admitted frames");runOnMainSync(()->callActivityOnResume(activity));settle();ready();awaitFx(renderer);
            byte[][] actual={null};runOnMainSync(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
            check(Arrays.equals(authority,actual[0]),"all source effects/camera/lifecycle preserve full rules/RNG/save");
            PcEffectProcess process=(PcEffectProcess)field(effects,"process");check(process!=null,"normal scene owns separate native visual VM");
            runOnMainSync(()->host.switchMode(false));check(field(renderer,"pcMapEffects")==null,"scene exit clears original effect owner");
            check((Boolean)field(effects,"closed"),"source map effect controller closed");check(((List<?>)field(effects,"entities")).isEmpty(),"source quad entities released");
            check(field(effects,"vertices")==null&&field(effects,"indices")==null&&field(effects,"material")==null,"source effect GPU buffers/material released");
            for(Object image:(Object[])field(effects,"textures"))check(image==null,"source image owner cleared after destroy");
            for(Object blend:(Object[])field(effects,"instances"))for(Object material:(Object[])blend)check(material==null,"source image material owner cleared after destroy");
            check(field(effects,"addMaterial")==null,"source additive material released");
            check((Long)field(effects,"textureBytes")==0&&(Integer)field(effects,"capacity")==0,"source effect resource counters cleared");
            check(((java.util.concurrent.atomic.AtomicBoolean)field(process,"closed")).get(),"native child destruction requested on scene exit");
            result.putString("stream","PASS PC MAP EFFECTS installed checks="+checks+"; normal PC map load/source SEFF, GPU quads, camera, pause/resume/exit, exact authority; PC pixels/MOD/ARM/sustained performance and combat/fullscreen pending\n");
        }catch(Throwable e){result.putString("stream","FAIL PC MAP EFFECTS "+android.util.Log.getStackTraceString(e));try{
            Files.write(report.toPath(),(host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");PcMapEffects effects=(PcMapEffects)field(renderer,"pcMapEffects");
            if(effects!=null){PcEffectProcess worker=(PcEffectProcess)field(effects,"process");if(worker!=null){byte[] commands=worker.sourceCommands();if(commands!=null)Files.write(new File(dir,"pc-map-effects-source-commands.bin").toPath(),commands);}}
            capture("pc-map-effects-failed");
        }catch(Exception ignored){}}
        finally {
            try{finishActivityForRestore();}catch(Throwable e){result.putString("stream",result.getString("stream")+"FAIL lifecycle restoration "+e);}
            try{if(backed){if(existed){Files.write(auto.toPath(),backup);if(!Arrays.equals(backup,Files.readAllBytes(auto.toPath())))throw new IOException("actual saved bytes differ");}else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"FAIL autosave restoration "+e);}
            prefs.edit().clear().commit();var a=prefs.edit();for(var e:old.entrySet())restore(a,e);a.commit();client.edit().clear().commit();var b=client.edit();for(var e:oldClient.entrySet())restore(b,e);b.commit();
        }
        try{Files.write(report.toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}
        finish(Activity.RESULT_OK,result);
    }
    private void awaitFx(FilamentMapView renderer)throws Exception {
        long end=SystemClock.uptimeMillis()+30000;
        while(SystemClock.uptimeMillis()<end){PcMapEffects effects=(PcMapEffects)field(renderer,"pcMapEffects");if(effects!=null){String error=(String)field(effects,"error");check(error.isEmpty(),"original map effect runtime error "+error);if((Long)field(effects,"frames")>=3&&(Integer)field(effects,"shown")>0)return;}settle();}
        throw new AssertionError("Original map effect source/GPU readiness30s "+host.report());
    }
    private void shot(String name)throws Exception{surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(name+"-ui");}
    /** Remove only the already running production source entities. No emitter,
     * material, camera, depth state or source data is substituted for this probe. */
    private void pixelContribution(FilamentMapView renderer,PcMapEffects effects,File report)throws Exception {
        int count=(Integer)field(effects,"shown");check(count>0,"paused production source entities available for pixel proof");
        com.google.android.filament.Scene scene=(com.google.android.filament.Scene)field(renderer,"scene");
        List<Integer> entities=new ArrayList<>(((List<Integer>)field(effects,"entities")).subList(0,count));
        shot("pc-map-effects-pixels-with");int[] with=pixels("pc-map-effects-pixels-with");
        try {
            runOnMainSync(()->{for(int entity:entities)scene.removeEntity(entity);});settle();
            shot("pc-map-effects-pixels-without");
        }finally{runOnMainSync(()->{for(int entity:entities)scene.addEntity(entity);});}
        settle();shot("pc-map-effects-pixels-restored");
        int[] without=pixels("pc-map-effects-pixels-without"),restored=pixels("pc-map-effects-pixels-restored");
        check(with.length==without.length&&with.length==restored.length,"same normal GPU surface dimensions");
        int changed=0,drift=0;for(int i=0;i<with.length;i++) {
            if(delta(with[i],restored[i])>3)drift++;
            if(delta(with[i],without[i])>8&&delta(restored[i],without[i])>8&&delta(with[i],restored[i])<=3)changed++;
        }
        Files.write(report.toPath(),("\nsource_pixel_probe changed="+changed+" drift="+drift+" total="+with.length+" entities="+count+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        check(drift==0,"paused source/background pixels stable after entity restoration");
        check(changed>=8,"original production source entities contribute reproducible GPU pixels");
        check((Integer)field(effects,"shown")==count,"pixel probe retains source scene count");
        check(((String)field(effects,"error")).isEmpty(),"pixel probe preserves native source worker");
    }
    private int[] pixels(String name){android.graphics.Bitmap b=android.graphics.BitmapFactory.decodeFile(new File(dir,name+".png").getAbsolutePath());if(b==null)throw new AssertionError("Pixel probe capture "+name);try{int[] p=new int[b.getWidth()*b.getHeight()];b.getPixels(p,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return p;}finally{b.recycle();}}
    private static int delta(int a,int b){return Math.max(Math.abs(((a>>16)&255)-((b>>16)&255)),Math.max(Math.abs(((a>>8)&255)-((b>>8)&255)),Math.abs((a&255)-(b&255))));}
    private static void restore(android.content.SharedPreferences.Editor editor,Map.Entry<String,?> entry){Object v=entry.getValue();if(v instanceof String)editor.putString(entry.getKey(),(String)v);else if(v instanceof Boolean)editor.putBoolean(entry.getKey(),(Boolean)v);else if(v instanceof Integer)editor.putInt(entry.getKey(),(Integer)v);else if(v instanceof Long)editor.putLong(entry.getKey(),(Long)v);else if(v instanceof Float)editor.putFloat(entry.getKey(),(Float)v);else if(v instanceof Set)editor.putStringSet(entry.getKey(),(Set<String>)v);}
}

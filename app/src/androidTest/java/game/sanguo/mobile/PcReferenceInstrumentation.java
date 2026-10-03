package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Actual normal September scenario and production renderer, measured source
 * camera. Captures are evidence, not an assertion of pixel parity or official
 * opening-state equivalence (chibi-207 is an authored reconstruction). */
public final class PcReferenceInstrumentation extends SceneInstrumentation {
    @Override public void onStart(){
        Bundle result=new Bundle();File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();File report=new File(dir,"pc-reference-report.txt");
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");byte[] backup=null;boolean existed=auto.isFile(),backed=false;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);Map<String,?> old=new HashMap<>(prefs.getAll());
        var client=getTargetContext().getSharedPreferences("MainActivity",0);Map<String,?> oldClient=new HashMap<>(client.getAll());
        try{
            Files.deleteIfExists(report.toPath());if(existed)backup=Files.readAllBytes(auto.toPath());backed=true;
            World w=ScenarioCatalog.load("chibi-207",0);byte[] authority=SaveCodec.encode(w);Files.write(auto.toPath(),authority);
            check(w.life.month()==9&&w.life.year()==207,"normal catalog September207 source season");prefs.edit().putString("quality","HIGH").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);host.pauseEffects(false);});
            FilamentMapView view=(FilamentMapView)field(host,"spatial");
            runOnMainSync(()->{try{
                MapSceneSnapshot s=(MapSceneSnapshot)field(view,"snapshot");check(s.ground.pcMap!=null,"normal original PC terrain");
                view.camera.x=2310*.05f-28.5f-s.ground.sourceOriginX;view.camera.z=2760*.05f-28.5f-s.ground.sourceOriginY;
                view.camera.span=(float)(410*.05*Math.tan(.5235987901687622*.5));view.camera.yaw=45;view.camera.tilt=45;
            }catch(Exception e){throw new RuntimeException(e);}});settle();ready();
            long end=SystemClock.uptimeMillis()+30000;boolean loaded=false;
            while(SystemClock.uptimeMillis()<end){PcMapEffects fx=(PcMapEffects)field(view,"pcMapEffects");if(fx!=null){check(((String)field(fx,"error")).isEmpty(),"native source scene no error");if((Long)field(fx,"frames")>=3&&(Integer)field(fx,"shown")>0){loaded=true;break;}}settle();}
            check(loaded,"actual map source effects ready within30s");
            com.google.android.filament.Material ground=(com.google.android.filament.Material)field(view,"pcGroundMaterial");
            check(ground.getShading()==com.google.android.filament.Material.Shading.UNLIT,"original ground does not use generic PBR light");
            for(String parameter:new String[]{"pcNormal","pcPaint","pcAmbient","pcFog","pcFade","pcFogColor"})check(ground.hasParameter(parameter),"compiled source ground parameter "+parameter);
            for(String texture:new String[]{"pcColor","pcPalette","pcGroundNormal","pcGroundPaint","pcGroundOutline"})check(((com.google.android.filament.Texture)field(view,texture)).getFormat()==com.google.android.filament.Texture.InternalFormat.RGBA8,"numeric/encoded source texture upload "+texture);
            check((Integer)field(view,"pcFogMonth")==9,"actual reference source fog bound");
            check(field(view,"pcGroundOutlineMaterial")!=null,"compiled source ground outline");
            for(int i=0;i<3;i++){
                surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"pc-reference-xinye-autumn-"+i+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture("pc-reference-xinye-autumn-"+i+"-ui");
                Files.write(report.toPath(),("CAPTURE "+i+" target_source=2310,16,2760 eye_distance_source=410 yaw=45 tilt=45 source_fov_rad=.5235987901687622\n"+host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);SystemClock.sleep(5000);
            }
            byte[][] actual={null};runOnMainSync(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
            check(Arrays.equals(authority,actual[0]),"reference camera/capture preserve whole authority/RNG/save");check(((Set<?>)field(view,"missingAssets")).isEmpty(),"no silent approximate asset fallback");
            runOnMainSync(()->host.switchMode(false));check(field(view,"pcMapEffects")==null,"reference exit releases native effects");
            result.putString("stream","PASS PC REFERENCE checks="+checks+"; normal207 September scenario, source camera and production GPU; authored opening states differ, PC pixel/material/timing parity pending; emulator only\n");
        }catch(Throwable e){result.putString("stream","FAIL PC REFERENCE "+android.util.Log.getStackTraceString(e));try{capture("pc-reference-failed");}catch(Exception ignored){}}
        finally{
            try{finishActivityForRestore();}catch(Throwable e){result.putString("stream",result.getString("stream")+"FAIL lifecycle restoration "+e);}
            try{if(backed){if(existed){Files.write(auto.toPath(),backup);if(!Arrays.equals(backup,Files.readAllBytes(auto.toPath())))throw new IOException("restored SG11 differs");}else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"FAIL save restoration "+e);}
            restore(prefs,old);restore(client,oldClient);
        }
        try{Files.write(report.toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}finish(Activity.RESULT_OK,result);
    }
    private static void restore(android.content.SharedPreferences prefs,Map<String,?> old){var e=prefs.edit().clear();for(var r:old.entrySet()){Object v=r.getValue();String k=r.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));else throw new IllegalArgumentException(k);}e.commit();}
}

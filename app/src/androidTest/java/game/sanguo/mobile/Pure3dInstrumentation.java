package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Same installed source snapshot, real Surface/UI captures and authority byte guard. */
public final class Pure3dInstrumentation extends SceneInstrumentation {
    private Bundle args;
    @Override public void onCreate(Bundle value){args=value==null?new Bundle():value;super.onCreate(value);}
    @Override public void onStart(){Bundle result=new Bundle();try{
        World seed=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(seed);
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(before);}
        // Controlled camera/quality comparison is a fixture preference, restored externally.
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("nativeFailure",false).putString("quality","MEDIUM").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");world=SessionProbe.view(activity);
        runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);});
        ready();FilamentMapView view=(FilamentMapView)field(host,"spatial");
        File dir=getTargetContext().getExternalFilesDir("s01");String prefix=args.getString("run","coast");
        boolean nationalSweep=Boolean.parseBoolean(args.getString("nationalSweep","false"));
        String[] sites=nationalSweep
            ?new String[]{"下邳","海陵港","小沛","襄平","北平","北海","建業","吳","會稽","柴桑","江夏","永安","成都","建寧","雲南"}
            :new String[]{"下邳","海陵港","小沛"};
        for(String name:sites){
            World.City city=null;for(World.City c:world.cities)if(c.name.equals(name))city=c;
            check(city!=null,"real source site "+name);final Hex h=city.hex;
            for(int orientation=0;orientation<4;orientation++){
                final int facing=(orientation&1)==0?1:-1;final float span=orientation<2?5:15;
                runOnMainSync(()->{if(nationalSweep){activity.selectAndFocus(h);invoke("closePanel",new Class<?>[0]);}else host.focus(h);view.camera.span=span;view.camera.facing=facing;view.camera.yaw=0;view.camera.tilt=55;});settle();ready();
                surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,prefix+"-"+name+"-"+span+"-"+facing+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(prefix+"-"+name+"-"+span+"-"+facing+"-ui");
                Files.write(new File(dir,prefix+"-report.txt").toPath(),(name+" span="+span+" facing="+facing+"\n"+sceneReport()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                check(((MapSceneSnapshot)field(view,"snapshot")).ground.pcMap!=null,"original PC geometry retained");
                check(((Set<?>)field(view,"missingAssets")).isEmpty(),"no missing source assets");
                if(nationalSweep){Object overlay=field(view,"overlay");
                    check(!((android.graphics.Path)field(overlay,"siteSelectionPath")).isEmpty(),"selected source site has a merged cell outline");
                    check((Long)field(overlay,"siteSelectionFailures")==0,"source selected cell union succeeds without internal shared edges");
                }
            }
        }
        byte[][] captured={null};runOnMainSync(()->{try{captured[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
        check(Arrays.equals(before,captured[0]),"camera/terrain viewing preserves full authority and RNG");
        result.putString("stream","PASS PURE3D coast captures checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL PURE3D "+android.util.Log.getStackTraceString(e));try{capture("pure3d-failed");}catch(Exception ignored){}}
    try{finishActivityForRestore();}catch(Exception e){result.putString("stream","FAIL teardown "+e);}
    finish(Activity.RESULT_OK,result);}
}

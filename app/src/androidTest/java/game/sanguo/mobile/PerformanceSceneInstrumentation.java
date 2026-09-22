package game.sanguo.mobile;
import android.app.*;
import android.content.*;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;
/** Native resource and resolution checks. Timing on emulator is not a device threshold. */
public final class PerformanceSceneInstrumentation extends SceneInstrumentation {
    @Override public void onStart(){Bundle result=new Bundle();try{
        world=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(world);
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(before);}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("nativeSession",false).commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");world=(World)field(activity,"world");before=SaveCodec.encode(world);
        File out=new File(getTargetContext().getExternalFilesDir("s01"),"s08-native.tsv");out.getParentFile().mkdirs();
        try(PrintWriter log=new PrintWriter(out)){
            log.println("cycle\tquality\tpss_kib\tjava_used_bytes\tnative_heap_bytes\tdiagnostics");
            for(int i=0;i<20;i++){
                SceneQuality q=SceneQuality.values()[i%3];
                runOnMainSync(()->{host.quality(q);host.switchMode(true);host.focus(world.home().hex);});settle();ready();
                FilamentMapView view=(FilamentMapView)field(host,"spatial");
                int width=(Integer)field(view,"bufferWidth"),height=(Integer)field(view,"bufferHeight");
                check(width==Math.round(host.getWidth()*q.scale)&&height==Math.round(host.getHeight()*q.scale),"actual fixed Surface resolution "+q);
                check(view.camera.width==host.getWidth()&&view.camera.height==host.getHeight(),"input projection remains UI resolution");
                check(((com.google.android.filament.Texture)field(view,"siteAtlas")).getFormat()==com.google.android.filament.Texture.InternalFormat.ETC2_SRGB8,"actual texture upload format");
                check(((com.google.android.filament.Texture)field(view,"fieldAtlas")).getFormat()==com.google.android.filament.Texture.InternalFormat.ETC2_SRGB8,"field texture upload format");
                if(i<3){capture("s08-"+q);surfaceCapture();}
                runOnMainSync(host::fit);settle();ready();runOnMainSync(()->host.focus(world.home().hex));settle();
                Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);
                String[] report={""};runOnMainSync(()->report[0]=host.report().replace('\n',' '));
                log.println(i+"\t"+q+"\t"+memory.getTotalPss()+"\t"+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())+"\t"+Debug.getNativeHeapAllocatedSize()+"\t"+report[0]);log.flush();
                check(Arrays.equals(before,SaveCodec.encode(world)),"quality/camera does not change strategic save");
                runOnMainSync(()->host.switchMode(false));settle();
                check((Boolean)field(view,"released")&&field(view,"engine")==null,"native engine released");
                check(((java.util.concurrent.ExecutorService)field(view,"worker")).isShutdown(),"mesh worker shutdown");
            }
        }
        result.putString("stream","PASS S08 "+checks+" checks; 20 lifecycle cycles; performance is NOT physical-device acceptance\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL S08 "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

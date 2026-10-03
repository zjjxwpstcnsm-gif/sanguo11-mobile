package game.sanguo.mobile;
import android.app.*;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Same MainActivity/MapHost and original 120s readiness; captures raw Surface/UI. */
public final class NativeLandmarks126Instrumentation extends SceneInstrumentation {
    private String mode="hukou";private File dir;
    @Override public void onCreate(Bundle b){if(b!=null)mode=b.getString("mode",mode);super.onCreate(b);}
    private void log(String text)throws Exception{Files.write(new File(dir,"landmarks126.txt").toPath(),(text+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private byte[] authority(){byte[][] bytes={null};runOnMainSync(()->{try{bytes[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return bytes[0];}
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        World seed;
        seed=ScenarioCatalog.all().get(0);
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).putString("quality","MEDIUM").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");world=SessionProbe.view(activity);
        {
            int[] p=mode.equals("hukou")?new int[]{66,58}:mode.equals("wall")?new int[]{92,15}:mode.equals("southwest")?new int[]{31,183}:new int[]{147,59};
            Hex focus=MapCoordinates.fromNationalSource(world,new SourceGridCoord(p[0],p[1]));byte[] before=authority();
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.setGridShown(false);host.setTerritoryMode(0);activity.selectAndFocus(focus);host.switchMode(true);});settle();
            check(host.is3D(),"requested native renderer initialized without fallback");FilamentMapView renderer=(FilamentMapView)field(host,"spatial");check(renderer!=null,"native renderer exists");runOnMainSync(()->{renderer.center(focus);renderer.camera.span=6;renderer.camera.tilt=55;});ready();
            for(int span:new int[]{3,6,10})for(int yaw:new int[]{0,180}){
                runOnMainSync(()->{renderer.camera.span=span;renderer.camera.yaw=yaw;});settle();ready();
                for(boolean grid:new boolean[]{false,true}){
                    runOnMainSync(()->host.setGridShown(grid));settle();surfaceCapture();String id=mode+"-span"+span+"-yaw"+yaw+"-grid"+grid;
                    Files.copy(new File(dir,"surface.png").toPath(),new File(dir,id+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(id+"-ui");
                }
            }
            int water=0;for(Object gpu:((Map<?,?>)field(renderer,"vegetation")).values()){
                SceneMesh mesh=(SceneMesh)field(gpu,"source");for(int i=0;i<mesh.uv.length;i+=2)if(mode.equals("wall")?mesh.vertices[i/2*7+3]==.98f&&mesh.vertices[i/2*7+4]==.87f&&mesh.vertices[i/2*7+5]==.69f:mode.equals("hukou")?mesh.uv[i]>.375f&&mesh.uv[i]<.5f:mesh.uv[i]>.625f&&mesh.uv[i]<.75f)water++;
            }
            check(water>0,"actual production GPU landscape contains requested landmark geometry");
            check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no fallback");
            check(Arrays.equals(before,authority()),"all map views preserve entire game and RNG");
            Files.write(new File(dir,"expected.sg11").toPath(),before);Files.write(new File(dir,"observed.sg11").toPath(),authority());
            log("GPU landmarkVertices="+water+" source="+Arrays.toString(p)+"\n"+host.report());
        }
        log("PASS LANDMARK126 mode="+mode+" checks="+checks);result.putString("stream","PASS LANDMARK126 "+mode+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL LANDMARK126 "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture(mode+"-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

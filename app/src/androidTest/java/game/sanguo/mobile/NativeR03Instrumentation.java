package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.MotionEvent;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Surface evidence from the normal official scenario, not a sample Activity. */
public final class NativeR03Instrumentation extends SceneInstrumentation {
    private FilamentMapView spatial()throws Exception{return (FilamentMapView)field(host,"spatial");}
    private void shot(String name)throws Exception{
        surfaceCapture();capture(name+"-ui");File dir=getTargetContext().getExternalFilesDir("s01");
        java.nio.file.Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        try(FileWriter out=new FileWriter(new File(dir,name+".txt"))){out.write("source="+BuildConfig.SOURCE_REVISION+"\n"+host.report());}
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",0));
        long end=SystemClock.uptimeMillis()+90000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<end)settle();
        host=(MapHost)field(activity,"map");world=(World)field(activity,"world");check(world!=null,"official scenario loaded");
        runOnMainSync(()->{host.switchMode(true);host.quality(SceneQuality.MEDIUM);activity.selectAndFocus(world.home().hex);});ready();
        FilamentMapView v=spatial();runOnMainSync(()->{v.camera.span=10;v.camera.tilt=55;v.camera.yaw=0;v.setGridShown(false);});settle();ready();shot("r03-home-baseline-comparable");
        MapSceneSnapshot.Ground g=((MapSceneSnapshot)field(v,"snapshot")).ground;
        check(Math.abs(v.camera.x-g.grid.x(world.home().hex))<.001f&&Math.abs(v.camera.z-g.grid.z(world.home().hex))<.001f,"initial city focus survives first streamed window acceptance");
        Hex mountain=null;float best=-1;
        // Real official-map ridge nearest a chunk seam, chosen deterministically, never edited.
        for(int r=16;r<g.height-16;r+=16)for(int q=16;q<g.width-16;q+=16){Hex h=new Hex(q,r);if(g.valid(h)&&g.surface.at(h)>best){best=g.surface.at(h);mountain=h;}}
        final Hex target=mountain;check(target!=null,"real cross-chunk region");
        for(float span:new float[]{8,24,70})for(float yaw:new float[]{0,90}){
            runOnMainSync(()->{v.center(target);v.camera.span=span;v.camera.yaw=yaw;v.camera.tilt=55;v.setGridShown(false);});settle();ready();
            shot("r03-seam-"+(int)span+"-"+(int)yaw+"-grid-off");
            @SuppressWarnings("unchecked") List<SceneMesh> chunks=(List<SceneMesh>)field(v,"chunks");
            check(!chunks.isEmpty(),"streamed terrain nonempty");for(SceneMesh m:chunks)check(m.distant==null,"one CPU LOD per chunk");
        }
        runOnMainSync(()->{v.center(target);v.camera.span=8;v.camera.yaw=0;v.setGridShown(true);});settle();ready();shot("r03-seam-grid-on");
        runOnMainSync(()->v.setGridShown(false));
        for(int step=0;step<4;step++){
            runOnMainSync(()->{long t=SystemClock.uptimeMillis();float x=v.getWidth()*.4f,y=v.getHeight()*.4f;
                for(int i=0;i<=12;i++){int action=i==0?MotionEvent.ACTION_DOWN:i==12?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;MotionEvent e=MotionEvent.obtain(t,t+i*25,action,x+i*15,y+i*5,0);v.onTouchEvent(e);e.recycle();}});
            settle();ready();shot("r03-drag-"+step);
        }
        commandFlow();shot("r03-command-result");
        result.putString("stream","PASS R03 "+checks+" installed checks; software emulator only\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{capture("r03-failure");}catch(Throwable ignored){}result.putString("stream","FAIL R03 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

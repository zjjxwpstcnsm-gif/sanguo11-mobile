package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Actual normal-game host, Surface captures, gestures and independent authoritative comparison. */
public final class NativeR02Instrumentation extends SceneInstrumentation {
    @Override void check(boolean value,String label){super.check(value,label);android.util.Log.i("NativeR02","PASS "+label);}
    private FilamentMapView spatial()throws Exception{return (FilamentMapView)field(host,"spatial");}
    private byte[] authority(){final byte[][] b={null};runOnMainSync(()->{try{b[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return b[0];}
    private void shot(String name)throws Exception{
        surfaceCapture();capture(name+"-ui");
        File dir=getTargetContext().getExternalFilesDir("s01");
        java.nio.file.Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    private void touch(FilamentMapView v,float x,float y,float dx,float dy,boolean hold){
        long t=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x,y,0);v.onTouchEvent(e);e.recycle();
        if(dx!=0||dy!=0){for(int i=1;i<=5;i++){e=MotionEvent.obtain(t,t+i*40,MotionEvent.ACTION_MOVE,x+dx*i/5,y+dy*i/5,0);v.onTouchEvent(e);e.recycle();}}
        e=MotionEvent.obtain(t,t+250,MotionEvent.ACTION_UP,x+dx,y+dy,0);v.onTouchEvent(e);e.recycle();
    }
    private void twist(FilamentMapView v){
        long t=SystemClock.uptimeMillis();float x=v.getWidth()*.4f,y=v.getHeight()*.4f;
        MotionEvent first=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x-70,y,0);v.onTouchEvent(first);first.recycle();
        MotionEvent.PointerProperties[] pp={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};
        for(int i=0;i<2;i++){pp[i].id=i;pp[i].toolType=MotionEvent.TOOL_TYPE_FINGER;}
        for(int step=0;step<=7;step++){
            double a=Math.toRadians(step*5);MotionEvent.PointerCoords[] pc={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};
            for(int i=0;i<2;i++){float sign=i==0?-1:1;pc[i].x=x+sign*70*(float)Math.cos(a);pc[i].y=y+sign*70*(float)Math.sin(a);pc[i].pressure=1;pc[i].size=1;}
            int action=step==0?MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT):step==7?MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT):MotionEvent.ACTION_MOVE;
            MotionEvent e=MotionEvent.obtain(t,t+20+step*35,action,2,pp,pc,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);v.onTouchEvent(e);e.recycle();
        }
        MotionEvent last=MotionEvent.obtain(t,t+350,MotionEvent.ACTION_UP,x-70,y,0);v.onTouchEvent(last);last.recycle();
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",0));
        long limit=SystemClock.uptimeMillis()+90000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<limit)settle();
        host=(MapHost)field(activity,"map");world=(World)field(activity,"world");check(world!=null,"normal official scenario");
        runOnMainSync(()->{host.switchMode(true);activity.selectAndFocus(world.home().hex);});ready();shot("r02-01-entry");
        byte[] saved=authority();FilamentMapView v=spatial();MapSceneSnapshot snapshot=(MapSceneSnapshot)field(v,"snapshot");
        for(float span:new float[]{3,10,30})for(float yaw:new float[]{0,90,180,270})for(float tilt:new float[]{40,55,70}){
            runOnMainSync(()->{v.camera.span=span;v.camera.yaw=yaw;v.camera.tilt=tilt;});settle();
            for(Hex h:SiteFootprint.cells(world.home())){
                float x=snapshot.ground.grid.x(h),z=snapshot.ground.grid.z(h);
                final Hex[] pick={null};runOnMainSync(()->pick[0]=v.pick(v.camera.screenX(x,z),v.camera.screenY(x,z,0),false));
                check(h.equals(pick[0]),"installed 3x4x3 seven-cell terrain pick "+h);
            }
            if(span==10&&tilt==55)shot("r02-view-"+(int)yaw);
        }
        runOnMainSync(()->{v.resetOrientation();host.focus(world.home().hex);});settle();
        float oldYaw=v.camera.yaw;runOnMainSync(()->twist(v));settle();check(v.camera.yaw!=oldYaw,"real two-finger twist updates camera");
        runOnMainSync(()->touch(v,v.getWidth()*.4f,v.getHeight()*.4f,90,70,false));settle();
        check(Arrays.equals(saved,authority()),"twist/drag never issue gameplay command");shot("r02-02-gestures");
        Hex selected=(Hex)field(activity,"selected");float oldX=v.camera.x,oldZ=v.camera.z;
        runOnMainSync(()->{host.setPanelOcclusion(140,120);touch(v,v.getWidth()-25,v.getHeight()*.5f,-50,30,false);});settle();
        check(oldX==v.camera.x&&oldZ==v.camera.z,"panel drag cannot move camera");check(Objects.equals(selected,field(activity,"selected")),"panel tap cannot select");
        runOnMainSync(()->host.setPanelOcclusion(0,0));
        Throwable captureFailure=null;
        for(SceneQuality quality:SceneQuality.values()){
            runOnMainSync(()->host.quality(quality));ready();FilamentMapView next=spatial();
            check(Math.abs(next.camera.yaw-v.camera.yaw)<.001f,"quality recreates native orientation");
            runOnMainSync(()->host.focus(world.home().hex));settle();
            final Hex[] pick={null};runOnMainSync(()->pick[0]=next.pick(next.camera.width*.5f,next.camera.height*.5f,false));
            check(world.home().hex.equals(pick[0]),"internal resolution touch invariant "+quality.scale);try{shot("r02-resolution-"+quality);}catch(AssertionError error){captureFailure=error;android.util.Log.e("NativeR02","Quality capture failed; remaining independent command tests still run",error);}
        }
        runOnMainSync(()->host.quality(SceneQuality.MEDIUM));ready();
        FilamentMapView restored=spatial();float yaw=restored.camera.yaw,tilt=restored.camera.tilt,span=restored.camera.span;
        runOnMainSync(()->host.switchMode(false));settle();capture("r02-03-2d");check(Arrays.equals(saved,authority()),"2D retains authority/RNG");
        runOnMainSync(()->host.switchMode(true));ready();check(spatial().camera.yaw==yaw&&spatial().camera.tilt==tilt&&spatial().camera.span==span,"2D/3D restores full orientation/span");
        check(Arrays.equals(saved,authority()),"all camera/quality/mode operations leave authority identical");
        // Existing independent deployment/movement/turn/autosave equivalence, not merely picking maths.
        commandFlow();shot("r02-04-command-result");
        try(OutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir("s01"),"r02.txt"))){out.write(("source="+BuildConfig.SOURCE_REVISION+"\nchecks="+checks+"\n"+host.report()).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        if(captureFailure!=null)throw new AssertionError("Quality Surface gate failed; command/mode checks completed",captureFailure);
        result.putString("stream","PASS R02 "+checks+" installed checks; emulator only\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable error){try{capture("r02-failure");}catch(Throwable ignored){}result.putString("stream","FAIL R02 "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}

package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Uses the production startScenario/loadSlot handlers, never a demo World or test Activity. */
public final class NativeR00Instrumentation extends SceneInstrumentation {
    private String mode;
    @Override void capture(String name)throws Exception{super.capture(mode+"-"+name);}
    @Override void check(boolean value,String label){super.check(value,label);android.util.Log.i("NativeR00",mode+" PASS "+label);}
    @Override public void onCreate(Bundle args){mode=args==null?null:args.getString("mode");super.onCreate(args);}
    private byte[] authority(){final byte[][] bytes={null};runOnMainSync(()->{try{bytes[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return bytes[0];}
    private void bind()throws Exception{host=(MapHost)field(activity,"map");world=(World)field(activity,"world");check(host!=null&&world!=null,"normal game UI/session loaded");}
    private void shot(String name)throws Exception{
        ready();FilamentMapView spatial=(FilamentMapView)field(host,"spatial");
        check((Integer)field(spatial,"visibleChunks")>0,"visible terrain chunks");
        check((Integer)field(spatial,"visibleObjects")>0,"visible map objects");
        // PixelCopy captures a completed Surface while uploads continue. Pausing before
        // surfaceCapture() can deadlock its readiness check when the viewport/LOD changes.
        surfaceCapture();
        // Pause only AFTER readiness/PixelCopy, so reporting cannot race cache mutation.
        runOnMainSync(()->spatial.resume(false));
        try{
            android.graphics.Bitmap pixels=android.graphics.BitmapFactory.decodeFile(new File(getTargetContext().getExternalFilesDir("s01"),"surface.png").getAbsolutePath());
            Set<Integer> colors=new HashSet<>();for(int y=0;y<pixels.getHeight();y+=8)for(int x=0;x<pixels.getWidth();x+=8)colors.add(pixels.getPixel(x,y));pixels.recycle();
            check(colors.size()>64,"Surface is not ordinary colored background (not art approval)");
            java.nio.file.Files.copy(new File(getTargetContext().getExternalFilesDir("s01"),"surface.png").toPath(),new File(getTargetContext().getExternalFilesDir("s01"),mode+"-"+name+"-surface.png").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);capture(name+"-ui");
            try(OutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir("s01"),mode+"-"+name+"-scene.txt"))){out.write((host.report()+"\nscenario="+world.scenarioId+" map="+world.mapId+" revision="+world.mapRevision).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        }finally{runOnMainSync(()->spatial.resume(true));}
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();capture("00-launch");
        if(!"reload".equals(mode)){
            check(field(activity,"world")==null,"clean installation starts at normal menu");
            // Normal asynchronous scenario + custom officer composition + GameSession install.
            runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",0));
            long limit=SystemClock.uptimeMillis()+90000;
            while(field(activity,"map")==null&&SystemClock.uptimeMillis()<limit)settle();bind();capture("01-new-game-2d");
        }else{bind();check(new File(getTargetContext().getFilesDir(),"auto.sg11").isFile(),"existing autosave cold loaded");}
        byte[] before=authority();
        runOnMainSync(()->{host.switchMode(true);activity.selectAndFocus(world.home().hex);});settle();ready();shot("02-native-map");
        check(Arrays.equals(before,authority()),"2D/3D retains full authority and RNG");
        FilamentMapView spatial=(FilamentMapView)field(host,"spatial");
        float oldX=spatial.camera.x,oldZ=spatial.camera.z,oldSpan=spatial.camera.span;
        runOnMainSync(()->{
            float x=spatial.getWidth()*.4f,y=spatial.getHeight()*.4f;long t=SystemClock.uptimeMillis();
            for(int i=0;i<5;i++){MotionEvent e=MotionEvent.obtain(t,t+i*40,i==0?0:i==4?1:2,x+i*15,y+i*10,0);spatial.onTouchEvent(e);e.recycle();}
            for(int i=0;i<4;i++){MotionEvent e=MotionEvent.obtain(t+500+(i/2)*120,t+500+i*60,i%2==0?0:1,x,y,0);spatial.onTouchEvent(e);e.recycle();}
        });settle();check(spatial.camera.x!=oldX||spatial.camera.z!=oldZ,"drag changes actual camera");check(spatial.camera.span!=oldSpan,"double-tap zoom changes actual camera");shot("03-gesture");
        if(!"reload".equals(mode)){
            int site=-1,officer=-1;for(World.City c:world.cities)if(c.owner==world.player)for(World.Officer o:world.idle(c)){
                World copy=SaveCodec.decode(before);if(copy.deploy(c.id,o.id,World.Weapon.SWORD,1000).ok){site=c.id;officer=o.id;break;}if(site>=0)break;
            }
            check(site>=0,"normal deployment available");final int c=site,o=officer;
            World reference=SaveCodec.decode(before);check(reference.deploy(c,o,World.Weapon.SWORD,1000).ok,"reference deployment");
            runOnMainSync(()->check(SessionProbe.command(activity,w->w.deploy(c,o,World.Weapon.SWORD,1000)).ok,"session deployment"));bind();
            check(Arrays.equals(SaveCodec.encode(reference),authority()),"rendered deployment equals unchanged rules");
            int id=world.units.get(world.units.size()-1).id;runOnMainSync(()->activity.selectUnitAndFocus(id));settle();shot("04-site-and-unit");
        }
        byte[] saved=authority();runOnMainSync(()->invoke("save",new Class<?>[]{String.class,boolean.class},"manual",false));
        runOnMainSync(()->invoke("loadSlot",new Class<?>[]{String.class},"manual"));settle();bind();check(Arrays.equals(saved,authority()),"normal manual save/load exact");shot("05-loaded");
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);runOnMainSync(activity::recreate);
        Activity recreated=monitor.waitForActivityWithTimeout(30000);removeMonitor(monitor);check(recreated instanceof MainActivity,"Activity recreation");activity=(MainActivity)recreated;settle();bind();
        check(Arrays.equals(saved,authority()),"recreation authority unchanged");check(host.is3D(),"recreation restores native mode");shot("06-recreated");
        try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME"))){while(in.read()!=-1){}}settle();
        try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("am start -W -f 0x10020000 -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity"))){while(in.read()!=-1){}}settle();shot("07-resumed");
        check(Arrays.equals(saved,authority()),"background recovery authority unchanged");
        try(OutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir("s01"),"r00-"+mode+".txt"))){out.write(("source="+BuildConfig.SOURCE_REVISION+"\n"+host.report()+"\nchecks="+checks).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        // Leave a valid autosave. The workflow force-stops and cold launches again separately.
        runOnMainSync(()->invoke("save",new Class<?>[]{String.class,boolean.class},"auto",false));
        result.putString("stream","PASS R00 "+mode+" "+checks+" installed checks; emulator only\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable error){try{capture("r00-failure");}catch(Throwable ignored){}result.putString("stream","FAIL R00 "+mode+" "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}

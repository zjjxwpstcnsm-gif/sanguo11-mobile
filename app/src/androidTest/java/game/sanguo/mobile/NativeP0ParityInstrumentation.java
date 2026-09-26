package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.widget.TextView;
import game.sanguo.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Explicit legal fixtures on the installed production session, not the cold-touch walkthrough.
 * Same saved bytes, legal attack and next-turn sequence in all six combinations.
 * Screens are original full-window captures; text observations alone are not visual acceptance. */
public final class NativeP0ParityInstrumentation extends SceneInstrumentation {
    private File dir;
    private String oldScale;
    private int cases;
    private void note(String s)throws Exception{
        Files.write(new File(dir,"p0-parity.txt").toPath(),(s+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        android.util.Log.i("P0Parity",s);
    }
    private String shell(String command)throws Exception{
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(command))){return new String(in.readAllBytes(),StandardCharsets.UTF_8).trim();}
    }
    private String digest(byte[] bytes)throws Exception{
        StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();
    }
    private byte[] authority()throws Exception{
        byte[][] result={null};runOnMainSync(()->{try{result[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception e){throw new RuntimeException(e);}});return result[0];
    }
    private void exact(byte[] expected,String name,boolean saved)throws Exception{
        byte[] actual=authority();Files.write(new File(dir,name+"-observed.sg11").toPath(),actual);
        Files.write(new File(dir,name+"-expected.sg11").toPath(),expected);
        check(Arrays.equals(expected,actual),"entire authority including rule RNG equals reference: "+name);
        if(saved){try(InputStream in=getTargetContext().openFileInput("auto.sg11")){check(Arrays.equals(expected,in.readAllBytes()),"entire real autosave equals reference: "+name);}}
        note("EXACT "+name+" bytes="+actual.length+" SHA256="+digest(actual)+" autosave="+saved);
    }
    private void motion(boolean enabled)throws Exception{
        shell("settings put global animator_duration_scale "+(enabled?"1":"0"));
        long end=SystemClock.uptimeMillis()+10000;boolean[] actual={false};
        do{runOnMainSync(()->actual[0]=UiMotion.enabled());if(actual[0]==enabled)return;settle();}while(SystemClock.uptimeMillis()<end);
        check(actual[0]==enabled,"actual process observes Android animator setting");
    }
    private void shot(String name,boolean nativeMode)throws Exception{
        if(nativeMode){ready();surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);}
        capture(name+"-ui");
        shell("screencap -p "+new File(dir,name+"-screencap.png").getAbsolutePath());
        String[] text={null};runOnMainSync(()->{try{
            TextView banner=(TextView)field(activity,"dateBanner");
            text[0]="authority="+SessionProbe.view(activity).date()+" bannerIdentity="+System.identityHashCode(banner)+" banner="+banner.getText()+" attached="+banner.isAttachedToWindow()+" focus="+banner.hasWindowFocus()+" shown="+banner.isShown();
        }catch(Exception e){throw new RuntimeException(e);}});
        note("SCREEN "+name+" "+text[0]+"\n"+host.report());
    }
    private void recreation(byte[] expected,String name,boolean nativeMode)throws Exception{
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);runOnMainSync(activity::recreate);
        Activity next=monitor.waitForActivityWithTimeout(30000);removeMonitor(monitor);
        check(next instanceof MainActivity,"actual Activity recreation completed");activity=(MainActivity)next;settle();host=(MapHost)field(activity,"map");
        check(host.is3D()==nativeMode,"recreation retains requested backend");exact(expected,name,true);shot(name,nativeMode);
    }
    private void one(byte[] initial,byte[] attacked,byte[] finalState,int month,boolean nativeMode,String mode)throws Exception{
        String name="month"+month+"-"+(nativeMode?"3d":"2d")+"-"+mode;
        motion(!mode.equals("off"));World restored=SaveCodec.decode(initial);
        runOnMainSync(()->{SessionProbe.install(activity,restored);activity.refresh();host.switchMode(nativeMode);activity.selectUnitAndFocus(1);});
        settle();if(nativeMode)ready();check(host.is3D()==nativeMode,"requested backend remains active");exact(initial,name+"-initial",false);shot(name+"-before",nativeMode);
        boolean[] timeline={false};runOnMainSync(()->{
            check(SessionProbe.command(activity,w->NativeR11Fixture.command(w,"counter")).ok,"legal attack through real Activity transaction");
            timeline[0]=host.commandEffectsActive();if(mode.equals("skip"))host.cancelCommandEffects();
        });
        check(timeline[0]!=mode.equals("off"),"animation mode actually governs production command timeline");
        exact(attacked,name+"-attack-committed",true);
        long end=SystemClock.uptimeMillis()+120000;
        while(host.commandEffectsActive()&&SystemClock.uptimeMillis()<end)settle();
        check(!host.commandEffectsActive(),"command timeline completed without a timeout change");exact(attacked,name+"-attack-finished",true);
        runOnMainSync(()->invoke("advanceTurn",new Class<?>[0]));
        boolean[] running={true},skipped={false};end=SystemClock.uptimeMillis()+120000;
        while(SystemClock.uptimeMillis()<end){
            runOnMainSync(()->{try{
                TurnPlayback p=(TurnPlayback)field(activity,"playback");
                if(mode.equals("skip")&&p!=null&&!skipped[0]){p.skip();skipped[0]=true;}
                running[0]=(Boolean)field(activity,"aiRunning");
            }catch(Exception e){throw new RuntimeException(e);}});
            if(!running[0])break;settle();
        }
        check(!running[0],"actual turn finished within original 120s bound");
        if(mode.equals("skip"))check(skipped[0],"real TurnPlayback.skip was reached, not an already-ended turn");
        exact(finalState,name+"-turn",true);check(host.is3D()==nativeMode,"turn did not silently fall back");
        World expected=SaveCodec.decode(finalState);check(expected.turn==3,"real rules crossed month boundary");
        runOnMainSync(()->world=SessionProbe.view(activity));check(world.date().equals(expected.date()),"authority calendar after real turn");
        shot(name+"-after",nativeMode);
        // Exercise real installed recreation on both backends after the cross-year skip case.
        if(month==12&&mode.equals("skip"))recreation(finalState,name+"-recreated",nativeMode);
        note("PASS CASE "+name+" finalDate="+world.date()+" inputSHA="+digest(initial)+" finalSHA="+digest(finalState));cases++;
    }
    @Override public void onStart(){Bundle result=new Bundle();boolean passed=false;try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();oldScale=shell("settings get global animator_duration_scale");
        World seed=NativeR11Fixture.world("counter");try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
        note("EXPLICIT FIXTURES; NOT cold-start/touch/art/physical-device acceptance. API="+Build.VERSION.SDK_INT+" ABI="+Arrays.toString(Build.SUPPORTED_ABIS));
        for(int month:new int[]{4,12}){
            World base=NativeR11Fixture.world("counter");base.startYear=190;base.startMonth=month;base.turn=2;byte[] initial=SaveCodec.encode(base);
            World reference=SaveCodec.decode(initial);check(NativeR11Fixture.command(reference,"counter").ok,"reference legal attack");byte[] attacked=SaveCodec.encode(reference);
            check(reference.nextTurn().ok,"reference complete rules turn");byte[] after=SaveCodec.encode(reference);
            Files.write(new File(dir,"input-month"+month+".sg11").toPath(),initial);
            for(boolean nativeMode:new boolean[]{false,true})for(String mode:new String[]{"normal","off","skip"})one(initial,attacked,after,month,nativeMode,mode);
        }
        check(cases==12,"all twelve installed combinations reached");passed=true;note("PASS P0_PARITY cases="+cases+" checks="+checks+"; visual dates require raw full-screen review, not getText");
        result.putString("stream","PASS P0_PARITY cases="+cases+" checks="+checks+"\n");
    }catch(Throwable e){try{note("FAIL "+android.util.Log.getStackTraceString(e));capture("p0-parity-failure");}catch(Exception ignored){}result.putString("stream","FAIL P0_PARITY cases="+cases+" "+android.util.Log.getStackTraceString(e));
    }finally{try{if(oldScale!=null){if(oldScale.equals("null"))shell("settings delete global animator_duration_scale");else if(oldScale.matches("[0-9.]+"))shell("settings put global animator_duration_scale "+oldScale);}}catch(Exception e){passed=false;result.putString("stream",result.getString("stream")+"\nRESTORE_FAILURE "+e);}}
    finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}

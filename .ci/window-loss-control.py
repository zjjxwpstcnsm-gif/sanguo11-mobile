"""Generate a strictly labelled diagnostic probe for an unchanged original APK.
This one-time layout intervention is causal diagnosis, never candidate acceptance.
"""
from pathlib import Path
p=Path('app/src/androidTest/java/game/sanguo/mobile/NativeWindowLossControl.java')
p.write_text(r'''package game.sanguo.mobile;
import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.TextView;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** DIAGNOSTIC ONLY: an unchanged APK, raw before/after a single Window attribute reapply.
 * Does not claim an application fix, normal UI flow, or visual acceptance. */
public final class NativeWindowLossControl extends SceneInstrumentation {
 File dir;long draws,frames;
 void note(String s)throws Exception {Files.write(new File(dir,"window-loss-control.txt").toPath(),(s+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);android.util.Log.i("WindowLossControl",s);}
 int windowCopy(String name)throws Exception {
  Bitmap b=Bitmap.createBitmap(activity.getWindow().getDecorView().getWidth(),activity.getWindow().getDecorView().getHeight(),Bitmap.Config.ARGB_8888);
  int[] result={-1};CountDownLatch latch=new CountDownLatch(1);
  runOnMainSync(()->{try{PixelCopy.request(activity.getWindow(),b,r->{result[0]=r;latch.countDown();},new Handler(Looper.getMainLooper()));}catch(IllegalArgumentException e){result[0]=-2;try{note(name+" PixelCopy exception="+e);}catch(Exception x){throw new RuntimeException(x);}latch.countDown();}});
  boolean done=latch.await(20,TimeUnit.SECONDS);note(name+" WindowCopy="+result[0]+" done="+done+" draws="+draws+" frames="+frames);
  if(done){if(result[0]==PixelCopy.SUCCESS)try(OutputStream out=new FileOutputStream(new File(dir,name+"-window.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
  return result[0];
 }
 void raw(String name)throws Exception {
  Bitmap b=getUiAutomation().takeScreenshot();if(b!=null){try(OutputStream out=new FileOutputStream(new File(dir,name+"-ui.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}else note(name+" original UiAutomation screenshot unavailable (not passed)");
  for(String command:new String[]{"screencap -p","dumpsys window","dumpsys SurfaceFlinger","dumpsys gfxinfo game.sanguo.mobile.dev framestats"}){
   String suffix=command.startsWith("screencap")?"shell.png":command.substring(8).replace(' ','-')+".txt";
   try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd);OutputStream out=new FileOutputStream(new File(dir,name+"-"+suffix))){byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)out.write(block,0,n);}
  }
  surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);
  runOnMainSync(()->{try{TextView v=(TextView)field(activity,"dateBanner");note(name+" authority="+SessionProbe.view(activity).date()+" snapshotMonth="+((MapSceneSnapshot)field(field(host,"spatial"),"snapshot")).month+" widget="+v.getText()+" activity="+System.identityHashCode(activity)+" widgetId="+System.identityHashCode(v)+" root="+System.identityHashCode(v.getRootView())+" focus="+v.hasWindowFocus()+" attached="+v.isAttachedToWindow()+" draws="+draws+" frames="+frames+"\n"+host.report());}catch(Exception e){throw new RuntimeException(e);}});
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
  note("DIAGNOSTIC ONLY. Original APK remains unchanged. One Window.setAttributes reapply only after a recorded missing backing Surface; no Activity restart, no renderer override, no timeout extension. Not candidate acceptance.");
  World initial=ScenarioCatalog.load("coalition-190",5,20260925L);try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(initial));}
  getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).commit();
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
  runOnMainSync(()->{activity.getWindow().getDecorView().getViewTreeObserver().addOnDrawListener(()->draws++);activity.getWindow().addOnFrameMetricsAvailableListener((w,m,d)->frames++,new Handler(Looper.getMainLooper()));world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.MEDIUM);host.setGridShown(false);host.setTerritoryMode(0);invoke("closePanel",new Class<?>[0]);});ready();
  byte[] saved=SaveCodec.encode(SessionProbe.view(activity));World july=SaveCodec.decode(saved);july.startMonth=7;july.turn=0;
  runOnMainSync(()->{SessionProbe.install(activity,july);activity.refresh();world=SessionProbe.view(activity);});ready();
  raw("before-july");int before=windowCopy("before-july");
  if(before!=-2){note("NOT_REPRODUCED missing backing Surface; no intervention executed.");result.putString("stream","NOT_REPRODUCED WINDOW_LOSS_CONTROL\n");finish(Activity.RESULT_CANCELED,result);return;}
  byte[] authority=SaveCodec.encode(SessionProbe.view(activity));Object originalHost=host,originalRenderer=field(host,"spatial");long priorDraws=draws,priorFrames=frames;
  runOnMainSync(()->{Window w=activity.getWindow();w.setAttributes(w.getAttributes());});note("DIAGNOSTIC_INTERVENTION Window.setAttributes(originalAttributes) exactly once; same window/page/renderer.");
  settle();settle();int after=windowCopy("after-reapply");raw("after-reapply");
  check(after==PixelCopy.SUCCESS,"one Window relayout reacquired actual backing surface");
  check(draws>priorDraws&&frames>priorFrames,"actual HWUI frame submission resumes");
  check(host==originalHost&&field(host,"spatial")==originalRenderer,"same native renderer survived");check(Arrays.equals(authority,SaveCodec.encode(SessionProbe.view(activity))),"one layout intervention preserved full authority/RNG/save bytes");
  World april=SaveCodec.decode(saved);april.startMonth=4;april.turn=0;runOnMainSync(()->{SessionProbe.install(activity,april);activity.refresh();world=SessionProbe.view(activity);});ready();raw("after-april");windowCopy("after-april");
  note("DIAGNOSTIC_BACKING_REACQUIRED. Whole-screen July/April require visual review. Not an application fix or acceptance PASS.");result.putString("stream","DIAGNOSTIC_BACKING_REACQUIRED WINDOW_LOSS_CONTROL\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){try{note("FAIL "+android.util.Log.getStackTraceString(e));}catch(Exception ignored){}result.putString("stream","FAIL WINDOW_LOSS_CONTROL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
''')
p=Path('app/src/androidTest/AndroidManifest.xml');s=p.read_text();assert s.count('</manifest>')==1
p.write_text(s.replace('</manifest>','    <instrumentation android:name="game.sanguo.mobile.NativeWindowLossControl" android:targetPackage="game.sanguo.mobile.dev" android:functionalTest="true" />\n</manifest>'))

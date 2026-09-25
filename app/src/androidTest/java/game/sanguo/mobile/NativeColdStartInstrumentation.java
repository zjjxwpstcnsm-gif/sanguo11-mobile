package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Isolated CI app data only. Real screen touches from an empty startup page;
 * never writes auto.sg11 or invokes scenarioPicker/startScenario directly. */
public final class NativeColdStartInstrumentation extends SceneInstrumentation {
 private File dir;
 private void note(String s)throws Exception{Files.write(new File(dir,"cold-runtime.txt").toPath(),(s+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private View find(View v,Predicate<View> match){
  if(!v.isShown())return null;
  if(match.test(v))return v;
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View hit=find(g.getChildAt(i),match);if(hit!=null)return hit;}}
  return null;
 }
 private View await(Predicate<View> match)throws Exception{
  long deadline=SystemClock.uptimeMillis()+30000;
  while(SystemClock.uptimeMillis()<deadline){View[] hit={null};runOnMainSync(()->{List<View> roots=WindowInspector.getGlobalWindowViews();for(int i=roots.size()-1;i>=0;i--){hit[0]=find(roots.get(i),match);if(hit[0]!=null)break;}});if(hit[0]!=null)return hit[0];settle();}
  capture("cold-missing-control");throw new AssertionError("visible UI control not found");
 }
 private void tap(View v)throws Exception{
  Rect rect=new Rect();int[] screen=new int[2];runOnMainSync(()->{check(v.isEnabled(),"control enabled");check(v.getLocalVisibleRect(rect),"control has visible bounds");v.getLocationOnScreen(screen);rect.offset(screen[0],screen[1]);});
  note("TOUCH screenRect="+rect+" control="+v.getClass().getSimpleName());
  long time=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,rect.exactCenterX(),rect.exactCenterY(),0),up=MotionEvent.obtain(time,time+80,MotionEvent.ACTION_UP,rect.exactCenterX(),rect.exactCenterY(),0);
  sendPointerSync(down);sendPointerSync(up);down.recycle();up.recycle();settle();
 }
 private void text(String prefix)throws Exception{note("TOUCH text="+prefix);tap(await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(prefix)));}
 private void description(String prefix)throws Exception{note("TOUCH description="+prefix);tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(prefix)));}
 private void shot(String name)throws Exception{ready();surfaceCapture();capture(name+"-screen");Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);note(name+"\n"+host.report());}
 @Override public void onStart(){Bundle result=new Bundle();try{
  dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
  check(!new File(getTargetContext().getFilesDir(),"auto.sg11").exists(),"fresh app has no autosave fixture");
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
  check(field(activity,"world")==null,"normal cold startup has no loaded world");capture("cold-start-page");
  text("新建游戏 · 选择剧本");description("选择剧本 ");
  host=(MapHost)await(v->v instanceof MapHost);
  text("2D/3D");check(host.is3D(),"user switches actual full-map preview to 3D");shot("cold-national-preview");
  runOnMainSync(()->{try{
   FilamentMapView nativeView=(FilamentMapView)field(host,"spatial");
   MapSceneSnapshot.Ground ground=((MapSceneSnapshot)field(nativeView,"snapshot")).ground;
   SceneCamera camera=nativeView.camera;int outside=0,total=0;
   for(int r=0;r<ground.height;r++)for(int q=0;q<ground.width;q++){
    Hex h=new Hex(q,r);if(!ground.valid(h))continue;total++;
    float x=ground.grid.x(h),z=ground.grid.z(h),sx=camera.screenX(x,z),sy=camera.screenY(x,z,ground.surface.at(h));
    if(sx<0||sy<0||sx>camera.width||sy>camera.height)outside++;
   }
   note("FIRST_NATIVE_PREVIEW total="+total+" outside="+outside+" span="+camera.span+" viewport="+camera.width+"x"+camera.height);
   check(total>0&&outside==0,"first native preview includes every valid national cell without an extra fit command");
   long initialJobs=(Long)field(field(nativeView,"meshWork"),"epoch");
   note("FIRST_PREVIEW_CPU_EPOCH="+initialJobs+" allCoarse="+field(nativeView,"distantTerrain"));
   check(initialJobs==1L,"first stable national preview requires only one CPU request, without a discarded local warmup");
   check((Boolean)field(nativeView,"distantTerrain"),"first national preview uses coarse terrain");
  }catch(Exception e){throw new RuntimeException(e);}});
  Object overlay=field(field(host,"spatial"),"overlay");long builds=(Long)field(overlay,"territoryBuilds"),draws=(Long)field(overlay,"draws");
  settle();settle();check((Long)field(overlay,"territoryBuilds")==builds,"stationary full-map territory is reused across frames");check((Long)field(overlay,"draws")>draws,"UI overlay continues drawing live labels");
  description("选择势力 · ");shot("cold-faction-selected");
  description("确认开局势力 · ");text("执行");
  long deadline=SystemClock.uptimeMillis()+90000;
  while(SystemClock.uptimeMillis()<deadline){boolean[] loaded={false};runOnMainSync(()->{try{loaded[0]=field(activity,"world")!=null&&field(activity,"map")!=null;}catch(Exception e){throw new RuntimeException(e);}});if(loaded[0])break;settle();}
  host=(MapHost)field(activity,"map");check(host!=null&&host.is3D(),"confirmed new session retains native rendering");runOnMainSync(()->world=SessionProbe.view(activity));
  check(world!=null,"fresh UI installed authority");shot("cold-started-game");
  note("PASS cold startup -> scenario button -> full national preview -> faction chip -> confirmation -> actual native session; source="+BuildConfig.SOURCE_REVISION+" authorityDate="+world.date());
  note("NOT_RUN in this probe: deployment/move/attack/report/turn/save-load touch chain; original R12 reports its separate API-level scope.");
  result.putString("stream","PASS COLD_START scoped new-game UI checks="+checks+"\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){try{note("FAIL "+android.util.Log.getStackTraceString(e));}catch(Exception ignored){}result.putString("stream","FAIL COLD_START "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}

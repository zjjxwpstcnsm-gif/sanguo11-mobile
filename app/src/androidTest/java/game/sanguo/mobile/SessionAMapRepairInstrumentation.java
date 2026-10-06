package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.StateToken;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;

/** Session A normal-menu source/preview/new-game/gesture/lifecycle acceptance.
 * No constructed World, snapshot, state injection or direct rule command.
 * Reflection is read-only; all scenario/faction actions use injected touches.
 */
public final class SessionAMapRepairInstrumentation extends Instrumentation {
    private MainActivity activity; private File output; private int checks;
    private StringBuilder log=new StringBuilder(), memory=new StringBuilder("phase,javaUsed,javaTotal,javaLimit,nativeAllocated,totalPss,graphicsPss\n");
    private String run; private int begin,end;
    @Override public void onCreate(Bundle args){super.onCreate(args);run=args.getString("run","session_a_map");begin=Integer.parseInt(args.getString("begin","0"));end=Integer.parseInt(args.getString("end","16"));if(!run.matches("[A-Za-z0-9_-]+"))throw new IllegalArgumentException();start();}
    @Override public void callActivityOnResume(Activity a){super.callActivityOnResume(a);if(a instanceof MainActivity)activity=(MainActivity)a;}
    private void check(boolean condition,String message){checks++;log.append(condition?"PASS ":"FAIL ").append(message).append('\n');if(!condition)throw new AssertionError(message);}
    private void ui(Runnable work){Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private View find(View v,Predicate<View> test){if(!v.isShown())return null;if(test.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View x=find(((ViewGroup)v).getChildAt(i),test);if(x!=null)return x;}return null;}
    private View await(Predicate<View> test){long deadline=SystemClock.uptimeMillis()+120000;while(SystemClock.uptimeMillis()<deadline){View[] found={null};ui(()->{List<View> roots=WindowInspector.getGlobalWindowViews();for(int i=roots.size()-1;i>=0;i--)if(roots.get(i).hasWindowFocus()&&(found[0]=find(roots.get(i),test))!=null)break;});if(found[0]!=null)return found[0];SystemClock.sleep(100);}throw new AssertionError("Normal widget unavailable");}
    private View desc(String value){return await(v->value.equals(v.getContentDescription()==null?"":v.getContentDescription().toString()));}
    private void tap(View view){Rect rect=new Rect();ui(()->view.requestRectangleOnScreen(new Rect(0,0,view.getWidth(),view.getHeight()),true));SystemClock.sleep(200);ui(()->{check(view.getGlobalVisibleRect(rect)&&rect.width()>0&&rect.height()>0,"control reachable "+view.getContentDescription());int[] loc=new int[2];view.getRootView().getLocationOnScreen(loc);rect.offset(loc[0],loc[1]);});long down=SystemClock.uptimeMillis();for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,rect.centerX(),rect.centerY(),0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(e,true),"pointer accepted");e.recycle();}SystemClock.sleep(200);}
    private void text(String s){tap(await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(s)&&(v.isClickable()||v.getParent() instanceof AdapterView)));}
    private void nav(String page){tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航")));tap(desc("导航 · "+page));}
    private byte[] capture(){byte[][] bytes={null};ui(()->{try{bytes[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new IllegalStateException(e);}});return bytes[0];}
    private StateToken token(){StateToken[] t={null};ui(()->t[0]=activity.deploymentState());return t[0];}
    private void unchanged(byte[] save,StateToken token,String phase){check(Arrays.equals(save,capture()),phase+" complete Save/RNG byte equal");check(token.equals(token()),phase+" full StateToken equal");}
    private MapHost host(boolean preview){return (MapHost)await(v->v instanceof MapHost&&(!preview||v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("开局势力地图")));}
    private void ready(MapHost host)throws Exception{long deadline=SystemClock.uptimeMillis()+120000;boolean[] good={false};while(SystemClock.uptimeMillis()<deadline){ui(()->{try{FilamentMapView view=(FilamentMapView)field(host,"spatial");good[0]=view!=null&&!(Boolean)field(view,"released")&&(Boolean)field(view,"outputVerified")&&(Long)field(view,"renderedFrames")>2&&(Integer)field(view,"pending")==0&&!(Boolean)field(view,"assetSyncPending");}catch(Exception e){throw new IllegalStateException(e);}});if(good[0])break;SystemClock.sleep(100);}check(good[0],"actual3D verified output and completed terrain, not renderer recovery");sample("ready");String[] report={null};ui(()->report[0]=host.report());try(FileOutputStream f=new FileOutputStream(new File(output,"renderer-timeline.txt"),true)){f.write(("\nCHECK "+checks+"\n"+report[0]+"\n").getBytes("UTF-8"));}}
    private void sample(String phase)throws Exception{Runtime rt=Runtime.getRuntime();Debug.MemoryInfo m=new Debug.MemoryInfo();Debug.getMemoryInfo(m);memory.append(phase).append(',').append(rt.totalMemory()-rt.freeMemory()).append(',').append(rt.totalMemory()).append(',').append(rt.maxMemory()).append(',').append(Debug.getNativeHeapAllocatedSize()).append(',').append(m.getTotalPss()).append(',').append(m.getMemoryStat("summary.graphics")).append('\n');Files.write(new File(output,"memory.csv").toPath(),memory.toString().getBytes("UTF-8"));}
    private void shot(String name)throws Exception{SystemClock.sleep(400);Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"screen captured");try(FileOutputStream stream=new FileOutputStream(new File(output,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}finally{bitmap.recycle();}}
    private void pointer(long down,int action,float...xy){MotionEvent.PointerProperties[] p=new MotionEvent.PointerProperties[xy.length/2];MotionEvent.PointerCoords[] c=new MotionEvent.PointerCoords[p.length];for(int i=0;i<p.length;i++){p[i]=new MotionEvent.PointerProperties();p[i].id=i;p[i].toolType=MotionEvent.TOOL_TYPE_FINGER;c[i]=new MotionEvent.PointerCoords();c[i].x=xy[2*i];c[i].y=xy[2*i+1];c[i].pressure=1;c[i].size=1;}MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,p.length,p,c,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);if(!getUiAutomation().injectInputEvent(e,true))throw new AssertionError("Gesture rejected");e.recycle();SystemClock.sleep(35);}
    private void zoom(MapHost host)throws Exception{Rect r=new Rect();ui(()->{host.getGlobalVisibleRect(r);int[] loc=new int[2];host.getRootView().getLocationOnScreen(loc);r.offset(loc[0],loc[1]);});float x=r.centerX(),y=r.top+r.height()*.42f,start=Math.min(r.width(),r.height())*.12f,last=start;long down=SystemClock.uptimeMillis();pointer(down,0,x-start,y);pointer(down,5|(1<<8),x-start,y,x+start,y);for(int i=1;i<=12;i++){last=start*(1+i*.13f);pointer(down,2,x-last,y,x+last,y);}pointer(down,6|(1<<8),x-last,y,x+last,y);pointer(down,1,x-last,y);SystemClock.sleep(350);sample("zoom");}
    private void pan(MapHost host)throws Exception{Rect r=new Rect();ui(()->host.getGlobalVisibleRect(r));long down=SystemClock.uptimeMillis();for(int i=0;i<10;i++)pointer(down,i==0?0:2,r.centerX()+i*4,r.top+r.height()*.45f+i*2);pointer(down,1,r.centerX()+36,r.top+r.height()*.45f+18);SystemClock.sleep(200);sample("pan");}
    private void normalFit(){text("视图");text("全图");}
    private void retired(MapHost host)throws Exception{check((Boolean)field(host,"released")&&field(host,"spatial")==null&&field(host,"world")==null&&field(host,"ground")==null,"dismissed preview releases engine, detachedWorld and ground");}
    @Override public void onStart(){Bundle result=new Bundle();try{
        output=new File(getTargetContext().getExternalFilesDir("session-a-map"),run);check(output.mkdirs(),"fresh evidence directory");activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));nav("地图");ready(host(false));sample("startup");
        List<PcScenarioCatalog.Source> sources=PcScenarioCatalog.all();check(sources.size()==16,"all16 installed sources");
        for(int index=begin;index<end;index++){
            byte[] before=capture();StateToken prior=token();nav("菜单");text("新游戏 / 选择势力");PcScenarioCatalog.Source source=sources.get(index);tap(desc("选择PC来源剧本 "+source.identity.path));MapHost preview=host(true);ready(preview);unchanged(before,prior,"source preview "+index);
            if(index==begin){text("返回");retired(preview);text("取消");unchanged(before,prior,"actual preview and source cancel");nav("菜单");text("新游戏 / 选择势力");tap(desc("选择PC来源剧本 "+source.identity.path));preview=host(true);ready(preview);unchanged(before,prior,"cancel/retry");}
            List<Button> chips=new ArrayList<>();ui(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus())collect(root,chips);});check(!chips.isEmpty(),"normal faction chips");for(Button b:chips)check((b.getCurrentTextColor()>>>24)>0&&(b.getCurrentTextColor()&0xffffff)!=0,"faction text has opaque nonzero color "+b.getText());
            Button first=null,last=null;for(Button b:chips)if(b.isEnabled()){if(first==null)first=b;last=b;}tap(last);tap(first);unchanged(before,prior,"faction changes "+index);shot("source-"+index+"-preview");
            for(int cycle=0;cycle<(index==begin?4:1);cycle++){tap(await(v->v instanceof Button&&"全图".contentEquals(((Button)v).getText())));for(int i=0;i<4;i++)zoom(preview);pan(preview);ready(preview);unchanged(before,prior,"preview gestures "+index+"/"+cycle);}
            tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力")));text("开始新局");long deadline=SystemClock.uptimeMillis()+120000;while(Arrays.equals(before,capture())&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(100);check(!Arrays.equals(before,capture()),"explicit new game committed "+index);retired(preview);nav("地图");MapHost current=host(false);ready(current);byte[] state=capture();StateToken currentToken=token();World saved=SaveCodec.decode(state);check(saved.scenarioId.equals(source.identity.scenarioId),"new game matches clicked exact source");
            for(int cycle=0;cycle<2;cycle++){normalFit();for(int i=0;i<4;i++)zoom(current);pan(current);ready(current);unchanged(state,currentToken,"new game gestures "+index+"/"+cycle);}
            shot("source-"+index+"-map");Files.write(new File(output,"source-"+index+".sg11").toPath(),state);nav("武将");shot("source-"+index+"-officers");unchanged(state,currentToken,"normal directory "+index);nav("地图");sample("source-"+index+"-complete");
            Bundle update=new Bundle();update.putString("stream","SESSION_A source "+index+" normal flow complete\n");sendStatus(0,update);
        }
        byte[] state=capture();StateToken prior=token();sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);SystemClock.sleep(800);getTargetContext().startActivity(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));SystemClock.sleep(1000);ready(host(false));unchanged(state,prior,"actual Home/resume");shot("resumed");
        text("视图");text("屏幕方向");text("横屏");SystemClock.sleep(1200);ready(host(false));check(Arrays.equals(state,capture()),"real orientation menu keeps complete Save/RNG");shot("landscape");text("视图");text("屏幕方向");text("竖屏");SystemClock.sleep(1200);ready(host(false));check(Arrays.equals(state,capture()),"portrait keeps complete Save/RNG");
        nav("菜单");text("保存局面");text("槽位 3");text("覆盖存档");check(Arrays.equals(state,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"normal save exact complete bytes");StateToken loadPrior=token();text("读取存档");text("槽位 3");text("读取存档");long loaded=SystemClock.uptimeMillis()+120000;while(loadPrior.equals(token())&&SystemClock.uptimeMillis()<loaded)SystemClock.sleep(100);check(!loadPrior.equals(token())&&Arrays.equals(state,capture()),"normal load replaces session and keeps complete Save/RNG");nav("地图");ready(host(false));
        ui(()->activity.finish());SystemClock.sleep(1000);activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));nav("地图");ready(host(false));check(Arrays.equals(state,capture()),"normal exit/reopen exact complete Save/RNG");shot("reopened");
        result.putString("stream","SESSION_A_MAP PASS "+checks+" checks\n"+log);
    }catch(Throwable error){result.putString("stream","SESSION_A_MAP FAIL "+LogTrace(error)+"\n"+log);try{shot("FAIL");}catch(Throwable ignored){}}
    finally{try{Files.write(new File(output,"result.txt").toPath(),result.getString("stream").getBytes("UTF-8"));}catch(Throwable ignored){}}
    finish(result.getString("stream").startsWith("SESSION_A_MAP PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
    private void collect(View v,List<Button> result){if(v instanceof Button&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("选择势力 ·"))result.add((Button)v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),result);}
    private String LogTrace(Throwable error){return android.util.Log.getStackTraceString(error);}
}

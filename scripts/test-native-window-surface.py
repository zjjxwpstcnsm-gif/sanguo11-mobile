#!/usr/bin/env python3
"""HOST ONLY: compile the complete real Window recovery class against deterministic API fakes.
This never substitutes for installed HWUI, whole-screen or lifecycle acceptance.
"""
from pathlib import Path
import subprocess, tempfile
root=Path(__file__).resolve().parent.parent
stubs={
'android/os/Looper.java':'''package android.os; public class Looper {static final Looper MAIN=new Looper();public static Looper getMainLooper(){return MAIN;}public static Looper myLooper(){return MAIN;}}''',
'android/os/SystemClock.java':'''package android.os; public class SystemClock {public static long now=10000;public static long uptimeMillis(){return now;}}''',
'android/os/Handler.java':'''package android.os;import java.util.*;public class Handler {static final List<Runnable> tasks=new ArrayList<>();public Handler(Looper l){}public boolean postDelayed(Runnable r,long d){tasks.add(r);return true;}public void removeCallbacks(Runnable r){tasks.removeIf(x->x==r);}public static void tick(){SystemClock.now+=1100;List<Runnable> run=new ArrayList<>(tasks);tasks.clear();for(Runnable r:run)r.run();}public static int pending(){return tasks.size();}}''',
'android/graphics/Rect.java':'''package android.graphics;public class Rect {public Rect(int a,int b,int c,int d){if(c-a!=1||d-b!=1)throw new AssertionError("bounded probe rectangle");}}''',
'android/graphics/Bitmap.java':'''package android.graphics;public class Bitmap {public static int live,max;boolean dead;public static class Config {public static final Config ARGB_8888=new Config();}public static Bitmap createBitmap(int w,int h,Config c){if(w!=1||h!=1)throw new AssertionError("not a full Window copy");live++;max=Math.max(live,max);return new Bitmap();}public void recycle(){if(dead)throw new AssertionError("double recycle");dead=true;live--;}}''',
'android/util/Log.java':'''package android.util;public class Log {public static int i(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m,Throwable e){return 0;}}''',
'android/view/ViewTreeObserver.java':'''package android.view;public class ViewTreeObserver {public interface OnDrawListener{void onDraw();}public interface OnWindowFocusChangeListener{void onWindowFocusChanged(boolean f);}public OnDrawListener draw;public OnWindowFocusChangeListener focus;public boolean isAlive(){return true;}public void addOnDrawListener(OnDrawListener l){draw=l;}public void removeOnDrawListener(OnDrawListener l){if(draw==l)draw=null;}public void addOnWindowFocusChangeListener(OnWindowFocusChangeListener l){focus=l;}public void removeOnWindowFocusChangeListener(OnWindowFocusChangeListener l){if(focus==l)focus=null;}}''',
'android/view/View.java':'''package android.view;import java.util.*;public class View {public interface OnAttachStateChangeListener{void onViewAttachedToWindow(View v);void onViewDetachedFromWindow(View v);}public boolean attached=true,shown=true,focused=true;public int width=100,height=100;public View root=this;public OnAttachStateChangeListener attach;final Map<Integer,Object> tags=new HashMap<>();final ViewTreeObserver tree=new ViewTreeObserver();public View getRootView(){return root;}public Object getTag(int n){return tags.get(n);}public void setTag(int n,Object x){tags.put(n,x);}public void addOnAttachStateChangeListener(OnAttachStateChangeListener l){attach=l;}public void removeOnAttachStateChangeListener(OnAttachStateChangeListener l){if(attach==l)attach=null;}public ViewTreeObserver getViewTreeObserver(){return tree;}public boolean isAttachedToWindow(){return attached;}public boolean isShown(){return shown;}public boolean hasWindowFocus(){return focused;}public int getWidth(){return width;}public int getHeight(){return height;}}''',
'android/view/Window.java':'''package android.view;import android.os.*;public class Window {public interface OnFrameMetricsAvailableListener{void onFrameMetricsAvailable(Window w,Object m,int d);}public final View decor=new View();public boolean missing;public String error="Window doesn't have a backing surface!";public int relayouts;public Object attributes=new Object();public OnFrameMetricsAvailableListener metrics;public View getDecorView(){return decor;}public Object getAttributes(){return attributes;}public void setAttributes(Object a){if(a!=attributes)throw new AssertionError("attributes replaced");relayouts++;}public void addOnFrameMetricsAvailableListener(OnFrameMetricsAvailableListener l,Handler h){metrics=l;}public void removeOnFrameMetricsAvailableListener(OnFrameMetricsAvailableListener l){if(metrics==l)metrics=null;}public void frame(){if(metrics!=null)metrics.onFrameMetricsAvailable(this,null,0);}}''',
'android/view/PixelCopy.java':'''package android.view;import android.graphics.*;import android.os.*;public class PixelCopy {public static final int SUCCESS=0;public interface OnPixelCopyFinishedListener{void onPixelCopyFinished(int result);}public static int calls,result;public static boolean hold;public static OnPixelCopyFinishedListener held;public static void request(Window w,Rect r,Bitmap b,OnPixelCopyFinishedListener l,Handler h){calls++;if(w.missing)throw new IllegalArgumentException(w.error);if(hold){if(held!=null)throw new AssertionError("unbounded copy concurrency");held=l;}else l.onPixelCopyFinished(result);}public static void finish(){OnPixelCopyFinishedListener l=held;held=null;l.onPixelCopyFinished(result);}}''',
'game/sanguo/mobile/R.java':'''package game.sanguo.mobile;public class R {public static class id {public static final int window_surface_recovery=0x7f0f0001;}}''',
'game/sanguo/mobile/WindowRecoveryHost.java':'''package game.sanguo.mobile;
import android.view.*;import android.os.*;import android.graphics.*;
public class WindowRecoveryHost {
 static int checks;static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void tick(WindowSurfaceRecovery r){r.request();Handler.tick();}
 public static void main(String[] args){
  Window w=new Window();WindowSurfaceRecovery r=new WindowSurfaceRecovery(w);
  for(int i=0;i<20;i++){r.request();w.frame();Handler.tick();}
  check(PixelCopy.calls==0&&w.relayouts==0,"healthy delivered HWUI requires no copy or relayout");
  r.request();Handler.tick();check(PixelCopy.calls==1&&w.relayouts==0,"stale metrics alone never justify relayout");
  Handler.tick();check(Handler.pending()==0,"idle healthy Window has no periodic work");
  w.missing=true;tick(r);check(w.relayouts==1,"one public same-Window reapply after confirmed missing backing");
  for(int i=0;i<40;i++)tick(r);check(w.relayouts==1,"persistent failure does not periodically relayout or restart");
  check(Bitmap.live==0&&Bitmap.max==1,"bounded four-byte probe released after each exception");
  w.missing=false;tick(r);check(w.relayouts==1,"real copy success rearms, never replays a recovery");
  w.missing=true;tick(r);check(w.relayouts==2,"a distinct proven loss can recover on the same Window");
  w.decor.focused=false;w.decor.getViewTreeObserver().focus.onWindowFocusChanged(false);for(int i=0;i<5;i++)tick(r);check(w.relayouts==2,"covered window never recovers behind dialog");
  w.decor.focused=true;w.missing=false;w.decor.getViewTreeObserver().focus.onWindowFocusChanged(true);Handler.tick();
  w.error="source rectangle is empty";w.missing=true;tick(r);check(w.relayouts==2,"unrelated IllegalArgumentException cannot trigger recovery");
  r.close();check(w.metrics==null&&w.decor.getTag(R.id.window_surface_recovery)==null&&w.decor.attach==null,"close removes Window observer, tag and attach listener");
  Window pending=new Window();WindowSurfaceRecovery p=new WindowSurfaceRecovery(pending);PixelCopy.hold=true;Handler.tick();check(Bitmap.live==1,"pending native callback owns bitmap");
  for(int i=0;i<50;i++)tick(p);check(Bitmap.live==1,"repeated signals never allocate another pending bitmap");
  p.close();PixelCopy.hold=false;PixelCopy.finish();check(Bitmap.live==0&&pending.relayouts==0&&Handler.pending()==0,"stale close callback cannot recover or retain queued tasks");
  Window first=new Window(),second=new Window();WindowSurfaceRecovery a=new WindowSurfaceRecovery(first),b=new WindowSurfaceRecovery(second);first.missing=true;
  View child=new View();child.root=first.decor;WindowSurfaceRecovery.changed(child);Handler.tick();check(first.relayouts==1&&second.relayouts==0,"Dialog and Activity root ownership never cross");
  a.close();b.close();
  Window transientWindow=new Window();WindowSurfaceRecovery t=new WindowSurfaceRecovery(transientWindow);PixelCopy.result=2;Handler.tick();check(transientWindow.relayouts==0,"copy timeout is not a missing backing surface");t.close();PixelCopy.result=0;
  WindowSurfaceRecoveryState s=new WindowSurfaceRecoveryState();long old=s.epoch;s.invalidateCallbacks();check(!s.accepts(old)&&s.accepts(s.epoch),"detach epoch rejects old callbacks");
  check(!WindowSurfaceRecoveryState.missingBacking(null)&&!WindowSurfaceRecoveryState.missingBacking("Surface invalid"),"specific demonstrated platform failure only");
  check(Handler.pending()==0&&Bitmap.live==0,"complete teardown releases all test-owned probes and callbacks");
  System.out.println("PASS HOST complete production WindowSurfaceRecovery: "+checks+" checks; no Android or whole-screen acceptance implied");
 }
}'''
}
with tempfile.TemporaryDirectory(prefix='window-recovery-host-') as d:
 p=Path(d)
 for name,text in stubs.items():
  f=p/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text)
 for name in ['WindowSurfaceRecovery','WindowSurfaceRecoveryState']:
  (p/f'game/sanguo/mobile/{name}.java').write_text((root/f'app/src/main/java/game/sanguo/mobile/{name}.java').read_text())
 subprocess.run(['javac','--release','17','-d',d]+[str(f) for f in p.rglob('*.java')],check=True)
 subprocess.run(['java','-cp',d,'game.sanguo.mobile.WindowRecoveryHost'],check=True)

package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.graphics.Rect;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Normal map menu + injected physical pointer gestures, original 120s readiness. */
public final class NativeWaterfall127Instrumentation extends SceneInstrumentation {
    private String mode="taishan";private File dir;
    @Override public void onCreate(Bundle b){if(b!=null)mode=b.getString("mode",mode);super.onCreate(b);}
    private void log(String s)throws Exception{Files.write(new File(dir,"waterfall127.txt").toPath(),(s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private byte[] authority(){byte[][] value={null};runOnMainSync(()->{try{value[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return value[0];}
    private boolean scroll(AccessibilityNodeInfo node){
        if(node==null)return false;
        if(node.isScrollable()&&node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return true;
        for(int i=0;i<node.getChildCount();i++){var child=node.getChild(i);if(child!=null){boolean done=scroll(child);child.recycle();if(done)return true;}}return false;
    }
    private void tapText(String label)throws Exception{
        for(int attempt=0;attempt<12;attempt++){
            var root=getUiAutomation().getRootInActiveWindow();
            if(root!=null){
                for(var node:root.findAccessibilityNodeInfosByText(label)){
                    Rect bounds=new Rect();node.getBoundsInScreen(bounds);boolean target=label.contentEquals(node.getText()==null?"":node.getText())&&node.isVisibleToUser()&&!bounds.isEmpty();node.recycle();
                    // ACTION_UP can post the click callback after the first UI
                    // barrier; engine initialization then takes more than 500ms.
                    if(target){root.recycle();pointerTap(bounds.centerX(),bounds.centerY());settle();runOnMainSync(()->{});return;}
                }
                boolean moved=scroll(root);root.recycle();if(!moved)break;
            }
            settle();
        }
        throw new AssertionError("normal UI target unavailable: "+label);
    }
    private void pointerTap(float x,float y){long down=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){var event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(event);event.recycle();}}
    private void touch(long down,int action,float x0,float x1,float y,int count){
        MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];
        for(int i=0;i<count;i++){props[i]=new MotionEvent.PointerProperties();props[i].id=i;props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=i==0?x0:x1;coords[i].y=y;coords[i].pressure=1;coords[i].size=1;}
        var e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,count,props,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);sendPointerSync(e);e.recycle();SystemClock.sleep(20);
    }
    private Rect viewport(FilamentMapView renderer){Rect rect=new Rect();runOnMainSync(()->renderer.getGlobalVisibleRect(rect));return rect;}
    private void pinch(FilamentMapView renderer){
        Rect r=viewport(renderer);float x=r.exactCenterX(),y=r.top+r.height()*.45f;long down=SystemClock.uptimeMillis();
        touch(down,MotionEvent.ACTION_DOWN,x-150,x+150,y,1);
        touch(down,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),x-150,x+150,y,2);
        for(int i=0;i<=8;i++){float d=150-i*7;touch(down,MotionEvent.ACTION_MOVE,x-d,x+d,y,2);}
        touch(down,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),x-94,x+94,y,2);touch(down,MotionEvent.ACTION_UP,x-94,x+94,y,1);
    }
    private void pan(FilamentMapView renderer){Rect r=viewport(renderer);float x=r.exactCenterX(),y=r.top+r.height()*.45f;long down=SystemClock.uptimeMillis();touch(down,MotionEvent.ACTION_DOWN,x,0,y,1);for(int i=1;i<=8;i++)touch(down,MotionEvent.ACTION_MOVE,x+i*6,0,y,1);touch(down,MotionEvent.ACTION_UP,x+48,0,y,1);}
    private void pair(String name)throws Exception{surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(name+"-ui");log(name+"\n"+host.report());}
    private double flow(FilamentMapView r){double[] seconds={0};runOnMainSync(()->{try{seconds[0]=(Double)field(r,"waterSeconds");}catch(Exception e){throw new RuntimeException(e);}});return seconds[0];}
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();World seed=ScenarioCatalog.all().get(0);
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).putString("quality","MEDIUM").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");world=SessionProbe.view(activity);byte[] before=authority();
        runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.setGridShown(false);});
        String label=mode.equals("hukou")?"黄河壶口瀑布":mode.equals("southwest")?"西南山涧瀑布":mode.equals("lushan")?"庐山区域瀑布":"泰山区域瀑布";
        tapText("视图");tapText("山河地标");capture(mode+"-landmark-menu-ui");tapText(label);
        check(host.is3D(),"normal landmark menu initializes native 3D");FilamentMapView renderer=(FilamentMapView)field(host,"spatial");ready();
        var entry=LandscapeLandmarks.ALL.stream().filter(e->e.label().equals(label)).findFirst().orElseThrow();Hex at=MapCoordinates.fromNationalSource(world,new game.sanguo.core.map.SourceGridCoord(entry.x(),entry.y()));
        var ground=((MapSceneSnapshot)field(renderer,"snapshot")).ground;
        check(Math.abs(renderer.camera.x-ground.grid.x(at))<.01&&Math.abs(renderer.camera.z-ground.grid.z(at))<.01,"normal menu centers actual production anchor");
        check(Math.abs(renderer.camera.span-10)<.1,"normal focus scale, not a special demo camera");double start=flow(renderer);pair(mode+"-menu-span10");
        pinch(renderer);settle();ready();check(renderer.camera.span>12&&renderer.camera.span<20,"actual pinch reaches ordinary zoom");pair(mode+"-ordinary-pinch");
        float x=renderer.camera.x,z=renderer.camera.z;pan(renderer);settle();ready();check(Math.hypot(renderer.camera.x-x,renderer.camera.z-z)>.01,"actual pointer pan moves map");pair(mode+"-ordinary-pan");
        tapText("视图");tapText("3D 反向查看");settle();ready();pair(mode+"-reverse");
        int water=0,foam=0;for(Object gpu:((Map<?,?>)field(renderer,"vegetation")).values()){
            SceneMesh m=(SceneMesh)field(gpu,"source");for(int i=0;i<m.uv.length;i+=2){float u=m.uv[i],v=m.uv[i+1];if(mode.equals("hukou")?u>.375f&&u<.5f:u>.625f&&u<.75f)water++;if(u>.5f&&u<.625f&&v>.65f)foam++;}
        }
        check(water>0&&foam>0,"actual resident GPU fall and foam survive normal zoom and pan");check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no asset fallback");check(flow(renderer)>start,"flow uniform advances on real admitted frames");
        check(Arrays.equals(before,authority()),"menu and actual gestures preserve complete authority and RNG");Files.write(new File(dir,"expected.sg11").toPath(),before);Files.write(new File(dir,"observed.sg11").toPath(),authority());
        log("PASS WATERFALL127 mode="+mode+" checks="+checks+" water="+water+" foam="+foam+" startFlow="+start+" endFlow="+flow(renderer));result.putString("stream","PASS WATERFALL127 "+mode+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL WATERFALL127 "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture(mode+"-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

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
public final class NativeLandmarks128Instrumentation extends SceneInstrumentation {
    private String mode="taishan";private File dir;
    @Override public void onCreate(Bundle b){if(b!=null)mode=b.getString("mode",mode);super.onCreate(b);}
    private void log(String s)throws Exception{Files.write(new File(dir,"landmarks128.txt").toPath(),(s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
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
    private long touch(long down,int action,float x0,float x1,float y,int count){
        MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];
        for(int i=0;i<count;i++){props[i]=new MotionEvent.PointerProperties();props[i].id=i;props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=i==0?x0:x1;coords[i].y=y;coords[i].pressure=1;coords[i].size=1;}
        long time=SystemClock.uptimeMillis();
        var e=MotionEvent.obtain(down,time,action,count,props,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        // Wait for EACH real input event to finish dispatch. A 20ms producer is
        // faster than the software-rendered UI; async MOVE events can be batched
        // past the detector's scale-begin threshold before it sees another move.
        try{if(!getUiAutomation().injectInputEvent(e,true))throw new AssertionError("touch injection rejected action="+action);}
        finally{e.recycle();}
        SystemClock.sleep(20);return time;
    }
    private Rect viewport(FilamentMapView renderer){Rect rect=new Rect();runOnMainSync(()->renderer.getGlobalVisibleRect(rect));return rect;}
    private float span(FilamentMapView renderer){float[] value={0};runOnMainSync(()->value[0]=renderer.camera.span);return value[0];}
    private float pinchSample(FilamentMapView renderer,long eventTime,int attempt,int step,float separation)throws Exception{
        long[] received={-1};float[] value={0,0};boolean[] flags={false,false,false};
        runOnMainSync(()->{try{
            ScaleGestureDetector detector=(ScaleGestureDetector)field(renderer,"scaler");
            received[0]=detector.getEventTime();value[0]=renderer.camera.span;value[1]=detector.getCurrentSpan();
            flags[0]=detector.isInProgress();flags[1]=(Boolean)field(renderer,"miniGesture");flags[2]=(Boolean)field(renderer,"panelGesture");
        }catch(Exception e){throw new RuntimeException(e);}});
        log("pinch attempt="+attempt+" step="+step+" injectedTime="+eventTime+" receivedTime="+received[0]+" pointerSpan="+separation+" detectorSpan="+value[1]+" scaling="+flags[0]+" mini="+flags[1]+" panel="+flags[2]+" cameraSpan="+value[0]);
        check(received[0]==eventTime&&!flags[1]&&!flags[2],"actual pinch MOVE reaches production scale detector without UI interception");
        return value[0];
    }
    private void pinch(FilamentMapView renderer,float target)throws Exception{
        // Observe state on its owning thread and stop moving fingers when the
        // requested scale is reached. All camera changes still come exclusively
        // from injected touchscreen events through the production input path.
        for(int attempt=0;attempt<4;attempt++){
            float current=span(renderer);
            if(target<10?current>4&&current<8:current>12&&current<20)return;
            Rect r=viewport(renderer);float x=r.exactCenterX(),y=r.top+r.height()*.45f;
            boolean in=current>target;float start=r.width()*(in?.19f:.34f),end=r.width()*(in?.34f:.18f);
            check(r.contains((int)(x-Math.max(start,end)),(int)y)&&r.contains((int)(x+Math.max(start,end)),(int)y),"entire real pinch stays inside visible map");
            ViewConfiguration config=ViewConfiguration.get(renderer.getContext());
            int minimumSpan=Build.VERSION.SDK_INT>=29?config.getScaledMinimumScalingSpan():-1;
            log("pinch target="+target+" attempt="+attempt+" viewport="+r+" y="+y+" startSpan="+(2*start)+" endSpan="+(2*end)+" minimumSpan="+minimumSpan+" touchSlop="+config.getScaledTouchSlop()+" cameraBefore="+current);
            long down=SystemClock.uptimeMillis();
            touch(down,MotionEvent.ACTION_DOWN,x-start,x+start,y,1);
            touch(down,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),x-start,x+start,y,2);
            float d=start;
            try{
                for(int i=0;i<=20;i++){
                    d=start+(end-start)*i/20;
                    long time=touch(down,MotionEvent.ACTION_MOVE,x-d,x+d,y,2);
                    current=pinchSample(renderer,time,attempt,i,2*d);
                    if(in?current<=target:current>=target)break;
                }
            }finally{
                touch(down,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),x-d,x+d,y,2);
                touch(down,MotionEvent.ACTION_UP,x-d,x+d,y,1);
            }
            settle();log("pinch target="+target+" attempt="+attempt+" cameraAfter="+span(renderer));
        }
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
        String label=mode.equals("hukou")?"黄河壶口瀑布":mode.equals("southwest")?"西南山涧瀑布":mode.equals("lushan")?"庐山区域瀑布":mode.equals("greatwall")?"北方长城":mode.equals("taihu")?"太湖烟波":mode.equals("poison")?"西南毒泉":"泰山区域瀑布";
        tapText("视图");tapText("山河地标");capture(mode+"-landmark-menu-ui");tapText(label);
        check(host.is3D(),"normal landmark menu initializes native 3D");FilamentMapView renderer=(FilamentMapView)field(host,"spatial");ready();
        var entry=LandscapeLandmarks.ALL.stream().filter(e->e.label().equals(label)).findFirst().orElseThrow();Hex at=MapCoordinates.fromNationalSource(world,new game.sanguo.core.map.SourceGridCoord(entry.x(),entry.y()));
        var ground=((MapSceneSnapshot)field(renderer,"snapshot")).ground;
        check(Math.abs(renderer.camera.x-ground.grid.x(at))<.01&&Math.abs(renderer.camera.z-ground.grid.z(at))<.01,"normal menu centers actual production anchor");
        check(Math.abs(renderer.camera.span-10)<.1,"normal focus scale, not a special demo camera");double start=flow(renderer);pair(mode+"-menu-span10");
        pinch(renderer,6);settle();ready();check(renderer.camera.span>4&&renderer.camera.span<8,"actual pinch reaches near zoom");pair(mode+"-near-pinch");
        tapText("视图");tapText("3D 反向查看");settle();ready();pair(mode+"-near-reverse");
        tapText("视图");tapText("3D 反向查看");settle();ready();
        pinch(renderer,15);settle();ready();check(renderer.camera.span>12&&renderer.camera.span<20,"actual pinch reaches ordinary zoom");pair(mode+"-ordinary-pinch");
        float x=renderer.camera.x,z=renderer.camera.z;pan(renderer);settle();ready();check(Math.hypot(renderer.camera.x-x,renderer.camera.z-z)>.01,"actual pointer pan moves map");pair(mode+"-ordinary-pan");
        tapText("视图");tapText("3D 反向查看");settle();ready();pair(mode+"-reverse");
        int water=0,foam=0,wall=0,shore=0;for(Object gpu:((Map<?,?>)field(renderer,"vegetation")).values()){
            SceneMesh m=(SceneMesh)field(gpu,"source");for(int i=0;i<m.uv.length;i+=2){float u=m.uv[i],v=m.uv[i+1];if(mode.equals("hukou")?u>.375f&&u<.5f:u>.625f&&u<.75f)water++;if(u>.5f&&u<.625f&&v>.65f)foam++;int k=i/2*7;if(m.vertices[k+3]==.98f&&m.vertices[k+4]==.87f&&m.vertices[k+5]==.69f)wall++;Hex cell=ground.grid.cell(m.vertices[k],m.vertices[k+2]);if(ground.valid(cell)&&ground.terrain[cell.r*ground.width+cell.q]==World.Terrain.NON_NAVIGABLE_WATER.ordinal()&&((int)(u*8)==0||(int)(u*8)==6))shore++;}
        }
        if(mode.equals("poison")){
            int hazard=0;
            for(Object gpu:((Map<?,?>)field(renderer,"terrain")).values())if((Boolean)field(gpu,"shown")){
                SceneMesh m=(SceneMesh)field(gpu,"source");if(m.surfaceData==null)continue;
                for(int k=0;k<m.vertices.length/7;k++)if(m.surfaceData[k*8+7]>1.05f){
                    Hex cell=ground.grid.cell(m.vertices[k*7],m.vertices[k*7+2]);
                    if(ground.valid(cell)&&ground.terrain[cell.r*ground.width+cell.q]==World.Terrain.POISON.ordinal())hazard++;
                }
            }
            check(hazard>0,"real visible GPU terrain carries poison-spring material signal");
            check(field(renderer,"poisonMaterialGround")==ground,"admitted material layout matches actual Ground");
        }
        else if(mode.equals("greatwall"))check(wall>0,"actual resident GPU earth wall survives normal zoom and pan");
        else if(mode.equals("taihu"))check(shore>0,"actual resident GPU shore dressing survives normal zoom and pan");
        else check(water>0&&foam>0,"actual resident GPU fall and foam survive normal zoom and pan");
        runOnMainSync(()->host.setGridShown(true));settle();ready();pair(mode+"-grid-on");
        runOnMainSync(()->host.setGridShown(false));settle();ready();pair(mode+"-grid-off");check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no asset fallback");check(flow(renderer)>start,"flow uniform advances on real admitted frames");
        check(Arrays.equals(before,authority()),"menu and actual gestures preserve complete authority and RNG");Files.write(new File(dir,"expected.sg11").toPath(),before);Files.write(new File(dir,"observed.sg11").toPath(),authority());
        log("PASS LANDMARK128 mode="+mode+" checks="+checks+" water="+water+" foam="+foam+" wall="+wall+" shore="+shore+" startFlow="+start+" endFlow="+flow(renderer));result.putString("stream","PASS LANDMARK128 "+mode+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL LANDMARK128 "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture(mode+"-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}

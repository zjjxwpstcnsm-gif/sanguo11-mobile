package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;

/** Real window pointer injection, not calls to showTerrain or the map hit helper.
 * Uses only v056 APIs so the same test APK can reproduce the unmodified baseline. */
class MapTap57Harness {
    final Instrumentation test; MainActivity activity; int checks;
    final StringBuilder report=new StringBuilder();
    MapTap57Harness(Instrumentation t){test=t;}
    void launch(World w)throws Exception{
        if(activity!=null){ui(activity::finish);settle();}
        writeInternal("auto.sg11",SaveCodec.encode(w));
        test.getTargetContext().getSharedPreferences("map-display",0).edit().clear().putBoolean("navigator",true).commit();
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        settle();require(world().scenarioId.equals(w.scenarioId),"shipped scenario loaded "+w.scenarioId);
        ui(this::page);settle();
    }
    void writeInternal(String name,byte[] bytes)throws Exception{try(OutputStream o=test.getTargetContext().openFileOutput(name,0)){o.write(bytes);}}
    byte[] readInternal(String name)throws Exception{try(InputStream in=test.getTargetContext().openFileInput(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[16384];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);return out.toByteArray();}}
    World world()throws Exception{return (World)field(activity,"world");}
    MapView map()throws Exception{return (MapView)field(activity,"map");}
    MapCamera camera()throws Exception{return (MapCamera)field(map(),"camera");}
    void page()throws Exception{ClientState ui=(ClientState)field(activity,"ui");ui.page="map";ui.panelVisible=false;ui.panelExpanded=false;activity.refresh();}
    void focus(Hex h,float scale)throws Exception{ui(()->{page();map().focus(h);camera().zoom(scale,map().getWidth()/2f,map().getHeight()/2f);map().center(h);});settle();}
    float[] screen(Hex h)throws Exception{return new float[]{((Number)invoke(map(),"x",h)).floatValue()*camera().scale+camera().x,((Number)invoke(map(),"y",h)).floatValue()*camera().scale+camera().y};}
    void tap(Hex h)throws Exception{float[] xy=screen(h);require(xy[0]>=0&&xy[0]<map().getWidth()&&xy[1]>=0&&xy[1]<map().getHeight(),"touch in real viewport "+h);record(h,xy);tapPoint(xy[0],xy[1]);}
    void record(Hex h,float[] xy)throws Exception{
        World w=world();War.Structure s=w.war.at(h);
        String row="scenario="+w.scenarioId+" map="+w.mapId+" revision="+w.mapRevision+" national="+MapCoordinates.nationalSource(w,h)+" local="+MapCoordinates.source(w,h)+" axial="+h+" terrain="+(w.sourceInside(h)?w.terrain[h.q][h.r]:"PADDING")+" structure="+(s==null?"none":s.kind+"/"+s.id+"/owner="+s.owner)+" screen="+xy[0]+","+xy[1]+" scale="+camera().scale+" dispatch=Instrumentation.sendPointerSync->MapView.onSingleTapUp\n";
        report.append(row);android.util.Log.i("Map57Test",row);
    }
    void tapPoint(float x,float y)throws Exception{int[] loc=new int[2];map().getLocationOnScreen(loc);long now=SystemClock.uptimeMillis();send(now,now,MotionEvent.ACTION_DOWN,x+loc[0],y+loc[1]);send(now,now+80,MotionEvent.ACTION_UP,x+loc[0],y+loc[1]);settle();}
    void send(long down,long time,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,time,action,x,y,0);test.sendPointerSync(e);e.recycle();}
    void pan()throws Exception{ui(this::page);settle();int[] loc=new int[2];map().getLocationOnScreen(loc);float x=loc[0]+map().getWidth()*.5f,y=loc[1]+map().getHeight()*.5f;long now=SystemClock.uptimeMillis();send(now,now,0,x,y);for(int i=1;i<=8;i++)send(now,now+i*45,2,x+12*i,y-8*i);send(now,now+410,1,x+96,y-64);settle();SystemClock.sleep(1000);}
    void pinch()throws Exception{
        MapView v=map();int[]loc=new int[2];v.getLocationOnScreen(loc);
        float cx=v.getWidth()*.5f+loc[0],cy=v.getHeight()*.5f+loc[1];
        float start=v.getWidth()*.20f,end=v.getWidth()*.44f,before=camera().scale;
        int minimum=Build.VERSION.SDK_INT>=29?ViewConfiguration.get(test.getTargetContext()).getScaledMinimumScalingSpan():-1;
        report.append("PINCH minimumSpan=").append(minimum).append(" inputSpan=").append(start*2).append("..").append(end*2).append(" before=").append(before).append(" max=").append(camera().maxScale).append('\n');
        MotionEvent.PointerProperties[] pp={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};
        MotionEvent.PointerCoords[] pc={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};
        for(int i=0;i<2;i++){pp[i].id=i;pp[i].toolType=MotionEvent.TOOL_TYPE_FINGER;pc[i].pressure=1;pc[i].size=1;pc[i].y=cy;pc[i].x=cx+(i==0?-start:start);}
        long t=SystemClock.uptimeMillis();multi(t,t,MotionEvent.ACTION_DOWN,1,pp,pc);SystemClock.sleep(40);
        multi(t,SystemClock.uptimeMillis(),MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,pp,pc);SystemClock.sleep(40);
        for(int i=1;i<=16;i++){float half=start+(end-start)*i/16f;pc[0].x=cx-half;pc[1].x=cx+half;multi(t,SystemClock.uptimeMillis(),MotionEvent.ACTION_MOVE,2,pp,pc);SystemClock.sleep(35);}
        multi(t,SystemClock.uptimeMillis(),MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,pp,pc);SystemClock.sleep(40);
        multi(t,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,1,pp,pc);settle();
        report.append("PINCH after=").append(camera().scale).append('\n');
    }
    void navigator(boolean shown)throws Exception{
        if(map().navigatorShown()!=shown){RectF b=(RectF)field(map(),"miniButton");tapPoint(b.centerX(),b.centerY());}
        require(map().navigatorShown()==shown,"actual navigator button toggles "+shown);
    }
    void multi(long down,long time,int action,int n,MotionEvent.PointerProperties[] pp,MotionEvent.PointerCoords[] pc){MotionEvent e=MotionEvent.obtain(down,time,action,n,pp,pc,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);test.sendPointerSync(e);e.recycle();}
    String panelText()throws Exception{return text((View)field(activity,"panel"));}
    String text(View v){StringBuilder s=new StringBuilder();if(v instanceof TextView)s.append(((TextView)v).getText()).append('\n');if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)s.append(text(g.getChildAt(i)));}return s.toString();}
    void showPanel()throws Exception{ui(()->{ClientState state=(ClientState)field(activity,"ui");state.panelVisible=true;activity.refresh();});settle();}
    void shot(String name)throws Exception{Bitmap b=test.getUiAutomation().takeScreenshot();require(b!=null,"real Android screenshot "+name);try(OutputStream out=new FileOutputStream(new File(dir(),name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    File dir(){File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();return dir;}
    void flush(String filename)throws Exception{try(Writer w=new OutputStreamWriter(new FileOutputStream(new File(dir(),filename)),"UTF-8")){w.write("Assertions="+checks+"\n"+report);}}
    void require(boolean ok,String label){checks++;report.append(ok?"PASS ":"FAIL ").append(label).append('\n');if(!ok)throw new AssertionError(label);}
    interface Checked{void run()throws Exception;}
    void ui(Checked action)throws Exception{Throwable[] error={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    void settle(){test.waitForIdleSync();SystemClock.sleep(300);test.waitForIdleSync();}
    static Object field(Object o,String n)throws Exception{for(Class<?> t=o.getClass();t!=null;t=t.getSuperclass())try{Field f=t.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){}throw new NoSuchFieldException(n);}
    static Object invoke(Object o,String name,Hex h)throws Exception{Method m=o.getClass().getDeclaredMethod(name,Hex.class);m.setAccessible(true);return m.invoke(o,h);}
}

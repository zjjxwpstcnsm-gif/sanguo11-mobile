package game.sanguo.mobile;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Installed-app checks; uses the actual widgets and official national scene, not a mock. */
final class Polish46Probe {
    private final Instrumentation test;
    private MainActivity activity;
    private MapView map;
    private World world;
    private int checks;
    private final StringBuilder report=new StringBuilder();
    Polish46Probe(Instrumentation test){this.test=test;}
    void run() throws Exception {
        World national=TestScenarios.load("heroes-250",0);
        try(OutputStream out=test.getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(national));}
        test.getTargetContext().getSharedPreferences("map-display",0).edit().clear().commit();
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        world=(World)field(activity,"world");map=(MapView)field(activity,"map");
        byte[] before=SaveCodec.encode(world);
        for(boolean portrait:new boolean[]{true,false}){
            test.runOnMainSync(()->activity.setRequestedOrientation(portrait?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();
            test.runOnMainSync(()->{page("map",false);map.fit();});settle();
            int width=map.getWidth(),height=map.getHeight();
            require(width>0&&height>0,"map has measured bounds");
            shot(portrait?"01-portrait-map":"04-landscape-map");
            test.runOnMainSync(()->activity.selectAndFocus(world.home().hex));settle();
            require(map.getWidth()==width&&map.getHeight()==height,"opening city keeps map bounds "+portrait);
            View panel=(View)field(activity,"panelShell");require(panel.getVisibility()==View.VISIBLE,"city panel visible");
            require(panel.getWidth()>0&&panel.getHeight()>0,"city panel measured");
            int builds=map.sceneBuilds();
            test.runOnMainSync(()->{for(int i=0;i<30;i++)activity.refresh();});settle();
            require(map.sceneBuilds()==builds,"30 view refreshes reuse scene "+portrait);
            CommandStats stats=find(panel,CommandStats.class);require(stats!=null,"city metrics present");
            android.graphics.Rect shown=new android.graphics.Rect();stats.getGlobalVisibleRect(shown);
            require(shown.height()==stats.getHeight(),"city metrics fully visible in collapsed panel "+portrait);
            shot(portrait?"02-portrait-city":"05-landscape-city");
            test.runOnMainSync(()->page("officers",true));settle();
            DataTable<?> table=find(activity.getWindow().getDecorView(),DataTable.class);
            require(table!=null,"native officer table exists");
            HorizontalScrollView horizontal=(HorizontalScrollView)field(table,"horizontal");
            View grid=(View)field(table,"grid");
            require(grid.getWidth()<=horizontal.getWidth()-horizontal.getPaddingLeft()-horizontal.getPaddingRight()+3,
                "abilities fit available table width "+portrait+" grid="+grid.getWidth()+" viewport="+horizontal.getWidth()+" padding="+horizontal.getPaddingLeft()+","+horizontal.getPaddingRight()+" table="+table.getWidth());
            int total=table.list.getAdapter().getCount();require(total>0,"officer rows bound");
            test.runOnMainSync(()->table.search.setText("NO_OFFICER_46"));settle();require(table.list.getAdapter().getCount()==0,"search filters rows");
            test.runOnMainSync(()->table.search.setText(""));settle();require(table.list.getAdapter().getCount()==total,"clearing restores rows");
            LinearLayout groups=(LinearLayout)field(table,"groups");
            test.runOnMainSync(()->groups.getChildAt(1).performClick());settle();require(table.list.getAdapter().getCount()==total,"column group keeps rows");
            test.runOnMainSync(()->groups.getChildAt(0).performClick());settle();
            shot(portrait?"03-portrait-officers":"06-landscape-officers");
            test.runOnMainSync(()->{page("map",false);map.center(world.home().hex);});settle();
            require(map.getWidth()==width&&map.getHeight()==height,"closing panel restores same map bounds "+portrait);
        }
        test.runOnMainSync(()->{page("map",false);map.focus(world.home().hex);});settle();
        MapCamera camera=(MapCamera)field(map,"camera");
        final boolean[] changed={false};
        test.runOnMainSync(()->{
            float qOffset=world.sourceMapWidth>0?(world.height-1)/2:0;
            float x=25*1.7320508f*(world.home().hex.q+world.home().hex.r*.5f-qOffset)*camera.scale+camera.x;
            float y=25*1.5f*world.home().hex.r*camera.scale+camera.y;
            long now=SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(now,now+40,MotionEvent.ACTION_UP,x,y,0);
            map.onTouchEvent(down);map.onTouchEvent(up);down.recycle();up.recycle();
            try{changed[0]=((ClientState)field(activity,"ui")).panelVisible;}catch(Exception e){throw new RuntimeException(e);}
        });
        require(changed[0],"single tap selects immediately without double-tap timeout");settle();
        test.runOnMainSync(()->{
            map.fit();
            long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,map.getWidth()/2f,map.getHeight()/2f,0);map.onTouchEvent(down);down.recycle();
            MotionEvent cancel=MotionEvent.obtain(now,now+1,MotionEvent.ACTION_CANCEL,map.getWidth()/2f,map.getHeight()/2f,0);map.onTouchEvent(cancel);cancel.recycle();
        });
        require(!map.cameraMoving(),"finger interrupts camera motion");
        require(Arrays.equals(before,SaveCodec.encode(world)),"all presentation interactions preserve authoritative world");
        File dir=test.getTargetContext().getExternalFilesDir("smoke");
        try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(dir,"v046-checks.txt")),"UTF-8")){out.write("PASS: "+checks+" installed UI checks\n"+report);}
    }
    private void page(String name,boolean visible){try{ClientState ui=(ClientState)field(activity,"ui");ui.page=name;ui.panelVisible=visible;ui.panelExpanded=!name.equals("map");activity.refresh();}catch(Exception e){throw new RuntimeException(e);}}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(400);test.waitForIdleSync();}
    private void require(boolean result,String description){if(!result)throw new AssertionError(description);checks++;report.append("PASS ").append(description).append('\n');}
    private void shot(String name)throws Exception {Bitmap image=test.getUiAutomation().takeScreenshot();require(image!=null,"screenshot "+name);File dir=test.getTargetContext().getExternalFilesDir("smoke");if(dir==null)throw new IOException("No screenshot directory");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,"v046-"+name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    private static Object field(Object target,String name)throws Exception {Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private static <T>T find(View view,Class<T> kind){if(kind.isInstance(view))return kind.cast(view);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){T found=find(group.getChildAt(i),kind);if(found!=null)return found;}}return null;}
}

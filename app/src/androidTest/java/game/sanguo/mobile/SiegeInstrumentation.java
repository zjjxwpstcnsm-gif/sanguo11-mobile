package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.TextView;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** Runs the actual activity and MapView from the installed APK, with screenshots. */
public final class SiegeInstrumentation extends Instrumentation {
    private MainActivity activity;
    private MapView map;
    private World world;
    private int checks;
    private final StringBuilder report=new StringBuilder();
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            World fixture=Ux64Fixture.create();World.Officer o=new World.Officer(100,"围城验证",1,20,80,80,80,80,80);
            o.cityId=-1;o.unitId=fixture.nextUnitId;
            fixture.officers.add(o);fixture.units.add(new World.Unit(fixture.nextUnitId++,1,o.id,World.Weapon.SPEAR,new Hex(6,3),6000,20000));
            try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(fixture));}
            getTargetContext().getSharedPreferences("map-display",0).edit().clear().putBoolean("navigator",false).commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            world=(World)field(activity,"world");map=(MapView)field(activity,"map");
            byte[] before=SaveCodec.encode(world);
            for(boolean portrait:new boolean[]{true,false}){
                runOnMainSync(()->activity.setRequestedOrientation(portrait?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();
                for(int id:new int[]{10,13,14}){
                    World.City c=world.city(id);
                    runOnMainSync(()->activity.selectAndFocus(c.hex));settle();
                    require(new HashSet<>(map.siegeCoverage()).equals(new HashSet<>(SiegeRules.cells(world,c))),"actual selected "+c.kind+" cells match simulation "+portrait);
                    require(visibleText(activity.getWindow().getDecorView(),"两圈围城范围"),"visible range legend "+c.kind);
                    if(id==10){require(map.siegeEnemies().contains(new Hex(6,3)),"besieger red-cell model");require(visibleText(activity.getWindow().getDecorView(),"钱粮−25%"),"visible siege penalty");}
                    shot((portrait?"portrait-":"landscape-")+c.kind.name());
                }
                World.City c=world.city(10);
                for(Hex h:SiteFootprint.cells(c)){
                    runOnMainSync(()->activity.selectAndFocus(h));settle();
                    require(map.siegeCoverage().size()==30,"every one of seven city cells selects same siege ring");
                }
                int builds=map.sceneBuilds();runOnMainSync(()->{for(int i=0;i<20;i++)activity.refresh();});settle();
                require(map.sceneBuilds()==builds,"refreshes reuse scene, no national per-frame siege scan");
                runOnMainSync(()->activity.selectAndFocus(new Hex(17,11)));settle();require(map.siegeCoverage().isEmpty(),"non-site selection clears range");
            }
            require(Arrays.equals(before,SaveCodec.encode(world)),"selection and rotation never mutate authoritative world");
            // Native, shipped odd-column 200x200 geometry, not only a synthetic test map.
            World national=TestScenarios.load("heroes-250",0);
            runOnMainSync(()->{
                try{Method activate=MainActivity.class.getDeclaredMethod("activateWorld",World.class);activate.setAccessible(true);activate.invoke(activity,national);}
                catch(Exception e){throw new IllegalStateException(e);}
            });world=national;
            for(World.SiteKind kind:World.SiteKind.values()){
                World.City c=national.cities.stream().filter(site->site.kind==kind).findFirst().orElseThrow(()->new AssertionError("Missing site kind "+kind));
                runOnMainSync(()->activity.selectAndFocus(c.hex));settle();
                require(map.siegeCoverage().equals(SiegeRules.cells(national,c)),"native national coordinates "+kind);shot("native-"+kind.name());
            }
            File dir=directory();try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(dir,"siege-android-checks.txt")),"UTF-8")){out.write("PASS: "+checks+" installed Android checks\n"+report);}
            result.putString("stream","SIEGE ANDROID PASS: "+checks+" checks\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){StringWriter error=new StringWriter();e.printStackTrace(new PrintWriter(error));result.putString("stream","SIEGE ANDROID FAIL\n"+error);finish(Activity.RESULT_CANCELED,result);}
    }
    private void require(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;report.append("PASS ").append(message).append('\n');}
    private void settle(){waitForIdleSync();SystemClock.sleep(300);waitForIdleSync();}
    private File directory()throws IOException{File dir=getTargetContext().getExternalFilesDir("smoke");if(dir==null)throw new IOException("No evidence directory");dir.mkdirs();return dir;}
    private void shot(String name)throws Exception{Bitmap image=getUiAutomation().takeScreenshot();require(image!=null,"screenshot "+name);try(OutputStream out=new FileOutputStream(new File(directory(),"siege-"+name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    private static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private static boolean visibleText(View v,String needle){
        if(v instanceof TextView&&((TextView)v).getText().toString().contains(needle)){
            android.graphics.Rect bounds=new android.graphics.Rect();
            return v.getGlobalVisibleRect(bounds)&&bounds.height()>=v.getHeight()/2;
        }
        if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)if(visibleText(group.getChildAt(i),needle))return true;}
        return false;
    }
}

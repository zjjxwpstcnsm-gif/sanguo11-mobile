package game.sanguo.mobile;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.Button;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Installed real-Activity verification of rendering, controls, retention and authoritative saves. */
final class Turn47Probe {
    private final Instrumentation test;private MainActivity activity;private int checks;
    private final StringBuilder report=new StringBuilder();
    Turn47Probe(Instrumentation test){this.test=test;}
    private World fixture(){
        World w=new World(24,14,"甲军","乙军");
        w.cities.add(new World.City(10,"甲城",new Hex(1,1),0));w.cities.add(new World.City(20,"乙城",new Hex(20,10),1));w.cities.add(new World.City(21,"乙后城",new Hex(22,1),1));
        World.Officer a=new World.Officer(0,"我将",0,-1,90,90,90,90,90);a.role=Strategy.Role.RULER;a.loyalty=100;a.unitId=1;Arrays.fill(a.aptitude,3);w.officers.add(a);
        World.Officer b=new World.Officer(1,"敌将",1,-1,60,60,60,60,60);b.role=Strategy.Role.RULER;b.loyalty=100;b.unitId=2;Arrays.fill(b.aptitude,3);w.officers.add(b);
        w.units.add(new World.Unit(1,0,0,World.Weapon.SPEAR,new Hex(3,6),8000,30000));w.units.add(new World.Unit(2,1,1,World.Weapon.SPEAR,new Hex(6,6),6000,30000));w.nextUnitId=3;return w;
    }
    void run()throws Exception{
        World before=fixture();byte[] initial=SaveCodec.encode(before);
        try(OutputStream out=test.getTargetContext().openFileOutput("auto.sg11",0)){out.write(initial);}
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        World after=SaveCodec.decode(initial);TurnJournal journal=new TurnJournal(after);
        require(after.move(1,new Hex(5,6)).ok,"fixture move uses real command");require(after.attack(1,2).ok,"fixture real attack");Method reset=UnitOrders.class.getDeclaredMethod("reset",World.Unit.class);reset.setAccessible(true);reset.invoke(after.orders,after.unit(1));
        require(after.war.plot(1,after.unit(2).hex,War.Plot.CONFUSE).ok,"fixture real plot");journal.close();require(journal.events().stream().anyMatch(e->e.kind==TurnJournal.Kind.PLOT),"real plot records explicit presentation event");byte[] expected=SaveCodec.encode(after);
        TurnWork work=new TurnWork((World)field(activity,"world"));work.after=after;work.visual=SaveCodec.decode(initial);work.events=journal.events();work.done=true;work.summary="演示验证";
        test.runOnMainSync(()->{try{
            set(activity,"turnWork",work);set(activity,"aiRunning",true);((MapView)field(activity,"map")).center(new Hex(5,6));call(activity,"finishTurn");
        }catch(Exception e){throw new RuntimeException(e);}});
        SystemClock.sleep(100);test.runOnMainSync(()->work.paused=true);settle();
        require(Arrays.equals(expected,readAuto()),"final authoritative result saved before playback");
        require((Boolean)field(activity,"aiRunning"),"commands remain locked during playback");
        Button controls=(Button)field(activity,"nextTurn");require(controls.isEnabled()&&controls.getText().toString().equals("演示控制"),"playback controls visible and enabled");
        float progress=work.fraction;int cursor=work.cursor;SystemClock.sleep(120);require(work.fraction==progress&&work.cursor==cursor,"pause stops presentation cursor");
        shot("01-movement-paused");
        test.runOnMainSync(controls::performClick);settle();
        require(!test.getUiAutomation().getRootInActiveWindow().findAccessibilityNodeInfosByText("跳过剩余演示").isEmpty(),"native controls expose skip");
        test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();
        test.runOnMainSync(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();
        activity=(MainActivity)field(test,"current");
        require(field(activity,"turnWork")==work&&work.paused&&work.cursor==cursor,"rotation retains same job and paused cursor");
        require(Arrays.equals(expected,readAuto()),"rotation never saves intermediate render state");shot("02-landscape-paused");
        test.runOnMainSync(()->{try{((MapView)field(activity,"map")).focus(new Hex(5,6));}catch(Exception e){throw new RuntimeException(e);}});settle();
        test.runOnMainSync(()->{try{MapCamera camera=(MapCamera)field(field(activity,"map"),"camera");float x=camera.x;camera.pan(-35,20);require(camera.x!=x,"camera can pan during playback");((MapView)field(activity,"map")).invalidate();}catch(Exception e){throw new RuntimeException(e);}});
        test.runOnMainSync(()->{work.speed=1;work.paused=false;});
        boolean[] captured={false,false};long deadline=SystemClock.uptimeMillis()+20000;
        while(SystemClock.uptimeMillis()<deadline&&(Boolean)field(activity,"aiRunning")){
            int[] shotKind={-1};
            // Pause on the UI thread before taking/compressing a screenshot: this can take longer
            // than a complete animation on a cold emulator and must not skip the next event.
            test.runOnMainSync(()->{try{
                MapView map=(MapView)field(activity,"map");TurnJournal.Event e=(TurnJournal.Event)field(map,"replayEvent");float f=(Float)field(map,"replayFraction");
                if(e!=null&&f>.35f){int index=e.kind==TurnJournal.Kind.ATTACK?0:e.kind==TurnJournal.Kind.PLOT?1:-1;
                    if(index>=0&&!captured[index]){work.paused=true;shotKind[0]=index;captured[index]=true;}}
            }catch(Exception e){throw new RuntimeException(e);}});
            if(shotKind[0]>=0){shot(shotKind[0]==0?"03-attack-hit":"04-stratagem");test.runOnMainSync(()->work.paused=false);}
            SystemClock.sleep(20);
        }
        boolean attack=captured[0],plot=captured[1];
        require(attack&&plot,"visible attack/hit and plot frames rendered");require(!(Boolean)field(activity,"aiRunning"),"playback finishes and releases commands");
        require(Arrays.equals(expected,SaveCodec.encode((World)field(activity,"world"))),"natural playback ends at exact computed result");
        // Exercise actual asynchronous next-turn job, then skip its presentation. No second simulation.
        TurnWork[] observed={null};
        test.runOnMainSync(()->{try{call(activity,"advanceTurn");observed[0]=(TurnWork)field(activity,"turnWork");if(observed[0]!=null)observed[0].paused=true;}catch(Exception e){throw new RuntimeException(e);}});
        deadline=SystemClock.uptimeMillis()+20000;TurnWork actual=observed[0];
        while(SystemClock.uptimeMillis()<deadline&&actual!=null&&!actual.done)SystemClock.sleep(40);
        require(actual!=null&&actual.done&&actual.error==null,"actual next-turn worker computes successfully");
        byte[] finalTurn=SaveCodec.encode(actual.after);final TurnWork actualWork=actual;
        test.runOnMainSync(()->{try{actualWork.speed=4;Object playback=field(activity,"playback");if(playback!=null)call(playback,"skip");}catch(Exception e){throw new RuntimeException(e);}});
        deadline=SystemClock.uptimeMillis()+8000;while(SystemClock.uptimeMillis()<deadline&&(Boolean)field(activity,"aiRunning"))SystemClock.sleep(40);
        require(!(Boolean)field(activity,"aiRunning"),"skip completes without stuck input lock");
        require(Arrays.equals(finalTurn,SaveCodec.encode((World)field(activity,"world")))&&Arrays.equals(finalTurn,readAuto()),"skip preserves final world and auto-save bytes");
        shot("05-turn-complete");
        File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();
        try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(dir,"v047-checks.txt")),"UTF-8")){out.write("PASS "+checks+" installed checks\n"+report);}
    }
    private byte[] readAuto()throws Exception{try(InputStream in=test.getTargetContext().openFileInput("auto.sg11");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return out.toByteArray();}}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(400);test.waitForIdleSync();}
    private void require(boolean condition,String msg){if(!condition)throw new AssertionError(msg);checks++;report.append("PASS ").append(msg).append('\n');}
    private void shot(String name)throws Exception{Bitmap b=test.getUiAutomation().takeScreenshot();require(b!=null,"screenshot "+name);File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,"v047-"+name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    private static void call(Object object,String name)throws Exception{Method m=object.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(object);}
}

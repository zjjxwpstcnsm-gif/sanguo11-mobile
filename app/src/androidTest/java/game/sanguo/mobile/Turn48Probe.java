package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Installed checks: actual retained worker plus native deployment controls and facility frames. */
final class Turn48Probe {
    private final Instrumentation test;private MainActivity activity;private int checks;
    private final StringBuilder report=new StringBuilder();
    Turn48Probe(Instrumentation test){this.test=test;}
    void run()throws Exception{
        World fixture=Turn48Fixture.world();byte[] initial=SaveCodec.encode(fixture);
        try(OutputStream out=test.getTargetContext().openFileOutput("auto.sg11",0)){out.write(initial);}
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        streaming(initial);facilities();deployment();
        File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();
        try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(dir,"v048-checks.txt")),"UTF-8")){out.write("PASS "+checks+" installed checks\n"+report);}
    }
    private void streaming(byte[] initial)throws Exception{
        World expected=SaveCodec.decode(initial);require(expected.nextTurn().ok,"reference turn succeeds");byte[] finalBytes=SaveCodec.encode(expected);
        TurnWork[] holder={null};ui(()->{call(activity,"advanceTurn");holder[0]=(TurnWork)field(activity,"turnWork");holder[0].paused=true;((MapView)field(field(activity,"map"),"flat")).center(new Hex(9,8));});
        TurnWork work=holder[0];long deadline=SystemClock.uptimeMillis()+20000;
        while(SystemClock.uptimeMillis()<deadline&&!work.waitingForPlayback&&!work.done)SystemClock.sleep(20);settle();
        require(work.batchReady&&!work.done&&work.error==null,"first faction is presented before whole turn completion");
        require(field(activity,"playback")!=null,"playback starts while actual worker is unfinished");
        int batches=work.publishedBatches,cursor=work.cursor;SystemClock.sleep(180);
        require(work.cursor==cursor&&work.paused&&!work.waitingForPlayback,"v52 presentation pause no longer blocks subsequent faction computation");
        require(Arrays.equals(work.done?finalBytes:initial,readAuto()),"only complete authoritative worlds are saved");
        Button controls=(Button)field(activity,"nextTurn");require(controls.isEnabled(),"controls stay enabled during a streamed turn");
        ui(()->{work.speed=2;MapView map=(MapView)field(field(activity,"map"),"flat");MapCamera camera=(MapCamera)field(map,"camera");camera.zoom(camera.maxScale,map.getWidth()/2f,map.getHeight()/2f);map.center(new Hex(9,8));float x=camera.x;camera.pan(-25,12);map.invalidate();require(camera.x!=x,"zoomed camera can move while phase is paused");});
        shot("01-stream-paused");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();activity=(MainActivity)field(test,"current");
        require(field(activity,"turnWork")==work&&work.paused&&work.speed==2,"rotation retains the same worker, cursor and speed");
        require(Arrays.equals(work.done?finalBytes:initial,readAuto()),"rotation never serializes intermediate visual state");
        ui(()->activity.recreate());settle();activity=(MainActivity)field(test,"current");
        require(field(activity,"turnWork")==work&&work.paused&&work.speed==2&&!work.cancelled,"Activity recreation retains live producer and paused batch");
        ui(()->((TurnPlayback)field(activity,"playback")).skip());
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));settle();activity=(MainActivity)field(test,"current");
        require(work.skipAnimations,"skip preference belongs to retained work, not destroyed playback");
        deadline=SystemClock.uptimeMillis()+20000;
        while(SystemClock.uptimeMillis()<deadline&&(Boolean)field(activity,"aiRunning"))SystemClock.sleep(25);
        require(work.done&&work.error==null&&!(Boolean)field(activity,"aiRunning"),"streamed skip finishes and unlocks commands");
        require(Arrays.equals(finalBytes,SaveCodec.encode((World)field(activity,"world")))&&Arrays.equals(finalBytes,readAuto()),"streamed/skip final world and autosave exactly match unobserved engine");
        require(work.publishedBatches>=3&&work.computeMillis>=work.cloneMillis,"multiple phase handoffs and actual compute time recorded");
    }
    private void facilities()throws Exception{
        World before=Turn48Fixture.world(),after=SaveCodec.decode(SaveCodec.encode(before));TurnJournal journal=new TurnJournal(after);
        Turn48Fixture.facilityCommands(after);journal.close();
        TurnWork work=new TurnWork(before);work.after=after;work.visual=SaveCodec.decode(SaveCodec.encode(before));work.events=journal.events();work.done=true;work.summary="设施动作实装验证";
        ui(()->{set(activity,"world",before);set(activity,"turnWork",work);set(activity,"aiRunning",true);((MapView)field(field(activity,"map"),"flat")).center(new Hex(9,8));call(activity,"finishTurn");});
        Set<TurnJournal.Kind> captured=new HashSet<>();long deadline=SystemClock.uptimeMillis()+20000;
        while(SystemClock.uptimeMillis()<deadline&&(Boolean)field(activity,"aiRunning")){
            TurnJournal.Kind[] kind={null};
            ui(()->{MapView map=(MapView)field(field(activity,"map"),"flat");TurnJournal.Event e=(TurnJournal.Event)field(map,"replayEvent");float f=(Float)field(map,"replayFraction");
                if(e!=null&&f>.36f&&!captured.contains(e.kind)&&(e.kind==TurnJournal.Kind.FACILITY_ATTACK||e.kind==TurnJournal.Kind.FACILITY_COUNTER||e.kind==TurnJournal.Kind.RECOVER)){work.paused=true;kind[0]=e.kind;captured.add(e.kind);}});
            if(kind[0]!=null){shot("02-"+kind[0].name().toLowerCase());ui(()->work.paused=false);}
            SystemClock.sleep(16);
        }
        require(captured.containsAll(Arrays.asList(TurnJournal.Kind.FACILITY_ATTACK,TurnJournal.Kind.FACILITY_COUNTER,TurnJournal.Kind.RECOVER)),"tower shot, camp retaliation and music actual-gain frames rendered");
        require(Arrays.equals(SaveCodec.encode(after),readAuto()),"facility animation does not modify the saved authoritative outcome");
    }
    private void deployment()throws Exception{
        World w=(World)field(activity,"world");World.City city=w.city(0);
        Bundle stale=new Bundle();stale.putString("kind","deploy");stale.putInt("city",city.id);stale.putInt("leader",2);stale.putInt("weapon",3);stale.putInt("ship",2);stale.putString("troops","8000");
        ui(()->{aRemember(stale);Bundle fresh=DeployWizard.start(activity,city,false);require(!fresh.containsKey("leader")&&!fresh.containsKey("weapon")&&!fresh.containsKey("troops")&&!fresh.containsKey("ship"),"new deployment discards every stale selection/quantity");new DeployWizard(activity,w,null,fresh).show();});settle();
        AlertDialog dialog=(AlertDialog)field(activity,"confirmationDialog");
        View weaponGrid=dialog.getWindow().getDecorView().findViewWithTag("deploy.weapons");
        require(weaponGrid!=null,"weapon grid visible in deployment overview");
        ui(()->{Button spear=(Button)dialog.getWindow().getDecorView().findViewWithTag("choice.SPEAR");require(spear!=null&&!spear.isSelected(),"new deployment starts with no chosen weapon");spear.performClick();
            require(field(activity,"confirmationDialog")==dialog&&spear.isSelected(),"weapon selection stays in same dialog with selected icon");
            Button ship=(Button)dialog.getWindow().getDecorView().findViewWithTag("choice.TOWER_SHIP");ship.performClick();
            require(field(activity,"confirmationDialog")==dialog&&ship.isSelected(),"ship selection is inline, not a nested picker");
        });shot("03-inline-deployment");
        ui(()->dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        require(activity.formDraft().isEmpty(),"cancel clears the closed deployment draft");
        ui(()->{Bundle fresh=DeployWizard.start(activity,city,false);fresh.putInt("leader",2);fresh.putInt("weapon",World.Weapon.SPEAR.ordinal());fresh.putString("troops","3000");fresh.putString("food","6000");new DeployWizard(activity,w,null,fresh).show();});settle();
        AlertDialog ready=(AlertDialog)field(activity,"confirmationDialog");
        require(ready.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"fully chosen fresh deployment can execute");
        int count=w.units.size();ui(()->ready.getButton(AlertDialog.BUTTON_POSITIVE).performClick());settle();
        require(w.units.size()==count+1&&activity.formDraft().isEmpty(),"successful native deployment executes once and clears draft");
        ui(()->new DeployWizard(activity,w,null,DeployWizard.start(activity,city,false)).show());settle();
        require(!activity.formDraft().containsKey("leader")&&!activity.formDraft().containsKey("weapon"),"second deployment does not inherit the successful first one");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();activity=(MainActivity)field(test,"current");shot("04-landscape-deployment");
        ui(()->((AlertDialog)field(activity,"confirmationDialog")).getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
    }
    private void aRemember(Bundle b){activity.rememberForm(b);}
    interface Action{void run()throws Exception;}
    private void ui(Action action){Throwable[] failure={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable e){failure[0]=e;}});if(failure[0]!=null)throw new AssertionError("UI check failed",failure[0]);}
    private byte[] readAuto()throws Exception{try(InputStream in=test.getTargetContext().openFileInput("auto.sg11");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return out.toByteArray();}}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(350);test.waitForIdleSync();}
    private void require(boolean condition,String msg){if(!condition)throw new AssertionError(msg);checks++;report.append("PASS ").append(msg).append('\n');}
    private void shot(String name)throws Exception{Bitmap b=test.getUiAutomation().takeScreenshot();require(b!=null,"screenshot "+name);File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,"v048-"+name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    private static void call(Object object,String name)throws Exception{Method m=object.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(object);}
}

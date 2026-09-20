package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Real installed controls and Activity recreation, not a separate UI mock. */
final class Reports53Probe {
    private final Instrumentation test;private MainActivity activity;private int checks;
    private final StringBuilder report=new StringBuilder();
    Reports53Probe(Instrumentation test){this.test=test;}
    void run()throws Exception{
        World initial=Personnel49Fixture.world();initial.reports.rebase();
        for(int t=0;t<3;t++){initial.turn=t;for(int i=0;i<65;i++)initial.note("本旬行动 "+t+"-"+i);}
        try(OutputStream out=test.getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(initial));}
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        reports();formation();
        File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(dir,"v053-checks.txt")),"UTF-8")){out.write("PASS "+checks+" installed checks\n"+report);}
        System.out.println("REPORTS53 INSTALLED CHECKS "+checks);
    }
    private World world()throws Exception{return (World)field(activity,"world");}
    private AlertDialog dialog()throws Exception{return (AlertDialog)field(activity,"confirmationDialog");}
    private View decor()throws Exception{return dialog().getWindow().getDecorView();}
    private View tag(String id)throws Exception{View v=decor().findViewWithTag(id);require(v!=null,"control exists: "+id);return v;}
    private void click(String id)throws Exception{View v=tag(id);ui(v::performClick);settle();}
    private void reports()throws Exception{
        View entry=activity.getWindow().getDecorView().findViewWithTag("reports.entry");require(entry!=null&&entry.getGlobalVisibleRect(new Rect()),"battle reports entry is permanently visible");ui(entry::performClick);settle();
        ListView list=(ListView)tag("reports.list");require(list.getAdapter().getCount()>40,"current turn contains more than the legacy 40 log entries");
        require(((TextView)tag("reports.count")).getText().toString().contains("65 条"),"default report filter shows exact current-turn results");
        Spinner time=(Spinner)tag("reports.turn");ui(()->time.setSelection(1));settle();require(((TextView)tag("reports.count")).getText().toString().contains("195 条"),"three-month filter includes all stored turns");
        EditText query=(EditText)tag("reports.search");ui(()->query.setText("1-64"));settle();require(((TextView)tag("reports.count")).getText().toString().contains("找到 1 条"),"keyword filter combines with time");
        ui(()->query.setText(""));settle();shot("01-battle-reports");ui(()->dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick());settle();
    }
    private void formation()throws Exception{
        World w=world();ui(()->new DeployWizard(activity,w,null,DeployWizard.start(activity,w.city(0),false)).show());settle();AlertDialog original=dialog();
        ListView list=(ListView)tag("deploy.officers");long[] ids=new long[3];for(int i=0;i<3;i++)ids[i]=list.getAdapter().getItemId(i);
        for(int i=0;i<3;i++){final int index=i;ui(()->list.setSelection(index));settle();View row=decor().findViewWithTag("deploy.officer."+ids[i]);require(row!=null,"officer row visible for selection");ui(row::performClick);settle();require(dialog()==original&&dialog().isShowing(),"choosing officer keeps same dialog open");}
        require(((Button)tag("deploy.crew.0")).getText().toString().contains(w.officer((int)ids[0]).name),"first selected officer becomes commander");
        require(((Button)tag("deploy.crew.2")).getText().toString().contains(w.officer((int)ids[2]).name),"third selected officer becomes second deputy");shot("02-formation-list");
        click("deploy.crew.1");require(((Button)tag("deploy.crew.0")).getText().toString().contains(w.officer((int)ids[1]).name),"deputy can be promoted without reopening picker");
        click("deploy.tab.1");click("choice.SPEAR");QuantityControl troops=(QuantityControl)tag("deploy.troops"),food=(QuantityControl)tag("deploy.food"),gold=(QuantityControl)tag("deploy.gold");
        ui(()->troops.set(4500));require(food.value()==9000,"troop edit automatically allocates twice the food");ui(()->{food.set(8000);gold.set(123);});
        click("deploy.tab.0");click("deploy.tab.2");require(troops.value()==4500&&food.value()==8000&&gold.value()==123,"all draft quantities survive free tab navigation");
        World.Unit preview=w.army.deploymentPreview(w.city(0),(int)ids[1],new int[]{(int)ids[0],(int)ids[2]},World.Weapon.SPEAR,Army.Ship.BOAT,4500,8000,123);
        String expected=String.format(Locale.ROOT,"攻击 %.1f",w.combat.attackRating(preview));require(((TextView)tag("deploy.attack")).getText().toString().equals(expected),"displayed attack equals formal combat calculation");
        require(((TextView)tag("deploy.details")).getText().toString().contains("防御取编队统率"),"detailed stat contribution explanation displayed");shot("03-sortie-details");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();activity=(MainActivity)field(test,"current");
        require(((QuantityControl)tag("deploy.troops")).value()==4500&&((QuantityControl)tag("deploy.food")).value()==8000,"rotation preserves edited quantities");
        require(((Button)tag("deploy.crew.0")).getText().toString().contains(world().officer((int)ids[1]).name),"rotation preserves reordered commander");
        require(tag("deploy.attack").getGlobalVisibleRect(new Rect()),"selected details tab survives rotation");
        require(dialog().getButton(AlertDialog.BUTTON_POSITIVE).getGlobalVisibleRect(new Rect()),"confirm button remains reachable in landscape");shot("04-landscape-sortie");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));settle();activity=(MainActivity)field(test,"current");
        World current=world();int units=current.units.size(),stock=current.city(0).troops;ui(()->dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick());settle();current=world();
        require(current.units.size()==units+1&&current.city(0).troops==stock-4500,"one confirmation deploys exactly one unit");World.Unit actual=current.unit(current.officer((int)ids[1]).unitId);
        require(actual!=null&&actual.deputies.length==2&&actual.food==8000&&actual.gold==123,"actual sortie carries selected crew and resources");require(w.combat.attackRating(preview)==current.combat.attackRating(actual),"preview matches deployed unit attack");
        require(!current.reports.query(-1,current.player,BattleReports.Scope.INITIATED,null,"出征").isEmpty(),"real deployed unit produces persistent battle report");
        World restored=SaveCodec.decode(SaveCodec.encode(current));require(restored.reports.size()==current.reports.size(),"actual UI command reports survive save replay");shot("05-deployed-map");
        World latest=current;ui(()->new DeployWizard(activity,latest,null,DeployWizard.start(activity,latest.city(0),false)).show());settle();require(((Button)tag("deploy.crew.0")).getText().toString().contains("待选"),"next sortie starts with empty crew instead of prior selection");ui(()->dialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
    }
    private interface Checked {void run()throws Exception;}
    private void ui(Checked action)throws Exception{Throwable[] error={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(700);test.waitForIdleSync();}
    private static Object field(Object object,String name)throws Exception{Class<?> type=object.getClass();while(type!=null){try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);}catch(NoSuchFieldException e){type=type.getSuperclass();}}throw new NoSuchFieldException(name);}
    private void require(boolean condition,String message){checks++;report.append(checks).append(' ').append(condition?"PASS ":"FAIL ").append(message).append('\n');if(!condition)throw new AssertionError(message);}
    private void shot(String name)throws Exception{File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();Bitmap image=test.getUiAutomation().takeScreenshot();require(image!=null,"actual window screenshot captured: "+name);try(OutputStream out=new FileOutputStream(new File(dir,"v053-"+name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
}

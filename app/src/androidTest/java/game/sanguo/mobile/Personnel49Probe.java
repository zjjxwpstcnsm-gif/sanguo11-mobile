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

/** Real installed Android controls, including touch-driven slider callbacks and Activity recreation. */
final class Personnel49Probe {
    private final Instrumentation test;private MainActivity activity;private int checks;
    private final StringBuilder report=new StringBuilder();
    Personnel49Probe(Instrumentation test){this.test=test;}
    void run()throws Exception{
        try(OutputStream out=test.getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(Personnel49Fixture.world()));}
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        statistics();deployment();recommendation();
        File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();
        try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(dir,"v049-checks.txt")),"UTF-8")){out.write("PASS "+checks+" installed checks\n"+report);}
    }
    private World world()throws Exception{return (World)field(activity,"world");}
    private AlertDialog dialog()throws Exception{return (AlertDialog)field(activity,"confirmationDialog");}
    private void statistics()throws Exception{
        World w=world();ui(()->activity.selectAndFocus(w.unit(2).hex));settle();
        View panel=(View)field(activity,"panel");require(contains(panel,"基础攻击")&&contains(panel,"基础防御"),"unit detail visibly contains computed attack/defense");
        require(contains(panel,"旬耗粮 600")&&contains(panel,"20 旬"),"unit detail visibly shows actual ration and endurance");shot("01-unit-stats");
    }
    private void openDeployment()throws Exception{
        World w=world();ui(()->{Bundle draft=DeployWizard.start(activity,w.city(0),false);draft.putInt("leader",1);draft.putInt("weapon",World.Weapon.SPEAR.ordinal());new DeployWizard(activity,w,null,draft).show();});settle();
    }
    private QuantityControl quantity(String tag)throws Exception{return (QuantityControl)dialog().getWindow().getDecorView().findViewWithTag(tag);}
    private void deployment()throws Exception{
        openDeployment();QuantityControl troops=quantity("deploy.troops"),food=quantity("deploy.food");
        require(troops.value()==3000&&food.value()==6000,"fresh form has twice troop rations");
        TextView ration=(TextView)dialog().getWindow().getDecorView().findViewWithTag("deploy.rations");
        require(ration!=null&&ration.getParent()==food&&ration.getText().toString().contains("20 旬"),"live endurance is placed beside food controls, not only below officers");
        ui(()->{EditText input=find(troops,EditText.class);input.setText("4500");});
        require(food.value()==9000,"exact soldier entry updates rations to twice soldiers");
        ui(()->find(food,EditText.class).setText("7000"));require(troops.value()==4500&&food.value()==7000,"manual ration adjustment remains possible");
        AlertDialog initial=dialog();ui(()->((Button)initial.getWindow().getDecorView().findViewWithTag("choice.SPEAR")).performClick());
        require(food.value()==7000,"same soldier count does not overwrite manual ration entry");
        SeekBar slider=find(troops,SeekBar.class);ui(()->slider.requestRectangleOnScreen(new android.graphics.Rect(0,0,slider.getWidth(),slider.getHeight()),false));settle();
        int[] p=new int[2];ui(()->slider.getLocationOnScreen(p));float x=p[0]+slider.getPaddingLeft()+(slider.getWidth()-slider.getPaddingLeft()-slider.getPaddingRight())*.61f,y=p[1]+slider.getHeight()/2f;
        long down=SystemClock.uptimeMillis();MotionEvent press=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,x,y,0),release=MotionEvent.obtain(down,down+80,MotionEvent.ACTION_UP,x,y,0);
        test.sendPointerSync(press);test.sendPointerSync(release);press.recycle();release.recycle();settle();
        require(troops.value()!=4500&&food.value()==2*troops.value(),"real slider touch updates both troop and ration quantities");
        ui(()->{troops.set(6000);food.set(9000);});require(food.value()==9000,"manual ration value prepared for recreation");
        ui(()->ration.requestRectangleOnScreen(new android.graphics.Rect(0,0,ration.getWidth(),ration.getHeight()),false));settle();
        require(ration.getGlobalVisibleRect(new android.graphics.Rect())&&ration.getText().toString().contains("15 旬"),"updated endurance is visible alongside the food input");shot("02-deployment");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();activity=(MainActivity)field(test,"current");
        require(quantity("deploy.troops").value()==6000&&quantity("deploy.food").value()==9000,"Activity rotation preserves manually edited rations");shot("03-landscape");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));settle();activity=(MainActivity)field(test,"current");
        ui(()->dialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        World w=world();ui(()->w.city(0).food=7000);openDeployment();QuantityControl limitedTroops=quantity("deploy.troops"),limitedFood=quantity("deploy.food");
        ui(()->limitedTroops.set(6000));require(limitedFood.value()==7000,"automatic food clamps to current city stock");
        require(contains(dialog().getWindow().getDecorView(),"11 旬"),"stock-limited endurance is shown as eleven full turns");
        ui(()->limitedTroops.set(10000));require(limitedTroops.value()==7000,"troop control respects the existing stock-limited departure cap");
        ui(()->limitedFood.set(6999));require(!dialog().getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"manual food below the existing departure minimum blocks submit");
        ui(()->limitedFood.set(7000));require(dialog().getButton(AlertDialog.BUTTON_POSITIVE).isEnabled(),"exact departure food minimum enables submit");
        ui(()->limitedTroops.set(6000));shot("04-stock-limit");
        ui(()->dialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick());ui(()->w.city(0).food=100000);
    }
    private void recommendation()throws Exception{
        World w=world();ui(()->{w.officer(1).affinity=50;w.officer(4).affinity=125;require(w.government.appointAdvisor(0,0,6).ok,"real adviser appointment command");new StrategyUi(activity,w,activity::applyResult).command(w.city(0),2);});settle();
        ListView targets=find(dialog().getWindow().getDecorView(),ListView.class);ui(()->clickId(targets,11));settle();
        View decor=dialog().getWindow().getDecorView();TextView advice=(TextView)decor.findViewWithTag("recruit.advice");
        require(advice!=null&&advice.getText().toString().contains("将6推荐")&&advice.getText().toString().contains("将1"),"adviser recommendation is visible text, not hidden accessibility metadata");
        require(contains(decor,"相性差")&&contains(decor,"成功率"),"recruiter table exposes sortable compatibility and success columns");
        ListView actors=find(decor,ListView.class);require(actors.getAdapter().getItemId(0)==1,"best actual compatible executor appears first");shot("05-adviser");
        ui(()->dialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        SaveCodec.validate(w);
    }
    private void clickId(ListView list,long id){for(int i=0;i<list.getAdapter().getCount();i++)if(list.getAdapter().getItemId(i)==id){list.performItemClick(list.getAdapter().getView(i,null,list),i,id);return;}throw new AssertionError("row not found: "+id);}
    private static boolean contains(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().contains(text))return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(contains(((ViewGroup)view).getChildAt(i),text))return true;return false;}
    private static <T extends View>T find(View view,Class<T> type){if(type.isInstance(view))return type.cast(view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){T result=find(((ViewGroup)view).getChildAt(i),type);if(result!=null)return result;}return null;}
    interface Action{void run()throws Exception;}
    private void ui(Action action){Throwable[] failure={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable e){failure[0]=e;}});if(failure[0]!=null)throw new AssertionError("UI check failed",failure[0]);}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(450);test.waitForIdleSync();}
    private void require(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;report.append("PASS ").append(message).append('\n');}
    private void shot(String name)throws Exception{settle();Bitmap b=test.getUiAutomation().takeScreenshot();require(b!=null,"screenshot "+name);File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,"v049-"+name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
}

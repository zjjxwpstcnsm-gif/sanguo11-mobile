package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Installed production-APK checks for the v66 public controls and rendered header. */
final class Governance66Probe {
    private final Instrumentation test;private MainActivity activity;private int checks;
    private final StringBuilder report=new StringBuilder();
    Governance66Probe(Instrumentation test){this.test=test;}
    void run()throws Exception{
        World w=ScenarioCatalog.load("heroes-250",0,12345L);
        try(OutputStream out=test.getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(w));}
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        header("01-portrait-header");
        ui(()->activity.openRealmPage("factions",-1));settle();assertColumns(table(),"君主","军师","兵源","兵源上限","季度增长","爵位");shot("02-faction-personnel-manpower");
        ui(()->activity.openRealmPage("officers",-1));settle();assertColumns(table(),"功绩","指挥兵数","官职","身份","宝物数");shot("03-officer-dossier");
        byte[] before=SaveCodec.encode(world());ScenarioFactionPicker[] picker={null};
        ui(()->{picker[0]=new ScenarioFactionPicker(activity,world(),side->{});picker[0].show();});settle();
        MapView preview=(MapView)field(picker[0],"map");
        for(World.City c:world().cities)if(c.owner>=0)require(preview.siteLabel(c).equals(world().governance.label(c.owner)),"opening label is faction ruler/nation for "+c.kind+" "+c.id);
        require(((TextView)field(picker[0],"summary")).getText().toString().contains("军师"),"opening dossier shows advisor");
        shot("04-opening-ruler-labels");ui(()->((Dialog)field(picker[0],"dialog")).dismiss());
        require(Arrays.equals(before,SaveCodec.encode(world())),"opening labels and dossier do not mutate campaign");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();activity=(MainActivity)field(test,"current");header("05-landscape-header");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));settle();activity=(MainActivity)field(test,"current");
        rewards();nation();arrival();
        try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(directory(),"governance66-checks.txt")),"UTF-8")){out.write("GOVERNANCE66 ANDROID PASS "+checks+" checks\n"+report);}
    }
    private void header(String image)throws Exception{
        TextView date=(TextView)field(activity,"dateBanner"),ap=(TextView)field(activity,"actionPointsBadge");
        ui(()->{Rect visible=new Rect();require(date.getVisibility()==View.VISIBLE&&date.getGlobalVisibleRect(visible),"date visible in real window");
            require(date.getText().toString().equals(world().date().replace(" ","")),"date contains complete year month and ten-day period");
            require(date.getLayout()!=null&&date.getLayout().getEllipsisCount(0)==0&&date.getPaint().measureText(date.getText().toString())<=date.getWidth()-date.getCompoundPaddingLeft()-date.getCompoundPaddingRight()+1,"complete date fits without clipping or ellipsis");
            require(visible.height()>=date.getHeight()-1,"date not vertically obscured");require(ap.getGlobalVisibleRect(visible)&&ap.getText().toString().contains("60"),"AP remains visible beside date");});shot(image);
    }
    private void rewards()throws Exception{
        World w=Ux64Fixture.create();World.Officer extra=new World.Officer(30,"待奖将",0,10,70,70,70,70,70);extra.loyalty=70;w.officers.add(extra);w.officer(5).loyalty=70;
        install(w);ui(()->new StrategyUi(activity,w,activity::applyResult).command(w.city(10),3));settle();shot("06-batch-reward-selection");
        click("全选",true);click("确认选择",true);click("甲将0",false);shot("07-batch-cost-confirmation");click("执行",true);settle();
        require(w.city(10).gold==29600&&w.actionPoints[0]==50,"two rewards deduct 400 gold and one 10 AP command");
        require(w.officer(5).loyalty>70&&w.officer(30).loyalty>70&&w.officer(5).lastRewardTurn==w.turn&&w.officer(30).lastRewardTurn==w.turn,"both selected officers actually rewarded once");
    }
    private void nation()throws Exception{
        World w=new World(70,35,"甲势力","乙势力");for(int n=0;n<25;n++)w.cities.add(new World.City(n,"城"+n,new Hex(3+(n%10)*6,3+(n/10)*10),n<24?0:1));
        w.officers.add(new World.Officer(0,"开国君主",0,0,90,90,90,90,90));w.officers.add(new World.Officer(1,"敌君主",1,24,80,80,80,80,80));w.strategy.initializeOffices();install(w);
        AlertDialog[] dossier={null};ui(()->dossier[0]=new RealmUi(activity,w,(ClientState)field(activity,"ui")).factionDetail(0,null));settle();click("设定国号",true);settle();
        AccessibilityNodeInfo input=find(test.getUiAutomation().getRootInActiveWindow(),"皇帝国号");require(input!=null,"emperor has an actual editable nation field");
        Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"大汉");require(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"nation field accepts input");
        shot("08-emperor-nation-name");click("确定",true);settle();require(w.governance.nation(0).equals("大汉")&&w.government.commandLimit(0)==15000,"nation UI changes actual campaign and emperor command limit");
        ui(()->dossier[0].dismiss());require(SaveCodec.decode(SaveCodec.encode(w)).faction(0).equals("大汉"),"national name survives saved campaign");
    }
    private void arrival()throws Exception{
        World w=Ux64Fixture.create();World.Unit u=new World.Unit(1,0,5,World.Weapon.SPEAR,new Hex(5,3),3000,6000);u.wounded=200;w.nextUnitId=2;w.units.add(u);w.officer(5).unitId=1;w.officer(5).cityId=-1;
        install(w);ui(()->activity.selectUnitAndFocus(1));settle();shot("09-selected-unit-wounded");
        require(!visibleText(activity.getWindow().getDecorView(),"进驻"),"selected unit has no separate garrison button");int soldiers=w.city(10).troops;
        ui(()->{World.Result result=w.move(1,new Hex(4,3));require(result.ok,"installed core accepts legal footprint move");activity.applyResult(result);});settle();
        require(w.unit(1)==null&&w.city(10).troops==soldiers+3200&&w.officer(5).cityId==10,"installed app movement auto-enters and heals exactly once");shot("10-auto-arrival-healed");
    }
    private void install(World w)throws Exception{ui(()->{call(activity,"activateWorld",new Class<?>[]{World.class},w);activity.selectAndFocus(w.home().hex);activity.refresh();});settle();}
    private void assertColumns(DataTable<?> table,String... expected)throws Exception{
        require(table!=null,"actual recycled information table exists");List<?> columns=(List<?>)field(table,"columns");int[] indices=(int[])field(table,"visible");Set<String> visible=new HashSet<>();for(int i:indices)visible.add((String)field(columns.get(i),"name"));
        for(String name:expected)require(visible.contains(name),"default table includes "+name);require(table.list.getAdapter().getCount()>0,"table contains actual campaign data");
    }
    private DataTable<?> table()throws Exception{return tableIn((View)field(activity,"panelHost"));}
    private DataTable<?> tableIn(View root){if(root instanceof DataTable)return (DataTable<?>)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){DataTable<?> t=tableIn(((ViewGroup)root).getChildAt(i));if(t!=null)return t;}return null;}
    private boolean visibleText(View root,String s){if(root.getVisibility()!=View.VISIBLE)return false;if(root instanceof TextView&&s.contentEquals(((TextView)root).getText()))return true;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)if(visibleText(((ViewGroup)root).getChildAt(i),s))return true;return false;}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo root,String description){if(root==null)return null;if(description.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;for(int i=0;i<root.getChildCount();i++){AccessibilityNodeInfo n=find(root.getChild(i),description);if(n!=null)return n;}return null;}
    private World world()throws Exception{return (World)field(activity,"world");}
    private void click(String text,boolean exact)throws Exception{call(test,"click",new Class<?>[]{String.class,boolean.class},text,exact);settle();}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(500);test.waitForIdleSync();}
    private File directory(){File dir=new File(test.getTargetContext().getExternalFilesDir(null),"governance66");dir.mkdirs();return dir;}
    private void shot(String name)throws Exception{Bitmap image=test.getUiAutomation().takeScreenshot();require(image!=null,"real screenshot "+name);try(OutputStream out=new FileOutputStream(new File(directory(),name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    private void require(boolean condition,String message){checks++;report.append(condition?"PASS ":"FAIL ").append(message).append('\n');if(!condition)throw new AssertionError(message);}
    interface Action{void run()throws Exception;}
    private void ui(Action action){Throwable[] errors={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable e){errors[0]=e;}});if(errors[0]!=null)throw new AssertionError(errors[0]);}
    private static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private static Object call(Object target,String name,Class<?>[] types,Object... args)throws Exception{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(target,args);}
}

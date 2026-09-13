package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;
import java.util.Arrays;
import game.sanguo.core.*;

/** Runs against an installed APK using platform UI automation, without a test framework dependency. */
public final class GameSmokeRunner extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity=startActivitySync(launch);waitText("选择剧本",false);
            screenshot("01-scenarios");
            click("区域争雄 ·",false);click("孙权军",true);click("执行",true);
            waitText("区域争雄  ·  孙权军",false);assertWorld(2,0,"regional-sandbox");
            click("菜单",true);click("城池一览 / 定位",true);click("建业 · 孙权军",false);
            waitText("建业",true);screenshot("02-city");
            click("出征",true);click("甘宁",true);click("弩兵",true);click("3000人",true);
            waitText("携粮 6000",false);waitForIdleSync();
            World w=saved();require(w.units.size()==1&&w.unit(1).owner==2&&w.unit(1).officerId==3003,"selected faction deployment");
            screenshot("03-deployment");
            click("下一旬  →",true);waitText("中旬",false);waitForIdleSync();assertWorld(2,1,"regional-sandbox");
            w=saved();require(w.units.stream().anyMatch(u->u.owner==0)&&w.units.stream().anyMatch(u->u.owner==1),"both opponents acted");
            click("菜单",true);click("保存局面（3个槽位）",true);click("槽位 1 ·",false);click("执行",true);waitForIdleSync();
            click("菜单",true);click("新游戏 / 选择势力",true);click("基础演练 ·",false);click("曹操军",true);click("执行",true);
            waitText("基础演练  ·  曹操军",false);waitForIdleSync();assertWorld(1,0,"m0-skirmish");
            click("菜单",true);click("保存局面（3个槽位）",true);click("槽位 2 ·",false);click("执行",true);waitForIdleSync();
            click("菜单",true);click("读取存档",true);screenshot("04-save-slots");click("槽位 1 · 区域争雄",false);click("执行",true);
            waitText("区域争雄  ·  孙权军",false);waitForIdleSync();assertWorld(2,1,"regional-sandbox");
            byte[] before=SaveCodec.encode(saved());
            try(FileOutputStream out=getTargetContext().openFileOutput("manual3.sg11",0)){out.write(new byte[]{1,2,3});}
            click("菜单",true);click("读取存档",true);click("槽位 3 · 文件损坏",false);click("执行",true);
            waitText("读取失败",true);click("返回",true);waitForIdleSync();
            require(Arrays.equals(before,SaveCodec.encode(saved())),"corrupt load leaves autosave unchanged");
            runOnMainSync(activity::recreate);waitText("区域争雄  ·  孙权军",false);waitForIdleSync();
            assertWorld(2,1,"regional-sandbox");screenshot("05-restored");
            strategicFlow();
            result.putString("stream","SMOKE PASS: installed APK launches; scenario/faction selection, city navigation, deployment, AI turns, three save slots, corrupt-load recovery, Activity recreation, construction, officer travel, editable cargo transport, task persistence and arrival verified.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){
            try{screenshot("failure");}catch(Exception ignored){}
            StringWriter trace=new StringWriter();error.printStackTrace(new PrintWriter(trace));
            result.putString("stream","SMOKE FAIL: "+trace+"\n");finish(Activity.RESULT_CANCELED,result);
        }
    }
    private void strategicFlow()throws Exception {
        click("菜单",true);click("新游戏 / 选择势力",true);click("区域争雄 ·",false);click("孙权军",true);click("执行",true);waitForIdleSync();
        assertWorld(2,0,"regional-sandbox");locateCity("柴桑");
        click("设施开发",true);click("市场 ·",false);click("周瑜 ·",false);click("地块 ",false);click("开工",true);waitForIdleSync();
        World w=saved();require(w.domestic.facilities.size()==1&&w.domestic.facilities.get(0).remaining==2&&w.domestic.busy(3001),"construction persisted and occupies builder");
        screenshot("06-construction");
        locateCity("柴桑");click("人员调动",true);click("建业 ·",false);click("孙权 ·",false);click("出发",true);waitForIdleSync();
        require(saved().domestic.missions.size()==1&&saved().officer(3000).cityId==-1,"officer travel starts through UI");
        locateCity("柴桑");click("资源运输",true);click("建业 ·",false);click("鲁肃 ·",false);waitText("运输数量",true);screenshot("07-cargo-form");click("发送",true);waitForIdleSync();
        w=saved();require(w.domestic.missions.size()==2&&w.city(300).food==35000&&w.city(300).troops==11000&&w.city(300).equipment[0]==11000,"cargo deducted once through UI");
        click("菜单",true);click("政务与在途",true);waitText("运输 · 鲁肃",false);screenshot("08-tasks");click("返回",true);
        click("菜单",true);click("保存局面（3个槽位）",true);click("槽位 3 ·",false);click("执行",true);waitForIdleSync();
        byte[] pending=SaveCodec.encode(saved());
        startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        waitText("区域争雄  ·  孙权军",false);waitForIdleSync();require(Arrays.equals(pending,SaveCodec.encode(saved())),"pending tasks survive Activity restart");
        click("下一旬  →",true);waitForTurn(1);require(saved().domestic.facilities.get(0).remaining==1,"construction advances one turn");
        click("菜单",true);click("读取存档",true);click("槽位 3 · 区域争雄",false);click("执行",true);waitForIdleSync();
        require(Arrays.equals(pending,SaveCodec.encode(saved())),"manual slot restores pending tasks exactly");
        for(int i=1;i<=8;i++){click("下一旬  →",true);waitForTurn(i);w=saved();if(w.officer(3000).cityId==310&&w.officer(3002).cityId==310)break;}
        w=saved();require(w.officer(3000).cityId==310&&w.officer(3002).cityId==310&&w.domestic.missions.stream().noneMatch(m->m.owner==2),"personnel and cargo arrive");
        require(w.city(310).troops==13000&&w.city(310).equipment[0]==13000,"arrival credits cargo exactly once");
        require(w.domestic.facilities.stream().anyMatch(f->f.cityId==300&&f.kind==Domestic.Kind.MARKET&&f.remaining==0),"market completes");
        locateCity("建业");screenshot("09-arrival");locateCity("柴桑");click("政务与在途",true);waitText("市场 · 已建成",false);screenshot("10-completed");click("返回",true);
    }
    private void locateCity(String city){click("菜单",true);click("城池一览 / 定位",true);click(city+" · 孙权军",false);waitText(city,true);}
    private void waitForTurn(int turn)throws Exception {
        long until=SystemClock.uptimeMillis()+15000;
        while(SystemClock.uptimeMillis()<until){waitForIdleSync();if(saved().turn==turn)return;SystemClock.sleep(100);}
        throw new AssertionError("persisted turn not reached: "+turn);
    }
    private void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void assertWorld(int player,int turn,String scenario)throws IOException {
        World w=saved();require(w.player==player&&w.turn==turn&&w.scenarioId.equals(scenario),"unexpected persisted opening/turn");
    }
    private World saved()throws IOException {
        try(InputStream in=getTargetContext().openFileInput("auto.sg11");ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)bytes.write(buffer,0,count);return SaveCodec.decode(bytes.toByteArray());
        }
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node,String text,boolean exact) {
        if(node==null)return null;
        CharSequence value=node.getText();
        if(value!=null&&(exact?value.toString().equals(text):value.toString().contains(text))&&node.isVisibleToUser())return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=find(node.getChild(i),text,exact);if(found!=null)return found;}return null;
    }
    private AccessibilityNodeInfo waitText(String text,boolean exact) {
        long until=SystemClock.uptimeMillis()+12000;
        while(SystemClock.uptimeMillis()<until) {
            waitForIdleSync();AccessibilityNodeInfo node=find(getUiAutomation().getRootInActiveWindow(),text,exact);
            if(node!=null)return node;SystemClock.sleep(100);
        }
        throw new AssertionError("UI text not found: "+text);
    }
    private void click(String text,boolean exact) {
        AccessibilityNodeInfo node=null;long until=SystemClock.uptimeMillis()+12000;
        while(node==null&&SystemClock.uptimeMillis()<until) {
            waitForIdleSync();AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();node=find(root,text,exact);
            if(node==null){if(!scroll(root))scrollBack(root);SystemClock.sleep(250);}
        }
        if(node==null)throw new AssertionError("UI action not found: "+text);
        Rect bounds=new Rect();node.getBoundsInScreen(bounds);
        long time=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0);
        MotionEvent up=MotionEvent.obtain(time,time+40,MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);
        sendPointerSync(down);sendPointerSync(up);down.recycle();up.recycle();waitForIdleSync();SystemClock.sleep(180);
    }
    private boolean scroll(AccessibilityNodeInfo node) {
        if(node==null)return false;
        if(node.isScrollable()&&node.isVisibleToUser()&&node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return true;
        for(int i=0;i<node.getChildCount();i++)if(scroll(node.getChild(i)))return true;return false;
    }
    private boolean scrollBack(AccessibilityNodeInfo node) {
        if(node==null)return false;
        if(node.isScrollable()&&node.isVisibleToUser()&&node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD))return true;
        for(int i=0;i<node.getChildCount();i++)if(scrollBack(node.getChild(i)))return true;return false;
    }
    private void screenshot(String name)throws IOException {
        waitForIdleSync();Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap==null)throw new IOException("Screenshot unavailable");
        File directory=getTargetContext().getExternalFilesDir("smoke");if(directory==null)throw new IOException("Screenshot directory unavailable");
        try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
    }
}

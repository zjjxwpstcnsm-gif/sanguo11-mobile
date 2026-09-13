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
    private Activity current;
    @Override public void callActivityOnResume(Activity a){super.callActivityOnResume(a);current=a;}
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity=startActivitySync(launch);waitText("选择剧本",false);
            screenshot("01-scenarios");
            click("区域争雄 ·",false);click("孙权军",true);click("执行",true);
            waitText("区域争雄  ·  孙权军",false);assertWorld(2,0,"regional-sandbox");
            clickNav("城市");click("建业 · 孙权军",false);
            waitText("建业",true);screenshot("02-city");
            click("军事",true);click("出征",true);click("甘宁",true);click("弩兵",true);click("3000人",true);
            waitText("携粮 6000",false);waitForIdleSync();
            World w=saved();require(w.units.size()==1&&w.unit(1).owner==2&&w.unit(1).officerId==3003,"selected faction deployment");
            screenshot("03-deployment");
            endTurn();waitText("中旬",false);waitForIdleSync();assertWorld(2,1,"regional-sandbox");
            w=saved();require(w.units.stream().anyMatch(u->u.owner==0)&&w.units.stream().anyMatch(u->u.owner==1),"both opponents acted");
            click("菜单",true);click("保存局面（3个槽位）",true);click("槽位 1 ·",false);waitForIdleSync();
            click("菜单",true);click("新游戏 / 选择势力",true);click("基础演练 ·",false);click("曹操军",true);click("执行",true);
            waitText("基础演练  ·  曹操军",false);waitForIdleSync();assertWorld(1,0,"m0-skirmish");
            click("菜单",true);click("保存局面（3个槽位）",true);click("槽位 2 ·",false);waitForIdleSync();
            click("菜单",true);click("读取存档",true);screenshot("04-save-slots");click("槽位 1 · 区域争雄",false);click("执行",true);
            waitText("区域争雄  ·  孙权军",false);waitForIdleSync();assertWorld(2,1,"regional-sandbox");
            byte[] before=SaveCodec.encode(saved());
            try(FileOutputStream out=getTargetContext().openFileOutput("manual3.sg11",0)){out.write(new byte[]{1,2,3});}
            click("菜单",true);click("读取存档",true);click("槽位 3 · 文件损坏",false);click("执行",true);
            waitText("读取失败",true);click("返回",true);waitForIdleSync();
            require(Arrays.equals(before,SaveCodec.encode(saved())),"corrupt load leaves autosave unchanged");
            runOnMainSync(current::recreate);waitText("区域争雄  ·  孙权军",false);waitForIdleSync();
            assertWorld(2,1,"regional-sandbox");screenshot("05-restored");
            strategicFlow();
            mobileFlow();
            result.putString("stream","SMOKE PASS: installed APK launches; scenario/faction selection, city navigation, deployment, AI turns, three save slots, corrupt-load recovery, Activity recreation, construction, officer travel, editable cargo transport, task persistence and arrival, map tap/pan/pinch/bounds, filters, empty states, cancel/overwrite confirmation and navigation recreation verified.\n");
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
        locateCity("柴桑");click("调动",true);click("人员调动",true);click("建业 ·",false);click("孙权 ·",false);click("出发",true);waitForIdleSync();
        require(saved().domestic.missions.size()==1&&saved().officer(3000).cityId==-1,"officer travel starts through UI");
        locateCity("柴桑");click("调动",true);click("资源运输",true);click("建业 ·",false);click("鲁肃 ·",false);waitText("运输数量",true);screenshot("07-cargo-form");click("发送",true);waitForIdleSync();
        w=saved();require(w.domestic.missions.size()==2&&w.city(300).food==35000&&w.city(300).troops==11000&&w.city(300).equipment[0]==11000,"cargo deducted once through UI");
        clickNav("任务");click("筛选 · 全部任务",true);click("运输",true);waitText("运输 · 鲁肃",false);screenshot("08-tasks");
        click("菜单",true);click("保存局面（3个槽位）",true);click("槽位 3 ·",false);click("执行",true);waitForIdleSync();
        byte[] pending=SaveCodec.encode(saved());
        startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        waitText("区域争雄  ·  孙权军",false);waitForIdleSync();require(Arrays.equals(pending,SaveCodec.encode(saved())),"pending tasks survive Activity restart");
        endTurn();waitForTurn(1);require(saved().domestic.facilities.get(0).remaining==1,"construction advances one turn");
        click("菜单",true);click("读取存档",true);click("槽位 3 · 区域争雄",false);click("执行",true);waitForIdleSync();
        require(Arrays.equals(pending,SaveCodec.encode(saved())),"manual slot restores pending tasks exactly");
        for(int i=1;i<=8;i++){endTurn();waitForTurn(i);w=saved();if(w.officer(3000).cityId==310&&w.officer(3002).cityId==310)break;}
        w=saved();require(w.officer(3000).cityId==310&&w.officer(3002).cityId==310&&w.domestic.missions.stream().noneMatch(m->m.owner==2),"personnel and cargo arrive");
        require(w.city(310).troops==13000&&w.city(310).equipment[0]==13000,"arrival credits cargo exactly once");
        require(w.domestic.facilities.stream().anyMatch(f->f.cityId==300&&f.kind==Domestic.Kind.MARKET&&f.remaining==0),"market completes");
        locateCity("建业");screenshot("09-arrival");locateCity("柴桑");click("内政",true);click("市场 · 已建成",false);screenshot("10-completed");click("返回",true);
    }
    private void clickNav(String name){click("导航 · "+name,true);}
    private void endTurn(){click("下一旬  →",true);click("执行",true);waitText("旬结算摘要",true);click("返回",true);}
    private void mobileFlow() throws Exception {
        // Test actual hit targets on the canvas, including the enlarged city target at full-map zoom.
        click("菜单",true);click("新游戏 / 选择势力",true);click("区域争雄 ·",false);click("孙权军",true);click("执行",true);
        click("全图",true);tapCity(310,20);waitText("建业",true);waitText("核心驻将",false);screenshot("11-city-hit-target");
        MapView map=mapView();MapCamera camera=camera(map);float[] scale={0};runOnMainSync(()->scale[0]=camera.scale);
        pinch(map,1.7f);float[] afterScale={0};runOnMainSync(()->afterScale[0]=camera.scale);require(afterScale[0]>scale[0]&&afterScale[0]<=camera.maxScale,"pinch changes camera scale: before="+scale[0]+", after="+afterScale[0]);
        World unchanged=saved();byte[] snapshot=SaveCodec.encode(unchanged);
        dragMap(map,150,80);require(Arrays.equals(snapshot,SaveCodec.encode(saved())),"map gestures never mutate world");
        screenshot("12-map-zoom");
        clickNav("武将");waitText("武将一览",true);setSearch("周瑜");waitText("周瑜 · 孙权军",true);
        click("周瑜 · 孙权军",true);waitText("统率 94",false);click("返回",true);
        setSearch("不存在");waitText("没有符合筛选条件的武将",false);setSearch("周瑜");
        runOnMainSync(current::recreate);waitText("武将一览",true);waitText("周瑜 · 孙权军",true);screenshot("13-officer-filter-restored");
        clickNav("任务");waitText("当前没有在途任务",false);click("筛选 · 全部任务",true);click("建设",true);waitText("当前没有建设中的设施",false);
        locateCity("柴桑");click("内政",true);waitText("当前没有建设中的设施",false);
        click("下一旬  →",true);click("取消",true);require(saved().turn==0,"cancel turn preserves world");
        // Filled slot replacement is explicit and cancellable; metadata is visible.
        click("菜单",true);click("保存局面（3个槽位）",true);waitText("最后保存",false);click("槽位 1 ·",false);waitText("覆盖以下存档",false);click("取消",true);
        // Large cargo review remains editable after cancellation, and never dispatches twice.
        locateCity("柴桑");click("调动",true);click("资源运输",true);click("建业 ·",false);click("孙权 ·",false);
        setInput("金（上限100000）","1000");click("发送",true);waitText("确认大额运输",true);click("取消",true);require(saved().domestic.missions.isEmpty(),"cancel large cargo does not dispatch");
        click("发送",true);click("确认发送",true);require(saved().domestic.missions.size()==1,"large cargo dispatches once");
        screenshot("14-confirmed-cargo");
    }
    private MapView mapView(){final MapView[] result={null};runOnMainSync(()->result[0]=findMap(current.getWindow().getDecorView()));require(result[0]!=null,"map exists");return result[0];}
    private MapView findMap(android.view.View v){if(v instanceof MapView)return (MapView)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){MapView m=findMap(g.getChildAt(i));if(m!=null)return m;}}return null;}
    private MapCamera camera(MapView map)throws Exception {java.lang.reflect.Field f=MapView.class.getDeclaredField("camera");f.setAccessible(true);return (MapCamera)f.get(map);}
    private void tapCity(int id,int dxDp)throws Exception {
        World w=saved();MapView map=mapView();MapCamera c=camera(map);int[] pos=new int[2];float[] xy=new float[2];
        runOnMainSync(()->{map.getLocationOnScreen(pos);Hex h=w.city(id).hex;xy[0]=pos[0]+25*1.7320508f*(h.q+h.r*.5f)*c.scale+c.x+dxDp*map.getResources().getDisplayMetrics().density;xy[1]=pos[1]+25*1.5f*h.r*c.scale+c.y;});
        long t=SystemClock.uptimeMillis();send(t,t,MotionEvent.ACTION_DOWN,xy[0],xy[1]);send(t,t+40,MotionEvent.ACTION_UP,xy[0],xy[1]);SystemClock.sleep(500);waitForIdleSync();
    }
    private void send(long down,long time,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,time,action,x,y,0);sendPointerSync(e);e.recycle();}
    private void dragMap(MapView map,int dx,int dy){int[] pos=new int[2];runOnMainSync(()->map.getLocationOnScreen(pos));float x=pos[0]+map.getWidth()/2f,y=pos[1]+map.getHeight()/2f;long t=SystemClock.uptimeMillis();send(t,t,MotionEvent.ACTION_DOWN,x,y);for(int i=1;i<=8;i++)send(t,t+i*20,MotionEvent.ACTION_MOVE,x+dx*i/8f,y+dy*i/8f);send(t,t+180,MotionEvent.ACTION_UP,x+dx,y+dy);waitForIdleSync();}
    private void pinch(MapView map,float factor){
        int[] pos=new int[2];runOnMainSync(()->map.getLocationOnScreen(pos));float cx=pos[0]+map.getWidth()/2f,cy=pos[1]+map.getHeight()/2f;long down=SystemClock.uptimeMillis();
        float density=map.getResources().getDisplayMetrics().density;
        int minimum=android.os.Build.VERSION.SDK_INT>=29?android.view.ViewConfiguration.get(map.getContext()).getScaledMinimumScalingSpan():Math.round(180*density);
        float startSpan=Math.min(map.getWidth()/(2*factor)-24*density,Math.max(100*density,minimum*.65f));
        require(startSpan*2>minimum,"test viewport must fit Android minimum pinch span");
        MotionEvent.PointerProperties[] properties={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};MotionEvent.PointerCoords[] coords={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};
        for(int i=0;i<2;i++){properties[i].id=i;properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i].pressure=1;coords[i].size=1;coords[i].y=cy;}
        for(int step=0;step<=14;step++){float span=startSpan*(1+(factor-1)*Math.min(step,12)/12f);coords[0].x=cx-span;coords[1].x=cx+span;int action=step==0?MotionEvent.ACTION_DOWN:step==1?MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT):step==13?MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT):step==14?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;
            MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,step==0||step==14?1:2,properties,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);sendPointerSync(e);e.recycle();SystemClock.sleep(30);}
        waitForIdleSync();SystemClock.sleep(500);
    }
    private void setSearch(String value){setInput("搜索武将姓名",value);}
    private void setInput(String description,String value){
        AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();AccessibilityNodeInfo input=findInput(root,description);require(input!=null,"input available: "+description);Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);require(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"set editable input");waitForIdleSync();SystemClock.sleep(200);
    }
    private AccessibilityNodeInfo findInput(AccessibilityNodeInfo n,String description){if(n==null)return null;if(description.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=findInput(n.getChild(i),description);if(found!=null)return found;}return null;}
    private void locateCity(String city){clickNav("城市");click(city+" · 孙权军",false);waitText(city,true);}
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
        if(text.startsWith("导航 · "))value=node.getContentDescription();
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
        sendPointerSync(down);sendPointerSync(up);down.recycle();up.recycle();waitForIdleSync();SystemClock.sleep(350);
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

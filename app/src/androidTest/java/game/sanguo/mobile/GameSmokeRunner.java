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
    private boolean upgradeOnly;
    @Override public void callActivityOnResume(Activity a){super.callActivityOnResume(a);current=a;}
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);upgradeOnly=arguments!=null&&"true".equals(arguments.getString("upgrade"));start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            if(upgradeOnly){upgradeFlow();result.putString("stream","UPGRADE PASS: v0.9 APK replaced in place, v8 save retained, loaded, written as v15 and restored identically.\n");finish(Activity.RESULT_OK,result);return;}
            Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity=startActivitySync(launch);waitText("选择剧本",false);
            screenshot("01-scenarios");
            click("区域争雄 ·",false);click("孙权军",true);click("执行",true);
            waitText("区域争雄  ·  孙权军",false);assertWorld(2,0,"regional-sandbox");
            adaptiveMapFlow();
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
            marchFlow();
            worldFlow();
            campaignAiFlow();
            lifecycleFlow();
            estatesFlow();
            fieldworkFlow();
            abilityFlow();
            strategicFlow();
            mobileFlow();
            personnelFlow();
            campaignFlow();
            armyFlow();
            rulesFlow();
            contentFlow();
            sourceProfileFlow();
            governmentFlow();
            documentTransferFlow();
            contestFlow();
            result.putString("stream","SMOKE PASS: v19 portrait/landscape/collapsible panels/hidden navigation/route rotation; v18 sourced-profile preview/cancel/import/relations/recreation; v17 biography edit/cancel/death/succession/recreation/scenario import/200x200 offset viewport; v14 integrated march/ZOC/districts/events/raids/magic/diplomatic-debate/recreation; v12 mediation/treasure/PK-editor/templates/save; v11 fieldwork/36-tech/carry-gold/construction/repair/upgrade; v10 PK research/training/finite uses/skill overwrite/cancel/turn progression/task count/recreation; integrated original game/save/city/task/personnel/combat/army regressions; v9 duel/debate/start/cancel/round/save/settlement, v8 governance/capture/rank/summon, document export/import/cancel/corruption; legacy move preview/cancel/recreation and 神算百出连环; sourced opening, content/search/navigation/save restore and viewport stress verified.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){
            try{screenshot("failure");}catch(Exception ignored){}
            StringWriter trace=new StringWriter();error.printStackTrace(new PrintWriter(trace));
            result.putString("stream","SMOKE FAIL: "+trace+"\n");finish(Activity.RESULT_CANCELED,result);
        }
    }
    private void adaptiveMapFlow()throws Exception {
        byte[] before=SaveCodec.encode(saved());
        require(find(getUiAutomation().getRootInActiveWindow(),"导航 · 城市",true)==null,"navigation is hidden until requested");
        require(find(getUiAutomation().getRootInActiveWindow(),"全图",true)==null,"map tools are hidden until requested");
        chooseOrientation("竖屏");assertOrientation(true);assertMapLayout(true,false);screenshot("90-portrait-map");
        clickNav("城市");waitText("城池一览",true);assertMapLayout(true,true);screenshot("91-portrait-sheet");
        int halfHeight=mapView().getHeight();click("展开",true);require(mapView().getHeight()<halfHeight,"expanded sheet has more room");
        screenshot("92-portrait-expanded");click("缩小",true);click("收起",true);assertMapLayout(true,false);
        click("选中对象指令 ·",false);assertMapLayout(true,true);
        runOnMainSync(current::recreate);waitText("区域争雄",false);assertOrientation(true);assertMapLayout(true,true);
        runOnMainSync(current::onBackPressed);waitForIdleSync();assertMapLayout(true,false);
        chooseOrientation("横屏");assertOrientation(false);assertMapLayout(false,false);screenshot("93-landscape-map");
        clickNav("城市");waitText("城池一览",true);assertMapLayout(false,true);screenshot("94-landscape-sheet");
        click("收起",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"layout changes preserve resources, turn and RNG");
    }
    private void chooseOrientation(String label)throws Exception {click("视图",true);click("屏幕方向",true);click(label,true);getUiAutomation().waitForIdle(800,5000);waitForIdleSync();}
    private void assertOrientation(boolean portrait){
        long until=SystemClock.uptimeMillis()+5000;
        while(SystemClock.uptimeMillis()<until){waitForIdleSync();if((current.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT)==portrait)return;SystemClock.sleep(100);}
        throw new AssertionError("orientation did not change");
    }
    private void assertMapLayout(boolean portrait,boolean opened)throws Exception {
        waitForIdleSync();java.lang.reflect.Field shell=MainActivity.class.getDeclaredField("panelShell");shell.setAccessible(true);
        java.lang.reflect.Field bodyField=MainActivity.class.getDeclaredField("body");bodyField.setAccessible(true);
        runOnMainSync(()->{try{
            android.view.View panel=(android.view.View)shell.get(current);android.view.View body=(android.view.View)bodyField.get(current);MapView map=findMap(current.getWindow().getDecorView());
            require((panel.getVisibility()==android.view.View.VISIBLE)==opened,"sheet visibility");
            if(!opened){require(map.getWidth()==body.getWidth()&&map.getHeight()==body.getHeight(),"closed sheet leaves entire play area for map");}
            else if(portrait){require(panel.getWidth()==body.getWidth()&&panel.getTop()>=map.getBottom(),"portrait panel stacks below map");}
            else require(panel.getLeft()>=map.getRight()&&panel.getHeight()==body.getHeight(),"landscape panel stacks beside map");
            require(map.getWidth()>0&&map.getHeight()>0,"map remains visible");
        }catch(IllegalAccessException e){throw new RuntimeException(e);}});
    }
    private void marchFlow()throws Exception {
        World w=new World(24,12,"甲军","乙军");w.scenarioId="ui-march";w.scenarioName="行军验证";
        w.cities.add(new World.City(10,"起点城",new Hex(1,1),0));w.cities.add(new World.City(20,"目标城",new Hex(22,10),1));
        World.Officer o=new World.Officer(0,"行军将",0,-1,80,80,80,80,80);o.role=Strategy.Role.RULER;o.loyalty=100;o.unitId=1;w.officers.add(o);
        World.Unit unit=new World.Unit(1,0,0,World.Weapon.SPEAR,new Hex(3,6),5000,100000);w.units.add(unit);w.nextUnitId=2;
        installFixture(w,unit.hex);click("全图",true);byte[] initial=SaveCodec.encode(saved());
        tapHex(w.city(20).hex);waitText("路线预览 · 目标城",true);screenshot("67-march-preview");
        require(Arrays.equals(initial,SaveCodec.encode(saved())),"route preview does not mutate game");
        chooseOrientation("竖屏");assertOrientation(true);waitText("路线预览 · 目标城",true);waitText("开始行军",true);screenshot("95-portrait-route");
        runOnMainSync(current::recreate);waitText("路线预览 · 目标城",true);require(Arrays.equals(initial,SaveCodec.encode(saved())),"pending preview survives rotation without issuing order");
        chooseOrientation("横屏");assertOrientation(false);waitText("路线预览 · 目标城",true);
        click("取消",true);require(Arrays.equals(initial,SaveCodec.encode(saved())),"route cancel has zero cost");
        click("全图",true);tapHex(w.city(20).hex);click("开始行军",true);w=saved();require(w.unit(1).march!=null&&w.unit(1).movementSpent<=4&&!w.unit(1).acted,"map city tap starts budgeted persistent march");screenshot("68-march-active");
        byte[] issued=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("行军验证",false);require(Arrays.equals(issued,SaveCodec.encode(saved())),"restart does not duplicate march movement");
        Hex previous=saved().unit(1).hex;endTurn();require(!saved().unit(1).hex.equals(previous),"next turn advances selected marching unit");click("选中对象指令 ·",false);waitText("停止行军",true);screenshot("69-march-next-turn");
        clickNav("任务");click("行军 · 行军将",true);waitText("目标 目标城",false);screenshot("70-march-task");click("定位部队",true);
        click("全图",true);tapHex(new Hex(4,9));click("开始行军",true);require(saved().unit(1).march!=null&&saved().unit(1).march.tile.equals(new Hex(4,9)),"tap new ground retargets queued order");
        click("停止行军",true);require(saved().unit(1).march==null,"stop through fixed command dock");screenshot("71-march-stopped");
        click("全图",true);tapHex(saved().city(20).hex);click("开始行军",true);int count=0;
        while(saved().unit(1).march!=null&&count++<12)endTurn();
        w=saved();require(w.unit(1).march==null&&w.unit(1).hex.distance(w.city(20).hex)==1&&w.city(20).owner==1&&!w.unit(1).acted,"arrival does not auto attack and retains action");screenshot("72-march-arrived");
        click("全图",true);tapHex(w.city(20).hex);click("执行",true);require(saved().city(20).defense<3000&&saved().unit(1).acted,"arrival followed by real siege through target tap");
    }
    private void campaignAiFlow()throws Exception {
        World w=new World(32,18,"守营","攻营");w.scenarioId="ui-campaign-ai";w.scenarioName="军情验证";
        w.cities.add(new World.City(10,"守城",new Hex(5,5),0));w.cities.add(new World.City(20,"攻城营",new Hex(27,5),1));w.city(20).troops=0;
        w.officers.add(new World.Officer(0,"守城官",0,10,75,75,75,75,75));
        w.officers.add(new World.Officer(1,"残部",0,-1,60,60,60,60,60));w.officers.add(new World.Officer(2,"完整守军",0,-1,80,80,80,80,80));
        w.officers.add(new World.Officer(20,"军械官",1,-1,85,85,60,60,60));w.officers.add(new World.Officer(21,"突击官",1,-1,90,90,60,60,60));
        World.Unit ram=new World.Unit(1,1,20,World.Weapon.RAM,new Hex(6,5),6000,60000);w.officer(20).unitId=1;
        World.Unit spear=new World.Unit(2,1,21,World.Weapon.SPEAR,new Hex(20,10),6000,60000);w.officer(21).unitId=2;spear.energy=0;
        World.Unit weak=new World.Unit(3,0,1,World.Weapon.CROSSBOW,new Hex(21,10),100,10000);w.officer(1).unitId=3;
        World.Unit strong=new World.Unit(4,0,2,World.Weapon.SPEAR,new Hex(20,11),6000,60000);w.officer(2).unitId=4;
        w.units.add(ram);w.units.add(spear);w.units.add(strong);w.units.add(weak);w.nextUnitId=5;
        w.officer(0).role=Strategy.Role.RULER;w.officer(0).loyalty=100;w.officer(20).role=Strategy.Role.RULER;w.officer(20).loyalty=100;
        installFixture(w,w.city(10).hex);byte[] before=SaveCodec.encode(saved());
        clickNav("菜单");click("军团与天下",true);click("军情评估",true);waitText("建议留守",false);screenshot("81-ai-assessment");click("返回",true);
        require(Arrays.equals(before,SaveCodec.encode(saved())),"assessment leaves game and random state unchanged");
        endTurn();waitForTurn(1);w=saved();require(w.city(10).defense<3000&&w.unit(1).energy<80,"installed AI uses ram siege tactic");
        require(w.unit(3)==null&&w.unit(4)!=null&&w.unit(4).troops==6000,"installed AI chooses a kill rather than first enemy in list");screenshot("82-ai-turn-result");
        byte[] after=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("军情验证",false);require(Arrays.equals(after,SaveCodec.encode(saved())),"AI turn result survives activity recreation");
    }
    private void lifecycleFlow()throws Exception {
        World w=ScenarioCatalog.load("lifecycle-drill",0);w.turn=2;installFixture(w,w.city(10).hex);
        byte[] before=SaveCodec.encode(saved());
        openLifetime();setInput("出生年","174");click("预览",true);waitText("生卒编辑预览",true);screenshot("83-lifetime-preview");click("取消",true);
        require(Arrays.equals(before,SaveCodec.encode(saved())),"biography preview cancellation is atomic");
        openLifetime();setInput("出生年","174");click("预览",true);click("执行",true);require(saved().life.life(1).birth==174&&saved().editor.edited(),"biography draft applies to playable save");
        endTurn();waitText("君主继承 · 先主已故",true);w=saved();require(w.life.pending()&&w.life.state(0)==Lifecycle.State.DEAD&&w.life.present(30),"January death and arrival require heir");
        byte[] pending=SaveCodec.encode(w);screenshot("84-succession-pending");runOnMainSync(current::recreate);waitText("君主继承 · 先主已故",true);
        require(Arrays.equals(pending,SaveCodec.encode(saved())),"pending succession survives recreation exactly");
        click("继承 · 继业",true);click("取消",true);require(Arrays.equals(pending,SaveCodec.encode(saved())),"heir cancellation preserves unresolved event");
        click("继承 · 继业",true);click("执行",true);w=saved();require(!w.life.pending()&&w.officer(1).role==Strategy.Role.RULER&&w.officer(1).loyalty==100,"real heir confirmation resumes play");
        clickNav("菜单");click("生卒与继承",true);click("事件履历",true);waitText("继业继承传承营君主之位",false);screenshot("85-succession-history");click("返回",true);
        byte[] settled=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("世代传承",false);require(Arrays.equals(settled,SaveCodec.encode(saved())),"inheritance settles once across restart");
        File source=new File(getTargetContext().getFilesDir(),"scenario-import.properties");
        try(InputStream in=ScenarioCatalog.class.getResourceAsStream("/scenarios/lifecycle-drill.properties");OutputStream out=new FileOutputStream(source)){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
        Intent document=new Intent().setData(android.net.Uri.fromFile(source));
        runOnMainSync(()->((MainActivity)current).onActivityResult(915,Activity.RESULT_OK,document));waitText("导入剧本 · 选择势力",true);click("守望营",true);click("取消",true);require(Arrays.equals(settled,SaveCodec.encode(saved())),"scenario import cancel preserves current campaign");
        runOnMainSync(()->((MainActivity)current).onActivityResult(915,Activity.RESULT_OK,document));click("守望营",true);click("执行",true);require(saved().player==1&&saved().turn==0&&saved().life.state(30)==Lifecycle.State.UNAPPEARED,"scenario import starts selected faction and configured lifetimes");screenshot("86-imported-scenario");
        before=SaveCodec.encode(saved());try(OutputStream out=new FileOutputStream(source)){out.write(new byte[]{0,1,2});}
        runOnMainSync(()->((MainActivity)current).onActivityResult(915,Activity.RESULT_OK,document));waitText("导入失败",true);click("返回",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"invalid scenario import is atomic");
    }
    private void openLifetime(){clickNav("菜单");click("生卒与继承",true);click("武将生卒资料",true);setInput("生卒武将姓名","继业");click("查找",true);click("继业 · 已登场",true);click("编辑生卒",true);}
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
    private void personnelMenu(String city){
        locateCity(city);click("人事 / 城市治理",true);
    }
    private void personnelAction(String city,String action){locateCity(city);click("武将",true);click(action,true);}
    private void personnelFlow()throws Exception {
        click("菜单",true);click("新游戏 / 选择势力",true);click("区域争雄 ·",false);click("孙权军",true);click("执行",true);waitForIdleSync();
        personnelMenu("柴桑");click("城市与武将状态",true);waitText("兵源 20000",false);screenshot("15-officer-state");click("返回",true);
        byte[] cancelled=SaveCodec.encode(saved());
        personnelAction("柴桑","搜索人才");click("鲁肃 ·",false);click("取消",true);
        require(Arrays.equals(cancelled,SaveCodec.encode(saved())),"cancel search preserves resources and RNG");
        personnelAction("柴桑","搜索人才");click("鲁肃 ·",false);click("执行",true);waitForIdleSync();
        World w=saved();require(w.officer(3002).acted&&w.actionPoints[2]==50&&w.officer(910002)!=null&&w.officer(910002).owner==-1,"seeded UI search discovers unaffiliated talent");
        clickNav("武将");click("全部势力",true);click("在野武将",true);waitText("陆苓 · 在野",true);
        click("陆苓 · 在野",true);waitText("身份：在野 · 忠诚 0",false);click("返回",true);
        runOnMainSync(current::recreate);waitText("在野武将",true);waitText("陆苓 · 在野",true);
        personnelAction("柴桑","登用武将");click("孙权 ·",false);click("陆苓 ·",false);click("执行",true);waitForIdleSync();
        w=saved();require(w.officer(910002).owner==2&&w.officer(910002).acted&&w.city(300).gold==4900,"UI hire uses core probability and persists ownership");
        personnelAction("柴桑","任命太守");click("周瑜 ·",false);click("周瑜 ·",false);click("执行",true);waitForIdleSync();
        w=saved();require(w.city(300).governorId==3001&&w.domestic.monthlyGold(300)>800,"UI appointment has actual income benefit");
        screenshot("16-personnel-actions");endTurn();waitForTurn(1);
        int loyalty=saved().officer(3001).loyalty;
        personnelAction("柴桑","褒奖武将");click("孙权 ·",false);click("周瑜 ·",false);click("执行",true);waitForIdleSync();
        require(saved().officer(3001).loyalty>loyalty,"UI reward persists loyalty");
        locateCity("柴桑");click("内政",true);click("巡察 · 金100",true);click("鲁肃 ·",false);click("执行",true);waitForIdleSync();require(saved().city(300).order==100,"UI patrol caps order");
        int readiness=saved().city(300).morale;
        locateCity("柴桑");click("军事",true);click("训练 · 金100",true);click("周瑜 ·",false);click("执行",true);waitForIdleSync();require(saved().strategy.getArmyReadiness(300)>readiness,"UI training uses readiness interface");
        int troops=saved().city(310).troops;
        locateCity("建业");click("军事",true);click("征兵 ·",false);click("甘宁 ·",false);click("执行",true);waitForIdleSync();
        w=saved();require(w.city(310).troops>troops&&w.city(310).troops-troops==20000-w.city(310).recruitReserve,"UI recruitment conserves finite manpower");
        byte[] before=SaveCodec.encode(w);
        startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        waitText("区域争雄  ·  孙权军",false);waitForIdleSync();require(Arrays.equals(before,SaveCodec.encode(saved())),"personnel v4 state survives Activity restart");
        personnelMenu("柴桑");click("城市与武将状态",true);waitText("太守：周瑜",false);screenshot("17-v4-restored");click("返回",true);
    }
    private void clickNav(String name){click("导航 · "+name,true);}
    private void endTurn(){click("下一旬  →",true);click("执行",true);waitText("旬结算摘要",true);click("返回",true);}
    private void mobileFlow() throws Exception {
        // Test actual hit targets on the canvas, including the enlarged city target at full-map zoom.
        click("菜单",true);click("新游戏 / 选择势力",true);click("区域争雄 ·",false);click("孙权军",true);click("执行",true);
        click("全图",true);tapCity(310,20);waitText("建业",true);waitText("太守",false);screenshot("11-city-hit-target");
        MapView map=mapView();MapCamera camera=camera(map);float[] scale={0};runOnMainSync(()->scale[0]=camera.scale);
        click("收起",true);pinch(map,1.7f);float[] afterScale={0};runOnMainSync(()->afterScale[0]=camera.scale);require(afterScale[0]>scale[0]&&afterScale[0]<=camera.maxScale,"pinch changes camera scale: before="+scale[0]+", after="+afterScale[0]);
        World unchanged=saved();byte[] snapshot=SaveCodec.encode(unchanged);
        dragMap(map,150,80);require(Arrays.equals(snapshot,SaveCodec.encode(saved())),"map gestures never mutate world");
        screenshot("12-map-zoom");
        clickNav("武将");waitText("武将一览",true);setSearch("周瑜");waitText("周瑜 · 孙权军",true);
        click("周瑜 · 孙权军",true);waitText("统率 94",false);click("返回",true);
        setSearch("不存在");waitText("没有符合筛选条件的武将",false);setSearch("周瑜");
        runOnMainSync(current::recreate);waitText("武将一览",true);waitText("周瑜 · 孙权军",true);screenshot("13-officer-filter-restored");
        clickNav("任务");waitText("当前没有在途任务",false);click("筛选 · 全部任务",true);click("建设",true);waitText("当前没有建设中的设施",false);
        locateCity("柴桑");click("内政",true);scrollToText("当前没有建设中的设施",false);
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
    private void campaignFlow()throws Exception {
        click("菜单",true);click("新游戏 / 选择势力",true);click("区域争雄 ·",false);click("孙权军",true);click("执行",true);waitForIdleSync();
        World opening=saved();int quote=opening.campaign.foodPrice(300,true),openingGold=opening.city(300).gold,openingFood=opening.city(300).food;
        require(opening.startMonth==9&&quote==140,"September fixture has seasonal quote140, not January price100");
        locateCity("柴桑");click("内政",true);click("商人 / 粮食买卖",true);waitText("买粮 · 每1000粮 / 金"+quote,true);click("买粮 ·",false);click("1000粮",true);click("孙权 ·",false);
        byte[] before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"cancel trade preserves exact save");
        locateCity("柴桑");click("内政",true);click("商人 / 粮食买卖",true);click("买粮 ·",false);click("1000粮",true);click("孙权 ·",false);click("执行",true);
        World w=saved();require(w.city(300).food==openingFood+1000&&w.city(300).gold==openingGold-quote&&w.campaign.traded(300)==1000,"UI trade executes quoted amount once");screenshot("18-merchant");
        locateCity("柴桑");click("技巧 / 培养",true);click("能力 / 适性培养",true);click("基础能力",true);waitText("没有符合条件的选项。",true);click("返回",true);
        require(saved().abilities.training().isEmpty(),"training requires actual completed research");
        locateCity("柴桑");click("外交 / 协定",true);click("曹操军 ·",false);click("亲善 ·",false);click("周瑜 ·",false);click("执行",true);
        w=saved();require(w.strategy.factionRelation(2,1)>0,"UI envoy affects actual relation");screenshot("20-diplomacy");

        // Construct a test-only combat position, then issue every combat action via real Android clicks.
        World battle=new World(15,10,"孙权军","曹操军","刘备军");battle.scenarioId="ui-combat-fixture";battle.scenarioName="战法验证";
        battle.cities.add(new World.City(300,"柴桑",new Hex(2,2),0));battle.cities.add(new World.City(200,"许昌",new Hex(12,2),1));battle.cities.add(new World.City(100,"江陵",new Hex(10,8),2));
        battle.officers.add(new World.Officer(1,"甘宁",0,-1,90,95,90,80,80));battle.officers.add(new World.Officer(2,"张辽",1,-1,90,90,80,80,80));battle.officers.add(new World.Officer(3,"周瑜",0,300,95,80,98,90,90));
        World.Unit actor=new World.Unit(1,0,1,World.Weapon.SPEAR,new Hex(6,5),5000,10000);World.Unit enemy=new World.Unit(2,1,2,World.Weapon.SPEAR,new Hex(7,5),5000,10000);
        battle.units.add(actor);battle.units.add(enemy);battle.nextUnitId=3;battle.officer(1).unitId=1;battle.officer(2).unitId=2;battle.officer(1).role=Strategy.Role.RULER;battle.officer(1).loyalty=100;battle.officer(2).role=Strategy.Role.RULER;battle.officer(2).loyalty=100;
        battle.strategy.setSeed(0);installFixture(battle,actor.hex);
        click("战法",true);click("突刺 ·",false);click("张辽 ·",false);click("取消",true);require(saved().unit(1).energy==80&&saved().unit(2).troops==5000,"cancel tactic has no cost");
        click("战法",true);click("突刺 ·",false);click("张辽 ·",false);click("执行",true);w=saved();require(w.unit(1).energy==65&&w.unit(1).acted&&w.unit(2).troops<5000&&w.unit(2).hex.equals(new Hex(8,5)),"UI spear thrust persists damage, action and displacement");screenshot("21-tactical-thrust");
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("战法验证",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"combat outcome survives recreation without duplicate settlement");
        battle=SaveCodec.decode(before);battle.unit(1).acted=false;battle.unit(1).energy=80;battle.strategy.setSeed(0);installFixture(battle,battle.unit(1).hex);
        // Fire has adjacent range in the selected PC rules; move after the push before casting.
        tapHex(new Hex(7,5));click("开始行军",true);
        require(!saved().unit(1).acted,"moving into fire range retains the plot command");
        click("选中对象指令 ·",false);
        click("部队计略",true);click("火计 ·",false);click("张辽 ·",false);click("执行",true);w=saved();require(w.war.fireAt(w.unit(2).hex)!=null&&w.unit(1).energy==70,"UI fire plot persists burning hex");screenshot("22-fire-field");
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("战法验证",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"fire survives Activity recreation");
    }
    private void governmentFlow()throws Exception {
        World w=new World(16,12,"刘备军","曹操军");w.scenarioId="ui-governance-fixture";w.scenarioName="军政验证";
        w.cities.add(new World.City(0,"营城",new Hex(2,3),0));w.cities.add(new World.City(1,"后方",new Hex(1,9),0));w.cities.add(new World.City(2,"敌城",new Hex(14,1),1));
        String[] names={"刘备","潘璋","张辽","鲁肃","吕蒙","诸葛瑾","黄盖","周泰"};
        for(int i=0;i<names.length;i++)w.officers.add(new World.Officer(i,names[i],i==2?1:0,i==2?2:i==6?1:0,80,80,80,80,80));
        w.officer(0).role=Strategy.Role.RULER;w.officer(0).loyalty=100;w.officer(1).skillId=Skill.BOFU.id;
        World.Unit a=new World.Unit(1,0,1,World.Weapon.SPEAR,new Hex(4,4),5000,10000),b=new World.Unit(2,1,2,World.Weapon.SPEAR,new Hex(5,4),1,5000);
        w.units.add(a);w.units.add(b);w.nextUnitId=3;w.officer(1).unitId=1;w.officer(1).cityId=-1;w.officer(2).unitId=2;w.officer(2).cityId=-1;
        installFixture(w,a.hex);tapHex(b.hex);click("执行",true);waitForIdleSync();
        require(saved().government.captive(2)&&saved().unit(2)==null,"capture skill produces persisted prisoner from actual map attack");
        require(saved().government.prisoner(2).cityId==0,"fixture routes prisoner to the city exercised by the UI");
        locateCity("营城");click("军政 / 俘虏 / 官职",true);click("俘虏处置",true);click("张辽 ·",false);click("释放",true);click("刘备 ·",false);
        byte[] before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"release cancellation preserves exact save");
        locateCity("营城");click("军政 / 俘虏 / 官职",true);click("俘虏处置",true);click("张辽 ·",false);click("释放",true);click("刘备 ·",false);click("执行",true);
        require(!saved().government.captive(2)&&saved().officer(2).cityId==2,"release returns officer through real UI");screenshot("37-prisoner-release");
        locateCity("营城");click("军政 / 俘虏 / 官职",true);click("任命军师",true);click("鲁肃 ·",false);click("吕蒙 ·",false);click("执行",true);
        require(saved().government.advisor(0).id==4,"advisor appointment persists");
        locateCity("营城");click("军政 / 俘虏 / 官职",true);click("授予官职",true);click("诸葛瑾 ·",false);click("鲁肃 ·",false);click("奋威校尉 ·",false);click("执行",true);
        require(saved().government.commandLimit(3)==6000,"native rank command enforces its real troop cap");screenshot("38-officer-rank");
        locateCity("营城");click("军政 / 俘虏 / 官职",true);click("召唤武将",true);click("黄盖 ·",false);click("执行",true);
        require(saved().domestic.missions.stream().anyMatch(m->m.officerId==6&&m.targetCity==0),"summon creates actual mission");
        World raid=saved();raid.active=1;raid.officer(2).acted=false;
        raid.cities.add(new World.City(3,"敌后方",new Hex(14,9),1));
        require(raid.domestic.transport(2,3,2,0,5000,1,new int[4]).ok,"prepare real hostile cargo mission");raid.active=0;raid.unit(1).acted=false;
        Domestic.Mission convoy=raid.domestic.missions.stream().filter(m->m.officerId==2).findFirst().get();convoy.hex=new Hex(5,4);
        installFixture(raid,raid.unit(1).hex);tapHex(convoy.hex);click("执行",true);
        require(saved().domestic.missions.stream().noneMatch(m->m.officerId==2)&&saved().government.captive(2),"map tap intercepts visible enemy transport and captures its courier");screenshot("41-transport-interception");
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("军政验证",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"governance state survives activity recreation");screenshot("39-governance-restored");
    }
    private void documentTransferFlow()throws Exception {
        // Execute the document-result callback against an actual resolver file. Picker navigation is system-owned.
        File exported=new File(getTargetContext().getFilesDir(),"smoke-export.sg11");Intent result=new Intent().setData(android.net.Uri.fromFile(exported));
        byte[] before=SaveCodec.encode(saved());
        runOnMainSync(()->((MainActivity)current).onActivityResult(911,Activity.RESULT_OK,result));waitText("存档已导出",true);click("返回",true);
        byte[] data;try(FileInputStream in=new FileInputStream(exported);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);data=out.toByteArray();}require(Arrays.equals(before,data),"export writes byte-identical playable save");
        runOnMainSync(()->((MainActivity)current).onActivityResult(912,Activity.RESULT_OK,result));waitText("导入“军政验证”",false);click("取消",true);
        require(Arrays.equals(before,SaveCodec.encode(saved())),"document import cancel keeps autosave");
        runOnMainSync(()->((MainActivity)current).onActivityResult(912,Activity.RESULT_OK,result));click("执行",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"document import restores exact complete state");
        try(FileOutputStream out=new FileOutputStream(exported)){out.write(new byte[]{1,2,3});}
        runOnMainSync(()->((MainActivity)current).onActivityResult(912,Activity.RESULT_OK,result));waitText("导入失败",true);click("返回",true);
        require(Arrays.equals(before,SaveCodec.encode(saved())),"corrupt external document does not mutate game");screenshot("40-document-restore");
    }
    private void upgradeFlow()throws Exception {
        World legacy=saved();String scenarioName=legacy.scenarioName;byte[] before=SaveCodec.encode(legacy);
        Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivitySync(launch);waitText(scenarioName,false);waitForIdleSync();
        java.lang.reflect.Field field=MainActivity.class.getDeclaredField("world");field.setAccessible(true);World[] loaded=new World[1];
        runOnMainSync(()->{try{loaded[0]=(World)field.get(current);}catch(IllegalAccessException e){throw new RuntimeException(e);}});
        require(Arrays.equals(before,SaveCodec.encode(loaded[0])),"upgraded app actually loaded all old state");
        require(getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).getLongVersionCode()>=14,"new app version installed");
        runOnMainSync(current::recreate);waitForIdleSync();waitText(scenarioName,false);waitForIdleSync();
        require(Arrays.equals(before,SaveCodec.encode(saved())),"upgrade and recreation preserve every gameplay field");
        try(DataInputStream in=new DataInputStream(getTargetContext().openFileInput("auto.sg11"))){in.readInt();require(in.readInt()==15,"upgraded writer produced v15 header");}
        screenshot("00-v09-upgrade-preserved");
    }
    private void contestFlow()throws Exception {
        clickNav("菜单");click("新游戏 / 选择势力",true);click("文武对决 ·",false);click("文武营",true);click("执行",true);
        World w=saved();require(w.scenarioId.equals("contest-drill")&&w.units.size()==2&&w.contests.hasProfile(2),"real bundled contest opening and profiles load");
        for(long seed=0;seed<100;seed++){w.strategy.setSeed(seed);World probe=SaveCodec.decode(SaveCodec.encode(w));probe.contests.challenge(1,2);if(probe.contests.busy())break;}
        installFixture(w,w.unit(1).hex);byte[] before=SaveCodec.encode(saved());
        click("单挑",true);click("岳平 ·",false);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"duel confirmation cancel costs nothing");
        click("单挑",true);click("岳平 ·",false);click("执行",true);waitText("单挑 · 第0",false);screenshot("41-duel-start");
        require(saved().unit(1).energy==70&&saved().unit(1).acted&&saved().contests.busy(),"duel initiation pays once");
        require(!waitText("下一旬  →",true).isEnabled(),"next turn blocked during duel");
        click("交锋",true);waitText("单挑 · 第1",false);before=SaveCodec.encode(saved());
        runOnMainSync(current::recreate);waitText("单挑 · 第1",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"duel round restores without duplicate exchange");screenshot("42-duel-restored");
        clickNav("菜单");click("保存局面（3个槽位）",true);click("槽位 2 ·",false);click("执行",true);click("继续当前对局",true);
        click("认输并结束单挑",true);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"concession cancel preserves contest");
        click("认输并结束单挑",true);click("执行",true);require(!saved().contests.busy()&&saved().government.captive(0)&&saved().unit(1)==null,"actual duel loss captures commander and dissolves unit");
        screenshot("43-duel-capture");
        clickNav("菜单");click("读取存档",true);click("槽位 2 · 文武对决",false);click("执行",true);waitText("单挑 · 第1",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"manual slot restores in-progress duel exactly");
        clickNav("菜单");click("新游戏 / 选择势力",true);click("文武对决 ·",false);click("文武营",true);click("执行",true);
        locateCity("东营");click("武将",true);click("舌战登用",true);click("林策 ·",false);click("苏澄 ·",false);
        before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"debate confirmation cancel costs nothing");
        click("舌战登用",true);click("林策 ·",false);click("苏澄 ·",false);click("执行",true);waitText("舌战 · 第0",false);screenshot("44-debate-start");
        require(saved().city(10).gold==9900&&saved().actionPoints[0]==50,"debate start pays once");
        click("再考 · 更换全部手牌",true);before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("舌战 · 第0",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"rethink cards/RNG/lock survive recreation");
        Debate d=saved().contests.current().debate();int choice=0;while(d.cardError(choice)!=null)choice++;
        click("出牌 · "+d.speaker(0).hand().get(choice).label(),true);waitText("舌战 · 第1",false);screenshot("45-debate-card");
        // Continue actual engine commands to a pending victory, then exercise the native result choice.
        World pending=null;
        for(int seed=0;seed<100&&pending==null;seed++){
            World candidate=ScenarioCatalog.load("contest-drill",0);candidate.strategy.setSeed(seed);candidate.contests.persuade(10,2,6);
            while(candidate.contests.current().debate().winner()==-2){
                Contests.Session cs=candidate.contests.current();Debate debate=cs.debate();int best=-1,score=-999;
                for(int i=0;i<debate.speaker(0).hand().size();i++){Debate.Card card=debate.speaker(0).hand().get(i);if(debate.cardError(i)!=null)continue;int n=card.talk==Debate.Talk.SHOUT?40:card.talk==Debate.Talk.GUILE?30:card.talk==Debate.Talk.IGNORE?25:card.talk!=null?0:card.size*4+(card.topic==debate.topic()?20:0);if(n>score){score=n;best=i;}}
                require(best>=0,"playable debate hand");require(candidate.contests.debateCard(cs.id(),cs.revision(),best).ok,"advance actual debate");
            }
            if(candidate.contests.current().debate().winner()==0)pending=candidate;
        }
        require(pending!=null,"deterministic actual debate victory available");installFixture(pending,pending.city(10).hex);waitText("舌战获胜",false);
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("舌战获胜",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"pending victory remains uncommitted after recreation");screenshot("46-debate-victory");
        click("留情 · 技巧+50",true);require(!saved().contests.busy()&&saved().officer(6).owner==0&&saved().campaign.points(0)==50,"native mercy choice recruits officer and awards points once");
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("文武对决",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"settled debate does not pay rewards twice");screenshot("47-contest-settled");
    }
    private void openEditor(){clickNav("菜单");click("PK编辑 / 新武将",true);}
    private void mediationMenu(){locateCity("文华城");click("武将",true);click("仲介 / 结义婚姻",true);}
    private void treasureMenu(){locateCity("文华城");click("武将",true);click("宝物 / 赏赐收回",true);}
    private void worldDistrictWizard() {
        clickNav("菜单");click("军团与天下",true);click("军团编制",true);click("新设军团",true);click("北境城",true);click("下一步",true);click("内政优先",true);click("经略主城",true);click("预览编制",true);
    }
    private void worldFlow()throws Exception {
        clickNav("菜单");click("新游戏 / 选择势力",true);click("军团与天下 ·",false);click("经略营",true);click("执行",true);
        World w=saved();require(w.scenarioId.equals("world-drill")&&w.events.camps().size()==1&&w.events.enabled(),"new bundled world scenario is playable");
        byte[] before=SaveCodec.encode(w);worldDistrictWizard();screenshot("70-district-preview");click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"district preview cancellation is pure");
        worldDistrictWizard();click("执行",true);require(saved().districts.all().size()==1&&saved().districts.all().get(0).cities().contains(11),"actual district created through UI");
        endTurn();waitForTurn(1);endTurn();waitForTurn(2);w=saved();require(w.domestic.missions.stream().anyMatch(m->m.sourceCity==11&&m.targetCity==10),"district really dispatched supply transport");
        clickNav("菜单");click("军团与天下",true);click("军团编制",true);click("第2军团 ·",false);waitText("军团行动力：",false);screenshot("71-district-orders");click("返回",true);
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("军团与天下",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"district and active cargo survive recreation");
        clickNav("菜单");click("军团与天下",true);click("灾害与贼患",true);waitText("蝗灾",false);screenshot("72-world-events");click("返回",true);
        w=saved();w.unit(6).hex=new Hex(7,18);w.unit(6).acted=false;w.unit(6).movementSpent=0;w.unit(6).movementBudget=-1;installFixture(w,w.unit(6).hex);tapHex(w.unit(6).hex);
        click("讨伐贼寨",true);click("盗贼 · 兵",false);click("执行",true);require(saved().events.camps().get(0).troops<3000,"real camp damage");screenshot("73-raider-attack");
        w=ScenarioCatalog.load("world-drill",0,41);for(int seed=0;seed<100;seed++){w.strategy.setSeed(seed);World trial=SaveCodec.decode(SaveCodec.encode(w));trial.war.plot(1,trial.unit(4).hex,War.Plot.LIGHTNING);if(trial.unit(4).troops<w.unit(4).troops)break;}
        installFixture(w,w.unit(1).hex);tapHex(w.unit(1).hex);click("部队计略",true);click("落雷 · 气力1",true);click("对阵将 ·",false);before=SaveCodec.encode(saved());screenshot("74-lightning-preview");click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"lightning preview is pure");
        tapHex(w.unit(1).hex);click("部队计略",true);click("落雷 · 气力1",true);click("对阵将 ·",false);click("执行",true);require(saved().unit(4).troops<6000&&saved().unit(1).energy==99&&!saved().war.fires().isEmpty(),"UI lightning consumes one and affects actual units/fire");screenshot("75-lightning-result");
        w=ScenarioCatalog.load("world-drill",0,41);for(int seed=0;seed<100;seed++){w.strategy.setSeed(seed);World trial=SaveCodec.decode(SaveCodec.encode(w));trial.campaign.negotiate(10,1,1,Campaign.TreatyKind.CEASEFIRE,6);if(trial.contests.busy())break;}
        installFixture(w,w.city(10).hex);locateCity("经略主城");click("外交",true);click("外交 / 协定",true);click("对阵营 ·",false);click("停战 · 金1000",true);click("使节 ·",false);click("6旬",true);click("执行",true);
        require(saved().contests.busy()&&saved().contests.current().diplomatic(),"real negotiation opens diplomatic debate");waitText("外交 · 停战 6旬",true);screenshot("76-diplomatic-debate");before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("外交 · 停战 6旬",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"diplomatic session survives recreation");
        click("认输并结束舌战",true);click("执行",true);require(!saved().contests.busy()&&saved().campaign.treaty(0,1)==null&&saved().city(10).gold==29000,"UI diplomatic defeat preserves fee and no treaty");
    }
    private void estatesFlow()throws Exception {
        clickNav("菜单");click("新游戏 / 选择势力",true);click("相知寻宝 ·",false);click("文华营",true);click("执行",true);waitForIdleSync();
        require(saved().treasures.items().size()==43,"scenario loads actual item states");
        byte[] before=SaveCodec.encode(saved());
        mediationMenu();click("义兄弟",true);click("司库官 ·",false);click("武备官 ·",false);screenshot("60-mediate-preview");click("取消",true);
        require(Arrays.equals(before,SaveCodec.encode(saved())),"mediation cancel is pure");
        mediationMenu();click("义兄弟",true);click("司库官 ·",false);click("武备官 ·",false);click("执行",true);
        require(saved().relations.sworn(1,2)&&saved().campaign.points(0)==4500,"actual sworn mediation and cost");
        mediationMenu();click("配偶",true);click("文华统领 ·",false);click("知音 ·",false);click("执行",true);
        require(saved().relations.spouse(0)==3&&saved().officer(0).war==76&&saved().officer(3).war==76,"actual marriage and inner assistance");
        treasureMenu();click("赏赐府库宝物",true);click("赤兔馬 ·",false);click("武备官 ·",false);click("司库官",true);screenshot("61-treasure-award");click("执行",true);
        require(saved().contests.profile(2).has(Contests.Gear.HORSE)&&saved().actionPoints[0]==50,"item command changes gear and charges action");
        locateCity("文华城");click("武将",true);click("武备官 ·",false);waitText("赤兔馬",false);screenshot("62-person-relations-items");click("返回",true);
        openEditor();click("编辑武将",true);click("武备官 ·",false);setInput("武力（0—100）","91");click("预览修改",true);waitText("75 → 91",false);screenshot("63-pk-officer-preview");before=SaveCodec.encode(saved());click("取消",true);
        require(Arrays.equals(before,SaveCodec.encode(saved())),"PK officer preview cancellation");
        openEditor();click("编辑武将",true);click("武备官 ·",false);setInput("武力（0—100）","91");click("预览修改",true);click("应用修改",true);
        require(saved().officer(2).war==91&&saved().editor.edited()&&saved().actionPoints[0]==50,"PK edit changes selected value without action cost");
        openEditor();click("编辑据点",true);click("文华城",true);setInput("金","12345");click("预览修改",true);click("应用修改",true);require(saved().city(10).gold==12345,"real site editor");
        openEditor();click("编辑势力",true);click("文华营",true);click("行动力与技巧点",true);setInput("技巧点（0—100000）","2345");click("预览修改",true);click("应用修改",true);require(saved().campaign.points(0)==2345,"real faction editor");
        openEditor();click("编辑部队",true);click("领阵将",true);setInput("携金（0—10000）","4321");click("预览修改",true);click("应用修改",true);require(saved().unit(1).gold==4321,"real unit editor");
        openEditor();click("制作新武将模板",true);setInput("姓名","清和");setInput("武力（0—100）","88");screenshot("64-custom-officer-form");click("保存模板",true);waitText("模板已保存",true);click("返回",true);
        openEditor();click("已保存新武将",true);click("清和",true);click("文华城",true);click("文华营",true);screenshot("65-custom-officer-placement");click("应用修改",true);
        World w=saved();World.Officer custom=w.officers.stream().filter(o->o.name.equals("清和")).findFirst().orElse(null);require(custom!=null&&custom.war==88&&custom.cityId==10&&w.editor.custom(custom.id),"custom officer created from persisted template");
        Editor.Template template=w.editor.template(custom.id);byte[] templateBytes=OfficerTemplateCodec.encode(template);File exported=new File(getTargetContext().getFilesDir(),"smoke-officer-export.sgof");
        try(FileOutputStream out=getTargetContext().openFileOutput("pending-officer-export.sgof",0)){out.write(templateBytes);}
        Intent document=new Intent().setData(android.net.Uri.fromFile(exported));runOnMainSync(()->((MainActivity)current).onActivityResult(913,Activity.RESULT_OK,document));waitText("模板已导出",true);click("返回",true);
        try(InputStream in=new FileInputStream(exported)){require(OfficerTemplateCodec.read(in).stat(1)==88,"actual document resolver writes reusable officer template");}
        before=SaveCodec.encode(saved());runOnMainSync(()->((MainActivity)current).onActivityResult(914,Activity.RESULT_OK,document));waitText("导入新武将模板",true);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"template import cancel preserves game");
        runOnMainSync(()->((MainActivity)current).onActivityResult(914,Activity.RESULT_OK,document));click("保存模板",true);waitText("模板已保存",true);click("返回",true);
        runOnMainSync(current::recreate);waitText("相知寻宝",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"all edits, relationships and items survive activity recreation");screenshot("66-estates-restored");
        openEditor();click("已保存新武将",true);waitText("清和",true);click("返回",true);
    }
    private void fieldworkFlow()throws Exception {
        World w=ScenarioCatalog.load("fieldworks-drill",0);w.city(20).troops=0;w.city(21).troops=0;
        World.Unit enemy=w.unit(2);enemy.hex=new Hex(23,17);enemy.acted=true;
        installFixture(w,w.unit(1).hex);click("设置军事设施",true);click("阵 · 金1500",true);click("7,5",true);
        byte[] before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"fieldwork cancel is pure");
        click("设置军事设施",true);click("阵 · 金1500",true);click("7,5",true);click("执行",true);
        w=saved();War.Structure s=w.war.at(new Hex(7,5));require(s!=null&&!s.complete&&s.builder==1&&w.unit(1).gold==8500,"UI construction uses carried gold and incomplete structure");screenshot("51-fieldwork-building");
        before=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("筑垒研兵",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"construction and carried gold survive recreation");
        click("中止施工",true);click("执行",true);int hp=saved().war.at(new Hex(7,5)).hp;
        endTurn();waitForTurn(1);require(saved().war.at(new Hex(7,5)).hp==hp,"stopped construction does not progress");
        tapHex(saved().unit(1).hex);click("补修军事设施",true);click("阵 ·",false);click("执行",true);
        endTurn();waitForTurn(2);require(saved().war.at(new Hex(7,5)).complete,"actual turn completes resumed repair");screenshot("52-fieldwork-completed");
        locateCity("工营主城");click("研究",true);click("技巧研究",true);click("发明",true);click("车轴强化 ·",false);click("工营统领 ·",false);
        before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"tech group preview is pure");
        locateCity("工营主城");click("研究",true);click("技巧研究",true);click("发明",true);click("车轴强化 ·",false);click("工营统领 ·",false);click("执行",true);
        require(saved().campaign.projects().size()==1&&saved().campaign.projects().get(0).tech==Campaign.Tech.AXLE,"real first-tier invention research started");
        for(int turn=3;turn<=5;turn++){endTurn();waitForTurn(turn);}require(saved().campaign.has(0,Campaign.Tech.AXLE),"UI research completes");
        locateCity("工营主城");click("研究",true);click("技巧研究",true);click("发明",true);waitText("石造建筑 ·",false);screenshot("53-tech-branch");click("取消",true);
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("筑垒研兵",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"new research and completed works persist");
    }
    private void abilityFlow()throws Exception {
        World w=new World(20,14,"学营","守营");w.scenarioId="pk-smoke";w.scenarioName="PK培养演练";
        w.cities.add(new World.City(10,"学宫",new Hex(2,2),0));w.cities.add(new World.City(20,"守城",new Hex(17,11),1));
        for(World.City c:w.cities){c.gold=30000;c.food=200000;c.troops=0;}
        w.officers.add(new World.Officer(0,"习武生",0,10,50,50,50,50,50));w.officer(0).role=Strategy.Role.RULER;w.officer(0).loyalty=100;
        w.officers.add(new World.Officer(1,"研习生",0,10,50,50,50,50,50));
        installFixture(w,w.city(10).hex);locateCity("学宫");click("研究",true);click("PK能力研究",true);click("防御",true);click("统率+5低 · 可研究",true);
        byte[] before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"cancel ability research changes no RNG/cost/hidden selection");
        locateCity("学宫");click("研究",true);click("PK能力研究",true);click("防御",true);click("统率+5低 · 可研究",true);click("执行",true);
        require(saved().abilities.research(0).remaining==9&&saved().city(10).gold==29700&&saved().actionPoints[0]==40,"UI starts real nine-turn research");
        click("功能",true);waitText("任务 1",true);
        clickNav("任务");click("筛选 · 全部任务",true);click("研究 / 培养",true);waitText("PK研究统率+5低",true);screenshot("48-pk-research");
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("PK研究统率+5低",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"research survives recreation");
        for(int turn=1;turn<=9;turn++){endTurn();waitForTurn(turn);}require(saved().abilities.learned(0,"lead.low"),"full UI turn loop unlocks research");
        locateCity("学宫");click("研究",true);click("能力 / 适性培养",true);click("基础能力",true);click("统率+5低 · 剩5次",true);click("习武生 · 50",true);click("执行",true);
        click("功能",true);waitText("任务 1",true);
        clickNav("任务");waitText("PK培养统率+5低 · 习武生",true);screenshot("49-pk-training");
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("PK培养统率+5低 · 习武生",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"training survives recreation");
        for(int turn=10;turn<=12;turn++){endTurn();waitForTurn(turn);}require(saved().officer(0).leadership==55&&saved().abilities.remaining(0,"lead.low")==4,"real stat change and finite use");
        locateCity("学宫");click("研究",true);click("PK能力研究",true);click("防御",true);click("不屈 · 可研究",true);click("执行",true);
        for(int turn=13;turn<=21;turn++){endTurn();waitForTurn(turn);}
        w=saved();w.officer(1).skillId=Skill.YANLI.id;installFixture(w,w.city(10).hex);
        locateCity("学宫");click("研究",true);click("能力 / 适性培养",true);click("特技",true);click("不屈 · 剩3次",true);click("研习生 · 眼力",true);waitText("原特技「眼力」将被「不屈」覆盖。",false);
        before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"cancel skill overwrite preserves original skill and uses");
        locateCity("学宫");click("研究",true);click("能力 / 适性培养",true);click("特技",true);click("不屈 · 剩3次",true);click("研习生 · 眼力",true);click("执行",true);
        for(int turn=22;turn<=24;turn++){endTurn();waitForTurn(turn);}require(saved().officer(1).skillId.equals(Skill.BUQU.id)&&saved().abilities.remaining(0,"buqu")==2,"learned skill overwrites actual officer after three turns");
        locateCity("学宫");click("研究",true);click("PK研究与培养进度",true);waitText("不屈 · 剩2次",false);screenshot("50-pk-completed");click("返回",true);
    }
    private void installFixture(World w,Hex focus)throws Exception {
        SaveCodec.validate(w);java.lang.reflect.Field field=MainActivity.class.getDeclaredField("world");field.setAccessible(true);
        runOnMainSync(()->{try{field.set(current,w);((MainActivity)current).selectAndFocus(focus);}catch(IllegalAccessException e){throw new RuntimeException(e);}});
        try(FileOutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(w));}waitForIdleSync();
    }
    private void armyCity(){clickNav("城市");click("江东大营 · 江东军",false);click("军事",true);}
    private void fillFormation(){
        armyCity();click("编队 / 水陆出征",true);click("孙权 ·",false);click("周瑜 ·",false);click("甘宁 ·",false);click("鲁肃 ·",false);
        click("下一步",true);click("冲车 ·",false);click("楼船 ·",false);click("3000人",true);click("18000粮",true);click("0金",true);
    }
    private void armyFlow()throws Exception {
        click("菜单",true);click("新游戏 / 选择势力",true);click("水陆攻防 ·",false);click("江东军",true);click("执行",true);
        byte[] initial=SaveCodec.encode(saved());fillFormation();screenshot("23-formation-confirm");click("取消",true);
        require(Arrays.equals(initial,SaveCodec.encode(saved())),"formation preview and cancel do not change state");
        fillFormation();click("执行",true);World w=saved();World.Unit army=w.unit(1);
        require(army!=null&&army.weapon==World.Weapon.RAM&&army.ship==Army.Ship.TOWER_SHIP&&army.deputies.length==2,"UI creates three-officer siege/ship formation, caps deputies");
        require(w.officer(1).unitId==1&&w.officer(2).unitId==1&&w.officer(3).unitId==-1,"only selected two deputies leave city");
        require(w.city(10).ships[0]==2&&w.city(10).equipment[5]==1&&army.food==18000&&w.actionPoints[0]==50,"UI charges one ship, one ram and one AP payment");screenshot("24-three-officer-army");
        byte[] before=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("水陆攻防",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"v6 formation survives Activity recreation");
        tapCity(10,0);click("执行",true);w=saved();require(w.units.isEmpty()&&w.officer(1).cityId==10&&w.city(10).ships[0]==3&&w.city(10).equipment[5]==2,"UI returns all crew and equipment once");

        World factory=ScenarioCatalog.load("river-siege-sandbox",0);for(World.City city:factory.cities)if(city.owner==1){city.gold=0;city.troops=0;}
        Hex site=factory.domestic.buildSites(10).get(0);require(factory.domestic.build(10,3,Domestic.Kind.WORKSHOP,site).ok,"manufacturing fixture workshop starts");
        for(int i=0;i<3;i++)require(factory.nextTurn().ok,"manufacturing fixture advances");installFixture(factory,factory.city(10).hex);
        armyCity();click("军备制造 / 攻城器械与舰船",true);click("井阑 ·",false);click("周瑜 ·",false);click("执行",true);
        w=saved();require(w.army.productions().size()==1&&w.officer(1).otherTaskTurns==3,"manufacturing starts from UI");click("功能",true);waitText("任务 1",true);
        clickNav("任务");click("筛选 · 全部任务",true);click("军备制造",true);waitText("制造井阑 · 周瑜",true);screenshot("25-manufacturing-task");
        int count=w.city(10).equipment[6],turn=w.turn;for(int i=1;i<=3;i++){endTurn();waitForTurn(turn+i);}w=saved();require(w.city(10).equipment[6]==count+1&&w.army.productions().isEmpty(),"UI turn loop completes one equipment item");

        World naval=ScenarioCatalog.load("river-siege-sandbox",0);
        World.Unit actor=new World.Unit(1,0,2,World.Weapon.SPEAR,new Hex(9,8),5000,20000);actor.ship=Army.Ship.WARSHIP;
        World.Unit target=new World.Unit(2,1,7,World.Weapon.CAVALRY,new Hex(10,8),5000,20000);target.ship=Army.Ship.TOWER_SHIP;
        naval.units.add(actor);naval.units.add(target);naval.nextUnitId=3;
        naval.officer(2).cityId=-1;naval.officer(2).unitId=1;naval.officer(7).cityId=-1;naval.officer(7).unitId=2;naval.strategy.setSeed(0);installFixture(naval,actor.hex);
        click("战法",true);click("火矢 ·",false);click("张辽",true);click("执行",true);w=saved();
        require(w.unit(2).burning==2&&w.unit(2).troops<5000&&w.unit(1).energy==70,"naval fire tactic updates actual troops and persistent burning");screenshot("26-naval-combat");
        before=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("水陆攻防",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"naval outcome survives restart");
        World crossing=SaveCodec.decode(before);crossing.unit(1).acted=false;installFixture(crossing,crossing.unit(1).hex);
        tapHex(new Hex(8,8));click("开始行军",true);w=saved();require(w.unit(1).hex.equals(new Hex(8,8))&&w.unit(1).weapon==World.Weapon.SPEAR&&w.unit(1).ship==Army.Ship.WARSHIP,"map tap disembarks with preserved land gear and ship");screenshot("27-disembarked");
    }
    private void rulesFlow()throws Exception {
        World w=ScenarioCatalog.load("river-siege-sandbox",0);
        World.Unit actor=new World.Unit(1,0,0,World.Weapon.SPEAR,new Hex(6,8),5000,20000);
        actor.deputies=new int[]{1,2};w.officer(0).skillId=Skill.SHENSUAN.id;w.officer(0).intelligence=100;
        w.officer(1).skillId=Skill.BAICHU.id;w.officer(2).skillId=Skill.LIANHUAN.id;
        World.Unit target=new World.Unit(2,1,6,World.Weapon.SPEAR,new Hex(8,8),5000,20000);
        World.Unit chained=new World.Unit(3,1,7,World.Weapon.SPEAR,new Hex(8,9),5000,20000);
        w.units.add(actor);w.units.add(target);w.units.add(chained);w.nextUnitId=4;
        for(World.Unit u:w.units)for(World.Officer o:w.army.crew(u)){o.cityId=-1;o.unitId=u.id;}
        for(World.City c:w.cities)c.governorId=-1;
        installFixture(w,actor.hex);byte[] initial=SaveCodec.encode(saved());
        tapHex(new Hex(7,8));waitText("路线预览",false);screenshot("28-move-preview");click("取消",true);
        require(Arrays.equals(initial,SaveCodec.encode(saved())),"move cancel does not change world or RNG");
        tapHex(new Hex(7,8));click("开始行军",true);w=saved();
        require(w.unit(1).hex.equals(new Hex(7,8))&&!w.unit(1).acted&&w.unit(1).movementSpent>0,"move preserves command and charges path");
        byte[] moved=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("水陆攻防",false);
        require(Arrays.equals(moved,SaveCodec.encode(saved())),"movement budget survives recreation");
        click("选中对象指令 ·",false);
        click("部队计略",true);click("扰乱 · 气力1",true);click("曹操 ·",false);screenshot("29-skill-plot-preview");click("取消",true);
        require(Arrays.equals(moved,SaveCodec.encode(saved())),"skill plot cancel is pure");
        click("部队计略",true);click("扰乱 · 气力1",true);click("曹操 ·",false);click("执行",true);w=saved();
        require(w.unit(1).acted&&w.unit(1).energy==79&&w.unit(2).statusTurns==2&&w.unit(3).statusTurns==2,"UI 神算百出连环 costs once and resolves two targets");
        screenshot("30-move-then-skills");
    }
    private String contentAnchor()throws Exception {java.lang.reflect.Field f=MainActivity.class.getDeclaredField("ui");f.setAccessible(true);return ((ClientState)f.get(current)).contentFirstId;}
    private void sourceProfileFlow()throws Exception {
        World w=ScenarioCatalog.load("regional-sandbox",0);installFixture(w,w.city(100).hex);
        int id=ContentCatalog.get().officers().stream().filter(o->o.name.equals("曹丕")).findFirst().get().id;
        click("菜单",true);click("全国资料 / 核验目录",true);setInput("搜索资料","曹丕");click("曹丕 · ID",false);
        scrollToText("已解析关系",false);click("加入局面",true);click("预览",true);waitText("确认加入资料武将",true);
        byte[] before=SaveCodec.encode(saved());screenshot("v018-source-preview");click("返回",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"cancel sourced import leaves save unchanged");
        click("曹丕 · ID",false);click("加入局面",true);click("预览",true);click("确认加入",true);waitForIdleSync();
        w=saved();require(w.officer(id)!=null&&w.life.life(id)!=null&&w.editor.edited(),"native import creates playable source officer and biography");
        require(w.relations.parent(id,false)==2000,"native imported child links to existing source father");
        before=SaveCodec.encode(w);runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(before,SaveCodec.encode(saved())),"source import survives Activity recreation");
        screenshot("v018-source-imported");
    }
    private void contentFlow()throws Exception {
        click("菜单",true);click("新游戏 / 选择势力",true);click("武将资料演练 ·",false);click("孙权军",true);waitText("能力/适性来自公开资料",false);screenshot("28-sourced-opening");click("执行",true);waitForIdleSync();
        assertWorld(2,0,"officer-reference-drill");ContentCatalog.get().validateOpening(saved());
        clickNav("城市");setInput("搜索城市或势力","不存在");waitText("没有符合条件的城池",false);setInput("搜索城市或势力","建业");click("建业 · 孙权军",false);
        click("军事",true);click("出征",true);click("甘宁",true);click("弩兵",true);click("3000人",true);waitForIdleSync();require(saved().officer(3003).unitId>0,"sourced opening deployed via native command");
        require(saved().officer(3003).skillId.equals(Skill.WEIFENG.id),"source skill ID binds to actual runtime officer");
        tapHex(saved().unit(saved().officer(3003).unitId).hex);click("编队特技",true);waitText("甘宁 · 威风",false);screenshot("33-sourced-runtime-skill");click("返回",true);
        endTurn();waitForTurn(1);
        clickNav("任务");setInput("搜索任务、武将或城市","不存在");waitText("没有符合检索条件的任务",false);
        click("菜单",true);click("势力一览",true);setInput("搜索势力","孙权");waitText("孙权军",true);screenshot("29-faction-search");
        click("菜单",true);click("全国资料 / 核验目录",true);waitText("显示 670 / 670 条",true);
        for(int attempt=0;attempt<4&&contentAnchor().equals("1000");attempt++){AccessibilityNodeInfo list=findInput(getUiAutomation().getRootInActiveWindow(),"资料列表");require(list!=null&&list.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD),"native long catalog scroll");waitForIdleSync();SystemClock.sleep(400);}String anchor=contentAnchor();require(!anchor.isEmpty()&&!anchor.equals("1000"),"scroll moved to later stable ID");runOnMainSync(current::recreate);waitText("显示 670 / 670 条",true);waitForIdleSync();require(anchor.equals(contentAnchor()),"long catalog stable row restores");
        setInput("搜索资料","诸葛亮");waitText("諸葛亮 · ID 1004",false);click("諸葛亮 · ID 1004",false);waitText("智力 100",false);click("返回",true);
        setInput("搜索资料","不存在");waitText("没有符合条件的资料",false);setInput("搜索资料","诸葛亮");runOnMainSync(current::recreate);waitText("显示 1 / 670 条",true);waitText("諸葛亮 · ID 1004",false);screenshot("30-content-restored");
        click("据点分布预览",true);waitText("42 城来源 X/Y 分布",false);screenshot("31-source-coordinates");click("返回",true);
        click("武将资料",true);click("剧本缺口",true);waitText("显示 14 / 14 条",true);click("黄巾之乱 · ID",false);waitText("缺少原版地形",false);click("返回",true);
        click("菜单",true);click("保存局面（3个槽位）",true);click("槽位 1 ·",false);click("执行",true);waitForIdleSync();byte[] before=SaveCodec.encode(saved());
        click("菜单",true);click("新游戏 / 选择势力",true);click("基础演练 ·",false);click("曹操军",true);click("执行",true);
        click("菜单",true);click("读取存档",true);click("槽位 1 · 武将资料演练",false);click("执行",true);waitForIdleSync();require(Arrays.equals(before,SaveCodec.encode(saved())),"sourced snapshot exact restore after different opening");
        click("全图",true);click("导航图",true);MapView map=mapView();MapCamera camera=camera(map);pinch(map,1.5f);
        float[] old={0};runOnMainSync(()->old[0]=camera.centerX());int[] pos=new int[2];float[] target=new float[2];runOnMainSync(()->{map.getLocationOnScreen(pos);float den=map.getResources().getDisplayMetrics().density;target[0]=pos[0]+map.getWidth()-20*den;target[1]=pos[1]+24*den;});
        long t=SystemClock.uptimeMillis();send(t,t,MotionEvent.ACTION_DOWN,target[0],target[1]);send(t,t+40,MotionEvent.ACTION_MOVE,target[0]-20,target[1]+20);send(t,t+80,MotionEvent.ACTION_UP,target[0]-20,target[1]+20);waitForIdleSync();require(camera.centerX()!=old[0],"navigator touch relocates camera");require(Arrays.equals(before,SaveCodec.encode(saved())),"navigator does not issue commands");screenshot("32-navigator");
        runOnMainSync(current::recreate);waitText("武将资料演练",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"sourced map recreation keeps snapshot");
        // A maximum-sized engineering stress fixture, explicitly unrelated to original national terrain.
        World stress=new World(299,200);stress.sourceMapWidth=200;stress.scenarioId="viewport-stress";stress.scenarioName="200 格绘制压测";
        for(int y=0;y<200;y++)for(int x=0;x<299;x++){Hex raw=MapCoordinates.source(new Hex(x,y),200);if(raw.q<0||raw.q>=200)stress.terrain[x][y]=World.Terrain.MOUNTAIN;}
        for(int i=0;i<87;i++)stress.cities.add(new World.City(40000+i,"压测城"+i,MapCoordinates.axial((i*17)%200,(i*13)%200,200),i%2));
        for(int i=0;i<670;i++)stress.officers.add(new World.Officer(50000+i,"压测将"+i,(i%87)%2,40000+i%87,70,70,70,70,70));
        long[] elapsed=new long[40];int[] visits=new int[2];MapView finalMap=mapView();
        runOnMainSync(()->{finalMap.setWorld(stress,null,-1);finalMap.focus(MapCoordinates.axial(100,100,200));Bitmap bitmap=Bitmap.createBitmap(finalMap.getWidth(),finalMap.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);for(int i=0;i<45;i++){finalMap.draw(canvas);if(i>=5)elapsed[i-5]=finalMap.drawNanos();}visits[0]=finalMap.tilesVisited();visits[1]=finalMap.objectsVisited();bitmap.recycle();});
        Arrays.sort(elapsed);require(visits[0]<200*200/4,"large map drawing visits visible area");
        try(PrintWriter out=new PrintWriter(new File(getTargetContext().getExternalFilesDir(null),"content-performance.txt"))){out.println("Engineering 200x200 odd-r (299x200 axial), 87 cities, 670 officers; software Canvas onDraw timing, NOT FPS or GPU latency");out.println("viewport="+finalMap.getWidth()+"x"+finalMap.getHeight()+" tilesVisited="+visits[0]+" objectsVisited="+visits[1]);out.println("warm samples=40 medianMs="+elapsed[20]/1e6+" p95Ms="+elapsed[38]/1e6+" maxMs="+elapsed[39]/1e6);}
        runOnMainSync(()->((MainActivity)current).refresh());
    }
    private void tapHex(Hex h)throws Exception {
        MapView map=mapView();MapCamera c=camera(map);int[] pos=new int[2];float[] point=new float[2];
        runOnMainSync(()->{map.getLocationOnScreen(pos);point[0]=pos[0]+25*1.7320508f*(h.q+h.r*.5f)*c.scale+c.x;point[1]=pos[1]+25*1.5f*h.r*c.scale+c.y;});
        long t=SystemClock.uptimeMillis();send(t,t,MotionEvent.ACTION_DOWN,point[0],point[1]);send(t,t+80,MotionEvent.ACTION_UP,point[0],point[1]);SystemClock.sleep(500);waitForIdleSync();
    }
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
    private void locateCity(String city){
        World w;try{w=saved();}catch(IOException e){throw new AssertionError("read city selection state",e);}
        World.City target=w.cities.stream().filter(c->c.name.equals(city)).findFirst().orElse(null);
        require(target!=null,"city exists in active scenario: "+city);
        clickNav("城市");click(city+" · "+w.faction(target.owner),false);waitText(city,true);
    }
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
        if(text.startsWith("导航 · ")||text.startsWith("选中对象指令 ·"))value=node.getContentDescription();
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
        if(exact&&text.equals("菜单")){clickNav("菜单");return;}
        AccessibilityNodeInfo active=getUiAutomation().getRootInActiveWindow();
        if(text.startsWith("导航 · ")&&find(active,text,exact)==null)click("功能",true);
        else if(exact&&(text.equals("全图")||text.equals("定位")||text.equals("导航图"))&&find(active,text,true)==null)click("视图",true);
        else if(exact&&(text.equals("停止行军")||text.equals("下一部队"))){
            AccessibilityNodeInfo button=find(active,"选中对象指令 ·",false);
            if(button!=null&&button.getText()!=null&&button.getText().toString().endsWith(" · 指令"))click("选中对象指令 ·",false);
        }
        AccessibilityNodeInfo node=scrollToText(text,exact);
        Rect bounds=new Rect();node.getBoundsInScreen(bounds);
        long time=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0);
        MotionEvent up=MotionEvent.obtain(time,time+40,MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);
        sendPointerSync(down);sendPointerSync(up);down.recycle();up.recycle();waitForIdleSync();SystemClock.sleep(350);
    }
    private AccessibilityNodeInfo scrollToText(String text,boolean exact) {
        AccessibilityNodeInfo node=null;long until=SystemClock.uptimeMillis()+12000;
        while(node==null&&SystemClock.uptimeMillis()<until) {
            waitForIdleSync();AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();node=find(root,text,exact);
            if(node==null){if(!scroll(root))scrollBack(root);SystemClock.sleep(250);}
        }
        if(node==null)throw new AssertionError("UI content not reachable by scrolling: "+text);
        return node;
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

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
            personnelFlow();
            campaignFlow();
            armyFlow();
            rulesFlow();
            contentFlow();
            governmentFlow();
            documentTransferFlow();
            result.putString("stream","SMOKE PASS: integrated original game/save/city/task/personnel/combat/army regressions; v8 governance/capture/rank/summon, document export/import/cancel/corruption; legacy move preview/cancel/recreation and 神算百出连环; sourced opening, content/search/navigation/save restore and viewport stress verified.\n");
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
    private void campaignFlow()throws Exception {
        click("菜单",true);click("新游戏 / 选择势力",true);click("区域争雄 ·",false);click("孙权军",true);click("执行",true);waitForIdleSync();
        World opening=saved();int quote=opening.campaign.foodPrice(300,true),openingGold=opening.city(300).gold,openingFood=opening.city(300).food;
        require(opening.startMonth==9&&quote==140,"September fixture has seasonal quote140, not January price100");
        locateCity("柴桑");click("内政",true);click("商人 / 粮食买卖",true);waitText("买粮 · 每1000粮 / 金"+quote,true);click("买粮 ·",false);click("1000粮",true);click("孙权 ·",false);
        byte[] before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"cancel trade preserves exact save");
        locateCity("柴桑");click("内政",true);click("商人 / 粮食买卖",true);click("买粮 ·",false);click("1000粮",true);click("孙权 ·",false);click("执行",true);
        World w=saved();require(w.city(300).food==openingFood+1000&&w.city(300).gold==openingGold-quote&&w.campaign.traded(300)==1000,"UI trade executes quoted amount once");screenshot("18-merchant");
        locateCity("柴桑");click("技巧 / 培养",true);click("能力 / 适性培养",true);click("周瑜 ·",false);click("枪兵适性 · 当前B",true);click("执行",true);
        w=saved();require(w.officer(3001).otherTaskTurns==3&&w.campaign.projects().size()==1&&w.city(300).gold==openingGold-quote-600,"study is real locked task");
        clickNav("任务");click("筛选 · 全部任务",true);click("研究 / 培养",true);waitText("培养枪兵适性 · 周瑜",true);screenshot("19-study-task");
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("培养枪兵适性 · 周瑜",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"new task and filter survive Activity recreation");
        for(int i=1;i<=3;i++){endTurn();waitForTurn(i);}w=saved();require(w.officer(3001).aptitude[0]==2&&w.campaign.projects().stream().noneMatch(p->p.officerId==3001),"UI turn loop finishes aptitude study exactly once");
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
        tapHex(new Hex(7,5));click("执行",true);
        require(!saved().unit(1).acted,"moving into fire range retains the plot command");
        click("部队计略",true);click("火计 ·",false);click("张辽 ·",false);click("执行",true);w=saved();require(w.war.fireAt(w.unit(2).hex)!=null&&w.unit(1).energy==70,"UI fire plot persists burning hex");screenshot("22-fire-field");
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("战法验证",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"fire survives Activity recreation");
    }
    private void governmentFlow()throws Exception {
        World w=new World(16,12,"刘备军","曹操军");w.scenarioId="ui-governance-fixture";w.scenarioName="军政验证";
        w.cities.add(new World.City(0,"营城",new Hex(1,1),0));w.cities.add(new World.City(1,"后方",new Hex(1,9),0));w.cities.add(new World.City(2,"敌城",new Hex(14,1),1));
        String[] names={"刘备","潘璋","张辽","鲁肃","吕蒙","诸葛瑾","黄盖","周泰"};
        for(int i=0;i<names.length;i++)w.officers.add(new World.Officer(i,names[i],i==2?1:0,i==2?2:i==6?1:0,80,80,80,80,80));
        w.officer(0).role=Strategy.Role.RULER;w.officer(0).loyalty=100;w.officer(1).skillId=Skill.BOFU.id;
        World.Unit a=new World.Unit(1,0,1,World.Weapon.SPEAR,new Hex(4,4),5000,10000),b=new World.Unit(2,1,2,World.Weapon.SPEAR,new Hex(5,4),1,5000);
        w.units.add(a);w.units.add(b);w.nextUnitId=3;w.officer(1).unitId=1;w.officer(1).cityId=-1;w.officer(2).unitId=2;w.officer(2).cityId=-1;
        installFixture(w,a.hex);tapHex(b.hex);click("执行",true);waitForIdleSync();
        require(saved().government.captive(2)&&saved().unit(2)==null,"capture skill produces persisted prisoner from actual map attack");
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
    private void installFixture(World w,Hex focus)throws Exception {
        SaveCodec.validate(w);java.lang.reflect.Field field=MainActivity.class.getDeclaredField("world");field.setAccessible(true);
        runOnMainSync(()->{try{field.set(current,w);((MainActivity)current).selectAndFocus(focus);}catch(IllegalAccessException e){throw new RuntimeException(e);}});
        try(FileOutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(w));}waitForIdleSync();
    }
    private void armyCity(){clickNav("城市");click("江东大营 · 江东军",false);click("军事",true);}
    private void fillFormation(){
        armyCity();click("编队 / 水陆出征",true);click("孙权 ·",false);click("周瑜 ·",false);click("甘宁 ·",false);click("鲁肃 ·",false);
        click("下一步",true);click("冲车 ·",false);click("楼船 ·",false);click("3000人",true);click("18000粮",true);
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
        w=saved();require(w.army.productions().size()==1&&w.officer(1).otherTaskTurns==3,"manufacturing starts from UI");waitText("任务 1",true);
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
        tapHex(new Hex(8,8));click("执行",true);w=saved();require(w.unit(1).hex.equals(new Hex(8,8))&&w.unit(1).weapon==World.Weapon.SPEAR&&w.unit(1).ship==Army.Ship.WARSHIP,"map tap disembarks with preserved land gear and ship");screenshot("27-disembarked");
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
        tapHex(new Hex(7,8));waitText("确认移动",true);screenshot("28-move-preview");click("取消",true);
        require(Arrays.equals(initial,SaveCodec.encode(saved())),"move cancel does not change world or RNG");
        tapHex(new Hex(7,8));click("执行",true);w=saved();
        require(w.unit(1).hex.equals(new Hex(7,8))&&!w.unit(1).acted&&w.unit(1).movementSpent>0,"move preserves command and charges path");
        byte[] moved=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("水陆攻防",false);
        require(Arrays.equals(moved,SaveCodec.encode(saved())),"movement budget survives recreation");
        click("部队计略",true);click("扰乱 · 气力1",true);click("曹操 ·",false);screenshot("29-skill-plot-preview");click("取消",true);
        require(Arrays.equals(moved,SaveCodec.encode(saved())),"skill plot cancel is pure");
        click("部队计略",true);click("扰乱 · 气力1",true);click("曹操 ·",false);click("执行",true);w=saved();
        require(w.unit(1).acted&&w.unit(1).energy==79&&w.unit(2).statusTurns==2&&w.unit(3).statusTurns==2,"UI 神算百出连环 costs once and resolves two targets");
        screenshot("30-move-then-skills");
    }
    private String contentAnchor()throws Exception {java.lang.reflect.Field f=MainActivity.class.getDeclaredField("ui");f.setAccessible(true);return ((ClientState)f.get(current)).contentFirstId;}
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
        World stress=new World(128,128);stress.scenarioId="viewport-stress";stress.scenarioName="128 格绘制压测";
        for(int i=0;i<128;i++){int id=40000+i;stress.cities.add(new World.City(id,"压测城"+i,new Hex(i,i),i%2));stress.officers.add(new World.Officer(50000+i,"压测将"+i,i%2,id,70,70,70,70,70));}
        long[] elapsed=new long[40];int[] visits=new int[2];MapView finalMap=mapView();
        runOnMainSync(()->{finalMap.setWorld(stress,null,-1);finalMap.focus(new Hex(64,64));Bitmap bitmap=Bitmap.createBitmap(finalMap.getWidth(),finalMap.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);for(int i=0;i<45;i++){finalMap.draw(canvas);if(i>=5)elapsed[i-5]=finalMap.drawNanos();}visits[0]=finalMap.tilesVisited();visits[1]=finalMap.objectsVisited();bitmap.recycle();});
        Arrays.sort(elapsed);require(visits[0]<128*128/4,"large map drawing visits visible area");
        try(PrintWriter out=new PrintWriter(new File(getTargetContext().getExternalFilesDir(null),"content-performance.txt"))){out.println("Engineering 128x128, 128 cities, 128 officers; software Canvas onDraw timing, NOT FPS or GPU latency");out.println("viewport="+finalMap.getWidth()+"x"+finalMap.getHeight()+" tilesVisited="+visits[0]+" objectsVisited="+visits[1]);out.println("warm samples=40 medianMs="+elapsed[20]/1e6+" p95Ms="+elapsed[38]/1e6+" maxMs="+elapsed[39]/1e6);}
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

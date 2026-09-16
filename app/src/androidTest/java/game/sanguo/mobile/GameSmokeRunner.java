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
    private String displacement="";
    private String recovery="";
    private boolean upgradeOnly,upgrade25,upgrade26,upgrade27,experience;
    @Override public void callActivityOnResume(Activity a){super.callActivityOnResume(a);current=a;}
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);displacement=arguments==null?"":arguments.getString("displacement","");recovery=arguments==null?"":arguments.getString("recovery","");upgrade27=arguments!=null&&"27".equals(arguments.getString("upgrade"));experience=arguments!=null&&"true".equals(arguments.getString("experience"));upgradeOnly=arguments!=null&&"true".equals(arguments.getString("upgrade"));upgrade25=arguments!=null&&"25".equals(arguments.getString("upgrade"));upgrade26=arguments!=null&&"26".equals(arguments.getString("upgrade"));start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            if(displacement.equals("upgrade")){upgrade28Flow();result.putString("stream","UPGRADE28 PASS: delivered v0.28 save retained and advanced once.\n");finish(Activity.RESULT_OK,result);return;}
            if(!displacement.isEmpty()){displacementFlow();result.putString("stream","DISPLACEMENT PASS: official map selection, cancellation and execution recorded.\n");finish(Activity.RESULT_OK,result);return;}
            if(!recovery.isEmpty()){recoveryFlow();result.putString("stream","RECOVERY "+recovery+" PASS: UI draft and authoritative save preserved.\n");finish(Activity.RESULT_OK,result);return;}
            if(experience){experienceFlow();result.putString("stream","EXPERIENCE PASS: installed APK observations and official entry flows completed.\n");finish(Activity.RESULT_OK,result);return;}
            if(upgradeOnly||upgrade25||upgrade26||upgrade27){upgradeFlow();result.putString("stream",upgrade27?"UPGRADE27 PASS: exact delivered v0.27 APK replaced in place, v21 fields retained, next turn and recreation execute only once.\n":upgrade26?"UPGRADE26 PASS: actual verified v0.26 APK replaced in place; real v20 three-officer cargo, spent ration, return personnel, district and AI intent retained and v21 continued once.\n":upgrade25?"UPGRADE25 PASS: actual v0.25 APK replaced in place; v19 settings, intent and convoy retained, v21 replay continued once.\n":"UPGRADE PASS: v0.9 APK replaced in place, v8 save retained, loaded, written as v21 and restored identically.\n");finish(Activity.RESULT_OK,result);return;}
            Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity=startActivitySync(launch);waitText("选择剧本",false);
            screenshot("01-scenarios");
            click("区域争雄 ·",false);click("孙权军",true);
            for(int orientation:new int[]{android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}){
                runOnMainSync(()->current.setRequestedOrientation(orientation));assertOrientation(orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                getUiAutomation().waitForIdle(800,5000);waitForIdleSync();
                Rect button=new Rect();waitText("执行",true).getBoundsInScreen(button);android.graphics.Point display=new android.graphics.Point();current.getWindowManager().getDefaultDisplay().getSize(display);
                require(button.left>=0&&button.top>=0&&button.right<=display.x&&button.bottom<=display.y,"open confirmation remains fully reachable after rotation");
            }
            screenshot("v021-rotated-confirmation");click("执行",true);
            waitText("区域争雄  ·  孙权军",false);assertWorld(2,0,"regional-sandbox");
            tacticalDisplacementFlow();
            deploymentWizardFlow();
            experienceRegressions();
            logisticsFlow();
            tacticalLogisticsFlow();
            strategicManagementFlow();
            territoryAiFlow();
            mobileShortcutsFlow();
            unitCommandFlow();
            adaptiveMapFlow();
            modelAtlas();
            clickNav("城市");click("建业 · 孙权军",false);
            waitText("建业",true);screenshot("02-city");
            click("军事",true);click("快速出征（单将）",true);click("甘宁 ·",false);click("弩兵 ·",false);click("确认出征",true);
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
            battleFeedbackFlow();
            diplomacyVisualFlow();
            result.putString("stream","SMOKE PASS: v29 authoritative tactic map preview/cancel/confirm, blocked displacement, own trap, lethal follow, result location/return, fresh next-unit entry, exact hook/breakthrough rejection; deployment wizard back/rotation/recreation/stale inventory rejection; v28 explicit attack/inspect/drag/back, quantity and overlap identity recreation, double/stale confirmation, national return position; v27 map convoy select/stop/route/cancel/field supply/recreation in both orientations; v26 convoy crew/food/return preview/cancel/confirm/recreation, logistics filter/task focus and authorized support; v25 national filters/sort/recreation/focus, batch cancel/execute and saved district settings; v24 territory modes/legend/frontline/recreation, quick delegation/cancel/real administration/save, melee response to ranged attack; v23 native quantity sliders/exact input/cancel/deploy/recreation, map construction/rotation/Lv3, expanded playable world and terrain legend; v22 fixed unit command dock, blank/self/button/back cancellation, movement range, move-then-tactic, facility attack/destruction/save restoration; v21 original portraits/building assets/shared icons, pure diplomacy previews, allied dispatch/task/recreation and whole-force surrender; v20 battle loot/capture/banner/queue-after-kill/haptics-settings and 44 procedural models; v19 portrait/landscape/collapsible panels/hidden navigation/route rotation; v18 sourced-profile preview/cancel/import/relations/recreation; v17 biography edit/cancel/death/succession/recreation/scenario import/200x200 offset viewport; v14 integrated march/ZOC/districts/events/raids/magic/diplomatic-debate/recreation; v12 mediation/treasure/PK-editor/templates/save; v11 fieldwork/36-tech/carry-gold/construction/repair/upgrade; v10 PK research/training/finite uses/skill overwrite/cancel/turn progression/task count/recreation; integrated original game/save/city/task/personnel/combat/army regressions; v9 duel/debate/start/cancel/round/save/settlement, v8 governance/capture/rank/summon, document export/import/cancel/corruption; legacy move preview/cancel/recreation and 神算百出连环; sourced opening, content/search/navigation/save restore and viewport stress verified.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){
            try{screenshot("failure");}catch(Exception ignored){}
            StringWriter trace=new StringWriter();error.printStackTrace(new PrintWriter(trace));
            result.putString("stream","SMOKE FAIL: "+trace+"\n");finish(Activity.RESULT_CANCELED,result);
        }
    }


    // BEGIN DISPLACEMENT OBSERVER: compiles against the delivered v0.28 APIs.
    private void displacementFlow()throws Exception {
        World initial=DisplacementFixture.create("plain");
        try(FileOutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(initial));}
        startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();chooseOrientation("竖屏");
        try(PrintWriter out=new PrintWriter(new File(getTargetContext().getExternalFilesDir(null),"displacement.txt"))){
            out.println("version="+getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).versionName);
            for(String scene:DisplacementFixture.CASES){
                World w=DisplacementFixture.create(scene);World.Unit a=w.unit(1),b=w.unit(2);installFixture(w,a.hex);
                byte[] before=SaveCodec.encode(w);try(FileOutputStream fixture=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),scene+".sg11"))){fixture.write(before);}
                out.println(scene+" actor="+a.hex+" target="+b.hex+" weapon="+a.weapon+" aptitude="+w.army.aptitude(a)+" energy="+a.energy+" acted="+a.acted+" retreat="+new Hex(a.hex.q-1,a.hex.r)+" rear1="+new Hex(b.hex.q+1,b.hex.r)+" rear2="+new Hex(b.hex.q+2,b.hex.r));out.flush();
                click("战法",true);String label=scene.equals("naval-shore")?Army.Tactic.RAM.label:DisplacementFixture.tactic(scene).label;
                click(label+" · 气力",false);waitForIdleSync();
                boolean selectable=find(getUiAutomation().getRootInActiveWindow(),"取消选取",true)!=null;
                out.println("  menu selectable="+selectable+" reason="+displacementText(getUiAutomation().getRootInActiveWindow()));out.flush();
                screenshot("v029-"+scene+"-target");
                if(!selectable){click("返回",true);require(Arrays.equals(before,SaveCodec.encode(w)),"rejected selection is pure: "+scene);continue;}
                tapHex(b.hex);waitText("执行",true);screenshot("v029-"+scene+"-preview");
                out.println("  preview="+displacementText(getUiAutomation().getRootInActiveWindow()));out.flush();
                require(Arrays.equals(before,SaveCodec.encode(w)),"preview is pure: "+scene);click("取消",true);
                require(Arrays.equals(before,SaveCodec.encode(w)),"cancel is pure: "+scene);
                tapHex(b.hex);click("执行",true);waitForIdleSync();
                out.println("  result actor="+(w.unit(1)==null?"destroyed":w.unit(1).hex)+" target="+(w.unit(2)==null?"destroyed":w.unit(2).hex+" troops="+w.unit(2).troops)+" energy="+a.energy+" log="+String.join(" | ",w.log));out.flush();
                screenshot("v029-"+scene+"-result");
                // The old runtime's illegal difficult-terrain movement is recorded, not hidden by validation.
                try{SaveCodec.validate(w);out.println("  state valid");}catch(Exception invalid){out.println("  INVALID: "+invalid.getMessage());}out.flush();
            }
        }
    }
    private String displacementText(AccessibilityNodeInfo node){
        if(node==null)return "";StringBuilder out=new StringBuilder();if(node.getText()!=null)out.append(node.getText()).append(" | ");
        for(int i=0;i<node.getChildCount();i++)out.append(displacementText(node.getChild(i)));return out.toString();
    }
    // END DISPLACEMENT OBSERVER

    private void upgrade28Flow()throws Exception {
        byte[] expected=java.nio.file.Files.readAllBytes(new File(getTargetContext().getExternalFilesDir(null),"upgrade-v028.sg11").toPath());
        World old=SaveCodec.decode(expected);startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        java.lang.reflect.Field wf=MainActivity.class.getDeclaredField("world");wf.setAccessible(true);require(Arrays.equals(expected,SaveCodec.encode((World)wf.get(current))),"actual old v21 loads unchanged after APK replacement");
        endTurn();require(saved().turn==old.turn+1,"v028 upgrade advances exactly one full turn");byte[] advanced=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(advanced,SaveCodec.encode(saved())),"recreation after upgrade cannot repeat action or AI");screenshot("v029-upgrade-v028");
    }
    private void tacticalDisplacementFlow()throws Exception {
        for(String orientation:new String[]{"竖屏","横屏"}){
            for(String scene:new String[]{"mountain","own-seed","second-block","naval-shore","kill"}){
                World w=DisplacementFixture.create(scene);installFixture(w,w.unit(1).hex);chooseOrientation(orientation);byte[] before=SaveCodec.encode(w);
                click("战法",true);String label=scene.equals("naval-shore")?Army.Tactic.RAM.label:DisplacementFixture.tactic(scene).label;click(label+" · 气力",false);tapHex(w.unit(2).hex);waitText("执行",true);
                java.lang.reflect.Field field=MainActivity.class.getDeclaredField("tacticPreview");field.setAccessible(true);Displacement.Preview plan=(Displacement.Preview)field.get(current);
                require(plan!=null&&plan.valid(),"native preview uses formal plan");require(Arrays.equals(before,SaveCodec.encode(w)),"native preview cannot consume RNG/energy");
                screenshot("v029-native-"+scene+"-preview-"+orientation);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(w)),"native cancellation is pure");
                tapHex(w.unit(2).hex);click("执行",true);require(w.unit(1).acted,"native confirmation ends real action");
                if(scene.equals("mountain"))require(w.unit(2).hex.equals(new Hex(6,6))&&w.unit(2).troops<8000,"blocked thrust still damages without invalid movement");
                if(scene.equals("own-seed"))require(w.war.at(new Hex(7,6))==null&&w.unit(2).hex.equals(new Hex(7,6)),"own trap really triggered");
                if(scene.equals("second-block"))require(w.unit(2).hex.equals(new Hex(7,6)),"second blocked tile never skipped");
                if(scene.equals("kill")){require(w.unit(2)==null&&w.unit(1).hex.equals(new Hex(6,6)),"lethal charge follows once");click("击破战果",false);click("定位发生地点",true);waitText("历史战果地点",false);screenshot("v029-result-location-"+orientation);click("返回定位前",true);}
                java.lang.reflect.Field moving=MainActivity.class.getDeclaredField("moving");moving.setAccessible(true);require(moving.getInt(current)==1,"battle preserves selected attacker identity");
                screenshot("v029-native-"+scene+"-result-"+orientation);SaveCodec.validate(w);
            }
            for(String scene:new String[]{"hook-retreat","break-block"}){
                World w=scene.equals("break-block")?DisplacementFixture.create("mountain",World.Weapon.CAVALRY):DisplacementFixture.create(scene);installFixture(w,w.unit(1).hex);byte[] before=SaveCodec.encode(w);
                click("战法",true);click((scene.equals("break-block")?"突破":"熊手")+" · 气力",false);waitText(scene.equals("break-block")?"身后落点":"己方退路",false);screenshot("v029-native-"+scene+"-"+orientation);click("返回",true);require(Arrays.equals(before,SaveCodec.encode(w)),"unavailable tactic explains exact cause without cost");
            }
            World clear=DisplacementFixture.create("plain");clear.unit(2).troops=1;DisplacementFixture.unit(clear,3,0,World.Weapon.SPEAR,new Hex(7,6),4000);installFixture(clear,clear.unit(1).hex);
            click("战法",true);click("突刺 · 气力",false);tapHex(new Hex(6,6));click("执行",true);click("下一个待行动部队",true);click("行军",true);tapHex(new Hex(6,6));click("开始行军",true);
            require(clear.unit(3).hex.equals(new Hex(6,6))&&clear.unit(1).acted,"fresh next unit can move onto newly cleared tile without resetting attacker");
        }
        installFixture(ScenarioCatalog.load("regional-sandbox",2),new Hex(1,1));
    }
    private void deploymentWizardFlow()throws Exception {
        World w=logisticsFixture();for(int id=30;id<46;id++)w.officers.add(new World.Officer(id,"候选"+id,0,11,70,70,70,70,70));w.officer(4).acted=true;
        installFixture(w,w.city(11).hex);chooseOrientation("竖屏");byte[] before=SaveCodec.encode(w);click("出征",true);setInput("出征武将搜索","将5");
        runOnMainSync(()->current.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));assertOrientation(false);waitText("编队 · 选择主将",true);
        require("将5".contentEquals(findInput(getUiAutomation().getRootInActiveWindow(),"出征武将搜索").getText()),"sensor rotation preserves wizard query and step");
        if(recovery.equals("check")){
            AccessibilityNodeInfo search=findInput(getUiAutomation().getRootInActiveWindow(),"出征武将搜索");search.performAction(AccessibilityNodeInfo.ACTION_FOCUS);search.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Rect keyboard=new Rect();for(int attempt=0;attempt<20&&keyboard.isEmpty();attempt++){for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows())if(window.getType()==android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD)window.getBoundsInScreen(keyboard);if(keyboard.isEmpty())SystemClock.sleep(150);}
            require(!keyboard.isEmpty(),"wizard search uses actual keyboard");Rect button=new Rect();waitText("继续",true).getBoundsInScreen(button);require(button.height()>=current.getResources().getDisplayMetrics().density*40&&button.bottom<=keyboard.top,"wizard action remains above keyboard at enlarged font");screenshot("v029-wizard-font-keyboard");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();
        }
        screenshot("v029-wizard-landscape");runOnMainSync(()->current.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));assertOrientation(true);click("将5 ·",false);click("清空副将",true);click("将6 ·",false);click("下一步",true);click("枪兵 ·",false);click("走舸 ·",false);
        setInput("兵力数量","2500");setInput("粮食数量","7654");setInput("金钱数量","321");click("上一步",true);click("上一步",true);click("上一步",true);
        require(waitText("将6 ·",false).isChecked(),"back from quantity retains chosen deputy");click("上一步",true);
        require("将5".contentEquals(findInput(getUiAutomation().getRootInActiveWindow(),"出征武将搜索").getText()),"leader query retained when going back");
        runOnMainSync(current::recreate);waitText("编队 · 选择主将",true);require("将5".contentEquals(findInput(getUiAutomation().getRootInActiveWindow(),"出征武将搜索").getText()),"recreation restores leader step/query");click("将5 ·",false);
        runOnMainSync(current::recreate);waitText("副将 ·",false);require(waitText("将6 ·",false).isChecked(),"recreation restores deputy selection");click("下一步",true);
        runOnMainSync(current::recreate);waitText("陆战兵装",true);click("枪兵 ·",false);runOnMainSync(current::recreate);waitText("携带舰船",true);click("走舸 ·",false);
        require("7654".contentEquals(findInput(getUiAutomation().getRootInActiveWindow(),"粮食数量").getText()),"back/recreation preserve raw quantity");require(Arrays.equals(before,SaveCodec.encode(saved())),"all wizard navigation/recreation remains pure");screenshot("v029-wizard-restored");
        // State changes while the unsubmitted form is open. The real confirm must reject the stale snapshot.
        java.lang.reflect.Field wf=MainActivity.class.getDeclaredField("world");wf.setAccessible(true);World live=(World)wf.get(current);runOnMainSync(()->live.city(11).food-=100);click("确认出征",true);waitText("库存或人物状态已变化",true);require(live.units.isEmpty(),"stale quantity cannot silently deploy");click("返回",true);
        screenshot("v029-wizard-revalidated");click("确认出征",true);require(live.units.size()==1&&live.unit(1).troops==2500&&live.unit(1).food==7654&&live.unit(1).deputies.length==1&&live.unit(1).deputies[0]==6,"retained choices create exactly one real deployment");
        installFixture(ScenarioCatalog.load("regional-sandbox",2),new Hex(1,1));
    }

    private void recoveryFlow()throws Exception {
        if(recovery.equals("prepare")){
            World w=logisticsFixture();try(FileOutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(w));}
            startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();installFixture(w,w.city(11).hex);chooseOrientation("竖屏");
            locateCity("后方");click("调动",true);click("资源运输",true);click("前方 ·",false);click("将4 ·",false);scrollToText("运输粮 · 可选",false);setInput("粮（上限200000）","7654");
            byte[] before=SaveCodec.encode(saved());try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"cold-world.sg11"))){out.write(before);}
            screenshot("v028-cold-before");require(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME),"system HOME action accepted");waitForIdleSync();SystemClock.sleep(500);
            require(!current.hasWindowFocus(),"HOME really puts the game in background before force-stop");
            require(!current.getPreferences(0).getString("clientState","").isEmpty(),"background pause persists UI hints before process termination");
            java.lang.reflect.Method hintsReader=MainActivity.class.getDeclaredMethod("readClientState");hintsReader.setAccessible(true);Bundle hints=(Bundle)hintsReader.invoke(current);Bundle form=hints==null?null:hints.getBundle("formDraft");
            require(form!=null&&form.getBoolean("open")&&"7654".equals(form.getStringArray("amounts")[1]),"persisted background hints match the actual unsubmitted cargo before force-stop");return;
        }
        startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitText("运输数量",true);scrollToText("运输粮 · 可选",false);
        require("7654".contentEquals(findInput(getUiAutomation().getRootInActiveWindow(),"粮（上限200000）").getText()),"force-stop and new process restore raw unsubmitted cargo");
        byte[] expected=java.nio.file.Files.readAllBytes(new File(getTargetContext().getExternalFilesDir(null),"cold-world.sg11").toPath());require(Arrays.equals(expected,SaveCodec.encode(saved())),"cold restore cannot spend goods or RNG");
        screenshot("v028-cold-after");
        android.accessibilityservice.AccessibilityServiceInfo service=getUiAutomation().getServiceInfo();service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(service);
        for(int orientation:new int[]{android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}){
            runOnMainSync(()->current.setRequestedOrientation(orientation));assertOrientation(orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);waitText("运输数量",true);scrollToText("运输粮 · 可选",false);
            AccessibilityNodeInfo input=findInput(getUiAutomation().getRootInActiveWindow(),"粮（上限200000）");input.performAction(AccessibilityNodeInfo.ACTION_FOCUS);input.performAction(AccessibilityNodeInfo.ACTION_CLICK);SystemClock.sleep(600);
            Rect keyboard=new Rect();for(int attempt=0;attempt<20&&keyboard.isEmpty();attempt++){for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows())if(window.getType()==android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD)window.getBoundsInScreen(keyboard);if(keyboard.isEmpty())SystemClock.sleep(150);}
            require(!keyboard.isEmpty(),"actual system keyboard is visible for keyboard overlap verification");
            Rect button=new Rect();waitText("发送",true).getBoundsInScreen(button);require(button.height()>=current.getResources().getDisplayMetrics().density*40&&button.bottom<=keyboard.top,"send remains visible above actual keyboard at enlarged font");
            screenshot("v028-font-keyboard-"+orientation);sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();
        }
        click("取消",true);require(Arrays.equals(expected,SaveCodec.encode(saved())),"keyboard/rotation/cancel preserve world");
        runOnMainSync(current::recreate);waitForIdleSync();require(find(getUiAutomation().getRootInActiveWindow(),"运输数量",true)==null,"cancelled draft does not reopen automatically");
        deploymentWizardFlow(); // Also exercise the new wizard under the real 1.3x-font/IME gate.
    }

    private void experienceFlow()throws Exception {
        World initial=ScenarioCatalog.load("regional-sandbox",2);
        try(FileOutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(initial));}
        startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        try(PrintWriter out=new PrintWriter(new File(getTargetContext().getExternalFilesDir(null),"experience.txt"))){
            out.println("version="+getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).versionName);
            for(String orientation:new String[]{"竖屏","横屏"}){
                World w=logisticsFixture();
                for(int i=1;i<=3;i++){World.Unit u=new World.Unit(i,0,3+i,World.Weapon.SPEAR,new Hex(8+i,15),4000,10000);w.units.add(u);w.officer(3+i).cityId=-1;w.officer(3+i).unitId=i;}w.nextUnitId=4;w.unit(2).acted=true;
                installFixture(w,w.unit(1).hex);chooseOrientation(orientation);click("收起",true);
                MapView map=mapView();MapCamera cam=camera(map);dragMap(map,-100,0);
                float cx=cam.centerX(),cy=cam.centerY();screenshot("v028-before-select-"+orientation);tapHex(w.unit(3).hex);
                require(Math.abs(cam.centerX()-cx)<.1&&Math.abs(cam.centerY()-cy)<.1,"ordinary selection preserves camera center");
                out.println(orientation+" select camera delta="+(cam.centerX()-cx)+","+(cam.centerY()-cy)+" map="+map.getWidth()+"x"+map.getHeight());screenshot("v028-after-select-"+orientation);
                installFixture(w,w.unit(1).hex);click("下一个待行动部队",true);
                java.lang.reflect.Field moving=MainActivity.class.getDeclaredField("moving");moving.setAccessible(true);
                require(moving.getInt(current)==3,"ready cycle skips spent unit2");
                out.println(orientation+" next selected="+moving.getInt(current)+" acted="+w.unit(moving.getInt(current)).acted);
                installFixture(w,w.unit(2).hex);click("攻击",true);waitText("这支部队本旬已行动",false);screenshot("v028-command-reason-"+orientation);click("返回",true);
                AccessibilityNodeInfo attack=waitText("攻击",true);out.println(orientation+" attack enabled="+attack.isEnabled());screenshot("v028-acted-command-"+orientation);
                World nation=ScenarioCatalog.load("heroes-mobile-sandbox",0);installFixture(nation,nation.home().hex);territoryMode("势力范围 · 同势力合并");click("全图",true);out.println(orientation+" full-map scale="+camera(mapView()).scale+" min="+camera(mapView()).minScale);require(Math.abs(camera(mapView()).scale-camera(mapView()).minScale)<.001f,"explicit full map reaches national scale");screenshot("v028-national-"+orientation);
                territoryMode("关闭领地着色");
            }
            World nation=ScenarioCatalog.load("heroes-mobile-sandbox",0);installFixture(nation,nation.home().hex);benchmarkExperience(out,nation,"42cities-670officers");
            World large=new World(200,200,"我军","敌军");large.scenarioId="experience-stress";
            for(int i=0;i<42;i++)large.cities.add(new World.City(i,"城"+i,new Hex(8+(i%7)*27,8+(i/7)*30),i%2));
            for(int i=0;i<670;i++)large.officers.add(new World.Officer(i,"将"+i,(i%42)%2,i%42,80,80,80,80,80));
            for(int i=0;i<40;i++){World.City c=large.city(i);World.Unit u=new World.Unit(i+1,c.owner,i,World.Weapon.SPEAR,new Hex(c.hex.q+1,c.hex.r),4000,10000);large.units.add(u);large.officer(i).cityId=-1;large.officer(i).unitId=u.id;}large.nextUnitId=41;
            for(int i:new int[]{0,2,4}){World.City c=large.city(i),d=large.city(i+2);c.food=100000;c.troops=30000;require(large.domestic.transport(c.id,d.id,i+42,new int[0],0,5000,1000,new int[World.Weapon.values().length],false,false).ok,"stress actual dispatch");}
            installFixture(large,large.unit(1).hex);benchmarkExperience(out,large,"200x200-42cities-40units-3transports");
        }
        installFixture(initial,initial.home().hex);
        experienceRegressions();
        mobileShortcutsFlow();unitCommandFlow();logisticsFlow();tacticalLogisticsFlow();strategicManagementFlow();armyFlow();
    }

    private void experienceRegressions()throws Exception {
        World original=saved();
        for(String orientation:new String[]{"竖屏","横屏"}){
            World w=logisticsFixture();World.Unit a=new World.Unit(1,0,4,World.Weapon.SPEAR,new Hex(8,15),5000,10000),b=new World.Unit(2,1,20,World.Weapon.SPEAR,new Hex(9,15),5000,10000);
            World.Officer enemy=new World.Officer(20,"敌将",1,-1,80,80,80,80,80);enemy.unitId=2;w.officers.add(enemy);w.officer(4).cityId=-1;w.officer(4).unitId=1;w.units.add(a);w.units.add(b);w.nextUnitId=3;
            installFixture(w,a.hex);chooseOrientation(orientation);byte[] before=SaveCodec.encode(saved());
            tapHex(b.hex);waitText("敌将",true);require(find(getUiAutomation().getRootInActiveWindow(),"执行",true)==null,"inspecting enemy cannot open attack confirmation");
            tapHex(a.hex);click("攻击",true);tapHex(b.hex);screenshot("v028-explicit-attack-"+orientation);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"cancel explicit attack is pure");
            click("行军",true);dragMap(mapView(),60,30);SystemClock.sleep(400);require(find(getUiAutomation().getRootInActiveWindow(),"开始行军",true)==null,"drag release never selects command target");
            runOnMainSync(current::onBackPressed);waitForIdleSync();runOnMainSync(current::onBackPressed);waitForIdleSync();runOnMainSync(current::onBackPressed);waitForIdleSync();
            require(Arrays.equals(before,SaveCodec.encode(saved())),"back layers never change game");
            if(find(getUiAutomation().getRootInActiveWindow(),"退出游戏？",false)!=null)click("取消",true);
            installFixture(SaveCodec.decode(before),a.hex);click("攻击",true);tapHex(b.hex);
            java.lang.reflect.Field dialogField=MainActivity.class.getDeclaredField("confirmationDialog");dialogField.setAccessible(true);
            AlertDialog repeated=(AlertDialog)dialogField.get(current);World once=SaveCodec.decode(before);require(once.attack(1,2).ok,"one formal expected attack");
            runOnMainSync(()->{repeated.getButton(AlertDialog.BUTTON_POSITIVE).performClick();repeated.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});waitForIdleSync();
            require(Arrays.equals(SaveCodec.encode(once),SaveCodec.encode(saved())),"queued double confirmation executes exactly one formal attack including RNG");
            installFixture(SaveCodec.decode(before),a.hex);click("攻击",true);tapHex(b.hex);AlertDialog stale=(AlertDialog)dialogField.get(current);
            installFixture(SaveCodec.decode(before),a.hex);runOnMainSync(()->stale.getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitText("局面已变化",false);click("返回",true);
            require(Arrays.equals(before,SaveCodec.encode(saved())),"old world confirmation cannot mutate replacement world");
            // Keep raw unsubmitted quantities and crew through Activity recreation, not just rotation.
            installFixture(ScenarioCatalog.load("regional-sandbox",2),new Hex(18,10));locateCity("建业");click("军事",true);click("快速出征（单将）",true);click("甘宁 ·",false);click("弩兵 ·",false);setInput("兵力数量","2345");
            before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitForIdleSync();waitText("确认出征",true);require("2345".contentEquals(findInput(getUiAutomation().getRootInActiveWindow(),"兵力数量").getText()),"deploy draft quantity survives recreation");
            screenshot("v028-deploy-draft-"+orientation);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"draft restore cannot deploy automatically");
            w=logisticsFixture();installFixture(w,w.city(11).hex);locateCity("后方");click("运输",true);click("前方 ·",false);click("将4 ·",false);scrollToText("运输粮 · 可选",false);setInput("粮（上限200000）","6789");
            before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitForIdleSync();waitText("运输数量",true);scrollToText("运输粮 · 可选",false);require("6789".contentEquals(findInput(getUiAutomation().getRootInActiveWindow(),"粮（上限200000）").getText()),"cargo draft survives recreation");screenshot("v028-cargo-draft-"+orientation);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"restored cargo is still unsubmitted");
            // Legacy overlap is an actual permitted v21 state; resolve each object by stable ID.
            w=logisticsFixture();require(w.domestic.transport(11,12,4,new int[0],0,5000,1000,new int[World.Weapon.values().length],false,false).ok,"first stacked convoy dispatch");
            require(w.domestic.transport(11,12,5,new int[0],0,5000,1000,new int[World.Weapon.values().length],false,false).ok,"second stacked convoy dispatch");
            installFixture(w,w.city(11).hex);tapHex(w.city(11).hex);waitText("同格对象",false);screenshot("v028-overlap-picker-"+orientation);click("将5 ·",false);waitText("将5运输队",true);
            java.lang.reflect.Field moving=MainActivity.class.getDeclaredField("moving");moving.setAccessible(true);require(moving.getInt(current)==w.domestic.missions.get(1).id,"second overlapping convoy is selectable by stable ID");
            runOnMainSync(current::recreate);waitForIdleSync();waitText("将5运输队",true);require(moving.getInt(current)==w.domestic.missions.get(1).id,"overlap identity survives recreation");
        }
        nationalReturnPosition();
        // Compact object sheets must not leave a management list with zero height under its filters.
        World officers=diplomacyFixture();installFixture(officers,officers.home().hex);chooseOrientation("竖屏");clickNav("武将");setSearch("诸葛亮");
        android.widget.ListView officerList=nativeList(current.getWindow().getDecorView());require(officerList!=null&&officerList.getHeight()>=current.getResources().getDisplayMetrics().density*80,"default officer list retains a usable row below search and filters without manual expansion");
        screenshot("v028-officer-list-default");click("诸葛亮 · 汉营",true);waitText("诸葛亮",true);click("返回",true);
        installFixture(original,original.home().hex);
    }
    private android.widget.ListView nativeList(android.view.View view){
        if(view instanceof android.widget.ListView)return (android.widget.ListView)view;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){android.widget.ListView found=nativeList(group.getChildAt(i));if(found!=null)return found;}}return null;
    }
    private void nationalReturnPosition()throws Exception {
        World nation=ScenarioCatalog.load("heroes-mobile-sandbox",0);installFixture(nation,nation.home().hex);chooseOrientation("竖屏");allCityStates();clickNav("城市");click("展开",true);
        for(int i=0;i<3;i++){require(scroll(getUiAutomation().getRootInActiveWindow()),"national list scrolls");waitForIdleSync();SystemClock.sleep(250);}
        android.widget.ListView list=nativeList(current.getWindow().getDecorView());require(list!=null,"native national list visible");
        int[] location=new int[2];String[] label=new String[1];runOnMainSync(()->{location[0]=list.getFirstVisiblePosition();location[1]=list.getChildAt(0).getTop();World.City city=(World.City)list.getItemAtPosition(location[0]+1);label[0]=city.name+" · "+nation.faction(city.owner);});
        require(location[0]>0,"national list genuinely left first row");require(find(getUiAutomation().getRootInActiveWindow(),label[0],true)!=null,"test taps an already visible row without scrolling to find it");byte[] before=SaveCodec.encode(saved());screenshot("v028-national-position-before");click(label[0],true);click("返回全国列表",true);waitForIdleSync();
        android.widget.ListView restored=nativeList(current.getWindow().getDecorView());require(restored!=null,"single action returns to national list");
        int[] actual=new int[2];runOnMainSync(()->{actual[0]=restored.getFirstVisiblePosition();actual[1]=restored.getChildAt(0).getTop();});screenshot("v028-national-position-after");
        require(actual[0]==location[0]&&Math.abs(actual[1]-location[1])<=1,"national list preserves first row and pixel offset: "+Arrays.toString(location)+" -> "+Arrays.toString(actual));
        require(Arrays.equals(before,SaveCodec.encode(saved())),"list map round trip is pure");
    }

    private void benchmarkExperience(PrintWriter out,World w,String label)throws Exception {
        MapView map=mapView();long[][] samples=new long[4][12];
        runOnMainSync(()->{map.focus(w.home().hex);Bitmap bitmap=Bitmap.createBitmap(map.getWidth(),map.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);
            for(int i=0;i<15;i++){long t=System.nanoTime();map.setWorld(w,w.home().hex,w.units.isEmpty()?-1:w.units.get(0).id);long selected=System.nanoTime()-t;t=System.nanoTime();map.draw(canvas);long draw=System.nanoTime()-t;t=System.nanoTime();((MainActivity)current).refresh();long refresh=System.nanoTime()-t;t=System.nanoTime();UiModels.cities(w,0,"",-1);long list=System.nanoTime()-t;if(i>=3){samples[0][i-3]=selected;samples[1][i-3]=draw;samples[2][i-3]=refresh;samples[3][i-3]=list;}}bitmap.recycle();});
        for(int i=0;i<samples.length;i++){Arrays.sort(samples[i]);out.println(label+" "+new String[]{"setWorld-range-index","softwareDraw","refresh-detail","city-list"}[i]+" medianMs="+samples[i][6]/1e6+" p95Ms="+samples[i][11]/1e6);}
        out.println(label+" nativeViewport="+map.getWidth()+"x"+map.getHeight()+" tilesVisited="+map.tilesVisited());
        long[] drag=new long[12],zoom=new long[12];
        for(int i=0;i<12;i++){long t=System.nanoTime();dragMap(map,i%2==0?80:-80,0);drag[i]=System.nanoTime()-t;t=System.nanoTime();timedPinch(map,i%2==0?1.15f:1/1.15f);zoom[i]=System.nanoTime()-t;}
        Arrays.sort(drag);Arrays.sort(zoom);out.println(label+" injected-drag-to-idle medianMs="+drag[6]/1e6+" p95Ms="+drag[11]/1e6);out.println(label+" injected-pinch-to-idle medianMs="+zoom[6]/1e6+" p95Ms="+zoom[11]/1e6);
        byte[] bytes=SaveCodec.encode(w);for(int i=0;i<3;i++){World copy=SaveCodec.decode(bytes);long t=System.nanoTime();copy.nextTurn();out.println(label+" actual-nextTurn-ms="+(System.nanoTime()-t)/1e6);}
        out.flush();
    }

    private void timedPinch(MapView map,float factor){
        int[] pos=new int[2];runOnMainSync(()->map.getLocationOnScreen(pos));float cx=pos[0]+map.getWidth()/2f,cy=pos[1]+map.getHeight()/2f,span=map.getWidth()*.30f;long down=SystemClock.uptimeMillis();
        MotionEvent.PointerProperties[] properties={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};MotionEvent.PointerCoords[] coords={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};
        for(int i=0;i<2;i++){properties[i].id=i;properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i].pressure=1;coords[i].size=1;coords[i].y=cy;}
        for(int step=0;step<=10;step++){float d=span*(1+(factor-1)*Math.min(step,8)/8f);coords[0].x=cx-d;coords[1].x=cx+d;int action=step==0?MotionEvent.ACTION_DOWN:step==1?MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT):step==9?MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT):step==10?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;
            MotionEvent e=MotionEvent.obtain(down,down+step*16,action,step==0||step==10?1:2,properties,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);sendPointerSync(e);e.recycle();}
        waitForIdleSync();
    }

    private void strategicManagementFlow()throws Exception {
        byte[] original=SaveCodec.encode(saved());World w=ScenarioCatalog.load("world-drill",0);w.city(11).food=0;installFixture(w,w.city(11).hex);
        for(String orientation:new String[]{"竖屏","横屏"}){
            chooseOrientation(orientation);byte[] before=SaveCodec.encode(saved());
            click("视图",true);click("全国城池总览",true);click("筛选 · 全部状态",true);click("城池状态",true);click("缺粮（不足6旬）",true);
            waitText("北境城 · 经略营",true);screenshot("v025-national-food-"+orientation);
            runOnMainSync(current::recreate);waitForIdleSync();waitText("筛选 · 缺粮（不足6旬）",true);
            click("排序 · 己方优先",true);click("粮食续航最少",true);waitText("北境城 · 经略营",true);
            click("北境城 · 经略营",true);waitText("北境城",true);click("返回全国列表",true);waitText("筛选 · 缺粮（不足6旬）",true);waitText("排序 · 粮食续航最少",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"national filters sort restore and focus are read-only");
            click("视图",true);click("全国城池总览",true);click("筛选 · 缺粮（不足6旬）",true);click("城池状态",true);click("全部状态",true);
            click("排序 · 粮食续航最少",true);click("己方优先",true);
            click("筛选 · 全部状态",true);click("批量划入军团",true);click("北境城",true);click("预览",true);click("新建内政军团",true);waitText("批量执行预览",true);screenshot("v025-batch-preview-"+orientation);click("取消",true);
            require(Arrays.equals(before,SaveCodec.encode(saved())),"batch preview cancellation leaves full game unchanged");
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();
        }
        click("视图",true);click("全国城池总览",true);click("筛选 · 全部状态",true);click("批量划入军团",true);click("北境城",true);click("预览",true);click("新建内政军团",true);click("执行",true);
        require(saved().districts.city(11)!=null&&saved().actionPoints[0]==40,"batch UI executes once with real budget");
        locateCity("北境城");click("城池托管 / 军团",true);click("经营设置",true);setInput("留守兵力","15000");setInput("保留金","6000");setInput("保留粮","50000");click("预览设置",true);click("执行",true);click("返回",true);
        runOnMainSync(current::recreate);waitForIdleSync();require(saved().districts.city(11).reserveTroops()==15000&&saved().districts.city(11).reserveFood()==50000&&saved().actionPoints[0]==20,"native settings saved and charged exactly once");
        locateCity("北境城");click("城池托管 / 军团",true);waitText("最近经营报告",false);screenshot("v025-district-report");click("返回",true);
        World restore=SaveCodec.decode(original);installFixture(restore,restore.home().hex);
    }
    private void territoryMode(String mode){click("视图",true);click("领地着色 / 前线",true);click(mode,false);}
    private void territoryAiFlow()throws Exception {
        byte[] original=SaveCodec.encode(saved());
        World nation=ScenarioCatalog.load("heroes-mobile-sandbox",0);installFixture(nation,nation.home().hex);
        byte[] before=SaveCodec.encode(saved());
        for(String orientation:new String[]{"竖屏","横屏"}){
            chooseOrientation(orientation);territoryMode("势力范围 · 同势力合并");click("全图",true);
            require(mapView().territoryMode()==1,"faction territory switch is active");screenshot("v024-faction-territory-"+orientation);
            territoryMode("据点辖区 · 城 / 关 / 港边界");
            require(mapView().territoryMode()==2,"per-site territory switch is active");screenshot("v024-site-territory-"+orientation);
            runOnMainSync(current::recreate);waitForIdleSync();require(mapView().territoryMode()==2,"territory preference survives recreation");
            click("视图",true);click("势力领地图例",true);waitText("前线",false);screenshot("v024-territory-legend-"+orientation);click("返回",true);
        }
        require(Arrays.equals(before,SaveCodec.encode(saved())),"territory tools never mutate game state");
        click("视图",true);click("领地着色 / 前线",true);click("前线据点",true);
        World.City front=nation.cities.stream().filter(c->new Territory(nation).frontline(c.id)).findFirst().get();
        click(front.name+" · "+nation.faction(front.owner),true);waitText(front.name,true);screenshot("v024-frontline-focus");
        territoryMode("关闭领地着色");require(mapView().territoryMode()==0,"overlay can be disabled");
        World w=ScenarioCatalog.load("world-drill",0);int mainAp=w.actionPoints[0];installFixture(w,w.city(11).hex);locateCity("北境城");
        click("城池托管 / 军团",true);click("快速托管内政（新军团）",true);before=SaveCodec.encode(saved());screenshot("v024-quick-delegate-preview");click("取消",true);
        require(Arrays.equals(before,SaveCodec.encode(saved())),"cancel quick delegation leaves resources unchanged");
        click("城池托管 / 军团",true);click("快速托管内政（新军团）",true);click("执行",true);
        require(saved().districts.city(11)!=null&&saved().actionPoints[0]==mainAp-20,"quick city action creates a real district and pays AP");
        endTurn();waitForTurn(1);endTurn();waitForTurn(2);
        require(saved().domestic.count(11)>0,"quick-delegated city is really developed by the AI");
        locateCity("北境城");click("城池托管 / 军团",true);waitText("军团行动力：",false);screenshot("v024-district-status");click("返回",true);
        runOnMainSync(current::recreate);waitForIdleSync();require(saved().districts.city(11)!=null,"delegated city survives recreation");
        // Real installed turn: opponent is shot at two tiles, then must close and act.
        World battle=new World(30,20,"玩家","电脑");
        battle.cities.add(new World.City(10,"我城",new Hex(2,3),0));battle.cities.add(new World.City(20,"敌城",new Hex(26,3),1));
        for(int id:new int[]{0,1,20,21})battle.officers.add(new World.Officer(id,"将"+id,id<20?0:1,id<20?10:20,80,80,80,80,80));
        battle.officer(0).role=Strategy.Role.RULER;battle.officer(20).role=Strategy.Role.RULER;
        battle.officer(0).loyalty=100;battle.officer(20).loyalty=100;
        World.Unit bow=new World.Unit(1,0,1,World.Weapon.CROSSBOW,new Hex(12,10),6000,20000),enemy=new World.Unit(2,1,21,World.Weapon.SPEAR,new Hex(10,10),6000,20000);
        for(World.Unit u:new World.Unit[]{bow,enemy}){battle.officer(u.officerId).cityId=-1;battle.officer(u.officerId).unitId=u.id;battle.units.add(u);}battle.nextUnitId=3;
        require(battle.attack(1,2).ok,"initial ranged attack fixture");installFixture(battle,bow.hex);endTurn();waitForTurn(1);
        World result=saved();require(result.unit(2)!=null&&!result.unit(2).hex.equals(new Hex(10,10)),"installed enemy responds by moving after being shot");
        screenshot("v024-ai-closes-on-archer");
        World replacement=new World(20,12,"甲","乙");
        replacement.cities.add(new World.City(10,"重用编号城",new Hex(3,3),0));replacement.cities.add(new World.City(40,"新邻城",new Hex(16,3),1));
        installFixture(replacement,replacement.city(10).hex);
        require(mapView().territory().neighbors(10).contains(40)&&!mapView().territory().neighbors(10).contains(20),"scenario replacement updates territory before city details read it");
        World restore=SaveCodec.decode(original);installFixture(restore,restore.home().hex);
    }
    private void mobileShortcutsFlow()throws Exception {
        byte[] original=SaveCodec.encode(saved());
        for(String orientation:new String[]{"竖屏","横屏"}){
            World w=ScenarioCatalog.load("regional-sandbox",2);installFixture(w,w.city(310).hex);chooseOrientation(orientation);
            locateCity("建业");click("军事",true);click("快速出征（单将）",true);click("甘宁 ·",false);click("弩兵 ·",false);
            byte[] before=SaveCodec.encode(saved());setInput("兵力数量","999");
            require(!waitText("确认出征",true).isEnabled(),"invalid exact quantity disables deployment");
            setInput("兵力数量","2345");
            AccessibilityNodeInfo slider=findInput(getUiAutomation().getRootInActiveWindow(),"兵力滑块");
            require(slider!=null,"native troop slider exists");Bundle amount=new Bundle();amount.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,1456);
            require(slider.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),amount),"native slider accepts drag-equivalent progress");waitForIdleSync();
            require("2456".contentEquals(findInput(getUiAutomation().getRootInActiveWindow(),"兵力数量").getText()),"slider and exact entry are synchronized");
            setInput("兵力数量","2345");scrollToText("粮食 · 可选",false);setInput("粮食数量","6789");scrollToText("金钱 · 可选",false);setInput("金钱数量","4321");
            screenshot("v023-quantities-"+orientation);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"quantity cancellation is pure");
            click("快速出征（单将）",true);click("甘宁 ·",false);click("弩兵 ·",false);setInput("兵力数量","2345");scrollToText("粮食 · 可选",false);setInput("粮食数量","6789");scrollToText("金钱 · 可选",false);setInput("金钱数量","4321");click("确认出征",true);
            World actual=saved();World.Unit u=actual.unit(actual.officer(3003).unitId);
            require(u.troops==2345&&u.food==6789&&u.gold==4321,"actual UI deploys nonpreset quantities");
            World stock=SaveCodec.decode(before);require(actual.city(310).troops==stock.city(310).troops-2345&&actual.city(310).gold==stock.city(310).gold-4321&&actual.city(310).food==stock.city(310).food-6789,"UI debits exact stock");
            byte[] after=SaveCodec.encode(actual);runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(after,SaveCodec.encode(saved())),"quantity deployment never replays after recreation");
        }
        World w=ScenarioCatalog.load("regional-sandbox",2);installFixture(w,w.city(300).hex);chooseOrientation("竖屏");locateCity("柴桑");click("建设",true);click("市场 ·",false);click("周瑜 ·",false);
        byte[] before=SaveCodec.encode(saved());waitText("点选高亮地块",false);require(!waitText("下一旬",false).isEnabled(),"cannot advance time during map placement");
        tapHex(w.city(300).hex);click("返回",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"invalid construction tile does not spend resources");
        screenshot("v023-map-construction-portrait");runOnMainSync(()->current.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));assertOrientation(false);waitText("点选高亮地块",false);
        Hex site=w.domestic.buildSites(300).get(0);tapHex(site);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"map construction confirmation cancels without cost");
        tapHex(site);click("开工",true);require(saved().domestic.at(site).level==3&&saved().domestic.at(site).remaining==2,"map-selected construction starts at Lv3");screenshot("v023-map-construction-landscape");
        World heroes=ScenarioCatalog.load("heroes-mobile-sandbox",0);installFixture(heroes,heroes.home().hex);click("全图",true);screenshot("v023-42-city-map");
        click("视图",true);click("兵种与建筑图例",true);click("地形与通行",true);screenshot("v023-terrain-legend");click("返回",true);
        installFixture(SaveCodec.decode(original),SaveCodec.decode(original).home().hex);
    }
    private void unitCommandFlow()throws Exception {
        World original=saved();
        World w=new World(14,10,"我军","敌军");w.scenarioId="unit-command-smoke";w.scenarioName="指令验证";
        w.cities.add(new World.City(10,"本营",new Hex(2,4),0));w.cities.add(new World.City(20,"敌营",new Hex(10,4),1));
        World.Officer a=new World.Officer(1,"枪将",0,-1,80,80,80,80,80),b=new World.Officer(2,"守将",1,-1,80,80,80,80,80),builder=new World.Officer(3,"建设官",1,20,80,80,80,80,80);
        a.unitId=1;b.unitId=2;a.aptitude[0]=3;w.officers.add(a);w.officers.add(b);w.officers.add(builder);
        World.Unit u=new World.Unit(1,0,1,World.Weapon.SPEAR,new Hex(8,4),5000,10000),enemy=new World.Unit(2,1,2,World.Weapon.SPEAR,new Hex(8,5),5000,10000);
        w.units.add(u);w.units.add(enemy);w.nextUnitId=3;w.active=1;
        require(w.domestic.build(20,3,Domestic.Kind.MARKET,new Hex(9,4)).ok,"fixture builds real facility");
        Domestic.Facility facility=w.domestic.at(new Hex(9,4));facility.remaining=0;facility.builderId=-1;facility.hp=400;w.active=0;
        byte[] initial=SaveCodec.encode(w);installFixture(w,u.hex);
        for(String orientation:new String[]{"竖屏","横屏"}){
            chooseOrientation(orientation);waitText("战法",true);waitText("取消选中",true);
            require(mapView().reachableCount()==w.orders.marchReachable(u).size()&&mapView().reachableCount()>1,"selection exposes actual move range");
            Rect button=new Rect();waitText("战法",true).getBoundsInScreen(button);require(button.width()>0&&button.height()>0,"tactic button is visible without scrolling");
            screenshot("v022-commands-"+orientation);
        }
        click("取消选中",true);require(mapView().reachableCount()==0,"cancel button clears range");
        tapHex(u.hex);tapHex(u.hex);require(mapView().reachableCount()==0,"tap same unit cancels selection");
        tapHex(u.hex);tapHex(new Hex(7,4));require(mapView().reachableCount()==0,"ordinary blank tile tap cancels instead of marching");
        require(Arrays.equals(initial,SaveCodec.encode(saved())),"selection and cancellation never mutate world");
        tapHex(u.hex);click("行军",true);runOnMainSync(current::onBackPressed);waitForIdleSync();waitText("青色为本旬",false);
        runOnMainSync(current::onBackPressed);waitForIdleSync();require(mapView().reachableCount()==0,"back cancels command then selection");
        tapHex(u.hex);click("行军",true);tapHex(new Hex(7,5));waitText("路线预览",false);click("取消",true);require(Arrays.equals(initial,SaveCodec.encode(saved())),"route cancel preserves action and movement");
        click("行军",true);runOnMainSync(current::recreate);waitForIdleSync();waitText("行军：点目标",false);tapHex(new Hex(7,5));click("开始行军",true);
        require(saved().unit(1).hex.equals(new Hex(7,5))&&!saved().unit(1).acted,"explicit movement retains attack action");
        click("战法",true);click("突刺 ·",false);tapHex(saved().unit(2).hex);click("执行",true);
        require(saved().unit(1).acted&&saved().unit(1).energy==65&&mapView().reachableCount()==0,"move then tactic pays energy and clears movement overlay");screenshot("v022-move-then-tactic");
        w=SaveCodec.decode(initial);installFixture(w,w.unit(1).hex);click("行军",true);tapHex(facility.hex);waitText("路线预览 · 市场",true);click("取消",true);require(Arrays.equals(initial,SaveCodec.encode(saved())),"march mode on adjacent facility never attacks");click("攻击",true);tapHex(facility.hex);waitText("耐久 400/1000",false);click("取消",true);require(Arrays.equals(initial,SaveCodec.encode(saved())),"facility attack cancel is pure");
        tapHex(facility.hex);click("执行",true);require(saved().domestic.at(facility.hex).hp==40&&saved().unit(1).acted,"map facility attack damages real facility");
        runOnMainSync(current::recreate);waitForIdleSync();require(saved().domestic.at(facility.hex).hp==40,"facility damage survives recreation");screenshot("v022-facility-damaged");
        w=saved();w.unit(1).acted=false;installFixture(w,w.unit(1).hex);click("攻击",true);tapHex(facility.hex);click("执行",true);
        require(saved().domestic.at(facility.hex)==null,"second attack destroys facility");waitText("地块已释放",false);screenshot("v022-facility-destroyed");
        installFixture(original,original.home().hex);click("收起",true);
    }
    private void adaptiveMapFlow()throws Exception {
        byte[] before=SaveCodec.encode(saved());
        require(find(getUiAutomation().getRootInActiveWindow(),"导航 · 城市",true)==null,"navigation is hidden until requested");
        require(find(getUiAutomation().getRootInActiveWindow(),"全图",true)!=null,"full map has a direct one-tap entry");
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
        click("行军",true);tapHex(w.city(20).hex);waitText("路线预览 · 目标城",true);screenshot("67-march-preview");
        require(Arrays.equals(initial,SaveCodec.encode(saved())),"route preview does not mutate game");
        chooseOrientation("竖屏");assertOrientation(true);waitText("路线预览 · 目标城",true);waitText("开始行军",true);screenshot("95-portrait-route");
        runOnMainSync(current::recreate);waitText("路线预览 · 目标城",true);require(Arrays.equals(initial,SaveCodec.encode(saved())),"pending preview survives rotation without issuing order");
        chooseOrientation("横屏");assertOrientation(false);waitText("路线预览 · 目标城",true);
        click("取消",true);require(Arrays.equals(initial,SaveCodec.encode(saved())),"route cancel has zero cost");
        click("全图",true);click("行军",true);tapHex(w.city(20).hex);click("开始行军",true);w=saved();require(w.unit(1).march!=null&&w.unit(1).movementSpent<=4&&!w.unit(1).acted,"map city tap starts budgeted persistent march");screenshot("68-march-active");
        byte[] issued=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("行军验证",false);require(Arrays.equals(issued,SaveCodec.encode(saved())),"restart does not duplicate march movement");
        Hex previous=saved().unit(1).hex;endTurn();require(!saved().unit(1).hex.equals(previous),"next turn advances selected marching unit");click("选中对象指令 ·",false);waitText("停止行军",true);screenshot("69-march-next-turn");
        clickNav("任务");click("行军 · 行军将",true);waitText("目标 目标城",false);screenshot("70-march-task");click("定位部队",true);
        click("全图",true);click("行军",true);tapHex(new Hex(4,9));click("开始行军",true);require(saved().unit(1).march!=null&&saved().unit(1).march.tile.equals(new Hex(4,9)),"tap new ground retargets queued order");
        click("停止行军",true);require(saved().unit(1).march==null,"stop through fixed command dock");screenshot("71-march-stopped");
        click("全图",true);click("行军",true);tapHex(saved().city(20).hex);click("开始行军",true);int count=0;
        while(saved().unit(1).march!=null&&count++<12)endTurn();
        w=saved();require(w.unit(1).march==null&&w.unit(1).hex.distance(w.city(20).hex)==1&&w.city(20).owner==1&&!w.unit(1).acted,"arrival does not auto attack and retains action");screenshot("72-march-arrived");
        click("全图",true);click("攻击",true);tapHex(w.city(20).hex);click("执行",true);require(saved().city(20).defense<3000&&saved().unit(1).acted,"arrival followed by real siege through target tap");
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
        click("设施开发",true);click("市场 ·",false);click("周瑜 ·",false);tapHex(saved().domestic.buildSites(300).get(0));click("开工",true);waitForIdleSync();
        World w=saved();require(w.domestic.facilities.size()==1&&w.domestic.facilities.get(0).remaining==2&&w.domestic.busy(3001),"construction persisted and occupies builder");
        screenshot("06-construction");
        locateCity("柴桑");click("调动",true);click("人员调动",true);click("建业 ·",false);click("孙权 ·",false);click("出发",true);waitForIdleSync();
        require(saved().domestic.missions.size()==1&&saved().officer(3000).cityId==-1,"officer travel starts through UI");
        locateCity("柴桑");click("调动",true);click("资源运输",true);click("建业 ·",false);click("鲁肃 ·",false);waitText("运输数量",true);screenshot("07-cargo-form");click("发送",true);click("确认发送",true);waitForIdleSync();
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
    private void endTurn(){click("下一旬  →",true);click("执行",true);click("旬结算完成",false);waitText("旬结算摘要",true);click("返回",true);}
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
        click("战法",true);click("突刺 ·",false);tapHex(saved().unit(2).hex);click("取消",true);require(saved().unit(1).energy==80&&saved().unit(2).troops==5000,"cancel tactic has no cost");click("取消选取",true);
        click("战法",true);click("突刺 ·",false);tapHex(saved().unit(2).hex);click("执行",true);w=saved();require(w.unit(1).energy==65&&w.unit(1).acted&&w.unit(2).troops<5000&&w.unit(2).hex.equals(new Hex(8,5)),"UI spear thrust persists damage, action and displacement");screenshot("21-tactical-thrust");
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("战法验证",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"combat outcome survives recreation without duplicate settlement");
        battle=SaveCodec.decode(before);battle.unit(1).acted=false;battle.unit(1).energy=80;battle.strategy.setSeed(0);installFixture(battle,battle.unit(1).hex);
        // Fire has adjacent range in the selected PC rules; move after the push before casting.
        click("行军",true);tapHex(new Hex(7,5));click("开始行军",true);
        require(!saved().unit(1).acted,"moving into fire range retains the plot command");
        click("选中对象指令 ·",false);
        click("部队计略",true);click("火计 ·",false);tapHex(saved().unit(2).hex);click("执行",true);w=saved();require(w.war.fireAt(w.unit(2).hex)!=null&&w.unit(1).energy==70,"UI fire plot persists burning hex");screenshot("22-fire-field");
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
        installFixture(w,a.hex);click("攻击",true);tapHex(b.hex);click("执行",true);waitForIdleSync();
        require(saved().government.captive(2)&&saved().unit(2)==null,"capture skill produces persisted prisoner from actual map attack");
        require(saved().government.prisoner(2).unitId==1&&saved().government.prisoner(2).cityId==-1,"prisoner initially follows victorious field unit");
        World returning=saved();returning.unit(1).hex=new Hex(3,3);returning.unit(1).acted=false;installFixture(returning,returning.unit(1).hex);
        click("入城",true);tapHex(returning.city(0).hex);click("执行",true);
        require(saved().government.prisoner(2).unitId==-1&&saved().government.prisoner(2).cityId==0,"actual enter command delivers prisoner to city");
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
        require(raid.domestic.transport(2,3,2,0,5000,1,new int[4]).ok,"prepare real hostile cargo mission");raid.active=0;raid.officer(1).acted=false;
        require(raid.deploy(0,1,World.Weapon.SPEAR,3000).ok,"redeploy former escort for transport interception");
        World.Unit raider=raid.unit(raid.officer(1).unitId);raider.hex=new Hex(4,4);
        Domestic.Mission convoy=raid.domestic.missions.stream().filter(m->m.officerId==2).findFirst().get();convoy.hex=new Hex(5,4);
        installFixture(raid,raider.hex);click("攻击",true);tapHex(convoy.hex);click("执行",true);
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
    private World logisticsFixture(){
        World w=new World(36,24,"我军","敌军");w.strategy.setSeed(26016);
        w.cities.add(new World.City(10,"主城",new Hex(2,2),0));w.cities.add(new World.City(11,"后方",new Hex(3,15),0));w.cities.add(new World.City(12,"前方",new Hex(15,15),0));w.cities.add(new World.City(20,"敌城",new Hex(32,15),1));
        for(World.City c:w.cities){c.gold=30000;c.food=160000;c.troops=30000;c.morale=100;}
        for(int i=0;i<12;i++)w.officers.add(new World.Officer(i,"将"+i,0,i<3?10:i<9?11:12,80,80,80,80,80));w.officer(0).role=Strategy.Role.RULER;w.officer(0).loyalty=100;return w;
    }
    private void logisticsFlow()throws Exception {
        for(String orientation:new String[]{"竖屏","横屏"}){
            World w=logisticsFixture();installFixture(w,w.city(11).hex);chooseOrientation(orientation);allCityStates();byte[] before=SaveCodec.encode(saved());
            locateCity("后方");click("调动",true);click("资源运输",true);click("前方 ·",false);click("将4 ·",false);
            click("运输副将（最多2名）",true);click("将5",true);click("将6",true);click("将7",true);click("完成",true);click("卸货后武将返回出发城",true);
            setInput("粮（上限200000）","6000");click("发送",true);waitText("派遣费0金",false);screenshot("v026-convoy-preview-"+orientation);click("取消",true);
            require(Arrays.equals(before,SaveCodec.encode(saved())),"new convoy preview and cancel are read only");
            click("发送",true);click("确认发送",true);w=saved();require(w.domestic.missions.size()==1&&w.domestic.missions.get(0).crew().length==3&&w.domestic.missions.get(0).returnOfficers,"three officers and return option actually saved");
            require(w.city(11).gold==30000&&w.city(11).food==154000&&w.actionPoints[0]==50,"new transport charges zero fee and one action");
            byte[] sent=SaveCodec.encode(w);runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(sent,SaveCodec.encode(saved())),"recreation cannot dispatch twice");
            endTurn();waitForTurn(1);require(saved().domestic.missions.get(0).food==5950,"installed convoy really consumes carried food");
            clickNav("任务");click("运输 · 将4",false);click("起点 / 改道",true);click("定位当前位置",true);screenshot("v026-convoy-focus-"+orientation);
            // Pure anomaly filtering retains the underlying convoy and camera/UI state through recreation.
            click("视图",true);click("全国城池总览",true);click("筛选 ·",false);click("城池状态",true);click("有在途援助",true);waitText("前方 · 我军",false);
            before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitText("筛选 · 有在途援助",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"logistics filter/recreation are pure");screenshot("v026-inbound-filter-"+orientation);
            w=saved();for(Hex h:w.domestic.missions.get(0).hex.neighbors())w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;installFixture(w,w.city(11).hex);before=SaveCodec.encode(saved());
            click("视图",true);click("全国城池总览",true);click("筛选 ·",false);click("城池状态",true);click("物流异常",true);waitText("后方 · 我军",false);screenshot("v026-anomaly-filter-"+orientation);click("后方 · 我军",false);
            require(Arrays.equals(before,SaveCodec.encode(saved())),"anomaly filter and city focus cannot change blocked cargo");
            w=logisticsFixture();require(w.districts.configure(-1,"后方军",new int[]{11},Districts.Policy.ECONOMY,-1,12,false,false).ok,"support fixture authorization");
            require(w.nextTurn().ok,"new group obtains next-turn budget");installFixture(w,w.city(11).hex);byte[] pending=SaveCodec.encode(saved());
            click("视图",true);click("军团托管",true);click("后方军 ·",false);click("支援申请 / 可执行预览",true);click("后方",true);waitText("支援执行预览",true);screenshot("v026-support-preview-"+orientation);click("取消",true);
            require(Arrays.equals(pending,SaveCodec.encode(saved())),"support preview cancellation spends no resources or AP");
            click("支援申请 / 可执行预览",true);click("后方",true);click("执行",true);w=saved();require(w.domestic.missions.size()==1&&w.districts.get(1).points()==50,"support sends one actual authorized convoy using district budget");waitText("军团行动力：50 / 60",false);click("返回",true);
        }
        allCityStates();installFixture(ScenarioCatalog.load("regional-sandbox",2),new Hex(18,10));
    }
    private void tacticalLogisticsFlow()throws Exception {
        for(String orientation:new String[]{"竖屏","横屏"}){
            World w=logisticsFixture();w.city(11).equipment[0]=5000;
            require(w.domestic.transport(11,12,4,new int[]{5,6},500,10000,2000,new int[]{1000,0,0,0},true,true).ok,"real city dispatch");
            require(w.nextTurn().ok,"real convoy departure");Domestic.Mission m=w.domestic.missions.get(0);
            // An adjacent friendly unit isolates the phone supply interaction; campaign tests use city deployments.
            World.Unit friend=new World.Unit(1,0,7,World.Weapon.SPEAR,m.hex.neighbors().get(0),3000,6000);w.units.add(friend);w.nextUnitId=2;w.officer(7).cityId=-1;w.officer(7).unitId=1;
            installFixture(w,m.hex);chooseOrientation(orientation);waitText("携兵 2000",false);screenshot("v027-convoy-map-"+orientation);
            byte[] before=SaveCodec.encode(saved());click("全图",true);click("行军",true);tapHex(w.city(12).hex);waitText("路线预览",false);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"transport route cancel is pure");
            click("停止",true);require(saved().domestic.missions.get(0).stopped,"map stop changes actual convoy command");before=SaveCodec.encode(saved());
            runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(before,SaveCodec.encode(saved())),"map convoy rebuild cannot move/refund/consume");
            click("补给",true);tapHex(friend.hex);setInput("运输补给粮","1000");setInput("运输补给金","100");click("预览",true);click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"field supply cancel is pure");
            click("补给",true);tapHex(friend.hex);setInput("运输补给粮","1000");setInput("运输补给金","100");click("预览",true);click("执行",true);w=saved();m=w.domestic.missions.get(0);
            require(m.food==8900&&m.gold==400&&m.acted&&w.unit(1).food==7000&&w.unit(1).gold==100,"installed field supply changes single cargo owner and real recipient once");screenshot("v027-field-supply-"+orientation);
            before=SaveCodec.encode(w);runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(before,SaveCodec.encode(saved())),"supply rebuild cannot repeat");
        }
        installFixture(ScenarioCatalog.load("regional-sandbox",2),new Hex(18,10));
    }

    private void allCityStates(){click("视图",true);click("全国城池总览",true);click("筛选 ·",false);click("城池状态",true);click("全部状态",true);}
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
        try(DataInputStream in=new DataInputStream(getTargetContext().openFileInput("auto.sg11"))){in.readInt();require(in.readInt()==21,"upgraded writer produced v21 header");}
        screenshot(upgrade27?"00-v27-upgrade-preserved":upgrade26?"00-v26-upgrade-preserved":upgrade25?"00-v25-upgrade-preserved":"00-v09-upgrade-preserved");
        if(upgrade26||upgrade27){
            require(legacy.districts.all().size()==1&&legacy.domestic.missions.size()==2&&legacy.units.stream().anyMatch(u->!legacy.aiOrders.describe(u).equals("待评估")),"v26 district, real army intention and two actual tasks retained");
            Domestic.Mission returning=legacy.domestic.missions.get(0),cargo=legacy.domestic.missions.get(1);
            require(returning.returning&&returning.crew().length==3&&cargo.transport&&cargo.crew().length==3&&cargo.food==9950&&cargo.consumedFood==50,"v20 return crew and already-paid grain retained");
            World expected=SaveCodec.decode(before);require(expected.nextTurn().ok,"expected migrated continuation");endTurn();waitForTurn(legacy.turn+1);
            require(Arrays.equals(SaveCodec.encode(expected),SaveCodec.encode(saved())),"v26 upgrade executes only new tactical transport executor");
            runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(SaveCodec.encode(expected),SaveCodec.encode(saved())),"v26 recreation no duplicate dispatch ration or delivery");screenshot("01-v26-upgrade-continued");
        }

        if(upgrade25){require(legacy.districts.all().size()==1&&legacy.domestic.missions.size()==1&&legacy.districts.get(1).reserveFood()==45000,"real v25 district and pending mission loaded");
            World expected=SaveCodec.decode(before);require(expected.nextTurn().ok,"expected next turn");int turn=legacy.turn+1;endTurn();waitForTurn(turn);
            require(Arrays.equals(SaveCodec.encode(expected),SaveCodec.encode(saved())),"v25 upgrade continues actual AI and convoy exactly once");
            runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(SaveCodec.encode(expected),SaveCodec.encode(saved())),"activity recreation does not redispatch or consume food");screenshot("01-v25-upgrade-continued");}

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
        require(pending!=null,"deterministic actual debate victory available");installFixture(pending,pending.city(10).hex);scrollToText("舌战获胜",false);
        before=SaveCodec.encode(saved());runOnMainSync(current::recreate);scrollToText("舌战获胜",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"pending victory remains uncommitted after recreation");screenshot("46-debate-victory");
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
        w=saved();w.unit(6).hex=new Hex(7,18);w.unit(6).acted=false;w.unit(6).movementSpent=0;w.unit(6).movementBudget=-1;installFixture(w,w.unit(6).hex);
        click("讨伐贼寨",true);click("盗贼 · 兵",false);click("执行",true);require(saved().events.camps().get(0).troops<3000,"real camp damage");screenshot("73-raider-attack");
        w=ScenarioCatalog.load("world-drill",0,41);for(int seed=0;seed<100;seed++){w.strategy.setSeed(seed);World trial=SaveCodec.decode(SaveCodec.encode(w));trial.war.plot(1,trial.unit(4).hex,War.Plot.LIGHTNING);if(trial.unit(4).troops<w.unit(4).troops)break;}
        installFixture(w,w.unit(1).hex);click("计略",true);click("落雷 · 气力1",true);tapHex(saved().unit(4).hex);before=SaveCodec.encode(saved());screenshot("74-lightning-preview");click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"lightning preview is pure");click("取消选取",true);
        click("计略",true);click("落雷 · 气力1",true);tapHex(saved().unit(4).hex);click("执行",true);require(saved().unit(4).troops<6000&&saved().unit(1).energy==99&&!saved().war.fires().isEmpty(),"UI lightning consumes one and affects actual units/fire");screenshot("75-lightning-result");
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
        installFixture(w,w.unit(1).hex);click("设置军事设施",true);click("阵 · 金1500",true);tapHex(new Hex(7,5));
        byte[] before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"fieldwork cancel is pure");click("取消选取",true);click("选中对象指令 ·",false);
        click("设置军事设施",true);click("阵 · 金1500",true);tapHex(new Hex(7,5));click("执行",true);
        w=saved();War.Structure s=w.war.at(new Hex(7,5));require(s!=null&&!s.complete&&s.builder==1&&w.unit(1).gold==8500,"UI construction uses carried gold and incomplete structure");screenshot("51-fieldwork-building");
        before=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("筑垒研兵",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"construction and carried gold survive recreation");click("选中对象指令 ·",false);
        click("中止施工",true);click("执行",true);int hp=saved().war.at(new Hex(7,5)).hp;
        endTurn();waitForTurn(1);require(saved().war.at(new Hex(7,5)).hp==hp,"stopped construction does not progress");
        click("补修军事设施",true);click("阵 ·",false);click("执行",true);
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
        runOnMainSync(()->{try{field.set(current,w);((MainActivity)current).rememberForm(new Bundle());((MainActivity)current).selectAndFocus(focus);}catch(IllegalAccessException e){throw new RuntimeException(e);}});
        try(FileOutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(w));}waitForIdleSync();
    }
    private void armyCity(){clickNav("城市");click("江东大营 · 江东军",false);click("军事",true);}
    private void fillFormation(){
        clickNav("城市");click("江东大营 · 江东军",false);click("出征",true);click("孙权 ·",false);click("清空副将",true);click("周瑜 ·",false);click("甘宁 ·",false);click("鲁肃 ·",false);
        click("下一步",true);click("冲车 ·",false);click("楼船 ·",false);setInput("粮食数量","18000");
    }
    private void armyFlow()throws Exception {
        click("菜单",true);click("新游戏 / 选择势力",true);click("水陆攻防 ·",false);click("江东军",true);click("执行",true);
        byte[] initial=SaveCodec.encode(saved());fillFormation();screenshot("23-formation-confirm");click("取消",true);
        require(Arrays.equals(initial,SaveCodec.encode(saved())),"formation preview and cancel do not change state");
        fillFormation();click("确认出征",true);World w=saved();World.Unit army=w.unit(1);
        require(army!=null&&army.weapon==World.Weapon.RAM&&army.ship==Army.Ship.TOWER_SHIP&&army.deputies.length==2,"UI creates three-officer siege/ship formation, caps deputies");
        require(w.officer(1).unitId==1&&w.officer(2).unitId==1&&w.officer(3).unitId==-1,"only selected two deputies leave city");
        require(w.city(10).ships[0]==2&&w.city(10).equipment[5]==1&&army.food==18000&&w.actionPoints[0]==50,"UI charges one ship, one ram and one AP payment");screenshot("24-three-officer-army");
        for(String name:new String[]{"主将 · 孙权 · 查看武将","副将 · 周瑜 · 查看武将","副将 · 甘宁 · 查看武将"}){
            click(name,true);waitText("统率",false);waitText("适性：",false);waitText("特技：",false);screenshot("v020-officer-"+name.charAt(0)+"-"+name.charAt(5));click("返回",true);
        }
        byte[] before=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("水陆攻防",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"v6 formation survives Activity recreation");
        click("入城",true);tapCity(10,0);click("执行",true);w=saved();require(w.units.isEmpty()&&w.officer(1).cityId==10&&w.city(10).ships[0]==3&&w.city(10).equipment[5]==2,"UI returns all crew and equipment once");

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
        click("战法",true);click("火矢 ·",false);tapHex(saved().unit(2).hex);click("执行",true);w=saved();
        require(w.unit(2).burning==2&&w.unit(2).troops<5000&&w.unit(1).energy==70,"naval fire tactic updates actual troops and persistent burning");screenshot("26-naval-combat");
        before=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("水陆攻防",false);require(Arrays.equals(before,SaveCodec.encode(saved())),"naval outcome survives restart");
        World crossing=SaveCodec.decode(before);crossing.unit(1).acted=false;installFixture(crossing,crossing.unit(1).hex);
        click("行军",true);tapHex(new Hex(8,8));click("开始行军",true);w=saved();require(w.unit(1).hex.equals(new Hex(8,8))&&w.unit(1).weapon==World.Weapon.SPEAR&&w.unit(1).ship==Army.Ship.WARSHIP,"map tap disembarks with preserved land gear and ship");screenshot("27-disembarked");
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
        click("行军",true);tapHex(new Hex(7,8));waitText("路线预览",false);screenshot("28-move-preview");click("取消",true);
        require(Arrays.equals(initial,SaveCodec.encode(saved())),"move cancel does not change world or RNG");
        click("行军",true);tapHex(new Hex(7,8));click("开始行军",true);w=saved();
        require(w.unit(1).hex.equals(new Hex(7,8))&&!w.unit(1).acted&&w.unit(1).movementSpent>0,"move preserves command and charges path");
        byte[] moved=SaveCodec.encode(w);runOnMainSync(current::recreate);waitText("水陆攻防",false);
        require(Arrays.equals(moved,SaveCodec.encode(saved())),"movement budget survives recreation");
        click("选中对象指令 ·",false);
        click("计略",true);click("扰乱 · 气力1",true);tapHex(saved().unit(2).hex);screenshot("29-skill-plot-preview");click("取消",true);
        require(Arrays.equals(moved,SaveCodec.encode(saved())),"skill plot cancel is pure");click("取消选取",true);
        click("计略",true);click("扰乱 · 气力1",true);tapHex(saved().unit(2).hex);click("执行",true);w=saved();
        require(w.unit(1).acted&&w.unit(1).energy==79&&w.unit(2).statusTurns==2&&w.unit(3).statusTurns==2,"UI 神算百出连环 costs once and resolves two targets");
        screenshot("30-move-then-skills");
    }
    private String contentAnchor()throws Exception {java.lang.reflect.Field f=MainActivity.class.getDeclaredField("ui");f.setAccessible(true);return ((ClientState)f.get(current)).contentFirstId;}
    private void sourceProfileFlow()throws Exception {
        World w=ScenarioCatalog.load("regional-sandbox",0);installFixture(w,w.city(100).hex);
        int id=ContentCatalog.get().officers().stream().filter(o->o.name.equals("曹丕")).findFirst().get().id;
        click("菜单",true);click("全国资料 / 核验目录",true);click("剧本缺口",true);click("武将资料",true);setInput("搜索资料","曹丕");click("曹丕 · ID",false);
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
        click("军事",true);click("快速出征（单将）",true);click("甘宁 ·",false);click("弩兵 ·",false);click("确认出征",true);waitForIdleSync();require(saved().officer(3003).unitId>0,"sourced opening deployed via native command");
        require(saved().officer(3003).skillId.equals(Skill.WEIFENG.id),"source skill ID binds to actual runtime officer");
        click("编队特技",true);waitText("甘宁 · 威风",false);screenshot("33-sourced-runtime-skill");click("返回",true);
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
        float[] old={0,0};runOnMainSync(()->{old[0]=camera.centerX();old[1]=camera.centerY();});int[] pos=new int[2];float[] target=new float[2];runOnMainSync(()->{map.getLocationOnScreen(pos);float den=map.getResources().getDisplayMetrics().density;target[0]=pos[0]+map.getWidth()-20*den;target[1]=pos[1]+24*den;});
        long t=SystemClock.uptimeMillis();send(t,t,MotionEvent.ACTION_DOWN,target[0],target[1]);send(t,t+40,MotionEvent.ACTION_MOVE,target[0]-20,target[1]+20);send(t,t+80,MotionEvent.ACTION_UP,target[0]-20,target[1]+20);waitForIdleSync();require(camera.centerX()!=old[0]||camera.centerY()!=old[1],"navigator touch relocates camera on an available axis");require(Arrays.equals(before,SaveCodec.encode(saved())),"navigator does not issue commands");screenshot("32-navigator");
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
        if(text.equals("返回全国列表")||text.startsWith("导航 · ")||text.startsWith("选中对象指令 ·")||text.endsWith("待行动部队"))value=node.getContentDescription();
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
        // Dialogs and scrolling can expose accessibility nodes before their final layout.
        try{getUiAutomation().waitForIdle(100,5000);}catch(java.util.concurrent.TimeoutException e){throw new AssertionError("UI did not settle before clicking: "+text,e);}
        node=waitText(text,exact);
        Rect bounds=new Rect();node.getBoundsInScreen(bounds);
        // A partly visible row may have its full center behind the fixed footer.
        for(AccessibilityNodeInfo parent=node.getParent();parent!=null;parent=parent.getParent()){
            Rect clip=new Rect();parent.getBoundsInScreen(clip);
            require(bounds.intersect(clip),"click target has visible bounds: "+text);
        }
        long time=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0);
        MotionEvent up=MotionEvent.obtain(time,time+40,MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);
        sendPointerSync(down);sendPointerSync(up);down.recycle();up.recycle();waitForIdleSync();SystemClock.sleep(350);
    }
    private AccessibilityNodeInfo scrollToText(String text,boolean exact) {
        AccessibilityNodeInfo node=null;long until=SystemClock.uptimeMillis()+12000;boolean forward=true;
        while(node==null&&SystemClock.uptimeMillis()<until) {
            waitForIdleSync();AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();node=find(root,text,exact);
            // Lists retain their position across reopening/recreation. Search the full
            // list in both directions, rather than bouncing around its last page.
            if(node==null){if(forward&&!scroll(root))forward=false;if(!forward)scrollBack(root);SystemClock.sleep(250);}
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
    private void battleFeedbackFlow()throws Exception {
        World w=new World(12,8,"我军","敌军");w.scenarioId="battle-feedback-smoke";w.scenarioName="战果验证";
        w.cities.add(new World.City(10,"本营",new Hex(3,3),0));w.cities.add(new World.City(20,"敌营",new Hex(10,6),1));
        World.Officer a=new World.Officer(1,"捕将",0,-1,80,80,80,80,80),b=new World.Officer(2,"敌将",1,-1,80,80,80,80,80);
        a.unitId=1;b.unitId=2;a.skillId=Skill.BOFU.id;w.officers.add(a);w.officers.add(b);
        World.Unit u=new World.Unit(1,0,1,World.Weapon.SPEAR,new Hex(4,4),5000,10000),enemy=new World.Unit(2,1,2,World.Weapon.HALBERD,new Hex(5,4),1,5000);
        enemy.gold=600;w.units.add(u);w.units.add(enemy);w.nextUnitId=3;
        for(long seed=0;seed<100;seed++){w.strategy.setSeed(seed);World probe=SaveCodec.decode(SaveCodec.encode(w));probe.attack(1,2);probe.marches.execute(probe.marches.preview(1,enemy.hex));probe.nextTurn();if(probe.government.captive(2))break;}
        installFixture(w,u.hex);
        click("攻击",true);tapHex(enemy.hex);waitText("敌将：100%",false);screenshot("v020-capture-preview");click("执行",true);
        waitText("击破战果",false);require(saved().unit(2)==null&&saved().unit(1).gold==600&&saved().unit(1).food==15000&&saved().government.captive(2),"actual map attack shows and saves captured resources and officer");
        click("击破战果",false);waitText("战斗结果",true);waitText("金+600",false);screenshot("v020-defeat-report");click("收起战果",true);
        click("行军",true);tapHex(enemy.hex);waitText("本旬已行动，下旬出发",false);screenshot("v020-defeated-tile-route");click("开始行军",true);
        require(saved().unit(1).hex.equals(new Hex(4,4))&&saved().unit(1).march!=null,"can queue former enemy tile without refunding action");
        endTurn();require(saved().unit(1).hex.equals(enemy.hex)&&saved().unit(1).march==null,"installed client enters defeated tile next turn");
        World marching=saved();require(marching.government.prisoner(2).unitId==1&&marching.government.location(marching.government.prisoner(2)).equals(enemy.hex),"prisoner follows real march");
        click("选中对象指令 ·",false);click("随军俘虏 · 敌将",true);waitText("随捕将部队行军",false);screenshot("v020-escort-officer");click("返回",true);
        byte[] escortSave=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(escortSave,SaveCodec.encode(saved())),"escorted captive survives client recreation");
        click("行军",true);tapHex(new Hex(4,3));click("开始行军",true);require(saved().unit(1).hex.equals(new Hex(4,3)),"carrier returns to city entrance");
        click("选中对象指令 ·",false);click("入城",true);tapHex(new Hex(3,3));click("执行",true);require(saved().unit(1)==null&&saved().government.prisoner(2).unitId==-1&&saved().government.prisoner(2).cityId==10,"only entering city changes prisoner to fixed city jail");
        screenshot("v020-prisoner-delivered");
        chooseOrientation("竖屏");click("全图",true);screenshot("v020-map-models-portrait");
        click("视图",true);click("战斗震动",true);click("关闭",true);runOnMainSync(current::recreate);waitForIdleSync();
        click("视图",true);click("战斗震动",true);require(waitText("关闭",true).isChecked(),"haptics off survives recreation");click("开启（遵循系统触感设置）",true);
        chooseOrientation("横屏");
    }
    private World diplomacyFixture()throws Exception {
        World w=new World(32,18,"汉营","盟营","敌营");w.scenarioId="diplomacy-smoke";w.scenarioName="合纵连横";
        w.cities.add(new World.City(0,"汉都",new Hex(2,3),0));w.cities.add(new World.City(1,"汉后方",new Hex(2,12),0));w.cities.add(new World.City(10,"盟都",new Hex(10,3),1));w.cities.add(new World.City(20,"敌都",new Hex(26,3),2));
        String[] names={"刘备","诸葛亮","关羽","张飞","赵云"};
        for(World.City c:w.cities){c.gold=30000;c.food=150000;c.troops=c.owner==0?60000:18000;c.morale=90;
            for(int i=0;i<5;i++){World.Officer o=new World.Officer(c.id*10+i,c.id==0?names[i]:"使将"+(c.id*10+i),c.owner,c.id,80,80,85,90,95);w.officers.add(o);}
        }
        for(int id:new int[]{0,100,200}){w.officer(id).role=Strategy.Role.RULER;w.officer(id).loyalty=100;}
        w.strategy.setFactionRelation(0,1,100);
        for(int seed=0;seed<100;seed++){w.strategy.setSeed(seed);World test=SaveCodec.decode(SaveCodec.encode(w));test.campaign.negotiate(0,1,1,Campaign.TreatyKind.ALLIANCE,12);if(test.campaign.treaty(0,1)!=null)break;}
        require(w.campaign.negotiate(0,1,1,Campaign.TreatyKind.ALLIANCE,12).ok,"fixture alliance");Arrays.fill(w.actionPoints,60);for(World.Officer o:w.officers)o.acted=false;
        return w;
    }
    private void aidWizard(){locateCity("汉都");click("外交",true);click("外交 / 协定",true);click("盟营 ·",false);click("请求援军",true);click("诸葛亮 ·",false);click("盟都 ·",false);click("敌都 ·",false);click("金1000",true);}
    private void diplomacyVisualFlow()throws Exception {
        World w=diplomacyFixture();
        for(int seed=0;seed<100;seed++){w.strategy.setSeed(seed);World test=SaveCodec.decode(SaveCodec.encode(w));test.diplomacy.requestAid(0,1,10,20,1000);if(!test.diplomacy.aids().isEmpty())break;}
        installFixture(w,w.city(0).hex);chooseOrientation("竖屏");locateCity("汉都");screenshot("v021-city-card");
        java.lang.reflect.Field stateField=MainActivity.class.getDeclaredField("ui");stateField.setAccessible(true);runOnMainSync(()->{try{ClientState state=(ClientState)stateField.get(current);state.owner=-1;state.city=-1;state.query="";}catch(IllegalAccessException e){throw new RuntimeException(e);}});
        clickNav("武将");setSearch("诸葛亮");screenshot("v021-officer-list");click("诸葛亮 · 汉营",true);waitForIdleSync();require(findInput(getUiAutomation().getRootInActiveWindow(),"诸葛亮头像或模型")!=null,"detail has officer portrait");screenshot("v021-officer-detail");click("返回",true);
        locateCity("汉都");click("军事",true);click("快速出征（单将）",true);click("张飞 ·",false);waitText("选择兵种",true);screenshot("v021-weapon-picker");click("取消",true);
        byte[] before=SaveCodec.encode(saved());aidWizard();waitText("期限18旬",false);screenshot("v021-aid-confirm");click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"aid cancellation leaves money, officers, RNG and requests untouched");
        aidWizard();click("执行",true);require(saved().diplomacy.aids().size()==1&&saved().actionPoints[0]==30,"UI aid request persists and spends 30 AP");
        endTurn();w=saved();require(w.diplomacy.aids().size()==1&&w.diplomacy.aids().get(0).unit>=0,"UI next turn dispatches actual allied army");
        clickNav("任务");click("援军 · 盟营",true);screenshot("v021-aid-task");click("返回",true);before=SaveCodec.encode(saved());runOnMainSync(current::recreate);waitForIdleSync();require(Arrays.equals(before,SaveCodec.encode(saved())),"aid survives activity recreation");
        click("视图",true);click("兵种与建筑图例",true);click("陆军与水军",true);screenshot("v021-icon-guide");click("返回",true);
        portraitAtlas();
        w=diplomacyFixture();w.city(10).troops=1000;w.city(10).defense=500;
        for(int seed=0;seed<100;seed++){w.strategy.setSeed(seed);World test=SaveCodec.decode(SaveCodec.encode(w));test.diplomacy.surrender(0,1,1);if(!test.alive(1))break;}
        installFixture(w,w.city(0).hex);locateCity("汉都");click("外交",true);click("外交 / 协定",true);click("盟营 ·",false);click("劝降",true);click("诸葛亮 ·",false);screenshot("v021-surrender");before=SaveCodec.encode(saved());click("取消",true);require(Arrays.equals(before,SaveCodec.encode(saved())),"cancel surrender is pure");
        locateCity("汉都");click("外交",true);click("外交 / 协定",true);click("盟营 ·",false);click("劝降",true);click("诸葛亮 ·",false);click("执行",true);require(!saved().alive(1)&&saved().city(10).owner==0&&saved().officer(100).role==Strategy.Role.OFFICER,"UI surrender transfers whole force");screenshot("v021-surrender-complete");
        chooseOrientation("横屏");
    }
    private void portraitAtlas()throws Exception {
        Bitmap bitmap=Bitmap.createBitmap(800,1000,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(0xff192d32);Paint label=new Paint(Paint.ANTI_ALIAS_FLAG);label.setColor(0xffead9ad);label.setTextSize(22);label.setTextAlign(Paint.Align.CENTER);World w=saved();
        try(InputStream in=getTargetContext().getAssets().open("portraits/officers.png")){Bitmap atlas=BitmapFactory.decodeStream(in);require(atlas!=null&&atlas.getWidth()>=1000&&atlas.getHeight()>=1000,"original portrait atlas packaged and decodable");atlas.recycle();}
        try(InputStream in=getTargetContext().getAssets().open("map/buildings.png")){Bitmap atlas=BitmapFactory.decodeStream(in);require(atlas!=null&&atlas.getWidth()==1254&&atlas.getHeight()==1254&&atlas.hasAlpha(),"complete transparent building atlas packaged");atlas.recycle();}
        for(int i=0;i<20;i++){String name=i<16?PortraitCatalog.NAMES[i]:"自建武将"+i;World.Officer o=new World.Officer(900+i,name,0,0,60,60,80,80,60);if(i==18)o.sex=World.Sex.FEMALE;
            OfficerPortrait portrait=new OfficerPortrait(getTargetContext(),w,o);int x=i%4*200,y=i/4*200;portrait.setBounds(x+20,y+10,x+180,y+170);portrait.draw(canvas);canvas.drawText(name,x+100,y+195,label);}
        File directory=getTargetContext().getExternalFilesDir("smoke");require(directory!=null,"portrait atlas directory");try(FileOutputStream out=new FileOutputStream(new File(directory,"v021-portrait-atlas.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    private void modelAtlas()throws Exception {
        MapModels models=new MapModels();java.util.List<String> labels=new java.util.ArrayList<>();
        java.util.List<java.util.function.Consumer<Canvas>> draws=new java.util.ArrayList<>();int color=0xff62af8f;
        for(World.Weapon weapon:World.Weapon.values()){labels.add(weapon.label);draws.add(c->models.unit(c,new World.Unit(1,0,1,weapon,new Hex(0,0),1000,1000),false,color));}
        for(Army.Ship ship:Army.Ship.values()){labels.add(ship.label);draws.add(c->{World.Unit u=new World.Unit(1,0,1,World.Weapon.SPEAR,new Hex(0,0),1000,1000);u.ship=ship;models.unit(c,u,true,color);});}
        for(World.SiteKind kind:World.SiteKind.values()){labels.add(kind.name());draws.add(c->models.city(c,kind,color));}
        for(Domestic.Kind kind:Domestic.Kind.values()){labels.add(kind.label);draws.add(c->models.facility(c,kind,color));}
        for(War.StructureKind kind:War.StructureKind.values()){labels.add(kind.label);draws.add(c->models.structure(c,kind,color));}
        require(labels.size()==44,"all current troop and building categories have a renderable model");
        Bitmap bitmap=Bitmap.createBitmap(1200,1200,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);c.drawColor(0xff192d32);
        Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);text.setColor(0xffead9ad);text.setTextSize(23);text.setTextAlign(Paint.Align.CENTER);
        for(int i=0;i<labels.size();i++){float x=(i%6)*200+100,y=(i/6)*150+70;c.save();c.translate(x,y);c.scale(2,2);draws.get(i).accept(c);c.restore();c.drawText(labels.get(i),x,y+62,text);}
        File directory=getTargetContext().getExternalFilesDir("smoke");if(directory==null)throw new IOException("Model screenshot directory unavailable");
        try(FileOutputStream out=new FileOutputStream(new File(directory,"v020-model-atlas.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
    }
    private void screenshot(String name)throws IOException {
        waitForIdleSync();SystemClock.sleep(250);Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap==null)throw new IOException("Screenshot unavailable");
        File directory=getTargetContext().getExternalFilesDir("smoke");if(directory==null)throw new IOException("Screenshot directory unavailable");
        try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
    }
}

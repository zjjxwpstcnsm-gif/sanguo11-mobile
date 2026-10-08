package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.GameSession;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/** Normal controls only. Authority reads choose legal commands; no world,
 * seed, resources, fighters, position or result is injected. Requires the
 * separately frozen A opening adapter. Compilation alone is not acceptance. */
public final class SessionBNativeDuelInstrumentation extends SessionBFieldworksInstrumentation {
    private boolean jiRoute,recruited,physicalRecovery; private int routeUnit;
    private String mode="normal",humanStrategy="attack",aiActorPolicy="preserve",humanActorPolicy="preserve",humanDisposition="detain",recruitItemPolicy="preserve";private boolean retreatSelected;
    @Override protected void nav(String name)throws Exception{
        for(int attempt=0;attempt<3;attempt++){
            android.app.AlertDialog dialog=(android.app.AlertDialog)field(activity,"navigationDialog");
            if(dialog==null||!dialog.isShowing()){
                View trigger=await(v->v.isEnabled()&&v.isClickable()&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
                android.graphics.Rect bounds=new android.graphics.Rect();int[] rootAt=new int[2];
                runOnMainSync(()->{trigger.getGlobalVisibleRect(bounds);trigger.getRootView().getLocationOnScreen(rootAt);});
                pointer(bounds.centerX()+rootAt[0],bounds.centerY()+rootAt[1],1);settle();
            }
            dialog=(android.app.AlertDialog)field(activity,"navigationDialog");
            note("nav="+name+" attempt="+attempt+" showing="+(dialog!=null&&dialog.isShowing()));
            if(dialog!=null&&dialog.isShowing()){revealDescription("导航 · "+name);settle();return;}
            shot("navigation-"+name+"-"+attempt);
        }
        throw new AssertionError("Normal enabled navigation trigger did not open "+name);
    }
    @Override public void onCreate(Bundle args){
        if(args!=null){jiRoute="ji-recruit".equals(args.getString("native_route","legacy"));mode=args.getString("mode","normal");physicalRecovery="adopt".equals(args.getString("physical_recovery","preserve"));humanStrategy=args.getString("native_strategy","attack");aiActorPolicy=args.getString("ai_actor_policy","preserve");humanActorPolicy=args.getString("human_actor_policy","preserve");humanDisposition=args.getString("human_disposition","detain");recruitItemPolicy=args.getString("recruit_item_policy","preserve");}
        super.onCreate(args);
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            evidence=new File(getTargetContext().getExternalFilesDir("session-b"),"native-duel-"+(mode.startsWith("continue-")?"normal":mode));
            evidence.mkdirs();put("output",evidence);
            check("attack".equals(humanStrategy)||"spirit".equals(humanStrategy)||"retreat".equals(humanStrategy)||"swap-critical".equals(humanStrategy),"registered ordinary human stance strategy");
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));put("activity",activity);
            awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
            if("terminal-cold".equals(mode)){
                byte[] expected=Files.readAllBytes(new File(getTargetContext().getExternalFilesDir("session-b"),"native-duel-cold/final.sg11").toPath());
                check(Arrays.equals(expected,capture()),"final new process resumes complete World and dual RNG");
                load();check(Arrays.equals(expected,capture()),"final new process manual load exact");
            }else if(coldMode){
                byte[] expected=Files.readAllBytes(new File(getTargetContext().getExternalFilesDir("session-b"),"native-duel-normal/battle.sg11").toPath());
                check(Arrays.equals(expected,capture()),"new process resumes actual human battle and both RNG");
                load();check(Arrays.equals(expected,capture()),"manual load actual human battle exact");
                if(jiRoute){World w=SessionProbe.view(activity);routeUnit=w.officer(PcDuelSourceFacts.saved(w).values().stream().filter(v->v.nativeId==98).findFirst().orElseThrow().officerId).unitId;}
                finishBattle();
                if(jiRoute){for(int n=0;!recruited&&n<8;n++){advance("ji-next-encounter-"+n);encounter(routeUnit);finishBattle();}check(recruited,"actual ordinary recruitment succeeds without rewriting seed/result");}
                for(int i=0;i<3;i++){advance("native-terminal-"+i);if(physicalRecovery)verifyPhysicalRecovery(i+1);}
                byte[] finalSave=capture();save(finalSave);load();check(Arrays.equals(finalSave,capture()),"three whole turns/final manual load exact");
                Files.write(new File(evidence,"final.sg11").toPath(),finalSave);
            }else{
                // Exercise both unconstrained and constrained source menus before
                // the campaign. These drafts must not alter the running save.
                int unit;
                if("continue-physical-terminal".equals(mode)){
                    byte[] actual=Files.readAllBytes(new File(evidence,"continue-input.sg11").toPath());load();check(capturedSha(actual).equals("a1e304702ef426190d50378e51b52d1647f6a41eb36845967c2720369c35327f")&&Arrays.equals(actual,capture()),"exact real r21 defeat terminal ordinary load/no automatic health upgrade");check(physicalRecovery,"explicit physical recovery test requested");adoptPhysicalRecovery();unit=-2;
                }else if("continue-ji-campaign".equals(mode)){
                    byte[] actual=Files.readAllBytes(new File(evidence,"continue-input.sg11").toPath());load();check(capturedSha(actual).equals("8a15fd9e5371b7303e9cabede78e964b1d8e7b0a071d4d8bb83ee4074771b4c6")&&Arrays.equals(actual,capture()),"exact actual r20 turn19 ordinary slot load whole World/dual RNG");World w=SessionProbe.view(activity);check(jiRoute&&w.player==4&&w.turn==19&&w.unit(1)!=null&&w.war.structures().stream().anyMatch(v->v.owner==4&&v.kind==War.StructureKind.DRUM&&v.complete),"exact three-person campaign and real completed drum preserved");unit=routeUnit=1;
                }else if("continue-human-pending".equals(mode)){
                    byte[] actual=Files.readAllBytes(new File(evidence,"continue-input.sg11").toPath());load();check(capturedSha(actual).equals("20f253f77b4fe8e21b205976a568ebcbfe53cd441bca730bbcf0a329fa6efa24")&&Arrays.equals(actual,capture()),"exact ordinary r18 failed attempt load in new process full World/model/dual RNG");
                    var pending=facts();check(pending.nativeDuel.humanActorPolicyEnabled&&pending.nativeDuel.humanActorNativeId==403&&!pending.nativeDuel.recruitItemPolicyEnabled&&!pending.nativeDuel.recruitItemPolicyAdoptionAvailable&&pending.nativeDuel.recruitItemRecipientNativeId==503,"failed saved attempt retains new judge and legacy item strategy, no upgrade or rebind");check(pending.nativeDuel.disposition.size()==1&&pending.nativeDuel.disposition.get(0).choice==4&&pending.nativeDuel.disposition.get(0).mask==14&&!pending.nativeDuel.disposition.get(0).enabled(0),"original failed attempt mask/no new recruitment draw preserved");unit=-2;
                }else if("continue-human-terminal".equals(mode)){
                    byte[] actual=Files.readAllBytes(new File(evidence,"continue-input.sg11").toPath());load();
                    check(capturedSha(actual).equals("5e2730ea0dcbba4702f5ce6f6bf9b105c2a576a8205c89d8fb7dedb8a73ee0c5")&&Arrays.equals(actual,capture()),"exact actual player victory ordinary load unchanged full World/model/dual RNG");
                    adoptLoyaltyInput();if("force-ruler".equals(humanActorPolicy))adoptHumanActor();if("force-ruler".equals(recruitItemPolicy))adoptRecruitItemRecipient();unit=-2;
                }else if("continue-terminal".equals(mode)){
                    byte[] actual=Files.readAllBytes(new File(evidence,"continue-input.sg11").toPath());load();
                    check(capturedSha(actual).equals("96bfcc08d64f4c53699a1506ef31c06df571fa418a1c8d32a7046618f2d3778f")&&Arrays.equals(actual,capture()),"exact actual normal defeat loaded with original full World/model/dual RNG and no adoption");
                    adoptLoyaltyInput();if("force-ruler".equals(aiActorPolicy))adoptAiActor();unit=-2;
                }else if("continue-battle".equals(mode)){
                    byte[] actual=Files.readAllBytes(new File(evidence,"continue-input.sg11").toPath());load();
                    check((capturedSha(actual).equals("668b77e99cbed02f6093530f17f0b38bdc2b0e4745a6b2f69ba8a58c0455a2b2")||capturedSha(actual).equals("b81af67a7a940d53061f960d2121b194b0e0240d7bc9affb4843c709b65741ca"))&&Arrays.equals(actual,capture()),"exact actual normal battle ordinary load with full World/model/RNG");
                    check(facts().nativeDuel!=null&&!facts().nativeDuel.terminal,"original actual accepted human battle resumes");unit=-1;
                }else if("continue-campaign".equals(mode)){
                    byte[] actual=Files.readAllBytes(new File(evidence,"continue-input.sg11").toPath());
                    load();check(Arrays.equals(actual,capture()),"ordinary load exact previous actual Android campaign/dual RNG, no reconstruction");
                    World continued=SessionProbe.view(activity);
                    check(continued.player==28&&continued.unit(1)==null,"same actual campaign, original army loss retained");
                    if(continued.turn==9){unit=deploy(503,13000,40000,World.Weapon.SWORD);}
                    else{
                        check(continued.turn==18&&capturedSha(actual).equals("06329224846382ae8cea34988458622ce13f8756c1912ac6f10f9e00e4fb2661"),"exact captured actual post-interception turn18 continuation");
                        var survivor=continued.unit(16);var source=survivor==null?null:PcDuelSourceFacts.saved(continued).get(survivor.officerId);
                        check(survivor!=null&&survivor.owner==continued.player&&source!=null&&source.nativeId==503&&continued.districts.directUnit(16),"same genuine survivor/source binding, no redeployment or resource reset");unit=16;
                    }
                }else{
                    opening(7,28,true);opening(0,jiRoute?4:28,false);unit=jiRoute?deployCrew():deploy(503);routeUnit=unit;
                }
                if(unit>=0)encounter(unit);
                var f=facts();check(f.nativeDuel!=null&&(unit==-2?f.nativeDuel.terminal:jiRoute||!f.nativeDuel.terminal),"actual ordinary challenge or unchanged terminal continues");
                if(unit!=-2&&!f.nativeDuel.terminal)humanInput();
                byte[] battle=capture();save(battle);load();check(Arrays.equals(battle,capture()),"real battle manual save/load full model and dual RNG");
                Files.write(new File(evidence,"battle.sg11").toPath(),battle);shot("actual-human-battle");
            }
            result.putString("stream","PASS SESSION B NATIVE DUEL"+(coldMode?" COLD":"terminal-cold".equals(mode)?" TERMINAL COLD":"")+" real controls/fullWorld/bothRNG; source defaults, full scenario matrix, retreat and ARM separate\n");
        }catch(Throwable t){
            result.putString("stream","FAIL SESSION B NATIVE DUEL "+android.util.Log.getStackTraceString(t));
            try{Files.write(new File(evidence,"failed-campaign.sg11").toPath(),capture());shot("failed");}catch(Throwable ignored){}
        }finally{
            try{if(activity!=null)runOnMainSync(activity::finish);settle();}catch(Throwable t){result.putString("stream",result.getString("stream")+"\nFAIL lifecycle "+t);}
        }
        try{Files.write(new File(evidence,"result.txt").toPath(),result.getString("stream").getBytes("UTF-8"));}catch(Exception ignored){}
        finish(Activity.RESULT_OK,result);
    }
    private void opening(int index,int player,boolean cancel)throws Exception{
        var source=PcScenarioCatalog.all().get(index);byte[] before=capture();var state=activity.deploymentState();
        var metadata=GameSession.previewNewSourceOptions(source.identity.scenarioId);
        check(metadata.sourceSha.equals(source.identity.sha)&&!metadata.defaultsKnown,"exact source menu identity with unknown defaults");
        nav("菜单");text("新游戏 / 选择势力");revealDescription("选择PC来源剧本 "+source.identity.path);
        awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力"));
        String faction=PcScenarioCatalog.preview(source.identity.scenarioId).faction(player);
        revealDescription("选择势力 · "+faction);
        // Tags requested from A, pending verification against its frozen adapter.
        // Missing tags must fail instead of silently taking the old three-arg route.
        tap(tag("pc.opening.options"));
        for(var group:metadata.groups){
            int value=group.fixedMenuValue!=null?group.fixedMenuValue:group.id.equals("death")?2:0;
            View choice=tag("pc.opening."+group.id+"."+value);
            if(group.fixedMenuValue!=null)check(choice.isSelected()&&!choice.isEnabled(),"actual source constraint selected and locked");
            else tap(choice);
        }
        check(state.equals(activity.deploymentState())&&Arrays.equals(before,capture()),"real source-bound option draft preserves complete running save/RNG/StateToken");
        shot("source"+index+"-options");
        if(cancel){
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();
            // Picker Return/Back reopens the real source chooser. Close its
            // ordinary Cancel button before navigating the campaign again.
            text("取消");
            check(state.equals(activity.deploymentState())&&Arrays.equals(before,capture()),"source options/faction/catalog cancel preserves save/RNG/StateToken");return;
        }
        tap(tag("pc.opening.confirm"));description("确认开局势力");text("开始新局");
        awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
        World w=SessionProbe.view(activity);var opts=PcDuelOptions.current(w);
        check(w.scenarioId.equals(source.identity.scenarioId)&&w.player==player&&opts!=null&&opts.life==0&&opts.death==2&&opts.difficulty==0,"ordinary four-arg explicit source opening");
        check(PcSourceOpeningOptions.saved(w)!=null,"normal new game has validated saved source opening policy");
        note("source="+source.identity.scenarioId+" SHA="+source.identity.sha+" player="+player+" turn="+w.turn+" seed is actual menu captured seed, no override");
        shot("source"+index+"-new");
    }
    private int deployCrew()throws Exception{
        World w=SessionProbe.view(activity);var people=PcDuelSourceFacts.saved(w);int[] crew=new int[3];int[] natives={98,432,635};for(int i=0;i<3;i++){final int nativeId=natives[i];crew[i]=people.values().stream().filter(p->p.nativeId==nativeId).findFirst().orElseThrow().officerId;}
        var city=w.city(w.officer(crew[0]).cityId);check(city!=null&&city.equipment[World.Weapon.HALBERD.ordinal()]>=6000,"source-joined three-person legal halberd stock");nav("地图");revealDescription("定位己方据点 "+city.name);byte[] before=capture();text("出征");
        ListView roster=(ListView)tag("deploy.officers");for(int id:crew){invoke("showRosterTag",new Class<?>[]{ListView.class,String.class},roster,"deploy.role."+id);tap(tag("deploy.role."+id));}
        tap(tag("deploy.tab.1"));description("戟兵 库存");quantity("兵力数量","6000");quantity("粮食数量","35000");quantity("金钱数量","1500");check(Arrays.equals(before,capture()),"three-person formation preview pure");long revision=activity.deploymentState().revision;tap(tag("deploy.confirm"),2);
        w=SessionProbe.view(activity);var u=w.unit(w.officer(crew[0]).unitId);check(u!=null&&u.troops==6000&&u.food==35000&&u.gold==1500&&u.weapon==World.Weapon.HALBERD&&Arrays.equals(u.deputies,new int[]{crew[1],crew[2]}),"ordinary exact three-person crew/resources");check(activity.deploymentState().revision==revision+1,"three-person double deployment once");note("ji route unit="+u.id+" crew="+Arrays.toString(crew)+" position="+u.hex);shot("ordinary-three-person-deployed");return u.id;
    }
    private int deploy(int nativeId)throws Exception{
        return deploy(nativeId,3000,30000,World.Weapon.SPEAR);
    }
    private int deploy(int nativeId,int troops,int food,World.Weapon weapon)throws Exception{
        World w=SessionProbe.view(activity);var record=PcScenarioPeople.saved(w).stream().filter(p->p.nativeId==nativeId).findFirst().orElseThrow();
        var leader=w.officer(record.officerId);var city=w.city(leader.cityId);
        check(city!=null&&w.idle(city).contains(leader)&&city.troops>=troops&&city.food>=food&&city.gold>=1000&&(weapon==World.Weapon.SWORD||city.equipment[weapon.ordinal()]>=troops),"actual idle source person/stock admission");
        nav("地图");revealDescription("定位己方据点 "+city.name);byte[] before=capture();text("出征");
        ListView roster=(ListView)tag("deploy.officers");invoke("showRosterTag",new Class<?>[]{ListView.class,String.class},roster,"deploy.role."+leader.id);tap(tag("deploy.role."+leader.id));
        await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().equals("从编队移除 "+leader.name));tap(tag("deploy.tab.1"));description(weapon==World.Weapon.SWORD?"剑兵 无需库存":"枪兵 库存");
        quantity("兵力数量",Integer.toString(troops));quantity("粮食数量",Integer.toString(food));quantity("金钱数量","1000");
        check(Arrays.equals(before,capture()),"normal roster/resources preview preserves complete save");long revision=activity.deploymentState().revision;tap(tag("deploy.confirm"),2);
        w=SessionProbe.view(activity);var u=w.unit(w.officer(leader.id).unitId);
        check(u!=null&&u.troops==troops&&u.food==food&&u.gold==1000&&u.deputies.length==0&&u.weapon==weapon,"actual solo formation with carried resources");
        check(w.city(city.id).troops==city.troops-troops&&w.city(city.id).food==city.food-food&&w.city(city.id).gold==city.gold-1000&&activity.deploymentState().revision==revision+1,"actual double confirmation costs exactly once");
        note("native="+nativeId+" stable="+leader.id+" record="+record.recordSha+" city="+city.id+" unit="+u.id+" gold="+u.gold+" position="+u.hex);shot("ordinary-deployed");return u.id;
    }
    private static String capturedSha(byte[] bytes)throws Exception{StringBuilder hex=new StringBuilder(64);for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))hex.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));return hex.toString();}
    private void quantity(String name,String value)throws Exception{revealDescription(name);EditText e=(EditText)await(v->v instanceof EditText&&name.equals(v.getContentDescription()));invoke("enter",new Class<?>[]{EditText.class,String.class},e,value);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();}
    private MarchOrders.Plan movementTarget(Hex target)throws Exception{
        byte[] before=capture();var token=activity.deploymentState();
        for(int attempt=0;attempt<3;attempt++){
            float[] xy=(float[])invoke("screenHex",new Class<?>[]{Hex.class},target);
            check((Boolean)invoke("routePoint",new Class<?>[]{Hex.class,float[].class},target,xy),"actual safe exact map ray before input "+target);
            note("map input="+target+" attempt="+attempt+" xy="+Arrays.toString(xy)+" mode="+field(activity,"unitCommand")+" moving="+field(activity,"moving")+" selected="+field(activity,"selected")+" aiRunning="+field(activity,"aiRunning"));
            pointer(xy[0],xy[1],1);settle();
            MarchOrders.Plan plan=(MarchOrders.Plan)field(activity,"pendingMarch");
            note("map delivered attempt="+attempt+" pending="+(plan==null?"none":plan.error)+" mode="+field(activity,"unitCommand")+" moving="+field(activity,"moving")+" selected="+field(activity,"selected"));
            check(token.equals(activity.deploymentState())&&Arrays.equals(before,capture()),"actual map target preview/input leaves World/RNG/token pure");
            if(plan!=null){check(plan.valid()&&target.equals(plan.target),"actual selected route admission/target "+plan.error);return plan;}
            if(!"march".equals(field(activity,"unitCommand")))break;
            shot("map-input-"+target.q+"-"+target.r+"-"+attempt);
        }
        throw new AssertionError("Actual map input did not produce a march preview; recorded precise UI coordinates/mode/source, no rule execute injected");
    }
    private void encounter(int unit)throws Exception{
        for(int attempt=0;attempt<36;attempt++){
            World w=SessionProbe.view(activity);var u=w.unit(unit);check(u!=null,"actual deployed unit survives; no replacement injected");
            note("encounter turn="+w.turn+" unit="+unit+" position="+u.hex+" status="+u.status+" acted="+u.acted+" troops="+u.troops+" food="+u.food);
            Files.write(new File(evidence,"campaign-turn-"+w.turn+".sg11").toPath(),capture());
            final var current=u;final var world=w;
            var enemies=w.units.stream().filter(t->world.campaign.hostile(current.owner,t.owner)&&t.status==War.Status.NORMAL&&!world.army.water(t.hex)&&!Army.siegeWeapon(t.weapon)).sorted(Comparator.comparingInt((World.Unit t)->current.hex.distance(t.hex)).thenComparingInt(t->t.id)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));
            if(!u.acted&&u.status==War.Status.NORMAL&&!enemies.isEmpty()){
                var target=enemies.get(0);
                final Hex targetCell=target.hex;
                if(jiRoute&&u.hex.distance(target.hex)<=7&&w.war.structures().stream().noneMatch(v->v.owner==world.player&&v.kind==War.StructureKind.DRUM)){
                    var legal=w.fieldworks.sites(unit,War.StructureKind.DRUM).stream().filter(h->h.distance(targetCell)>0&&h.distance(targetCell)<=7).findFirst();if(legal.isPresent()){Hex site=legal.get();int gold=u.gold;int cost=w.fieldworks.buildCost(unit,War.StructureKind.DRUM,site);byte[] before=capture();selectUnit(unit);pose(site);militaryMenu();option("太鼓台 · 金");tile(site);text("取消");check(Arrays.equals(before,capture()),"actual drum map confirmation cancel pure");text("取消选取");selectUnit(unit);pose(site);militaryMenu();option("太鼓台 · 金");tile(site);long revision=activity.deploymentState().revision;tap(await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText())),2);World built=SessionProbe.view(activity);check(built.war.at(site)!=null&&built.war.at(site).builder==unit&&built.unit(unit).gold==gold-cost&&built.unit(unit).acted&&activity.deploymentState().revision==revision+1,"ordinary drum build double once actual cargo/action");shot("ordinary-drum-construction");advance("ji-drum-build");continue;}
                }
                if(u.hex.distance(target.hex)>1){
                    Hex destination=w.orders.reachable(u).keySet().stream().filter(h->!h.equals(current.hex)&&(!jiRoute||world.cityAt(h)==null)).min(Comparator.comparingInt((Hex h)->h.distance(targetCell)).thenComparingInt(h->h.q).thenComparingInt(h->h.r)).orElse(null);
                    if(destination!=null&&destination.distance(target.hex)<u.hex.distance(target.hex)){
                        byte[] pre=capture();selectUnit(unit);pose(destination);text("行军");var plan=movementTarget(destination);check(Arrays.equals(pre,capture()),"actual map route preview pure");
                        check(plan.stepsNow>=0&&plan.stepsNow<plan.path.size(),"actual immediate movement prefix available");Hex immediate=plan.path.get(plan.stepsNow);
                        note("actual march preview target="+destination+" stepsNow="+plan.stepsNow+" endpoint="+immediate+" estimatedTurns="+plan.estimatedTurns+" path="+plan.path);text("确认任务");
                        w=SessionProbe.view(activity);u=w.unit(unit);check(u!=null&&u.hex.equals(immediate),"normal formal movement reaches exact preview immediate endpoint");
                        if(!immediate.equals(destination))check(u.march!=null&&u.march.intent==MarchOrders.Intent.MOVE&&destination.equals(w.marches.target(u.march)),"normal interception/budget stop preserves exact pending destination");
                    }
                }
                target=w.unit(target.id);
                if(target!=null)note("actual encounter target="+target.id+" actor="+u.hex+" targetCell="+target.hex+" distance="+u.hex.distance(target.hex)+" acted="+u.acted+" march="+u.march+" energy="+u.energy+" rejection="+w.contests.duelError(u.id,target.id));
                if(target!=null&&u.hex.distance(target.hex)==1&&w.contests.duelError(u.id,target.id)==null){
                    byte[] pre=capture();var candidates=w.contests.nativeDuelCandidates(unit,target.id);var nominee=candidates.stream().max(Comparator.comparingInt(c->c.chance)).orElseThrow();
                    selectUnit(unit);unitAction("单挑");option(w.officer(target.officerId).name+" · 应战率");option(nominee.name+" · 原应战估计");check(Arrays.equals(pre,capture()),"actual target/candidate/confirm preview pure");
                    note("target="+target.id+" nominee="+nominee.officerId+" actualChance="+nominee.chance);text("执行");
                    if(facts().nativeDuel!=null)return;
                    note("actual response refused; same campaign costs/RNG retained");
                }
            }
            advance("native-encounter-"+attempt);
        }
        throw new AssertionError("No accepted ordinary encounter through36 whole turns; preserve actual campaign without seed/army injection");
    }
    private ContestSnapshot facts(){ContestSnapshot[] result={null};runOnMainSync(()->result[0]=activity.contestSnapshot());return result[0];}
    private void pageButton(String name)throws Exception{
        runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus()){View hit=search(root,v->v instanceof Button&&v.isEnabled()&&((Button)v).getText().toString().startsWith(name));if(hit!=null)hit.requestRectangleOnScreen(new android.graphics.Rect(0,0,hit.getWidth(),hit.getHeight()),true);}});settle();
        tap(await(v->v instanceof Button&&v.isEnabled()&&v.isClickable()&&((Button)v).getText().toString().startsWith(name)));
    }
    private void humanInput()throws Exception{
        if("swap-critical".equals(humanStrategy)){
            var f=facts();var ordinary=f.nativeDuel.choices.stream().filter(c->c.enabled()&&c.special==-1&&c.replacement==-1).collect(java.util.stream.Collectors.toList());var choice=ordinary.stream().filter(c->c.stance==0).findFirst().orElseGet(()->ordinary.stream().filter(c->c.stance==-1).findFirst().orElseThrow());
            var team=f.nativeDuel.teams.stream().filter(t->t.human).findFirst().orElseThrow();var guan=team.fighters.stream().filter(v->v.officerId==1001&&v.nativeId==98).findFirst().orElseThrow();
            if(!guan.active){var swap=f.nativeDuel.choices.stream().filter(c->c.enabled()&&c.replacement==guan.slot).findFirst();if(swap.isPresent())choice=swap.get();}else {var critical=f.nativeDuel.choices.stream().filter(c->c.enabled()&&c.special==0).findFirst();if(critical.isPresent())choice=critical.get();}
            note("actual swap/critical round="+f.round+" phase="+f.phase+" stance="+choice.stance+" special="+choice.special+" replacement="+choice.replacement);pageButton(f.phase==3&&choice.special>=0?"选择特殊动作":choice.label);var after=facts();check(after.contestId==f.contestId&&after.revision>f.revision,"real enabled swap/special button original model once");return;
        }
        var f=facts();var retreat=f.nativeDuel.choices.stream().filter(c->c.enabled()&&c.special==3).findFirst();
        if("retreat".equals(humanStrategy)&&!retreatSelected&&retreat.isPresent()){
            pageButton(f.phase==3?"选择特殊动作":retreat.get().label);
            if(f.phase==7)retreatSelected=true;
            note("actual legal retreat input phase="+f.phase+" round="+f.round+" selected="+retreatSelected);
        }else{
            var choices=f.nativeDuel.choices.stream().filter(c->c.enabled()&&c.special==-1&&c.replacement==-1).collect(java.util.stream.Collectors.toList());
            var choice=choices.stream().filter(c->c.stance==(!"attack".equals(humanStrategy)?2:0)).findFirst().orElseGet(()->choices.stream().filter(c->c.stance==-1).findFirst().orElseThrow());pageButton(choice.label);
        }
        var after=facts();check(after.contestId==f.contestId&&after.revision>f.revision,"real enabled human button advances original model once");
    }
    private void adoptPhysicalRecovery()throws Exception{
        var initial=facts();byte[] before=capture();var token=activity.deploymentState();check(initial.nativeDuel.physicalRecovery!=null&&!initial.nativeDuel.physicalRecovery.enabled&&initial.nativeDuel.physicalRecovery.adoptionAvailable,"actual old recovery absent/explicit available");
        pageButton("采用原逐旬体力恢复");text("取消");check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"actual physical recovery modal cancel all World/dual RNG/token pure");pageButton("采用原逐旬体力恢复");tap(await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText())),2);var after=facts();check(after.nativeDuel.physicalRecovery.enabled&&!after.nativeDuel.physicalRecovery.adoptionAvailable&&after.revision==initial.revision+1&&activity.deploymentState().revision==token.revision+1,"actual double adopts future recovery once");World old=SaveCodec.decode(before),now=SaveCodec.decode(capture());check(Arrays.equals(SessionBPolicyProbe.numericalModel(old),SessionBPolicyProbe.numericalModel(now))&&old.strategy.getRandomState()==now.strategy.getRandomState(),"actual physical recovery adoption model/dual RNG unchanged");for(var source:PcDuelSourceFacts.saved(old).values())check(old.contests.nativePhysicalHealth(source.officerId)==now.contests.nativePhysicalHealth(source.officerId),"actual adoption no immediate HP "+source.officerId);shot("physical-recovery-adopted");
    }
    private void verifyPhysicalRecovery(int elapsed)throws Exception{
        World w=SessionProbe.view(activity);var source=PcDuelSourceFacts.saved(w).values().stream().filter(v->v.nativeId==660).findFirst().orElseThrow();int hp=w.contests.nativePhysicalHealth(source.officerId);check(hp==Math.min(100,36+elapsed*30),"actual source660 physical recovery fullturn "+elapsed+" hp="+hp);note("actual physical recovery turn="+w.turn+" stable="+source.officerId+" native="+source.nativeId+" name="+w.officer(source.officerId).name+" hp="+hp);Files.write(new File(evidence,"physical-turn-"+elapsed+".sg11").toPath(),capture());if(elapsed==3){var own=w.unit(1);var enemy=w.units.stream().filter(u->w.army.contains(u,source.officerId)).findFirst().orElse(null);check(own!=null&&enemy!=null&&w.contests.duelError(own.id,enemy.id)==null,"actual recovered opposing units legal adjacent normal challenge");var candidates=w.contests.nativeDuelCandidates(own.id,enemy.id);check(candidates.stream().anyMatch(c->c.chance>0),"actual low-health0 response blocker restored lawful candidates");note("restored actual response="+candidates.stream().map(c->c.officerId+":"+c.chance).collect(java.util.stream.Collectors.joining(",")));}shot("physical-recovery-turn-"+elapsed);
    }
    private void adoptLoyaltyInput()throws Exception{
        var initial=facts();byte[] before=capture();var token=activity.deploymentState();
        check(initial.nativeDuel.terminal&&!initial.nativeDuel.loyaltyInputEnabled&&initial.nativeDuel.loyaltyInputAdoptionAvailable,"old normal loss has explicit adoption option, no automatic upgrade");
        pageButton("采用已核实的忠诚输入解析");text("取消");
        check(token.equals(activity.deploymentState())&&Arrays.equals(before,capture()),"actual policy modal cancellation full World/model/dual RNG/token pure");
        pageButton("采用已核实的忠诚输入解析");tap(await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText())),2);settle();
        var adopted=facts();check(adopted.nativeDuel.loyaltyInputEnabled&&!adopted.nativeDuel.loyaltyInputAdoptionAvailable&&activity.deploymentState().revision==token.revision+1&&adopted.revision==initial.revision+1,"actual double confirmation explicitly adopts exactly once");
        World old=SaveCodec.decode(before),now=SaveCodec.decode(capture());
        check(Arrays.equals(SessionBPolicyProbe.numericalModel(old),SessionBPolicyProbe.numericalModel(now)),"actual adoption original numerical model and native RNG unchanged");
        check(Arrays.equals(old.extensions.get("pc-duel-raw-loyalty-v1"),now.extensions.get("pc-duel-raw-loyalty-v1")),"actual adoption preserves unknown PDL1 raw/trust bytes");
        for(var officer:old.officers)check(now.officer(officer.id).loyalty==officer.loyalty,"actual officer loyalty unchanged "+officer.id);
        shot("explicit-loyalty-input-adopted");
    }
    private void adoptAiActor()throws Exception{
        var original=facts();byte[] before=capture();var token=activity.deploymentState();
        check(original.nativeDuel.aiActorNativeId==189&&!original.nativeDuel.aiActorPolicyEnabled&&original.nativeDuel.aiActorPolicyAdoptionAvailable,"actual old AI unit head189 and explicit adoption option");
        pageButton("采用原AI势力君主绑定");text("取消");check(token.equals(activity.deploymentState())&&Arrays.equals(before,capture()),"real AI policy modal cancel preserves allWorld/model/dual RNG/token");
        pageButton("采用原AI势力君主绑定");tap(await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText())),2);settle();
        var after=facts();check(after.nativeDuel.aiActorPolicyEnabled&&!after.nativeDuel.aiActorPolicyAdoptionAvailable&&after.nativeDuel.aiActorNativeId==91&&activity.deploymentState().revision==token.revision+1&&after.revision==original.revision+1,"actual double confirmation chooses authoritative force ruler91 exactly once");
        World old=SaveCodec.decode(before),now=SaveCodec.decode(capture());check(Arrays.equals(SessionBPolicyProbe.numericalModel(old),SessionBPolicyProbe.numericalModel(now))&&Arrays.equals(old.extensions.get("pc-duel-raw-loyalty-v1"),now.extensions.get("pc-duel-raw-loyalty-v1"))&&old.strategy.getRandomState()==now.strategy.getRandomState(),"actual AI adoption preserves original numerical model/dual RNG/raw unknown");
        check(old.unit(25).officerId==now.unit(25).officerId&&old.officer(10091).cityId==now.officer(10091).cityId,"ruler binding does not move officers or change unit captain");shot("original-ai-force-ruler-adopted");
    }
    private void adoptHumanActor()throws Exception{
        var original=facts();byte[] before=capture();var token=activity.deploymentState();
        check(original.nativeDuel.humanActorNativeId==503&&!original.nativeDuel.humanActorPolicyEnabled&&original.nativeDuel.humanActorPolicyAdoptionAvailable,"actual old human commander503 and explicit adoption option");
        pageButton("采用原人工登用君主绑定");text("取消");check(token.equals(activity.deploymentState())&&Arrays.equals(before,capture()),"human policy cancel whole World/model/dual RNG/token pure");
        pageButton("采用原人工登用君主绑定");tap(await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText())),2);settle();
        var after=facts();check(after.nativeDuel.humanActorPolicyEnabled&&!after.nativeDuel.humanActorPolicyAdoptionAvailable&&after.nativeDuel.humanActorNativeId==403&&activity.deploymentState().revision==token.revision+1&&after.revision==original.revision+1,"double human confirmation current force ruler403 exactly once");
        World old=SaveCodec.decode(before),now=SaveCodec.decode(capture());check(Arrays.equals(SessionBPolicyProbe.numericalModel(old),SessionBPolicyProbe.numericalModel(now))&&Arrays.equals(old.extensions.get("pc-duel-raw-loyalty-v1"),now.extensions.get("pc-duel-raw-loyalty-v1"))&&old.strategy.getRandomState()==now.strategy.getRandomState(),"human binding numerical model/dual RNG/raw unknown unchanged");
        check(old.unit(16).officerId==now.unit(16).officerId&&old.officer(10403).cityId==now.officer(10403).cityId,"human actor binding preserves deployment and ruler location");shot("original-human-force-ruler-adopted");
    }
    private void adoptRecruitItemRecipient()throws Exception{
        var initial=facts();byte[] before=capture();var token=activity.deploymentState();check(!initial.nativeDuel.recruitItemPolicyEnabled&&initial.nativeDuel.recruitItemPolicyAdoptionAvailable&&initial.nativeDuel.recruitItemRecipientNativeId==503,"actual legacy item holder and explicit option");
        pageButton("采用原登用宝物归属");text("取消");check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"actual item policy modal cancel whole World/RNG/token pure");
        pageButton("采用原登用宝物归属");tap(await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText())),2);settle();var adopted=facts();check(adopted.nativeDuel.recruitItemPolicyEnabled&&!adopted.nativeDuel.recruitItemPolicyAdoptionAvailable&&adopted.nativeDuel.recruitItemRecipientNativeId==403&&adopted.revision==initial.revision+1&&activity.deploymentState().revision==token.revision+1,"actual item policy double confirmation changes binding exactly once");
        World old=SaveCodec.decode(before),now=SaveCodec.decode(capture());check(Arrays.equals(SessionBPolicyProbe.numericalModel(old),SessionBPolicyProbe.numericalModel(now))&&old.strategy.getRandomState()==now.strategy.getRandomState(),"adoption original numerical model and dual RNG unchanged");for(var item:old.treasures.items()){var current=now.treasures.item(item.definition.id);check(current.place==item.place&&current.holder==item.holder,"actual item adoption no immediate transfer "+item.definition.id);}shot("original-recruit-item-recipient-adopted");
    }
    private void finishBattle()throws Exception{
        boolean recruitmentAttempted=false;int inputs=0;while(facts().nativeDuel!=null&&!facts().nativeDuel.terminal&&inputs++<300)humanInput();
        var terminal=facts();check(terminal.nativeDuel!=null&&terminal.nativeDuel.terminal,"actual human normal terminal");shot("human-terminal");Files.write(new File(evidence,"terminal.sg11").toPath(),capture());
        for(int step=0;step<20&&facts().nativeDuel!=null;step++){
            var f=facts();
            if(f.nativeDuel.inheritance!=null&&f.nativeDuel.inheritance.selectedHeir<0){var heir=f.nativeDuel.inheritance.candidates.stream().filter(h->h.enabled()).findFirst().orElseThrow();pageButton("继承 · "+heir.name);text("执行");continue;}
            var pending=f.nativeDuel.disposition.stream().filter(r->r.choice==4).findFirst().orElse(null);
            if(pending!=null){
                if("recruit".equals(humanDisposition)&&!recruitmentAttempted){
                    check(pending.enabled(0),"actual original recruitment preview legal "+pending);byte[] before=capture();
                    pageButton("登用");recruitmentAttempted=true;var selected=facts();
                    check(selected.revision==f.revision+1,"normal recruitment consumes original attempt once");recruited|=selected.nativeDuel.disposition.stream().anyMatch(r->r.officerId==pending.officerId&&r.choice==0);
                    Files.write(new File(evidence,"recruitment-attempt.sg11").toPath(),capture());note("original recruitment target="+pending.officerId+" result="+selected.nativeDuel.disposition.get(0).choice+" mask="+selected.nativeDuel.disposition.get(0).mask);shot("original-recruitment-attempt");
                    byte[] attempt=capture();save(attempt);load();check(Arrays.equals(attempt,capture()),"successful or failed recruitment whole save/manual load exact");nav("菜单");pageButton("继续当前对局");continue;
                }
                int action=-1;for(int candidate:new int[]{1,2,3})if(pending.enabled(candidate)){action=candidate;break;}check(action>=0,"actual terminal verified disposition available");pageButton(new String[]{"登用","拘留","释放","处斩"}[action]);continue;}
            check(f.settlementAvailable,"actual terminal settlement allowed "+f.nativeDuel.settlementError);
            pageButton(f.nativeDuel.disposition.isEmpty()?"查看战役结果":"确认处置并结算");
        }
        check(facts().nativeDuel==null,"actual terminal returns to normal campaign once");
        Files.write(new File(evidence,"normal-settled.sg11").toPath(),capture());
        note("actual terminal contest="+terminal.contestId+" humanInputs="+inputs+" strategy="+humanStrategy+" outcome not forced");shot("normal-settled");
    }
    private void save(byte[] expected)throws Exception{boolean existed=new File(activity.getFilesDir(),"manual3.sg11").exists();nav("菜单");text("保存局面");preparedOption("槽位 3");if(existed)text("覆盖存档");check(Arrays.equals(expected,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"ordinary manual save complete exact bytes");}
    private void load()throws Exception{nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));}
}

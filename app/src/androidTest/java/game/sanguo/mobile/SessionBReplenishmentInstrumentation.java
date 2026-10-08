package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/** Actual source menu/deployment/map/government widgets, no constructed World. */
public final class SessionBReplenishmentInstrumentation extends SessionBFieldworksInstrumentation {
    private void quantity(String name,String value)throws Exception{revealDescription(name);EditText e=(EditText)await(v->v instanceof EditText&&name.equals(v.getContentDescription()));invoke("enter",new Class<?>[]{EditText.class,String.class},e,value);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();}
    private void save(byte[] expected)throws Exception{boolean existed=new File(activity.getFilesDir(),"manual3.sg11").exists();nav("菜单");text("保存局面");preparedOption("槽位 3");if(existed)text("覆盖存档");check(Arrays.equals(expected,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"manual slot exact whole World/dual RNG");}
    private void load()throws Exception{nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();}
    private void openSupply(int actor)throws Exception{
        World w=SessionProbe.view(activity);World.City c=w.city(20008);
        nav("地图");revealDescription("定位己方据点 "+c.name);ClientState ui=(ClientState)field(activity,"ui");if(!ui.panelVisible)description("选中对象指令 ·");if(!ui.panelExpanded)text("展开");text("概览");
        runOnMainSync(()->{View hit=search(activity.getWindow().getDecorView(),v->v instanceof Button&&((Button)v).getText().toString().startsWith("军政 / 俘虏 / 官职"));if(hit!=null)hit.requestRectangleOnScreen(new android.graphics.Rect(0,0,hit.getWidth(),hit.getHeight()),true);});settle();
        text("军政 / 俘虏 / 官职");option("补给城外部队");option(w.officer(10365).name+" · 兵");option(w.officer(actor).name);
    }
    private List<String> visibleChoices()throws Exception{
        List<String> rows=new ArrayList<>();runOnMainSync(()->{for(View root:android.view.inspector.WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus())collect(root,rows);});return rows;
    }
    private void collect(View v,List<String> rows){if(!v.isShown())return;if(v instanceof TextView)rows.add(((TextView)v).getText().toString());if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),rows);}
    @Override public void onStart(){Bundle result=new Bundle();try{
        evidence=new File(getTargetContext().getExternalFilesDir("session-b"),coldMode?"replenishment-cold":"replenishment-normal");evidence.mkdirs();put("output",evidence);
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));put("activity",activity);
        awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
        if(coldMode){
            byte[] expected=Files.readAllBytes(new File(getTargetContext().getExternalFilesDir("session-b"),"replenishment-normal/after.sg11").toPath());
            check(Arrays.equals(expected,capture()),"new process auto-resumes exact complete actual supply campaign/dual RNG");load();check(Arrays.equals(expected,capture()),"ordinary manual cold load whole World/dual RNG");shot("cold-full-world");
        }else{
            var source=PcScenarioCatalog.all().get(0);byte[] original=capture();nav("菜单");text("新游戏 / 选择势力");revealDescription("选择PC来源剧本 "+source.identity.path);
            awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力"));revealDescription("选择势力 · "+PcScenarioCatalog.preview(source.identity.scenarioId).faction(2));configurePcOpening(source.identity.scenarioId,0,2,0);
            description("确认开局势力");text("开始新局");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
            World w=SessionProbe.view(activity);check(w.player==2&&w.scenarioId.equals(source.identity.scenarioId)&&!Arrays.equals(original,capture()),"ordinary Source0 Sun faction new game, actual menu seed");
            int[] crew={10365,10116,10466};int[] natives={365,116,466};for(int i=0;i<crew.length;i++)check(PcDuelSourceFacts.saved(w).get(crew[i]).nativeId==natives[i],"exact source stable/native join");
            World.City city=w.city(20008);nav("地图");revealDescription("定位己方据点 "+city.name);byte[] opening=capture();text("出征");ListView roster=(ListView)tag("deploy.officers");
            for(int id:crew){invoke("showRosterTag",new Class<?>[]{ListView.class,String.class},roster,"deploy.role."+id);tap(tag("deploy.role."+id));}
            tap(tag("deploy.tab.1"));description("剑兵 无需库存");quantity("兵力数量","6000");quantity("粮食数量","35000");quantity("金钱数量","1500");check(Arrays.equals(opening,capture()),"ordinary deployment preview pure");long revision=activity.deploymentState().revision;tap(tag("deploy.confirm"),2);settle();
            w=SessionProbe.view(activity);World.Unit u=w.unit(w.officer(10365).unitId);check(u!=null&&u.weapon==World.Weapon.SWORD&&u.troops==6000&&u.food==35000&&u.gold==1500&&Arrays.equals(u.deputies,new int[]{10116,10466})&&activity.deploymentState().revision==revision+1,"real three-person deployment exact quantities and double-click once");int id=u.id;
            selectUnit(id);pose(city.hex);text("行军");tile(city.hex);text("确认任务");settle();w=SessionProbe.view(activity);u=w.unit(id);city=w.city(20008);
            check(u!=null&&u.hex.equals(city.hex)&&w.army.canEnterSite(u,u.hex,city)&&city.food==3000,"ordinary map movement to legal seven-cell site station, actual stock3000");
            int actor=w.idle(city).get(0).id;byte[] before=capture();Files.write(new File(evidence,"before.sg11").toPath(),before);openSupply(actor);
            await(v->v instanceof TextView&&((TextView)v).getText().toString().contains("据点兵粮不足：现有3000，需要5000"));shot("unavailable-5000-reason");text("取消");check(Arrays.equals(before,capture()),"real supply overview cancel pure whole World/dual RNG");
            openSupply(actor);text("选择可用数量");List<String> choices=visibleChoices();check(choices.contains("0兵 / 1000粮")&&choices.contains("1000兵 / 1000粮")&&!choices.contains("0兵 / 5000粮")&&!choices.contains("3000兵 / 5000粮"),"actual picker offers only same-rule legal quantities");option("1000兵 / 1000粮");shot("legal-confirmation");text("取消");check(Arrays.equals(before,capture()),"real legal confirmation cancel pure");
            openSupply(actor);text("选择可用数量");option("1000兵 / 1000粮");View execute=await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText()));revision=activity.deploymentState().revision;tap(execute,2);settle();
            byte[] after=capture();World old=SaveCodec.decode(before);w=SessionProbe.view(activity);World.City c=w.city(20008);u=w.unit(id);ReplenishmentPlan p=old.supply.replenishPlan(20008,actor,id,1000,1000);
            check(p.allowed()&&p.equipmentCost==0&&activity.deploymentState().revision==revision+1&&c.troops==old.city(20008).troops-1000&&c.food==old.city(20008).food-1000&&Arrays.equals(c.equipment,old.city(20008).equipment)&&u.troops==old.unit(id).troops+1000&&u.food==old.unit(id).food+1000&&u.gold==old.unit(id).gold&&w.officer(actor).acted&&u.acted==old.unit(id).acted,"actual widget supply exact physical debit/credit/AP actor, double once");
            check(old.supply.replenish(20008,actor,id,1000,1000).ok&&Arrays.equals(SaveCodec.encode(old),after),"actual widget matches complete ordinary core result including all reports/dual RNG");
            save(after);load();check(Arrays.equals(after,capture()),"normal manual load exact actual supply world/dual RNG");Files.write(new File(evidence,"after.sg11").toPath(),after);shot("after-supply-save");
        }
        result.putString("stream","PASS SESSION B REPLENISHMENT"+(coldMode?" COLD":"")+" normal source/newgame/deploy/map/government widgets/fullWorld dualRNG; ARM and numerical PC supply parity pending\n");
    }catch(Throwable t){result.putString("stream","FAIL SESSION B REPLENISHMENT "+android.util.Log.getStackTraceString(t));try{Files.write(new File(evidence,"failed.sg11").toPath(),capture());shot("failed");}catch(Throwable ignored){}
    }finally{try{if(activity!=null)runOnMainSync(activity::finish);settle();}catch(Throwable t){result.putString("stream",result.getString("stream")+"\nFAIL lifecycle "+t);}}
    try{if(evidence!=null)Files.write(new File(evidence,"result.txt").toPath(),result.getString("stream").getBytes("UTF-8"));}catch(Exception ignored){}finish(Activity.RESULT_OK,result);}
}

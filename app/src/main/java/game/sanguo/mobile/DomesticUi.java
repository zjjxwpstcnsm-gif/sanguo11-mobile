package game.sanguo.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Consumer;

/** Native command forms; every confirmed mutation goes through the validated core. */
final class DomesticUi {
    private final MainActivity activity;private final World w;private final LegacyCommandSink apply;private final Consumer<Hex> focus;
    DomesticUi(MainActivity a,World w,LegacyCommandSink apply,Consumer<Hex> focus){activity=a;this.w=w;this.apply=apply;this.focus=focus;}
    private void message(String title,String text){UiTheme.dialog(new AlertDialog.Builder(activity).setTitle(title).setMessage(text).setPositiveButton("返回",null).show());}
    private void confirm(String title,String text,String positive,Runnable run){activity.commandDialog(title,text,positive,w,run);}
    private void officer(World.City c,Consumer<World.Officer> next){DataTable.choose(activity,w,"执行武将",w.idle(c),o->"",next,null);}
    private void destination(int owner,int excluded,Consumer<World.City> next){
        List<World.City> options=new ArrayList<>();for(World.City c:w.cities)if(c.owner==owner&&c.id!=excluded)options.add(c);
        ChoiceDialog.show(activity,w,"选择目的地",options,c->c.name+" · "+w.personnel.description(excluded,c.id),next);
    }
    void build(World.City c){
        List<Hex> sites=w.domestic.buildSites(c.id);
        if(sites.isEmpty()){message("无法开发","没有可用开发地或已达到本城设施上限。");return;}
        activity.pickOnMap("设施开发 · 选择开发地",c.hex,sites,h->{activity.cancelMapPick();BuildPicker.open(activity,w,c,h);});
    }

    void transfer(World.City c){destination(c.owner,c.id,d->officer(c,o->confirm("人员调动",o.name+"："+c.name+" → "+d.name+"\n"+w.personnel.description(c.id,d.id)+"\n行动力10；在途期间不可执行其他命令。","出发",()->apply.execute(w,()->w.domestic.transfer(c.id,d.id,o.id)))));}
    void transport(World.City c){destination(c.owner,c.id,d->cargo(c,d,false));}
    void transportSea(World.City c){destination(c.owner,c.id,d->cargo(c,d,true));}
    void restoreDraft(Bundle draft){World.City c=w.city(draft.getInt("city")),d=w.city(draft.getInt("target"));if(c==null||d==null||c.owner!=w.player||d.owner!=w.player){activity.closeForm();return;}cargo(c,d,draft.getBoolean("sea"));}
    private void cargo(World.City c,World.City d,boolean sea){
        Bundle previous=activity.formDraft();
        Bundle draft="cargo".equals(previous.getString("kind"))&&previous.getInt("city")==c.id&&previous.getInt("target")==d.id?new Bundle(previous):CargoWizard.start(c,d,sea);
        new CargoWizard(activity,w,draft).show();
    }
    void overview(){
        List<String> labels=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        for(World.City c:w.cities)if(c.owner==w.player){labels.add(c.name+" · 设施"+w.domestic.count(c.id)+"/"+w.development.capacity(c.id)+" · 月金"+w.domestic.monthlyGold(c.id)+" / 季粮"+w.domestic.monthlyFood(c.id));actions.add(()->focus.accept(c.hex));}
        for(World.City c:w.cities)if(c.owner==w.player){labels.add(c.name+" · 人事 / 城市治理 / 武将状态");actions.add(()->new StrategyUi(activity,w,apply).city(c));}
        for(Domestic.Facility f:w.domestic.facilities)if(w.city(f.cityId).owner==w.player){labels.add(w.city(f.cityId).name+" · "+f.kind.label+" · "+(f.remaining==0?"已建成":"剩"+f.remaining+"旬"));actions.add(()->facility(f));}
        for(Domestic.Mission m:w.domestic.missions)if(m.owner==w.player){labels.add((m.transport?"运输":"调动")+" · "+w.officer(m.officerId).name+" → "+w.city(m.targetCity).name+" · "+w.domestic.status(m));actions.add(()->mission(m));}
        UiTheme.dialog(new AlertDialog.Builder(activity).setTitle("政务与在途 · 点击详情").setItems(labels.toArray(new String[0]),(d,i)->actions.get(i).run()).setNegativeButton("返回",null).show());
    }
    void facility(Domestic.Facility f){
        focus.accept(f.hex);World.City c=w.city(f.cityId);
        AlertDialog.Builder dialog=new AlertDialog.Builder(activity).setTitle(c.name+" · "+f.kind.label+" Lv"+f.level).setMessage("耐久 "+f.hp+"/"+f.maxHp()+"\n"+(f.level==3?Domestic.buildEffect(f.kind):f.kind.effect)+"\n建设等级与工期请在开工前查看；已有设施等级保留。\n"+(f.remaining==0?"已建成":w.officer(f.builderId).name+(f.upgradeTo>0?"合并中":"建设中")+"，剩"+f.remaining+"旬")+"\n坐标 "+f.hex.q+", "+f.hex.r).setNegativeButton("返回",null);
        if(c.owner==w.player&&!w.gameOver()){
            if(f.remaining>0)dialog.setPositiveButton("取消建设",(d,n)->confirm("取消建设","不会退还建设费用，本旬不能重复使用武将。","确定取消",()->apply.execute(w,()->w.domestic.cancelBuild(f.id))));
            else dialog.setPositiveButton("拆除",(d,n)->officer(c,o->confirm("拆除设施","需要一名闲置武将与行动力10，不退还费用。","确定拆除",()->apply.execute(w,()->w.domestic.demolish(f.id,o.id)))));
        }
        UiTheme.dialog(dialog.show());
    }
    void mission(Domestic.Mission m){
        focus.accept(m.hex);StringBuilder detail=new StringBuilder(w.officer(m.officerId).name+"\n"+w.city(m.sourceCity).name+" → "+w.city(m.targetCity).name+"\n"+w.domestic.status(m)+"\n当前坐标 "+m.hex.q+", "+m.hex.r);
        detail.append("\n编队武将：");for(int id:m.crew())detail.append(w.officer(id).name).append(" ");
        if(m.transport){detail.append("\n金 ").append(m.gold).append(" / 粮 ").append(m.food).append(" / 兵 ").append(m.troops);for(int i=0;i<m.equipment.length;i++)detail.append('\n').append(World.Weapon.values()[i].label).append("兵装 ").append(m.equipment[i]);}
        if(m.transport)detail.append("\n舰船货物：楼船 ").append(m.cargoShips[0]).append(" / 斗舰 ").append(m.cargoShips[1]);
        detail.append("\n累计途中耗粮："+m.consumedFood+"；"+(m.returnOfficers?"卸货后人员返程":m.returning?"仅人员返程，无返程物资":"抵达留驻"));
        detail.append("\n\n运输队可被截击；城内受城防保护。目的地失守自动选择可达己城；无路则等待。满仓保留货物，下一旬重试。");
        AlertDialog.Builder d=new AlertDialog.Builder(activity).setTitle(m.transport?"运输详情":"调动详情").setMessage(detail).setNegativeButton("返回",null);
        if(!w.gameOver())d.setPositiveButton("改道 / 返回",(dialog,n)->destination(m.owner,m.targetCity,c->confirm("任务改道","改道至"+c.name+"，消耗行动力10。","执行",()->apply.execute(w,()->w.domestic.redirect(m.id,c.id)))));
        UiTheme.dialog(d.show());
    }
}

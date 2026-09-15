package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** Native foreign affairs forms, with read-only previews and explicit final confirmation. */
final class DiplomacyUi {
    private final Activity a;private final World w;private final Consumer<World.Result> apply;
    DiplomacyUi(Activity a,World w,Consumer<World.Result> apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String text){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private <T> void choose(String title,List<T> options,Function<T,String> label,Consumer<T> next){
        if(options.isEmpty()){info(title,"没有符合条件的选项。");return;}
        String[] names=new String[options.size()];for(int i=0;i<names.length;i++)names[i]=label.apply(options.get(i));
        AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(title).setNegativeButton("取消",null);
        if(GameIcon.supports(options.get(0)))dialog.setAdapter(GameIcon.adapter(a,w,options,label),(d,i)->next.accept(options.get(i)));
        else dialog.setItems(names,(d,i)->next.accept(options.get(i)));
        dialog.show();
    }
    private void confirm(String title,String error,String text,Runnable action){
        if(error!=null){info(title,error);return;}
        new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,i)->action.run()).setNegativeButton("取消",null).show();
    }
    void surrender(World.City c,World.Officer o,int side){
        confirm("劝降 · "+w.faction(side),w.diplomacy.surrenderError(c.id,o.id,side),
            "使者 "+o.name+" · 魅力"+o.charm+"\n成功率 "+w.diplomacy.surrenderChance(o.id,side)+"% · 行动力30\n城市 "+w.diplomacy.cityCount(c.owner)+" 对 "+w.diplomacy.cityCount(side)+" · 军力 "+w.diplomacy.strength(c.owner)+" 对 "+w.diplomacy.strength(side)+
            "\n成功接收全部据点、武将、部队、宝物与库存。在途运输和军备制造继续，未完成研究与培养中止；新归顺部队本旬不能再行动。\n拒绝消耗行动力，关系下降10。",()->apply.accept(w.diplomacy.surrender(c.id,o.id,side)));
    }
    void aid(World.City c,World.Officer o,int side){
        List<World.City> sources=new ArrayList<>(),targets=new ArrayList<>();
        for(World.City t:w.cities){if(t.owner==side)sources.add(t);if(t.owner>=0&&w.campaign.hostile(c.owner,t.owner)&&w.campaign.hostile(side,t.owner))targets.add(t);}
        choose("盟军出兵据点",sources,t->t.name+" · 兵"+t.troops+" / 粮"+t.food,source->choose("选择攻略据点",targets,t->t.name+" · "+w.faction(t.owner),target->
            choose("援军礼金",Arrays.asList(0,1000,3000,5000),gold->"金"+gold,gold->{
                String error=w.diplomacy.aidError(c.id,o.id,source.id,target.id,gold);CampaignAi.Deployment plan=error==null?w.diplomacy.plannedAid(source.id,target.id):null;
                confirm("请求援军",error,"成功率 "+w.diplomacy.aidChance(o.id,side,gold)+"% · 行动力30\n接受后支付金"+gold+"，拒绝保留礼金。\n"+
                    (plan==null?"":w.officer(plan.leader).name+" · 预计"+plan.troops+plan.weapon.label+"，守城留兵至少"+plan.reserve+"\n")+
                    "盟军下一次行动出征，保留兵力和指挥权。期限18旬；目标易主、同盟结束或期满后归城。出兵条件变化时等待，并可在援军任务中查看。",()->apply.accept(w.diplomacy.requestAid(c.id,o.id,source.id,target.id,gold)));
            })));
    }
    void exchange(World.City c,World.Officer o,int side){
        List<Integer> wanted=new ArrayList<>(),offered=new ArrayList<>();offered.add(-1);
        for(Government.Prisoner p:w.government.prisoners()){
            if(p.captor==side&&w.officer(p.officerId).owner==c.owner)wanted.add(p.officerId);
            if(p.captor==c.owner&&p.cityId==c.id&&p.unitId<0&&w.officer(p.officerId).owner==side)offered.add(p.officerId);
        }
        choose("选择赎回武将",wanted,id->w.officer(id).name+" · "+w.government.locationLabel(w.government.prisoner(id)),target->
            choose("选择交换俘虏",offered,id->id<0?"仅提供金":w.officer(id).name,gift->
                choose("交换金",Arrays.asList(0,1000,3000,5000),gold->"金"+gold,gold->confirm("交换俘虏",w.diplomacy.exchangeError(c.id,o.id,target,gift,gold),
                    "赎回 "+w.officer(target).name+"\n提供 "+(gift<0?"无交换武将":w.officer(gift).name)+" / 金"+gold+
                    "\n成功率 "+w.diplomacy.exchangeChance(o.id,target,gift,gold)+"% · 行动力30\n接受后同时交割，武将返回各自据点；拒绝消耗行动力，金与俘虏保留。",()->apply.accept(w.diplomacy.exchange(c.id,o.id,target,gift,gold))))));
    }
    void missions(int side){
        List<Diplomacy.Aid> options=new ArrayList<>();for(Diplomacy.Aid aid:w.diplomacy.aids())if(aid.requester==w.active&&aid.ally==side)options.add(aid);
        choose("援军任务",options,x->w.diplomacy.describe(x),x->{
            AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle("援军任务").setMessage(w.diplomacy.describe(x)).setPositiveButton("返回",null);
            if(!x.returning)dialog.setNeutralButton("结束援军",(d,i)->confirm("结束援军",null,"援军将返回盟军据点，已交付礼金不退还。",()->apply.accept(w.diplomacy.cancelAid(x.source))));
            dialog.show();
        });
    }
}

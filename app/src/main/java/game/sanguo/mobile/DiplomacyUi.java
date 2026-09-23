package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** Native foreign affairs forms, with read-only previews and explicit final confirmation. */
final class DiplomacyUi {
    private final Activity a;private final World w;private final LegacyCommandSink apply;
    DiplomacyUi(Activity a,World w,LegacyCommandSink apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String text){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private <T> void choose(String title,List<T> values,java.util.function.Function<T,String> label,Consumer<T> next){ChoiceDialog.show(a,w,title,values,label,next);}
    private void confirm(String title,String error,String text,Runnable action){
        if(error!=null){info(title,error);return;}
        new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,i)->action.run()).setNegativeButton("取消",null).show();
    }
    void surrender(World.City c,World.Officer o,int side){
        confirm("劝降 · "+w.faction(side),w.diplomacy.surrenderError(c.id,o.id,side),
            w.personnel.description(c.id,w.personnel.destination(side))+"\n使者 "+o.name+" · 魅力"+o.charm+"\n成功率 "+w.diplomacy.surrenderChance(o.id,side)+"% · 行动力30\n城市 "+w.diplomacy.cityCount(c.owner)+" 对 "+w.diplomacy.cityCount(side)+" · 军力 "+w.diplomacy.strength(c.owner)+" 对 "+w.diplomacy.strength(side)+
            "\n成功接收全部据点、武将、部队、宝物与库存。在途运输和军备制造继续，未完成研究与培养中止；新归顺部队本旬不能再行动。\n拒绝消耗行动力，关系下降10。",()->apply.execute(w,()->w.diplomacy.surrender(c.id,o.id,side)));
    }
    void aid(World.City c,int side){
        List<World.City> sources=new ArrayList<>(),targets=new ArrayList<>();for(World.City t:w.cities){if(t.owner==side)sources.add(t);if(t.owner>=0&&w.campaign.hostile(c.owner,t.owner)&&w.campaign.hostile(side,t.owner))targets.add(t);}
        choose("盟军出兵据点",sources,t->t.name+" · 兵"+t.troops,source->choose("选择攻略据点",targets,t->t.name+" · "+w.faction(t.owner),target->choose("援军礼金",Arrays.asList(0,1000,3000,5000),gold->"金"+gold,gold->choose("选择执行武将",w.idle(c),o->o.name,o->confirm("请求援军",w.diplomacy.aidError(c.id,o.id,source.id,target.id,gold),
            "成功率 "+w.diplomacy.aidChance(o.id,side,gold)+"% · 行动力30\n"+w.personnel.description(c.id,source.id)+"\n礼金"+gold+"出发时预留，接受后交付，拒绝后返还；抵达交涉后盟军执行出征。援军期限18旬。",()->apply.execute(w,()->w.diplomacy.requestAid(c.id,o.id,source.id,target.id,gold)))))));
    }
    void exchange(World.City c,int side){
        List<Integer> wanted=new ArrayList<>(),offered=new ArrayList<>();offered.add(-1);for(Government.Prisoner p:w.government.prisoners()){if(p.captor==side&&w.officer(p.officerId).owner==c.owner)wanted.add(p.officerId);if(p.captor==c.owner&&p.cityId==c.id&&p.unitId<0&&w.officer(p.officerId).owner==side)offered.add(p.officerId);}
        choose("选择赎回武将",wanted,id->w.officer(id).name,target->choose("选择交换俘虏",offered,id->id<0?"仅提供金":w.officer(id).name,gift->choose("交换金",Arrays.asList(0,1000,3000,5000),gold->"金"+gold,gold->choose("选择执行武将",w.idle(c),o->o.name,o->confirm("交换俘虏",w.diplomacy.exchangeError(c.id,o.id,target,gift,gold),
            "赎回 "+w.officer(target).name+"；提供 "+(gift<0?"无交换武将":w.officer(gift).name)+" / 金"+gold+"\n成功率 "+w.diplomacy.exchangeChance(o.id,target,gift,gold)+"% · 行动力30\n"+w.personnel.description(c.id,w.personnel.destination(side))+"\n金在出发时预留，抵达后交割；被拒绝则返还。",()->apply.execute(w,()->w.diplomacy.exchange(c.id,o.id,target,gift,gold)))))));
    }
    void missions(int side){
        List<Diplomacy.Aid> options=new ArrayList<>();for(Diplomacy.Aid aid:w.diplomacy.aids())if(aid.requester==w.active&&aid.ally==side)options.add(aid);
        choose("援军任务",options,x->w.diplomacy.describe(x),x->{
            AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle("援军任务").setMessage(w.diplomacy.describe(x)).setPositiveButton("返回",null);
            if(!x.returning)dialog.setNeutralButton("结束援军",(d,i)->confirm("结束援军",null,"援军将返回盟军据点，已交付礼金不退还。",()->apply.execute(w,()->w.diplomacy.cancelAid(x.source))));
            dialog.show();
        });
    }
}

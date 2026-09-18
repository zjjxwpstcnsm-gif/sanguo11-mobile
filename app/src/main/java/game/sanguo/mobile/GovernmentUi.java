package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** Native command forms; the core owns validation, results and persisted state. */
final class GovernmentUi {
    private final Activity a;private final World w;private final Consumer<World.Result> apply;
    GovernmentUi(Activity a,World w,Consumer<World.Result> apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String message){new AlertDialog.Builder(a).setTitle(title).setMessage(message).setPositiveButton("返回",null).show();}
    private void confirm(String title,String message,Runnable run){new AlertDialog.Builder(a).setTitle(title).setMessage(message).setPositiveButton("执行",(d,n)->run.run()).setNegativeButton("取消",null).show();}
    private <T> void choose(String title,List<T> values,java.util.function.Function<T,String> label,Consumer<T> next){ChoiceDialog.show(a,w,title,values,label,next);}
    private String officer(World.Officer o){Government.Rank r=w.government.office(o.id);return o.name+" · 功绩"+w.government.merit(o.id)+(r==null?"":" · "+r.id);}
    private List<World.Officer> residents(World.City c){List<World.Officer> list=new ArrayList<>();for(World.Officer o:w.officers)if(o.cityId==c.id&&o.owner==c.owner&&!w.government.captive(o.id))list.add(o);return list;}
    private void actor(World.City c,Consumer<World.Officer> next){choose("选择执行武将",w.idle(c),this::officer,next);}
    void city(World.City c){
        String[] commands={"军师建议","任命军师","授予官职","免除官职","俘虏处置","赎回己将","召唤武将","城池委任","补给城外部队"};
        new AlertDialog.Builder(a).setTitle(c.name+" · 军政管理").setItems(commands,(d,n)->{
            switch(n){
                case 0:info("军师建议",w.government.advice(c.id));break;
                case 1:actor(c,o->choose("选择军师",w.idle(c),t->t.name+" · 智"+t.intelligence,t->confirm("任命军师","需要智力70，执行者和军师消耗本旬行动、行动力10。",()->apply.accept(w.government.appointAdvisor(c.id,o.id,t.id)))));break;
                case 2:actor(c,o->choose("选择受任武将",residents(c),this::officer,t->choose("选择武官职位",Government.ranks(),r->r.id+" · 功绩"+r.merit+" · 统兵"+r.troops,r->confirm("授予"+r.id,
                    t.name+"\n需要功绩"+r.merit+"，统兵上限"+r.troops+"，月俸"+r.salary+"金。\n同势力同一官职限一人。金100、行动力10；受任者本旬休整。",()->apply.accept(w.government.appointRank(c.id,o.id,t.id,r.id))))));break;
                case 3:actor(c,o->choose("选择免官武将",residents(c),this::officer,t->confirm("免除官职","行动力10，忠诚下降5；已出征部队不受影响。",()->apply.accept(w.government.removeRank(c.id,o.id,t.id)))));break;
                case 4:prisoners(c);break;
                case 5:{List<Government.Prisoner> list=new ArrayList<>();for(Government.Prisoner p:w.government.prisoners())if(w.officer(p.officerId).owner==c.owner)list.add(p);
                    choose("赎回己将",list,p->w.officer(p.officerId).name+" · "+w.government.locationLabel(p)+" · 金"+w.government.ransomCost(p.officerId),p->actor(c,o->confirm("赎回武将","支付"+w.government.ransomCost(p.officerId)+"金与行动力10，武将返回最近己城，本旬休整。",()->apply.accept(w.government.ransom(c.id,o.id,p.officerId)))));break;}
                case 6:{List<World.Officer> list=new ArrayList<>();for(World.City from:w.cities)if(from.owner==c.owner&&from.id!=c.id)list.addAll(w.idle(from));
                    choose("召唤武将",list,o->o.name+" · "+w.city(o.cityId).name,o->confirm("召唤"+o.name,"从驻城出发前往"+c.name+"，消耗行动力10；使用实际陆路任务，在途不可行动。",()->apply.accept(w.government.summon(c.id,o.id))));break;}
                case 7:actor(c,o->choose("委任方针 · "+w.government.policy(c.id).label,Arrays.asList(Government.Policy.values()),p->p.label,p->confirm("调整委任","行动力10。结束旬时，使用剩余行动力和闲将自动执行一项城务。\n内政优先开发；守备优先修复和治理。",()->apply.accept(w.government.delegate(c.id,o.id,p)))));break;
                case 8:replenish(c);break;
                default:break;
            }
        }).setNegativeButton("返回",null).show();
    }
    private void prisoners(World.City c){
        List<Government.Prisoner> list=new ArrayList<>();for(Government.Prisoner p:w.government.prisoners())if(p.captor==c.owner&&p.cityId==c.id)list.add(p);
        choose("本城俘虏",list,p->w.officer(p.officerId).name+" · 忠诚"+w.officer(p.officerId).loyalty,p->
            new AlertDialog.Builder(a).setTitle(w.officer(p.officerId).name).setItems(new String[]{"招降","释放","处决"},(d,n)->actor(c,o->{
                if(n==0)confirm("招降俘虏","当前成功率"+w.government.recruitChance(o.id,p.officerId)+"%。\n金100、行动力10；失败也消耗，每人每旬限一次。",()->apply.accept(w.government.recruitPrisoner(c.id,o.id,p.officerId)));
                else if(n==1)confirm("释放俘虏","行动力10；返回原势力最近城池，原势力无城则在野；双方关系改善。",()->apply.accept(w.government.release(c.id,o.id,p.officerId)));
                else confirm("处决俘虏","武将将永久死亡，宝物移交原势力府库；若为君主则触发继承。消耗行动力10，双方关系降至敌对。",()->apply.accept(w.life.executePrisoner(c.id,o.id,p.officerId)));
            })).setNegativeButton("返回",null).show());
    }
    private void amounts(String title,Consumer<int[]> next){choose(title,Arrays.asList(new int[]{0,1000},new int[]{0,5000},new int[]{1000,1000},new int[]{3000,5000}),v->v[0]+"兵 / "+v[1]+"粮",next);}
    private void replenish(World.City c){
        List<World.Unit> list=new ArrayList<>();for(World.Unit u:w.units)if(u.owner==c.owner&&u.hex.distance(c.hex)<=1)list.add(u);
        choose("选择城外部队",list,u->w.officer(u.officerId).name+" · 兵"+u.troops,u->actor(c,o->amounts("补给兵粮",v->confirm("城池补给","消耗城池"+v[0]+"兵、"+v[1]+"粮及对应基础兵装；行动力10。",()->apply.accept(w.supply.replenish(c.id,o.id,u.id,v[0],v[1]))))));
    }
    void supply(World.Unit u){
        List<World.Unit> list=new ArrayList<>();for(World.Unit b:w.units)if(b.id!=u.id&&b.owner==u.owner&&b.hex.distance(u.hex)==1)list.add(b);
        choose("向相邻部队移交兵粮",list,b->w.officer(b.officerId).name,b->amounts("移交数量",v->confirm("移交兵粮","移交"+v[0]+"兵、"+v[1]+"粮；本部队结束本旬行动，双方合计兵粮守恒。",()->apply.accept(w.supply.transfer(u.id,b.id,v[0],v[1])))));
    }
    void raid(World.Unit u){
        List<Domestic.Mission> list=new ArrayList<>();for(Domestic.Mission m:w.domestic.missions)if(w.supply.raidError(u.id,m.id)==null)list.add(m);
        choose("截击运输队",list,m->w.officer(m.officerId).name+" · 护送兵"+m.troops+" · "+m.hex,m->raid(u,m));
    }
    void raid(World.Unit u,Domestic.Mission m){
        String error=w.supply.raidError(u.id,m.id);if(error!=null){info("无法截击",error);return;}
        confirm("截击运输队","预计护送兵损失"+w.supply.raidDamage(u.id,m.id)+"，消耗本部队行动。\n击破后优先缴获可携带粮草，其余货物损失；城内和非交战运输队不能截击。",()->apply.accept(w.supply.raid(u.id,m.id)));
    }

}

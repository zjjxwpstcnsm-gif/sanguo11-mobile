package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Consumer;

/** Native campaign forms; previews never execute commands or advance RNG. */
final class CampaignUi {
    private final Activity a;private final World w;private final Consumer<World.Result> apply;
    CampaignUi(Activity a,World w,Consumer<World.Result> apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String text){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,Runnable action){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,n)->action.run()).setNegativeButton("取消",null).show();}
    private <T> void choose(String title,List<T> values,java.util.function.Function<T,String> label,Consumer<T> next){ChoiceDialog.show(a,w,title,values,label,next);}
    private void officer(World.City c,Consumer<World.Officer> next){choose("选择执行武将",w.idle(c),o->o.name+" · 政"+o.politics+" / 智"+o.intelligence+" / 魅"+o.charm,next);}
    void diplomacy(World.City c){
        List<Integer> sides=new ArrayList<>();for(int side=0;side<w.factions.length;side++)if(side!=c.owner&&w.alive(side))sides.add(side);
        choose("外交 · 选择势力",sides,side->w.faction(side)+" · "+w.campaign.relationLabel(c.owner,side),side->{
            DiplomacyUi foreign=new DiplomacyUi(a,w,apply);String[] commands={"亲善 · 金500","停战 · 金1000","同盟 · 金1000","解除协定","请求援军","劝降","交换俘虏","援军任务"};
            new AlertDialog.Builder(a).setTitle(w.faction(side)).setItems(commands,(d,index)->{
                if(index==7){foreign.missions(side);return;}if(index==4){foreign.aid(c,side);return;}if(index==6){foreign.exchange(c,side);return;}
                if(index==1||index==2){Campaign.TreatyKind kind=index==1?Campaign.TreatyKind.CEASEFIRE:Campaign.TreatyKind.ALLIANCE;
                    choose("选择期限",Arrays.asList(3,6,12),n->n+"旬",turns->officer(c,o->confirm(kind.label,"成功率 "+w.campaign.treatyChance(o.id,side,kind)+"%\n金1000、行动力10，期限"+turns+"旬；抵达交涉时开始生效。\n"+w.personnel.description(c.id,w.personnel.destination(side)),()->apply.accept(w.campaign.negotiate(c.id,o.id,side,kind,turns)))));return;}
                officer(c,o->{if(index==0)confirm("亲善",o.name+"出使：金500、行动力10。\n关系 +"+(15+o.politics/10)+"，抵达后交涉。\n"+w.personnel.description(c.id,w.personnel.destination(side)),()->apply.accept(w.campaign.goodwill(c.id,o.id,side)));
                    else if(index==3)confirm("解除协定","行动力10；双方关系 -50，其他势力关系 -10。",()->apply.accept(w.campaign.breakTreaty(c.id,o.id,side)));
                    else foreign.surrender(c,o,side);});
            }).setNegativeButton("返回",null).show();
        });
    }
    void rumor(World.City c){
        List<World.City> targets=new ArrayList<>();for(World.City target:w.cities)if(target.owner>=0&&w.campaign.hostile(c.owner,target.owner))targets.add(target);
        choose("流言目标",targets,t->t.name+" · "+w.faction(t.owner)+" · 治安"+t.order,target->officer(c,o->confirm("散布流言",
            "目标 "+target.name+"\n"+w.personnel.description(c.id,target.id)+"\n抵达后结算；成功率 "+w.campaign.rumorChance(o.id,target.id)+"%\n金300、行动力10；失败也消耗资源。\n成功降低治安10、非君主在城武将忠诚5。",()->apply.accept(w.campaign.rumor(c.id,o.id,target.id)))));
    }
    void trade(World.City c){
        new AlertDialog.Builder(a).setTitle("商人 · "+c.name).setItems(new String[]{"买粮 · 每1000粮 / 金"+w.campaign.foodPrice(c.id,true),"卖粮 · 每1000粮 / 金"+w.campaign.foodPrice(c.id,false)},(d,index)->{
            boolean buy=index==0;choose("交易数量",Arrays.asList(1000,5000,10000,20000),amount->amount+"粮",amount->officer(c,o->confirm(buy?"买入粮草":"卖出粮草",
                amount+"粮 ⇄ "+(amount/1000*w.campaign.foodPrice(c.id,buy))+"金\n行动力10，消耗武将本旬行动。\n本城本旬交易余量 "+(20000-w.campaign.traded(c.id))+"粮。",()->apply.accept(w.campaign.trade(c.id,o.id,buy,amount)))));
        }).setNegativeButton("返回",null).show();
    }
    void research(World.City c){
        choose("技巧研究 · 九系36项",Arrays.asList(0,1,2,3,4,5,6,7,8),i->Campaign.Tech.BRANCHES[i],branch->researchBranch(c,branch));
    }
    private void researchBranch(World.City c,int branch){
        choose(Campaign.Tech.BRANCHES[branch]+" · 当前"+w.campaign.points(c.owner)+"点",Campaign.Tech.branch(branch),tech->tech.label+(w.campaign.has(c.owner,tech)?" · 已掌握":" · "+tech.points+"点 / 金"+tech.gold),tech->officer(c,o->{
            String error=w.campaign.researchError(c.id,o.id,tech);
            if(error!=null){info(tech.label,error+"\n效果："+tech.effect);return;}
            confirm("研究"+tech.label,tech.effect+"\n消耗"+tech.points+"技巧点、金"+w.campaign.researchGold(o.id,tech)+"、行动力10。\n研究占用"+o.name+tech.turns+"旬，每势力同时研究一项。\n城池失守时中止，费用不退还。",()->apply.accept(w.campaign.research(c.id,o.id,tech)));
        }));
    }
    void study(World.City c){new AbilityUi(a,w,apply).train(c);}
    void projects(World.City c){
        StringBuilder text=new StringBuilder("技巧点 "+w.campaign.points(c.owner)+"\n每城每旬产出10点（势力上限100点/旬），战斗和部分军政命令也可获得技巧点。\n");
        for(Campaign.Project p:w.campaign.projects())if(p.owner==c.owner)text.append('\n').append(w.city(p.cityId).name).append(" · ").append(w.officer(p.officerId).name).append(" · ").append(p.label()).append(" · 剩").append(w.officer(p.officerId).otherTaskTurns).append("旬");
        for(Campaign.Tech t:Campaign.Tech.values())if(w.campaign.has(c.owner,t))text.append("\n已掌握 ").append(t.label).append("：").append(t.effect);
        new AlertDialog.Builder(a).setTitle("研究与培养进度").setMessage(text.toString()).setPositiveButton("返回",null)
            .setNeutralButton("中止任务",(d,n)->{
                List<Campaign.Project> own=new ArrayList<>();for(Campaign.Project p:w.campaign.projects())if(p.owner==w.active)own.add(p);
                choose("选择中止的任务",own,p->w.officer(p.officerId).name+" · "+p.label(),p->confirm("中止"+p.label(),"已付金和技巧点不退还，武将本旬仍算已行动。",()->apply.accept(w.campaign.cancelProject(p.officerId))));
            }).show();
    }
    void repair(World.City c){officer(c,o->confirm("修复城防","金300、行动力10；修复"+w.cityDefense.repairAmount(c,o)+"城防。"+(w.cityDefense.besieged(c)?"\n受围攻，补修效率为平时的¼。":""),()->apply.accept(w.campaign.repair(c.id,o.id))));}
    void dismiss(World.City c){List<World.Officer> targets=new ArrayList<>();for(World.Officer t:w.officers)if(t.owner==c.owner&&t.cityId==c.id&&t.role!=Strategy.Role.RULER&&!w.domestic.busy(t.id)&&!w.strategy.busy(t.id))targets.add(t);
        choose("流放武将",targets,t->t.name,t->{List<World.Officer> actors=new ArrayList<>(w.idle(c));actors.removeIf(o->o.id==t.id);choose("选择执行武将",actors,o->o.name,o->confirm("流放"+t.name,"行动力10；解除太守任命并成为本城在野武将。",()->apply.accept(w.campaign.dismiss(c.id,o.id,t.id))));});}
    void buildMilitary(World.City c){info("部队设置", "请在编队出征时携带金，移动到工地相邻格，然后选择部队→设置军事设施。\n施工期间部队自动补修，完成后设施开始生效。");}
    void structures(World.City c){List<War.Structure> options=new ArrayList<>();for(War.Structure s:w.war.structures())if(s.owner==c.owner&&SiteFootprint.distance(c,s.hex)<=3)options.add(s);
        choose("军事设施管理",options,s->s.kind.label+" · "+s.hex+" · 耐久"+s.hp,s->officer(c,o->confirm("拆除"+s.kind.label,"行动力10，不退还建造费用。",()->apply.accept(w.war.removeStructure(c.id,o.id,s.id)))));}
}

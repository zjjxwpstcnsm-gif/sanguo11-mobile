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
    private <T> void choose(String title,List<T> options,java.util.function.Function<T,String> label,Consumer<T> next){
        if(options.isEmpty()){info(title,"没有符合条件的选项。");return;}
        String[] names=new String[options.size()];for(int i=0;i<names.length;i++)names[i]=label.apply(options.get(i));
        new AlertDialog.Builder(a).setTitle(title).setItems(names,(d,i)->next.accept(options.get(i))).setNegativeButton("取消",null).show();
    }
    private void officer(World.City c,Consumer<World.Officer> next){choose("选择执行武将",w.idle(c),o->o.name+" · 政"+o.politics+" / 智"+o.intelligence,next);}
    void diplomacy(World.City c){
        List<Integer> sides=new ArrayList<>();for(int side=0;side<w.factions.length;side++)if(side!=c.owner&&w.alive(side))sides.add(side);
        choose("外交 · 选择势力",sides,side->w.faction(side)+" · "+w.campaign.relationLabel(c.owner,side),side->{
            String[] commands={"亲善 · 金500","停战 · 金1000","同盟 · 金1000","解除协定"};
            new AlertDialog.Builder(a).setTitle(w.faction(side)+" · "+w.campaign.relationLabel(c.owner,side)).setItems(commands,(d,index)->officer(c,o->{
                if(index==0)confirm("亲善",o.name+"出使：金500、行动力10。\n关系 +"+(15+o.politics/10),()->apply.accept(w.campaign.goodwill(c.id,o.id,side)));
                else if(index==3)confirm("解除协定","行动力10；双方关系 -50，其他势力关系 -10。\n解除后即可交战。",()->apply.accept(w.campaign.breakTreaty(c.id,o.id,side)));
                else {Campaign.TreatyKind kind=index==1?Campaign.TreatyKind.CEASEFIRE:Campaign.TreatyKind.ALLIANCE;
                    choose("选择期限",Arrays.asList(3,6,12),n->n+"旬",turns->confirm(kind.label,
                        "成功率 "+w.campaign.treatyChance(o.id,side,kind)+"%"+(w.skills.has(o,Skill.LUNKE)?"\n论客：提议被拒绝后，可与对方代表舌战争取协定。":"")+"\n金1000、行动力10；拒绝也消耗费用。\n有效期 "+turns+"旬，双方玩家和电脑均不能主动攻击。\n同盟需要关系至少20。",()->apply.accept(w.campaign.negotiate(c.id,o.id,side,kind,turns))));
                }
            })).setNegativeButton("返回",null).show();
        });
    }
    void rumor(World.City c){
        List<World.City> targets=new ArrayList<>();for(World.City target:w.cities)if(target.owner>=0&&w.campaign.hostile(c.owner,target.owner)&&c.hex.distance(target.hex)<=12)targets.add(target);
        choose("流言目标",targets,t->t.name+" · "+w.faction(t.owner)+" · 治安"+t.order,target->officer(c,o->confirm("散布流言",
            "目标 "+target.name+"；成功率 "+w.campaign.rumorChance(o.id,target.id)+"%\n金300、行动力10；失败也消耗资源。\n成功降低治安10、非君主在城武将忠诚5。",()->apply.accept(w.campaign.rumor(c.id,o.id,target.id)))));
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
    void repair(World.City c){officer(c,o->confirm("修复城防","金300、行动力10；修复"+Math.min(Math.max(0,w.campaign.defenseCap(c)-c.defense),(400+o.politics*4)*(w.campaign.has(c.owner,Campaign.Tech.ENGINEERING)?150:100)/100)+"城防。",()->apply.accept(w.campaign.repair(c.id,o.id))));}
    void dismiss(World.City c){officer(c,o->{List<World.Officer> targets=new ArrayList<>();for(World.Officer t:w.officers)if(t.owner==c.owner&&t.cityId==c.id&&t.id!=o.id&&t.role!=Strategy.Role.RULER&&!w.domestic.busy(t.id)&&!w.strategy.busy(t.id))targets.add(t);
        choose("流放武将",targets,t->t.name,t->confirm("流放"+t.name,"行动力10；解除太守任命并成为本城在野武将，需要重新登用。",()->apply.accept(w.campaign.dismiss(c.id,o.id,t.id))));});}
    void merge(Domestic.Facility f){World.City c=w.city(f.cityId);choose("选择被吸收的Lv1设施",w.domestic.mergeCandidates(f.id),t->t.kind.label+" · "+t.hex,t->officer(c,o->confirm("吸收合并",f.kind.label+" Lv"+f.level+" → Lv"+(f.level+1)+"\n金200、行动力10、工期2旬。\n被吸收设施立即移除，地块腾空；施工期间保留原等级收益。\n取消保留目标原等级，已消耗设施和费用不退还。",()->apply.accept(w.domestic.merge(f.id,t.id,o.id)))));}
    void buildMilitary(World.City c){info("部队设置", "请在编队出征时携带金，移动到工地相邻格，然后选择部队→设置军事设施。\n施工期间部队自动补修，完成后设施开始生效。");}
    void structures(World.City c){List<War.Structure> options=new ArrayList<>();for(War.Structure s:w.war.structures())if(s.owner==c.owner&&s.hex.distance(c.hex)<=3)options.add(s);
        choose("军事设施管理",options,s->s.kind.label+" · "+s.hex+" · 耐久"+s.hp,s->officer(c,o->confirm("拆除"+s.kind.label,"行动力10，不退还建造费用。",()->apply.accept(w.war.removeStructure(c.id,o.id,s.id)))));}
}

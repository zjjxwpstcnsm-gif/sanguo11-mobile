package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** Four research branches and finite training share the actual saved world and command validation. */
final class AbilityUi {
    private final Activity a;private final World w;private final Consumer<World.Result> apply;
    AbilityUi(Activity a,World w,Consumer<World.Result> apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String message){new AlertDialog.Builder(a).setTitle(title).setMessage(message).setPositiveButton("返回",null).show();}
    private void confirm(String title,String message,Runnable run){new AlertDialog.Builder(a).setTitle(title).setMessage(message).setPositiveButton("执行",(d,n)->run.run()).setNegativeButton("取消",null).show();}
    private <T> void choose(String title,List<T> values,Function<T,String> label,Consumer<T> next){if(values.isEmpty()){info(title,"没有符合条件的选项。");return;}String[] labels=values.stream().map(label).toArray(String[]::new);new AlertDialog.Builder(a).setTitle(title).setItems(labels,(d,i)->next.accept(values.get(i))).setNegativeButton("返回",null).show();}
    private String state(int side,AbilityResearch.Node n){AbilityResearch.Research r=w.abilities.research(side);return w.abilities.learned(side,n.id)?"已研究 · 剩"+w.abilities.remaining(side,n.id)+"次":r!=null&&r.nodeId.equals(n.id)?"研究中 · 剩"+r.remaining+"旬":w.abilities.unlocked(side,n)?"可研究":"待解锁";}
    void research(World.City city){
        choose("PK能力研究 · 选择方向",Arrays.asList("攻击","防御","计略","内政"),s->s,branch->{
            List<AbilityResearch.Node> nodes=new ArrayList<>();for(AbilityResearch.Node n:w.abilities.visible(city.owner))if(n.branch.equals(branch))nodes.add(n);
            choose(branch+"能力表",nodes,n->n.label+" · "+state(city.owner,n),n->{
                StringJoiner prerequisites=new StringJoiner("、");for(String id:n.prerequisites)prerequisites.add(AbilityResearch.node(id).label);
                String description=n.effect()+"\n培养总次数："+n.uses+"\n"+(n.prerequisites.isEmpty()?"起始能力":"前置："+prerequisites)+"\n研究：金300、行动力20、"+n.turns+"旬。不占用武将。";
                if(n.category==AbilityResearch.Category.SKILL&&!AbilityResearch.skillAvailable(n.skill))description+="\n此特技当前暂不可培养，可研究以解锁后续能力。";
                String error=w.abilities.researchError(city.id,n.id);
                if(error!=null){info(n.label,error+"\n\n"+description);return;}
                confirm("研究"+n.label,description,()->apply.accept(w.abilities.startResearch(city.id,n.id)));
            });
        });
    }
    void train(World.City city){
        choose("武将培养 · 选择类别",Arrays.asList(AbilityResearch.Category.values()),c->c.label,category->{
            List<AbilityResearch.Node> nodes=new ArrayList<>();for(AbilityResearch.Node n:w.abilities.visible(city.owner))if(n.category==category&&w.abilities.learned(city.owner,n.id))nodes.add(n);
            choose("已研究的"+category.label,nodes,n->n.label+" · 剩"+w.abilities.remaining(city.owner,n.id)+"次",n->choose("选择培养武将",w.idle(city),o->o.name+" · "+(category==AbilityResearch.Category.SKILL?Skill.label(o.skillId):category==AbilityResearch.Category.STAT?w.abilities.value(o.id,n):War.rankLabel(w.abilities.value(o.id,n))),o->{
                String error=w.abilities.trainingError(city.id,o.id,n.id,true);if(error!=null){info(n.label,error);return;}
                String description=o.name+"："+n.effect()+"\n行动力20，培养3旬，期间不能执行其他命令。\n完成消耗1次；本势力每个类别同时培养一人。";
                if(category==AbilityResearch.Category.STAT)description+="\n此项研究成长已增加"+w.abilities.gained(o.id,n.index)+"点；达到20点后不能继续培养。";
                if(category==AbilityResearch.Category.SKILL)description+="\n原特技「"+Skill.label(o.skillId)+"」将被「"+n.skill.label+"」覆盖。";
                confirm("培养"+n.label,description,()->apply.accept(w.abilities.train(city.id,o.id,n.id,true)));
            }));
        });
    }
    void progress(World.City city){
        StringBuilder text=new StringBuilder("能力研究：每势力同时一项。\n武将培养：基础能力、适性、特技各同时一人。\n");
        AbilityResearch.Research r=w.abilities.research(city.owner);
        text.append(r==null?"\n暂无能力研究":"\n"+w.city(r.cityId).name+" · 研究"+AbilityResearch.node(r.nodeId).label+" · 剩"+r.remaining+"旬");
        List<AbilityResearch.Training> own=new ArrayList<>();for(AbilityResearch.Training t:w.abilities.training())if(t.owner==city.owner){own.add(t);text.append("\n").append(w.officer(t.officerId).name).append(" · ").append(t.label()).append(" · 剩").append(w.officer(t.officerId).otherTaskTurns).append("旬");}
        for(AbilityResearch.Node n:w.abilities.visible(city.owner))if(w.abilities.learned(city.owner,n.id))text.append("\n").append(n.label).append(" · 剩").append(w.abilities.remaining(city.owner,n.id)).append("次");
        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle("PK研究与培养进度").setMessage(text.toString()).setPositiveButton("返回",null);
        if(city.owner==w.active&&r!=null)b.setNeutralButton("中止研究",(d,i)->confirm("中止能力研究","已付金与行动力不退还。",()->apply.accept(w.abilities.cancelResearch(city.owner))));
        if(city.owner==w.active&&!own.isEmpty())b.setNegativeButton("中止培养",(d,i)->choose("中止培养",own,t->w.officer(t.officerId).name+" · "+t.label(),t->confirm("中止培养","本次未完成，不消耗培养次数；行动力不退还。",()->apply.accept(w.abilities.cancelTraining(t.officerId)))));
        b.show();
    }
}

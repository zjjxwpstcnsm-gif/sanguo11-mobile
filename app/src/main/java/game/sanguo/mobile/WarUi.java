package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Consumer;

final class WarUi {
    private final MainActivity a;private final World w;private final Consumer<World.Result> apply;
    WarUi(MainActivity a,World w,Consumer<World.Result> apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String text){new AlertDialog.Builder(a).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,Runnable command){a.commandDialog(title,text,"执行",w,command);}
    void move(World.Unit u,Hex target){
        UnitOrders.MovePlan plan=w.orders.previewMove(u.id,target);
        if(!plan.valid()){info(plan.error);return;}
        StringBuilder route=new StringBuilder();for(Hex h:plan.path){if(route.length()>0)route.append(" → ");route.append(h);}
        confirm("确认移动",route+"\n消耗移动力 "+plan.cost+"，移动后剩余 "+plan.remaining+"。\n仍可攻击、战法、计略、入城或待命。",()->apply.accept(w.orders.execute(plan)));
    }
    void skills(World.Unit u){
        StringBuilder text=new StringBuilder();for(World.Officer o:w.army.crew(u))text.append(o.name).append(" · ").append(Skill.label(o.skillId)).append('\n');
        info(text.toString());
    }
    void attack(World.Unit u,World.Unit target){
        StringBuilder capture=new StringBuilder();
        for(World.Officer o:w.army.crew(target))capture.append("\n").append(o.name).append("：").append(w.government.captureChance(u,target,o)).append("%");
        confirm("攻击"+w.officer(target.officerId).name,"预计敌损约"+w.war.previewDamage(u.id,target.id)+"（有随机波动）。\n邻接近战敌军可反击，攻击结束本旬行动。\n击破时回收敌军剩余金粮，加入实际击破部队，受携带容量限制。\n击破后俘虏概率："+capture,()->apply.accept(w.attack(u.id,target.id)));
    }
    void tactics(World.Unit u){
        List<War.Tactic> list=new ArrayList<>();for(War.Tactic t:War.Tactic.values())if(t.weapon==u.weapon)list.add(t);
        if(list.isEmpty()){info("剑兵没有专属战法，可普攻或使用部队计略。");return;}
        String[] labels=new String[list.size()];for(int i=0;i<labels.length;i++){War.Tactic t=list.get(i);labels[i]=t.label+" · 气力"+t.energy+" · "+War.rankLabel(t.rank)+"级";}
        new AlertDialog.Builder(a).setTitle("战法 · 适性"+War.rankLabel(w.army.aptitude(u))).setItems(labels,(d,i)->{
            War.Tactic tactic=list.get(i);List<World.Unit> targets=new ArrayList<>();for(World.Unit t:w.fieldUnits())if(w.war.tacticError(u.id,t.id,tactic)==null)targets.add(t);
            if(targets.isEmpty()){
                List<World.Unit> nearby=new ArrayList<>();for(World.Unit t:w.fieldUnits())if(w.campaign.hostile(u.owner,t.owner))nearby.add(t);
                nearby.sort(Comparator.comparingInt((World.Unit t)->u.hex.distance(t.hex)).thenComparingInt(t->t.id));
                StringBuilder reasons=new StringBuilder(tactic.label+"当前没有合法目标。\n");
                if(nearby.isEmpty())reasons.append(w.war.tacticError(u.id,-1,tactic));
                for(int n=0;n<Math.min(3,nearby.size());n++){World.Unit t=nearby.get(n);reasons.append(w.officer(t.officerId).name).append("：").append(w.war.tacticError(u.id,t.id,tactic)).append('\n');}
                info(reasons.toString());return;
            }
            a.pickOnMap(tactic.label+" · 选择目标",u.hex,targets.stream().map(t->t.hex).collect(java.util.stream.Collectors.toList()),h->{World.Unit t=w.unitAt(h);if(t==null)return;a.showTacticPreview(w,w.war.tacticPreview(u.id,t.id,tactic),()->apply.accept(w.war.tactic(u.id,t.id,tactic)));},h->h==null?"目标在地图范围外":w.war.tacticError(u.id,w.unitAt(h)==null?-1:w.unitAt(h).id,tactic));
        }).setNegativeButton("取消",null).show();
    }
    void joint(World.Unit u){
        List<World.Unit> targets=new ArrayList<>();for(World.Unit t:w.fieldUnits())if(w.advancedBattle.jointError(u.id,t.id)==null)targets.add(t);
        if(targets.isEmpty()){info("齐攻需要至少两支未行动的陆上近战部队邻接同一敌军。");return;}
        a.pickOnMap("齐攻 · 选择目标",u.hex,targets.stream().map(t->t.hex).collect(java.util.stream.Collectors.toList()),h->{World.Unit t=w.unitAt(h);if(t==null)return;StringBuilder text=new StringBuilder("参与：");for(World.Unit member:w.advancedBattle.jointParticipants(u.id,t.id))text.append(w.officer(member.officerId).name).append(" ");text.append("\n参与部队本旬行动均结束。敌方铁壁将齐攻化为主攻部队的普攻，其他部队保留行动。");confirm("齐攻",text.toString(),()->apply.accept(w.advancedBattle.joint(u.id,t.id)));});
    }
    void plots(World.Unit u){
        List<War.Plot> plots=new ArrayList<>();for(War.Plot p:War.Plot.values())if(w.advancedBattle.unlocked(u,p))plots.add(p);
        String[] names=new String[plots.size()];for(int i=0;i<names.length;i++){War.Plot p=plots.get(i);names[i]=p.label+" · 气力"+w.war.plotCost(u.id,p);}
        new AlertDialog.Builder(a).setTitle("部队计略").setItems(names,(d,i)->{
            War.Plot plot=plots.get(i);List<Hex> targets=new ArrayList<>();
            int range=w.war.plotRange(u.id,plot);
            for(int q=Math.max(0,u.hex.q-range);q<=Math.min(w.width-1,u.hex.q+range);q++)for(int r=Math.max(0,u.hex.r-range);r<=Math.min(w.height-1,u.hex.r+range);r++){Hex h=new Hex(q,r);if(w.war.plotError(u.id,h,plot)==null)targets.add(h);}
            if(targets.isEmpty()){info("没有可施展目标。\n"+plot.effect+"\n需要计略范围内有效目标和足够气力；伏兵还需自身位于森林。");return;}
            a.pickOnMap(plot.label+" · 选择目标",u.hex,targets,h->confirm(plot.label,plot.effect+(plot==War.Plot.FIRE?"\n"+w.skills.firePreview(u,w.unitAt(h),w.war.at(h)!=null&&w.fieldworks.trap(w.war.at(h).kind)):"")+"\n成功率 "+w.war.plotChance(u.id,h,plot)+"%\n消耗气力"+w.war.plotCost(u.id,plot)+"和本旬行动；失败也消耗。\n火种可连锁引爆，己方部队进入火场也会受伤。",()->apply.accept(w.war.plot(u.id,h,plot))),h->h==null?"目标在地图范围外":w.war.plotError(u.id,h,plot));
        }).setNegativeButton("取消",null).show();
    }
}

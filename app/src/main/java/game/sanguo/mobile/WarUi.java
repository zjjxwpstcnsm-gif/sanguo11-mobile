package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Consumer;

final class WarUi {
    private final Activity a;private final World w;private final Consumer<World.Result> apply;
    WarUi(Activity a,World w,Consumer<World.Result> apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String text){new AlertDialog.Builder(a).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,Runnable command){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,n)->command.run()).setNegativeButton("取消",null).show();}
    void attack(World.Unit u,World.Unit target){confirm("攻击"+w.officer(target.officerId).name,"预计敌损约"+w.war.previewDamage(u.id,target.id)+"（有随机波动）。\n邻接近战敌军可反击，攻击结束本旬行动。",()->apply.accept(w.attack(u.id,target.id)));}
    void tactics(World.Unit u){
        List<War.Tactic> list=new ArrayList<>();for(War.Tactic t:War.Tactic.values())if(t.weapon==u.weapon)list.add(t);
        String[] labels=new String[list.size()];for(int i=0;i<labels.length;i++){War.Tactic t=list.get(i);labels[i]=t.label+" · 气力"+t.energy+" · "+War.rankLabel(t.rank)+"级";}
        new AlertDialog.Builder(a).setTitle("战法 · 适性"+War.rankLabel(w.officer(u.officerId).aptitude[u.weapon.ordinal()])).setItems(labels,(d,i)->{
            War.Tactic tactic=list.get(i);List<World.Unit> targets=new ArrayList<>();for(World.Unit t:w.units)if(w.war.tacticError(u.id,t.id,tactic)==null)targets.add(t);
            if(targets.isEmpty()){info("没有可施展目标。\n"+tactic.effect+"\n需要"+War.rankLabel(tactic.rank)+"级适性、气力"+tactic.energy+"和有效范围内交战目标；位移还需要空地。");return;}
            String[] names=new String[targets.size()];for(int j=0;j<names.length;j++){World.Unit t=targets.get(j);names[j]=w.officer(t.officerId).name+" · 兵"+t.troops+" · 成功率"+w.war.tacticChance(u.id,t.id,tactic)+"%";}
            new AlertDialog.Builder(a).setTitle("选择战法目标").setItems(names,(dialog,j)->{World.Unit t=targets.get(j);confirm(tactic.label,tactic.effect+"\n成功率 "+w.war.tacticChance(u.id,t.id,tactic)+"%，消耗气力"+tactic.energy+"。\n失败也消耗气力和本旬行动。",()->apply.accept(w.war.tactic(u.id,t.id,tactic)));}).setNegativeButton("取消",null).show();
        }).setNegativeButton("取消",null).show();
    }
    void plots(World.Unit u){
        String[] names=new String[War.Plot.values().length];for(int i=0;i<names.length;i++){War.Plot p=War.Plot.values()[i];names[i]=p.label+" · 气力"+p.energy;}
        new AlertDialog.Builder(a).setTitle("部队计略").setItems(names,(d,i)->{
            War.Plot plot=War.Plot.values()[i];List<Hex> targets=new ArrayList<>();
            for(int q=Math.max(0,u.hex.q-2);q<=Math.min(w.width-1,u.hex.q+2);q++)for(int r=Math.max(0,u.hex.r-2);r<=Math.min(w.height-1,u.hex.r+2);r++){Hex h=new Hex(q,r);if(w.war.plotError(u.id,h,plot)==null)targets.add(h);}
            if(targets.isEmpty()){info("没有可施展目标。\n"+plot.effect+"\n需要两格内有效目标和足够气力；伏兵还需自身位于森林并邻接敌军。");return;}
            String[] labels=new String[targets.size()];for(int j=0;j<labels.length;j++){Hex h=targets.get(j);World.Unit target=w.unitAt(h);labels[j]=(target==null?"地块 "+h:w.officer(target.officerId).name+" · "+h)+" · 成功率"+w.war.plotChance(u.id,h,plot)+"%";}
            new AlertDialog.Builder(a).setTitle("选择计略目标").setItems(labels,(dialog,j)->{Hex h=targets.get(j);confirm(plot.label,plot.effect+"\n消耗气力"+plot.energy+"和本旬行动；失败也消耗。\n火种可连锁引爆，己方部队进入火场也会受伤。",()->apply.accept(w.war.plot(u.id,h,plot)));}).setNegativeButton("取消",null).show();
        }).setNegativeButton("取消",null).show();
    }
}

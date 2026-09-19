package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

final class FieldworkUi {
    private final MainActivity a;private final World w;private final Consumer<World.Result> apply;
    FieldworkUi(MainActivity a,World w,Consumer<World.Result> apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String text){new AlertDialog.Builder(a).setMessage(text).setPositiveButton("返回",null).show();}
    private <T> void choose(String title,List<T> list,Function<T,String> label,Consumer<T> next){
        if(list.isEmpty()){info("没有符合条件的选项，请检查携金、行动状态、前置技巧和邻接地块。");return;}
        String[] labels=new String[list.size()];for(int i=0;i<labels.length;i++)labels[i]=label.apply(list.get(i));
        AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(title).setNegativeButton("取消",null);
        if(GameIcon.supports(list.get(0)))dialog.setAdapter(GameIcon.adapter(a,w,list,label),(d,i)->next.accept(list.get(i)));
        else dialog.setItems(labels,(d,i)->next.accept(list.get(i)));dialog.show();
    }
    private void confirm(String title,String text,Runnable action){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,i)->action.run()).setNegativeButton("取消",null).show();}
    void build(World.Unit u){choose("部队设置 · 携金"+u.gold,w.fieldworks.available(u.owner),k->k.label+" · 金"+k.gold,k->
        a.pickOnMap("设置"+k.label,u.hex,w.fieldworks.sites(u.id,k),h->{
            if(w.fieldworks.ball(k))a.pickOnMap("火球方向",h,h.neighbors().stream().filter(w::inside).collect(java.util.stream.Collectors.toList()),direction->place(u,k,h,h.neighbors().indexOf(direction)));
            else place(u,k,h,0);
        },h->w.fieldworks.buildError(u.id,k,h,0)));}
    private void place(World.Unit u,War.StructureKind k,Hex h,int direction){
        confirm("设置"+k.label,k.effect+"\n消耗携金"+k.gold+"与部队本旬行动。\n施工 "+Math.min(k.hp,w.fieldworks.constructionRate(u))+"/"+k.hp+"；未完成时每旬自动补修，占用本部队行动。\n工地 "+h+(w.fieldworks.ball(k)?"，朝向"+h.neighbors().get(direction):""),()->apply.accept(w.fieldworks.build(u.id,k,h,direction)));
    }
    void repair(World.Unit u){List<War.Structure> list=new ArrayList<>();for(War.Structure s:w.war.structures())if(s.owner==u.owner&&s.hex.distance(u.hex)==1&&s.hp<s.kind.hp&&(s.builder<0||s.builder==u.id))list.add(s);
        choose("邻接设施补修",list,s->s.kind.label+" · "+s.hp+"/"+s.kind.hp,s->confirm("补修"+s.kind.label,"本旬修复最多"+w.fieldworks.constructionRate(u)+"耐久，消耗部队行动；后续自动补修。",()->apply.accept(w.fieldworks.repair(u.id,s.id))));
    }
    void stop(World.Unit u){confirm("中止施工","保留设施和当前耐久，已付携金不退还，本旬已消耗的行动不会恢复。",()->apply.accept(w.fieldworks.stop(u.id)));}
    void fund(World.Unit u){List<World.City> cities=new ArrayList<>();for(World.City c:w.cities)if(c.owner==u.owner&&c.hex.distance(u.hex)==1)cities.add(c);
        choose("补充携金 · 相邻据点",cities,c->c.name+" · 金"+c.gold,c->{List<Integer> amounts=new ArrayList<>();for(int amount:new int[]{200,1000,3000,5000,10000})if(amount<=c.gold&&u.gold+amount<=10000)amounts.add(amount);
            choose("提取金额",amounts,n->n+"金",n->confirm("提取携金","从"+c.name+"转移"+n+"金到本部队，消耗本旬行动。",()->apply.accept(w.fieldworks.withdraw(u.id,c.id,n))));});}
}

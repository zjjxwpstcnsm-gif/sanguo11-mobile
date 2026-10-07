package game.sanguo.mobile;

import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

final class FieldworkUi {
    private final MainActivity a;private final World w;private final LegacyCommandSink apply;
    FieldworkUi(MainActivity a,World w,LegacyCommandSink apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String text){AlertDialog dialog=new AlertDialog.Builder(a).setMessage(text).setPositiveButton("返回",null).show();a.trackDialog(dialog);}
    private <T> void choose(String title,List<T> list,Function<T,String> label,Consumer<T> next){
        if(list.isEmpty()){info("没有符合条件的选项，请检查携金、行动状态、前置技巧和邻接地块。");return;}
        String[] labels=new String[list.size()];for(int i=0;i<labels.length;i++)labels[i]=label.apply(list.get(i));
        AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(title).setNegativeButton("取消",null);
        if(GameIcon.supports(list.get(0)))dialog.setAdapter(GameIcon.adapter(a,w,list,label),(d,i)->next.accept(list.get(i)));
        else dialog.setItems(labels,(d,i)->next.accept(list.get(i)));a.trackDialog(dialog.show());
    }
    private void confirm(String title,String text,Runnable action){a.commandDialog(title,text,"执行",w,action);}
    private String buildReason(World.Unit u,War.StructureKind kind,Hex target,int direction){
        Fieldworks.Validation check=w.fieldworks.buildCheck(u.id,kind,target,direction);if(check.allowed())return null;
        String location=MapCoordinates.display(w,target);
        if(target!=null&&w.inside(target))location+=" · "+TerrainPresentation.of(w.terrain[target.q][target.r]).name();
        String extra="";
        if("SITE_DISTANCE".equals(check.code))for(World.City c:w.cities)if(SiteFootprint.distance(c,target)<=2){extra=" · 距"+c.name+"占地 "+SiteFootprint.distance(c,target)+"格（需超过2格）";break;}
        return location+"\n"+check.code+" · "+check.detail+extra;
    }
    void build(World.Unit u){choose("部队设置 · 携金"+u.gold,w.fieldworks.available(u.owner),k->k.label+" · 金"+w.fieldworks.baseBuildCost(k),k->{
        List<Hex> sites=w.fieldworks.sites(u.id,k);
        if(sites.isEmpty()){
            StringBuilder reasons=new StringBuilder("当前相邻格均不能设置"+k.label+"。\n部队携金 "+u.gold+" / 需要 "+w.fieldworks.baseBuildCost(k)+"；"+(u.acted?"本旬已行动":"本旬未行动")+" · "+u.status);
            for(Hex h:u.hex.neighbors())reasons.append("\n\n").append(buildReason(u,k,h,0));
            info(reasons.toString());return;
        }
        a.pickOnMap("设置"+k.label,u.hex,sites,h->{
            if(w.fieldworks.ball(k))a.pickOnMap("火球方向",h,h.neighbors().stream().filter(w::inside).collect(java.util.stream.Collectors.toList()),direction->place(u,k,h,h.neighbors().indexOf(direction)));
            else place(u,k,h,0);
        },h->buildReason(u,k,h,0));
    });}
    private void place(World.Unit u,War.StructureKind k,Hex h,int direction){
        String reason=buildReason(u,k,h,direction);if(reason!=null){info(reason);return;}
        confirm("设置"+k.label,k.effect+"\n消耗携金"+w.fieldworks.buildCost(u.id,k,h)+"与部队本旬行动。\n施工 "+Math.min(k.hp,w.fieldworks.constructionRate(u))+"/"+k.hp+"；未完成时每旬自动补修，占用本部队行动。\n工地 "+h+(w.fieldworks.ball(k)?"，朝向"+h.neighbors().get(direction):""),()->apply.execute(w,()->w.fieldworks.build(u.id,k,h,direction)));
    }
    void repair(World.Unit u){List<War.Structure> list=w.fieldworks.repairSites(u.id);
        if(list.isEmpty()){
            String error=w.orders.combatError(u);if(error!=null){info(error);return;}
            StringBuilder reasons=new StringBuilder("当前没有可补修的相邻设施。");
            for(War.Structure s:w.war.structures())if(s.hex.distance(u.hex)==1){var check=w.fieldworks.repairCheck(u.id,s.id);reasons.append("\n").append(s.kind.label).append(" · ").append(check.detail);}
            info(reasons.toString());return;
        }
        choose("邻接设施补修",list,s->s.kind.label+" · "+s.hp+"/"+s.kind.hp,s->{var check=w.fieldworks.repairCheck(u.id,s.id);if(!check.allowed()){info(check.detail);return;}confirm("补修"+s.kind.label,"本旬修复最多"+w.fieldworks.constructionRate(u)+"耐久，消耗部队行动；后续自动补修。",()->apply.execute(w,()->w.fieldworks.repair(u.id,s.id)));});
    }
    void stop(World.Unit u){confirm("中止施工","保留设施和当前耐久，已付携金不退还，本旬已消耗的行动不会恢复。",()->apply.execute(w,()->w.fieldworks.stop(u.id)));}
    void fund(World.Unit u){List<World.City> cities=w.fieldworks.fundingSites(u.id);
        if(cities.isEmpty()){
            String error=w.orders.combatError(u);
            info(error!=null?error:u.gold>=10000?"部队携金已达10000上限":"当前位置没有可补充携金的己方据点。城市须进入其七格占地；关港须满足实际入港条件，并有可提取的金。");return;
        }
        choose("补充携金 · 可进入据点",cities,c->c.name+" · 金"+c.gold,c->{
            int maximum=w.fieldworks.withdrawMaximum(u.id,c.id);SortedSet<Integer> values=new TreeSet<>();
            for(int amount:new int[]{200,1000,3000,5000,10000,maximum})if(amount>0&&w.fieldworks.withdrawCheck(u.id,c.id,amount).allowed())values.add(amount);
            choose("提取金额 · 可提取最多"+maximum+"金",new ArrayList<>(values),n->n+"金",n->confirm("提取携金","从"+c.name+"转移"+n+"金到本部队，消耗本旬行动。",()->apply.execute(w,()->w.fieldworks.withdraw(u.id,c.id,n))));
        });
    }
}

package game.sanguo.mobile;

import android.app.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** In-game mediation and item commands. Editing is deliberately a separate menu. */
final class EstatesUi {
    private final Activity a;private final World w;private final LegacyCommandSink apply;
    EstatesUi(Activity a,World w,LegacyCommandSink apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String message){new AlertDialog.Builder(a).setTitle(title).setMessage(message).setPositiveButton("返回",null).show();}
    private <T> void choose(String title,List<T> values,java.util.function.Function<T,String> label,Consumer<T> next){ChoiceDialog.show(a,w,title,values,label,next);}
    private void confirm(String title,String text,Runnable run){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,n)->run.run()).setNegativeButton("取消",null).show();}
    private List<World.Officer> residents(World.City c){List<World.Officer> out=new ArrayList<>();for(World.Officer o:w.officers)if(o.owner==c.owner&&o.cityId==c.id&&!w.government.captive(o.id))out.add(o);return out;}
    void mediate(World.City c){
        choose("仲介 · 技巧500",Arrays.asList(Relations.Kind.SWORN,Relations.Kind.SPOUSE),k->k.label,k->
            choose("仲介 · 第一位武将",residents(c),o->o.name+" · 功绩"+w.government.merit(o.id),first->{
                List<World.Officer> others=new ArrayList<>();for(World.Officer o:residents(c))if(o.id!=first.id)others.add(o);
                choose("仲介 · 第二位武将",others,o->o.name+" · 功绩"+w.government.merit(o.id),second->{
                    String error=w.relations.mediateError(c.id,first.id,second.id,k);if(error!=null){info("无法仲介",error);return;}
                    confirm("确认"+k.label,first.name+"与"+second.name+"\n消耗500技巧点，结义最多3人。\n配偶/结义影响忠诚、登用、编队与支援；内助在结婚时使双方五维+1。",()->apply.execute(w,()->w.relations.mediate(c.id,first.id,second.id,k)));
                });
            }));
    }
    void treasures(World.City c){
        new AlertDialog.Builder(a).setTitle(c.name+" · 宝物").setItems(new String[]{"本势力宝物","赏赐府库宝物","收回武将宝物","搜索宝物"},(d,n)->{
            if(n==0)choose("本势力宝物",w.treasures.owned(c.owner),i->i.definition.name+" · "+w.treasures.location(i),i->info(i.definition.name,i.definition.kind.label+" · 价值"+i.definition.value+"\n"+i.definition.kind.effect+"\n持有："+w.treasures.location(i)));
            else if(n==1){List<Treasures.Item> items=new ArrayList<>();for(Treasures.Item i:w.treasures.owned(c.owner))if(i.place==Treasures.Place.TREASURY)items.add(i);
                choose("选择府库宝物",items,i->i.definition.name+" · 价值"+i.definition.value,i->choose("选择受赏武将",residents(c),o->o.name+" · 忠诚"+o.loyalty,t->
                    choose("选择执行武将",w.idle(c),o->o.name,o->confirm("赏赐宝物",i.definition.name+" → "+t.name+"\n忠诚 "+t.loyalty+" → "+Math.min(100,t.loyalty+i.definition.value)+"\n行动力10，执行武将本旬行动。",()->apply.execute(w,()->w.treasures.award(c.id,o.id,i.definition.id,t.id))))));
            }else if(n==2){List<Treasures.Item> items=new ArrayList<>();for(World.Officer o:residents(c))items.addAll(w.treasures.held(o.id));
                choose("选择收回宝物",items,i->i.definition.name+" · "+w.treasures.location(i),i->choose("选择执行武将",w.idle(c),o->o.name,o->confirm("收回宝物",i.definition.name+"将返回府库。\n非君主持有人忠诚下降"+i.definition.value+"，行动力10，执行武将本旬行动。",()->apply.execute(w,()->w.treasures.confiscate(c.id,o.id,i.definition.id)))));
            }else choose("选择搜索武将",w.idle(c),o->o.name+" · 政治"+o.politics,o->confirm("搜索宝物","与搜索人才使用同一命令。行动力10；仅会发现剧本中明确放置于本城的宝物，也可能发现人才或金。",()->apply.execute(w,()->w.strategy.search(c.id,o.id))));
        }).setNegativeButton("返回",null).show();
    }
}

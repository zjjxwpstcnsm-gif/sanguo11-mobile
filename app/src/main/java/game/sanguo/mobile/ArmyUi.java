package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** All dialogs collect choices first. Only the final confirmation changes the world. */
final class ArmyUi {
    private final Activity a;private final World w;private final Consumer<World.Result> apply;private final Consumer<Hex> focus;
    ArmyUi(Activity a,World w,Consumer<World.Result> apply,Consumer<Hex> focus){this.a=a;this.w=w;this.apply=apply;this.focus=focus;}
    private void info(String title,String text){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,Runnable action){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,n)->action.run()).setNegativeButton("取消",null).show();}
    private <T> void choose(String title,List<T> list,Function<T,String> label,Consumer<T> next){
        if(list.isEmpty()){info(title,"没有可用选项");return;}String[] names=new String[list.size()];for(int i=0;i<names.length;i++)names[i]=label.apply(list.get(i));
        new AlertDialog.Builder(a).setTitle(title).setItems(names,(d,i)->next.accept(list.get(i))).setNegativeButton("取消",null).show();
    }
    private List<Integer> troopOptions(int officer){List<Integer> n=new ArrayList<>();int cap=w.government.commandLimit(officer);for(int v:new int[]{1000,3000,5000,8000,10000,12000,15000})if(v<=cap)n.add(v);if(!n.contains(cap))n.add(cap);Collections.sort(n);return n;}
    void deploy(World.City c){choose("编队 · 选择主将",w.idle(c),o->o.name+" · 统"+o.leadership,leader->{
        List<World.Officer> candidates=new ArrayList<>(w.idle(c));candidates.remove(leader);boolean[] selected=new boolean[candidates.size()];String[] labels=new String[candidates.size()];
        for(int i=0;i<labels.length;i++){World.Officer o=candidates.get(i);labels[i]=o.name+" · 武"+o.war+" / 智"+o.intelligence;}
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("副将 · 最多选择2人").setMultiChoiceItems(labels,selected,(d,i,on)->{
            int count=0;for(boolean value:selected)if(value)count++;
            if(count>2){selected[i]=false;((AlertDialog)d).getListView().setItemChecked(i,false);}
        }).setPositiveButton("下一步",(d,n)->{
            List<Integer> ids=new ArrayList<>();for(int i=0;i<selected.length;i++)if(selected[i])ids.add(candidates.get(i).id);
            int[] deputies=new int[ids.size()];for(int i=0;i<ids.size();i++)deputies[i]=ids.get(i);
            choose("陆战兵装",Arrays.asList(World.Weapon.values()),weapon->weapon.label+(weapon==World.Weapon.SWORD?" · 无需库存":" · 库存"+c.equipment[weapon.ordinal()]),weapon->
                choose("携带舰船",Arrays.asList(Army.Ship.values()),ship->ship.label+(ship==Army.Ship.BOAT?" · 免费配备":" · 库存"+c.ships[ship.ordinal()-1]),ship->
                    choose("编队兵力",troopOptions(leader.id),n1->n1+"人",troops->
                        choose("出征携粮",Arrays.asList(troops,2*troops,6*troops,10*troops),food->food+"粮",food->{
                            List<World.Officer> crew=new ArrayList<>();crew.add(leader);for(int id:deputies)crew.add(w.officer(id));
                            StringBuilder text=new StringBuilder("主将 "+leader.name);for(int id:deputies)text.append("\n副将 ").append(w.officer(id).name);
                            text.append("\n").append(troops).append("兵 · ").append(food).append("粮 · ").append(ship.label)
                                .append("\n兵装 ").append(weapon.label).append(" · 消耗 ").append(Army.equipmentNeeded(weapon,troops)).append(Army.siegeWeapon(weapon)?"件":"份")
                                .append("\n陆战适性 ").append(War.rankLabel(w.army.aptitude(crew,Army.category(weapon)))).append(" · 水军适性 ").append(War.rankLabel(w.army.aptitude(crew,5)))
                                .append("\n行动力10；整队武将出征后不能再执行城池任务。\n统率取主将，武力、智力与适性取队内最高值。");
                            confirm("确认编队出征",text.toString(),()->{World.Result result=w.army.deploy(c.id,leader.id,deputies,weapon,ship,troops,food);apply.accept(result);if(result.ok)focus.accept(w.unit(leader.unitId).hex);});
                        }))));
        }).setNegativeButton("取消",null).create();dialog.show();
    });}
    void manufacture(World.City c){
        List<String> labels=new ArrayList<>();List<World.Weapon> weapons=Arrays.asList(World.Weapon.RAM,World.Weapon.SIEGE_TOWER,World.Weapon.WOODEN_BEAST,World.Weapon.CATAPULT);
        for(World.Weapon weapon:weapons)labels.add(weapon.label+" · 金"+Army.productionGold(weapon));for(Army.Ship ship:Arrays.asList(Army.Ship.TOWER_SHIP,Army.Ship.WARSHIP))labels.add(ship.label+" · 金"+ship.gold);
        new AlertDialog.Builder(a).setTitle("军备制造").setItems(labels.toArray(new String[0]),(d,index)->choose("选择制造武将",w.idle(c),o->o.name+" · 政"+o.politics,o->{
            World.Weapon weapon=index<4?weapons.get(index):null;Army.Ship ship=index<4?null:Army.Ship.values()[index-3];String error=w.army.productionError(c.id,o.id,weapon,ship);
            if(error!=null){info("暂不能制造",error);return;}
            confirm("制造"+(weapon==null?ship.label:weapon.label),"金"+(weapon==null?ship.gold:Army.productionGold(weapon))+"、行动力10，占用"+o.name+w.skills.productionTurns(o.id,weapon)+"旬。\n完成1件；城池失守或工场拆除时中止。",()->apply.accept(w.army.produce(c.id,o.id,weapon,ship)));
        })).setNegativeButton("取消",null).show();
    }
    void production(Army.Production p){confirm(p.label(),w.city(p.cityId).name+" · "+w.officer(p.officerId).name+" · 剩余"+w.officer(p.officerId).otherTaskTurns+"旬\n是否中止？已付费用不退还。",()->apply.accept(w.army.cancelProduction(p.officerId)));}
    void tactics(World.Unit u){choose("兵器 / 水军战法",w.army.tactics(u),t->t.label+" · 气力"+t.energy,t->{
        List<Hex> targets=new ArrayList<>();for(World.Unit enemy:w.units)if(w.army.tacticError(u.id,enemy.hex,t)==null)targets.add(enemy.hex);for(World.City city:w.cities)if(w.army.tacticError(u.id,city.hex,t)==null)targets.add(city.hex);for(War.Structure structure:w.war.structures())if(w.army.tacticError(u.id,structure.hex,t)==null)targets.add(structure.hex);
        choose("选择军备战法目标",targets,h->w.cityAt(h)!=null?w.cityAt(h).name:w.war.at(h)!=null?w.war.at(h).kind.label:w.officer(w.unitAt(h).officerId).name,h->confirm(t.label,t.effect+"\n成功率"+w.army.tacticChance(u.id,h)+"%；消耗"+t.energy+"气力，失败也消耗行动。",()->apply.accept(w.army.tactic(u.id,h,t))));
    });}
}

package game.sanguo.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** All dialogs collect choices first. Only the final confirmation changes the world. */
final class ArmyUi {
    private final MainActivity a;private final World w;private final Consumer<World.Result> apply;private final Consumer<Hex> focus;
    ArmyUi(MainActivity a,World w,Consumer<World.Result> apply,Consumer<Hex> focus){this.a=a;this.w=w;this.apply=apply;this.focus=focus;}
    private void info(String title,String text){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,Runnable action){a.commandDialog(title,text,"执行",w,action);}
    private <T> void choose(String title,List<T> values,java.util.function.Function<T,String> label,Consumer<T> next){ChoiceDialog.show(a,w,title,values,label,next);}
    void quickDeploy(World.City c){new DeployWizard(a,w,this,DeployWizard.start(a,c,true)).show();}
    void restoreDraft(Bundle draft){new DeployWizard(a,w,this,draft).show();}

    void deploy(World.City c){new DeployWizard(a,w,this,DeployWizard.start(a,c,false)).show();}
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
        List<Hex> targets=new ArrayList<>();for(World.Unit enemy:w.fieldUnits())if(w.army.tacticError(u.id,enemy.hex,t)==null)targets.add(enemy.hex);for(World.City city:w.cities)if(w.army.tacticError(u.id,city.hex,t)==null)targets.add(city.hex);for(War.Structure structure:w.war.structures())if(w.army.tacticError(u.id,structure.hex,t)==null)targets.add(structure.hex);for(Domestic.Facility f:w.domestic.facilities)if(w.army.tacticError(u.id,f.hex,t)==null)targets.add(f.hex);
        if(targets.isEmpty()){World.Unit nearest=w.fieldUnits().stream().filter(x->w.campaign.hostile(u.owner,x.owner)).min(Comparator.comparingInt(x->u.hex.distance(x.hex))).orElse(null);info("不能发动"+t.label,w.army.tacticError(u.id,nearest==null?u.hex:nearest.hex,t));return;}
        a.pickOnMap(t.label+" · 选择目标",u.hex,targets,h->a.showTacticPreview(w,w.army.tacticPreview(u.id,h,t),()->apply.accept(w.army.tactic(u.id,h,t))),h->h==null?"目标在地图范围外":w.army.tacticError(u.id,h,t));
    });}
}

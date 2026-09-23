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
    private final MainActivity a;private final World w;private final LegacyCommandSink apply;private final Consumer<Hex> focus;
    ArmyUi(MainActivity a,World w,LegacyCommandSink apply,Consumer<Hex> focus){this.a=a;this.w=w;this.apply=apply;this.focus=focus;}
    private void info(String title,String text){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,Runnable action){a.commandDialog(title,text,"执行",w,action);}
    private <T> void choose(String title,List<T> values,java.util.function.Function<T,String> label,Consumer<T> next){ChoiceDialog.show(a,w,title,values,label,next);}
    void quickDeploy(World.City c){new DeployWizard(a,w,this,DeployWizard.start(a,c,true)).show();}
    void restoreDraft(Bundle draft){new DeployWizard(a,w,this,draft).show();}

    void deploy(World.City c){new DeployWizard(a,w,this,DeployWizard.start(a,c,false)).show();}
    void basicProduction(World.City c){
        choose("生产基础兵装",Arrays.asList(World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CROSSBOW,World.Weapon.CAVALRY),
            weapon->weapon.label+" · "+w.domestic.usage(c.id,Domestic.productionFacility(weapon)),weapon->{
                Domestic.Kind facility=Domestic.productionFacility(weapon);String error=w.domestic.operationError(c.id,facility);
                if(error!=null){info("暂不能生产",error);return;}
                choose("选择生产武将",w.idle(c),o->o.name+" · 政"+o.politics,o->confirm("生产"+weapon.label,
                    w.domestic.usage(c.id,facility)+"\n"+o.name+"生产"+w.skills.produceAmount(c.id,o.id,weapon)+"份；花费金"+w.skills.productionGold(o.id,weapon)+"、行动力10。",()->apply.execute(w,()->w.produce(c.id,o.id,weapon))));
            });
    }
    void manufacture(World.City c){
        List<String> labels=new ArrayList<>();List<World.Weapon> weapons=Arrays.asList(World.Weapon.RAM,World.Weapon.SIEGE_TOWER,World.Weapon.WOODEN_BEAST,World.Weapon.CATAPULT);
        for(World.Weapon weapon:weapons)labels.add(weapon.label+" · 金"+Army.productionGold(weapon)+" · "+w.domestic.usage(c.id,Domestic.Kind.WORKSHOP));for(Army.Ship ship:Arrays.asList(Army.Ship.TOWER_SHIP,Army.Ship.WARSHIP))labels.add(ship.label+" · 金"+ship.gold+" · "+w.domestic.usage(c.id,Domestic.Kind.SHIPYARD));
        new AlertDialog.Builder(a).setTitle("军备制造").setItems(labels.toArray(new String[0]),(d,index)->{
            String facilityError=w.domestic.operationError(c.id,index<4?Domestic.Kind.WORKSHOP:Domestic.Kind.SHIPYARD);
            if(facilityError!=null){info("暂不能制造",facilityError);return;}
            choose("选择制造武将",w.idle(c),o->o.name+" · 政"+o.politics,o->{
            World.Weapon weapon=index<4?weapons.get(index):null;Army.Ship ship=index<4?null:Army.Ship.values()[index-3];String error=w.army.productionError(c.id,o.id,weapon,ship);
            if(error!=null){info("暂不能制造",error);return;}
            confirm("制造"+(weapon==null?ship.label:weapon.label),"金"+(weapon==null?ship.gold:Army.productionGold(weapon))+"、行动力10，占用"+o.name+w.skills.productionTurns(o.id,weapon)+"旬。\n完成1件；城池失守或工场拆除时中止。",()->apply.execute(w,()->w.army.produce(c.id,o.id,weapon,ship)));
        });}).setNegativeButton("取消",null).show();
    }
    void production(Army.Production p){confirm(p.label(),w.city(p.cityId).name+" · "+w.officer(p.officerId).name+" · 剩余"+w.officer(p.officerId).otherTaskTurns+"旬\n是否中止？已付费用不退还。",()->apply.execute(w,()->w.army.cancelProduction(p.officerId)));}
    void tactics(World.Unit u){choose("兵器 / 水军战法",w.army.tactics(u),t->t.label+" · 气力"+t.energy,t->{
        List<Hex> targets=new ArrayList<>();for(World.Unit enemy:w.fieldUnits())if(w.army.tacticError(u.id,enemy.hex,t)==null)targets.add(enemy.hex);for(World.City city:w.cities)if(w.army.tacticCityError(u.id,city.id,t)==null)for(Hex h:SiteFootprint.cells(city))if(h.equals(w.army.cityTacticHit(u,city,t,h)))targets.add(h);for(War.Structure structure:w.war.structures())if(w.army.tacticError(u.id,structure.hex,t)==null)targets.add(structure.hex);for(Domestic.Facility f:w.domestic.facilities)if(w.army.tacticError(u.id,f.hex,t)==null)targets.add(f.hex);
        if(targets.isEmpty()){World.Unit nearest=w.fieldUnits().stream().filter(x->w.campaign.hostile(u.owner,x.owner)).min(Comparator.comparingInt(x->u.hex.distance(x.hex))).orElse(null);info("不能发动"+t.label,w.army.tacticError(u.id,nearest==null?u.hex:nearest.hex,t));return;}
        a.pickOnMap(t.label+" · 选择目标",u.hex,targets,h->{World.City c=w.cityAt(h);World.Unit b=w.unitAt(h);
            Runnable field=()->a.showTacticPreview(w,w.army.tacticPreview(u.id,h,t),()->apply.execute(w,()->w.army.tactic(u.id,h,t)));
            Runnable site=()->a.showTacticPreview(w,w.army.tacticCityPreview(u.id,c.id,t),()->apply.execute(w,()->w.army.tacticCity(u.id,c.id,t)));
            if(c!=null&&b!=null)new AlertDialog.Builder(a).setTitle("同格战法目标").setItems(new String[]{w.officer(b.officerId).name+" · 部队",c.name+" · 据点"},(d,i)->{if(i==0)field.run();else site.run();}).setNegativeButton("取消",null).show();
            else if(c!=null)site.run();else field.run();},h->h==null?"目标在地图范围外":w.army.tacticError(u.id,h,t));
    });}
}

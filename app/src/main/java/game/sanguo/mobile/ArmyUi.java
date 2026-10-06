package game.sanguo.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.*;
import java.util.*;
import java.util.function.*;

/** All dialogs collect choices first. Only the final confirmation changes the world. */
final class ArmyUi {
    private final MainActivity a;private final World w;private final LegacyCommandSink apply;private final Consumer<Hex> focus;
    ArmyUi(MainActivity a,World w,LegacyCommandSink apply,Consumer<Hex> focus){this.a=a;this.w=w;this.apply=apply;this.focus=focus;}
    private void info(String title,String text){UiTheme.dialog(new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show());}
    private void confirm(String title,String text,Runnable action){a.commandDialog(title,text,"执行",w,action);}
    private <T> void choose(String title,List<T> values,java.util.function.Function<T,String> label,Consumer<T> next){ChoiceDialog.show(a,w,title,values,label,next);}
    void quickDeploy(World.City c){new DeployWizard(a,w,this,DeployWizard.start(a,c,true)).show();}
    void restoreDraft(Bundle draft){new DeployWizard(a,w,this,draft).show();}

    void deploy(World.City c){new DeployWizard(a,w,this,DeployWizard.start(a,c,false)).show();}
    void basicProduction(World.City c){
        ChoiceDialog.show(a,w,"生产基础兵装",Arrays.asList(World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CROSSBOW,World.Weapon.CAVALRY),
            weapon->weapon.label,weapon->reviewProduction(c,"EQUIPMENT",weapon.name(),weapon.label,()->basicProduction(c)));
    }
    void manufacture(World.City c){
        List<String> items=Arrays.asList("RAM","SIEGE_TOWER","WOODEN_BEAST","CATAPULT","TOWER_SHIP","WARSHIP");
        ChoiceDialog.show(a,w,"军备制造",items,item->productionLabel(item),item->reviewProduction(c,item.endsWith("SHIP")?"SHIP":"EQUIPMENT",item,productionLabel(item),()->manufacture(c)));
    }
    private String productionLabel(String item){return item.endsWith("SHIP")?Army.Ship.valueOf(item).label:World.Weapon.valueOf(item).label;}
    private void reviewProduction(World.City c,String operation,String item,String label,Runnable back){
        final AlertDialog[] picker={null};
        picker[0]=DataTable.chooseRetained(a,w,"生产"+label+" · 选择执行武将",w.idle(c),o->{
            ProductionCommand command=new ProductionCommand(a.deploymentState(),operation,c.id,o.id,item);
            ProductionPreview p=a.previewProduction(command);StringBuilder detail=new StringBuilder(c.name+" · 执行武将 "+o.name);
            if(p.resources!=null){ProductionPreview.Resources r=p.resources;
                detail.append("\n消耗金 ").append(r.goldCost).append(" / 行动力 ").append(r.actionPointsCost)
                    .append("\n当前金 ").append(r.goldAvailable).append(" / 行动力 ").append(r.actionPointsAvailable)
                    .append("\n库存 ").append(r.stockBefore).append(" / 容量 ").append(r.stockCapacity)
                    .append("\n本旬设施剩余次数 ").append(r.facilityUsesBefore).append(" / 总次数 ").append(r.facilityCapacity);
            }
            if(p.effects!=null){ProductionPreview.Effects e=p.effects;
                if(e.delayed)detail.append("\n\n预计制造 ").append(e.outputQuantity).append("件，占用武将 ").append(e.busyTurns).append("旬。")
                    .append("\n派工后库存仍为 ").append(e.stockAfterImmediate).append("，在制数量 ").append(e.pendingAfter)
                    .append("。\n任务完成后才入库；城池失守或工场拆除可能中止。");
                else detail.append("\n\n立即入库 ").append(e.outputQuantity).append("，库存变为 ").append(e.stockAfterImmediate).append("。");
                detail.append("\n执行后金 ").append(e.goldAfter).append(" / 行动力 ").append(e.actionPointsAfter);
            }
            if(!p.allowed())detail.append("\n\n不能执行：").append(MainActivity.commandError(p.error,p.detail));
            AlertDialog review=a.commandDialog("生产"+label,detail.toString(),"执行","返回修改",w,()->{picker[0].dismiss();a.executeProduction(command);});
            review.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(p.allowed());review.setOnDismissListener(d->{if(picker[0].isShowing())a.trackDialog(picker[0]);});
        });
        picker[0].getButton(AlertDialog.BUTTON_NEGATIVE).setText("上一步");picker[0].getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->{picker[0].dismiss();back.run();});picker[0].setOnCancelListener(d->back.run());
    }
    void production(Army.Production p){confirm(p.label(),w.city(p.cityId).name+" · "+w.officer(p.officerId).name+" · 剩余"+w.officer(p.officerId).otherTaskTurns+"旬\n是否中止？已付费用不退还。",()->apply.execute(w,()->w.army.cancelProduction(p.officerId)));}
    void tactics(World.Unit u){ChoiceDialog.enabledChoices(a,w,"兵器 / 水军战法",w.army.tactics(u),
        t->t.label+" · 气力"+w.army.tacticCost(u,t)+(w.army.tacticFormationError(u,t)==null?"":"\n"+w.army.tacticFormationError(u,t)),
        t->w.army.tacticFormationError(u,t)==null,t->{
        List<Hex> targets=new ArrayList<>();for(World.Unit enemy:w.fieldUnits())if(w.army.tacticError(u.id,enemy.hex,t)==null)targets.add(enemy.hex);for(World.City city:w.cities)if(w.army.tacticCityError(u.id,city.id,t)==null)for(Hex h:SiteFootprint.cells(city))if(h.equals(w.army.cityTacticHit(u,city,t,h)))targets.add(h);for(War.Structure structure:w.war.structures())if(w.army.tacticError(u.id,structure.hex,t)==null)targets.add(structure.hex);for(Domestic.Facility f:w.domestic.facilities)if(w.army.tacticError(u.id,f.hex,t)==null)targets.add(f.hex);
        if(targets.isEmpty()){World.Unit nearest=w.fieldUnits().stream().filter(x->w.campaign.hostile(u.owner,x.owner)).min(Comparator.comparingInt(x->u.hex.distance(x.hex))).orElse(null);info("不能发动"+t.label,w.army.tacticError(u.id,nearest==null?u.hex:nearest.hex,t));return;}
        a.pickOnMap(t.label+" · 选择目标",u.hex,targets,h->{World.City c=w.cityAt(h);World.Unit b=w.unitAt(h);
            Runnable field=()->a.showTacticPreview(w,w.army.tacticPreview(u.id,h,t),()->apply.execute(w,()->w.army.tactic(u.id,h,t)));
            Runnable site=()->a.showTacticPreview(w,w.army.tacticCityPreview(u.id,c.id,t),()->apply.execute(w,()->w.army.tacticCity(u.id,c.id,t)));
            if(c!=null&&b!=null)UiTheme.dialog(new AlertDialog.Builder(a).setTitle("同格战法目标").setItems(new String[]{w.officer(b.officerId).name+" · 部队",c.name+" · 据点"},(d,i)->{if(i==0)field.run();else site.run();}).setNegativeButton("取消",null).show());
            else if(c!=null)site.run();else field.run();},h->h==null?"目标在地图范围外":w.army.tacticError(u.id,h,t));
    });}
}

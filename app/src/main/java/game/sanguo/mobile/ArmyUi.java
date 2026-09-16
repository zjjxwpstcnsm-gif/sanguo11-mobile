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
    private <T> void choose(String title,List<T> list,Function<T,String> label,Consumer<T> next){
        if(list.isEmpty()){info(title,"没有可用选项");return;}String[] names=new String[list.size()];for(int i=0;i<names.length;i++)names[i]=label.apply(list.get(i));
        AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(title).setNegativeButton("取消",null);
        if(GameIcon.supports(list.get(0)))dialog.setAdapter(GameIcon.adapter(a,w,list,label),(d,i)->next.accept(list.get(i)));
        else dialog.setItems(names,(d,i)->next.accept(list.get(i)));dialog.show();
    }
    void quickDeploy(World.City c){new DeployWizard(a,w,this,DeployWizard.start(a,c,true)).show();}
    void restoreDraft(Bundle draft){
        if(draft.getBoolean("wizard")){new DeployWizard(a,w,this,draft).show();return;}
        World.City c=w.city(draft.getInt("city"));World.Officer leader=w.officer(draft.getInt("leader"));
        if(c==null||leader==null||!w.idle(c).contains(leader)){a.closeForm();info("草稿需要重新选将","库存和武将状态已变化，请重新打开出征。");return;}
        quantities(c,leader,draft.getIntArray("deputies"),World.Weapon.values()[Math.max(0,Math.min(World.Weapon.values().length-1,draft.getInt("weapon")))],Army.Ship.values()[Math.max(0,Math.min(Army.Ship.values().length-1,draft.getInt("ship")))]);
    }
    void quantities(World.City c,World.Officer leader,int[] deputies,World.Weapon weapon,Army.Ship ship){
        Bundle previous=new Bundle(a.formDraft()),draft=new Bundle(a.formDraft());draft.putString("step","quantity");draft.putString("kind","deploy");draft.putInt("city",c.id);draft.putInt("leader",leader.id);draft.putIntArray("deputies",deputies);draft.putInt("weapon",weapon.ordinal());draft.putInt("ship",ship.ordinal());draft.putBoolean("open",true);
        boolean reusable="deploy".equals(previous.getString("kind"))&&previous.getInt("city")==c.id;
        int troopCap=UiModels.deployTroopCap(w,c,leader.id,weapon,ship);
        if(troopCap<1000){draft.putString("step","weapon");a.rememberForm(draft);new DeployWizard(a,w,this,draft).show();info("无法出征","兵力、粮食或选定兵装/舰船不足，至少需要1000兵和1000粮。请修改兵装或返回。原数量保留。");return;}
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(a.dp(16),a.dp(8),a.dp(16),a.dp(12));
        form.addView(a.text(leader.name+" · "+weapon.label+" · "+ship.label+"\n带兵上限 "+w.government.commandLimit(leader.id)+" · 兵装库存 "+c.equipment[weapon.ordinal()],14,a.gold));
        int initial=Math.min(3000,troopCap);
        QuantityControl troops=new QuantityControl(a,"兵力",1000,troopCap,initial);
        QuantityControl food=new QuantityControl(a,"粮食",initial,Math.min(c.food,1000000),Math.min(c.food,initial*2));
        QuantityControl gold=new QuantityControl(a,"金钱",0,Math.min(c.gold,10000),0);
        if(reusable){troops.restoreValue(previous.getString("troops",troops.draftValue()));food.restoreValue(previous.getString("food",food.draftValue()));gold.restoreValue(previous.getString("gold",gold.draftValue()));}
        if(troops.valid())food.bounds(troops.value(),Math.min(c.food,1000000));
        StringBuilder crew=new StringBuilder("主将 "+leader.name+" · "+Skill.label(leader.skillId));for(int id:deputies){World.Officer o=w.officer(id);if(o!=null)crew.append("\n副将 ").append(o.name).append(" · ").append(Skill.label(o.skillId));}form.addView(a.text(crew.toString(),13,a.paper));
        TextView summary=a.text("",13,a.gold);form.addView(summary);
        form.addView(troops);form.addView(food);form.addView(gold);

        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("出征兵钱粮 · 滑动调整").setView(scroll).setPositiveButton("确认出征",null).setNeutralButton("上一步",(d,n)->{draft.putString("step",draft.getBoolean("quick")?"weapon":"ship");a.rememberForm(draft);new DeployWizard(a,w,this,draft).show();}).setNegativeButton("取消",(d,n)->a.closeForm()).create();
        dialog.setOnCancelListener(d->a.closeForm());
        Runnable update=()->{
            draft.putString("troops",troops.draftValue());draft.putString("food",food.draftValue());draft.putString("gold",gold.draftValue());a.rememberForm(draft);
            boolean valid=troops.valid()&&food.valid()&&gold.valid()&&food.value()>=troops.value();
            summary.setText(valid?"城内保留：兵 "+(c.troops-troops.value())+" / 粮 "+(c.food-food.value())+" / 金 "+(c.gold-gold.value())+"\n携粮基础续航约"+(food.value()/Math.max(1,(troops.value()+19)/20))+"旬（实际受地形/设施影响）\n兵装消耗 "+Army.equipmentNeeded(weapon,troops.value())+" · 行动力10\n滑动粗调，也可点击数字精确输入。":"请检查数量范围；携粮不能少于兵力。");
            if(dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(valid);
        };
        troops.onChange(()->{if(troops.valid())food.bounds(troops.value(),Math.min(c.food,1000000));update.run();});food.onChange(update);gold.onChange(update);
        final byte[] opened;try{opened=SaveCodec.encode(w);}catch(java.io.IOException e){info("无法出征","无法校验当前局面");return;}
        boolean[] submitted={false};
        dialog.setOnShowListener(v->{
            update.run();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
                if(submitted[0])return;
                if(!a.currentWorld(w)){info("无法出征","局面已变化，请重新打开出征");return;}
                try{if(!Arrays.equals(opened,SaveCodec.encode(w))){dialog.dismiss();quantities(c,leader,deputies,weapon,ship);info("库存或人物状态已变化","已刷新当前库存与人物状态，请重新核对后确认。本次未出征。");return;}}catch(java.io.IOException e){info("无法出征","局面校验失败");return;}
                if(!troops.valid()||!food.valid()||!gold.valid()||food.value()<troops.value())return;
                submitted[0]=true;World.Result result=w.army.deploy(c.id,leader.id,deputies,weapon,ship,troops.value(),food.value(),gold.value());
                apply.accept(result);if(result.ok){a.closeForm();dialog.dismiss();focus.accept(w.unit(leader.unitId).hex);}else submitted[0]=false;
            });
            dialog.getWindow().setLayout(a.dp(Math.min(560,a.getResources().getConfiguration().screenWidthDp-24)),android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        });
        dialog.show();a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
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

package game.sanguo.mobile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;

/** One deployment = one fresh draft. Only recreation and officer picking resume that draft. */
final class DeployWizard {
    private final MainActivity a;private final World w;private final Bundle draft;
    DeployWizard(MainActivity a,World w,ArmyUi army,Bundle draft){this.a=a;this.w=w;this.draft=new Bundle(draft);}
    static Bundle start(MainActivity a,World.City c,boolean quick){
        Bundle d=new Bundle();d.putString("kind","deploy");d.putInt("city",c.id);d.putBoolean("wizard",true);
        d.putBoolean("quick",quick);d.putBoolean("open",true);d.putString("step","overview");d.putIntArray("deputies",new int[0]);return d;
    }
    private void save(){draft.putBoolean("open",true);a.rememberForm(draft);}
    private World.Weapon weapon(){return World.Weapon.values()[Math.max(0,Math.min(World.Weapon.values().length-1,draft.getInt("weapon",0)))];}
    private Army.Ship ship(){return Army.Ship.values()[Math.max(0,Math.min(Army.Ship.values().length-1,draft.getInt("ship",0)))];}
    private int[] deputies(){int[] ids=draft.getIntArray("deputies");return ids==null?new int[0]:ids;}
    private String name(int id){World.Officer o=w.officer(id);return o==null?"点击选择":o.name;}
    private int number(String key,int fallback){try{return Integer.parseInt(draft.getString(key,""+fallback));}catch(NumberFormatException e){return fallback;}}
    private int cap(World.City c){int leader=draft.getInt("leader",-1);return leader<0||!draft.containsKey("weapon")?Math.min(10000,c.troops):UiModels.deployTroopCap(w,c,leader,weapon(),ship());}
    void show(){
        World.City c=w.city(draft.getInt("city",-1));if(c==null||c.owner!=w.player){a.closeForm();return;}save();
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(a.dp(12),a.dp(6),a.dp(12),a.dp(12));
        final AlertDialog[] holder={null};final Runnable[] update={()->{}};
        int cap=cap(c);
        QuantityControl troops=new QuantityControl(a,"兵力",1000,Math.max(1000,cap),number("troops",Math.min(3000,Math.max(1000,cap))));
        QuantityControl food=new QuantityControl(a,"粮食",0,Math.min(c.food,1000000),number("food",Logistics.defaultFood(troops.value(),c.food)));
        QuantityControl gold=new QuantityControl(a,"金钱",0,Math.min(c.gold,10000),number("gold",0));
        for(String key:new String[]{"troops","food","gold"})if(draft.containsKey(key))(key.equals("troops")?troops:key.equals("food")?food:gold).restoreValue(draft.getString(key));
        form.addView(a.text(c.name+" · 兵种直接点选，无需进入详情",14,a.gold));
        Runnable changed=()->{int max=Math.max(1000,cap(c));troops.bounds(1000,max);if(troops.value()>max)troops.set(max);update[0].run();};
        InlineChoices<World.Weapon> weapons=new InlineChoices<>(a,w,Arrays.asList(World.Weapon.values()),5,
            k->k.label+"\n"+(k==World.Weapon.SWORD?"无需库存":"库存 "+c.equipment[k.ordinal()]),
            k->k==World.Weapon.SWORD||c.equipment[k.ordinal()]>=Army.equipmentNeeded(k,1000),
            draft.containsKey("weapon")?weapon():null,k->{draft.putInt("weapon",k.ordinal());changed.run();});
        weapons.setTag("deploy.weapons");form.addView(weapons);
        form.addView(a.text("舰船 · 默认走舸，水战船只直接切换",12,a.muted));
        InlineChoices<Army.Ship> ships=new InlineChoices<>(a,w,Arrays.asList(Army.Ship.values()),3,
            s->s.label+"\n"+(s==Army.Ship.BOAT?"免费携带":"库存 "+c.ships[s.ordinal()-1]),
            s->s==Army.Ship.BOAT||c.ships[s.ordinal()-1]>0,ship(),s->{draft.putInt("ship",s.ordinal());changed.run();});
        ships.setTag("deploy.ships");form.addView(ships);
        troops.setTag("deploy.troops");food.setTag("deploy.food");
        TextView ration=a.text("",12,a.gold);ration.setTag("deploy.rations");food.addView(ration,1);
        form.addView(troops);form.addView(food);form.addView(gold);
        form.addView(a.text("编队武将 · 主将必选，副将可选",12,a.muted));
        LinearLayout crew=new LinearLayout(a);form.addView(crew);
        crew.addView(officerTile("主将\n"+name(draft.getInt("leader",-1)),w.officer(draft.getInt("leader",-1)),v->{holder[0].dismiss();officer(c,-1);}),new LinearLayout.LayoutParams(0,a.dp(88),1));
        if(!draft.getBoolean("quick"))for(int slot=0;slot<2;slot++){
            final int index=slot;int[] ids=deputies();int id=slot<ids.length?ids[slot]:-1;
            LinearLayout cell=new LinearLayout(a);cell.setOrientation(LinearLayout.VERTICAL);crew.addView(cell,new LinearLayout.LayoutParams(0,-2,1));
            cell.addView(officerTile("副将"+(slot+1)+"\n"+name(id),w.officer(id),v->{holder[0].dismiss();officer(c,index);}),new LinearLayout.LayoutParams(-1,a.dp(88)));
            if(id>=0)cell.addView(a.button("移除",v->{removeDeputy(index);save();holder[0].dismiss();show();}));
        }
        TextView summary=a.text("",13,a.gold);summary.setTag("deploy.summary");form.addView(summary);
        ScrollView scroll=new ScrollView(a);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("出征编队").setView(scroll).setNegativeButton("取消",(d,i)->a.closeForm()).setPositiveButton("确认出征",null).create();holder[0]=dialog;
        update[0]=()->{
            draft.putString("troops",troops.draftValue());draft.putString("food",food.draftValue());draft.putString("gold",gold.draftValue());save();
            String error=error(c,troops,food,gold);
            int use=Logistics.baseUse(troops.value(),false);
            ration.setText(use==0?"请填写有效兵力":"基础旬耗 "+use+" · 可支撑 "+Logistics.turns(food.value(),use)+" 旬");
            String supplies=use==0?"请填写有效兵力":"旬耗粮 "+use+" · 携粮可支撑 "+Logistics.turns(food.value(),use)+" 旬（不计设施减耗）";
            summary.setText(supplies+"\n调整兵力时携粮自动设为兵力×2，可再手动改粮。"+(2L*troops.value()>Math.min(c.food,1000000)?"\n库存 / 携带上限不足两倍粮；自动配粮受上限限制。":"")+"\n"+
                (error==null?"留守：兵"+(c.troops-troops.value())+" / 粮"+(c.food-food.value())+" / 金"+(c.gold-gold.value())+"\n兵装消耗"+Army.equipmentNeeded(weapon(),troops.value())+"；行动力10":error));
            if(dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error==null);
        };
        int[] previousTroops={troops.value()};
        troops.onChange(()->{if(troops.valid()&&troops.value()!=previousTroops[0]){previousTroops[0]=troops.value();food.set(Logistics.defaultFood(troops.value(),c.food));}update[0].run();});
        food.onChange(update[0]);gold.onChange(update[0]);dialog.setOnCancelListener(d->a.closeForm());
        final long revision=w.commandRevision();boolean[] submitted={false};dialog.setOnShowListener(v->{update[0].run();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{
            if(submitted[0]||!a.currentWorld(w)||error(c,troops,food,gold)!=null)return;
            if(revision!=w.commandRevision()){dialog.dismiss();show();return;}
            submitted[0]=true;World.Officer o=w.officer(draft.getInt("leader",-1));World.Result result=w.army.deploy(c.id,o.id,deputies(),weapon(),ship(),troops.value(),food.value(),gold.value());a.applyResult(result);
            if(result.ok){a.closeForm();dialog.dismiss();a.selectAndFocus(w.unit(o.unitId).hex);}else submitted[0]=false;
        });dialog.getWindow().setLayout(a.dp(Math.min(560,a.getResources().getConfiguration().screenWidthDp-24)),-2);});
        dialog.show();a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private Button officerTile(String label,World.Officer officer,View.OnClickListener click){
        Button button=a.button(label,click);button.setGravity(Gravity.CENTER);button.setMaxLines(2);button.setPadding(a.dp(4),a.dp(8),a.dp(4),a.dp(8));
        if(officer!=null){android.graphics.drawable.Drawable icon=GameIcon.drawable(a,w,officer);icon.setBounds(0,0,a.dp(32),a.dp(32));button.setCompoundDrawables(null,icon,null,null);button.setCompoundDrawablePadding(a.dp(3));}
        return button;
    }
    private String error(World.City c,QuantityControl troops,QuantityControl food,QuantityControl gold){
        if(!draft.containsKey("weapon"))return "请先点选一个兵种图标";
        World.Officer leader=w.officer(draft.getInt("leader",-1));if(!w.idle(c).contains(leader))return "请选择当前可用的主将";
        for(int id:deputies())if(id==leader.id||!w.idle(c).contains(w.officer(id)))return "副将已被占用或与主将重复，请重新选择";
        if(!troops.valid()||!food.valid()||!gold.valid()||troops.value()>UiModels.deployTroopCap(w,c,leader.id,weapon(),ship())||food.value()<troops.value())return "检查兵装库存与数量；至少1000兵，携粮不少于兵力";
        return null;
    }
    private void removeDeputy(int slot){List<Integer> ids=new ArrayList<>();for(int id:deputies())ids.add(id);if(slot<ids.size())ids.remove(slot);draft.putIntArray("deputies",ids.stream().mapToInt(i->i).toArray());}
    private void officer(World.City c,int slot){
        List<World.Officer> options=new ArrayList<>(w.idle(c));if(slot>=0)options.removeIf(o->o.id==draft.getInt("leader",-1));
        DataTable.choose(a,w,slot<0?"选择主将":"选择副将"+(slot+1),options,o->"",o->{
            List<Integer> ids=new ArrayList<>();for(int id:deputies())ids.add(id);
            if(slot<0){draft.putInt("leader",o.id);ids.remove(Integer.valueOf(o.id));if(draft.getBoolean("quick"))ids.clear();}
            else{ids.remove(Integer.valueOf(o.id));if(slot<ids.size())ids.set(slot,o.id);else ids.add(o.id);}
            draft.putIntArray("deputies",ids.stream().mapToInt(i->i).toArray());save();show();
        },this::show);
    }
}

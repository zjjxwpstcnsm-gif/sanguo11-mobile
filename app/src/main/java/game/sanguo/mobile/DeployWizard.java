package game.sanguo.mobile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;

/** A single editable deployment overview; no command runs until the final button. */
final class DeployWizard {
    private final MainActivity a;private final World w;private final Bundle draft;
    DeployWizard(MainActivity a,World w,ArmyUi army,Bundle draft){this.a=a;this.w=w;this.draft=new Bundle(draft);}
    static Bundle start(MainActivity a,World.City c,boolean quick){
        Bundle old=a.formDraft(),d="deploy".equals(old.getString("kind"))&&old.getInt("city",-1)==c.id?new Bundle(old):new Bundle();
        d.putString("kind","deploy");d.putInt("city",c.id);d.putBoolean("wizard",true);d.putBoolean("quick",quick);d.putBoolean("open",true);d.putString("step","overview");
        if(!d.containsKey("deputies"))d.putIntArray("deputies",new int[0]);return d;
    }
    private void save(){draft.putBoolean("open",true);a.rememberForm(draft);}
    private World.Weapon weapon(){return World.Weapon.values()[Math.max(0,Math.min(World.Weapon.values().length-1,draft.getInt("weapon",0)))];}
    private Army.Ship ship(){return Army.Ship.values()[Math.max(0,Math.min(Army.Ship.values().length-1,draft.getInt("ship",0)))];}
    private int[] deputies(){int[] ids=draft.getIntArray("deputies");return ids==null?new int[0]:ids;}
    private String name(int id){World.Officer o=w.officer(id);return o==null?"点击选择":o.name;}
    private int number(String key,int fallback){try{return Integer.parseInt(draft.getString(key,""+fallback));}catch(NumberFormatException e){return fallback;}}
    void show(){
        World.City c=w.city(draft.getInt("city",-1));if(c==null||c.owner!=w.player){a.closeForm();return;}save();
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(a.dp(12),a.dp(6),a.dp(12),a.dp(12));
        final AlertDialog[] holder={null};
        form.addView(a.text(c.name+" · 点击任一项目修改",15,a.gold));
        form.addView(selection("主将 · "+name(draft.getInt("leader",-1)),w.officer(draft.getInt("leader",-1)),v->{holder[0].dismiss();officer(c,-1);}));
        if(!draft.getBoolean("quick"))for(int slot=0;slot<2;slot++){final int index=slot;int[] ids=deputies();int id=slot<ids.length?ids[slot]:-1;form.addView(selection("副将"+(slot+1)+" · "+name(id),w.officer(id),v->{holder[0].dismiss();officer(c,index);}));}
        form.addView(selection("兵装 · "+weapon().label+(weapon()==World.Weapon.SWORD?" · 无需库存":" · 库存"+c.equipment[weapon().ordinal()]),weapon(),v->{holder[0].dismiss();equipment(c,false);}));
        form.addView(selection("舰船 · "+ship().label+(ship()==Army.Ship.BOAT?" · 免费": " · 库存"+c.ships[ship().ordinal()-1]),ship(),v->{holder[0].dismiss();equipment(c,true);}));
        int leader=draft.getInt("leader",-1);int cap=leader<0?Math.min(10000,c.troops):UiModels.deployTroopCap(w,c,leader,weapon(),ship());
        QuantityControl troops=new QuantityControl(a,"兵力",1000,Math.max(1000,cap),number("troops",Math.min(3000,Math.max(1000,cap))));
        QuantityControl food=new QuantityControl(a,"粮食",0,Math.min(c.food,1000000),number("food",Math.min(c.food,6000)));
        QuantityControl gold=new QuantityControl(a,"金钱",0,Math.min(c.gold,10000),number("gold",0));
        for(String key:new String[]{"troops","food","gold"})if(draft.containsKey(key))(key.equals("troops")?troops:key.equals("food")?food:gold).restoreValue(draft.getString(key));
        form.addView(troops);form.addView(food);form.addView(gold);TextView summary=a.text("",13,a.gold);form.addView(summary);
        ScrollView scroll=new ScrollView(a);scroll.addView(form);AlertDialog dialog=new AlertDialog.Builder(a).setTitle("出征编队总览").setView(scroll).setNegativeButton("取消",(d,i)->a.closeForm()).setPositiveButton("确认出征",null).create();holder[0]=dialog;
        Runnable update=()->{draft.putString("troops",troops.draftValue());draft.putString("food",food.draftValue());draft.putString("gold",gold.draftValue());save();String error=error(c,troops,food,gold);summary.setText(error==null?"留守：兵"+(c.troops-troops.value())+" / 粮"+(c.food-food.value())+" / 金"+(c.gold-gold.value())+"\n携粮约"+(food.value()/Math.max(1,(troops.value()+19)/20))+"旬；兵装消耗"+Army.equipmentNeeded(weapon(),troops.value())+"；行动力10":error);if(dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error==null);};
        troops.onChange(update);food.onChange(update);gold.onChange(update);dialog.setOnCancelListener(d->a.closeForm());
        final long revision=w.commandRevision();boolean[] submitted={false};dialog.setOnShowListener(v->{update.run();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{
            if(submitted[0]||!a.currentWorld(w)||error(c,troops,food,gold)!=null)return;
            if(revision!=w.commandRevision()){dialog.dismiss();show();return;}
            submitted[0]=true;World.Officer o=w.officer(draft.getInt("leader",-1));World.Result result=w.army.deploy(c.id,o.id,deputies(),weapon(),ship(),troops.value(),food.value(),gold.value());a.applyResult(result);
            if(result.ok){a.closeForm();dialog.dismiss();a.selectAndFocus(w.unit(o.unitId).hex);}else submitted[0]=false;
        });dialog.getWindow().setLayout(a.dp(Math.min(560,a.getResources().getConfiguration().screenWidthDp-24)),-2);});
        dialog.show();a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private Button selection(String label,Object item,View.OnClickListener click){
        Button button=a.button(label,click);button.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);button.setMinHeight(a.dp(56));
        button.setPadding(a.dp(10),a.dp(4),a.dp(10),a.dp(4));
        if(item!=null){android.graphics.drawable.Drawable icon=GameIcon.drawable(a,w,item);icon.setBounds(0,0,a.dp(44),a.dp(44));button.setCompoundDrawables(icon,null,null,null);button.setCompoundDrawablePadding(a.dp(12));}
        return button;
    }
    private String error(World.City c,QuantityControl troops,QuantityControl food,QuantityControl gold){
        World.Officer leader=w.officer(draft.getInt("leader",-1));if(!w.idle(c).contains(leader))return "请选择当前可用的主将";
        for(int id:deputies())if(id==leader.id||!w.idle(c).contains(w.officer(id)))return "副将已被占用或与主将重复，请重新选择";
        if(!troops.valid()||!food.valid()||!gold.valid()||troops.value()>UiModels.deployTroopCap(w,c,leader.id,weapon(),ship())||food.value()<troops.value())return "检查兵装库存与数量；至少1000兵，携粮不少于兵力";
        return null;
    }
    private void officer(World.City c,int slot){
        List<World.Officer> options=new ArrayList<>(w.idle(c));if(slot>=0)options.removeIf(o->o.id==draft.getInt("leader",-1));
        DataTable.choose(a,w,slot<0?"选择主将":"选择副将"+(slot+1),options,o->"",o->{
            List<Integer> ids=new ArrayList<>();for(int id:deputies())ids.add(id);
            if(slot<0){draft.putInt("leader",o.id);ids.remove(Integer.valueOf(o.id));if(draft.getBoolean("quick"))ids.clear();}
            else{ids.remove(Integer.valueOf(o.id));if(slot<ids.size())ids.set(slot,o.id);else ids.add(o.id);}
            draft.putIntArray("deputies",ids.stream().mapToInt(i->i).toArray());save();show();
        },()->{if(slot>=0){List<Integer> ids=new ArrayList<>();for(int id:deputies())ids.add(id);if(slot<ids.size())ids.remove(slot);draft.putIntArray("deputies",ids.stream().mapToInt(i->i).toArray());save();}show();});
    }
    private void equipment(World.City c,boolean ships){
        if(ships)ChoiceDialog.show(a,w,"携带舰船",Arrays.asList(Army.Ship.values()),s->s.label+(s==Army.Ship.BOAT?" · 免费":" · 库存"+c.ships[s.ordinal()-1]),s->{draft.putInt("ship",s.ordinal());save();show();});
        else ChoiceDialog.show(a,w,"陆战兵装",Arrays.asList(World.Weapon.values()),weapon->weapon.label+(weapon==World.Weapon.SWORD?" · 无需库存":" · 库存"+c.equipment[weapon.ordinal()]),weapon->{draft.putInt("weapon",weapon.ordinal());save();show();});
    }
}

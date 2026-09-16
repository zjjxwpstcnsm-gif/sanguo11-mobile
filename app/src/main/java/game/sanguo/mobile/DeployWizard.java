package game.sanguo.mobile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;

/** ID/input-only draft. Availability and inventory always come from the current world. */
final class DeployWizard {
    private final MainActivity a;private final World w;private final ArmyUi army;private final Bundle draft;
    DeployWizard(MainActivity a,World w,ArmyUi army,Bundle draft){this.a=a;this.w=w;this.army=army;this.draft=new Bundle(draft);}
    static Bundle start(MainActivity a,World.City c,boolean quick){
        Bundle old=a.formDraft(),d="deploy".equals(old.getString("kind"))&&old.getInt("city",-1)==c.id?new Bundle(old):new Bundle();
        d.putString("kind","deploy");d.putInt("city",c.id);d.putBoolean("wizard",true);d.putBoolean("quick",quick);d.putBoolean("open",true);d.putString("step","leader");
        if(!d.containsKey("deputies"))d.putIntArray("deputies",new int[0]);return d;
    }
    private void save(){draft.putBoolean("open",true);a.rememberForm(draft);}
    private void show(String step){draft.putString("step",step);save();show();}
    void show(){
        World.City c=w.city(draft.getInt("city",-1));
        if(c==null||c.owner!=w.player){a.closeForm();info("出发城已变化，请重新选择。");return;}
        String step=draft.getString("step","quantity");
        if(!step.equals("leader")&&!w.idle(c).contains(w.officer(draft.getInt("leader",-1)))){draft.putString("step","leader");save();step="leader";info("原主将已不可用，请重新选将；其他输入仍保留。");}
        if(step.equals("leader")||step.equals("deputies")){officers(c,step.equals("deputies"));return;}
        if(step.equals("weapon")){equipment(c,false);return;}
        if(step.equals("ship")){equipment(c,true);return;}
        save();army.quantities(c,w.officer(draft.getInt("leader")),draft.getIntArray("deputies"),weapon(),ship());
    }
    private World.Weapon weapon(){int n=draft.getInt("weapon",0);return World.Weapon.values()[Math.max(0,Math.min(World.Weapon.values().length-1,n))];}
    private Army.Ship ship(){int n=draft.getInt("ship",0);return Army.Ship.values()[Math.max(0,Math.min(Army.Ship.values().length-1,n))];}
    private void info(String text){new AlertDialog.Builder(a).setMessage(text).setPositiveButton("返回",null).show();}
    private LinkedHashSet<Integer> selected(){LinkedHashSet<Integer> out=new LinkedHashSet<>();int[] ids=draft.getIntArray("deputies");if(ids!=null)for(int id:ids)out.add(id);return out;}
    private void selected(Set<Integer> ids){int[] values=new int[ids.size()];int i=0;for(int id:ids)values[i++]=id;draft.putIntArray("deputies",values);save();}
    private void officers(World.City c,boolean deputy){
        String key=deputy?"deputies":"leader";save();
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(a.dp(12),0,a.dp(12),0);
        EditText query=new EditText(a);query.setSingleLine();query.setHint("搜索武将");query.setContentDescription("出征武将搜索");query.setText(draft.getString(key+"Query",""));form.addView(query);
        LinearLayout controls=new LinearLayout(a);CheckBox all=new CheckBox(a);all.setText("显示被占用武将");all.setChecked(draft.getBoolean(key+"All"));controls.addView(all,new LinearLayout.LayoutParams(0,-2,1));
        Button clear=a.button("清空副将",v->{});if(deputy)controls.addView(clear);form.addView(controls);
        TextView status=a.text("",13,a.gold);form.addView(status);
        ListView list=new ListView(a);list.setChoiceMode(deputy?ListView.CHOICE_MODE_MULTIPLE:ListView.CHOICE_MODE_SINGLE);form.addView(list,new LinearLayout.LayoutParams(-1,a.dp(180)));
        List<World.Officer> rows=new ArrayList<>();LinkedHashSet<Integer> chosen=selected();boolean[] binding={false};
        Runnable refresh=()->{
            binding[0]=true;rows.clear();List<String> labels=new ArrayList<>();List<World.Officer> available=w.idle(c);String q=query.getText().toString().trim();
            for(World.Officer o:w.officers){if(o.owner!=c.owner||o.cityId!=c.id&&!(deputy&&chosen.contains(o.id))||deputy&&o.id==draft.getInt("leader",-1)||!o.name.contains(q))continue;
                boolean can=available.contains(o),picked=deputy?chosen.contains(o.id):draft.getInt("leader",-1)==o.id;if(!all.isChecked()&&!can&&!picked)continue;
                rows.add(o);labels.add(o.name+(deputy?" · 武"+o.war+" / 智"+o.intelligence:" · 统"+o.leadership)+(picked?" · 已选":"")+(can?" · 可用":" · 被占用/本旬已行动"));}
            list.setAdapter(new ArrayAdapter<>(a,deputy?android.R.layout.simple_list_item_multiple_choice:android.R.layout.simple_list_item_single_choice,labels));
            for(int i=0;i<rows.size();i++)list.setItemChecked(i,deputy?chosen.contains(rows.get(i).id):rows.get(i).id==draft.getInt("leader",-1));
            list.setSelectionFromTop(Math.min(Math.max(0,rows.size()-1),draft.getInt(key+"Position",0)),draft.getInt(key+"Top",0));
            status.setText(deputy?"已选"+chosen.size()+"/2人 · 不可用成员需取消选择":"选择主将；返回修改会保留编队和数量");binding[0]=false;
        };
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(deputy?"副将 · 可选0–2人":draft.getBoolean("quick")?"选择武将":"编队 · 选择主将").setView(form)
            .setNegativeButton("取消",(d,n)->a.closeForm()).setNeutralButton(deputy?"上一步":"保留返回",(d,n)->{if(deputy)show("leader");else a.closeForm();}).setPositiveButton(deputy?"下一步":"继续",null).create();
        dialog.setOnCancelListener(d->a.closeForm());dialog.setOnShowListener(v->{
            refresh.run();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
                if(!w.idle(c).contains(w.officer(draft.getInt("leader",-1)))){info("请先选择当前可用的主将。");return;}
                for(int id:chosen)if(id==draft.getInt("leader")||!w.idle(c).contains(w.officer(id))){info("副将已不可用或与主将重复，请重新选择。");return;}
                dialog.dismiss();show(deputy?"weapon":draft.getBoolean("quick")?"weapon":"deputies");
            });
        });
        list.setOnItemClickListener((parent,view,index,id)->{
            World.Officer o=rows.get(index);if(deputy&&chosen.contains(o.id)){chosen.remove(o.id);selected(chosen);refresh.run();return;}
            if(!w.idle(c).contains(o)){list.setItemChecked(index,false);info(o.name+"当前被占用或已行动，不能出征。");return;}
            if(deputy){if(chosen.size()>=2){list.setItemChecked(index,false);status.setText("最多两名副将，请先取消一人");return;}chosen.add(o.id);selected(chosen);refresh.run();}
            else{draft.putInt("leader",o.id);chosen.remove(o.id);if(draft.getBoolean("quick"))chosen.clear();selected(chosen);dialog.dismiss();show(draft.getBoolean("quick")?"weapon":"deputies");}
        });
        list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView v,int state){}public void onScroll(AbsListView v,int first,int visible,int total){if(binding[0]||v.getChildCount()==0)return;draft.putInt(key+"Position",first);draft.putInt(key+"Top",v.getChildAt(0).getTop());save();}});
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){}public void afterTextChanged(Editable s){draft.putString(key+"Query",s.toString());draft.putInt(key+"Position",0);draft.putInt(key+"Top",0);save();refresh.run();}});
        all.setOnCheckedChangeListener((v,on)->{draft.putBoolean(key+"All",on);save();refresh.run();});clear.setOnClickListener(v->{chosen.clear();selected(chosen);refresh.run();});
        dialog.show();a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void equipment(World.City c,boolean ships){
        save();List<String> labels=new ArrayList<>();
        if(ships)for(Army.Ship ship:Army.Ship.values())labels.add(ship.label+(ship==Army.Ship.BOAT?" · 免费配备":" · 库存"+c.ships[ship.ordinal()-1])+(ship==ship()?" · 已选":""));
        else for(World.Weapon weapon:World.Weapon.values())labels.add(weapon.label+(weapon==World.Weapon.SWORD?" · 无需库存":" · 库存"+c.equipment[weapon.ordinal()])+(weapon==weapon()?" · 已选":""));
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(ships?"携带舰船":draft.getBoolean("quick")?"选择兵种":"陆战兵装")
            .setSingleChoiceItems(labels.toArray(new String[0]),ships?ship().ordinal():weapon().ordinal(),(d,index)->{
                draft.putInt(ships?"ship":"weapon",index);save();d.dismiss();show(ships||draft.getBoolean("quick")?"quantity":"ship");
            }).setNegativeButton("取消",(d,n)->a.closeForm()).setNeutralButton("上一步",(d,n)->show(ships?"weapon":draft.getBoolean("quick")?"leader":"deputies")).create();
        dialog.setOnCancelListener(d->a.closeForm());dialog.show();a.trackDialog(dialog);
    }
}

package game.sanguo.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Consumer;

/** Native command forms; every confirmed mutation goes through the validated core. */
final class DomesticUi {
    private final MainActivity activity;private final World w;private final Consumer<World.Result> apply;private final Consumer<Hex> focus;
    DomesticUi(MainActivity a,World w,Consumer<World.Result> apply,Consumer<Hex> focus){activity=a;this.w=w;this.apply=apply;this.focus=focus;}
    private void message(String title,String text){new AlertDialog.Builder(activity).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,String positive,Runnable run){activity.commandDialog(title,text,positive,w,run);}
    private void officer(World.City c,Consumer<World.Officer> next){
        List<World.Officer> options=w.idle(c);if(options.isEmpty()){message("武将不足","没有本旬可行动的在城武将。建设中与在途武将不可重复派遣。");return;}
        new AlertDialog.Builder(activity).setTitle("执行武将").setAdapter(GameIcon.adapter(activity,w,options,o->o.name+" · 政治"+o.politics),(d,i)->next.accept(options.get(i))).setNegativeButton("取消",null).show();
    }
    private void destination(int owner,int excluded,Consumer<World.City> next){
        List<World.City> options=new ArrayList<>();for(World.City c:w.cities)if(c.owner==owner&&c.id!=excluded)options.add(c);
        if(options.isEmpty()){message("目的地不足","需要另一座己方城池。");return;}
        new AlertDialog.Builder(activity).setTitle("选择目的地").setAdapter(GameIcon.adapter(activity,w,options,c->c.name+" · 兵"+c.troops),(d,i)->next.accept(options.get(i))).setNegativeButton("取消",null).show();
    }
    void build(World.City c){
        new AlertDialog.Builder(activity).setTitle("设施开发 · 直接最高级").setAdapter(GameIcon.adapter(activity,w,Arrays.asList(Domestic.Kind.values()),k->k.label+" · Lv"+Domestic.buildLevel(k)+" · 金"+k.cost+" · "+Domestic.buildEffect(k)),(d,i)->{
            Domestic.Kind kind=Domestic.Kind.values()[i];officer(c,o->{
                List<Hex> sites=w.domestic.buildSites(c.id);if(kind==Domestic.Kind.SHIPYARD)sites.removeIf(h->h.neighbors().stream().noneMatch(w.army::water));if(sites.isEmpty()){message("无法开发","没有可用开发地或已经达到本城设施上限。");return;}
                activity.pickOnMap("建设"+kind.label+" Lv"+Domestic.buildLevel(kind),c.hex,sites,h->
                    confirm("建设"+kind.label,c.name+" · "+o.name+"\n金"+kind.cost+" / 行动力10 / "+(o.politics>=80?2:3)+"旬\n建设期间武将不可执行其他命令。\n建成即最高等级 Lv"+Domestic.buildLevel(kind)+"："+Domestic.buildEffect(kind)+"\n取消不退费。","开工",()->apply.accept(w.domestic.build(c.id,o.id,kind,h))));
            });
        }).setNegativeButton("取消",null).show();
    }
    void transfer(World.City c){destination(c.owner,c.id,d->officer(c,o->{
        List<Hex> path=w.domestic.route(c.hex,d.hex,c.owner);
        confirm("人员调动",o.name+"："+c.name+" → "+d.name+"\n行动力10；按陆路每旬移动4点。"+(path==null?"\n当前无可用陆路。":"")+"\n在途期间不可执行其他命令。","出发",()->apply.accept(w.domestic.transfer(c.id,d.id,o.id)));
    }));}
    void transport(World.City c){destination(c.owner,c.id,d->officer(c,o->cargo(c,d,o,false)));}
    void transportSea(World.City c){destination(c.owner,c.id,d->officer(c,o->cargo(c,d,o,true)));}
    void restoreDraft(Bundle draft){
        World.City c=w.city(draft.getInt("city")),d=w.city(draft.getInt("target"));World.Officer o=w.officer(draft.getInt("leader"));
        if(c==null||d==null||o==null||!w.idle(c).contains(o)){activity.closeForm();message("草稿需要重新选将","目的地或武将状态已变化，请重新打开运输。");return;}
        cargo(c,d,o,draft.getBoolean("sea"));
    }
    private void cargo(World.City c,World.City d,World.Officer o,boolean sea){
        Bundle previous=new Bundle(activity.formDraft()),draft=new Bundle();draft.putString("kind","cargo");draft.putInt("city",c.id);draft.putInt("target",d.id);draft.putInt("leader",o.id);draft.putBoolean("sea",sea);draft.putBoolean("open",true);
        boolean reusable="cargo".equals(previous.getString("kind"))&&previous.getInt("city")==c.id&&previous.getInt("target")==d.id&&previous.getInt("leader")==o.id&&previous.getBoolean("sea")==sea;
        LinearLayout form=new LinearLayout(activity);form.setOrientation(LinearLayout.VERTICAL);int padding=Math.round(16*activity.getResources().getDisplayMetrics().density);form.setPadding(padding,padding,padding,padding);
        TextView note=new TextView(activity);note.setText(o.name+"："+c.name+" → "+d.name+"\n派遣费0金、行动力10。满仓等待，不丢弃货物。\n途中可被敌军截击，护送兵归零时货物会损失。");form.addView(note);
        List<World.Officer> candidates=new ArrayList<>(w.idle(c));candidates.removeIf(member->member.id==o.id);boolean[] selected=new boolean[candidates.size()];
        int[] oldCrew=reusable?previous.getIntArray("deputies"):null;for(int i=0;i<candidates.size();i++)if(oldCrew!=null)for(int id:oldCrew)if(candidates.get(i).id==id)selected[i]=true;
        Runnable[] update={()->{}};
        Button crew=new Button(activity);crew.setText("运输副将（最多2名）");form.addView(crew);
        crew.setOnClickListener(v->new AlertDialog.Builder(activity).setTitle("运输副将（最多2名）").setMultiChoiceItems(candidates.stream().map(member->member.name).toArray(String[]::new),selected,(dlg,i,yes)->{int n=0;for(boolean value:selected)if(value)n++;if(yes&&n>2){((AlertDialog)dlg).getListView().setItemChecked(i,false);selected[i]=false;return;}selected[i]=yes;}).setPositiveButton("完成",(dlg,i)->update[0].run()).setNegativeButton("清空",(dlg,i)->{Arrays.fill(selected,false);update[0].run();}).show());
        CheckBox returning=new CheckBox(activity);returning.setText("卸货后武将返回出发城");returning.setChecked(reusable&&previous.getBoolean("returning"));form.addView(returning);
        TextView summary=activity.text("",13,activity.gold);form.addView(summary);
        int equipmentEnd=3+World.Weapon.values().length;int count=equipmentEnd+2;String[] labels=new String[count];labels[0]="金（上限100000）";labels[1]="粮（上限200000）";labels[2]="兵（上限20000）";
        int[] stock=new int[count],initial=new int[count];stock[0]=c.gold;stock[1]=c.food;stock[2]=c.troops;initial[1]=5000;initial[2]=1000;initial[3]=1000;
        for(World.Weapon weapon:World.Weapon.values()){int i=3+weapon.ordinal();labels[i]=weapon.label+"装";stock[i]=c.equipment[weapon.ordinal()];}
        labels[equipmentEnd]="楼船货物（上限100）";labels[equipmentEnd+1]="斗舰货物（上限100）";stock[equipmentEnd]=c.ships[0];stock[equipmentEnd+1]=c.ships[1];
        labels[3]="枪兵装";labels[4]="戟兵装";labels[5]="弩兵装";labels[6]="骑兵装";QuantityControl[] inputs=new QuantityControl[count];
        String[] raw=reusable?previous.getStringArray("amounts"):null;
        LinearLayout advanced=new LinearLayout(activity);advanced.setOrientation(LinearLayout.VERTICAL);advanced.setVisibility(previous.getBoolean("advanced",false)?android.view.View.VISIBLE:android.view.View.GONE);
        for(int i=0;i<count;i++){
            int maximum=Math.min(stock[i],i==0?100000:i==1?200000:i==2?20000:i>=equipmentEnd?100:100000);
            inputs[i]=new QuantityControl(activity,i==0?"运输金":i==1?"运输粮":i==2?"运输兵":labels[i],0,maximum,Math.min(initial[i],maximum));inputs[i].inputDescription(labels[i]);
            if(raw!=null&&i<raw.length)inputs[i].restoreValue(raw[i]);
            if(i<3)form.addView(inputs[i]);else advanced.addView(inputs[i]);
        }
        form.addView(activity.button("兵装和舰船货物 · 展开 / 收起",v->{boolean open=advanced.getVisibility()!=android.view.View.VISIBLE;advanced.setVisibility(open?android.view.View.VISIBLE:android.view.View.GONE);draft.putBoolean("advanced",open);update[0].run();}));form.addView(advanced);
        ScrollView scroll=new ScrollView(activity);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("运输数量").setView(scroll).setPositiveButton("发送",null).setNegativeButton("取消",(x,n)->activity.closeForm()).create();
        dialog.setOnCancelListener(x->activity.closeForm());
        update[0]=()->{
            String[] amounts=new String[count];boolean valid=true;for(int i=0;i<count;i++){amounts[i]=inputs[i].draftValue();valid&=inputs[i].valid();}draft.putStringArray("amounts",amounts);draft.putBoolean("returning",returning.isChecked());
            List<Integer> ids=new ArrayList<>();for(int i=0;i<selected.length;i++)if(selected[i])ids.add(candidates.get(i).id);draft.putIntArray("deputies",ids.stream().mapToInt(id->id).toArray());activity.rememberForm(draft);
            int soldiers=inputs[2].value(),food=inputs[1].value();int ration=Math.max(0,(soldiers+19)/20);
            summary.setText(valid?"主将 "+o.name+" · 副将"+ids.size()+"人\n留守：兵"+(c.troops-soldiers)+" / 粮"+(c.food-food)+" / 金"+(c.gold-inputs[0].value())+"\n运输携粮约"+(ration==0?"不限":food/ration)+"旬 · 当前船型走舸\n目标余量：兵"+Math.max(0,w.campaign.troopCap(d)-d.troops)+" / 粮"+Math.max(0,w.campaign.foodCap(d)-d.food):"草稿数量超出当前库存或容量，请修改标红输入。");
            if(dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(valid);
        };
        for(QuantityControl input:inputs)input.onChange(update[0]);returning.setOnCheckedChangeListener((button,checked)->update[0].run());
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int[] values=new int[count];try{for(int i=0;i<count;i++){if(!inputs[i].valid())return;values[i]=inputs[i].value();}}
            catch(NumberFormatException e){Toast.makeText(activity,"请输入有效的非负整数",Toast.LENGTH_SHORT).show();return;}
            List<Integer> ids=new ArrayList<>();for(int i=0;i<selected.length;i++)if(selected[i])ids.add(candidates.get(i).id);int[] deputies=ids.stream().mapToInt(i->i).toArray();int[] equipment=Arrays.copyOfRange(values,3,equipmentEnd);
            String error=w.domestic.transportError(c.id,d.id,o.id,deputies,values[0],values[1],values[2],equipment,sea);
            String preview=w.domestic.transportPreview(c.id,d.id,o.id,deputies,values[0],values[1],values[2],equipment,sea,returning.isChecked());
            int[] ships=Arrays.copyOfRange(values,equipmentEnd,count);String shipError=w.domestic.shipCargoError(c.id,ships);if(error!=null||shipError!=null){message("运输不能执行",shipError==null?preview:shipError);return;}
            String shipPreview="\n舰船货物：楼船"+ships[0]+" / 斗舰"+ships[1]+"；目的地余量：楼船"+Math.max(0,100-d.ships[0])+" / 斗舰"+Math.max(0,100-d.ships[1])+"\n"+(d.ships[0]+ships[0]>100||d.ships[1]+ships[1]>100?"目的地舰船仓不足，抵达后保留货物等待":"舰船货物不会改变当前使用的走舸");
            boolean large=values[0]>=1000||values[1]>=10000||values[2]>=3000;for(int i=3;i<count;i++)large|=values[i]>=3000;
            confirm(large?"确认大额运输":"确认运输",o.name+"："+c.name+" → "+d.name+"\n金 "+values[0]+" / 粮 "+values[1]+" / 兵 "+values[2]+"\n"+preview+shipPreview,"确认发送",()->{
                World.Result result=w.domestic.transport(c.id,d.id,o.id,deputies,values[0],values[1],values[2],equipment,sea,returning.isChecked(),ships);apply.accept(result);if(result.ok){activity.closeForm();dialog.dismiss();}
            });
        }));dialog.show();update[0].run();activity.trackDialog(dialog);dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    void overview(){
        List<String> labels=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        for(World.City c:w.cities)if(c.owner==w.player){labels.add(c.name+" · 设施"+w.domestic.count(c.id)+"/"+w.development.capacity(c.id)+" · 月金"+w.domestic.monthlyGold(c.id)+" / 季粮"+w.domestic.monthlyFood(c.id));actions.add(()->focus.accept(c.hex));}
        for(World.City c:w.cities)if(c.owner==w.player){labels.add(c.name+" · 人事 / 城市治理 / 武将状态");actions.add(()->new StrategyUi(activity,w,apply).city(c));}
        for(Domestic.Facility f:w.domestic.facilities)if(w.city(f.cityId).owner==w.player){labels.add(w.city(f.cityId).name+" · "+f.kind.label+" · "+(f.remaining==0?"已建成":"剩"+f.remaining+"旬"));actions.add(()->facility(f));}
        for(Domestic.Mission m:w.domestic.missions)if(m.owner==w.player){labels.add((m.transport?"运输":"调动")+" · "+w.officer(m.officerId).name+" → "+w.city(m.targetCity).name+" · "+w.domestic.status(m));actions.add(()->mission(m));}
        new AlertDialog.Builder(activity).setTitle("政务与在途 · 点击详情").setItems(labels.toArray(new String[0]),(d,i)->actions.get(i).run()).setNegativeButton("返回",null).show();
    }
    void facility(Domestic.Facility f){
        focus.accept(f.hex);World.City c=w.city(f.cityId);
        AlertDialog.Builder dialog=new AlertDialog.Builder(activity).setTitle(c.name+" · "+f.kind.label+" Lv"+f.level).setMessage("耐久 "+f.hp+"/"+f.maxHp()+"\n"+(f.level==3?Domestic.buildEffect(f.kind):f.kind.effect)+"\n新建设施直接最高级，无需吸收合并。\n"+(f.remaining==0?"已建成":w.officer(f.builderId).name+(f.upgradeTo>0?"合并中":"建设中")+"，剩"+f.remaining+"旬")+"\n坐标 "+f.hex.q+", "+f.hex.r).setNegativeButton("返回",null);
        if(c.owner==w.player&&!w.gameOver()){
            if(f.remaining>0)dialog.setPositiveButton("取消建设",(d,n)->confirm("取消建设","不会退还建设费用，本旬不能重复使用武将。","确定取消",()->apply.accept(w.domestic.cancelBuild(f.id))));
            else dialog.setPositiveButton("拆除",(d,n)->officer(c,o->confirm("拆除设施","需要一名闲置武将与行动力10，不退还费用。","确定拆除",()->apply.accept(w.domestic.demolish(f.id,o.id)))));
        }
        dialog.show();
    }
    void mission(Domestic.Mission m){
        focus.accept(m.hex);StringBuilder detail=new StringBuilder(w.officer(m.officerId).name+"\n"+w.city(m.sourceCity).name+" → "+w.city(m.targetCity).name+"\n"+w.domestic.status(m)+"\n当前坐标 "+m.hex.q+", "+m.hex.r);
        detail.append("\n编队武将：");for(int id:m.crew())detail.append(w.officer(id).name).append(" ");
        if(m.transport){detail.append("\n金 ").append(m.gold).append(" / 粮 ").append(m.food).append(" / 兵 ").append(m.troops);for(int i=0;i<m.equipment.length;i++)detail.append('\n').append(World.Weapon.values()[i].label).append("兵装 ").append(m.equipment[i]);}
        if(m.transport)detail.append("\n舰船货物：楼船 ").append(m.cargoShips[0]).append(" / 斗舰 ").append(m.cargoShips[1]);
        detail.append("\n累计途中耗粮："+m.consumedFood+"；"+(m.returnOfficers?"卸货后人员返程":m.returning?"仅人员返程，无返程物资":"抵达留驻"));
        detail.append("\n\n运输队可被截击；城内受城防保护。目的地失守自动选择可达己城；无路则等待。满仓保留货物，下一旬重试。");
        AlertDialog.Builder d=new AlertDialog.Builder(activity).setTitle(m.transport?"运输详情":"调动详情").setMessage(detail).setNegativeButton("返回",null);
        if(!w.gameOver())d.setPositiveButton("改道 / 返回",(dialog,n)->destination(m.owner,m.targetCity,c->confirm("任务改道","改道至"+c.name+"，消耗行动力10。","执行",()->apply.accept(w.domestic.redirect(m.id,c.id)))));
        d.show();
    }
}

package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Consumer;

/** Native command forms; every confirmed mutation goes through the validated core. */
final class DomesticUi {
    private final Activity activity;private final World w;private final Consumer<World.Result> apply;private final Consumer<Hex> focus;
    DomesticUi(Activity a,World w,Consumer<World.Result> apply,Consumer<Hex> focus){activity=a;this.w=w;this.apply=apply;this.focus=focus;}
    private void message(String title,String text){new AlertDialog.Builder(activity).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,String positive,Runnable run){new AlertDialog.Builder(activity).setTitle(title).setMessage(text).setPositiveButton(positive,(d,n)->run.run()).setNegativeButton("取消",null).show();}
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
        new AlertDialog.Builder(activity).setTitle("设施开发 · 工程规则").setAdapter(GameIcon.adapter(activity,w,Arrays.asList(Domestic.Kind.values()),k->k.label+" · 金"+k.cost+" · "+k.effect),(d,i)->{
            Domestic.Kind kind=Domestic.Kind.values()[i];officer(c,o->{
                List<Hex> sites=w.domestic.buildSites(c.id);if(kind==Domestic.Kind.SHIPYARD)sites.removeIf(h->h.neighbors().stream().noneMatch(w.army::water));if(sites.isEmpty()){message("无法开发","没有可用平地或已经达到每城6处设施上限。");return;}
                String[] names=new String[sites.size()];for(int j=0;j<names.length;j++)names[j]="地块 "+sites.get(j).q+", "+sites.get(j).r;
                new AlertDialog.Builder(activity).setTitle("选择开发地 · 点击预览").setItems(names,(dialog,j)->{
                    Hex h=sites.get(j);focus.accept(h);
                    confirm("建设"+kind.label,c.name+" · "+o.name+"\n金"+kind.cost+" / 行动力10 / "+(o.politics>=80?2:3)+"旬\n建设期间武将不可执行其他命令。\n建成后："+kind.effect+"\n取消不退费。","开工",()->apply.accept(w.domestic.build(c.id,o.id,kind,h)));
                }).setNegativeButton("取消",null).show();
            });
        }).setNegativeButton("取消",null).show();
    }
    void transfer(World.City c){destination(c.owner,c.id,d->officer(c,o->{
        List<Hex> path=w.domestic.route(c.hex,d.hex,c.owner);
        confirm("人员调动",o.name+"："+c.name+" → "+d.name+"\n行动力10；按陆路每旬移动4点。"+(path==null?"\n当前无可用陆路。":"")+"\n在途期间不可执行其他命令。","出发",()->apply.accept(w.domestic.transfer(c.id,d.id,o.id)));
    }));}
    void transport(World.City c){destination(c.owner,c.id,d->officer(c,o->cargo(c,d,o,false)));}
    void transportSea(World.City c){destination(c.owner,c.id,d->officer(c,o->cargo(c,d,o,true)));}
    private void cargo(World.City c,World.City d,World.Officer o,boolean sea){
        LinearLayout form=new LinearLayout(activity);form.setOrientation(LinearLayout.VERTICAL);int padding=Math.round(16*activity.getResources().getDisplayMetrics().density);form.setPadding(padding,padding,padding,padding);
        TextView note=new TextView(activity);note.setText(o.name+"："+c.name+" → "+d.name+"\n另收金100、行动力10。满仓等待，不丢弃货物。\n途中可被敌军截击，护送兵归零时货物会损失。");form.addView(note);
        int count=3+World.Weapon.values().length;String[] labels=new String[count];labels[0]="金（上限100000）";labels[1]="粮（上限200000）";labels[2]="兵（上限20000）";
        int[] stock=new int[count],initial=new int[count];stock[0]=c.gold;stock[1]=c.food;stock[2]=c.troops;initial[1]=5000;initial[2]=1000;initial[3]=1000;
        for(World.Weapon weapon:World.Weapon.values()){int i=3+weapon.ordinal();labels[i]=weapon.label+"装";stock[i]=c.equipment[weapon.ordinal()];}
        labels[3]="枪兵装";labels[4]="戟兵装";labels[5]="弩兵装";labels[6]="骑兵装";EditText[] inputs=new EditText[count];
        for(int i=0;i<count;i++){
            TextView label=new TextView(activity);label.setText(labels[i]+" · 现有"+stock[i]);form.addView(label);
            EditText input=new EditText(activity);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setSingleLine(true);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});input.setText(Integer.toString(initial[i]));input.setContentDescription(labels[i]);form.addView(input);inputs[i]=input;
        }
        ScrollView scroll=new ScrollView(activity);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("运输数量").setView(scroll).setPositiveButton("发送",null).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int[] values=new int[count];try{for(int i=0;i<count;i++){String value=inputs[i].getText().toString().trim();values[i]=value.isEmpty()?0:Integer.parseInt(value);}}
            catch(NumberFormatException e){Toast.makeText(activity,"请输入有效的非负整数",Toast.LENGTH_SHORT).show();return;}
            Runnable send=()->{World.Result result=sea?w.domestic.transportSea(c.id,d.id,o.id,values[0],values[1],values[2],Arrays.copyOfRange(values,3,count)):w.domestic.transport(c.id,d.id,o.id,values[0],values[1],values[2],Arrays.copyOfRange(values,3,count));apply.accept(result);if(result.ok)dialog.dismiss();};
            // UX threshold only. Validity/costs are still checked by the engine on execution.
            boolean large=values[0]>=1000||values[1]>=10000||values[2]>=3000;
            for(int i=3;i<count;i++)large|=values[i]>=3000;
            if(large){StringBuilder summary=new StringBuilder(o.name+"："+c.name+" → "+d.name+"\n金 "+values[0]+" / 粮 "+values[1]+" / 兵 "+values[2]);
                for(int i=3;i<count;i++)if(values[i]>0)summary.append("\n").append(World.Weapon.values()[i-3].label).append("兵装 ").append(values[i]);
                summary.append("\n另收金100、行动力10；出发后货物从本城扣除。");confirm("确认大额运输",summary.toString(),"确认发送",send);
            }else send.run();
        }));dialog.show();dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    void overview(){
        List<String> labels=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        for(World.City c:w.cities)if(c.owner==w.player){labels.add(c.name+" · 设施"+w.domestic.count(c.id)+"/6 · 月金"+w.domestic.monthlyGold(c.id)+" / 季粮"+w.domestic.monthlyFood(c.id));actions.add(()->focus.accept(c.hex));}
        for(World.City c:w.cities)if(c.owner==w.player){labels.add(c.name+" · 人事 / 城市治理 / 武将状态");actions.add(()->new StrategyUi(activity,w,apply).city(c));}
        for(Domestic.Facility f:w.domestic.facilities)if(w.city(f.cityId).owner==w.player){labels.add(w.city(f.cityId).name+" · "+f.kind.label+" · "+(f.remaining==0?"已建成":"剩"+f.remaining+"旬"));actions.add(()->facility(f));}
        for(Domestic.Mission m:w.domestic.missions)if(m.owner==w.player){labels.add((m.transport?"运输":"调动")+" · "+w.officer(m.officerId).name+" → "+w.city(m.targetCity).name+" · "+w.domestic.status(m));actions.add(()->mission(m));}
        new AlertDialog.Builder(activity).setTitle("政务与在途 · 点击详情").setItems(labels.toArray(new String[0]),(d,i)->actions.get(i).run()).setNegativeButton("返回",null).show();
    }
    void facility(Domestic.Facility f){
        focus.accept(f.hex);World.City c=w.city(f.cityId);
        AlertDialog.Builder dialog=new AlertDialog.Builder(activity).setTitle(c.name+" · "+f.kind.label+" Lv"+f.level).setMessage("耐久 "+f.hp+"/"+f.maxHp()+"\n"+f.kind.effect+"\n等级倍率：Lv1 100% / Lv2 120% / Lv3 150%\n"+(f.remaining==0?"已建成":w.officer(f.builderId).name+(f.upgradeTo>0?"合并中":"建设中")+"，剩"+f.remaining+"旬")+"\n坐标 "+f.hex.q+", "+f.hex.r).setNegativeButton("返回",null);
        if(c.owner==w.player&&!w.gameOver()){
            if(f.remaining>0)dialog.setPositiveButton("取消建设",(d,n)->confirm("取消建设","不会退还建设费用，本旬不能重复使用武将。","确定取消",()->apply.accept(w.domestic.cancelBuild(f.id))));
            else dialog.setPositiveButton("拆除",(d,n)->officer(c,o->confirm("拆除设施","需要一名闲置武将与行动力10，不退还费用。","确定拆除",()->apply.accept(w.domestic.demolish(f.id,o.id)))));
            if(f.remaining==0&&f.level<3&&Domestic.mergeable(f.kind))dialog.setNeutralButton("吸收合并",(d,n)->new CampaignUi(activity,w,apply).merge(f));
        }
        dialog.show();
    }
    void mission(Domestic.Mission m){
        focus.accept(m.hex);StringBuilder detail=new StringBuilder(w.officer(m.officerId).name+"\n"+w.city(m.sourceCity).name+" → "+w.city(m.targetCity).name+"\n"+w.domestic.status(m)+"\n当前坐标 "+m.hex.q+", "+m.hex.r);
        if(m.transport){detail.append("\n金 ").append(m.gold).append(" / 粮 ").append(m.food).append(" / 兵 ").append(m.troops);for(int i=0;i<m.equipment.length;i++)detail.append('\n').append(World.Weapon.values()[i].label).append("兵装 ").append(m.equipment[i]);}
        detail.append("\n\n运输队可被截击；城内受城防保护。目的地失守自动选择可达己城；无路则等待。满仓保留货物，下一旬重试。");
        AlertDialog.Builder d=new AlertDialog.Builder(activity).setTitle(m.transport?"运输详情":"调动详情").setMessage(detail).setNegativeButton("返回",null);
        if(!w.gameOver())d.setPositiveButton("改道 / 返回",(dialog,n)->destination(m.owner,m.targetCity,c->confirm("任务改道","改道至"+c.name+"，消耗行动力10。","执行",()->apply.accept(w.domestic.redirect(m.id,c.id)))));
        d.show();
    }
}

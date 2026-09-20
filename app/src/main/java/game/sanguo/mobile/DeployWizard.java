package game.sanguo.mobile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;

/** Three tabs, one draft, one final command. Picking crew never dismisses this dialog. */
final class DeployWizard {
    private final MainActivity a;private final World w;private final Bundle draft;
    private World.City city;private AlertDialog dialog;
    private final View[] pages=new View[3];private final Button[] tabs=new Button[3];
    private QuantityControl troops,food,gold;private TextView ration,summary,crewDetails,stats,attack,defense;
    private LinearLayout crew;private EditText search;private Roster roster;
    private List<World.Officer> available=new ArrayList<>(),filtered=new ArrayList<>();
    private int sort,previousTroops;private boolean updating;
    DeployWizard(MainActivity a,World w,ArmyUi army,Bundle draft){this.a=a;this.w=w;this.draft=new Bundle(draft);}
    static Bundle start(MainActivity a,World.City c,boolean quick){Bundle d=new Bundle();d.putString("kind","deploy");d.putInt("city",c.id);d.putBoolean("wizard",true);d.putBoolean("quick",quick);d.putBoolean("open",true);d.putString("step","overview");d.putInt("tab",0);d.putIntArray("deputies",new int[0]);return d;}
    private void save(){draft.putBoolean("open",true);a.rememberForm(draft);}
    private World.Weapon weapon(){return World.Weapon.values()[Math.max(0,Math.min(World.Weapon.values().length-1,draft.getInt("weapon",0)))];}
    private Army.Ship ship(){return Army.Ship.values()[Math.max(0,Math.min(Army.Ship.values().length-1,draft.getInt("ship",0)))];}
    private int[] deputies(){int[] ids=draft.getIntArray("deputies");return ids==null?new int[0]:ids;}
    private int leader(){return draft.getInt("leader",-1);}
    private int number(String key,int fallback){try{return Integer.parseInt(draft.getString(key,""+fallback));}catch(NumberFormatException e){return fallback;}}
    private int cap(){return leader()<0||!draft.containsKey("weapon")?Math.min(10000,city.troops):UiModels.deployTroopCap(w,city,leader(),weapon(),ship());}
    private LinearLayout column(){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);return v;}
    void show(){
        city=w.city(draft.getInt("city",-1));if(city==null||city.owner!=w.player){a.closeForm();return;}available=new ArrayList<>(w.idle(city));save();
        LinearLayout root=column();root.setPadding(a.dp(10),a.dp(4),a.dp(10),a.dp(6));root.setBackgroundColor(a.ink);
        LinearLayout tabbar=new LinearLayout(a);String[] names={"武将编队","兵种钱粮","部队详情"};
        for(int i=0;i<3;i++){final int tab=i;tabs[i]=a.button(names[i],v->tab(tab));tabs[i].setTag("deploy.tab."+i);tabbar.addView(tabs[i],new LinearLayout.LayoutParams(0,a.dp(48),1));}root.addView(tabbar);
        FrameLayout body=new FrameLayout(a);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        int cap=Math.max(1000,cap());troops=new QuantityControl(a,"兵力",1000,cap,number("troops",Math.min(3000,cap)));food=new QuantityControl(a,"粮食",0,Math.min(city.food,1000000),number("food",Logistics.defaultFood(troops.value(),city.food)));gold=new QuantityControl(a,"金钱",0,Math.min(city.gold,10000),number("gold",0));
        for(String key:new String[]{"troops","food","gold"})if(draft.containsKey(key))(key.equals("troops")?troops:key.equals("food")?food:gold).restoreValue(draft.getString(key));
        troops.setTag("deploy.troops");food.setTag("deploy.food");gold.setTag("deploy.gold");
        pages[0]=crewPage();pages[1]=suppliesPage();pages[2]=detailsPage();for(View page:pages)body.addView(page,new FrameLayout.LayoutParams(-1,-1));
        summary=a.text("",12,a.gold);summary.setTag("deploy.summary");summary.setPadding(a.dp(4),a.dp(6),a.dp(4),a.dp(3));summary.setOnClickListener(v->tab(leader()<0?0:1));root.addView(summary);
        dialog=new AlertDialog.Builder(a).setTitle(city.name+" · 出征编队").setView(root).setNegativeButton("取消",(d,i)->a.closeForm()).setPositiveButton("确认出征",null).create();
        previousTroops=troops.value();troops.onChange(()->{if(updating)return;if(troops.valid()&&troops.value()!=previousTroops){previousTroops=troops.value();food.set(Logistics.defaultFood(troops.value(),city.food));}update();});food.onChange(this::update);gold.onChange(this::update);dialog.setOnCancelListener(d->a.closeForm());
        final boolean[] submitted={false};final long[] revision={w.commandRevision()};dialog.setOnShowListener(v->{
            ViewGroup.LayoutParams p=root.getLayoutParams();p.height=a.dp(Math.max(240,Math.min(700,a.getResources().getConfiguration().screenHeightDp-150)));root.setLayoutParams(p);dialog.getWindow().setLayout(a.dp(Math.min(620,a.getResources().getConfiguration().screenWidthDp-16)),-2);
            tab(Math.max(0,Math.min(2,draft.getInt("tab",0))));update();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag("deploy.confirm");dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{
                if(submitted[0]||!a.currentWorld(w)||error()!=null)return;
                if(revision[0]!=w.commandRevision()){revision[0]=w.commandRevision();available=new ArrayList<>(w.idle(city));filter();update();Toast.makeText(a,"局面已更新，请核对后出征",Toast.LENGTH_SHORT).show();return;}
                submitted[0]=true;int commander=leader();World.Result result=w.army.deploy(city.id,commander,deputies(),weapon(),ship(),troops.value(),food.value(),gold.value());a.applyResult(result);
                if(result.ok){a.closeForm();dialog.dismiss();World.Officer officer=w.officer(commander);World.Unit unit=officer==null?null:w.unit(officer.unitId);if(unit!=null)a.selectAndFocus(unit.hex);}else{submitted[0]=false;update();}
            });
        });dialog.show();a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private View crewPage(){
        LinearLayout page=column();crew=new LinearLayout(a);crew.setTag("deploy.crew");page.addView(crew);
        crewDetails=a.text("",12,a.paper);crewDetails.setPadding(a.dp(4),a.dp(3),a.dp(4),a.dp(3));ScrollView selected=new ScrollView(a);selected.addView(crewDetails);page.addView(selected,new LinearLayout.LayoutParams(-1,a.dp(76)));
        TextView tip=a.text("连续点击选主将、副将 · 再点取消 · 副将可一键设为主将",11,a.muted);page.addView(tip);
        LinearLayout tools=new LinearLayout(a);search=new EditText(a);search.setTextSize(13);search.setSingleLine(true);search.setTextColor(a.paper);search.setHintTextColor(a.muted);search.setHint("搜索姓名 / 特技");search.setTag("deploy.officer.search");tools.addView(search,new LinearLayout.LayoutParams(0,a.dp(42),1));
        Button order=a.button("统率 ↓",v->{sort=(sort+1)%5;((Button)v).setText(new String[]{"统率 ↓","武力 ↓","智力 ↓","政治 ↓","魅力 ↓"}[sort]);filter();});order.setTag("deploy.officer.sort");tools.addView(order,new LinearLayout.LayoutParams(a.dp(82),a.dp(42)));page.addView(tools);
        ListView list=new ListView(a);list.setTag("deploy.officers");list.setDividerHeight(a.dp(1));roster=new Roster();list.setAdapter(roster);list.setOnItemClickListener((parent,view,p,id)->toggle(filtered.get(p)));page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int start,int before,int count){filter();}});filter();return page;
    }
    private View suppliesPage(){
        LinearLayout form=column();form.setPadding(a.dp(3),a.dp(5),a.dp(3),a.dp(10));form.addView(a.text("兵种 · 同屏点选，库存不足的兵种不可选",13,a.gold));
        InlineChoices<World.Weapon> weapons=new InlineChoices<>(a,w,Arrays.asList(World.Weapon.values()),5,k->k.label+"\n"+(k==World.Weapon.SWORD?"无需库存":"库存 "+city.equipment[k.ordinal()]),k->k==World.Weapon.SWORD||city.equipment[k.ordinal()]>=Army.equipmentNeeded(k,1000),draft.containsKey("weapon")?weapon():null,k->{draft.putInt("weapon",k.ordinal());changed();});weapons.setTag("deploy.weapons");form.addView(weapons);
        form.addView(a.text("舰船 · 默认走舸",12,a.muted));InlineChoices<Army.Ship> ships=new InlineChoices<>(a,w,Arrays.asList(Army.Ship.values()),3,s->s.label+"\n"+(s==Army.Ship.BOAT?"免费携带":"库存 "+city.ships[s.ordinal()-1]),s->s==Army.Ship.BOAT||city.ships[s.ordinal()-1]>0,ship(),s->{draft.putInt("ship",s.ordinal());changed();});ships.setTag("deploy.ships");form.addView(ships);
        ration=a.text("",12,a.gold);ration.setTag("deploy.rations");food.addView(ration,1);form.addView(troops);form.addView(food);form.addView(gold);form.addView(a.text("调整兵力自动配兵力×2的粮食；可再手动修改。\n新出征清空旧选择，页签切换不会丢失当前配置。",12,a.muted));ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(form);return scroll;
    }
    private View detailsPage(){LinearLayout form=column();form.setPadding(a.dp(5),a.dp(8),a.dp(5),a.dp(12));LinearLayout ratings=new LinearLayout(a);attack=a.text("攻击 —",24,a.gold);defense=a.text("防御 —",24,a.gold);attack.setTag("deploy.attack");defense.setTag("deploy.defense");ratings.addView(attack,new LinearLayout.LayoutParams(0,a.dp(58),1));ratings.addView(defense,new LinearLayout.LayoutParams(0,a.dp(58),1));form.addView(ratings);stats=a.text("",14,a.paper);stats.setTag("deploy.details");stats.setTextIsSelectable(true);form.addView(stats);ScrollView scroll=new ScrollView(a);scroll.addView(form);return scroll;}
    private void tab(int index){draft.putInt("tab",index);for(int i=0;i<3;i++){pages[i].setVisibility(i==index?View.VISIBLE:View.GONE);tabs[i].setTextColor(i==index?a.gold:a.muted);tabs[i].setSelected(i==index);tabs[i].setBackground(UiTheme.surface(a,i==index?0xff38584f:0xff23323b,i==index?0xff183b32:0xff15212a,8));}save();}
    private int rank(World.Officer o){return sort==0?o.leadership:sort==1?o.war:sort==2?o.intelligence:sort==3?o.politics:o.charm;}
    private void filter(){String q=search==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);filtered=new ArrayList<>();for(World.Officer o:available)if(q.isEmpty()||(o.name+Skill.label(o.skillId)).toLowerCase(Locale.ROOT).contains(q))filtered.add(o);filtered.sort(Comparator.comparingInt((World.Officer o)->-rank(o)).thenComparingInt(o->o.id));if(roster!=null)roster.notifyDataSetChanged();}
    private String role(int id){if(id==leader())return "主将";int[] ids=deputies();for(int i=0;i<ids.length;i++)if(ids[i]==id)return "副将"+(i+1);return "";}
    private List<Integer> deputyList(){List<Integer> result=new ArrayList<>();for(int id:deputies())result.add(id);return result;}
    private void putDeputies(List<Integer> ids){draft.putIntArray("deputies",ids.stream().mapToInt(i->i).toArray());}
    private void toggle(World.Officer o){
        List<Integer> ids=deputyList();if(leader()==o.id){draft.putInt("leader",ids.isEmpty()?-1:ids.remove(0));}
        else if(ids.remove(Integer.valueOf(o.id))){}else if(leader()<0)draft.putInt("leader",o.id);else if(ids.size()<2)ids.add(o.id);else{Toast.makeText(a,"最多三名武将；点击已选行可取消",Toast.LENGTH_SHORT).show();return;}
        putDeputies(ids);changed();
    }
    private void promote(World.Officer o){if(leader()==o.id)return;List<Integer> ids=deputyList();int pos=ids.indexOf(o.id);if(pos<0){toggle(o);return;}ids.set(pos,leader());draft.putInt("leader",o.id);putDeputies(ids);changed();}
    private void changed(){if(troops==null)return;int limit=Math.max(1000,cap());troops.bounds(1000,limit);if(troops.value()>limit)troops.set(limit);update();if(roster!=null)roster.notifyDataSetChanged();}
    private String values(World.Officer o){return "统"+o.leadership+" 武"+o.war+" 智"+o.intelligence+" 政"+o.politics+" 魅"+o.charm+" 忠"+o.loyalty;}
    private String aptitudes(World.Officer o){StringBuilder s=new StringBuilder();String[] labels={"枪","戟","弩","骑","器","水"};for(int i=0;i<6;i++)s.append(labels[i]).append(War.rankLabel(o.aptitude[i])).append(' ');return s.toString();}
    private void update(){
        if(updating||summary==null)return;updating=true;
        try{
            draft.putString("troops",troops.draftValue());draft.putString("food",food.draftValue());draft.putString("gold",gold.draftValue());save();
            crew.removeAllViews();StringBuilder selected=new StringBuilder();List<Integer> ids=new ArrayList<>();ids.add(leader());ids.addAll(deputyList());
            for(int slot=0;slot<3;slot++){int id=slot<ids.size()?ids.get(slot):-1;World.Officer o=w.officer(id);String label=slot==0?"主将":"副将"+slot;Button tile=a.button(label+"\n"+(o==null?"待选":o.name),v->{if(o!=null&&o.id!=leader())promote(o);});tile.setTag("deploy.crew."+slot);tile.setMaxLines(2);tile.setTextSize(12);tile.setGravity(Gravity.CENTER);tile.setPadding(a.dp(3),a.dp(4),a.dp(3),a.dp(4));
                if(o!=null){android.graphics.drawable.Drawable icon=GameIcon.drawable(a,w,o);icon.setBounds(0,0,a.dp(27),a.dp(27));tile.setCompoundDrawables(null,icon,null,null);tile.setContentDescription(label+o.name+(slot==0?"":"，点击设为主将"));selected.append(label).append(' ').append(o.name).append(" · ").append(values(o)).append("\n").append(aptitudes(o)).append(" · ").append(Skill.label(o.skillId)).append("\n");}
                crew.addView(tile,new LinearLayout.LayoutParams(0,a.dp(78),1));
            }
            crewDetails.setText(selected.length()==0?"在下方同一个列表连续点击即可选好三将。\n第一位为主将，之后两位为副将；副将可留空。":selected.toString().trim());
            int use=Logistics.baseUse(troops.value(),false);ration.setText("基础旬耗 "+use+" · 可支撑 "+Logistics.turns(food.value(),use)+" 旬"+(2L*troops.value()>Math.min(city.food,1000000)?"\n库存或携带上限不足两倍粮，已按上限配粮":""));
            String error=error();summary.setText(error==null?"留守：兵 "+(city.troops-troops.value())+" · 粮 "+(city.food-food.value())+" · 金 "+(city.gold-gold.value())+"\n"+weapon().label+" · "+(1+deputies().length)+"将 · 行动力10 · 可直接确认出征":error);
            if(dialog!=null&&dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error==null);
            World.Unit preview=leader()<0||!draft.containsKey("weapon")?null:w.army.deploymentPreview(city,leader(),deputies(),weapon(),ship(),troops.value(),food.value(),gold.value());
            if(preview==null){attack.setText("攻击 —");defense.setText("防御 —");stats.setText("选好主将与兵种后，显示实际出征格上的部队数值。\n"+(error==null?"城外没有可用出征格":error));return;}
            attack.setText(String.format(Locale.ROOT,"攻击 %.1f",w.combat.attackRating(preview)));defense.setText(String.format(Locale.ROOT,"防御 %.1f",w.combat.defenseRating(preview)));
            int actualUse=Logistics.foodUse(w,preview);StringBuilder details=new StringBuilder();details.append(w.army.equipmentLabel(preview)).append(" · 适性 ").append(War.rankLabel(w.army.aptitude(preview))).append("\n兵力 ").append(troops.value()).append(" / 统兵上限 ").append(w.government.commandLimit(leader())).append(" · 气力 ").append(preview.energy).append("\n移动力 ").append(w.war.movement(preview)).append(" · 普攻射程 ").append(w.war.range(preview)).append("\n出征格 (").append(preview.hex.q).append(',').append(preview.hex.r).append(") · ").append(w.terrain[preview.hex.q][preview.hex.r]).append("\n\n后勤\n携金 ").append(gold.value()).append(" · 携粮 ").append(food.value()).append("\n当前地块旬耗 ").append(actualUse).append(" · 可支撑 ").append(Logistics.turns(food.value(),actualUse)).append(" 旬\n兵装消耗 ").append(Army.equipmentNeeded(weapon(),troops.value())).append(" · 行动力10\n\n编队特技\n");
            for(World.Officer o:w.army.crew(preview))if(o!=null)details.append(o.name).append(" · ").append(Skill.label(o.skillId)).append("：").append(Skill.description(o.skillId)).append("\n");
            details.append("\n战法（仍须满足目标、地形与气力条件）\n");boolean has=false;if(!w.army.water(preview.hex))for(War.Tactic t:War.Tactic.values())if(t.weapon==preview.weapon&&w.army.aptitude(preview)>=t.rank){has=true;details.append(t.label).append(" · 气力").append(t.energy).append(" · ").append(t.effect).append("\n");}for(Army.Tactic t:w.army.tactics(preview)){has=true;details.append(t.label).append(" · 气力").append(t.energy).append(" · ").append(t.effect).append("\n");}if(!has)details.append("当前兵种 / 适性没有可用战法\n");
            details.append("\n攻防与副将补正\n").append(w.combat.statExplanation(preview));stats.setText(details);
        }finally{updating=false;}
    }
    private String error(){
        if(!a.currentWorld(w)||w.active!=w.player||w.commandsBlocked()||w.gameOver())return "当前局面不能出征";
        World.Officer o=w.officer(leader());if(o==null||!w.idle(city).contains(o))return "① 武将编队：请选择一名当前可用的主将";
        Set<Integer> used=new HashSet<>();used.add(o.id);for(int id:deputies())if(!used.add(id)||!w.idle(city).contains(w.officer(id)))return "① 武将编队：副将已不可用，请重新选择";
        if(!draft.containsKey("weapon"))return "② 兵种钱粮：请点选一个兵种";
        if(!troops.valid()||!food.valid()||!gold.valid()||troops.value()>cap()||food.value()<troops.value())return "② 兵种钱粮：至少1000兵，携粮不少于兵力，且不能超过库存";
        if(w.actionPoints[w.player]<10)return "行动力不足10";
        String reserve=w.districts.reserveError(city,gold.value(),food.value(),troops.value());if(reserve!=null)return reserve;
        if(w.army.deploymentExit(city,weapon())==null)return "城外没有可用出征格";return null;
    }
    private final class Roster extends BaseAdapter {
        public int getCount(){return filtered.size();}public World.Officer getItem(int p){return filtered.get(p);}public long getItemId(int p){return getItem(p).id;}public boolean hasStableIds(){return true;}
        public View getView(int p,View convert,ViewGroup parent){
            World.Officer o=getItem(p);LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(a.dp(3),a.dp(4),a.dp(3),a.dp(4));row.setTag("deploy.officer."+o.id);
            ImageView portrait=new ImageView(a);portrait.setImageDrawable(GameIcon.drawable(a,w,o));row.addView(portrait,new LinearLayout.LayoutParams(a.dp(34),a.dp(42)));
            LinearLayout texts=column();TextView name=a.text((role(o.id).isEmpty()?"":"〔"+role(o.id)+"〕")+o.name+" · "+Skill.label(o.skillId),14,role(o.id).isEmpty()?a.paper:a.gold);texts.addView(name);texts.addView(a.text(values(o),11,a.muted));texts.addView(a.text(aptitudes(o),11,a.muted));row.addView(texts,new LinearLayout.LayoutParams(0,-2,1));
            String role=role(o.id);Button select=a.button(role.isEmpty()?"选用":role.equals("主将")?"取消":"设主将",v->{if(!role.isEmpty()&&!role.equals("主将"))promote(o);else toggle(o);});select.setTextSize(11);select.setTag("deploy.role."+o.id);row.addView(select,new LinearLayout.LayoutParams(a.dp(64),a.dp(44)));row.setOnClickListener(v->toggle(o));return row;
        }
    }
}

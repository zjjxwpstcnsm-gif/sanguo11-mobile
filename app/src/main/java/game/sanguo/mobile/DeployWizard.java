package game.sanguo.mobile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.*;
import java.util.*;

/** Three tabs, one draft, one final command. Picking crew never dismisses this dialog. */
final class DeployWizard {
    private final MainActivity a;private final World w;private final Bundle draft;
    private World.City city;private AlertDialog dialog;
    private final View[] pages=new View[3];private final Button[] tabs=new Button[3];
    private QuantityControl troops,food,gold;private TextView ration,summary,crewDetails,stats,attack,defense;
    private LinearLayout root,tabbar,supplies;private LinearLayout crew;private ScrollView crewDescription;private TextView crewTip;private EditText search;private TextView rosterStatus;private Roster roster;
    private List<World.Officer> available=new ArrayList<>(),filtered=new ArrayList<>();
    private int sort,previousTroops;private boolean updating;
    private StateToken expected;private DeploymentCommand command;private DeploymentPreview preview;
    private final android.os.Handler previewHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable previewWork=this::refreshPreview;
    DeployWizard(MainActivity a,World w,ArmyUi army,Bundle draft){this.a=a;this.w=w;this.draft=new Bundle(draft);}
    static Bundle start(MainActivity a,World.City c,boolean quick){Bundle d=new Bundle();d.putString("kind","deploy");d.putInt("city",c.id);d.putBoolean("wizard",true);d.putBoolean("quick",quick);d.putBoolean("open",true);d.putString("step","overview");d.putInt("tab",0);d.putIntArray("deputies",new int[0]);return d;}
    private void save(){draft.putBoolean("open",true);a.rememberForm(draft);}
    private World.Weapon weapon(){return World.Weapon.values()[Math.max(0,Math.min(World.Weapon.values().length-1,draft.getInt("weapon",0)))];}
    private Army.Ship ship(){return Army.Ship.values()[Math.max(0,Math.min(Army.Ship.values().length-1,draft.getInt("ship",0)))];}
    private int[] deputies(){int[] ids=draft.getIntArray("deputies");return ids==null?new int[0]:ids;}
    private int leader(){return draft.getInt("leader",-1);}
    private int number(String key,int fallback){try{return Integer.parseInt(draft.getString(key,""+fallback));}catch(NumberFormatException e){return fallback;}}
    private LinearLayout column(){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);return v;}
    void show(){
        city=w.city(draft.getInt("city",-1));if(city==null||city.owner!=w.player){a.closeForm();return;}available=new ArrayList<>(w.idle(city));expected=a.deploymentState();save();
        root=column();root.setPadding(a.dp(10),a.dp(4),a.dp(10),a.dp(6));root.setBackgroundColor(a.ink);
        tabbar=new LinearLayout(a);String[] names={"武将编队","兵种钱粮","部队详情"};
        for(int i=0;i<3;i++){final int tab=i;tabs[i]=a.button(names[i],v->tab(tab));tabs[i].setTag("deploy.tab."+i);tabbar.addView(tabs[i],new LinearLayout.LayoutParams(0,a.dp(48),1));}root.addView(tabbar);
        FrameLayout body=new FrameLayout(a);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        // Initial values are draft defaults, not validation limits. The query supplies all rule ranges.
        troops=new QuantityControl(a,"兵力",0,9999999,number("troops",3000));food=new QuantityControl(a,"粮食",0,9999999,number("food",Logistics.defaultFood(troops.value(),city.food)));gold=new QuantityControl(a,"金钱",0,9999999,number("gold",0));
        for(String key:new String[]{"troops","food","gold"})if(draft.containsKey(key))(key.equals("troops")?troops:key.equals("food")?food:gold).restoreValue(draft.getString(key));
        troops.setTag("deploy.troops");food.setTag("deploy.food");gold.setTag("deploy.gold");
        pages[0]=crewPage();pages[1]=suppliesPage();pages[2]=detailsPage();for(View page:pages)body.addView(page,new FrameLayout.LayoutParams(-1,-1));
        summary=a.text("",12,a.gold);summary.setTag("deploy.summary");summary.setMaxLines(2);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);summary.setPadding(a.dp(4),a.dp(6),a.dp(4),a.dp(3));summary.setOnClickListener(v->tab(leader()<0?0:1));root.addView(summary);
        dialog=new AlertDialog.Builder(a).setTitle(city.name+" · 出征编队").setView(root).setNegativeButton("取消",(d,i)->a.closeForm()).setPositiveButton("确认出征",null).create();
        previousTroops=troops.value();troops.onChange(()->{if(updating)return;if(troops.valid()&&troops.value()!=previousTroops){previousTroops=troops.value();food.set(Logistics.defaultFood(troops.value(),city.food));}update();});food.onChange(this::update);gold.onChange(this::update);dialog.setOnCancelListener(d->a.closeForm());
        final boolean[] submitted={false};
        android.graphics.Rect viewport=new android.graphics.Rect();final int[] lastHeight={-1};
        ViewTreeObserver.OnPreDrawListener fit=()->{root.getWindowVisibleDisplayFrame(viewport);if(lastHeight[0]!=viewport.height()){lastHeight[0]=viewport.height();fitForm(root);}return true;};
        dialog.setOnDismissListener(d->{previewHandler.removeCallbacks(previewWork);if(root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnPreDrawListener(fit);});
        dialog.setOnShowListener(v->{
            // Floating-window bounds can change after layout (notably when dismissing the IME).
            // Check the frame before drawing, and refit only when its height actually changes.
            root.getViewTreeObserver().addOnPreDrawListener(fit);fitForm(root);dialog.getWindow().setLayout(a.dp(Math.min(620,a.getResources().getConfiguration().screenWidthDp-16)),-2);
            tab(Math.max(0,Math.min(2,draft.getInt("tab",0))));update();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag("deploy.confirm");dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{
                if(submitted[0]||preview==null||!preview.allowed()||error()!=null)return;
                submitted[0]=true;dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                CommandResult result=a.executeDeployment(command);
                if(result.ok()){a.closeForm();dialog.dismiss();}else{submitted[0]=false;update();}

            });
        });dialog.show();a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void fitForm(LinearLayout root){
        android.graphics.Rect visible=new android.graphics.Rect();root.getWindowVisibleDisplayFrame(visible);
        if(visible.height()<=0)return;
        boolean compact=visible.height()<a.dp(480);
        // Reserve space for the focused search and actual results instead of a tall crew header.
        for(View v:new View[]{crew,crewDescription,crewTip})if(v!=null)v.setVisibility(compact?View.GONE:View.VISIBLE);
        // With a landscape keyboard, move navigation and feedback into the supply scroll.
        // They remain reachable, while the focused 48dp input and dialog actions keep their space.
        boolean scrollChrome=visible.height()<a.dp(320)&&draft.getInt("tab",0)==1;
        if(scrollChrome&&tabbar.getParent()==root){root.removeView(tabbar);root.removeView(summary);supplies.addView(tabbar,0);supplies.addView(summary);}
        else if(!scrollChrome&&tabbar.getParent()==supplies){supplies.removeView(tabbar);supplies.removeView(summary);root.addView(tabbar,0);root.addView(summary);}
        int height=Math.max(a.dp(64),Math.min(a.dp(700),visible.height()-a.dp(scrollChrome?120:150)));
        ViewGroup.LayoutParams p=root.getLayoutParams();if(p.height!=height){p.height=height;root.setLayoutParams(p);}
    }
    private View crewPage(){
        // One scrolling surface owns the crew header and roster. Fixed header siblings
        // used to consume the entire landscape viewport and give the roster zero height.
        LinearLayout page=column();crew=new LinearLayout(a);crew.setTag("deploy.crew");page.addView(crew);
        crewDetails=a.text("",12,a.paper);crewDetails.setPadding(a.dp(4),a.dp(3),a.dp(4),a.dp(3));crewDescription=new ScrollView(a);crewDescription.addView(crewDetails);page.addView(crewDescription,new LinearLayout.LayoutParams(-1,a.dp(76)));
        crewTip=a.text("连续点击选主将、副将 · 再点移除 · 副将可一键设为主将",11,a.muted);page.addView(crewTip);
        LinearLayout tools=new LinearLayout(a);search=new EditText(a);UiTheme.search(search);search.setSingleLine(true);search.setHint("搜索姓名 / 特技");search.setTag("deploy.officer.search");tools.addView(search,new LinearLayout.LayoutParams(0,a.dp(48),1));
        search.setOnEditorActionListener((v,action,event)->{if(action!=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)return false;((android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();return true;});
        Button clear=a.button("×",v->search.setText(""));clear.setContentDescription("清空编队武将搜索");tools.addView(clear,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
        Button order=a.button("统率 ↓",v->{sort=(sort+1)%5;((Button)v).setText(new String[]{"统率 ↓","武力 ↓","智力 ↓","政治 ↓","魅力 ↓"}[sort]);filter();});order.setTag("deploy.officer.sort");tools.addView(order,new LinearLayout.LayoutParams(a.dp(82),a.dp(48)));page.addView(tools);
        rosterStatus=a.text("",13,a.muted);rosterStatus.setPadding(a.dp(4),a.dp(6),a.dp(4),a.dp(6));rosterStatus.setTag("deploy.officer.status");rosterStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);page.addView(rosterStatus);
        ListView list=new ListView(a);list.setTag("deploy.officers");list.setDividerHeight(a.dp(1));list.addHeaderView(page,null,false);roster=new Roster();list.setAdapter(roster);
        list.setOnItemClickListener((parent,view,p,id)->{int row=p-list.getHeaderViewsCount();if(row>=0&&row<filtered.size())toggle(filtered.get(row));});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int start,int before,int count){filter();}});filter();return list;
    }
    private View suppliesPage(){
        LinearLayout form=supplies=column();form.setPadding(a.dp(3),a.dp(5),a.dp(3),a.dp(10));form.addView(a.text("兵种 · 点选后显示实际库存与可用原因",13,a.gold));
        InlineChoices<World.Weapon> weapons=new InlineChoices<>(a,w,Arrays.asList(World.Weapon.values()),5,k->k.label+"\n"+(k==World.Weapon.SWORD?"无需库存":"库存 "+city.equipment[k.ordinal()]),k->true,draft.containsKey("weapon")?weapon():null,k->{draft.putInt("weapon",k.ordinal());changed();});weapons.setTag("deploy.weapons");form.addView(weapons);
        form.addView(a.text("舰船 · 默认走舸",12,a.muted));InlineChoices<Army.Ship> ships=new InlineChoices<>(a,w,Arrays.asList(Army.Ship.values()),3,s->s.label+"\n"+(s==Army.Ship.BOAT?"免费携带":"库存 "+city.ships[s.ordinal()-1]),s->true,ship(),s->{draft.putInt("ship",s.ordinal());changed();});ships.setTag("deploy.ships");form.addView(ships);
        ration=a.text("",12,a.gold);ration.setTag("deploy.rations");food.addView(ration,1);form.addView(troops);form.addView(food);form.addView(gold);form.addView(a.text("调整兵力时按核心建议配粮；可再手动修改。\n新出征清空旧选择，页签切换不会丢失当前配置。",12,a.muted));ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(form);return scroll;
    }
    private View detailsPage(){LinearLayout form=column();form.setPadding(a.dp(5),a.dp(8),a.dp(5),a.dp(12));LinearLayout ratings=new LinearLayout(a);attack=a.text("攻击 —",24,a.gold);defense=a.text("防御 —",24,a.gold);attack.setTag("deploy.attack");defense.setTag("deploy.defense");ratings.addView(attack,new LinearLayout.LayoutParams(0,a.dp(58),1));ratings.addView(defense,new LinearLayout.LayoutParams(0,a.dp(58),1));form.addView(ratings);stats=a.text("",14,a.paper);stats.setTag("deploy.details");stats.setTextIsSelectable(true);form.addView(stats);ScrollView scroll=new ScrollView(a);scroll.addView(form);return scroll;}
    private void tab(int index){draft.putInt("tab",index);for(int i=0;i<3;i++){pages[i].setVisibility(i==index?View.VISIBLE:View.GONE);tabs[i].setTextColor(i==index?a.gold:a.muted);tabs[i].setSelected(i==index);tabs[i].setBackground(UiTheme.surface(a,i==index?0xff38584f:0xff23323b,i==index?0xff183b32:0xff15212a,8));}save();if(root!=null)fitForm(root);}
    private int rank(World.Officer o){return sort==0?o.leadership:sort==1?o.war:sort==2?o.intelligence:sort==3?o.politics:o.charm;}
    private void filter(){String q=search==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);filtered=new ArrayList<>();for(World.Officer o:available)if(q.isEmpty()||(o.name+Skill.label(o.skillId)).toLowerCase(Locale.ROOT).contains(q))filtered.add(o);filtered.sort(Comparator.comparingInt((World.Officer o)->-rank(o)).thenComparingInt(o->o.id));if(rosterStatus!=null)rosterStatus.setText(filtered.isEmpty()?(available.isEmpty()?"当前没有可出征的闲置武将":"没有匹配的武将 · 点 × 清空搜索"):"可选武将 "+filtered.size()+" 人");if(roster!=null)roster.notifyDataSetChanged();}
    private String role(int id){if(id==leader())return "主将";int[] ids=deputies();for(int i=0;i<ids.length;i++)if(ids[i]==id)return "副将"+(i+1);return "";}
    private List<Integer> deputyList(){List<Integer> result=new ArrayList<>();for(int id:deputies())result.add(id);return result;}
    private void putDeputies(List<Integer> ids){draft.putIntArray("deputies",ids.stream().mapToInt(i->i).toArray());}
    private void toggle(World.Officer o){
        List<Integer> ids=deputyList();if(leader()==o.id){draft.putInt("leader",ids.isEmpty()?-1:ids.remove(0));}
        else if(ids.remove(Integer.valueOf(o.id))){}else if(leader()<0)draft.putInt("leader",o.id);else if(ids.size()<2)ids.add(o.id);else{Toast.makeText(a,"最多三名武将；点击已选行可取消",Toast.LENGTH_SHORT).show();return;}
        putDeputies(ids);changed();
    }
    private void promote(World.Officer o){if(leader()==o.id)return;List<Integer> ids=deputyList();int pos=ids.indexOf(o.id);if(pos<0){toggle(o);return;}ids.set(pos,leader());draft.putInt("leader",o.id);putDeputies(ids);changed();}
    private void changed(){if(troops==null)return;update();if(roster!=null)roster.notifyDataSetChanged();}
    private String values(World.Officer o){return "统"+o.leadership+" 武"+o.war+" 智"+o.intelligence+" 政"+o.politics+" 魅"+o.charm+" 忠"+o.loyalty;}
    private String aptitudes(World.Officer o){StringBuilder s=new StringBuilder();String[] labels={"枪","戟","弩","骑","器","水"};for(int i=0;i<6;i++)s.append(labels[i]).append(War.rankLabel(o.aptitude[i])).append(' ');return s.toString();}
    private void update(){
        if(updating||summary==null)return;
        previewHandler.removeCallbacks(previewWork);preview=null;
        draft.putString("troops",troops.draftValue());draft.putString("food",food.draftValue());draft.putString("gold",gold.draftValue());save();
        summary.setText("正在核对出征条件…");summary.setTextColor(a.muted);UiTheme.readable(summary);if(dialog!=null&&dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        previewHandler.postDelayed(previewWork,100);
    }
    private void refreshPreview(){
        if(updating||dialog==null||!dialog.isShowing())return;updating=true;
        try{
            command=new DeploymentCommand(expected,city.id,leader(),deputies(),draft.containsKey("weapon")?weapon().name():null,ship().name(),troops.value(),food.value(),gold.value());
            long started=android.os.SystemClock.uptimeMillis();preview=a.previewDeployment(command);
            android.util.Log.d("DeploymentPreview","queryMs="+(android.os.SystemClock.uptimeMillis()-started)+" reason="+preview.reasonCode);
            if(preview.limits!=null){
                DeploymentPreview.Limits limits=preview.limits;
                troops.bounds(limits.troopsMin,limits.troopsMax);
                food.bounds(Math.max(0,limits.foodMin),limits.foodMax);gold.bounds(limits.goldMin,limits.goldMax);
            }
            crew.removeAllViews();StringBuilder selected=new StringBuilder();List<Integer> ids=new ArrayList<>();ids.add(leader());ids.addAll(deputyList());
            for(int slot=0;slot<3;slot++){int id=slot<ids.size()?ids.get(slot):-1;World.Officer o=w.officer(id);String label=slot==0?"主将":"副将"+slot;Button tile=a.button(label+"\n"+(o==null?"待选":o.name),v->{if(o!=null&&o.id!=leader())promote(o);});tile.setTag("deploy.crew."+slot);tile.setMaxLines(2);tile.setTextSize(12);tile.setGravity(Gravity.CENTER);tile.setPadding(a.dp(3),a.dp(4),a.dp(3),a.dp(4));
                if(o!=null){android.graphics.drawable.Drawable icon=GameIcon.drawable(a,w,o);icon.setBounds(0,0,a.dp(27),a.dp(27));tile.setCompoundDrawables(null,icon,null,null);tile.setContentDescription(label+o.name+(slot==0?"":"，点击设为主将"));selected.append(label).append(' ').append(o.name).append(" · ").append(values(o)).append("\n").append(aptitudes(o)).append(" · ").append(Skill.label(o.skillId)).append("\n");}
                crew.addView(tile,new LinearLayout.LayoutParams(0,a.dp(78),1));
            }
            crewDetails.setText(selected.length()==0?"在下方同一个列表连续点击即可选好三将。\n第一位为主将，之后两位为副将；副将可留空。":selected.toString().trim());
            DeploymentPreview.UnitFacts facts=preview.unit;
            ration.setText(facts==null?"完成有效编队后显示当前地块粮耗":"当前地块旬耗 "+facts.foodUse+" · 可支撑 "+(facts.foodTurns==Integer.MAX_VALUE?"不限":facts.foodTurns)+" 旬\n按当前编队、位置与补给范围估算");
            String error=error();summary.setTextColor(error==null?a.gold:0xfff2aa9e);UiTheme.readable(summary);summary.setText(error==null?"留守：兵 "+preview.remaining.troops+" · 粮 "+preview.remaining.food+" · 金 "+preview.remaining.gold+"\n"+weapon().label+" · "+(1+deputies().length)+"将 · 行动力"+preview.actionPointsCost+" · 可直接确认出征":error);
            summary.setContentDescription("出征条件 · "+preview.reasonCode+" · "+preview.field+" · "+summary.getText());
            if(dialog!=null&&dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error==null);
            if(facts==null){attack.setText("攻击 —");defense.setText("防御 —");stats.setText("选好主将与兵种后，显示实际出征格上的部队数值。\n"+(error==null?"城外没有可用出征格":error));return;}
            attack.setText(String.format(Locale.ROOT,"攻击 %.1f",facts.attackRating));defense.setText(String.format(Locale.ROOT,"防御 %.1f",facts.defenseRating));
            StringBuilder details=new StringBuilder();details.append(facts.equipmentLabel).append(" · 适性 ").append(War.rankLabel(facts.aptitude)).append("\n兵力 ").append(troops.value()).append(" / 统兵上限 ").append(preview.limits.commandLimit).append(" · 气力 ").append(facts.energy)
                .append("\n部队统率 ").append(facts.leadership).append(" · 武力 ").append(facts.war).append(" · 智力 ").append(facts.intelligence)
                .append("\n中心起点移动预算 ").append(facts.movementBudget).append(" · 出城已付 ").append(facts.movementSpent).append(" · 本旬剩余 ").append(facts.movementRemaining).append(" · 普攻射程 ").append(facts.attackRange)
                .append("\n出征格 (").append(facts.exitQ).append(',').append(facts.exitR).append(") · ").append(TerrainPresentation.detail(w,new Hex(facts.exitQ,facts.exitR)).presentation().name())
                .append("\n\n后勤\n携金 ").append(gold.value()).append(" · 携粮 ").append(food.value()).append("\n当前地块旬耗 ").append(facts.foodUse).append(" · 可支撑 ").append(facts.foodTurns==Integer.MAX_VALUE?"不限":facts.foodTurns).append(" 旬\n兵装消耗 ").append(preview.cost.equipment).append(" · 行动力").append(preview.actionPointsCost).append("\n\n编队特技\n");
            for(DeploymentPreview.SkillFact skill:facts.skills)details.append(skill.label).append("：").append(skill.description).append('\n');
            if(facts.skills.isEmpty())details.append("无编队特技\n");
            details.append("\n战法（目标合法性在选点时核对）\n");
            for(DeploymentPreview.TacticFact tactic:facts.tactics){details.append(tactic.label).append(" · 气力").append(tactic.energy).append(" · 射程 ").append(tactic.minRange).append('–').append(tactic.maxRange).append("\n").append(tactic.description);if(tactic.formationError!=null)details.append("\n当前不可用：").append(tactic.formationError);details.append('\n');}
            if(facts.tactics.isEmpty())details.append("当前编队没有专属战法\n");
            details.append("\n攻防数值\n基础攻击 ").append(facts.baseAttack).append(" · 基础防御 ").append(facts.baseDefense).append("\n出征地块修正后的攻防见上方，数值来自当前编队权威预览。");stats.setText(details);

        }finally{updating=false;}
    }
    private String error(){
        if(leader()<0)return "① 武将编队：请选择主将";
        if(!draft.containsKey("weapon"))return "② 兵种钱粮：请点选一个兵种";
        if(preview==null)return "正在核对出征条件…";
        if(preview.error!=CommandResult.Error.NONE)return MainActivity.deploymentError(preview.error,preview.detail);
        return null;
    }
    private final class Roster extends BaseAdapter {
        public int getCount(){return filtered.size();}public World.Officer getItem(int p){return filtered.get(p);}public long getItemId(int p){return getItem(p).id;}public boolean hasStableIds(){return true;}
        public View getView(int p,View convert,ViewGroup parent){
            World.Officer o=getItem(p);LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(a.dp(3),a.dp(4),a.dp(3),a.dp(4));row.setTag("deploy.officer."+o.id);
            ImageView portrait=new ImageView(a);portrait.setImageDrawable(GameIcon.drawable(a,w,o));row.addView(portrait,new LinearLayout.LayoutParams(a.dp(34),a.dp(42)));
            LinearLayout texts=column();TextView name=a.text((role(o.id).isEmpty()?"":"〔"+role(o.id)+"〕")+o.name+" · "+Skill.label(o.skillId),14,role(o.id).isEmpty()?a.paper:a.gold);texts.addView(name);texts.addView(a.text(values(o),11,a.muted));texts.addView(a.text(aptitudes(o),11,a.muted));row.addView(texts,new LinearLayout.LayoutParams(0,-2,1));
            String role=role(o.id);Button select=a.button(role.isEmpty()?"选用":role.equals("主将")?"移除":"设主将",v->{if(!role.isEmpty()&&!role.equals("主将"))promote(o);else toggle(o);});select.setTextSize(11);select.setContentDescription((role.isEmpty()?"选入编队 ":role.equals("主将")?"从编队移除 ":"设为主将 ")+o.name);select.setTag("deploy.role."+o.id);row.addView(select,new LinearLayout.LayoutParams(a.dp(64),a.dp(48)));row.setOnClickListener(v->toggle(o));return row;
        }
    }
}

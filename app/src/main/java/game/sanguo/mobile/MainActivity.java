package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.content.res.Configuration;
import android.util.AtomicFile;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import game.sanguo.core.*;

public final class MainActivity extends Activity {
    private World world;
    private MapView map;
    private LinearLayout panel,root,body,commandDock,panelShell;
    private MarchOrders.Plan pendingMarch;
    private FrameLayout panelHost;
    private ScrollView panelScroll;
    private TextView title,panelTitle,battleBanner;
    private String lastBattleReport="";
    private World battleReportWorld;
    private Button selectionButton,expandPanel,closePanel;
    private AlertDialog navigationDialog;
    private AlertDialog confirmationDialog;
    private Hex selected;
    private int moving=-1;
    private String unitCommand="select";
    private Set<Hex> pickTargets=Collections.emptySet();
    private java.util.function.Consumer<Hex> mapPick;
    private String pickTitle="";
    private boolean aiRunning;
    private Button nextTurn;
    private final ClientState ui=new ClientState();
    private TurnWork turnWork;
    private final Map<String,Button> navigation=new LinkedHashMap<>();
    final int ink=Color.rgb(13,22,32),paper=Color.rgb(235,241,247),gold=Color.rgb(109,220,197),muted=Color.rgb(155,176,195);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);world=DemoScenario.create();ui.read(state);
        applyOrientationPreference();
        String restoreError=null;boolean restored=false;
        AtomicFile autosave=file("auto");
        if(present(autosave)) {
            try{world=readSave(autosave);restored=true;}catch(IOException e){restoreError="自动存档损坏或版本不兼容。可在菜单读取手动存档。";}
        }
        turnWork=(TurnWork)getLastNonConfigurationInstance();
        if(turnWork!=null){world=turnWork.before;aiRunning=!turnWork.done;}
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(ink);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(12),0,dp(8),0);
        title=text("",12,gold);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setOnClickListener(v->{if(!aiRunning)message("当前军情",world.scenarioName+" · "+world.faction(world.player)+"\n"+world.date()+" · 行动力 "+world.actionPoints[world.player]+"\n进行中任务 "+taskCount());});
        header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        Button tools=button("视图",v->showMapTools());tools.setContentDescription("地图工具 · 全图、定位、导航图和屏幕方向");
        header.addView(tools,new LinearLayout.LayoutParams(dp(56),dp(48)));root.addView(header);
        body=new LinearLayout(this);map=new MapView(this,this::onTile);body.addView(map);map.setTerritoryMode(getPreferences(MODE_PRIVATE).getInt("territoryMode",0));
        panelShell=new LinearLayout(this);panelShell.setOrientation(LinearLayout.VERTICAL);panelShell.setBackgroundColor(0xff131f2c);
        panelShell.setVisibility(View.GONE);body.addView(panelShell);
        LinearLayout panelHeader=new LinearLayout(this);panelHeader.setPadding(dp(12),0,dp(4),0);panelHeader.setGravity(Gravity.CENTER_VERTICAL);
        panelTitle=text("指令",14,gold);panelTitle.setMaxLines(1);panelTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panelHeader.addView(panelTitle,new LinearLayout.LayoutParams(0,dp(48),1));
        expandPanel=button("展开",v->{ui.panelExpanded=!ui.panelExpanded;layoutPanels();});expandPanel.setContentDescription("展开或缩小操作面板");
        panelHeader.addView(expandPanel,new LinearLayout.LayoutParams(dp(56),dp(48)));
        closePanel=button("收起",v->closePanel());closePanel.setContentDescription("收起操作面板，返回大地图");
        panelHeader.addView(closePanel,new LinearLayout.LayoutParams(dp(56),dp(48)));panelShell.addView(panelHeader);
        panelHost=new FrameLayout(this);panelShell.addView(panelHost,new LinearLayout.LayoutParams(-1,0,1));
        panelScroll=new ScrollView(this);panelScroll.setFillViewport(true);panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(12),dp(4),dp(12),dp(10));panelScroll.addView(panel);
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        battleBanner=text("",13,paper);battleBanner.setMaxLines(2);battleBanner.setEllipsize(android.text.TextUtils.TruncateAt.END);
        battleBanner.setPadding(dp(12),dp(6),dp(12),dp(6));battleBanner.setBackgroundColor(0xff30443b);battleBanner.setVisibility(View.GONE);
        battleBanner.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        battleBanner.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("战斗结果").setMessage(lastBattleReport)
            .setPositiveButton("返回",null).setNeutralButton("收起战果",(d,n)->battleBanner.setVisibility(View.GONE)).show());
        root.addView(battleBanner,new LinearLayout.LayoutParams(-1,-2));
        body.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)layoutPanels();});
        commandDock=new LinearLayout(this);commandDock.setPadding(dp(8),dp(4),dp(8),dp(4));commandDock.setBackgroundColor(0xff162934);root.addView(commandDock);
        LinearLayout bottom=new LinearLayout(this);bottom.setPadding(dp(6),0,dp(6),0);bottom.setGravity(Gravity.CENTER_VERTICAL);
        Button functions=button("功能",v->showNavigation());functions.setContentDescription("打开功能导航 · 城市、武将、任务、菜单");
        bottom.addView(functions,new LinearLayout.LayoutParams(dp(64),dp(52)));
        selectionButton=button("点选城池",v->{ui.page="map";ui.panelVisible=!ui.panelVisible;refresh();revealPanel();});
        selectionButton.setMaxLines(1);selectionButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
        bottom.addView(selectionButton,new LinearLayout.LayoutParams(0,dp(52),1));
        nextTurn=button("下一旬  →",v->confirmTurn());nextTurn.setSelected(true);bottom.addView(nextTurn,new LinearLayout.LayoutParams(dp(104),dp(52)));root.addView(bottom);
        setContentView(root);root.requestApplyInsets();selected=world.home()==null?null:world.home().hex;
        if(state!=null){Hex h=new Hex(state.getInt("selectedQ",-1),state.getInt("selectedR",-1));if(world.inside(h))selected=h;moving=state.getInt("moving",-1);}
        if(state!=null)unitCommand=state.getString("unitCommand","select");
        if(state!=null&&state.containsKey("routeQ")&&world.unit(moving)!=null)pendingMarch=world.marches.preview(moving,new Hex(state.getInt("routeQ"),state.getInt("routeR")));
        refresh();if(state!=null)map.restoreCamera(state);
        if(turnWork!=null)turnWork.observe(this::finishTurn);
        if(!restored&&restoreError==null&&state==null)root.post(this::scenarioPicker);
        if(restoreError!=null)message("自动存档未能读取",restoreError);
    }
    private boolean portrait(){return getResources().getConfiguration().orientation!=Configuration.ORIENTATION_LANDSCAPE;}
    private void layoutPanels(){
        if(body==null||body.getWidth()==0||body.getHeight()==0)return;
        boolean vertical=portrait();body.setOrientation(vertical?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams mapParams=new LinearLayout.LayoutParams(vertical?-1:0,vertical?0:-1,1);
        LinearLayout.LayoutParams sheetParams=vertical
            ?new LinearLayout.LayoutParams(-1,Math.round(body.getHeight()*(ui.panelExpanded?.78f:.50f)))
            :new LinearLayout.LayoutParams(Math.min(dp(ui.panelExpanded?460:344),Math.round(body.getWidth()*(ui.panelExpanded?.60f:.42f))),-1);
        setPanelParams(map,mapParams);setPanelParams(panelShell,sheetParams);
        expandPanel.setText(ui.panelExpanded?"缩小":"展开");
    }
    private void setPanelParams(View view,LinearLayout.LayoutParams params){
        android.view.ViewGroup.LayoutParams old=view.getLayoutParams();
        if(!(old instanceof LinearLayout.LayoutParams)||old.width!=params.width||old.height!=params.height||((LinearLayout.LayoutParams)old).weight!=params.weight)view.setLayoutParams(params);
    }
    private void closePanel(){ui.panelVisible=false;ui.panelExpanded=false;ui.page="map";refresh();}
    private void showNavigation(){
        if(mapPick!=null)cancelMapPick();
        if(navigationDialog!=null&&navigationDialog.isShowing())return;
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(12),dp(4),dp(12),dp(12));
        navigation.clear();String[] keys={"map","cities","officers","tasks","menu"},labels={"地图","城市","武将","任务","菜单"};
        ScrollView scroll=new ScrollView(this);scroll.addView(list);
        navigationDialog=new AlertDialog.Builder(this).setTitle("功能导航").setView(scroll).setNegativeButton("返回",null).create();
        int pendingTasks=taskCount();
        for(int i=0;i<keys.length;i++){
            final String page=keys[i];String label=labels[i]+(page.equals("tasks")&&pendingTasks>0?" "+pendingTasks:"");
            Button item=button(label,v->{navigationDialog.dismiss();ui.page=page;ui.panelVisible=!page.equals("map");ui.panelExpanded=false;refresh();revealPanel();});
            item.setContentDescription("导航 · "+labels[i]);navigation.put(page,item);list.addView(item,new LinearLayout.LayoutParams(-1,dp(48)));
        }
        navigationDialog.show();
    }
    private void showMapTools(){
        if(mapPick!=null)cancelMapPick();
        String[] labels={"全图","定位","导航图","屏幕方向","战报","操作说明","战斗震动","兵种与建筑图例","领地着色 / 前线","势力领地图例","军团托管","全国城池总览"};
        new AlertDialog.Builder(this).setTitle("地图视图").setItems(labels,(d,index)->{
            if(aiRunning)return;
            if(index==0){closePanel();map.post(map::fit);}
            else if(index==1){closePanel();if(selected!=null)map.post(()->map.focus(selected));}
            else if(index==2){closePanel();map.toggleNavigator();}
            else if(index==3)showOrientationPicker();
            else if(index==4)message("战报",String.join("\n",world.log));
            else if(index==6){boolean enabled=getPreferences(MODE_PRIVATE).getBoolean("battleHaptics",true);
                new AlertDialog.Builder(this).setTitle("战斗震动").setSingleChoiceItems(new String[]{"开启（遵循系统触感设置）","关闭"},enabled?0:1,(dialog,which)->{getPreferences(MODE_PRIVATE).edit().putBoolean("battleHaptics",which==0).apply();dialog.dismiss();}).setNegativeButton("返回",null).show();}
            else if(index==7)VisualGuide.show(this,world);
            else if(index==8)showTerritoryPicker();
            else if(index==9)showTerritoryLegend();
            else if(index==10)new WorldUi(this,world,this::apply).districts();
            else if(index==11){ui.page="cities";ui.panelVisible=true;ui.panelExpanded=true;refresh();revealPanel();}
            else message("地图操作","单指拖动 · 双指缩放 · 双击城池定位\n点城池或部队打开指令，点空地或「收起」返回大地图。\n「功能」打开城市、武将、任务和存档菜单。\n竖屏使用底部面板，横屏使用右侧面板；「展开」可查看更多内容。\n选中部队即显示青色行动范围和红色攻击目标。先点「行军」再点目标预览路线；「攻击」「战法」「计略」在固定底栏。普通点空地、再点本队或「取消选中」可解除选择。返回键依次取消路线、指令、选中。");
        }).setNegativeButton("返回",null).show();
    }
    private void showTerritoryPicker(){
        new AlertDialog.Builder(this).setTitle("领地着色 / 前线").setSingleChoiceItems(
            new String[]{"关闭领地着色","势力范围 · 同势力合并","据点辖区 · 城 / 关 / 港边界"},map.territoryMode(),(dialog,which)->{
                getPreferences(MODE_PRIVATE).edit().putInt("territoryMode",which).apply();map.setTerritoryMode(which);dialog.dismiss();closePanel();
            }).setNeutralButton("前线据点",(d,n)->showFrontlines()).setNegativeButton("返回",null).show();
    }
    private void showFrontlines(){
        List<World.City> cities=new ArrayList<>();Territory territory=map.territory();
        for(World.City city:world.cities)if(territory.frontline(city.id))cities.add(city);
        cities.sort(Comparator.comparingInt((World.City c)->c.owner==world.player?0:1).thenComparingInt(c->c.owner).thenComparingInt(c->c.id));
        if(cities.isEmpty()){message("前线据点","当前没有与交战势力辖区接壤的据点。");return;}
        new AlertDialog.Builder(this).setTitle("前线据点 · 点选定位").setItems(cities.stream().map(c->c.name+" · "+world.faction(c.owner)).toArray(String[]::new),
            (d,n)->selectAndFocus(cities.get(n).hex)).setNegativeButton("返回",null).show();
    }
    private void showTerritoryLegend(){
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(20),dp(10),dp(20),dp(10));
        TextView help=text("半透明色块表示辖区，橙圈为与交战势力接壤的前线，红色!为已有敌军逼近。点据点可查看辖区与邻接关系。",14,paper);list.addView(help);
        for(int owner=-1;owner<world.factions.length;owner++){
            int count=0,front=0;for(World.City c:world.cities)if(c.owner==owner){count++;if(map.territory().frontline(c.id))front++;}
            if(count==0)continue;TextView row=text("■  "+world.faction(owner)+" · "+count+"据点 · 前线"+front,16,MapView.factionColor(owner));row.setPadding(0,dp(8),0,dp(8));list.addView(row);
        }
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(ink);scroll.addView(list);
        new AlertDialog.Builder(this).setTitle("势力领地图例").setView(scroll).setPositiveButton("前线据点",(d,n)->showFrontlines()).setNegativeButton("返回",null).show();
    }
    private void showOrientationPicker(){
        int selectedMode=getPreferences(MODE_PRIVATE).getInt("screenMode",0);
        new AlertDialog.Builder(this).setTitle("屏幕方向").setSingleChoiceItems(new String[]{"跟随系统","竖屏","横屏"},selectedMode,(d,which)->{
            getPreferences(MODE_PRIVATE).edit().putInt("screenMode",which).apply();d.dismiss();applyOrientationPreference();
        }).setNegativeButton("返回",null).show();
    }
    private void applyOrientationPreference(){
        int mode=getPreferences(MODE_PRIVATE).getInt("screenMode",0);
        setRequestedOrientation(mode==1?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:mode==2?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
    }
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);if(root!=null){layoutPanels();refreshCommandDock();root.requestApplyInsets();}fitConfirmation();}
    void trackDialog(AlertDialog dialog){confirmationDialog=dialog;fitConfirmation();}
    private void fitConfirmation(){
        if(confirmationDialog==null||!confirmationDialog.isShowing())return;
        // A dialog opened during sensor rotation can retain the previous orientation's minimum width.
        int width=dp(Math.min(560,Math.max(240,getResources().getConfiguration().screenWidthDp-32)));
        confirmationDialog.getWindow().setLayout(width,WindowManager.LayoutParams.WRAP_CONTENT);
    }
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    Button button(String value,View.OnClickListener action){Button b=new Button(this);b.setText(value);b.setTextSize(13);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(4),0,dp(4),0);b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff6b7d8d,paper}));
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_selected},new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff285256,0xff182638,0xff213547}));shape.setCornerRadius(dp(12));shape.setStroke(dp(1),0xff30485b);
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x446ddcc5),new android.graphics.drawable.InsetDrawable(shape,dp(3),dp(3),dp(3),dp(3)),null));b.setOnClickListener(v->{if(!aiRunning)action.onClick(v);});return b;}
    private void line(String value,int size,int color){TextView t=text(value,size,color);t.setPadding(0,dp(4),0,dp(4));panel.addView(t);}
    private void action(String label,View.OnClickListener click){Button b=button(label,click);b.setEnabled(!aiRunning);panel.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));}
    private void iconAction(String label,Object item,View.OnClickListener click){
        Button b=button(label,click);android.graphics.drawable.Drawable icon=GameIcon.drawable(this,world,item);icon.setBounds(0,0,dp(36),dp(36));b.setCompoundDrawables(icon,null,null,null);b.setCompoundDrawablePadding(dp(8));b.setEnabled(!aiRunning);panel.addView(b,new LinearLayout.LayoutParams(-1,dp(52)));
    }
    private LinearLayout visualHeader(Object item,String name,String subtitle,int size){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(6),0,dp(6));
        ImageView icon=new ImageView(this);icon.setImageDrawable(GameIcon.drawable(this,world,item));icon.setContentDescription(name+"头像或模型");row.addView(icon,new LinearLayout.LayoutParams(dp(size),dp(size)));
        LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(dp(12),0,0,0);words.addView(text(name,22,gold));words.addView(text(subtitle,13,paper));row.addView(words,new LinearLayout.LayoutParams(0,-2,1));return row;
    }
    private void onTile(Hex h) {
        if(aiRunning||world.commandsBlocked())return;
        if(mapPick!=null){
            if(h!=null&&pickTargets.contains(h)){map.center(h);mapPick.accept(h);}
            else Toast.makeText(this,"请点选高亮地块，或点「取消选取」返回",Toast.LENGTH_SHORT).show();
            return;
        }
        if(h==null){clearUnitSelection();return;}
        World.Unit target=world.unitAt(h);World.City city=world.cityAt(h);World.Unit source=world.unit(moving);
        if(source!=null&&source.owner==world.player) {
            if(target==source){clearUnitSelection();return;}
            if(unitCommand.equals("march")){previewMarch(source,h);return;}
            Domestic.Facility facility=world.domestic.at(h);
            if(facility!=null){
                if(world.campaign.hostile(source.owner,world.city(facility.cityId).owner)){
                    pendingMarch=null;
                    if(source.hex.distance(h)>world.war.range(source)){approach(source,h);return;}
                    if(!world.army.canAttackUnit(source)){showTactics(source);return;}
                    String error=world.war.facilityAttackError(source.id,h);if(error!=null){message("无法攻击",error);return;}
                    confirm("攻击"+facility.kind.label+"？\n耐久 "+facility.hp+"/"+facility.maxHp()+" · 预计减少"+Math.min(facility.hp,world.war.facilityDamage(source.id))+"\n攻击结束本旬行动，摧毁后释放地块。",()->apply(world.war.attackFacility(source.id,h)));return;
                }
                if(unitCommand.equals("attack")){message("无法攻击","不能攻击己方或有协定的设施");return;}
                selectAndFocus(h);return;
            }
            if(world.events.at(h)!=null){pendingMarch=null;new WorldUi(this,world,this::apply).attack(source,world.events.at(h));return;}
            if(source instanceof Domestic.Mission&&target!=null&&target.owner==world.player&&target.id!=source.id){convoySupply((Domestic.Mission)source);return;}
            if(target!=null&&target.owner!=world.player){if(!world.campaign.hostile(source.owner,target.owner)){selectAndFocus(h);return;}if(source.hex.distance(h)<=world.war.range(source)){pendingMarch=null;if(!world.army.canAttackUnit(source))showTactics(source);else warUi().attack(source,target);}else approach(source,h);return;}
            if(world.war.at(h)!=null&&!world.campaign.hostile(source.owner,world.war.at(h).owner)){selectAndFocus(h);return;}
            if(world.war.at(h)!=null&&source.hex.distance(h)>world.war.range(source)){approach(source,h);return;}
            if(world.war.at(h)!=null){pendingMarch=null;if(!world.army.canAttackUnit(source)){showTactics(source);return;}confirm("攻击军事设施？",()->apply(world.war.attackStructure(source.id,h)));return;}
            if(city!=null){
                if(city.owner!=world.player&&!world.campaign.hostile(source.owner,city.owner)){selectAndFocus(h);return;}
                if(source.hex.distance(h)>(city.owner==world.player?1:world.army.siegeRange(source))){approach(source,h);return;}
                pendingMarch=null;
                if(city.owner==world.player)confirm("进入"+city.name+"并归还兵装与粮草？",()->{World.Result result=world.enter(source.id,city.id);if(result.ok)moving=-1;apply(result);});
                else if(!world.army.canAttackUnit(source))showTactics(source);
                else confirm("攻击"+city.name+"？",()->apply(world.siege(source.id,city.id)));
                return;
            }
            for(Domestic.Mission m:world.domestic.missions)if(m.transport&&m.hex.equals(h)&&world.campaign.hostile(source.owner,m.owner)){governmentUi().raid(source,m);return;}
            if(target==null&&unitCommand.equals("march")){previewMarch(source,h);return;}
            if(unitCommand.equals("attack")){message("选择攻击目标","请点选射程内敌军、城池或设施；返回键可退出攻击。 ");return;}
            if(target==null){clearUnitSelection();return;}
        }
        pendingMarch=null;unitCommand="select";
        if(world.events.at(h)!=null){new WorldUi(this,world,this::apply).camp(world.events.at(h));return;}
        if(!h.equals(selected))ui.group="概览";
        selected=h;moving=target!=null&&target.owner==world.player?target.id:-1;ui.page="map";ui.panelVisible=target!=null||city!=null||world.domestic.at(h)!=null||world.war.at(h)!=null||world.war.fireAt(h)!=null;ui.panelExpanded=false;refresh();if(ui.panelVisible)map.post(()->map.center(h));revealPanel();
    }
    private void clearUnitSelection(){pendingMarch=null;unitCommand="select";moving=-1;selected=null;closePanel();}
    void pickOnMap(String title,Hex origin,Collection<Hex> targets,java.util.function.Consumer<Hex> action){
        if(targets.isEmpty()){message(title,"当前没有可用目标。请检查射程、气力、适性、库存或地块条件。");return;}
        pendingMarch=null;unitCommand="select";pickTitle=title;pickTargets=new LinkedHashSet<>(targets);mapPick=action;
        ui.page="map";ui.panelVisible=false;ui.panelExpanded=false;refresh();map.post(()->map.focus(origin));
        commandDock.announceForAccessibility(title+"，请点选高亮地块");
    }
    private void cancelMapPick(){mapPick=null;pickTargets=Collections.emptySet();pickTitle="";refresh();}
    private void approach(World.Unit u,Hex h){
        if(unitCommand.equals("march")){previewMarch(u,h);return;}
        confirm("目标在当前射程外，规划行军到目标附近？抵达后需要再次下令攻击。",()->{unitCommand="march";previewMarch(u,h);});
    }
    private void showTactics(World.Unit u){
        pendingMarch=null;unitCommand="select";refresh();
        String error=world.orders.error(u);if(error!=null){message("战法暂不可用",error);return;}
        if(world.army.water(u.hex)||Army.siegeWeapon(u.weapon))armyUi().tactics(u);else warUi().tactics(u);
    }
    private void message(String title,String value){new AlertDialog.Builder(this).setTitle(title).setMessage(value).setPositiveButton("返回",null).show();}
    private void confirm(String value,Runnable action){boolean[] submitted={false};confirmationDialog=new AlertDialog.Builder(this).setMessage(value).setPositiveButton("执行",(d,w)->{if(!aiRunning&&!submitted[0]){submitted[0]=true;action.run();}}).setNegativeButton("取消",null).show();fitConfirmation();}
    void applyResult(World.Result result){apply(result);}
    private void apply(World.Result result){
        if(!result.ok)message("命令未执行",result.message);
        if(result.ok){pendingMarch=null;unitCommand="select";mapPick=null;pickTargets=Collections.emptySet();pickTitle="";}
        if(result.ok&&result.feedback!=World.Feedback.NONE){
            lastBattleReport=result.message;battleReportWorld=world;
            battleBanner.setText((result.feedback==World.Feedback.DEFEAT?"击破战果 · ":"战斗 · ")+result.message+"  · 点此详情");
            battleBanner.setVisibility(View.VISIBLE);
            map.battleFeedback(result,getPreferences(MODE_PRIVATE).getBoolean("battleHaptics",true));
        }
        if(result.ok&&moving>=0&&world.unit(moving)!=null)selected=world.unit(moving).hex;
        refresh();if(result.ok)save("auto",false);
        if(result.ok&&world.gameOver())message(world.winner==world.player?"战场胜利":"战场战败","本局结束，可从菜单重新选择剧本。");
    }
    void refresh(){
        if(battleReportWorld!=world){battleReportWorld=world;lastBattleReport="";battleBanner.setVisibility(View.GONE);}
        if(ui.city>=0&&world.city(ui.city)==null)ui.city=-1;
        if(ui.cityDistrict>0&&world.districts.get(ui.cityDistrict)==null)ui.cityDistrict=-1;
        if(ui.owner>=world.factions.length)ui.owner=-1;
        if(ui.cityOwner>=world.factions.length)ui.cityOwner=-1;
        title.setText(world.scenarioName+"  ·  "+world.faction(world.player)+"\n"+world.date()+" · 行动力 "+world.actionPoints[world.player]+(aiRunning?" · 结算中…":""));
        title.setContentDescription("军情 · "+title.getText());
        nextTurn.setEnabled(mapPick==null&&!aiRunning&&!world.gameOver()&&!world.commandsBlocked());nextTurn.setText(aiRunning?"结算中…":"下一旬  →");
        for(Map.Entry<String,Button> e:navigation.entrySet()){e.getValue().setEnabled(!aiRunning);e.getValue().setSelected(e.getKey().equals(ui.page));e.getValue().setTextColor(e.getKey().equals(ui.page)?gold:paper);}
        if(navigation.get("tasks")!=null)navigation.get("tasks").setText("任务"+(taskCount()>0?" "+taskCount():""));
        panelHost.removeAllViews();panel.removeAllViews();
        boolean required=(world.life.pending()||world.contests.busy())&&!ui.page.equals("menu");
        panelShell.setVisibility(ui.panelVisible||required?View.VISIBLE:View.GONE);closePanel.setEnabled(!required&&!aiRunning);
        String selectedName=selected==null?"点选城池":world.cityAt(selected)!=null?world.cityAt(selected).name:world.unitAt(selected)!=null?world.officer(world.unitAt(selected).officerId).name:"地块";
        selectionButton.setText(selectedName+(panelShell.getVisibility()==View.VISIBLE?" · 收起":" · 指令"));
        selectionButton.setContentDescription("选中对象指令 · "+selectedName);selectionButton.setEnabled(mapPick==null&&!aiRunning&&!required);
        panelTitle.setText(ui.page.equals("map")?selectedName+" · 指令":ui.page.equals("cities")?"城池一览":ui.page.equals("officers")?"武将":ui.page.equals("tasks")?"任务":ui.page.equals("menu")?"菜单":"资料");
        if(world.unit(moving)==null)moving=-1;
        map.setWorld(world,selected,moving);
        if(world.life.pending()&&!ui.page.equals("menu")){panelHost.addView(new LifecycleUi(this,world,this::apply).succession());}
        else if(world.contests.busy()&&!ui.page.equals("menu"))panelHost.addView(new ContestUi(this,world,this::apply).view());
        else if(ui.page.equals("cities"))panelHost.addView(new OverviewUi(this,world,ui).cities());
        else if(ui.page.equals("officers"))panelHost.addView(new OverviewUi(this,world,ui).officers());
        else if(ui.page.equals("content"))panelHost.addView(new ContentUi(this,world,ui).view());
        else if(ui.page.equals("factions"))panelHost.addView(new OverviewUi(this,world,ui).factions());
        else if(ui.page.equals("tasks"))panelHost.addView(new OverviewUi(this,world,ui).tasks());
        else {panelHost.addView(panelScroll);if(ui.page.equals("menu"))showMenu();else showSelection();}
        map.setRoute(pendingMarch!=null?pendingMarch:world.unit(moving)!=null&&world.unit(moving).march!=null?world.marches.current(world.unit(moving)):null);
        map.setPickTargets(mapPick==null?null:pickTargets);
        map.setEnabled(!aiRunning&&!world.commandsBlocked());refreshCommandDock();layoutPanels();
    }
    private void previewMarch(World.Unit unit,Hex target){
        pendingMarch=world.marches.preview(unit.id,target);ui.page="map";ui.panelVisible=false;refresh();
        if(!pendingMarch.valid())commandDock.announceForAccessibility(pendingMarch.error);
    }
    private void refreshCommandDock(){
        commandDock.removeAllViews();World.Unit u=world.unit(moving);
        if(mapPick!=null){
            commandDock.setVisibility(View.VISIBLE);commandDock.setOrientation(LinearLayout.HORIZONTAL);
            TextView hint=text(pickTitle+" · 点选高亮地块（"+pickTargets.size()+"处）",13,gold);hint.setMaxLines(2);
            commandDock.addView(hint,new LinearLayout.LayoutParams(0,-2,1));
            commandDock.addView(button("取消选取",v->cancelMapPick()),new LinearLayout.LayoutParams(dp(96),dp(48)));return;
        }
        commandDock.setVisibility(u!=null&&ui.page.equals("map")&&!world.commandsBlocked()?View.VISIBLE:View.GONE);
        if(u==null)return;
        commandDock.setOrientation(portrait()?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);commandDock.setGravity(Gravity.CENTER_VERTICAL);
        if(pendingMarch==null){
            String error=world.orders.error(u);
            String hint=unitCommand.equals("march")?"行军：点目标预览，确认后出发":unitCommand.equals("attack")?"攻击：点红框敌军、城池或设施":error!=null?error:"青色为本旬可达范围 · 红框可攻击";
            TextView state=text(hint+" · 剩余移动 "+world.orders.remaining(u),12,gold);state.setMaxLines(2);
            commandDock.addView(state,new LinearLayout.LayoutParams(portrait()?-1:0,-2,portrait()?0:1));
            LinearLayout actions=new LinearLayout(this);
            Button march=button("行军",v->{unitCommand="march";ui.panelVisible=false;refresh();});march.setSelected(unitCommand.equals("march"));
            Button attack=button("攻击",v->{unitCommand="attack";ui.panelVisible=false;refresh();});attack.setSelected(unitCommand.equals("attack"));attack.setEnabled(error==null&&!aiRunning);
            Button tactics=button("战法",v->showTactics(u));tactics.setEnabled(error==null&&!aiRunning);
            Button plots=button("计略",v->{pendingMarch=null;unitCommand="select";refresh();warUi().plots(u);});plots.setEnabled(error==null&&!aiRunning);
            Button cancel=button("取消选中",v->clearUnitSelection());
            if(u instanceof Domestic.Mission){attack.setText("补给");attack.setOnClickListener(v->convoySupply((Domestic.Mission)u));tactics.setText("停止");tactics.setOnClickListener(v->apply(world.marches.stop(u.id)));plots.setText("货物");plots.setOnClickListener(v->{ui.panelVisible=true;refresh();revealPanel();});}
            for(Button b:new Button[]{march,attack,tactics,plots,cancel})actions.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));
            commandDock.addView(actions,new LinearLayout.LayoutParams(portrait()?-1:dp(360),-2));return;
        }
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=text("路线预览 · "+pendingMarch.label,14,paper);heading.setMaxLines(1);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(heading);
        TextView hint=text(pendingMarch.valid()?(u.acted?"本旬已行动，下旬出发 · ":u.status!=War.Status.NORMAL?"异常状态，恢复后继续 · ":world.orders.remaining(u)==0?"移动力已用尽，下旬继续 · ":"本旬 "+pendingMarch.stepsNow+" 格 · ")+"预计再需 "+pendingMarch.estimatedTurns+" 旬 · 抵达后手动下令":pendingMarch.error,12,muted);hint.setMaxLines(2);hint.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(hint);
        commandDock.addView(copy,new LinearLayout.LayoutParams(portrait()?-1:0,-2,portrait()?0:1));
        LinearLayout actions=new LinearLayout(this);MarchOrders.Plan plan=pendingMarch;
        Button cancel=button("取消",v->{pendingMarch=null;unitCommand="select";refresh();});
        Button execute=button("开始行军",v->apply(world.marches.execute(plan)));execute.setTextColor(gold);execute.setEnabled(plan.valid()&&!aiRunning);
        actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(execute,new LinearLayout.LayoutParams(0,dp(48),1));
        commandDock.addView(actions,new LinearLayout.LayoutParams(portrait()?-1:dp(208),-2));
    }
    private void nextUnit(){
        List<World.Unit> units=new ArrayList<>();for(World.Unit unit:world.fieldUnits())if(unit.owner==world.player)units.add(unit);
        units.sort(Comparator.comparingInt(unit->unit.id));if(units.isEmpty())return;
        int index=0;for(int i=0;i<units.size();i++)if(units.get(i).id==moving)index=(i+1)%units.size();selectAndFocus(units.get(index).hex);
    }
    private void unitStats(World.Unit u){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(10),dp(6),dp(10),dp(6));
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setColor(0xff203346);shape.setCornerRadius(dp(12));card.setBackground(shape);
        TextView stats=text("兵力 "+u.troops+"   气力 "+u.energy+"\n携粮 "+u.food,15,paper);card.addView(stats);
        ProgressBar energy=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);energy.setMax(world.campaign.energyCap(u.owner));energy.setProgress(u.energy);energy.setProgressTintList(android.content.res.ColorStateList.valueOf(gold));energy.setContentDescription("气力 "+u.energy);card.addView(energy,new LinearLayout.LayoutParams(-1,dp(10)));panel.addView(card);
    }
    private int taskCount(){return UiModels.tasks(world,0).size();}
    private void showSelection(){
        World.Unit unit=selected==null?null:world.unitAt(selected);World.City city=selected==null?null:world.cityAt(selected);
        if(unit!=null)showUnit(unit);else if(city!=null)showCity(city);else if(selected!=null&&world.domestic.at(selected)!=null){
            Domestic.Facility f=world.domestic.at(selected);panel.addView(visualHeader(f,f.kind.label,world.city(f.cityId).name,64));line("耐久 "+f.hp+"/"+f.maxHp(),14,gold);line(f.remaining==0?f.kind.effect:"建设中 · 剩"+f.remaining+"旬",14,paper);
            action("设施详情 / 管理",v->domesticUi().facility(f));action("返回所属城池",v->selectAndFocus(world.city(f.cityId).hex));
        }else if(selected!=null&&world.war.at(selected)!=null){War.Structure s=world.war.at(selected);panel.addView(visualHeader(s.kind,s.kind.label,world.faction(s.owner),64));line(world.faction(s.owner)+" · 耐久"+s.hp+"/"+s.kind.hp,15,paper);line(s.complete?s.kind.effect:"施工中，建成后生效",14,paper);if(s.builder>=0&&world.unit(s.builder)!=null)action("定位施工部队",v->selectAndFocus(world.unit(s.builder).hex));}
        else {line("山河之间",23,gold);if(selected!=null&&world.war.fireAt(selected)!=null)line("火场 · 剩"+world.war.fireAt(selected).remaining+"旬",16,gold);line("点选城池或部队",15,paper);line("单指拖动 · 双指缩放\n双击城池聚焦\n缩小时保留占格标记，放大查看模型与名称。",14,paper);action("定位本城",v->{if(world.home()!=null)selectAndFocus(world.home().hex);});}
    }
    private void showCity(World.City c){
        boolean compact=!ui.group.equals("概览");
        panel.addView(visualHeader(c,c.name,world.faction(c.owner)+" · 太守 "+UiModels.governor(world,c.id),compact?44:64));
        line(compact?"金 "+c.gold+" · 粮 "+c.food+" · 兵 "+c.troops:"金 "+c.gold+"    粮 "+c.food+"\n兵 "+c.troops+"    城防 "+c.defense,compact?13:15,paper);
        String[] groups={"概览","内政","武将","军事","调动","外交","研究"};
        for(int first=0;first<groups.length;first+=4){
            LinearLayout row=new LinearLayout(this);
            for(int i=first;i<Math.min(first+4,groups.length);i++){
                String name=groups[i];Button b=button(name,v->{ui.group=name;refresh();revealPanel();});b.setSelected(ui.group.equals(name));b.setTextColor(ui.group.equals(name)?gold:paper);
                row.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));
            }
            panel.addView(row);
        }
        Districts.District district=world.districts.city(c.id);if(district!=null)line("所属军团："+district.name()+" · "+district.policy().label,13,gold);
        String incident=world.events.cityStatus(c.id);if(!incident.equals("无灾害"))line(incident,12,muted);
        boolean own=c.owner==world.player&&!world.gameOver()&&world.districts.directCity(c.id);
        switch(ui.group){
            case "内政":
                if(c.owner==world.player)action("城池托管 / 军团",v->new WorldUi(this,world,this::apply).city(c));
                line("设施 "+world.domestic.count(c.id)+"/"+Domestic.CITY_SLOTS+"\n"+world.domestic.incomeSchedule(c.id),14,paper);
                boolean construction=false;
                for(Domestic.Facility f:world.domestic.facilities)if(f.cityId==c.id){construction|=f.remaining>0;iconAction(f.kind.label+" · "+(f.remaining==0?"已建成":"剩"+f.remaining+"旬"),f,v->domesticUi().facility(f));}
                if(!construction)line("当前没有建设中的设施",13,muted);
                if(own)action("设施开发",v->domesticUi().build(c));
                if(own)action("商人 / 粮食买卖",v->campaignUi().trade(c));
                if(own)action("巡察 · 金100",v->strategyUi().command(c,5));break;
            case "外交":
                if(own){action("外交 / 协定",v->campaignUi().diplomacy(c));action("流言",v->campaignUi().rumor(c));}
                for(int side=0;side<world.factions.length;side++)if(side!=c.owner&&c.owner>=0&&world.alive(side))line(world.faction(side)+"\n"+world.campaign.relationLabel(c.owner,side),14,paper);break;
            case "研究":
                line("技巧点 "+world.campaign.points(c.owner),18,gold);
                if(own){action("PK能力研究",v->new AbilityUi(this,world,this::apply).research(c));action("技巧研究",v->campaignUi().research(c));action("能力 / 适性培养",v->campaignUi().study(c));}
                action("PK研究与培养进度",v->new AbilityUi(this,world,this::apply).progress(c));
                action("研究与培养进度",v->campaignUi().projects(c));break;
            case "武将":
                if(own){
                    action("仲介 / 结义婚姻",v->new EstatesUi(this,world,this::apply).mediate(c));
                    action("宝物 / 赏赐收回",v->new EstatesUi(this,world,this::apply).treasures(c));
                    action("搜索人才",v->strategyUi().command(c,1));
                    action("登用武将",v->strategyUi().command(c,2));
                    action("舌战登用",v->strategyUi().command(c,8));
                    action("褒奖武将",v->strategyUi().command(c,3));
                    action("任命太守",v->strategyUi().command(c,4));
                    action("能力 / 适性培养",v->campaignUi().study(c));
                    action("流放武将",v->campaignUi().dismiss(c));
                }
                line("可用武将 "+world.idle(c).size()+" / 驻扎 "+UiModels.officerCount(world,c.id),14,paper);
                for(World.Officer o:world.officers)if(o.cityId==c.id)iconAction(o.name+" · "+o.role.label+" · "+UiModels.status(world,o),o,v->officerDetail(o));
                if(UiModels.officerCount(world,c.id)==0)line("当前没有驻扎武将",14,muted);
                action("筛选本城武将",v->{ui.city=c.id;ui.owner=-1;ui.query="";ui.page="officers";refresh();});break;
            case "军事":
                line("治安 "+c.order+" · 气力 "+c.morale+" · 兵源 "+c.recruitReserve,13,paper);
                if(own)for(CityCommand command:militaryCommands(c))action(command.label,v->command.run.run());
                else line("仅可在己方城池下达军令",14,muted);
                if(own){action("修复城防",v->campaignUi().repair(c));action("建造军事设施",v->campaignUi().buildMilitary(c));action("军事设施管理",v->campaignUi().structures(c));}break;
            case "调动":
                if(own){action("人员调动",v->domesticUi().transfer(c));action("资源运输",v->domesticUi().transport(c));action("水陆运输",v->domesticUi().transportSea(c));}else line("仅可从己方城池派遣",14,muted);
                action("查看任务 / 在途",v->{ui.page="tasks";refresh();});break;
            default:
                Territory territory=map.territory();
                if(territory!=null){StringBuilder border=new StringBuilder(c.name+"辖区 · "+territory.size(c.id)+"格 · "+(territory.frontline(c.id)?"前线":"非前线"));
                    for(int id:territory.neighbors(c.id)){World.City adjacent=world.city(id);border.append("\n邻接 ").append(adjacent.name).append(" · ").append(world.faction(adjacent.owner));}
                    line(border.toString(),13,muted);}
                if(c.owner==world.player)action("城池托管 / 军团",v->new WorldUi(this,world,this::apply).city(c));
                line("治安 "+c.order+" · 气力 "+c.morale+" · 兵源 "+c.recruitReserve+"\n可用武将 "+world.idle(c).size()+" · 驻扎 "+UiModels.officerCount(world,c.id),14,paper);
                if(own){action("人事 / 城市治理",v->strategyUi().city(c));action("军政 / 俘虏 / 官职",v->governmentUi().city(c));}
                StringBuilder stocks=new StringBuilder("兵装库存\n");for(World.Weapon weapon:World.Weapon.values())stocks.append(weapon.label).append(" ").append(c.equipment[weapon.ordinal()]).append("  ");stocks.append("\n楼船 ").append(c.ships[0]).append(" · 斗舰 ").append(c.ships[1]);line(stocks.toString(),13,paper);
                line("月金 / 季粮基准 金 "+world.domestic.monthlyGold(c.id)+" / 粮 "+world.domestic.monthlyFood(c.id),13,muted);
                if(own)action("设施开发",v->domesticUi().build(c));
                action("展开军事命令",v->{ui.group="军事";refresh();revealPanel();});
                if(own){action("外交 / 协定",v->campaignUi().diplomacy(c));action("技巧 / 培养",v->{ui.group="研究";refresh();revealPanel();});}
        }
    }
    private static final class CityCommand {final String label;final Runnable run;CityCommand(String label,Runnable run){this.label=label;this.run=run;}}
    private ArmyUi armyUi(){return new ArmyUi(this,world,this::apply,this::selectAndFocus);}
    private List<CityCommand> militaryCommands(World.City c){return Arrays.asList(
        new CityCommand("出征",()->armyUi().deploy(c)),
        new CityCommand("快速出征（单将）",()->armyUi().quickDeploy(c)),
        new CityCommand("编队 / 水陆出征",()->armyUi().deploy(c)),
        new CityCommand("军备制造 / 攻城器械与舰船",()->armyUi().manufacture(c)),
        new CityCommand("征兵 · 金300 · 兵源 "+c.recruitReserve,()->strategyUi().command(c,6)),
        new CityCommand("训练 · 金100",()->strategyUi().command(c,7)),
        new CityCommand("生产兵装 · 查看费用",()->chooseOfficer(c,o->chooseBasicWeapon(weapon->confirm(o.name+"生产"+world.skills.produceAmount(c.id,o.id,weapon)+"份"+weapon.label+"兵装\n花费金"+world.skills.productionGold(o.id,weapon)+"、行动力10",()->apply(world.produce(c.id,o.id,weapon))))))
    );}
    private void showUnit(World.Unit u){
        if(u instanceof Domestic.Mission){showConvoy((Domestic.Mission)u);return;}
        World.Officer o=world.officer(u.officerId);panel.addView(visualHeader(o,o.name,world.faction(u.owner)+" · "+world.army.equipmentLabel(u),64));
        Diplomacy.Aid aid=world.diplomacy.aidForUnit(u.id);if(aid!=null)line("援军 · "+world.diplomacy.describe(aid),13,gold);
        if(u.owner==world.player){
            LinearLayout quick=new LinearLayout(this);
            quick.addView(button("选择目标",v->{unitCommand="march";closePanel();}),new LinearLayout.LayoutParams(0,dp(48),1));
            quick.addView(button("下一部队",v->nextUnit()),new LinearLayout.LayoutParams(0,dp(48),1));
            if(u.march!=null)quick.addView(button("停止行军",v->apply(world.marches.stop(u.id))),new LinearLayout.LayoutParams(0,dp(48),1));
            panel.addView(quick);
        }
        unitStats(u);line("统率 "+o.leadership+"  武力 "+o.war,13,paper);line("剩余移动 "+world.orders.remaining(u)+"  射程 "+world.war.range(u),13,paper);
        line("适性 "+War.rankLabel(world.army.aptitude(u))+" · 状态 "+u.status.label,14,paper);
        iconAction("主将 · "+o.name+" · 查看武将",o,v->officerDetail(o));
        for(int id:u.deputies){World.Officer deputy=world.officer(id);iconAction("副将 · "+deputy.name+" · 查看武将",deputy,v->officerDetail(deputy));}
        List<Government.Prisoner> prisoners=world.government.escorted(u.id);
        if(!prisoners.isEmpty()){
            line("随军俘虏 "+prisoners.size()+"人 · 部队入城后收押",14,gold);
            for(Government.Prisoner prisoner:prisoners){World.Officer captive=world.officer(prisoner.officerId);iconAction("随军俘虏 · "+captive.name,captive,v->officerDetail(captive));}
        }
        line("携金 "+u.gold+" · "+(world.fieldworks.project(u.id)==null?"未施工":"施工中"),13,paper);
        line("部队武力 "+world.army.war(u)+" · 智力 "+world.army.intelligence(u),13,paper);
        if(u.burning>0)line("部队燃烧 · 剩"+u.burning+"旬",14,gold);
        action("编队特技",v->warUi().skills(u));
        Districts.District district=world.districts.unit(u.id);if(district!=null)line("所属军团："+district.name()+" · 自动指挥",13,gold);
        if(u.owner==world.player){line(u.acted?"本旬已行动，攻击后不能再移动 · 可安排下旬行军":"底栏选择行军、攻击、战法；点空地或本队取消选中",14,paper);
            if(u.march!=null)line("行军 → "+world.marches.label(u.march)+(u.march.paused.isEmpty()?"\n每旬自动前进":"\n暂停："+u.march.paused),14,gold);
            if(world.fieldworks.project(u.id)!=null)action("中止施工",v->new FieldworkUi(this,world,this::apply).stop(u));
            if(!u.acted&&u.status==War.Status.NORMAL){action("设置军事设施",v->new FieldworkUi(this,world,this::apply).build(u));action("补修军事设施",v->new FieldworkUi(this,world,this::apply).repair(u));action("补充携金",v->new FieldworkUi(this,world,this::apply).fund(u));action("单挑",v->new ContestUi(this,world,this::apply).challenge(u));action("齐攻",v->warUi().joint(u));action("讨伐贼寨",v->new WorldUi(this,world,this::apply).raids(u));action("截击运输队",v->governmentUi().raid(u));action("移交兵粮",v->governmentUi().supply(u));action("部队战法详情",v->showTactics(u));
                if(u.burning>0)action("部队灭火 · 气力5",v->confirm("扑灭本部队火焰？",()->apply(world.army.extinguish(u.id))));action("部队计略",v->{moving=u.id;warUi().plots(u);});action("待命 · 恢复5气力",v->confirm("本旬待命并恢复5气力？",()->apply(world.war.waitUnit(u.id))));}
            for(Domestic.Mission convoy:world.domestic.missions)if(convoy.transport&&convoy.escortId==u.id)action("定位护送运输队",v->selectAndFocus(convoy.hex));
            action("取消部队选择",v->clearUnitSelection());}
    }
    private void showConvoy(Domestic.Mission m){
        World.Officer leader=world.officer(m.officerId);
        panel.addView(visualHeader(leader,leader.name+"运输队",world.faction(m.owner)+" · "+(world.army.water(m.hex)?"水运 · 走舸":"陆运"),64));
        line(world.domestic.status(m),14,gold);line("目的地："+world.city(m.targetCity).name+" · 当前 "+m.hex,14,paper);
        line("携兵 "+m.troops+" · 金 "+m.gold+" · 粮 "+m.food+" · 已耗粮 "+m.consumedFood,14,paper);
        for(World.Weapon weapon:World.Weapon.values())if(m.equipment[weapon.ordinal()]>0)line(weapon.label+"货物 "+m.equipment[weapon.ordinal()],13,paper);
        line("舰船货物：楼船 "+m.cargoShips[0]+" / 斗舰 "+m.cargoShips[1]+"；当前走舸不入库存",13,paper);
        for(int id:m.crew())iconAction("编队 · "+world.officer(id).name,world.officer(id),v->officerDetail(world.officer(id)));
        line("剩余移动 "+world.orders.remaining(m)+"；路线和到达时间依赖当前道路、占格和威胁",13,muted);
        if(m.owner==world.player){action("地图选择目的地 / 改道",v->{unitCommand="march";moving=m.id;closePanel();});action("停止运输",v->apply(world.marches.stop(m.id)));action("地图补给友军",v->convoySupply(m));
            action("定位目的地",v->selectAndFocus(world.city(m.targetCity).hex));
            if(world.unit(m.escortId)!=null)action("定位护送部队",v->selectAndFocus(world.unit(m.escortId).hex));
        }
    }
    private void convoySupply(Domestic.Mission m){
        List<Hex> targets=new ArrayList<>();for(World.Unit u:world.units)if(u.owner==m.owner&&u.hex.distance(m.hex)==1)targets.add(u.hex);
        pickOnMap("运输补给：选择相邻友军",m.hex,targets,h->{World.Unit target=world.unitAt(h);if(target==null)return;cancelMapPick();
            LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),dp(8),dp(16),dp(8));
            EditText[] fields=new EditText[3];String[] names={"兵","粮","金"};for(int i=0;i<3;i++){fields[i]=new EditText(this);fields[i].setInputType(android.text.InputType.TYPE_CLASS_NUMBER);fields[i].setHint(names[i]+"数量（0为不移交）");fields[i].setContentDescription("运输补给"+names[i]);form.addView(fields[i]);}
            ScrollView scroll=new ScrollView(this);scroll.addView(form);
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("补给"+world.officer(target.officerId).name).setView(scroll).setNegativeButton("取消",null).setPositiveButton("预览",(d,n)->{try{int[] amount=new int[3];for(int i=0;i<3;i++)amount[i]=fields[i].getText().toString().isEmpty()?0:Integer.parseInt(fields[i].getText().toString());confirm("移交兵"+amount[0]+"、粮"+amount[1]+"、金"+amount[2]+"；确认后运输队结束本旬行动",()->apply(world.supply.convoyTransfer(m.id,target.id,amount[0],amount[1],amount[2])));}catch(NumberFormatException e){message("数量无效","请输入有效整数");}}).show();dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        });
    }
    void officerDetail(World.Officer o){
        String stats="统率 "+o.leadership+"    武力 "+o.war+"\n智力 "+o.intelligence+"    政治 "+o.politics+"\n魅力 "+o.charm;
        if(world.contests.injury(o.id)>0)stats+="\n负伤 · 有效武力 "+world.contests.war(o)+" · 剩"+world.contests.injuryTurns(o.id)+"旬";
        Government.Rank office=world.government.office(o.id);stats+="\n功绩 "+world.government.merit(o.id)+" · 官职 "+(office==null?"未授官":office.id)+" · 统兵 "+world.government.commandLimit(o.id);
        stats+="\n适性：枪"+War.rankLabel(o.aptitude[0])+" 戟"+War.rankLabel(o.aptitude[1])+" 弩"+War.rankLabel(o.aptitude[2])+" 骑"+War.rankLabel(o.aptitude[3])+" 器"+War.rankLabel(o.aptitude[4])+" 水"+War.rankLabel(o.aptitude[5]);
        stats+="\n"+world.life.describe(o.id);
        stats+="\n特技："+Skill.label(o.skillId)+"\n\n"+world.relations.describe(o.id)+"\n\n宝物：\n"+world.treasures.describe(o.id);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(8),dp(20),dp(16));content.addView(visualHeader(o,o.name,o.role.label+" · 忠诚 "+o.loyalty,88));
        TextView description=text(stats+"\n身份："+o.role.label+" · 忠诚 "+o.loyalty+"\n\n所在地："+UiModels.location(world,o)+"\n状态："+UiModels.status(world,o),15,paper);content.addView(description);
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        AlertDialog.Builder d=new AlertDialog.Builder(this).setTitle(o.name+" · "+UiModels.faction(world,o)).setView(scroll).setNegativeButton("返回",null);
        Hex h=o.cityId>=0?world.city(o.cityId).hex:o.unitId>=0&&world.unit(o.unitId)!=null?world.unit(o.unitId).hex:null;
        if(world.government.captive(o.id))h=world.government.location(world.government.prisoner(o.id));
        for(Domestic.Mission m:world.domestic.missions)if(m.officerId==o.id)h=m.hex;
        final Hex target=h;if(target!=null)d.setPositiveButton("地图定位",(dialog,n)->selectAndFocus(target));d.show();
    }
    private interface OfficerChoice {void choose(World.Officer officer);}
    private interface WeaponChoice {void choose(World.Weapon weapon);}
    private void chooseOfficer(World.City c,OfficerChoice callback){
        List<World.Officer> options=world.idle(c);if(options.isEmpty()){message("武将不足","没有本旬可行动的在城武将");return;}
        new AlertDialog.Builder(this).setTitle("执行武将").setAdapter(GameIcon.adapter(this,world,options,o->o.name),(d,index)->callback.choose(options.get(index))).setNegativeButton("取消",null).show();
    }
    private void chooseBasicWeapon(WeaponChoice callback){new AlertDialog.Builder(this).setTitle("生产基础兵装").setAdapter(GameIcon.adapter(this,world,Arrays.asList(World.Weapon.values()).subList(0,4),x->x.label),(d,i)->callback.choose(World.Weapon.values()[i])).setNegativeButton("取消",null).show();}
    private void chooseWeapon(WeaponChoice callback){new AlertDialog.Builder(this).setTitle("选择兵种").setAdapter(GameIcon.adapter(this,world,Arrays.asList(World.Weapon.values()),x->x.label),(d,index)->callback.choose(World.Weapon.values()[index])).setNegativeButton("取消",null).show();}
    private void revealPanel(){panelScroll.post(()->panelScroll.scrollTo(0,0));}
    void selectAndFocus(Hex h){if(h==null)return;pendingMarch=null;unitCommand="select";World.Unit unit=world.unitAt(h);moving=unit!=null&&unit.owner==world.player?unit.id:-1;selected=h;ui.page="map";ui.panelVisible=true;ui.panelExpanded=false;ui.group="概览";refresh();map.post(()->map.focus(h));revealPanel();}
    private GovernmentUi governmentUi(){return new GovernmentUi(this,world,this::apply);}
    private StrategyUi strategyUi(){return new StrategyUi(this,world,this::apply);}
    private CampaignUi campaignUi(){return new CampaignUi(this,world,this::apply);}
    private WarUi warUi(){return new WarUi(this,world,this::apply);}
    DomesticUi domesticUi(){return new DomesticUi(this,world,this::apply,this::selectAndFocus);}
    private void showMenu(){
        line("军政菜单",22,gold);
        action("屏幕方向 / 横竖屏",v->showOrientationPicker());
        action("地图视图与操作",v->showMapTools());
        action("生卒与继承",v->new LifecycleUi(this,world,this::apply).menu());
        action("军团与天下",v->new WorldUi(this,world,this::apply).menu());
        action("PK编辑 / 新武将",v->new EditorUi(this,world,this::apply).menu());
        if(world.editor.edited())line("当前局面已使用PK编辑",13,muted);
        if(world.life.pending())action("继续君主继承",v->{ui.page="map";refresh();});
        if(world.contests.busy())action("继续当前对局",v->{ui.page="map";refresh();});
        if(!world.contests.lastResult().isEmpty())action("最近对局结果",v->message("对局结果",world.contests.lastResult()));action("保存局面（3个槽位）",v->saveSlots(false));action("读取存档",v->saveSlots(true));action("导出当前存档",v->exportSave());action("导入存档文件",v->importSave());action("导入剧本文件",v->{if(!aiRunning)startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT_SCENARIO);});
        action("本旬结算摘要",v->message("旬结算摘要",ui.summary.isEmpty()?"结束一旬后将在这里显示结算摘要。":ui.summary));
        action("全国资料 / 核验目录",v->{ui.page="content";refresh();});action("势力一览",v->{ui.page="factions";refresh();});
        action("战报",v->message("战报",String.join("\n",world.log)));
        action("新游戏 / 选择势力",v->scenarioPicker());action("版本与范围",v->message("0.24 · 领地与军团AI","势力范围 / 城关港辖区着色、前线标记与图例；城池快捷托管、后方批量编团；接敌走位与军团预算调度改进。\n设施直接最高级；兵钱粮滑块与精确输入；战法、计略、内政和野战建设地图点选。\n八类地形纹理；新增42城670将、18城180将、12城120将三种自制开局。\n援军请求、势力劝降、交换俘虏；八项特技恢复培养。\n16位名将原创头像、其余武将稳定默认头像；选将、兵种与设施图标统一；施工模型与城防/兵力条优化。\n内政设施耐久与攻击；固定部队指令栏；显式行军、取消选择、行动范围。\n存档v18，兼容v1—v17。\n全国原版格点、官方完整开局、全事件、全特技交互与精确公式仍有缺口。"));
    }
    private void scenarioPicker(){
        try {List<World> scenarios=ScenarioCatalog.all();String[] labels=new String[scenarios.size()];for(int i=0;i<labels.length;i++){World w=scenarios.get(i);labels[i]=w.scenarioName+" · "+w.cities.size()+"城 / "+w.officers.size()+"将 / "+w.factions.length+"势力";}
            new AlertDialog.Builder(this).setTitle("选择剧本 · 原创测试沙盘").setItems(labels,(dialog,index)->{World template=scenarios.get(index);new AlertDialog.Builder(this).setTitle(template.scenarioName+" · 选择势力").setItems(template.factions,(d,side)->confirm(openingInfo(template,side)+"\n\n以"+template.faction(side)+"开始新局？当前自动存档将更新，手动存档保留。",()->startScenario(template.scenarioId,side))).setNegativeButton("返回",(d,n)->scenarioPicker()).show();}).setNegativeButton("取消",null).show();
        }catch(IOException e){showError("剧本读取失败");}
    }
    private String openingInfo(World w,int side){
        StringBuilder b=new StringBuilder(w.date()+" · "+w.width+"×"+w.height+"格\n");int people=0;for(World.Officer o:w.officers)if(o.owner==side)people++;
        b.append(people).append("名武将 · 领地：");for(World.City c:w.cities)if(c.owner==side)b.append(c.name).append(" ");
        b.append(w.dataSource.equals("community-reference")?"\n能力/适性来自公开资料，地图/领地/资源为原创演练；特技、性格和已解析关系依剧本载入。群英类自制沙盘为跨时代配置，不按生卒年退场。":"\n原创测试地图、资源和武将数值；非官方历史开局。");return b.toString();
    }
    private void startScenario(String id,int player){try{world=ScenarioCatalog.load(id,player,System.nanoTime());ui.summary="";ui.city=-1;ui.owner=-1;ui.query="";ui.cityQuery="";ui.cityOwner=-1;ui.taskQuery="";ui.taskType=0;selectAndFocus(world.home().hex);closePanel();save("auto",false);}catch(IOException e){showError("无法开始剧本");}}
    private String slotName(int index){return index==0?"manual":"manual"+(index+1);}
    private boolean present(AtomicFile f){return f.getBaseFile().exists()||new File(f.getBaseFile()+".bak").exists();}
    private void saveSlots(boolean loading){
        String[] labels=new String[3];boolean[] exists=new boolean[3];
        for(int i=0;i<labels.length;i++) {
            AtomicFile entry=file(slotName(i));exists[i]=present(entry);labels[i]="槽位 "+(i+1)+" · 空";
            if(exists[i])try{World saved=readSave(entry);long stamp=entry.getBaseFile().lastModified();labels[i]="槽位 "+(i+1)+" · "+saved.scenarioName+" · "+saved.faction(saved.player)+"\n"+saved.date()+" · 第 "+(saved.turn+1)+" 旬\n最后保存 "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(stamp));}
            catch(IOException e){labels[i]="槽位 "+(i+1)+" · 文件损坏 / 不兼容";}
        }
        new AlertDialog.Builder(this).setTitle(loading?"读取存档":"保存局面").setItems(labels,(d,index)->{
            String slot=slotName(index);
            if(loading){if(!exists[index]){message("空槽位","此槽位还没有存档，请选择其他槽位。");return;}confirm("读取以下存档并替换当前局面？\n\n"+labels[index],()->loadSlot(slot));}
            else if(exists[index])confirm("覆盖以下存档？原存档将被替换。\n\n"+labels[index]+"\n\n新局面："+world.faction(world.player)+" · "+world.date(),()->save(slot,true));
            else save(slot,true);
        }).setNegativeButton("取消",null).show();
    }
    private void showError(String title){message(title,"操作未完成，当前局面未改变。请检查存档是否损坏、版本是否兼容，以及设备存储空间后重试。");}
    private void confirmTurn(){
        if(aiRunning||world.gameOver()||world.commandsBlocked())return;int idle=0;for(World.City c:world.cities)if(c.owner==world.player)idle+=world.idle(c).size();
        confirm("结束 "+world.date()+"？\n还有 "+idle+" 名闲置武将、"+world.actionPoints[world.player]+" 点行动力。\n将执行电脑行动，推进建设、调动、运输与自动行军，并自动保存。",this::advanceTurn);
    }
    private void advanceTurn(){if(aiRunning||world.gameOver()||world.commandsBlocked())return;aiRunning=true;turnWork=new TurnWork(world);turnWork.observe(this::finishTurn);refresh();turnWork.start();}
    private void finishTurn(){
        if(turnWork==null||!turnWork.done||isFinishing()||isDestroyed())return;TurnWork completed=turnWork;completed.observe(null);turnWork=null;aiRunning=false;
        if(completed.error!=null){refresh();showError("回合结算失败");return;}
        world=completed.after;if(world.life.pending()){ui.page="map";ui.panelVisible=true;}ui.summary=completed.summary;pendingMarch=null;if(world.unit(moving)!=null)selected=world.unit(moving).hex;else moving=-1;refresh();save("auto",false);message("旬结算摘要",ui.summary);
    }
    @Override public Object onRetainNonConfigurationInstance(){if(turnWork!=null)turnWork.observe(null);return turnWork;}
    @Override protected void onDestroy(){if(turnWork!=null)turnWork.observe(null);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);ui.write(state);if(selected!=null){state.putInt("selectedQ",selected.q);state.putInt("selectedR",selected.r);}state.putInt("moving",moving);state.putString("unitCommand",unitCommand);if(pendingMarch!=null&&pendingMarch.target!=null){state.putInt("routeQ",pendingMarch.target.q);state.putInt("routeR",pendingMarch.target.r);}map.saveCamera(state);save("auto",false);}
    private AtomicFile file(String slot){return new AtomicFile(new File(getFilesDir(),slot+".sg11"));}
    private void save(String slot,boolean announce){
        AtomicFile f=file(slot);FileOutputStream out=null;try{byte[] bytes=SaveCodec.encode(world);out=f.startWrite();out.write(bytes);f.finishWrite(out);if(announce)Toast.makeText(this,"局面已保存",Toast.LENGTH_SHORT).show();}
        catch(IOException e){if(out!=null)f.failWrite(out);Toast.makeText(this,"保存失败，请检查设备存储空间后重试",Toast.LENGTH_LONG).show();}
    }
    private World readSave(AtomicFile f)throws IOException {try(FileInputStream in=f.openRead()){return SaveCodec.read(in);}}
    private static final int EXPORT_SAVE=911, IMPORT_SAVE=912, EXPORT_OFFICER=913, IMPORT_OFFICER=914, IMPORT_SCENARIO=915;
    void importOfficerTemplate(){if(!aiRunning)startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT_OFFICER);}
    void exportOfficerTemplate(Editor.Template template){
        if(aiRunning)return;
        AtomicFile pending=new AtomicFile(new File(getFilesDir(),"pending-officer-export.sgof"));FileOutputStream out=null;
        try{byte[] bytes=OfficerTemplateCodec.encode(template);out=pending.startWrite();out.write(bytes);pending.finishWrite(out);
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,"sanguo11-officer.sgof"),EXPORT_OFFICER);
        }catch(IOException e){if(out!=null)pending.failWrite(out);showError("模板导出失败");}
    }
    private void exportSave(){
        if(aiRunning)return;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,"sanguo11-"+world.scenarioId+"-turn"+world.turn+".sg11");
        startActivityForResult(intent,EXPORT_SAVE);
    }
    private void importSave(){
        if(aiRunning)return;
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT_SAVE);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null||aiRunning)return;
        try {
            if(request==IMPORT_SCENARIO){
                final World imported;try(InputStream in=getContentResolver().openInputStream(data.getData())){imported=ScenarioData.read(in,0);}
                new AlertDialog.Builder(this).setTitle("导入剧本 · 选择势力").setItems(imported.factions,(dialog,n)->confirm("开始“"+imported.scenarioName+"”？\n"+imported.faction(n)+" · "+(imported.sourceMapWidth>0?imported.sourceMapWidth:imported.width)+"×"+imported.height+"格 · "+imported.cities.size()+"据点 · "+imported.officers.size()+"武将\n将替换当前局面及自动存档，手动槽位保留。",()->{
                    if(aiRunning)return;imported.player=n;imported.active=n;world=imported;ui.summary="";ui.city=-1;ui.owner=-1;ui.query="";ui.cityQuery="";ui.cityOwner=-1;ui.taskQuery="";ui.taskType=0;pendingMarch=null;moving=-1;selectAndFocus(world.home().hex);save("auto",false);
                })).setNegativeButton("取消",null).show();
            }else if(request==IMPORT_OFFICER){
                Editor.Template template;try(InputStream in=getContentResolver().openInputStream(data.getData())){template=OfficerTemplateCodec.read(in);}
                new AlertDialog.Builder(this).setTitle("导入新武将模板").setMessage(template.name+" · 统"+template.stat(0)+" / 武"+template.stat(1)+" / 智"+template.stat(2)+" / 政"+template.stat(3)+" / 魅"+template.stat(4)+"\n保存到模板列表后，可选择登场据点与身份。")
                    .setPositiveButton("保存模板",(dialog,n)->{try{EditorUi.store(this,template);message("模板已保存",template.name+"已加入新武将列表");}catch(IOException e){showError("模板保存失败");}}).setNegativeButton("取消",null).show();
            }else if(request==EXPORT_OFFICER){
                Editor.Template template;AtomicFile pending=new AtomicFile(new File(getFilesDir(),"pending-officer-export.sgof"));
                try(InputStream in=pending.openRead()){template=OfficerTemplateCodec.read(in);}
                try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")){if(out==null)throw new IOException("无法写入模板");out.write(OfficerTemplateCodec.encode(template));}
                pending.delete();message("模板已导出",template.name+"可导入其他安装包或存留备用");
            }else if(request==EXPORT_SAVE){
                byte[] bytes=SaveCodec.encode(world);
                try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")){
                    if(out==null)throw new IOException("无法写入文件");out.write(bytes);
                }
                message("存档已导出","可通过“导入存档文件”在另一安装包中恢复此局面。");
            }else if(request==IMPORT_SAVE){
                final World imported;
                try(InputStream in=getContentResolver().openInputStream(data.getData())){imported=SaveCodec.read(in);}
                if(imported.active!=imported.player)throw new IOException("非玩家回合存档");
                confirm("导入“"+imported.scenarioName+"”？\n"+imported.faction(imported.player)+" · "+imported.date()+"\n将替换当前局面和自动存档，手动槽位保留。",()->{
                    if(aiRunning)return;
                    world=imported;ui.summary="";ui.city=-1;ui.owner=-1;ui.query="";ui.cityQuery="";ui.cityOwner=-1;ui.taskQuery="";ui.taskType=0;
                    selectAndFocus(world.home().hex);save("auto",false);
                });
            }
        }catch(IOException|SecurityException e){showError(request==EXPORT_SAVE?"导出失败":"导入失败");}
    }
    private void loadSlot(String slot){try{World restored=readSave(file(slot));world=restored;ui.summary="";ui.city=-1;ui.owner=-1;ui.query="";ui.cityQuery="";ui.cityOwner=-1;ui.taskQuery="";ui.taskType=0;selectAndFocus(world.home().hex);save("auto",false);}catch(IOException e){showError("读取失败");}}
    @Override protected void onPause(){super.onPause();if(world!=null)save("auto",false);}
    @Override public void onBackPressed(){if(aiRunning)return;if(mapPick!=null){cancelMapPick();return;}if(pendingMarch!=null){pendingMarch=null;unitCommand="select";refresh();return;}if(!unitCommand.equals("select")){unitCommand="select";refresh();return;}if(moving>=0&&ui.page.equals("map")){clearUnitSelection();return;}if(!ui.page.equals("map")){closePanel();}else if(ui.panelVisible){closePanel();}else confirm("退出游戏？当前局面将自动保存。",this::finish);}
}

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
    private boolean unreadableAutosave;
    private MapHost map;
    private boolean nextScenario3D;
    boolean current3D(){return map!=null&&map.is3D();}
    void setNextScenario3D(boolean value){nextScenario3D=value;}
    private LinearLayout panel,root,commandDock,panelShell,primaryActions;
    private FrameLayout body;
    private World navigatorWorld;
    private long navigatorRevision=Long.MIN_VALUE;
    private int navigatorTurn=-1,navigatorPlayer=-1;
    private String panelIdentity="";
    private MarchOrders.Plan pendingMarch;
    private FrameLayout panelHost;
    private ScrollView panelScroll;
    private TextView title,panelTitle,battleBanner,actionPointsBadge,mapRevisionNotice,dateBanner;
    private CriticalFlash criticalFlash;
    private long lastTurnWallMillis,lastTurnComputeMillis;
    private String lastBattleReport="";
    private World battleReportWorld;
    private Button selectionButton,expandPanel,closePanel,returnList,territoryToggle,gridToggle;
    private AlertDialog navigationDialog;
    private AlertDialog confirmationDialog;
    private Hex selected;
    private int moving=-1;
    private String unitCommand="select";
    private Set<Hex> pickTargets=Collections.emptySet();
    private java.util.function.Consumer<Hex> mapPick;
    private String pickTitle="";
    private Displacement.Preview tacticPreview;
    private Runnable tacticConfirmation;
    private Hex reportLocation;
    private Bundle reportReturn;
    private java.util.function.Function<Hex,String> pickError;
    private Button previousReady,nextReady;
    private TextView turnBanner,turnProgress;
    private LinearLayout quickCityStrip,quickUnitStrip;
    private boolean aiRunning;
    private Button nextTurn;
    private final ClientState ui=new ClientState();
    private TurnWork turnWork;
    private TurnPlayback playback;
    private final Map<String,Button> navigation=new LinkedHashMap<>();
    final int ink=UiTheme.INK,paper=UiTheme.TEXT,gold=UiTheme.JADE,muted=UiTheme.MUTED;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);boolean coldStart=state==null;ui.read(state);
        applyOrientationPreference();
        String restoreError=null;boolean restored=false;
        AtomicFile autosave=file("auto");
        if(present(autosave)) {
            try{world=readSave(autosave);restored=true;}catch(IOException e){unreadableAutosave=true;restoreError="自动存档无法读取："+e.getMessage()+"。原文件已保留；请选择手动存档、导入文件或新建游戏。";}
        }
        if(state==null&&restored){state=readClientState();ui.read(state);}
        turnWork=(TurnWork)getLastNonConfigurationInstance();
        if(turnWork!=null){world=turnWork.before;aiRunning=true;}
        if(world==null){showStartScreen(restoreError);return;}
        buildGameUi(state,restored,coldStart);
    }
    private void showStartScreen(String error){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(40),dp(24),dp(40));root.setBackgroundColor(ink);
        TextView heading=text("三国志 · 掌上战略",25,gold);root.addView(heading);
        TextView detail=text(error==null?"开始一个年代，或继续已有存档。":error,16,paper);detail.setPadding(0,dp(24),0,dp(24));root.addView(detail);
        root.addView(button("新建游戏 · 选择剧本",v->scenarioPicker()));
        root.addView(button("武将自定义 · 模板与投放",v->startActivity(new Intent(this,CustomOfficerActivity.class))));
        root.addView(button("读取手动存档",v->saveSlots(true)));
        root.addView(button("导入存档文件",v->importSave()));
        root.addView(button("地图编辑器 · 自定义地图",v->new MapEditorUi(this).show()));
        if(error!=null)root.addView(button("重试自动存档",v->{try{World loaded=readSave(file("auto"));unreadableAutosave=false;world=loaded;buildGameUi(null,true,true);}catch(IOException e){showError("自动存档仍无法读取，原文件保留");}}));
        ScrollView startScroll=new ScrollView(this);startScroll.setFillViewport(true);startScroll.setBackgroundColor(ink);startScroll.addView(root,new ScrollView.LayoutParams(-1,-2));setContentView(startScroll);
    }
    /** Called only after a user-confirmed replacement; unreadable bytes remain recoverable. */
    private boolean activateWorld(World next){
        if(unreadableAutosave){
            File backup=new File(getFilesDir(),"auto-unreadable-"+System.currentTimeMillis()+".sg11");
            try(InputStream in=file("auto").openRead();OutputStream out=new FileOutputStream(backup)){byte[] data=new byte[8192];int n;while((n=in.read(data))!=-1)out.write(data,0,n);}
            catch(IOException e){showError("无法保留损坏存档，本次没有替换局面");return false;}
            unreadableAutosave=false;
        }
        world=next;ui.read(new Bundle());pendingMarch=null;moving=-1;unitCommand="select";mapPick=null;pickTargets=Collections.emptySet();return true;
    }
    private void buildGameUi(Bundle state,boolean restored,boolean coldStart){
        world.reports.prepare();
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(ink);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(8),0,dp(8),0);
        header.setMinimumHeight(dp(56));header.setTag("hud.compact");
        actionPointsBadge=text("",12,paper);actionPointsBadge.setGravity(Gravity.CENTER);actionPointsBadge.setMaxLines(2);
        actionPointsBadge.setBackground(UiTheme.surface(this,0xff2a5146,0xff17352e,10));
        actionPointsBadge.setOnClickListener(v->message("行动力",world.faction(world.player)+" · 当前行动力 "+world.actionPoints[world.player]+" 点"));
        LinearLayout.LayoutParams apParams=new LinearLayout.LayoutParams(dp(48),dp(44));apParams.setMargins(0,dp(4),dp(6),dp(4));header.addView(actionPointsBadge,apParams);
        LinearLayout headline=new LinearLayout(this);headline.setOrientation(LinearLayout.VERTICAL);headline.setGravity(Gravity.CENTER_VERTICAL);
        dateBanner=text("",16,gold);dateBanner.setTag("hud.date");dateBanner.setSingleLine(true);dateBanner.setEllipsize(null);dateBanner.setMinHeight(dp(24));
        if(android.os.Build.VERSION.SDK_INT>=26)dateBanner.setAutoSizeTextTypeUniformWithConfiguration(11,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        headline.addView(dateBanner,new LinearLayout.LayoutParams(-1,dp(27)));
        title=text("",11,muted);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setOnClickListener(v->message("当前军情",world.scenarioName+" · "+world.faction(world.player)+"\n"+world.date()+" · 进行中任务 "+taskCount()));
        headline.addView(title,new LinearLayout.LayoutParams(-1,dp(21)));header.addView(headline,new LinearLayout.LayoutParams(0,dp(52),1));
        Button reportsButton=button("战报",v->new BattleReportUi(this,world).show());reportsButton.setTag("reports.entry");reportsButton.setTextSize(11);reportsButton.setContentDescription("战报中心：近三个月全部势力交互结果");header.addView(reportsButton,new LinearLayout.LayoutParams(dp(44),dp(52)));
        territoryToggle=CompactButtons.create(this);territoryToggle.setTextSize(11);territoryToggle.setText("势力");
        territoryToggle.setOnClickListener(v->setTerritoryMode(map.territoryMode()==0?1:0));territoryToggle.setOnLongClickListener(v->{showTerritoryPicker();return true;});
        header.addView(territoryToggle,new LinearLayout.LayoutParams(dp(44),dp(52)));
        gridToggle=button("网格",v->{map.setGridShown(!map.gridShown());refreshGridToggle();});gridToggle.setTag("map.grid.toggle");gridToggle.setTextSize(11);
        header.addView(gridToggle,new LinearLayout.LayoutParams(dp(44),dp(52)));
        Button tools=button("视图",v->showMapTools());tools.setTextSize(11);tools.setContentDescription("地图工具 · 全图、定位、导航图和屏幕方向");
        header.addView(tools,new LinearLayout.LayoutParams(dp(44),dp(52)));header.setBackground(UiTheme.surface(this,0xff1b2b33,0xff101b24,0));root.addView(header,new LinearLayout.LayoutParams(-1,dp(56)));
        mapRevisionNotice=text("",11,gold);mapRevisionNotice.setTag("map.revision.notice");mapRevisionNotice.setPadding(dp(12),dp(3),dp(12),dp(3));
        mapRevisionNotice.setOnClickListener(v->message("地图存档版本",NationalMap.compatibilityNotice(world)));root.addView(mapRevisionNotice);
        body=new FrameLayout(this);if(map!=null)map.release();map=new MapHost(this,this::onTile);body.addView(map,new FrameLayout.LayoutParams(-1,-1));map.setUnitDrop(this::dropUnit);map.setTerritoryMode(getPreferences(MODE_PRIVATE).getInt("territoryMode",0));refreshTerritoryToggle();refreshGridToggle();
        panelShell=new LinearLayout(this);panelShell.setOrientation(LinearLayout.VERTICAL);UiTheme.panel(panelShell);
        panelShell.setVisibility(View.GONE);body.addView(panelShell);
        LinearLayout panelHeader=new LinearLayout(this);panelHeader.setPadding(dp(12),0,dp(4),0);panelHeader.setGravity(Gravity.CENTER_VERTICAL);
        returnList=button("列表",v->returnToCities());returnList.setContentDescription("返回全国列表");panelHeader.addView(returnList,new LinearLayout.LayoutParams(dp(48),dp(48)));
        panelTitle=text("指令",14,gold);panelTitle.setMaxLines(1);panelTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panelHeader.addView(panelTitle,new LinearLayout.LayoutParams(0,dp(48),1));
        expandPanel=button("展开",v->{ui.panelExpanded=!ui.panelExpanded;layoutPanels();});expandPanel.setContentDescription("展开或缩小操作面板");
        panelHeader.addView(expandPanel,new LinearLayout.LayoutParams(dp(56),dp(48)));
        closePanel=button("收起",v->closePanel());closePanel.setContentDescription("收起操作面板，返回大地图");
        panelHeader.addView(closePanel,new LinearLayout.LayoutParams(dp(56),dp(48)));panelShell.addView(panelHeader);
        primaryActions=new LinearLayout(this);primaryActions.setPadding(dp(8),0,dp(8),dp(4));primaryActions.setBackgroundColor(0x202f776a);primaryActions.setVisibility(View.GONE);
        panelShell.addView(primaryActions,new LinearLayout.LayoutParams(-1,-2));
        panelHost=new FrameLayout(this);panelShell.addView(panelHost,new LinearLayout.LayoutParams(-1,0,1));
        panelScroll=new ScrollView(this);panelScroll.setFillViewport(true);panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(12),dp(4),dp(12),dp(10));panelScroll.addView(panel);
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        battleBanner=text("",13,paper);battleBanner.setMaxLines(2);battleBanner.setEllipsize(android.text.TextUtils.TruncateAt.END);
        battleBanner.setPadding(dp(12),dp(6),dp(12),dp(6));battleBanner.setBackgroundColor(0xff30443b);battleBanner.setVisibility(View.GONE);
        battleBanner.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        battleBanner.setOnClickListener(v->{
            AlertDialog.Builder report=new AlertDialog.Builder(this).setTitle(reportLocation==null?"操作结果":"战斗结果").setMessage(lastBattleReport)
                .setPositiveButton("返回",null).setNeutralButton("收起战果",(d,n)->battleBanner.setVisibility(View.GONE));
            if(reportLocation!=null&&battleReportWorld==world)report.setNegativeButton("定位发生地点",(d,n)->locateBattleReport());
            report.show();
        });
        root.addView(battleBanner,new LinearLayout.LayoutParams(-1,-2));
        turnBanner=text("",13,paper);turnBanner.setPadding(dp(12),dp(5),dp(12),dp(5));turnBanner.setMaxLines(2);turnBanner.setBackgroundColor(0xff243e4b);turnBanner.setVisibility(View.GONE);turnBanner.setContentDescription("查看本旬结算摘要");turnBanner.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);turnBanner.setOnClickListener(v->showTurnReport());root.addView(turnBanner,new LinearLayout.LayoutParams(-1,-2));
        body.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)layoutPanels();});
        commandDock=new LinearLayout(this);commandDock.setPadding(dp(8),dp(4),dp(8),dp(4));UiTheme.panel(commandDock);body.addView(commandDock,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        commandDock.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(b-t!=ob-ot)layoutPanels();});
        turnProgress=text("",12,gold);turnProgress.setPadding(dp(12),dp(3),dp(12),dp(3));turnProgress.setBackgroundColor(0xff1c3340);turnProgress.setVisibility(View.GONE);turnProgress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(turnProgress,new LinearLayout.LayoutParams(-1,-2));
        quickCityStrip=new LinearLayout(this);quickCityStrip.setOrientation(LinearLayout.HORIZONTAL);root.addView(quickNavigatorRow("城池",quickCityStrip),new LinearLayout.LayoutParams(-1,dp(42)));
        quickUnitStrip=new LinearLayout(this);quickUnitStrip.setOrientation(LinearLayout.HORIZONTAL);root.addView(quickNavigatorRow("部队",quickUnitStrip),new LinearLayout.LayoutParams(-1,dp(42)));
        LinearLayout bottom=new LinearLayout(this);bottom.setPadding(dp(6),0,dp(6),0);bottom.setGravity(Gravity.CENTER_VERTICAL);
        Button functions=button("功能",v->showNavigation());functions.setContentDescription("打开功能导航 · 城市、武将、任务、菜单");
        UiTheme.icon(functions,"menu");bottom.addView(functions,new LinearLayout.LayoutParams(dp(72),dp(48)));
        selectionButton=button("点选城池",v->{ui.page="map";ui.panelVisible=!ui.panelVisible;refresh();revealPanel();});
        selectionButton.setMaxLines(1);selectionButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
        bottom.addView(selectionButton,new LinearLayout.LayoutParams(0,dp(48),1));
        previousReady=button("‹",v->cycleReady(-1));previousReady.setContentDescription("上一个待行动部队");
        nextReady=button("›",v->cycleReady(1));nextReady.setContentDescription("下一个待行动部队");
        bottom.addView(previousReady,new LinearLayout.LayoutParams(dp(48),dp(48)));bottom.addView(nextReady,new LinearLayout.LayoutParams(dp(48),dp(48)));
        nextTurn=button("下一旬  →",v->confirmTurn());nextTurn.setSelected(true);bottom.setBackground(UiTheme.surface(this,0xff1e3035,0xff142128,0));bottom.addView(nextTurn,new LinearLayout.LayoutParams(dp(104),dp(48)));root.addView(bottom);
        setContentView(root);root.requestApplyInsets();selected=world.home()==null?null:world.home().hex;
        if(state!=null){Hex h=new Hex(state.getInt("selectedQ",-1),state.getInt("selectedR",-1));selected=world.inside(h)?h:null;moving=state.getInt("moving",-1);}
        if(state!=null)unitCommand=state.getString("unitCommand","select");
        if(state!=null&&state.containsKey("routeQ")&&world.unit(moving)!=null)pendingMarch=state.getBoolean("routeMove")?world.marches.previewMove(moving,new Hex(state.getInt("routeQ"),state.getInt("routeR"))):state.getInt("routeCity",-1)>=0?world.marches.previewCity(moving,state.getInt("routeCity")):world.marches.preview(moving,new Hex(state.getInt("routeQ"),state.getInt("routeR")));
        refresh();if(state!=null){map.restoreCamera(state);if(!aiRunning&&!ui.summary.isEmpty()){turnBanner.setText("旬结算完成 · 点此查看重要变化与待处理");turnBanner.setVisibility(View.VISIBLE);}}
        if(turnWork!=null)turnWork.observe(this::finishTurn);
        if(state!=null&&!aiRunning&&ui.formDraft.getBoolean("open"))root.post(this::restoreFormDraft);
        if(restored&&coldStart)Toast.makeText(this,"已恢复自动存档 · "+world.date(),Toast.LENGTH_SHORT).show();
    }
    private boolean portrait(){return getResources().getConfiguration().orientation!=Configuration.ORIENTATION_LANDSCAPE;}
    private void layoutPanels(){
        if(body==null||body.getWidth()==0||body.getHeight()==0)return;
        boolean vertical=portrait();boolean visible=Boolean.TRUE.equals(panelShell.getTag());
        int dock=commandDock!=null&&commandDock.getVisibility()==View.VISIBLE?commandDock.getHeight():0;
        int available=Math.max(dp(80),body.getHeight()-dock-dp(16));
        int height=vertical?Math.min(available,Math.round(body.getHeight()*(ui.page.equals("map")?(ui.panelExpanded?.80f:.43f):(ui.panelExpanded?.98f:.90f)))):available;
        int width=vertical?body.getWidth()-dp(16):Math.min(dp(ui.panelExpanded?460:328),Math.round(body.getWidth()*(ui.panelExpanded?.64f:.42f)));
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(width,height,vertical?Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL:Gravity.TOP|Gravity.RIGHT);
        params.setMargins(dp(8),dp(8),dp(8),dock+dp(8));
        android.view.ViewGroup.LayoutParams current=panelShell.getLayoutParams();
        if(!(current instanceof FrameLayout.LayoutParams)||current.width!=width||current.height!=height||((FrameLayout.LayoutParams)current).gravity!=params.gravity||((FrameLayout.LayoutParams)current).bottomMargin!=params.bottomMargin)panelShell.setLayoutParams(params);
        if(commandDock!=null){FrameLayout.LayoutParams d=(FrameLayout.LayoutParams)commandDock.getLayoutParams();if(d.leftMargin!=dp(8)){d.setMargins(dp(8),0,dp(8),dp(4));commandDock.setLayoutParams(d);}}
        map.setPanelOcclusion(visible&&!vertical?width+dp(16):0,dock+(visible&&vertical?height+dp(16):0));
        expandPanel.setText(ui.panelExpanded?"缩小":"展开");
    }
    private void closePanel(){ui.panelVisible=false;ui.panelExpanded=false;ui.page="map";refresh();}
    private View quickNavigatorRow(String label,LinearLayout strip){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(6),0,dp(6),0);row.setBackgroundColor(0xff101d28);
        TextView name=text(label,12,gold);name.setGravity(Gravity.CENTER);name.setContentDescription("查看全部"+label);name.setOnClickListener(v->openRealmPage(label.equals("部队")?"units":"cities",-1));row.addView(name,new LinearLayout.LayoutParams(dp(42),-1));
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);scroll.setFillViewport(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.addView(strip,new HorizontalScrollView.LayoutParams(-2,-1));row.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));return row;
    }
    private void refreshQuickNavigator(){
        if(quickCityStrip==null||quickUnitStrip==null||world==null)return;
        View cityRow=(View)quickCityStrip.getParent().getParent();View unitRow=(View)quickUnitStrip.getParent().getParent();
        boolean mapContext="map".equals(ui.page);
        if(!mapContext){
            cityRow.setVisibility(View.GONE);unitRow.setVisibility(View.GONE);return;
        }
        if(navigatorWorld==world&&navigatorRevision==world.commandRevision()&&navigatorTurn==world.turn&&navigatorPlayer==world.player){
            cityRow.setVisibility(quickCityStrip.getChildCount()==0?View.GONE:View.VISIBLE);
            unitRow.setVisibility(quickUnitStrip.getChildCount()==0?View.GONE:View.VISIBLE);return;
        }
        navigatorWorld=world;navigatorRevision=world.commandRevision();navigatorTurn=world.turn;navigatorPlayer=world.player;
        quickCityStrip.removeAllViews();quickUnitStrip.removeAllViews();
        List<World.City> ownCities=new ArrayList<>();for(World.City c:world.cities)if(c.owner==world.player)ownCities.add(c);ownCities.sort(Comparator.comparingInt(c->c.id));
        for(World.City c:ownCities){Button b=button(c.name+" · "+(c.troops/1000)+"k",v->selectObject(c.hex,-2,true));b.setTextSize(11);b.setContentDescription("定位己方据点 "+c.name);quickCityStrip.addView(b,new LinearLayout.LayoutParams(dp(104),-1));}
        List<World.Unit> ownUnits=new ArrayList<>();for(World.Unit u:world.fieldUnits())if(u.owner==world.player)ownUnits.add(u);ownUnits.sort(Comparator.comparingInt(u->u.id));
        for(World.Unit u:ownUnits){World.Officer o=world.officer(u.officerId);String n=o==null?("部队"+u.id):o.name;Button b=button(n+" · "+u.troops,v->selectObject(u.hex,u.id,true));b.setTextSize(11);b.setContentDescription("定位己方部队 "+n);quickUnitStrip.addView(b,new LinearLayout.LayoutParams(dp(116),-1));}
        cityRow.setVisibility(ownCities.isEmpty()?View.GONE:View.VISIBLE);unitRow.setVisibility(ownUnits.isEmpty()?View.GONE:View.VISIBLE);
    }
    private void refreshTurnProgress(){
        if(turnProgress==null)return;
        turnProgress.setOnClickListener(v->showPlaybackControls());
        if(aiRunning&&turnWork!=null){turnProgress.setText(turnWork.status());turnProgress.setVisibility(View.VISIBLE);turnProgress.removeCallbacks(turnProgressTicker);turnProgress.postDelayed(turnProgressTicker,120);}
        else {turnProgress.removeCallbacks(turnProgressTicker);turnProgress.setVisibility(View.GONE);}
    }
    private final Runnable turnProgressTicker=()->refreshTurnProgress();
    private void showNavigation(){
        if(mapPick!=null)cancelMapPick();
        if(navigationDialog!=null&&navigationDialog.isShowing())return;
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(12),dp(4),dp(12),dp(12));
        navigation.clear();String[] keys={"map","factions","units","cities","ports","gates","officers","facilities","tasks","menu"},labels={"地图","势力总览","全部部队","城市","港口","关卡","武将","设施","任务","菜单"};
        ScrollView scroll=new ScrollView(this);scroll.addView(list);
        navigationDialog=new AlertDialog.Builder(this).setTitle("功能导航").setView(scroll).setNegativeButton("返回",null).create();
        int pendingTasks=taskCount();
        for(int i=0;i<keys.length;i++){
            final String page=keys[i];String label=labels[i]+(page.equals("tasks")&&pendingTasks>0?" "+pendingTasks:"");
            Button item=button(label,v->{navigationDialog.dismiss();ui.returnToCities=false;ui.page=page;ui.panelVisible=!page.equals("map");ui.panelExpanded=false;refresh();revealPanel();});
            item.setContentDescription("导航 · "+labels[i]);navigation.put(page,item);list.addView(item,new LinearLayout.LayoutParams(-1,dp(48)));
        }
        navigationDialog.show();UiTheme.dialog(navigationDialog);
    }
    private void showMapTools(){
        if(mapPick!=null)cancelMapPick();
        String[] labels={"全图","定位","导航图","屏幕方向","战报","操作说明","战斗震动","兵种与建筑图例","领地着色 / 前线","势力领地图例","军团托管","全国城池总览","部队标注 / 双条","2D / 3D 试验模式","渲染诊断","3D 镜头回正","3D 反向查看","3D 画质",map.gridShown()?"棋盘网格 · 已开启":"棋盘网格 · 已关闭"};
        new AlertDialog.Builder(this).setTitle("地图视图").setItems(labels,(d,index)->{
            if(aiRunning&&index!=0&&index!=1&&index!=2&&index!=3&&index!=12&&index!=18){if(index==13)message("渲染模式","请等待本旬演示结束后切换，避免中断当前事件游标。");return;}
            if(index==0){closePanel();map.post(map::fit);}
            else if(index==1){closePanel();if(selected!=null)map.post(()->map.focus(selected));}
            else if(index==2){closePanel();map.toggleNavigator();}
            else if(index==3)showOrientationPicker();
            else if(index==13){new AlertDialog.Builder(this).setTitle("地图渲染模式").setSingleChoiceItems(new String[]{"2D · 兼容模式","3D · 战略地图（试验）"},map.is3D()?1:0,(dialog,which)->{map.switchMode(which==1);dialog.dismiss();}).setNegativeButton("返回",null).show();}
            else if(index==18){map.setGridShown(!map.gridShown());refreshGridToggle();}
            else if(index==17){new AlertDialog.Builder(this).setTitle("3D 画质（切换时重载场景）").setSingleChoiceItems(new String[]{SceneQuality.LOW.label,SceneQuality.MEDIUM.label,SceneQuality.HIGH.label},map.quality().ordinal(),(dialog,which)->{map.quality(SceneQuality.values()[which]);dialog.dismiss();}).setNegativeButton("返回",null).show();}
            else if(index==15){map.resetOrientation();}
            else if(index==16){map.reverseOrientation();}
            else if(index==14){map.toggleDiagnostics();message("渲染诊断",map.report());}
            else if(index==4)showTurnReport();
            else if(index==6){boolean enabled=getPreferences(MODE_PRIVATE).getBoolean("battleHaptics",true);
                new AlertDialog.Builder(this).setTitle("战斗震动").setSingleChoiceItems(new String[]{"开启（遵循系统触感设置）","关闭"},enabled?0:1,(dialog,which)->{getPreferences(MODE_PRIVATE).edit().putBoolean("battleHaptics",which==0).apply();dialog.dismiss();}).setNegativeButton("返回",null).show();}
            else if(index==7)VisualGuide.show(this,world);
            else if(index==8)showTerritoryPicker();
            else if(index==9)showTerritoryLegend();
            else if(index==10)new WorldUi(this,world,this::apply).districts();
            else if(index==12){new AlertDialog.Builder(this).setTitle("部队地图标注").setMultiChoiceItems(new String[]{"显示主将姓名","显示兵力（绿）与气力（蓝）"},new boolean[]{map.commandersShown(),map.unitBarsShown()},(dialog,which,checked)->{if(which==0)map.setCommandersShown(checked);else map.setUnitBarsShown(checked);}).setPositiveButton("完成",null).show();}
            else if(index==11){ui.returnToCities=false;ui.page="cities";ui.panelVisible=true;ui.panelExpanded=true;refresh();revealPanel();}
            else message("地图操作","单指拖动地图 · 双指缩放 · 双击城池定位\n点城池或部队打开指令，点空地或「收起」返回大地图。\n「功能」打开城市、武将、任务和存档菜单。\n竖屏使用底部面板，横屏使用右侧面板；「展开」可查看更多内容。\n选中部队即显示青色行动范围和红色攻击目标。长按选中的部队，再拖到高亮空格，松手立即移动；拖出范围或双指触摸会取消。\n点击空地查看状态，金色＋开发地的「开发此地」按钮固定在面板顶部，再选择设施和执行人。先点「行军」再点目标预览路线；「攻击」「战法」「计略」在固定底栏。普通点空地、再点本队或「取消选中」可解除选择。返回键依次取消路线、指令、选中。");
        }).setNegativeButton("返回",null).show();
    }
    private void refreshGridToggle(){
        if(gridToggle==null||map==null)return;boolean shown=map.gridShown();
        gridToggle.setSelected(shown);gridToggle.setTextColor(shown?gold:muted);
        gridToggle.setContentDescription("棋盘网格："+(shown?"已开启":"已关闭")+"；点击切换，远景自动隐藏，保留行动与选区提示");
        gridToggle.setTooltipText("棋盘网格 · "+(shown?"开启":"关闭"));
        if(android.os.Build.VERSION.SDK_INT>=30)gridToggle.setStateDescription(shown?"已开启":"已关闭");
    }
    private void setTerritoryMode(int mode){
        map.setTerritoryMode(mode);
        getPreferences(MODE_PRIVATE).edit().putInt("territoryMode",map.territoryMode()).apply();
        refreshTerritoryToggle();
    }
    private void refreshTerritoryToggle(){
        if(territoryToggle==null||map==null)return;
        int mode=map.territoryMode();territoryToggle.setSelected(mode>0);
        String state=mode==0?"已关闭":mode==1?"势力范围已开启":"据点辖区已开启";
        territoryToggle.setContentDescription("地图着色："+state+"；点击切换势力着色，长按选择辖区模式");
        territoryToggle.setTooltipText("地图着色："+state+" · 长按更多选项");
        if(android.os.Build.VERSION.SDK_INT>=30)territoryToggle.setStateDescription(state);
    }
    private void showTerritoryPicker(){
        new AlertDialog.Builder(this).setTitle("领地着色 / 前线").setSingleChoiceItems(
            new String[]{"关闭领地着色","势力范围 · 同势力合并","据点辖区 · 城 / 关 / 港边界"},map.territoryMode(),(dialog,which)->{
                setTerritoryMode(which);dialog.dismiss();
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
            if(count==0)continue;TextView row=text("■  "+world.faction(owner)+" · "+count+"据点 · 前线"+front,16,FactionColors.color(world,owner));row.setPadding(0,dp(8),0,dp(8));list.addView(row);
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
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);if(map!=null&&world!=null){layoutPanels();refreshCommandDock();}if(root!=null)root.requestApplyInsets();fitConfirmation();}
    void trackDialog(AlertDialog dialog){confirmationDialog=dialog;UiTheme.dialog(dialog);fitConfirmation();}
    private void fitConfirmation(){
        if(confirmationDialog==null||!confirmationDialog.isShowing())return;
        // A dialog opened during sensor rotation can retain the previous orientation's minimum width.
        int width=dp(Math.min(560,Math.max(240,getResources().getConfiguration().screenWidthDp-32)));
        confirmationDialog.getWindow().setLayout(width,WindowManager.LayoutParams.WRAP_CONTENT);
    }
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);UiTheme.text(t);return t;}
    Button button(String value,View.OnClickListener action){
        Button b=CompactButtons.create(this);b.setText(value);
        b.setOnClickListener(v->{if(!aiRunning||value.equals("收起")||value.equals("展开")||value.equals("全图")||value.equals("视图"))action.onClick(v);});return b;
    }
    private void line(String value,int size,int color){TextView t=text(value,size,color);t.setPadding(0,dp(4),0,dp(4));panel.addView(t);}
    private void action(String label,View.OnClickListener click){Button b=button(label,click);b.setEnabled(!aiRunning);panel.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));}
    private void iconAction(String label,Object item,View.OnClickListener click){
        Button b=button(label,click);android.graphics.drawable.Drawable icon=GameIcon.drawable(this,world,item);icon.setBounds(0,0,dp(36),dp(36));b.setCompoundDrawables(icon,null,null,null);b.setCompoundDrawablePadding(dp(8));b.setEnabled(!aiRunning);panel.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    private LinearLayout visualHeader(Object item,String name,String subtitle,int size){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(6),0,dp(6));
        ImageView icon=new ImageView(this);icon.setImageDrawable(GameIcon.drawable(this,world,item));icon.setContentDescription(name+"头像或模型");row.addView(icon,new LinearLayout.LayoutParams(dp(size),dp(size)));
        LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(dp(12),0,0,0);words.addView(text(name,22,gold));words.addView(text(subtitle,13,paper));row.addView(words,new LinearLayout.LayoutParams(0,-2,1));return row;
    }
    private World.Unit selectedUnit(){
        if(selected==null||ui.selectedUnit==-2)return null;
        World.Unit u=world.unit(ui.selectedUnit);if(u!=null&&selected.equals(u.hex))return u;
        return ui.selectedUnit>=0?null:world.unitAt(selected);
    }
    private void onTile(Hex h) {
        if(aiRunning||world.commandsBlocked())return;
        if(mapPick!=null){
            if(h!=null&&pickTargets.contains(h))mapPick.accept(h);
            else message("目标不可执行",pickError==null?"当前地块不满足"+pickTitle+"的条件。请点高亮目标，或取消选取。":pickError.apply(h));
            return;
        }
        if(h==null){clearUnitSelection();return;}
        World.Unit source=world.unit(moving);
        if(source!=null&&unitCommand.equals("march")){if(h.equals(source.hex)){clearUnitSelection();return;}previewMarch(source,h);return;}
        if(source!=null&&unitCommand.equals("attack")){
            List<World.Unit> candidates=new ArrayList<>();for(World.Unit u:world.fieldUnits())if(h.equals(u.hex)&&u.id!=source.id)candidates.add(u);
            World.City underlying=world.cityAt(h);
            if(underlying!=null&&!candidates.isEmpty()){
                World.Unit field=candidates.get(0);
                new AlertDialog.Builder(this).setTitle("同格攻击对象").setItems(new String[]{world.officer(field.officerId).name+" · 野战部队",underlying.name+" · 据点"},(d,i)->attackOnMap(source,h,i==0?field:null)).setNegativeButton("取消",null).show();return;
            }
            if(candidates.size()>1){new AlertDialog.Builder(this).setTitle("选择攻击对象").setItems(candidates.stream().map(u->world.officer(u.officerId).name+" · "+world.army.equipmentLabel(u)).toArray(String[]::new),(d,i)->attackOnMap(source,h,candidates.get(i))).setNegativeButton("取消",null).show();return;}
            attackOnMap(source,h,candidates.isEmpty()?null:candidates.get(0));return;
        }
        List<World.Unit> candidates=new ArrayList<>();for(World.Unit u:world.units)if(h.equals(u.hex))candidates.add(u);
        // City staging and migrated overlapping convoys are visible objects, although not field combat targets.
        for(Domestic.Mission mission:world.domestic.missions)if(mission.transport&&h.equals(mission.hex))candidates.add(mission);
        World.City city=world.cityAt(h);
        if(candidates.size()+(city==null?0:1)>1){
            List<String> labels=new ArrayList<>();if(city!=null)labels.add(city.name+" · 据点");
            for(World.Unit u:candidates)labels.add(world.officer(u.officerId).name+" · "+world.army.equipmentLabel(u)+" · "+world.faction(u.owner));
            new AlertDialog.Builder(this).setTitle("同格对象 · 选择操作").setItems(labels.toArray(new String[0]),(d,i)->selectObject(h,city!=null&&i==0?-2:candidates.get(i-(city==null?0:1)).id,false)).setNegativeButton("取消",null).show();return;
        }
        World.Unit target=candidates.isEmpty()?null:candidates.get(0);
        if(target!=null&&selected!=null&&h.equals(selected)&&ui.selectedUnit==target.id){clearUnitSelection();return;}
        if(target==null&&city==null&&world.domestic.at(h)==null&&world.war.at(h)==null&&world.events.at(h)==null){
            if(world.inside(h)&&world.terrain[h.q][h.r]!=World.Terrain.VOID)selectObject(h,-2,false);
            else clearUnitSelection();return;
        }
        if(world.events.at(h)!=null){new WorldUi(this,world,this::apply).camp(world.events.at(h));return;}
        selectObject(h,target==null?-2:target.id,false);
    }
    private void dropUnit(MarchOrders.Plan plan){
        if(aiRunning||world.commandsBlocked()||plan==null||plan.unitId!=moving)return;
        World.Unit u=world.unit(moving);if(world.orders.error(u)!=null)return;
        if(!world.orders.marchReachable(u).containsKey(plan.target)||plan.stepsNow!=plan.path.size()-1)return;
        pendingMarch=null;unitCommand="select";apply(world.marches.execute(plan));
    }
    private void attackOnMap(World.Unit source,Hex h,World.Unit target){
        String error=world.orders.combatError(source);
        if(error==null&&target!=null)error=world.war.attackError(source.id,target.id);
        Domestic.Facility facility=world.domestic.at(h);World.City city=world.cityAt(h);War.Structure structure=world.war.at(h);
        if(error==null&&target==null){
            if(facility!=null)error=world.war.facilityAttackError(source.id,h);
            else if(city!=null)error=world.siegeError(source.id,city.id);
            else if(structure!=null)error=world.war.structureAttackError(source.id,h);
            else error="请选择射程内可交战的部队、城池或设施";
        }
        if(error!=null){message("无法攻击",error);return;}
        if(target!=null){warUi().attack(source,target);return;}
        if(facility!=null){confirm("攻击"+facility.kind.label+"？\n耐久 "+facility.hp+"/"+facility.maxHp()+" · 预计减少"+Math.min(facility.hp,world.war.facilityDamage(source.id))+"\n攻击结束本旬行动，摧毁后释放地块。",()->apply(world.war.attackFacility(source.id,h)));return;}
        if(city!=null){Hex hit=world.siegeHit(source,city,h);showTacticPreview(world,world.siegePreview(source.id,city.id,hit),()->apply(world.siege(source.id,city.id,hit)));return;}
        confirm("攻击军事设施？攻击结束本旬行动。",()->apply(world.war.attackStructure(source.id,h)));
    }
    private void selectObject(Hex h,int unitId,boolean focus){
        pendingMarch=null;unitCommand="select";mapPick=null;pickTargets=Collections.emptySet();
        selected=h;ui.selectedUnit=unitId;World.Unit unit=selectedUnit();moving=unit!=null&&unit.owner==world.player?unit.id:-1;
        ui.page="map";ui.panelVisible=unit==null;ui.panelExpanded=false;ui.group="概览";refresh();
        if(focus)map.post(()->map.focus(h));if(ui.panelVisible)revealPanel();
    }
    private void clearUnitSelection(){pendingMarch=null;unitCommand="select";moving=-1;ui.selectedUnit=-1;selected=null;closePanel();}
    void pickOnMap(String title,Hex origin,Collection<Hex> targets,java.util.function.Consumer<Hex> action){
        pickError=null;
        if(targets.isEmpty()){message(title,"当前没有可用目标。请检查射程、气力、适性、库存或地块条件。");return;}
        pendingMarch=null;unitCommand="select";pickTitle=title;pickTargets=new LinkedHashSet<>(targets);mapPick=action;
        ui.page="map";ui.panelVisible=false;ui.panelExpanded=false;refresh();
        commandDock.announceForAccessibility(title+"，请点选高亮地块");
    }
    void pickOnMap(String title,Hex origin,Collection<Hex> targets,java.util.function.Consumer<Hex> action,java.util.function.Function<Hex,String> error){
        pickOnMap(title,origin,targets,action);pickError=h->{String reason=error.apply(h);return reason==null?"目标已变化，请重新选择":reason;};
    }
    private void cancelMapPick(){clearTacticPreview();mapPick=null;pickTargets=Collections.emptySet();pickTitle="";refresh();}
    private void approach(World.Unit u,Hex h){
        if(unitCommand.equals("march")){previewMarch(u,h);return;}
        confirm("目标在当前射程外，预览自动攻击路线？确认后将逐旬接近并持续攻击，攻下据点后自动进驻。",()->{unitCommand="march";previewMarch(u,h);});
    }
    private void showTactics(World.Unit u){
        pendingMarch=null;unitCommand="select";refresh();
        String error=world.orders.error(u);if(error!=null){message("战法暂不可用",error);return;}
        if(world.army.water(u.hex)||Army.siegeWeapon(u.weapon))armyUi().tactics(u);else warUi().tactics(u);
    }
    private void message(String title,String value){new AlertDialog.Builder(this).setTitle(title).setMessage(value).setPositiveButton("返回",null).show();}
    private void confirm(String value,Runnable action){commandDialog(null,value,"执行",world,action);}
    boolean currentWorld(World expected){return !aiRunning&&!isFinishing()&&!isDestroyed()&&world==expected;}
    void commandDialog(String heading,String value,String positive,World expected,Runnable action){
        final long revision=expected==null?0:expected.commandRevision();
        boolean[] submitted={false};
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(heading).setMessage(value).setPositiveButton(positive,(d,n)->{
            if(submitted[0])return;submitted[0]=true;
            if(!currentWorld(expected)||(world==null?0:world.commandRevision())!=revision){message("命令未执行","局面已变化，请重新预览；本次没有扣除资源。");return;}
            action.run();
        }).setNegativeButton("取消",null).show();trackDialog(dialog);
    }
    void showTacticPreview(World expected,Displacement.Preview preview,Runnable execute){
        if(!preview.valid()){message("不能发动",preview.error);return;}
        final long revision=expected.commandRevision();
        tacticPreview=preview;boolean[] submitted={false};
        tacticConfirmation=()->{
            if(submitted[0])return;submitted[0]=true;
            if(!currentWorld(expected)||world.commandRevision()!=revision){clearTacticPreview();refresh();message("命令未执行","局面已变化，请重新选择目标；本次未扣资源。");return;}
            clearTacticPreview();execute.run();
        };
        ui.panelVisible=false;refresh();
    }
    private void clearTacticPreview(){tacticPreview=null;tacticConfirmation=null;if(map!=null)map.setTacticPreview(null);}
    private void locateBattleReport(){
        if(reportLocation==null||battleReportWorld!=world)return;
        reportReturn=new Bundle();writeClientState(reportReturn);mapPick=null;pickTargets=Collections.emptySet();clearTacticPreview();
        // A historical event points to its coordinate, never a potentially replaced unit ID.
        selected=reportLocation;ui.selectedUnit=-2;moving=-1;ui.page="map";ui.panelVisible=false;refresh();map.post(()->map.focus(reportLocation));
    }
    private void returnFromBattleReport(){
        Bundle saved=reportReturn;reportReturn=null;if(saved==null)return;ui.read(saved);moving=saved.getInt("moving",-1);selected=new Hex(saved.getInt("selectedQ",-1),saved.getInt("selectedR",-1));if(!world.inside(selected))selected=null;
        pendingMarch=null;unitCommand="select";refresh();map.restoreCamera(saved);
    }
    void applyResult(World.Result result){apply(result);}
    void apply(World.Result result){
        if(!result.ok)message("命令未执行",result.message);
        if(result.ok){map.invalidateScene();navigatorRevision=Long.MIN_VALUE;clearTacticPreview();pendingMarch=null;unitCommand="select";mapPick=null;pickTargets=Collections.emptySet();pickTitle="";}
        if(result.ok&&result.feedback==World.Feedback.NONE){lastBattleReport=result.message;battleReportWorld=world;reportLocation=null;battleBanner.setText("结果 · 点此展开\n"+result.message);battleBanner.setVisibility(View.VISIBLE);}
        if(result.ok&&result.feedback!=World.Feedback.NONE){
            lastBattleReport=result.message;battleReportWorld=world;reportLocation=result.impact;
            battleBanner.setText((result.feedback==World.Feedback.DEFEAT?"击破战果":"战斗")+" · 点此展开\n"+result.message);
            battleBanner.setVisibility(View.VISIBLE);
            map.battleFeedback(result,getPreferences(MODE_PRIVATE).getBoolean("battleHaptics",true));
        }
        if(result.ok&&moving>=0&&world.unit(moving)!=null)selected=world.unit(moving).hex;
        refresh();if(result.ok)save("auto",false);
        if(result.ok&&result.critical!=null)showCritical(result.critical);
        if(result.ok&&world.gameOver())message(world.winner==world.player?"战场胜利":"战场战败","本局结束，可从菜单重新选择剧本。");
    }
    void refresh(){
        refreshQuickNavigator();refreshTurnProgress();
        if(battleReportWorld!=world){battleReportWorld=world;lastBattleReport="";reportLocation=null;reportReturn=null;clearTacticPreview();battleBanner.setVisibility(View.GONE);}
        if(ui.city>=0&&world.city(ui.city)==null)ui.city=-1;
        if(ui.cityDistrict>0&&world.districts.get(ui.cityDistrict)==null)ui.cityDistrict=-1;
        if(ui.owner>=world.factions.length)ui.owner=-1;
        if(ui.cityOwner>=world.factions.length)ui.cityOwner=-1;
        title.setText(world.faction(world.player)+" · "+world.scenarioName);
        dateBanner.setText(world.date().replace(" ",""));dateBanner.setVisibility(View.VISIBLE);
        dateBanner.setContentDescription("当前日期 "+world.date());
        mapRevisionNotice.setText(NationalMap.compatibilityNotice(world));mapRevisionNotice.setVisibility(mapRevisionNotice.getText().length()==0?View.GONE:View.VISIBLE);
        actionPointsBadge.setText("行动力\n"+world.actionPoints[world.player]);actionPointsBadge.setContentDescription("玩家行动力 "+world.actionPoints[world.player]+" 点");
        UiTheme.title(title);title.setContentDescription("军情 · "+title.getText());
        nextTurn.setEnabled(playback!=null||mapPick==null&&!aiRunning&&!world.gameOver()&&!world.commandsBlocked());nextTurn.setText(playback!=null?"演示控制":aiRunning?"结算中…":"下一旬  →");
        for(Map.Entry<String,Button> e:navigation.entrySet()){e.getValue().setEnabled(!aiRunning);e.getValue().setSelected(e.getKey().equals(ui.page));e.getValue().setTextColor(e.getKey().equals(ui.page)?gold:paper);}
        if(navigation.get("tasks")!=null)navigation.get("tasks").setText("任务"+(taskCount()>0?" "+taskCount():""));
        panelHost.removeAllViews();panel.removeAllViews();primaryActions.removeAllViews();primaryActions.setVisibility(View.GONE);
        boolean required=(world.life.pending()||world.contests.busy())&&!ui.page.equals("menu");
        boolean showPanel=ui.panelVisible||required;UiMotion.surface(panelShell,showPanel,portrait());closePanel.setEnabled(!required);
        returnList.setVisibility(ui.returnToCities&&ui.page.equals("map")?View.VISIBLE:View.GONE);
        if(aiRunning||ui.summary.isEmpty())turnBanner.setVisibility(View.GONE);
        World.Unit selectedActor=selectedUnit();
        String selectedName=selectedActor!=null?world.officer(selectedActor.officerId).name:selected==null?"点选城池":world.cityAt(selected)!=null?world.cityAt(selected).name:"地块";
        int ready=UiModels.readyUnits(world).size();previousReady.setEnabled(!aiRunning&&mapPick==null);nextReady.setEnabled(!aiRunning&&mapPick==null);previousReady.setTooltipText("上一个待行动部队 · "+ready+"队");nextReady.setTooltipText("下一个待行动部队 · "+ready+"队");
        selectionButton.setText(selectedName+(showPanel?" · 收起":selectedUnit()!=null?" · 详情":" · 指令"));
        selectionButton.setContentDescription("选中对象指令 · "+selectedName);selectionButton.setEnabled(mapPick==null&&!aiRunning&&!required);
        panelTitle.setText(ui.page.equals("map")?selectedName+" · 指令":ui.page.equals("cities")?"城池一览":ui.page.equals("officers")?"武将一览":ui.page.equals("tasks")?"任务":ui.page.equals("menu")?"菜单":ui.page.equals("factions")?"天下势力":ui.page.equals("units")?"全部部队":ui.page.equals("ports")?"港口一览":ui.page.equals("gates")?"关卡一览":ui.page.equals("facilities")?"设施一览":"资料");
        if(world.unit(moving)==null)moving=-1;
        map.setWorld(playback!=null&&turnWork!=null?turnWork.visual:world,playback==null?selected:null,playback==null?moving:-1);
        if(!showPanel){/* Hidden details are not inflated or measured. */}
        else if(world.life.pending()&&!ui.page.equals("menu")){panelHost.addView(new LifecycleUi(this,world,this::apply).succession());}
        else if(world.contests.busy()&&!ui.page.equals("menu"))panelHost.addView(new ContestUi(this,world,this::apply).view());
        else if(ui.page.equals("cities"))panelHost.addView(new OverviewUi(this,world,ui).cities());
        else if(ui.page.equals("officers"))panelHost.addView(new OverviewUi(this,world,ui).officers());
        else if(ui.page.equals("content"))panelHost.addView(new ContentUi(this,world,ui).view());
        else if(ui.page.equals("factions"))panelHost.addView(new RealmUi(this,world,ui).factions());
        else if(ui.page.equals("units"))panelHost.addView(new RealmUi(this,world,ui).units());
        else if(ui.page.equals("ports"))panelHost.addView(new RealmUi(this,world,ui).sites(World.SiteKind.PORT));
        else if(ui.page.equals("gates"))panelHost.addView(new RealmUi(this,world,ui).sites(World.SiteKind.GATE));
        else if(ui.page.equals("facilities"))panelHost.addView(new RealmUi(this,world,ui).facilities());
        else if(ui.page.equals("tasks"))panelHost.addView(new OverviewUi(this,world,ui).tasks());
        else {panelHost.addView(panelScroll);if(ui.page.equals("menu"))showMenu();else showSelection();}
        map.setRoute(pendingMarch!=null?pendingMarch:world.unit(moving)!=null&&world.unit(moving).march!=null?world.marches.current(world.unit(moving)):null);
        map.setPickTargets(mapPick==null?null:pickTargets);map.setTacticPreview(tacticPreview);
        // onTile/dropUnit guard commands; panning/zooming and closing panels remain available during AI.
        map.setEnabled(true);refreshCommandDock();layoutPanels();
        String identity=ui.page+"/"+selected+"/"+ui.selectedUnit+"/"+ui.group;
        if(showPanel&&!identity.equals(panelIdentity))UiMotion.enter(panelHost,portrait());
        panelIdentity=identity;
    }
    private void previewMarch(World.Unit unit,Hex target){
        World.City c=world.cityAt(target);
        pendingMarch=c!=null&&c.owner==unit.owner?world.marches.previewCity(unit.id,c.id):world.marches.preview(unit.id,target);
        ui.page="map";ui.panelVisible=false;refresh();
        if(!pendingMarch.valid())commandDock.announceForAccessibility(pendingMarch.error);
    }
    private void garrisonOnMap(World.Unit u){
        List<Hex> targets=new ArrayList<>();for(World.City c:world.cities)if(c.owner==u.owner)targets.addAll(SiteFootprint.cells(c));
        pickOnMap("进驻 · 点选己方据点（自动选择可达入口）",u.hex,targets,h->{World.City c=world.cityAt(h);if(c==null)return;
            mapPick=null;pickTargets=Collections.emptySet();pendingMarch=world.marches.previewCity(u.id,c.id);ui.panelVisible=false;refresh();});
    }
    private void refreshCommandDock(){
        commandDock.removeAllViews();World.Unit u=world.unit(moving);
        if(tacticPreview!=null){
            commandDock.setVisibility(View.VISIBLE);commandDock.setOrientation(portrait()?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
            ScrollView scroll=new ScrollView(this);scroll.addView(text(tacticPreview.text+"\n地图：青箭头己方，橙箭头目标，红叉阻挡，橙圈可能风险。",13,paper));
            int height=dp(portrait()?132:112);commandDock.addView(scroll,new LinearLayout.LayoutParams(portrait()?-1:0,height,portrait()?0:1));
            LinearLayout actions=new LinearLayout(this);Runnable confirm=tacticConfirmation;
            actions.addView(button("取消",v->{clearTacticPreview();refresh();}),new LinearLayout.LayoutParams(0,dp(48),1));
            actions.addView(button("执行",v->confirm.run()),new LinearLayout.LayoutParams(0,dp(48),1));
            commandDock.addView(actions,new LinearLayout.LayoutParams(portrait()?-1:dp(180),-2));return;
        }
        if(reportReturn!=null){commandDock.setVisibility(View.VISIBLE);commandDock.setOrientation(LinearLayout.HORIZONTAL);commandDock.addView(text("历史战果地点 "+reportLocation,13,paper),new LinearLayout.LayoutParams(0,-2,1));commandDock.addView(button("返回定位前",v->returnFromBattleReport()),new LinearLayout.LayoutParams(dp(120),dp(48)));return;}

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
            String hint=unitCommand.equals("march")?"终点为己方据点任意占地格即自动入城；途中可穿行":unitCommand.equals("attack")?"攻击：点红框敌军、城池或设施":error!=null?error:"青色为本旬可达范围 · 红框可攻击";
            TextView state=text((u instanceof Domestic.Mission?"运输":"兵"+u.troops)+" · 携粮 "+u.food+" · "+hint+" · 剩余移动 "+world.orders.remaining(u),12,gold);state.setMaxLines(2);
            LinearLayout summary=new LinearLayout(this);summary.setGravity(Gravity.CENTER_VERTICAL);
            summary.addView(state,new LinearLayout.LayoutParams(0,-2,1));
            Button details=button("详情",v->{ui.panelVisible=true;refresh();revealPanel();});details.setTag("unit.details");
            details.setContentDescription("主动查看部队详情");summary.addView(details,new LinearLayout.LayoutParams(dp(56),dp(48)));
            commandDock.addView(summary,new LinearLayout.LayoutParams(portrait()?-1:0,-2,portrait()?0:1));
            LinearLayout actions=new LinearLayout(this);
            Button march=button("行军",v->{unitCommand="march";ui.panelVisible=false;refresh();});march.setSelected(unitCommand.equals("march"));
            Button attack=button("攻击",v->{if(error!=null){message("攻击暂不可用",error);return;}unitCommand="attack";ui.panelVisible=false;refresh();});attack.setSelected(unitCommand.equals("attack"));attack.setAlpha(error==null?1f:.55f);
            Button tactics=button("战法",v->showTactics(u));tactics.setAlpha(error==null?1f:.55f);
            Button plots=button("计略",v->{if(error!=null){message("计略暂不可用",error);return;}pendingMarch=null;unitCommand="select";refresh();warUi().plots(u);});plots.setAlpha(error==null?1f:.55f);
            Button cancel=button("取消",v->clearUnitSelection());
            if(u instanceof Domestic.Mission){attack.setText("补给");attack.setOnClickListener(v->{if(error!=null)message("补给暂不可用",error);else convoySupply((Domestic.Mission)u);});tactics.setText("停止");tactics.setOnClickListener(v->apply(world.marches.stop(u.id)));plots.setText("货物");plots.setOnClickListener(v->{ui.panelVisible=true;refresh();revealPanel();});}
            for(Button b:new Button[]{march,attack,tactics,plots})actions.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));
            if(!(u instanceof Domestic.Mission)&&u.march!=null)actions.addView(button("停止任务",v->apply(world.marches.stop(u.id))),new LinearLayout.LayoutParams(0,dp(48),1));
            actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(48),1));
            commandDock.addView(actions,new LinearLayout.LayoutParams(portrait()?-1:dp(360),-2));return;
        }
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=text(pendingMarch.actionLabel+" · "+pendingMarch.label,14,paper);heading.setMaxLines(1);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(heading);
        TextView hint=text(pendingMarch.valid()?(u.acted?"本旬已行动，下旬出发 · ":u.status!=War.Status.NORMAL?"异常状态，恢复后继续 · ":world.orders.remaining(u)==0?"移动力已用尽，下旬继续 · ":"本旬 "+pendingMarch.stepsNow+" 格 · ")+"行军预计再需 "+pendingMarch.estimatedTurns+" 旬 · "+pendingMarch.completion:pendingMarch.error,12,muted);hint.setMaxLines(3);hint.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(hint);
        if(pendingMarch.valid())copy.addView(text("路线总耗移动 "+pendingMarch.cost+" · 本旬走"+pendingMarch.stepsNow+"格 · "+(u.food<Logistics.foodUse(world,u)*(pendingMarch.estimatedTurns+1)?"携粮可能不足":"携粮按现有兵力估算可支撑路线"),12,gold));
        commandDock.addView(copy,new LinearLayout.LayoutParams(portrait()?-1:0,-2,portrait()?0:1));
        LinearLayout actions=new LinearLayout(this);MarchOrders.Plan plan=pendingMarch;
        Button cancel=button("取消",v->{pendingMarch=null;unitCommand="select";refresh();});
        Button execute=button("确认任务",v->apply(world.marches.execute(plan)));execute.setTextColor(gold);execute.setEnabled(plan.valid()&&!aiRunning);
        actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(execute,new LinearLayout.LayoutParams(0,dp(48),1));
        commandDock.addView(actions,new LinearLayout.LayoutParams(portrait()?-1:dp(208),-2));
    }
    private void nextUnit(){cycleReady(1);}
    private void cycleReady(int direction){
        World.Unit next=UiModels.cycleReady(world,moving,direction);
        if(next==null){message("待行动部队","没有需要手动指挥的待行动队伍。\n按部队编号循环；排除已行动、异常、委任、施工和自动行军部队。运输仅包含已停止且可手动操作的队伍。");return;}
        selectObject(next.hex,next.id,true);ui.panelVisible=false;refresh();
        commandDock.announceForAccessibility("待行动部队 · "+world.officer(next.officerId).name);
    }
    private void unitStats(World.Unit u){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(10),dp(6),dp(10),dp(6));
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setColor(0xff203346);shape.setCornerRadius(dp(12));card.setBackground(shape);
        card.addView(CommandStats.unit(this,u),new LinearLayout.LayoutParams(-1,-2));
        ProgressBar energy=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);energy.setMax(world.campaign.energyCap(u.owner));energy.setProgress(u.energy);energy.setProgressTintList(android.content.res.ColorStateList.valueOf(gold));energy.setContentDescription("气力 "+u.energy);card.addView(energy,new LinearLayout.LayoutParams(-1,dp(10)));panel.addView(card);
    }
    private int taskCount(){return UiModels.tasks(world,0).size();}
    private void primaryAction(String label,Runnable run){
        Button b=button(label,v->{if(!aiRunning&&!world.commandsBlocked())run.run();});b.setSelected(true);b.setEnabled(!aiRunning);
        primaryActions.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));primaryActions.setVisibility(View.VISIBLE);
    }
    private void showSelection(){
        World.Unit unit=selectedUnit();World.City city=selected==null?null:world.cityAt(selected);
        if(unit!=null)showUnit(unit);else if(city!=null)showCity(city);else if(selected!=null&&world.domestic.at(selected)!=null){
            Domestic.Facility f=world.domestic.at(selected);panel.addView(visualHeader(f,f.kind.label,world.city(f.cityId).name,64));line("耐久 "+f.hp+"/"+f.maxHp(),14,gold);line(f.remaining==0?f.kind.effect:"建设中 · 剩"+f.remaining+"旬",14,paper);
            primaryAction("设施详情 / 管理",()->domesticUi().facility(f));primaryAction("所属城池",()->selectAndFocus(world.city(f.cityId).hex));
        }else if(selected!=null&&world.war.at(selected)!=null){War.Structure s=world.war.at(selected);MapTapTrace.detail("MainActivity.showSelection.structure",world,selected);panel.addView(visualHeader(s,s.kind.label,NaturalStructures.ownerLabel(world,s.owner),64));line(NaturalStructures.ownerLabel(world,s.owner)+" · 耐久"+s.hp+"/"+s.kind.hp,15,paper);line(s.complete?s.kind.effect:"施工中，建成后生效",14,paper);line(world.fieldworks.coverageDescription(s),13,gold);if(s.builder>=0&&world.unit(s.builder)!=null)action("定位施工部队",v->selectAndFocus(world.unit(s.builder).hex));}
        else if(selected!=null)showTerrain(selected);
        else {line("山河之间",23,gold);line("点空地查看地形与开发条件，点城池或部队下达指令。",15,paper);action("定位本城",v->{if(world.home()!=null)selectAndFocus(world.home().hex);});}
    }
    private void showTerrain(Hex h){
        MapTapTrace.detail("MainActivity.showTerrain",world,h);
        TerrainPresentation.Detail detail=TerrainPresentation.detail(world,h);
        World.Terrain t=detail.terrain();World.City c=world.development.cityAt(h);
        String reason=null;
        if(c!=null){
            if(c.owner!=world.player)reason="仅能开发己方城市的地块";
            else if(!world.districts.directCity(c.id))reason="此城由军团托管，请先改为直属经营";
            else if(!world.domestic.buildSites(c.id).contains(h))reason="地块不可用：检查设施容量、占用、火场、地形或城池出口";
            final String unavailable=reason;
            if(reason==null){primaryAction("＋ 开发此地",()->BuildPicker.open(this,world,c,h));line("可开发 · "+c.name+"开发地",18,gold);}
            else {primaryAction("查看开发条件",()->message("暂不可开发",unavailable));line(reason,14,gold);}
            primaryAction("前往"+c.name,()->selectAndFocus(c.hex));
        }
        line(detail.presentation().name()+" · "+MapCoordinates.display(world,h),18,paper);
        line(detail.presentation().description(),13,muted);
        if(c!=null)line(c.name+" · "+world.faction(c.owner)+"\n设施 "+world.domestic.count(c.id)+" / "+world.development.capacity(c.id)+" · 可用金 "+c.gold,14,paper);
        else line("此地不属于城市开发用地",14,muted);
        int cost=detail.infantryCost();
        line(cost>0?"步兵通行 · 基础移动消耗 "+cost:"普通步兵不可通行",14,paper);
        if(t==World.Terrain.SAND)line("沙地 · 枪兵不能施放战法；普通攻击与通行不受影响",13,gold);
        if(t==World.Terrain.PLANK_ROAD||t==World.Terrain.MOUNTAIN_PATH)line("山地通路 · 相邻栈道与山径按六角方向连接",13,muted);
        if(world.war.fireAt(h)!=null)line("火场 · 剩"+world.war.fireAt(h).remaining+"旬 · 不能建设",14,gold);
    }
    private void showCity(World.City c){
        boolean compact=!ui.group.equals("概览");
        if(c.owner==world.player&&world.districts.directCity(c.id)&&!world.gameOver()){
            primaryAction("出征",()->armyUi().deploy(c));primaryAction("运输",()->domesticUi().transport(c));
            if(c.kind==World.SiteKind.CITY)primaryAction("设施开发",()->domesticUi().build(c));
            else primaryAction("修复城防",()->campaignUi().repair(c));
        }
        line(SiegeRules.summary(world,c)+"\n青格：两圈围城范围 · 红格：敌军",12,SiegeRules.blockaded(world,c)?0xffff9a82:gold);
        panel.addView(visualHeader(c,c.name,world.faction(c.owner)+" · 太守 "+UiModels.governor(world,c.id),44));
        if(compact)line("金 "+c.gold+" · 粮 "+c.food+" · 兵 "+c.troops,13,paper);else panel.addView(CommandStats.city(this,c),new LinearLayout.LayoutParams(-1,-2));
        String[] groups={"概览","内政","武将","军事","调动","外交","研究"};
        HorizontalScrollView tabs=new HorizontalScrollView(this);tabs.setHorizontalScrollBarEnabled(false);tabs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout tabRow=new LinearLayout(this);tabs.addView(tabRow,new HorizontalScrollView.LayoutParams(-2,-1));
        for(String name:groups){Button b=button(name,v->{ui.group=name;refresh();revealPanel();});b.setSelected(ui.group.equals(name));tabRow.addView(b,new LinearLayout.LayoutParams(dp(58),dp(48)));if(ui.group.equals(name))tabs.post(()->tabs.scrollTo(Math.max(0,b.getLeft()-dp(58)),0));}
        tabs.setContentDescription("城池指令分组");
        panel.addView(tabs,new LinearLayout.LayoutParams(-1,dp(48)));
        if(ui.group.equals("概览")){
            action("围城与守备详情",v->message(c.name+" · 围城",world.cityDefense.describe(c)));
            line(world.domestic.incomeSchedule(c.id),13,muted);
            line(Conscription.description(world,c),13,gold);
            if(c.kind!=World.SiteKind.CITY){
                line(world.districts.affiliation(c.id),13,muted);
            }
            line("可用武将 "+world.idle(c).size()+" · 行动力 "+(world.districts.city(c.id)==null?world.actionPoints[world.player]:world.districts.city(c.id).points())+" · "+(world.districts.directCity(c.id)?"直属经营":"军团托管"),13,gold);
        }
        Districts.District district=world.districts.city(c.id);if(district!=null)line("所属军团："+district.name()+" · "+district.policy().label,13,gold);
        String incident=world.events.cityStatus(c.id);if(!incident.equals("无灾害"))line(incident,12,muted);
        boolean own=c.owner==world.player&&!world.gameOver()&&world.districts.directCity(c.id);
        switch(ui.group){
            case "内政":
                if(c.owner==world.player)action("城池托管 / 军团",v->new WorldUi(this,world,this::apply).city(c));
                line("设施 "+world.domestic.count(c.id)+"/"+world.development.capacity(c.id)+" · 地图点＋建设\n"+world.domestic.incomeSchedule(c.id),14,paper);
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
                action("守备与反击规则",v->message(c.name+" · 城防",world.cityDefense.describe(c)));
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
    Bundle formDraft(){return ui.formDraft;}
    void rememberForm(Bundle draft){ui.formDraft=new Bundle(draft);}
    void closeForm(){ui.formDraft=new Bundle();}
    private void restoreFormDraft(){
        if(isFinishing()||isDestroyed()||aiRunning)return;
        Bundle draft=new Bundle(ui.formDraft);
        if("deploy".equals(draft.getString("kind")))armyUi().restoreDraft(draft);
        else if("build".equals(draft.getString("kind")))new BuildPicker(this,world,draft).show();
        else if("cargo".equals(draft.getString("kind")))domesticUi().restoreDraft(draft);
    }
    private ArmyUi armyUi(){return new ArmyUi(this,world,this::apply,this::selectAndFocus);}
    private List<CityCommand> militaryCommands(World.City c){return Arrays.asList(
        new CityCommand("出征",()->armyUi().deploy(c)),
        new CityCommand("快速出征（单将）",()->armyUi().quickDeploy(c)),
        new CityCommand("编队 / 水陆出征",()->armyUi().deploy(c)),
        new CityCommand("军备制造 / 攻城器械与舰船",()->armyUi().manufacture(c)),
        new CityCommand("征兵 · "+world.domestic.usage(c.id,Domestic.Kind.BARRACKS),()->strategyUi().command(c,6)),
        new CityCommand("训练 · 金100",()->strategyUi().command(c,7)),
        new CityCommand("生产兵装 · 查看设施次数与费用",()->armyUi().basicProduction(c))
    );}
    private void showUnit(World.Unit u){
        if(u.owner==world.player&&!world.gameOver()){
            for(World.City base:world.cities)if(base.owner==u.owner&&(world.army.canEnterSite(u,u.hex,base)||base.kind==World.SiteKind.CITY&&SiteFootprint.distance(base,u.hex)<=1)){
                primaryAction("进入"+base.name,()->confirm("沿合法入口进入"+base.name+"并归还兵装与粮草？移动消耗按实际路径计算。",()->apply(world.army.canEnterSite(u,u.hex,base)?world.enter(u.id,base.id):world.marches.execute(world.marches.previewCity(u.id,base.id)))));break;
            }
            if(u.march!=null)primaryAction("停止任务",()->apply(world.marches.stop(u.id)));
        }
        if(u instanceof Domestic.Mission){showConvoy((Domestic.Mission)u);return;}
        World.Officer o=world.officer(u.officerId);panel.addView(visualHeader(o,o.name,world.faction(u.owner)+" · "+world.army.equipmentLabel(u),44));
        unitStats(u);
        line(world.combat.unitStats(u),14,gold);
        line(Logistics.describe(world,u),12,paper);
        action("攻防与副将补正",v->message("部队数值组成",world.combat.statExplanation(u)));
        Diplomacy.Aid aid=world.diplomacy.aidForUnit(u.id);if(aid!=null)line("援军 · "+world.diplomacy.describe(aid),13,gold);
        if(u.owner==world.player){
            LinearLayout quick=new LinearLayout(this);
            quick.addView(button("选择目标",v->{unitCommand="march";closePanel();}),new LinearLayout.LayoutParams(0,dp(48),1));
            quick.addView(button("下一部队",v->nextUnit()),new LinearLayout.LayoutParams(0,dp(48),1));
            if(u.march!=null)quick.addView(button("停止任务",v->apply(world.marches.stop(u.id))),new LinearLayout.LayoutParams(0,dp(48),1));
            panel.addView(quick);
        }
        line("统率 "+o.leadership+"  武力 "+o.war,13,paper);line("剩余移动 "+world.orders.remaining(u)+"  射程 "+world.war.range(u),13,paper);
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
        line("伤兵 "+u.wounded+" · 行军终点为己方据点时立即归队",13,gold);
        Districts.District district=world.districts.unit(u.id);if(district!=null)line("所属军团："+district.name()+" · 自动指挥",13,gold);
        if(u.owner==world.player){line(u.acted?"本旬已行动，攻击后不能再移动 · 可安排下旬行军":"底栏选择行军、攻击、战法；点空地或本队取消选中",14,paper);
            if(u.march!=null)line(world.marches.describe(u),14,gold);
            if(world.fieldworks.project(u.id)!=null)action("中止施工",v->new FieldworkUi(this,world,this::apply).stop(u));
            if(!u.acted&&u.status==War.Status.NORMAL){action("设置军事设施",v->new FieldworkUi(this,world,this::apply).build(u));action("补修军事设施",v->new FieldworkUi(this,world,this::apply).repair(u));action("补充携金",v->new FieldworkUi(this,world,this::apply).fund(u));action("单挑",v->new ContestUi(this,world,this::apply).challenge(u));action("齐攻",v->warUi().joint(u));action("讨伐贼寨",v->new WorldUi(this,world,this::apply).raids(u));action("截击运输队",v->governmentUi().raid(u));action("移交兵粮",v->governmentUi().supply(u));action("部队战法详情",v->showTactics(u));
                if(u.burning>0)action("部队灭火 · 气力5",v->confirm("扑灭本部队火焰？",()->apply(world.army.extinguish(u.id))));action("部队计略",v->{moving=u.id;warUi().plots(u);});action("待命 · 恢复5气力",v->confirm("本旬待命并恢复5气力？",()->apply(world.war.waitUnit(u.id))));}
            for(Domestic.Mission convoy:world.domestic.missions)if(convoy.transport&&convoy.escortId==u.id)action("定位护送运输队",v->selectAndFocus(convoy.hex));
            action("取消部队选择",v->clearUnitSelection());}
    }
    private void showConvoy(Domestic.Mission m){
        World.Officer leader=world.officer(m.officerId);
        panel.addView(visualHeader(leader,leader.name+"运输队",world.faction(m.owner)+" · "+(world.army.water(m.hex)?"水运 · 走舸":"陆运"),44));
        line("携兵 "+m.troops+" · 金 "+m.gold+" · 粮 "+m.food+" · 已耗粮 "+m.consumedFood,14,paper);
        line(world.combat.unitStats(m),14,gold);
        action("运输队攻防组成",v->message("运输队数值",world.combat.statExplanation(m)));
        line(world.domestic.status(m),14,gold);line("目的地："+world.city(m.targetCity).name+" · 当前 "+m.hex,14,paper);
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
        stats+="\n"+world.life.describe(o.id)+"\n\n"+world.loyalty.describe(o);
        stats+="\n特技："+Skill.label(o.skillId)+"\n"+Skill.description(o.skillId)+"\n\n"+world.relations.describe(o.id)+"\n\n宝物：\n"+world.treasures.describe(o.id);
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
    private void returnToCities(){ui.returnToCities=false;ui.page=ui.listPages.getString("returnPage","cities");ui.panelVisible=true;ui.panelExpanded=true;pendingMarch=null;unitCommand="select";refresh();}
    private void revealPanel(){panelScroll.post(()->panelScroll.scrollTo(0,0));}
    void selectAndFocus(Hex h){if(map==null&&world!=null)buildGameUi(null,false,false);if(h==null)return;rememberReturnPage();World.Unit unit=world.unitAt(h);selectObject(h,unit==null?-2:unit.id,true);}
    private GovernmentUi governmentUi(){return new GovernmentUi(this,world,this::apply);}
    private StrategyUi strategyUi(){return new StrategyUi(this,world,this::apply);}
    private CampaignUi campaignUi(){return new CampaignUi(this,world,this::apply);}
    private WarUi warUi(){return new WarUi(this,world,this::apply);}
    private void rememberReturnPage(){
        if(Arrays.asList("cities","units","ports","gates","facilities","officers").contains(ui.page)){ui.listPages.putString("returnPage",ui.page);ui.returnToCities=true;}
    }
    void selectUnitAndFocus(int id){World.Unit u=world.unit(id);if(u==null){message("部队已离场","请返回列表查看当前部队。");return;}rememberReturnPage();selectObject(u.hex,u.id,true);}
    void openRealmPage(String page,int side){
        ui.returnToCities=false;ui.page=page;ui.panelVisible=true;ui.panelExpanded=true;
        ui.listPages.putInt(page+"Owner",side);
        if(page.equals("officers")){ui.owner=side;ui.city=-1;}
        if(page.equals("cities")){ui.cityOwner=side;ui.cityFilter=0;ui.cityDistrict=-1;}
        refresh();revealPanel();
    }
    private void showCritical(CriticalHit hit){
        if(hit==null||body==null||!UiMotion.enabled())return;
        if(criticalFlash!=null)criticalFlash.dismiss();
        CriticalFlash flash=new CriticalFlash(this,world,hit,()->{if(criticalFlash!=null){body.removeView(criticalFlash);criticalFlash=null;}});
        criticalFlash=flash;body.addView(flash,new FrameLayout.LayoutParams(-1,-1));flash.bringToFront();
    }
    DomesticUi domesticUi(){return new DomesticUi(this,world,this::apply,this::selectAndFocus);}
    private void showMenu(){
        line("军政菜单",22,gold);
        action("屏幕方向 / 横竖屏",v->showOrientationPicker());
        action("地图视图与操作",v->showMapTools());
        action("生卒与继承",v->new LifecycleUi(this,world,this::apply).menu());
        action("天下总览 · 势力 / 部队 / 钱粮 / 技巧树",v->openRealmPage("factions",-1));
        action("军团与天下",v->new WorldUi(this,world,this::apply).menu());
        action("武将自定义 · 新战局模板与投放",v->startActivity(new Intent(this,CustomOfficerActivity.class)));
        action("PK编辑 / 新武将",v->new EditorUi(this,world,this::apply).menu());
        if(world.editor.edited())line("当前局面已使用PK编辑",13,muted);
        if(world.life.pending())action("继续君主继承",v->{ui.page="map";refresh();});
        if(world.contests.busy())action("继续当前对局",v->{ui.page="map";refresh();});
        if(!world.contests.lastResult().isEmpty())action("最近对局结果",v->message("对局结果",world.contests.lastResult()));action("保存局面（3个槽位）",v->saveSlots(false));action("读取存档",v->saveSlots(true));action("导出当前存档",v->exportSave());action("导入存档文件",v->importSave());action("导入剧本文件",v->{if(!aiRunning)startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT_SCENARIO);});
        action("本旬结算摘要",v->showTurnReport());
        action("全国资料 / 核验目录",v->{ui.page="content";refresh();});action("势力一览",v->{ui.page="factions";refresh();});
        action("战报",v->message("战报",String.join("\n",world.log)));
        action("新游戏 / 选择势力",v->scenarioPicker());action("版本与范围",v->message("v"+BuildConfig.VERSION_NAME+" · 天下总览与快速旬结算", "构建 "+BuildConfig.VERSION_CODE+" · 源码 "+BuildConfig.SOURCE_REVISION+"\n全国势力/部队/武将/城池/港关/设施列表；左上角常驻行动力。开局支持实际地图点选势力与技巧树预览。\n旬结算单线程顺序执行规则，动画与计算解耦；默认8秒演示预算、10秒总耗时目标（主动暂停和完整演示除外）。不会截断AI或规则。\n收支为下旬定期收入与兵粮需求预测，不是所有命令开销的历史流水。战法暴击使用现有头像图集展示680ms高光。\n保留v51逐城精修与全部美术。西北绿洲道路、许新森林、吴附近曲阿港及邺晋间壶关；地形更新需新开局。\n下河须经过己方港口；目标任务支持持续攻击、攻占后进驻和连续修理，可随时停止。\n港口、关卡、城市使用不同攻城系数；器械伤害随兵力增长。围攻停止自然修复，主动补修降为¼。\n地块开发、城市出征与运输固定在面板顶部；结算期间仍可拖动地图与收起面板。\n栈道与山径按六方向连接，河岸连续描边。\n每座兵舍/生产设施每旬1次；枪戟弩共用锻冶所次数，战马使用厩舍。部队按2500兵一档显示1至5个模型，万人以上5个。新开局为开发基线。\n历史重建/定制剧本，完整官方数据及精确公式仍待核验。"));
    }
    private void scenarioPicker(){
        try {
            List<ScenarioCatalog.Summary> scenarios=ScenarioCatalog.summaries();
            LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(8),dp(4),dp(8),dp(4));
            ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(true);scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
            final AlertDialog[] holder=new AlertDialog[1];
            for(ScenarioCatalog.Summary scenario:scenarios){
                String label=scenario.name+" · "+scenario.sites+"据点 / "+scenario.officers+"将 / "+scenario.factions+"势力";
                Button option=button(label,v->{if(holder[0]!=null)holder[0].dismiss();chooseScenarioTemplate(scenario.id);});
                option.setAllCaps(false);option.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);option.setContentDescription("选择剧本 "+scenario.name);
                list.addView(option,new LinearLayout.LayoutParams(-1,dp(48)));
            }
            int rows=Math.min(5,Math.max(3,scenarios.size()));
    int target=dp(rows*56+16);
    int cap=Math.max(dp(180),getResources().getDisplayMetrics().heightPixels*58/100);
    LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);
    shell.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(target,cap)));
    holder[0]=new AlertDialog.Builder(this).setTitle("选择剧本 · 六个年代与自制沙盘").setView(shell).setNegativeButton("取消",null).show();
        }catch(IOException e){showError("剧本读取失败："+e.getMessage());}
    }
    private void chooseScenarioTemplate(String id){
        new Thread(()->{try{java.util.List<MapLibrary.Entry> entries=new MapLibrary(this).entries();runOnUiThread(()->{
            if(isFinishing()||isDestroyed())return;if(entries.isEmpty()){chooseScenarioTemplate(id,null);return;}
            String[] labels=new String[entries.size()+1];labels[0]="原版全国地图（始终保留）";for(int i=0;i<entries.size();i++)labels[i+1]=entries.get(i).label();
            new AlertDialog.Builder(this).setTitle("选择本局地图版本").setItems(labels,(d,n)->{if(n==0){chooseScenarioTemplate(id,null);return;}
                new Thread(()->{try{MapPatch pinned=new MapLibrary(this).load(entries.get(n-1));runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())chooseScenarioTemplate(id,pinned);});}catch(IOException e){runOnUiThread(()->showError("地图版本读取失败："+e.getMessage()));}},"custom-map-version").start();
            }).setNegativeButton("取消",null).show();
        });}catch(IOException e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle("自定义地图库读取失败").setMessage(e.getMessage()+"；原文件保留，原版仍可选择。").setPositiveButton("使用原版",(d,n)->chooseScenarioTemplate(id,null)).setNegativeButton("取消",null).show();});}},"custom-map-list").start();
    }
    private void chooseScenarioTemplate(String id,MapPatch pinned){
        java.util.concurrent.atomic.AtomicBoolean canceled=new java.util.concurrent.atomic.AtomicBoolean();
        AlertDialog loading=new AlertDialog.Builder(this).setMessage("正在读取剧本…").setNegativeButton("取消",(d,n)->canceled.set(true)).create();
        loading.setOnCancelListener(d->canceled.set(true));loading.show();
        new Thread(()->{try {World template=pinned==null?ScenarioCatalog.load(id,0):CustomMaps.preview(pinned,id);if(pinned!=null)for(CustomMaps.Issue issue:CustomMaps.diagnose(template,pinned,id))if(issue.blocking())throw new IOException(issue.message());runOnUiThread(()->{
            if(isFinishing()||isDestroyed()||canceled.get())return;loading.dismiss();
            new ScenarioFactionPicker(this,template,side->confirm(openingInfo(template,side)+"\n\n以"+template.faction(side)+"开始新局？当前自动存档将更新，手动存档保留；损坏的自动存档会另存备份。",()->startScenario(template.scenarioId,side,pinned))).show();
        });}catch(IOException e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&!canceled.get()){loading.dismiss();showError("剧本读取失败："+e.getMessage());}});}},"scenario-preview").start();
    }

    private String openingInfo(World w,int side){
        StringBuilder b=new StringBuilder(w.date()+" · "+w.width+"×"+w.height+"格\n");int people=0;for(World.Officer o:w.officers)if(o.owner==side)people++;
        if(!w.customMapId.isEmpty())b.append("自定义地图：").append(w.customMapName).append(" r").append(w.customMapRevision).append("（版本随存档固定）\n");
        b.append(people).append("名武将 · 领地：");for(World.City c:w.cities)if(c.owner==side)b.append(c.name).append(" ");
        b.append(w.dataSource.equals("community-reference")?"\n能力/适性来自公开资料；地图、领地与资源为历史重建或定制。群英类跨时代配置不按生卒年退场。":"\n重建或定制开局；官方完整资源、事件与归属仍待核验。");return b.toString();
    }
    private void startScenario(String id,int player){startScenario(id,player,null);}
    private void startScenario(String id,int player,MapPatch pinned){
        java.util.concurrent.atomic.AtomicBoolean canceled=new java.util.concurrent.atomic.AtomicBoolean();
        AlertDialog loading=new AlertDialog.Builder(this).setMessage("正在建立新局…").setNegativeButton("取消",(d,n)->canceled.set(true)).create();loading.setOnCancelListener(d->canceled.set(true));loading.show();
        new Thread(()->{try{World resolved=pinned==null?ScenarioCatalog.load(id,player,System.nanoTime()):CustomMaps.load(pinned,id,player,System.nanoTime());World next=CustomOfficerSetup.apply(this,resolved);runOnUiThread(()->{
            if(isFinishing()||isDestroyed()||canceled.get())return;loading.dismiss();if(!activateWorld(next))return;selectAndFocus(world.home().hex);map.switchMode(nextScenario3D);closePanel();save("auto",false);
        });}catch(IOException e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&!canceled.get()){loading.dismiss();showError("无法开始剧本："+e.getMessage());}});}},"scenario-start").start();
    }
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
    private void showTurnReport(){new BattleReportUi(this,world).show(Math.max(0,world.turn-1));}
    private void showAttention(){
        List<UiModels.Attention> items=UiModels.attention(world);
        if(items.isEmpty()){message("当前待处理","当前没有缺粮、受威胁或受阻异常。");return;}
        new AlertDialog.Builder(this).setTitle("当前待处理 · 点选定位").setItems(items.stream().map(item->item.text).toArray(String[]::new),(d,n)->{
            UiModels.Attention item=items.get(n);World.Unit unit=world.unit(item.unitId);World.City city=world.city(item.cityId);
            if(unit!=null&&unit.owner==world.player)selectObject(unit.hex,unit.id,true);
            else if(city!=null&&city.owner==world.player)selectObject(city.hex,-2,true);
            else message("对象已变化",item.text+"\n当前对象已消失或归属改变，保留记录供查看。");
        }).setNegativeButton("返回",null).show();
    }
    private void confirmTurn(){
        if(aiRunning||world.gameOver()||world.commandsBlocked())return;int idle=0;for(World.City c:world.cities)if(c.owner==world.player)idle+=world.idle(c).size();
        confirm("结束 "+world.date()+"？\n还有 "+idle+" 名闲置武将、"+world.actionPoints[world.player]+" 点行动力。\n将执行电脑行动，推进建设、调动、运输与自动行军，并自动保存。",this::advanceTurn);
    }
    private void advanceTurn(){if(aiRunning||world.gameOver()||world.commandsBlocked())return;turnWork=new TurnWork(world);long saving=android.os.SystemClock.elapsedRealtime();save("auto",false);turnWork.saveMillis=android.os.SystemClock.elapsedRealtime()-saving;aiRunning=true;turnWork.speed=getPreferences(MODE_PRIVATE).getInt("turnPlaybackSpeed",1);turnWork.observe(this::finishTurn);refresh();turnWork.start();}
    private void finishTurn(){
        if(turnWork==null||isFinishing()||isDestroyed())return;
        if(turnWork.error!=null){
            if(playback!=null){playback.detach();playback=null;}map.replayFrame(null,0);
            turnWork.cancel();turnWork=null;aiRunning=false;nextTurn.setOnClickListener(v->confirmTurn());
            refresh();showError("回合结算失败，原局面保留");return;
        }
        // Never save the render world or a partially computed faction. Commit only the complete turn.
        if(turnWork.done&&!turnWork.savedFinal){long began=android.os.SystemClock.elapsedRealtime();turnWork.savedFinal=save("auto",false);turnWork.saveMillis+=android.os.SystemClock.elapsedRealtime()-began;}
        if(turnWork.visual==null||playback!=null)return;
        playback=new TurnPlayback(map,turnWork,()->refreshTurnProgress(),this::completeTurnPlayback);
        nextTurn.setText("演示控制");nextTurn.setEnabled(true);nextTurn.setOnClickListener(v->showPlaybackControls());
        playback.start();refreshTurnProgress();
    }
    private void completeTurnPlayback(){
        if(turnWork==null)return;TurnWork completed=turnWork;
        if(playback!=null){playback.detach();playback=null;}map.replayFrame(null,0);
        completed.observe(null);turnWork=null;aiRunning=false;world=completed.after;
        if(world.life.pending()){ui.page="map";ui.panelVisible=true;}
        ui.summary=completed.summary+"\n\n视野内已演示行动（"+completed.visibleCount+"项）\n"+completed.actionReport;
        pendingMarch=null;if(world.unit(moving)!=null)selected=world.unit(moving).hex;else moving=-1;
        nextTurn.setOnClickListener(v->confirmTurn());refresh();if(!completed.savedFinal)save("auto",false);
        completed.totalMillis=completed.activeMillis();lastTurnWallMillis=completed.totalMillis;lastTurnComputeMillis=completed.computeMillis;
        ui.summary+="\n\n本旬耗时 "+String.format(java.util.Locale.ROOT,"%.2f",completed.totalMillis/1000.0)+" 秒 · 纯运算 "+String.format(java.util.Locale.ROOT,"%.2f",completed.computeMillis/1000.0)+" 秒 · 存档 "+completed.saveMillis+"ms"
            +"\n默认演示预算8秒，10秒为总耗时目标；不跳过规则。"+(completed.fastForward()?"本轮已压缩剩余演示。":"")+"\n用户主动暂停时间不计入此统计。\n\n阶段运算记录\n"+completed.timings;
        android.util.Log.i("Turn52","TURN_END totalMs="+lastTurnWallMillis+" computeMs="+lastTurnComputeMillis+" saveMs="+completed.saveMillis+" factions="+world.factions.length);
        turnBanner.setText("旬结算完成 · "+String.format(java.util.Locale.ROOT,"%.1f",completed.totalMillis/1000.0)+"秒 · 点此查看战报");turnBanner.setVisibility(View.VISIBLE);
    }
    private void showPlaybackControls(){
        if(playback==null||turnWork==null)return;
        new AlertDialog.Builder(this).setTitle("回合行动演示 · 默认总演示预算8秒")
            .setItems(new String[]{turnWork.paused?"继续演示":"暂停演示","1× 正常速度","2× 加速","4× 快速","跳过剩余演示","完整演示（本旬不限时）"},(d,index)->{
                if(playback==null||turnWork==null)return;
                if(index==0)playback.pause(!turnWork.paused);
                else if(index==4)playback.skip();
                else if(index==5){turnWork.fullReplay=true;turnWork.skipAnimations=false;}
                else {turnWork.speed=1<<(index-1);getPreferences(MODE_PRIVATE).edit().putInt("turnPlaybackSpeed",turnWork.speed).apply();}
                refreshTurnProgress();
            }).setNegativeButton("返回地图",null).show();
    }
    private World authoritativeSaveWorld(){return turnWork!=null&&turnWork.done&&turnWork.error==null?turnWork.after:world;}
    @Override public Object onRetainNonConfigurationInstance(){if(playback!=null)playback.detach();if(turnWork!=null)turnWork.observe(null);return turnWork;}
    @Override protected void onDestroy(){if(map!=null)map.release();if(playback!=null)playback.detach();if(turnProgress!=null)turnProgress.removeCallbacks(turnProgressTicker);if(turnWork!=null){turnWork.observe(null);if(!isChangingConfigurations())turnWork.cancel();}if(confirmationDialog!=null)confirmationDialog.dismiss();super.onDestroy();}
    private void writeClientState(Bundle state){
        if(world==null||map==null)return;
        ui.write(state);state.putInt("selectedQ",selected==null?-1:selected.q);state.putInt("selectedR",selected==null?-1:selected.r);state.putInt("moving",moving);state.putString("unitCommand",unitCommand);
        if(pendingMarch!=null&&pendingMarch.target!=null){state.putInt("routeQ",pendingMarch.target.q);state.putInt("routeR",pendingMarch.target.r);state.putBoolean("routeMove",pendingMarch.order!=null&&pendingMarch.order.intent==MarchOrders.Intent.MOVE);state.putInt("routeCity",pendingMarch.order!=null&&pendingMarch.order.kind==MarchOrders.Kind.CITY?pendingMarch.order.targetId:-1);}map.saveCamera(state);
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);writeClientState(state);save("auto",false);}
    private String worldDigest()throws Exception{return android.util.Base64.encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(authoritativeSaveWorld())),android.util.Base64.NO_WRAP);}
    private void persistClientState(){
        if(map==null)return;android.os.Parcel parcel=android.os.Parcel.obtain();
        try{Bundle state=new Bundle();writeClientState(state);parcel.writeBundle(state);String data=android.util.Base64.encodeToString(parcel.marshall(),android.util.Base64.NO_WRAP);if(!getPreferences(MODE_PRIVATE).edit().putString("clientWorld",worldDigest()).putString("clientState",data).commit())android.util.Log.w("UiRecovery","Could not persist UI hints");}
        catch(Exception e){android.util.Log.w("UiRecovery","Could not encode UI hints",e);}finally{parcel.recycle();}
    }
    private Bundle readClientState(){
        android.os.Parcel parcel=android.os.Parcel.obtain();
        try{if(!worldDigest().equals(getPreferences(MODE_PRIVATE).getString("clientWorld",""))){android.util.Log.i("UiRecovery","No UI hints matching authoritative world");return null;}String encoded=getPreferences(MODE_PRIVATE).getString("clientState","");if(encoded.length()>100000)return null;byte[] bytes=android.util.Base64.decode(encoded,android.util.Base64.DEFAULT);parcel.unmarshall(bytes,0,bytes.length);parcel.setDataPosition(0);return parcel.readBundle(getClassLoader());}
        catch(Exception e){android.util.Log.w("UiRecovery","Could not restore UI hints",e);return null;}finally{parcel.recycle();}
    }
    private AtomicFile file(String slot){return new AtomicFile(new File(getFilesDir(),slot+".sg11"));}
    private boolean save(String slot,boolean announce){
        if(world==null||unreadableAutosave)return false;
        AtomicFile f=file(slot);FileOutputStream out=null;try{byte[] bytes=SaveCodec.encode(authoritativeSaveWorld());new MapLibrary(this).rememberVisual(authoritativeSaveWorld());out=f.startWrite();out.write(bytes);f.finishWrite(out);if(announce)Toast.makeText(this,"局面已保存",Toast.LENGTH_SHORT).show();return true;}
        catch(IOException e){if(out!=null)f.failWrite(out);Toast.makeText(this,"保存失败，请检查设备存储空间后重试",Toast.LENGTH_LONG).show();return false;}
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
                new AlertDialog.Builder(this).setTitle("导入剧本 · 选择势力").setItems(imported.factions,(dialog,n)->confirm("开始“"+imported.scenarioName+"”？\n"+imported.faction(n)+" · "+imported.sourceColumns()+"×"+imported.sourceRows()+"格 · "+imported.cities.size()+"据点 · "+imported.officers.size()+"武将\n将替换当前局面及自动存档，手动槽位保留。",()->{
                    if(aiRunning)return;imported.player=n;imported.active=n;if(!activateWorld(imported))return;selectAndFocus(world.home().hex);save("auto",false);
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
                byte[] bytes=SaveCodec.encode(world);if(world.visualMap!=null&&(!world.visualMap.heights.isEmpty()||!world.visualMap.appearances.isEmpty()))Toast.makeText(this,"旧格式战局不包含3D视觉属性；请同时从地图编辑器导出地图JSON",Toast.LENGTH_LONG).show();
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
                    if(!activateWorld(imported))return;ui.summary="";ui.city=-1;ui.owner=-1;ui.query="";ui.cityQuery="";ui.cityOwner=-1;ui.taskQuery="";ui.taskType=0;
                    selectAndFocus(world.home().hex);save("auto",false);
                });
            }
        }catch(IOException|SecurityException e){showError(request==EXPORT_SAVE?"导出失败":"导入失败");}
    }
    private void loadSlot(String slot){try{World restored=readSave(file(slot));if(!activateWorld(restored))return;selectAndFocus(world.home().hex);save("auto",false);Toast.makeText(this,"已读取存档 · "+world.date(),Toast.LENGTH_SHORT).show();}catch(IOException e){showError("读取失败");}}
    @Override protected void onResume(){super.onResume();if(map!=null)map.resume(true);}
    @Override protected void onPause(){if(map!=null)map.resume(false);super.onPause();if(world!=null){save("auto",false);persistClientState();}}
    @Override public void onBackPressed(){
        if(criticalFlash!=null){criticalFlash.dismiss();return;}
        if(world==null){finish();return;}
        if(aiRunning)return;
        if(tacticPreview!=null){clearTacticPreview();refresh();return;}
        if(reportReturn!=null){returnFromBattleReport();return;}
        if(mapPick!=null){cancelMapPick();return;}
        if(pendingMarch!=null){pendingMarch=null;unitCommand="select";refresh();return;}
        if(!unitCommand.equals("select")){unitCommand="select";refresh();return;}
        if(!ui.page.equals("map")||ui.panelVisible){closePanel();return;}
        if(moving>=0){clearUnitSelection();return;}
        if(ui.returnToCities){returnToCities();return;}
        confirm("退出游戏？当前局面将自动保存。",this::finish);
    }
}

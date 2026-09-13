package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
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
    private LinearLayout panel,root,body;
    private FrameLayout panelHost;
    private ScrollView panelScroll;
    private TextView title,log;
    private Hex selected;
    private int moving=-1;
    private boolean aiRunning;
    private Button nextTurn;
    private final ClientState ui=new ClientState();
    private TurnWork turnWork;
    private final Map<String,Button> navigation=new LinkedHashMap<>();
    final int ink=Color.rgb(17,32,37),paper=Color.rgb(235,227,205),gold=Color.rgb(216,183,116),muted=Color.rgb(175,190,185);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);world=DemoScenario.create();ui.read(state);
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
        title=text("",14,gold);title.setMaxLines(2);header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        header.addView(button("全图",v->map.fit()),new LinearLayout.LayoutParams(dp(56),dp(48)));
        header.addView(button("定位",v->{if(selected!=null)map.focus(selected);}),new LinearLayout.LayoutParams(dp(56),dp(48)));
        root.addView(header);
        body=new LinearLayout(this);map=new MapView(this,this::onTile);body.addView(map,new LinearLayout.LayoutParams(0,-1,1));
        panelHost=new FrameLayout(this);body.addView(panelHost,new LinearLayout.LayoutParams(dp(panelWidth()),-1));
        panelScroll=new ScrollView(this);panelScroll.setFillViewport(true);panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(12),dp(4),dp(12),dp(10));panelScroll.addView(panel);
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        log=text("",11,muted);log.setMaxLines(1);log.setPadding(dp(12),0,dp(12),0);root.addView(log,new LinearLayout.LayoutParams(-1,dp(22)));
        LinearLayout bottom=new LinearLayout(this);bottom.setPadding(dp(6),0,dp(6),0);bottom.setGravity(Gravity.CENTER_VERTICAL);
        String[] keys={"map","cities","officers","tasks","menu"},labels={"地图","城市","武将","任务","菜单"};
        for(int i=0;i<keys.length;i++){final String page=keys[i];Button b=button(labels[i],v->{if(page.equals("map")){ui.page="map";ui.panelVisible=!ui.panelVisible;}else{ui.page=page;ui.panelVisible=true;}refresh();revealPanel();});b.setContentDescription("导航 · "+labels[i]);navigation.put(page,b);bottom.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));}
        nextTurn=button("下一旬  →",v->confirmTurn());bottom.addView(nextTurn,new LinearLayout.LayoutParams(dp(122),dp(52)));root.addView(bottom);
        setContentView(root);root.requestApplyInsets();selected=world.home()==null?null:world.home().hex;
        if(state!=null){Hex h=new Hex(state.getInt("selectedQ",-1),state.getInt("selectedR",-1));if(world.inside(h))selected=h;moving=state.getInt("moving",-1);}
        refresh();if(state!=null)map.restoreCamera(state);
        if(turnWork!=null)turnWork.observe(this::finishTurn);
        if(!restored&&restoreError==null&&state==null)root.post(this::scenarioPicker);
        if(restoreError!=null)message("自动存档未能读取",restoreError);
    }
    private int panelWidth(){return Math.min(344,Math.max(240,Math.round(getResources().getConfiguration().screenWidthDp*.38f)));}
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);panelHost.getLayoutParams().width=dp(panelWidth());panelHost.requestLayout();}
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    Button button(String value,View.OnClickListener action){Button b=new Button(this);b.setText(value);b.setTextSize(13);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(4),0,dp(4),0);b.setTextColor(paper);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(38,58,63)));b.setOnClickListener(v->{if(!aiRunning)action.onClick(v);});return b;}
    private void line(String value,int size,int color){TextView t=text(value,size,color);t.setPadding(0,dp(4),0,dp(4));panel.addView(t);}
    private void action(String label,View.OnClickListener click){Button b=button(label,click);b.setEnabled(!aiRunning);panel.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));}
    private void onTile(Hex h) {
        if(h==null||aiRunning)return;
        World.Unit target=world.unitAt(h);World.City city=world.cityAt(h);World.Unit source=world.unit(moving);
        if(source!=null&&source.owner==world.player) {
            if(target!=null&&target.owner!=world.player){warUi().attack(source,target);return;}
            if(world.war.at(h)!=null){confirm("攻击军事设施？",()->apply(world.war.attackStructure(source.id,h)));return;}
            if(city!=null){
                if(city.owner==world.player)confirm("进入"+city.name+"并归还兵装与粮草？",()->{World.Result result=world.enter(source.id,city.id);if(result.ok)moving=-1;apply(result);});
                else confirm("攻击"+city.name+"？",()->apply(world.siege(source.id,city.id)));
                return;
            }
            if(target==null){World.Result result=world.move(source.id,h);if(result.ok)selected=h;apply(result);return;}
        }
        if(!h.equals(selected))ui.group="概览";
        selected=h;moving=target!=null&&target.owner==world.player?target.id:-1;ui.page="map";ui.panelVisible=true;refresh();revealPanel();
    }
    private void message(String title,String value){new AlertDialog.Builder(this).setTitle(title).setMessage(value).setPositiveButton("返回",null).show();}
    private void confirm(String value,Runnable action){new AlertDialog.Builder(this).setMessage(value).setPositiveButton("执行",(d,w)->{if(!aiRunning)action.run();}).setNegativeButton("取消",null).show();}
    private void apply(World.Result result){
        if(!result.ok)message("命令未执行",result.message);
        if(result.ok&&moving>=0&&world.unit(moving)!=null)selected=world.unit(moving).hex;
        refresh();if(result.ok)save("auto",false);
        if(result.ok&&world.gameOver())message(world.winner==world.player?"战场胜利":"战场战败","本局结束，可从菜单重新选择剧本。");
    }
    void refresh(){
        title.setText(world.scenarioName+"  ·  "+world.faction(world.player)+"    "+world.date()+"    行动力 "+world.actionPoints[world.player]);
        nextTurn.setEnabled(!aiRunning&&!world.gameOver());nextTurn.setText(aiRunning?"结算中…":"下一旬  →");
        for(Map.Entry<String,Button> e:navigation.entrySet()){e.getValue().setEnabled(!aiRunning);e.getValue().setTextColor(e.getKey().equals(ui.page)?gold:paper);}
        navigation.get("tasks").setText("任务"+(taskCount()>0?" "+taskCount():""));
        panelHost.removeAllViews();panel.removeAllViews();panelHost.setVisibility(ui.panelVisible?View.VISIBLE:View.GONE);
        if(world.unit(moving)==null)moving=-1;
        if(ui.page.equals("cities"))panelHost.addView(new OverviewUi(this,world,ui).cities());
        else if(ui.page.equals("officers"))panelHost.addView(new OverviewUi(this,world,ui).officers());
        else if(ui.page.equals("tasks"))panelHost.addView(new OverviewUi(this,world,ui).tasks());
        else {panelHost.addView(panelScroll);if(ui.page.equals("menu"))showMenu();else showSelection();}
        log.setText(aiRunning?"正在结算电脑行动与本旬任务…":world.log.isEmpty()?"拖动地图 · 双指缩放 · 双击城池定位":world.log.get(world.log.size()-1));
        map.setWorld(world,selected,moving);map.setEnabled(!aiRunning);
    }
    private int taskCount(){int n=0;for(Domestic.Facility f:world.domestic.facilities)if(f.remaining>0&&world.city(f.cityId).owner==world.player)n++;for(Domestic.Mission m:world.domestic.missions)if(m.owner==world.player)n++;for(Campaign.Project p:world.campaign.projects())if(p.owner==world.player)n++;return n;}
    private void showSelection(){
        World.Unit unit=selected==null?null:world.unitAt(selected);World.City city=selected==null?null:world.cityAt(selected);
        if(unit!=null)showUnit(unit);else if(city!=null)showCity(city);else if(selected!=null&&world.domestic.at(selected)!=null){
            Domestic.Facility f=world.domestic.at(selected);line(f.kind.label,24,gold);line(world.city(f.cityId).name,15,paper);line(f.remaining==0?f.kind.effect:"建设中 · 剩"+f.remaining+"旬",14,paper);
            action("设施详情 / 管理",v->domesticUi().facility(f));action("返回所属城池",v->selectAndFocus(world.city(f.cityId).hex));
        }else if(selected!=null&&world.war.at(selected)!=null){War.Structure s=world.war.at(selected);line(s.kind.label,24,gold);line(world.faction(s.owner)+" · 耐久"+s.hp,15,paper);line(s.kind.effect,14,paper);}
        else {line("山河之间",23,gold);if(selected!=null&&world.war.fireAt(selected)!=null)line("火场 · 剩"+world.war.fireAt(selected).remaining+"旬",16,gold);line("点选城池或部队",15,paper);line("单指拖动 · 双指缩放\n双击城池聚焦\n缩小时查看势力，放大查看设施与在途。",14,paper);action("定位本城",v->{if(world.home()!=null)selectAndFocus(world.home().hex);});}
    }
    private void showCity(World.City c){
        boolean compact=!ui.group.equals("概览");
        line(c.name,compact?21:25,gold);line(world.faction(c.owner)+" · 太守 "+UiModels.governor(world,c.id),compact?12:13,paper);
        line(compact?"金 "+c.gold+" · 粮 "+c.food+" · 兵 "+c.troops:"金 "+c.gold+"    粮 "+c.food+"\n兵 "+c.troops+"    城防 "+c.defense,compact?13:15,paper);
        HorizontalScrollView tabs=new HorizontalScrollView(this);tabs.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);tabs.addView(row);
        for(String name:new String[]{"概览","内政","武将","军事","调动","外交","研究"}){Button b=button(name,v->{ui.group=name;refresh();revealPanel();});b.setTextColor(ui.group.equals(name)?gold:paper);row.addView(b,new LinearLayout.LayoutParams(dp(48),dp(48)));}panel.addView(tabs);tabs.post(()->{int index=Arrays.asList("概览","内政","武将","军事","调动","外交","研究").indexOf(ui.group);tabs.smoothScrollTo(Math.max(0,dp(index*48)-dp(90)),0);});
        boolean own=c.owner==world.player&&!world.gameOver();
        switch(ui.group){
            case "内政":
                line("设施 "+world.domestic.count(c.id)+"/"+Domestic.CITY_SLOTS+"\n每月收入：金 "+world.domestic.monthlyGold(c.id)+" / 粮 "+world.domestic.monthlyFood(c.id),14,paper);
                boolean construction=false;
                for(Domestic.Facility f:world.domestic.facilities)if(f.cityId==c.id){construction|=f.remaining>0;action(f.kind.label+" · "+(f.remaining==0?"已建成":"剩"+f.remaining+"旬"),v->domesticUi().facility(f));}
                if(!construction)line("当前没有建设中的设施",13,muted);
                if(own)action("设施开发",v->domesticUi().build(c));
                if(own)action("商人 / 粮食买卖",v->campaignUi().trade(c));
                if(own)action("巡察 · 金100",v->strategyUi().command(c,5));break;
            case "外交":
                if(own){action("外交 / 协定",v->campaignUi().diplomacy(c));action("流言",v->campaignUi().rumor(c));}
                for(int side=0;side<world.factions.length;side++)if(side!=c.owner&&c.owner>=0&&world.alive(side))line(world.faction(side)+"\n"+world.campaign.relationLabel(c.owner,side),14,paper);break;
            case "研究":
                line("技巧点 "+world.campaign.points(c.owner),18,gold);
                if(own){action("技巧研究",v->campaignUi().research(c));action("能力 / 适性培养",v->campaignUi().study(c));}
                action("研究与培养进度",v->campaignUi().projects(c));break;
            case "武将":
                if(own){
                    action("搜索人才",v->strategyUi().command(c,1));
                    action("登用武将",v->strategyUi().command(c,2));
                    action("褒奖武将",v->strategyUi().command(c,3));
                    action("任命太守",v->strategyUi().command(c,4));
                    action("能力 / 适性培养",v->campaignUi().study(c));
                    action("流放武将",v->campaignUi().dismiss(c));
                }
                line("可用武将 "+world.idle(c).size()+" / 驻扎 "+UiModels.officerCount(world,c.id),14,paper);
                for(World.Officer o:world.officers)if(o.cityId==c.id)action(o.name+" · "+o.role.label+" · "+UiModels.status(world,o),v->officerDetail(o));
                if(UiModels.officerCount(world,c.id)==0)line("当前没有驻扎武将",14,muted);
                action("筛选本城武将",v->{ui.city=c.id;ui.owner=-1;ui.query="";ui.page="officers";refresh();});break;
            case "军事":
                line("治安 "+c.order+" · 气力 "+c.morale+" · 兵源 "+c.recruitReserve,13,paper);
                if(own)for(CityCommand command:militaryCommands(c))action(command.label,v->command.run.run());
                else line("仅可在己方城池下达军令",14,muted);
                if(own){action("修复城防",v->campaignUi().repair(c));action("建造军事设施",v->campaignUi().buildMilitary(c));action("军事设施管理",v->campaignUi().structures(c));}break;
            case "调动":
                if(own){action("人员调动",v->domesticUi().transfer(c));action("资源运输",v->domesticUi().transport(c));}else line("仅可从己方城池派遣",14,muted);
                action("查看任务 / 在途",v->{ui.page="tasks";refresh();});break;
            default:
                line("治安 "+c.order+" · 气力 "+c.morale+" · 兵源 "+c.recruitReserve+"\n可用武将 "+world.idle(c).size()+" · 驻扎 "+UiModels.officerCount(world,c.id),14,paper);
                if(own)action("人事 / 城市治理",v->strategyUi().city(c));
                StringBuilder stocks=new StringBuilder("兵装库存\n");for(World.Weapon weapon:World.Weapon.values())stocks.append(weapon.label).append(" ").append(c.equipment[weapon.ordinal()]).append("  ");line(stocks.toString(),13,paper);
                line("月收入 金 "+world.domestic.monthlyGold(c.id)+" / 粮 "+world.domestic.monthlyFood(c.id),13,muted);
                if(own)action("设施开发",v->domesticUi().build(c));
                action("展开军事命令",v->{ui.group="军事";refresh();revealPanel();});
                if(own){action("外交 / 协定",v->campaignUi().diplomacy(c));action("技巧 / 培养",v->{ui.group="研究";refresh();revealPanel();});}
        }
    }
    private static final class CityCommand {final String label;final Runnable run;CityCommand(String label,Runnable run){this.label=label;this.run=run;}}
    private List<CityCommand> militaryCommands(World.City c){return Arrays.asList(
        new CityCommand("出征",()->chooseOfficer(c,o->chooseWeapon(weapon->new AlertDialog.Builder(this).setTitle("出征兵力").setItems(new String[]{"3000人","5000人","8000人"},(d,which)->{
            World.Result result=world.deploy(c.id,o.id,weapon,new int[]{3000,5000,8000}[which]);if(result.ok){World.Unit u=world.unit(o.unitId);selected=u.hex;moving=u.id;}apply(result);
        }).show()))),
        new CityCommand("征兵 · 金300 · 兵源 "+c.recruitReserve,()->strategyUi().command(c,6)),
        new CityCommand("训练 · 金100",()->strategyUi().command(c,7)),
        new CityCommand("生产兵装  +"+world.domestic.produceAmount(c.id)+" · 金400",()->chooseOfficer(c,o->chooseWeapon(weapon->apply(world.produce(c.id,o.id,weapon)))))
    );}
    private void showUnit(World.Unit u){
        World.Officer o=world.officer(u.officerId);line(o.name,25,gold);line(world.faction(u.owner)+" · "+u.weapon.label,14,paper);
        line("兵力 "+u.troops+"\n携粮 "+u.food+"\n气力 "+u.energy,16,paper);line("统率 "+o.leadership+"  武力 "+o.war,13,paper);line("移动 "+world.war.movement(u)+"  射程 "+world.war.range(u),13,paper);
        line("适性 "+War.rankLabel(o.aptitude[u.weapon.ordinal()])+" · 状态 "+u.status.label,14,paper);
        if(u.owner==world.player){line(u.acted?"本旬已行动":"点击高亮空地移动\n点敌军攻击 / 点城池攻城或入城",14,paper);
            if(!u.acted&&u.status==War.Status.NORMAL){action("战法",v->{moving=u.id;warUi().tactics(u);});action("部队计略",v->{moving=u.id;warUi().plots(u);});action("待命 · 恢复5气力",v->confirm("本旬待命并恢复5气力？",()->apply(world.war.waitUnit(u.id))));}
            action("取消部队选择",v->{moving=-1;selected=null;refresh();});}
    }
    void officerDetail(World.Officer o){
        String stats="统率 "+o.leadership+"    武力 "+o.war+"\n智力 "+o.intelligence+"    政治 "+o.politics+"\n魅力 "+o.charm;
        stats+="\n适性：枪"+War.rankLabel(o.aptitude[0])+" 戟"+War.rankLabel(o.aptitude[1])+" 弩"+War.rankLabel(o.aptitude[2])+" 骑"+War.rankLabel(o.aptitude[3]);
        AlertDialog.Builder d=new AlertDialog.Builder(this).setTitle(o.name+" · "+UiModels.faction(world,o)).setMessage(stats+"\n身份："+o.role.label+" · 忠诚 "+o.loyalty+"\n\n所在地："+UiModels.location(world,o)+"\n状态："+UiModels.status(world,o)).setNegativeButton("返回",null);
        Hex h=o.cityId>=0?world.city(o.cityId).hex:o.unitId>=0&&world.unit(o.unitId)!=null?world.unit(o.unitId).hex:null;
        for(Domestic.Mission m:world.domestic.missions)if(m.officerId==o.id)h=m.hex;
        final Hex target=h;if(target!=null)d.setPositiveButton("地图定位",(dialog,n)->selectAndFocus(target));d.show();
    }
    private interface OfficerChoice {void choose(World.Officer officer);}
    private interface WeaponChoice {void choose(World.Weapon weapon);}
    private void chooseOfficer(World.City c,OfficerChoice callback){
        List<World.Officer> options=world.idle(c);if(options.isEmpty()){message("武将不足","没有本旬可行动的在城武将");return;}
        String[] names=new String[options.size()];for(int i=0;i<names.length;i++)names[i]=options.get(i).name;
        new AlertDialog.Builder(this).setTitle("执行武将").setItems(names,(d,index)->callback.choose(options.get(index))).setNegativeButton("取消",null).show();
    }
    private void chooseWeapon(WeaponChoice callback){String[] labels=new String[World.Weapon.values().length];for(int i=0;i<labels.length;i++)labels[i]=World.Weapon.values()[i].label;new AlertDialog.Builder(this).setTitle("选择兵种").setItems(labels,(d,index)->callback.choose(World.Weapon.values()[index])).setNegativeButton("取消",null).show();}
    private void revealPanel(){panelScroll.post(()->panelScroll.scrollTo(0,0));}
    void selectAndFocus(Hex h){if(h==null)return;moving=-1;selected=h;ui.page="map";ui.panelVisible=true;ui.group="概览";refresh();map.focus(h);revealPanel();}
    private StrategyUi strategyUi(){return new StrategyUi(this,world,this::apply);}
    private CampaignUi campaignUi(){return new CampaignUi(this,world,this::apply);}
    private WarUi warUi(){return new WarUi(this,world,this::apply);}
    DomesticUi domesticUi(){return new DomesticUi(this,world,this::apply,this::selectAndFocus);}
    private void showMenu(){
        line("军政菜单",22,gold);action("保存局面（3个槽位）",v->saveSlots(false));action("读取存档",v->saveSlots(true));
        action("本旬结算摘要",v->message("旬结算摘要",ui.summary.isEmpty()?"结束一旬后将在这里显示结算摘要。":ui.summary));
        action("战报",v->message("战报",String.join("\n",world.log)));
        action("新游戏 / 选择势力",v->scenarioPicker());action("版本与范围",v->message("0.6 · 军政与战法","同图战法、部队计略、火场与军事设施；外交协定、商人、技巧研究、能力培养、设施合并。\n存档 v5，兼容 v1～v4。\n\n当前为原创沙盘与工程规则，尚未达到原版完整还原。全国地图、全量人物、全特技、官方剧本事件等仍未完成。"));
    }
    private void scenarioPicker(){
        try {List<World> scenarios=ScenarioCatalog.all();String[] labels=new String[scenarios.size()];for(int i=0;i<labels.length;i++){World w=scenarios.get(i);labels[i]=w.scenarioName+" · "+w.cities.size()+"城 / "+w.officers.size()+"将 / "+w.factions.length+"势力";}
            new AlertDialog.Builder(this).setTitle("选择剧本 · 原创测试沙盘").setItems(labels,(dialog,index)->{World template=scenarios.get(index);new AlertDialog.Builder(this).setTitle(template.scenarioName+" · 选择势力").setItems(template.factions,(d,side)->confirm("以"+template.faction(side)+"开始新局？当前自动存档将更新，手动存档保留。",()->startScenario(template.scenarioId,side))).setNegativeButton("返回",(d,n)->scenarioPicker()).show();}).setNegativeButton("取消",null).show();
        }catch(IOException e){showError("剧本读取失败");}
    }
    private void startScenario(String id,int player){try{world=ScenarioCatalog.load(id,player);ui.summary="";ui.city=-1;ui.owner=-1;ui.query="";ui.taskType=0;selectAndFocus(world.home().hex);save("auto",false);}catch(IOException e){showError("无法开始剧本");}}
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
        if(aiRunning||world.gameOver())return;int idle=0;for(World.City c:world.cities)if(c.owner==world.player)idle+=world.idle(c).size();
        confirm("结束 "+world.date()+"？\n还有 "+idle+" 名闲置武将、"+world.actionPoints[world.player]+" 点行动力。\n将执行电脑行动，推进建设、调动与运输，并自动保存。",this::advanceTurn);
    }
    private void advanceTurn(){if(aiRunning||world.gameOver())return;aiRunning=true;turnWork=new TurnWork(world);turnWork.observe(this::finishTurn);refresh();turnWork.start();}
    private void finishTurn(){
        if(turnWork==null||!turnWork.done||isFinishing()||isDestroyed())return;TurnWork completed=turnWork;completed.observe(null);turnWork=null;aiRunning=false;
        if(completed.error!=null){refresh();showError("回合结算失败");return;}
        world=completed.after;ui.summary=completed.summary;moving=-1;refresh();save("auto",false);message("旬结算摘要",ui.summary);
    }
    @Override public Object onRetainNonConfigurationInstance(){if(turnWork!=null)turnWork.observe(null);return turnWork;}
    @Override protected void onDestroy(){if(turnWork!=null)turnWork.observe(null);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);ui.write(state);if(selected!=null){state.putInt("selectedQ",selected.q);state.putInt("selectedR",selected.r);}state.putInt("moving",moving);map.saveCamera(state);save("auto",false);}
    private AtomicFile file(String slot){return new AtomicFile(new File(getFilesDir(),slot+".sg11"));}
    private void save(String slot,boolean announce){
        AtomicFile f=file(slot);FileOutputStream out=null;try{byte[] bytes=SaveCodec.encode(world);out=f.startWrite();out.write(bytes);f.finishWrite(out);if(announce)Toast.makeText(this,"局面已保存",Toast.LENGTH_SHORT).show();}
        catch(IOException e){if(out!=null)f.failWrite(out);Toast.makeText(this,"保存失败，请检查设备存储空间后重试",Toast.LENGTH_LONG).show();}
    }
    private World readSave(AtomicFile f)throws IOException {try(FileInputStream in=f.openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>4*1024*1024+20)throw new IOException("存档超过大小限制");out.write(buffer,0,count);}return SaveCodec.decode(out.toByteArray());}}
    private void loadSlot(String slot){try{World restored=readSave(file(slot));world=restored;ui.summary="";ui.city=-1;ui.owner=-1;ui.query="";ui.taskType=0;selectAndFocus(world.home().hex);save("auto",false);}catch(IOException e){showError("读取失败");}}
    @Override protected void onPause(){super.onPause();if(world!=null)save("auto",false);}
    @Override public void onBackPressed(){if(aiRunning)return;if(!ui.page.equals("map")){ui.page="map";refresh();}else if(ui.panelVisible){ui.panelVisible=false;refresh();}else confirm("退出游戏？当前局面将自动保存。",this::finish);}
}

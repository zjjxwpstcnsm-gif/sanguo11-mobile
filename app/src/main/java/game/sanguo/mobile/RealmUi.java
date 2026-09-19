package game.sanguo.mobile;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** National, read-only lists and faction dossiers share the same underlying world, not mock data. */
final class RealmUi {
    private final MainActivity a;private final World w;private final ClientState state;
    private RealmOverview accounts;
    RealmUi(MainActivity a,World w,ClientState state){this.a=a;this.w=w;this.state=state;}
    private RealmOverview accounts(){if(accounts==null)accounts=new RealmOverview(w);return accounts;}
    private LinearLayout column(){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(a.dp(8),a.dp(4),a.dp(8),a.dp(4));return v;}
    private TextView copy(String text,int size,int color){TextView t=a.text(text,size,color);t.setPadding(a.dp(6),a.dp(7),a.dp(6),a.dp(7));return t;}
    private void heading(LinearLayout host,String text){TextView t=copy(text,16,a.gold);t.setTypeface(null,Typeface.BOLD);host.addView(t);}
    private static String number(long n){return String.format(Locale.ROOT,"%,d",n);}
    private static String signed(long n){return (n>=0?"+":"−")+number(Math.abs(n));}
    private <T> DataTable.Column<T> word(String title,int width,Function<T,String> text){return new DataTable.Column<>(title,width,text,Comparator.comparing(text),false);}
    private <T> DataTable.Column<T> value(String title,int width,ToLongFunction<T> number){return new DataTable.Column<>(title,width,t->number(number.applyAsLong(t)),Comparator.comparingLong(number),true);}
    private <T> void remember(DataTable<T> table,String key){
        table.search.setText(state.listPages.getString(key+"Query",""));table.onQuery=q->state.listPages.putString(key+"Query",q);
        table.order(state.listPages.getInt(key+"Sort",-1),state.listPages.getBoolean(key+"Descending"));
        table.onSort=(i,d)->{state.listPages.putInt(key+"Sort",i);state.listPages.putBoolean(key+"Descending",d);};
        table.list.setSelectionFromTop(state.listPages.getInt(key+"Position",0),state.listPages.getInt(key+"Top",0));
        table.list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView v,int s){}public void onScroll(AbsListView v,int first,int count,int total){if(v.getChildCount()>0){state.listPages.putInt(key+"Position",first);state.listPages.putInt(key+"Top",v.getChildAt(0).getTop());}}});
    }
    private void navigation(LinearLayout host){
        HorizontalScrollView scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(a);scroll.addView(row);
        String[] keys={"factions","units","officers","cities","ports","gates","facilities"},names={"势力","部队","武将","城池","港口","关卡","设施"};
        for(int i=0;i<keys.length;i++){final String key=keys[i];Button b=a.button(names[i],v->a.openRealmPage(key,-1));b.setSelected(state.page.equals(key));b.setContentDescription("全国列表 · "+names[i]);row.addView(b,new LinearLayout.LayoutParams(a.dp(61),a.dp(42)));}
        host.addView(scroll,new LinearLayout.LayoutParams(-1,a.dp(42)));
    }
    private int owner(){return state.listPages.getInt(state.page+"Owner",-1);}
    private void ownerFilter(LinearLayout host){
        LinearLayout row=new LinearLayout(a);host.addView(row);
        row.addView(a.button(owner()<0?"全部势力":w.faction(owner()),v->{String[] labels=new String[w.factions.length+1];labels[0]="全部势力";System.arraycopy(w.factions,0,labels,1,w.factions.length);
            new AlertDialog.Builder(a).setTitle("按所属势力筛选").setItems(labels,(d,i)->{state.listPages.putInt(state.page+"Owner",i-1);a.refresh();}).show();}),new LinearLayout.LayoutParams(0,a.dp(40),1));
        row.addView(a.button("仅己方",v->{state.listPages.putInt(state.page+"Owner",w.player);a.refresh();}),new LinearLayout.LayoutParams(a.dp(76),a.dp(40)));
        row.addView(a.button("全部",v->{state.listPages.putInt(state.page+"Owner",-1);a.refresh();}),new LinearLayout.LayoutParams(a.dp(60),a.dp(40)));
    }
    View factions(){
        LinearLayout host=column();navigation(host);
        long alive=accounts().factions.stream().filter(f->f.alive).count();host.addView(copy("天下势力  "+alive+" 存续 / "+w.factions.length+" 总数 · 点选查看详情",12,a.muted));
        List<DataTable.Column<RealmOverview.Faction>> cols=Arrays.asList(
            word("势力",88,f->f.name+(f.alive?"":"·灭亡")),value("兵力",90,f->f.troops),value("武将",55,f->f.officers),value("城池",50,f->f.cities),value("部队",55,f->f.units),
            value("总金",90,f->f.gold),value("总粮",95,f->f.food),value("预计金入",80,f->f.goldIncome),value("预计粮入",80,f->f.foodIncome),value("旬耗粮",80,f->f.foodUse),
            value("港口",52,f->f.ports),value("关卡",52,f->f.gates),value("技巧",52,f->f.techs),value("能力",52,f->f.abilities),value("研究中",58,f->f.projects));
        DataTable<RealmOverview.Faction> table=new DataTable<>(a,accounts().factions,cols,new int[]{0,1,2,3,4},f->f.name+" "+(f.alive?"存续":"灭亡")+" 兵力"+f.troops+" 武将"+f.officers,f->f.id,f->factionDetail(f.id),f->factionDetail(f.id));
        table.search.setHint("搜索势力 / 存续 / 灭亡");table.search.setContentDescription("搜索全部势力");
        table.columnGroups(new String[]{"军力","财赋","收支","领地 / 研究"},new int[][]{{0,1,2,3,4},{0,5,6},{0,7,8,9},{0,10,11,12,13,14}});
        remember(table,"realmFactions");host.addView(table,new LinearLayout.LayoutParams(-1,0,1));return host;
    }
    View units(){
        LinearLayout host=column();navigation(host);ownerFilter(host);
        List<World.Unit> rows=new ArrayList<>();for(World.Unit u:accounts().units)if(owner()<0||owner()==u.owner)rows.add(u);
        List<DataTable.Column<World.Unit>> cols=Arrays.asList(
            word("主将",76,u->RealmOverview.commander(w,u)),word("势力",76,u->w.faction(u.owner)),word("兵种",66,RealmOverview::unitType),value("兵力",85,u->u.troops),
            value("携粮",85,u->u.food),value("旬耗",70,u->Logistics.foodUse(w,u)),value("气力",55,u->u.energy),value("攻击",65,u->Math.round(w.combat.attackRating(u))),value("防御",65,u->Math.round(w.combat.defenseRating(u))),
            word("状态",82,u->u.status!=War.Status.NORMAL?u.status.label:u.acted?"已行动":"可行动"),word("位置 / 任务",120,this::unitLocation),
            word("续航",65,u->Logistics.foodUse(w,u)==0?"不限":Logistics.turns(u.food,Logistics.foodUse(w,u))+"旬"));
        DataTable<World.Unit> table=new DataTable<>(a,rows,cols,new int[]{0,1,2,3},u->RealmOverview.crew(w,u)+" "+w.faction(u.owner)+" "+RealmOverview.unitType(u)+" "+unitLocation(u)+" "+u.status.label,u->u.id,u->a.selectUnitAndFocus(u.id),this::unitDetail);
        table.search.setHint("搜索主将、副将、势力、兵种、位置");table.search.setContentDescription("搜索全国部队");
        table.columnGroups(new String[]{"编队","战力","粮秣","任务"},new int[][]{{0,1,2,3},{0,7,8,6},{0,4,5,11},{0,1,9,10}});remember(table,"realmUnits");
        host.addView(table,new LinearLayout.LayoutParams(-1,0,1));return host;
    }
    private String unitLocation(World.Unit u){World.City region=w.personnel.region(u.hex);String where=(region==null?"":region.name+"附近 ")+u.hex.q+","+u.hex.r;
        if(u instanceof Domestic.Mission){Domestic.Mission m=(Domestic.Mission)u;World.City c=w.city(m.targetCity);return where+" → "+(c==null?"—":c.name)+(m.stopped?" · 已停止":"");}
        return where+(u.march==null?"":" · 自动行军");}
    private void unitDetail(World.Unit u){
        new AlertDialog.Builder(a).setTitle(RealmOverview.crew(w,u)+" · "+RealmOverview.unitType(u))
            .setMessage(w.faction(u.owner)+"\n兵力 "+number(u.troops)+" · 气力 "+u.energy+"\n攻击 "+Math.round(w.combat.attackRating(u))+" · 防御 "+Math.round(w.combat.defenseRating(u))+"\n"+Logistics.describe(w,u)+"\n"+unitLocation(u))
            .setPositiveButton("定位部队",(d,n)->a.selectUnitAndFocus(u.id)).setNeutralButton("主将详情",(d,n)->{World.Officer o=w.officer(u.officerId);if(o!=null)a.officerDetail(o);}).setNegativeButton("返回",null).show();
    }
    View sites(World.SiteKind kind){
        LinearLayout host=column();navigation(host);ownerFilter(host);List<World.City> rows=new ArrayList<>();
        for(World.City c:w.cities)if((kind==null||c.kind==kind)&&(owner()<0||owner()==c.owner))rows.add(c);
        List<DataTable.Column<World.City>> cols=Arrays.asList(word("据点",85,c->c.name),word("势力",80,c->w.faction(c.owner)),value("兵力",82,c->c.troops),value("耐久",72,c->c.defense),value("金",82,c->c.gold),value("粮",90,c->c.food),value("武将",55,c->UiModels.officerCount(w,c.id)));
        DataTable<World.City> table=new DataTable<>(a,rows,cols,new int[]{0,1,2,3},c->c.name+" "+w.faction(c.owner)+" "+RealmOverview.siteType(c.kind),c->c.id,c->a.selectAndFocus(c.hex),c->a.selectAndFocus(c.hex));
        table.search.setHint("搜索据点或势力");table.columnGroups(new String[]{"驻军 / 城防","库存 / 人员"},new int[][]{{0,1,2,3},{0,4,5,6}});remember(table,"realm"+state.page);host.addView(table,new LinearLayout.LayoutParams(-1,0,1));return host;
    }
    private static final class Building {
        long id;String name,type,place,effect,status;int owner,hp;Hex hex;
    }
    View facilities(){
        LinearLayout host=column();navigation(host);ownerFilter(host);List<Building> rows=new ArrayList<>();
        for(Domestic.Facility f:w.domestic.facilities){World.City c=w.city(f.cityId);if(c==null)continue;Building b=new Building();b.id=f.id;b.name=f.kind.label+"·"+f.level;b.type="内政";b.owner=c.owner;b.place=c.name;b.hex=f.hex;b.hp=f.hp;b.effect=f.kind.effect;b.status=f.remaining>0?"施工剩"+f.remaining+"旬":"已建成";if(owner()<0||b.owner==owner())rows.add(b);}
        for(War.Structure f:w.war.structures()){Building b=new Building();b.id=1000000000L+f.id;b.name=f.kind.label;b.type="军事";b.owner=f.owner;World.City c=w.personnel.region(f.hex);b.place=c==null?"野外":c.name+"附近";b.hex=f.hex;b.hp=f.hp;b.effect=f.kind.effect;b.status=f.complete?"已建成":"施工中";if(owner()<0||b.owner==owner())rows.add(b);}
        List<DataTable.Column<Building>> cols=Arrays.asList(word("设施",88,b->b.name),word("势力",76,b->w.faction(b.owner)),word("类别",54,b->b.type),value("耐久",68,b->b.hp),word("地区",90,b->b.place),word("状态",95,b->b.status));
        DataTable<Building> table=new DataTable<>(a,rows,cols,new int[]{0,1,2,3},b->b.name+" "+w.faction(b.owner)+" "+b.type+" "+b.place+" "+b.status,b->b.id,b->a.selectAndFocus(b.hex),b->new AlertDialog.Builder(a).setTitle(b.name).setMessage(b.effect+"\n"+b.status+" · 耐久"+b.hp).setPositiveButton("定位",(d,i)->a.selectAndFocus(b.hex)).setNegativeButton("返回",null).show());
        table.search.setHint("搜索名称、内政 / 军事、势力、地区");table.columnGroups(new String[]{"类别 / 耐久","位置 / 进度"},new int[][]{{0,1,2,3},{0,4,5}});remember(table,"realmFacilities");host.addView(table,new LinearLayout.LayoutParams(-1,0,1));return host;
    }
    private World.Officer ruler(int side){for(World.Officer o:w.officers)if(o.owner==side&&o.role==Strategy.Role.RULER)return o;return null;}
    void factionDetail(int side){factionDetail(side,null);}
    AlertDialog factionDetail(int side,Runnable choose){
        RealmOverview.Faction f=accounts().factions.get(side);LinearLayout sheet=column();LinearLayout hero=new LinearLayout(a);hero.setGravity(Gravity.CENTER_VERTICAL);sheet.addView(hero);
        World.Officer ruler=ruler(side);if(ruler!=null){ImageView portrait=new ImageView(a);portrait.setImageDrawable(new OfficerPortrait(a,w,ruler));hero.addView(portrait,new LinearLayout.LayoutParams(a.dp(66),a.dp(72)));}
        TextView name=copy(f.name+"\n"+(ruler==null?"君主未定":"君主 · "+ruler.name)+"  "+(f.alive?"存续":"已灭亡"),18,a.paper);hero.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        View color=new View(a);color.setBackgroundColor(FactionColors.color(w,side));hero.addView(color,new LinearLayout.LayoutParams(a.dp(6),a.dp(58)));
        LinearLayout tabs=new LinearLayout(a);sheet.addView(tabs);ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);LinearLayout content=column();scroll.addView(content);sheet.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        String[] labels={"概况","收支","技巧树","能力研究"};Button[] buttons=new Button[labels.length];
        IntConsumer show=i->{content.removeAllViews();scroll.scrollTo(0,0);for(int n=0;n<buttons.length;n++)if(buttons[n]!=null)buttons[n].setSelected(n==i);
            if(i==0)overview(content,f);else if(i==1)economy(content,f);else if(i==2)technology(content,side);else abilities(content,side);};
        for(int i=0;i<labels.length;i++){final int index=i;buttons[i]=a.button(labels[i],v->show.accept(index));buttons[i].setContentDescription("势力详情 · "+labels[i]);tabs.addView(buttons[i],new LinearLayout.LayoutParams(0,a.dp(44),1));}
        AlertDialog.Builder builder=new AlertDialog.Builder(a).setView(sheet).setNegativeButton("返回",null);
        if(choose!=null)builder.setPositiveButton("选定此势力",(d,n)->choose.run());
        else builder.setNeutralButton("旗下列表",(d,n)->new AlertDialog.Builder(a).setTitle(f.name+" · 查看").setItems(new String[]{"部队","武将","城池","港口","关卡","设施"},(x,i)->a.openRealmPage(new String[]{"units","officers","cities","ports","gates","facilities"}[i],side)).show());
        AlertDialog dialog=builder.create();dialog.show();UiTheme.dialog(dialog);if(dialog.getWindow()!=null)dialog.getWindow().setLayout(Math.min(a.getResources().getDisplayMetrics().widthPixels-a.dp(16),a.dp(680)),Math.round(a.getResources().getDisplayMetrics().heightPixels*.88f));show.accept(0);return dialog;
    }
    private void metric(LinearLayout host,String left,String right){LinearLayout row=new LinearLayout(a);row.setBackground(UiTheme.surface(a,0xff213a3c,0xff172a32,10));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,a.dp(4),0,a.dp(4));host.addView(row,lp);row.addView(copy(left,14,a.paper),new LinearLayout.LayoutParams(0,-2,1));row.addView(copy(right,14,a.gold),new LinearLayout.LayoutParams(0,-2,1));}
    private void overview(LinearLayout host,RealmOverview.Faction f){
        heading(host,"领地与军力");metric(host,"城池 "+f.cities,"港口 "+f.ports+" / 关卡 "+f.gates);metric(host,"武将 "+f.officers,"其中被俘 "+f.prisoners);metric(host,"总兵力 "+number(f.troops),"驻军 "+number(f.garrison));metric(host,"部队 "+f.units,"其中运输队 "+f.convoys);
        metric(host,"总金 "+number(f.gold),"总粮 "+number(f.food));metric(host,"技巧点 "+w.campaign.points(f.id),"行动力 "+w.actionPoints[f.id]);
        heading(host,"研究进行中 · "+f.projects);host.addView(copy(RealmOverview.researchSummary(w,f.id),14,a.paper));
        heading(host,"领地明细");StringJoiner sites=new StringJoiner("  ·  ");for(World.City c:w.cities)if(c.owner==f.id)sites.add(c.name+"（"+RealmOverview.siteType(c.kind)+"）");host.addView(copy(sites.length()==0?"无据点":sites.toString(),13,a.muted));
        host.addView(copy("总量包含在途部队与未卸货运输队，不重复计算货物；武将计入已登场的所属武将（含被俘者）。",12,a.muted));
    }
    private void economy(LinearLayout host,RealmOverview.Faction f){
        heading(host,"钱粮资产 · "+w.date());metric(host,"总金 "+number(f.gold),"总粮 "+number(f.food));metric(host,"其中在途金 "+number(f.cargoGold),"其中在途粮 "+number(f.cargoFood));
        heading(host,"下旬预计收支");metric(host,"金钱收入 "+signed(f.goldIncome),"固定金耗 "+number(f.goldUse));metric(host,"粮食收入 "+signed(f.foodIncome),"兵粮需求 −"+number(f.foodUse));metric(host,"金净额 "+signed(f.netGold()),"粮净额 "+signed(f.netFood()));
        heading(host,"基础产能（非每旬发放）");metric(host,"月初基准金 "+number(f.monthGold),"季初基准粮 "+number(f.seasonFood));host.addView(copy(RealmOverview.FORECAST_NOTE+"\n兵粮需求按足额供应计算；断粮会产生逃兵，实际可扣粮不超过库存。",13,a.muted));
    }
    private void technology(LinearLayout host,int side){
        heading(host,"技巧点 "+number(w.campaign.points(side)));host.addView(copy("按真实前置关系展示；点选节点查看效果与成本。研究耗时沿用当前规则。",12,a.muted));
        for(int branch=0;branch<Campaign.Tech.BRANCHES.length;branch++){
            heading(host,Campaign.Tech.BRANCHES[branch]);
            for(Campaign.Tech t:Campaign.Tech.branch(branch)){
                Campaign.Project project=null;for(Campaign.Project p:w.campaign.projects())if(p.owner==side&&p.tech==t){project=p;break;}
                String status=w.campaign.has(side,t)?"✓ 已掌握":project!=null?"◉ 研究中 · 剩"+w.officer(project.officerId).otherTaskTurns+"旬":t.prerequisite==null||w.campaign.has(side,t.prerequisite)?"○ 前置满足":"锁定";
                Button node=a.button((t.prerequisite==null?"":"↳ ")+t.label+"  ·  "+status,v->new AlertDialog.Builder(a).setTitle(t.label).setMessage(t.effect+"\n前置："+(t.prerequisite==null?"无":t.prerequisite.label)+"\n技巧点 "+t.points+" · 金 "+t.gold+" · "+t.turns+"旬").setPositiveButton("返回",null).show());
                node.setSelected(w.campaign.has(side,t));node.setContentDescription("技巧节点 · "+t.label+" · "+status);host.addView(node,new LinearLayout.LayoutParams(-1,a.dp(46)));
            }
        }
    }
    private void abilities(LinearLayout host,int side){
        host.addView(copy(RealmOverview.researchSummary(w,side),13,a.paper));host.addView(copy("展示本势力的真实能力树；隐藏能力仅在原有解锁规则允许时显示，不展开未选中的随机分支。",12,a.muted));
        Map<String,List<AbilityResearch.Node>> branches=new LinkedHashMap<>();for(AbilityResearch.Node n:w.abilities.visible(side))branches.computeIfAbsent(n.branch,k->new ArrayList<>()).add(n);
        AbilityResearch.Research research=w.abilities.research(side);
        for(Map.Entry<String,List<AbilityResearch.Node>> branch:branches.entrySet()){
            heading(host,branch.getKey());for(AbilityResearch.Node n:branch.getValue()){
                String status=w.abilities.learned(side,n.id)?"✓ 已研究 · 可用"+w.abilities.remaining(side,n.id)+"次":research!=null&&research.nodeId.equals(n.id)?"◉ 研究中 · 剩"+research.remaining+"旬":w.abilities.unlocked(side,n)?"○ 前置满足":"锁定";
                StringJoiner pre=new StringJoiner(" + ");for(String id:n.prerequisites)pre.add(AbilityResearch.node(id).label);
                String info=n.effect()+"\n前置："+(pre.length()==0?"无":pre)+"\n研究时长 "+n.turns+"旬 · 总使用次数 "+n.uses;
                Button node=a.button(n.label+" · "+status,v->new AlertDialog.Builder(a).setTitle(n.label).setMessage(info).setPositiveButton("返回",null).show());node.setSelected(w.abilities.learned(side,n.id));node.setContentDescription("能力节点 · "+n.label+" · "+status);host.addView(node,new LinearLayout.LayoutParams(-1,a.dp(46)));
                if(pre.length()>0)host.addView(copy("前置  "+pre+"  →",11,a.muted));
            }
        }
    }
}

package game.sanguo.mobile;

import android.app.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

final class WorldUi {
    private final Activity a;private final World w;private final Consumer<World.Result> apply;
    WorldUi(Activity a,World w,Consumer<World.Result> apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String text){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,Runnable action){boolean[] submitted={false};new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,n)->{if(!submitted[0]){submitted[0]=true;action.run();}}).setNegativeButton("取消",null).show();}
    private <T> void choose(String title,List<T> list,Function<T,String> label,Consumer<T> next){
        if(list.isEmpty()){info(title,"没有符合条件的选项");return;}String[] labels=list.stream().map(label).toArray(String[]::new);
        new AlertDialog.Builder(a).setTitle(title).setItems(labels,(d,n)->next.accept(list.get(n))).setNegativeButton("取消",null).show();
    }
    void menu(){new AlertDialog.Builder(a).setTitle("军团与天下").setItems(new String[]{"军团编制","灾害与贼患","随机事件 · "+(w.events.enabled()?"已开启":"已关闭"),"军情评估"},(d,n)->{
        if(n==0)districts();else if(n==1)events();else if(n==3)assessment();else confirm("随机事件",w.events.enabled()?"停止产生新灾害和贼患？现有灾害、营寨继续结算。":"开启月度贼患、季节灾害与七月丰收？风水、祈愿和亲族特技将影响发生条件。",()->apply.accept(w.events.toggle()));
    }).setNegativeButton("返回",null).show();}
    private void assessment(){
        CampaignAi ai=new CampaignAi(w);StringBuilder text=new StringBuilder();
        for(World.City c:w.cities)if(c.owner==w.player){int reserve=ai.reserve(c);if(w.districts.city(c.id)!=null)reserve=Math.max(10000,reserve);
            text.append(c.name).append(" · 周边敌兵 ").append(ai.incoming(c)).append("\n建议留守 ").append(reserve).append(" · 现有 ").append(c.troops).append("\n\n");}
        for(World.Unit u:w.units)if(u.owner==w.player)text.append(w.officer(u.officerId).name).append("部队 · 兵 ").append(u.troops).append(" · 携粮约可用 ").append(ai.foodTurns(u)).append("旬\n");
        text.append("\n按当前可见兵力与本旬粮耗估算；军情变化后会重新判断。");
        ScrollView scroll=new ScrollView(a);TextView body=new TextView(a);body.setText(text.toString());body.setTextSize(16);body.setPadding(24,16,24,16);scroll.addView(body);
        new AlertDialog.Builder(a).setTitle("军情评估").setView(scroll).setPositiveButton("返回",null).show();
    }
    void districts(){List<Districts.District> list=w.districts.all();String[] names=new String[list.size()+2];names[0]="新设军团";for(int i=0;i<list.size();i++){Districts.District d=list.get(i);names[i+1]=d.name()+" · "+d.policy().label+" · "+d.cities().size()+"据点";}
        names[names.length-1]="批量托管后方据点";
        new AlertDialog.Builder(a).setTitle("第一军团 · 行动力"+w.actionPoints[w.player]).setItems(names,(dialog,n)->{if(n==0)members(null);else if(n==names.length-1)rear();else detail(list.get(n-1));}).setNegativeButton("返回",null).show();
    }
    private String nextName(){Set<String> names=new HashSet<>();for(Districts.District d:w.districts.all())names.add(d.name());for(int i=2;i<=9;i++)if(!names.contains("第"+i+"军团"))return "第"+i+"军团";return "新军团";}
    void city(World.City c){
        Districts.District district=w.districts.city(c.id);if(district!=null){detail(district);return;}
        new AlertDialog.Builder(a).setTitle(c.name+" · 城池托管").setItems(new String[]{"快速托管内政（新军团）","编入已有军团","自定义军团"},(d,n)->{
            if(n==0)quick(new int[]{c.id});
            else if(n==1)choose("选择接管军团",w.districts.all(),group->group.name()+" · "+group.policy().label,group->{
                Set<Integer> members=new TreeSet<>(group.cities());members.add(c.id);int[] ids=members.stream().mapToInt(i->i).toArray();
                String error=w.districts.configureError(group.id,group.name(),ids,group.policy(),group.target(),group.supply());
                if(error!=null){info("无法编制",error);return;}
                confirm("编入"+group.name(),c.name+"交由军团经营，消耗第一军团20行动力。现有方针和军团行动力不变。",()->apply.accept(w.districts.configure(group.id,group.name(),ids,group.policy(),group.target(),group.supply(),group.attack(),group.produce())));
            });else members(null);
        }).setNegativeButton("返回",null).show();
    }
    private boolean rulerCity(World.City c){for(World.Officer o:w.officers)if(o.owner==w.player&&o.role==Strategy.Role.RULER&&o.cityId==c.id)return true;return false;}
    private void rear(){
        Territory territory=new Territory(w);List<World.City> cities=new ArrayList<>();CampaignAi ai=new CampaignAi(w);
        for(World.City c:w.cities)if(c.owner==w.player&&w.districts.city(c.id)==null&&!rulerCity(c)&&!territory.frontline(c.id)&&ai.incoming(c)==0)cities.add(c);
        if(cities.isEmpty()){info("后方托管","没有可编组的后方直属据点。君主驻地、前线、敌兵接近的据点保留手动管理，也可自定义选择。");return;}
        boolean[] checked=new boolean[cities.size()];Arrays.fill(checked,true);
        new AlertDialog.Builder(a).setTitle("后方托管 · 勾选据点").setMultiChoiceItems(cities.stream().map(c->c.name+" · 闲将"+w.idle(c).size()).toArray(String[]::new),checked,(d,n,yes)->checked[n]=yes)
            .setPositiveButton("预览托管",(d,n)->{List<Integer> selected=new ArrayList<>();for(int i=0;i<checked.length;i++)if(checked[i])selected.add(cities.get(i).id);quick(selected.stream().mapToInt(i->i).toArray());}).setNegativeButton("取消",null).show();
    }
    private void quick(int[] members){
        String name=nextName();String error=w.districts.configureError(-1,name,members,Districts.Policy.ECONOMY,-1,-1);
        if(error!=null){info("无法编制",error);return;}
        StringBuilder text=new StringBuilder();for(int id:members){World.City c=w.city(id);text.append(c.name).append(" · 闲将").append(w.idle(c).size()).append('\n');}
        text.append("内政优先，允许兵装生产，不主动进攻。消耗第一军团20行动力；下一旬恢复本团60行动力后，结束旬时自动经营。缺少驻城武将的据点需要先调入人才。可随时重编或撤销。");
        confirm(name+" · 快速托管",text.toString(),()->apply.accept(w.districts.configure(-1,name,members,Districts.Policy.ECONOMY,-1,-1,false,true)));
    }
    private String describe(Districts.District d){StringBuilder b=new StringBuilder(d.policy().label+"\n都督："+(d.leader()<0?"暂无在城武将":w.officer(d.leader()).name)+"\n军团行动力："+d.points()+" / 60\n");
        b.append(w.districts.status(d)).append('\n');
        for(int id:d.cities()){World.City c=w.city(id);b.append(c.name).append(" · 闲将").append(w.idle(c).size()).append(" · 金").append(c.gold).append(" · 粮").append(c.food).append('\n').append(new DistrictManagement(w).forecast(c)).append('\n');}
        if(d.target()>=0)b.append("\n攻略目标：").append(d.policy()==Districts.Policy.FORCE_ATTACK?w.faction(d.target()):w.city(d.target()).name);
        b.append("\n运输目标：").append(d.supply()<0?"无":w.city(d.supply()).name).append("\n允许进攻：").append(d.attack()?"是":"否").append("；允许生产：").append(d.produce()?"是":"否");
        b.append("\n留存：兵").append(d.reserveTroops()).append(" / 金").append(d.reserveGold()).append(" / 粮").append(d.reserveFood());
        b.append("\n自动调将：").append(d.transfer()?"允许":"禁止").append("；补给运输：").append(d.supplyEnabled()?"允许":"禁止");
        b.append("\n\n最近经营报告（旬 ").append(d.reportTurn()).append("）\n").append(d.report());
        b.append("\n在途任务\n").append(new DistrictManagement(w).cargo(d));
        for(World.Unit u:w.units)if(w.districts.unit(u.id)==d)b.append("\n").append(w.officer(u.officerId).name).append("：").append(w.aiOrders.describe(u));
        return b.toString();
    }
    private void detail(Districts.District d){ScrollView scroll=new ScrollView(a);TextView body=new TextView(a);body.setText(describe(d));body.setTextSize(16);body.setPadding(24,16,24,16);LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);panel.addView(body);Button settings=new Button(a);settings.setText("经营设置");settings.setOnClickListener(v->settings(d));panel.addView(settings);
        Button support=new Button(a);support.setText("支援申请 / 可执行预览");support.setOnClickListener(v->support(d));panel.addView(support);
        Button locate=new Button(a);locate.setText("定位在途任务");locate.setOnClickListener(v->{List<Domestic.Mission> tasks=new ArrayList<>();for(Domestic.Mission m:w.domestic.missions)if(d.cities().contains(m.sourceCity)||d.cities().contains(m.targetCity))tasks.add(m);choose("选择在途任务",tasks,m->w.officer(m.officerId).name+" → "+w.city(m.targetCity).name,m->{if(a instanceof MainActivity)((MainActivity)a).domesticUi().mission(m);});});panel.addView(locate);
        scroll.addView(panel);new AlertDialog.Builder(a).setTitle(d.name()).setView(scroll).setPositiveButton("重编",(dialog,n)->members(d)).setNeutralButton("撤销军团",(dialog,n)->confirm("撤销"+d.name(),"消耗第一军团20行动力。全部据点、部队恢复直接指挥，余下军团行动力作废。",()->apply.accept(w.districts.dissolve(d.id)))).setNegativeButton("返回",null).show();}
    private void support(Districts.District d){
        if(d.supply()<0){info("支援申请","请先在重编中指定运输目的地，以明确授权本军团向该城支援。未授权时不会抽调其他军团。");return;}
        List<World.City> sources=new ArrayList<>();for(int id:d.cities())if(id!=d.supply())sources.add(w.city(id));
        choose("支援出发城",sources,c->c.name,c->{DistrictManagement.SupplyPlan p=w.districts.supportPlan(c.id,d.supply());
            if(p==null){info("支援不可执行","来源或目的地已改变");return;}
            String body=c.name+" → "+w.city(p.target).name+"\n"+(p.valid()?"可派送：金"+p.gold+" / 粮"+p.food+" / 兵"+p.troops+"\n执行武将："+w.officer(p.officer).name+"；军团行动力10，派遣费0金\n"+(p.returning?"卸货后人员返程":"武将抵达留驻"):"不能执行："+p.reason)+"\n"+new DistrictManagement(w).forecast(c);
            if(!p.valid()){info("支援不可执行",body);return;}
            confirm("支援执行预览",body,()->apply.accept(w.districts.requestSupport(p.source,p.target)));
        });
    }
    void batch(List<World.City> visible){
        List<World.City> cities=new ArrayList<>();for(World.City c:visible)if(c.owner==w.player)cities.add(c);
        if(cities.isEmpty()){info("批量划入军团","当前筛选下没有己方据点");return;}
        boolean[] selected=new boolean[cities.size()];
        new AlertDialog.Builder(a).setTitle("批量划入军团 · 选择据点").setMultiChoiceItems(cities.stream().map(c->c.name+(rulerCity(c)?" · 君主驻地，不可托管":"")).toArray(String[]::new),selected,(d,i,yes)->selected[i]=yes).setPositiveButton("预览",(d,n)->{
            List<Integer> ids=new ArrayList<>();for(int i=0;i<selected.length;i++)if(selected[i])ids.add(cities.get(i).id);
            List<Districts.District> groups=new ArrayList<>();groups.add(null);groups.addAll(w.districts.all());
            choose("接管军团",groups,g->g==null?"新建内政军团":g.name(),g->{
                Set<Integer> members=new TreeSet<>();if(g!=null)members.addAll(g.cities());StringBuilder preview=new StringBuilder();
                for(int id:ids){World.City c=w.city(id);Districts.District current=w.districts.city(id);
                    if(rulerCity(c)||current!=null&&current!=g)preview.append(c.name).append("：不可执行（君主驻地或已属其他军团）\n");
                    else{members.add(id);preview.append(c.name).append("：可划入\n");}}
                if(ids.isEmpty()){info("未选择据点","请选择后再预览");return;}
                int[] all=members.stream().mapToInt(i->i).toArray();String name=g==null?nextName():g.name();Districts.Policy policy=g==null?Districts.Policy.ECONOMY:g.policy();int target=g==null?-1:g.target(),supply=g==null?-1:g.supply();
                String error=w.districts.configureError(g==null?-1:g.id,name,all,policy,target,supply);
                if(error!=null){info("批量不可执行",preview+"\n"+error);return;}
                if(g!=null&&members.equals(g.cities())){info("批量无变更",preview.toString());return;}
                confirm("批量执行预览",preview+"\n消耗第一军团20行动力；仅执行可划入据点。",()->apply.accept(w.districts.configure(g==null?-1:g.id,name,all,policy,target,supply,g!=null&&g.attack(),g==null||g.produce())));
            });
        }).setNegativeButton("取消",null).show();
    }
    private EditText amount(LinearLayout panel,String label,int value){TextView title=new TextView(a);title.setText(label);panel.addView(title);EditText input=new EditText(a);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setContentDescription(label);input.setText(Integer.toString(value));panel.addView(input);return input;}
    private void settings(Districts.District d){
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(24,12,24,12);
        EditText troops=amount(panel,"留守兵力",d.reserveTroops()),gold=amount(panel,"保留金",d.reserveGold()),food=amount(panel,"保留粮",d.reserveFood());
        CheckBox transfer=new CheckBox(a);transfer.setText("允许军团内自动调将");transfer.setChecked(d.transfer());panel.addView(transfer);
        CheckBox supply=new CheckBox(a);supply.setText("允许自动运输与部队补给");supply.setChecked(d.supplyEnabled());panel.addView(supply);
        ScrollView scroll=new ScrollView(a);scroll.addView(panel);
        new AlertDialog.Builder(a).setTitle("军团经营设置").setView(scroll).setPositiveButton("预览设置",(dialog,n)->{try{
            int t=Integer.parseInt(troops.getText().toString()),g=Integer.parseInt(gold.getText().toString()),f=Integer.parseInt(food.getText().toString());
            confirm("保存经营设置","兵 / 金 / 粮留存："+t+" / "+g+" / "+f+"\n消耗第一军团20行动力。已出发任务按原货物继续运抵。",()->apply.accept(w.districts.settings(d.id,t,g,f,transfer.isChecked(),supply.isChecked())));
        }catch(NumberFormatException ex){info("数值无效","请输入范围内的非负整数");}}).setNegativeButton("取消",null).show();
    }
    private void members(Districts.District old){
        List<World.City> list=new ArrayList<>();for(World.City c:w.cities){Districts.District other=w.districts.city(c.id);boolean ruler=false;for(World.Officer o:w.officers)if(o.owner==w.player&&o.role==Strategy.Role.RULER&&o.cityId==c.id)ruler=true;if(c.owner==w.player&&!ruler&&(other==null||other==old))list.add(c);}
        if(list.isEmpty()){info("编制据点","需要君主驻地之外的己方据点");return;}
        boolean[] selected=new boolean[list.size()];for(int i=0;i<list.size();i++)selected[i]=old!=null&&old.cities().contains(list.get(i).id);
        new AlertDialog.Builder(a).setTitle("选择军团据点").setMultiChoiceItems(list.stream().map(c->c.name).toArray(String[]::new),selected,(d,n,checked)->selected[n]=checked).setPositiveButton("下一步",(d,n)->{
            List<Integer> ids=new ArrayList<>();for(int i=0;i<selected.length;i++)if(selected[i])ids.add(list.get(i).id);
            if(ids.isEmpty()){info("编制据点","至少选择一个据点");return;}
            int[] members=ids.stream().mapToInt(i->i).toArray();choose("军团方针",Arrays.asList(Districts.Policy.values()),p->p.label,p->target(old,members,p));
        }).setNegativeButton("取消",null).show();
    }
    private void target(Districts.District old,int[] members,Districts.Policy p){
        if(p==Districts.Policy.CITY_ATTACK){List<World.City> cities=new ArrayList<>();for(World.City c:w.cities)if(w.campaign.hostile(w.player,c.owner))cities.add(c);choose("攻略据点",cities,c->c.name,c->supply(old,members,p,c.id));}
        else if(p==Districts.Policy.FORCE_ATTACK){List<Integer> sides=new ArrayList<>();for(int side=0;side<w.factions.length;side++)if(w.alive(side)&&w.campaign.hostile(w.player,side))sides.add(side);choose("攻略势力",sides,w::faction,side->supply(old,members,p,side));}
        else supply(old,members,p,-1);
    }
    private void supply(Districts.District old,int[] members,Districts.Policy p,int target){
        List<Integer> targets=new ArrayList<>();targets.add(-1);for(World.City c:w.cities)if(c.owner==w.player)targets.add(c.id);
        choose("物资运输目的地",targets,id->id<0?"不指定运输":w.city(id).name,id->options(old,members,p,target,id));
    }
    private void options(Districts.District old,int[] members,Districts.Policy p,int target,int supply){
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(24,12,24,12);EditText name=new EditText(a);name.setSingleLine(true);name.setHint("军团名称");name.setContentDescription("军团名称");name.setText(old==null?nextName():old.name());panel.addView(name);
        CheckBox attack=new CheckBox(a);attack.setText("允许进攻");attack.setChecked(old==null?p==Districts.Policy.CITY_ATTACK||p==Districts.Policy.FORCE_ATTACK:old.attack());panel.addView(attack);
        CheckBox produce=new CheckBox(a);produce.setText("允许兵装生产");produce.setChecked(old==null||old.produce());panel.addView(produce);
        new AlertDialog.Builder(a).setTitle("军团委任内容").setView(panel).setPositiveButton("预览编制",(dialog,n)->{
            int id=old==null?-1:old.id;String title=name.getText().toString();String error=w.districts.configureError(id,title,members,p,target,supply);if(error!=null){info("无法编制",error);return;}
            try{byte[] before=SaveCodec.encode(w);StringBuilder text=new StringBuilder(p.label+"\n据点：");for(int city:members)text.append(w.city(city).name).append(' ');
                text.append("\n消耗第一军团20行动力。新军团下一旬开始获得60行动力；军团指令消耗本团预算。\n自动运输保留5000金、40000粮；出兵保留至少10000守军，附近敌兵较多时增加留守。按兵装适性编队并携带足够粮草，低兵力或将要断粮时回城。");
                confirm(title,text.toString(),()->{try{if(!Arrays.equals(before,SaveCodec.encode(w))){info("局面变化","请重新编制");return;}apply.accept(w.districts.configure(id,title,members,p,target,supply,attack.isChecked(),produce.isChecked()));}catch(java.io.IOException ex){info("编制失败",ex.getMessage());}});
            }catch(java.io.IOException ex){info("编制失败",ex.getMessage());}
        }).setNegativeButton("取消",null).show();
    }
    void events(){StringBuilder b=new StringBuilder("新事件："+(w.events.enabled()?"开启":"关闭")+"\n");for(World.City c:w.cities)b.append(c.name).append(" · ").append(w.events.cityStatus(c.id)).append('\n');
        new AlertDialog.Builder(a).setTitle("灾害与贼患").setMessage(b.toString()).setPositiveButton("贼寨一览",(d,n)->choose("贼寨",w.events.camps(),c->c.tribe.label+" · "+w.city(c.city).name,c->camp(c))).setNegativeButton("返回",null).show();
    }
    void camp(WorldEvents.Camp c){info(c.tribe.label+"营寨",w.city(c.city).name+"附近 · "+c.hex+"\n兵力 "+c.troops+"\n每月劫掠粮草、降低治安，可能破坏设施。选中己方部队后可讨伐，摧毁营寨停止其活动。");}
    void raids(World.Unit u){List<WorldEvents.Camp> list=new ArrayList<>();for(WorldEvents.Camp c:w.events.camps())if(w.events.attackError(u.id,c.id)==null)list.add(c);choose("讨伐贼寨",list,c->c.tribe.label+" · 兵"+c.troops,c->attack(u,c));}
    void attack(World.Unit u,WorldEvents.Camp c){confirm("讨伐"+c.tribe.label,"营寨兵力"+c.troops+"。消耗部队本旬行动；邻接时可能受到反击。",()->apply.accept(w.events.attack(u.id,c.id)));}
}

package game.sanguo.core;

import java.util.*;

/** Delegated groups share a saved order budget and execute the same domestic/army commands as the player. */
public final class Districts {
    public enum Policy {
        DELEGATE("自主委任"), ECONOMY("内政优先"), DEFENSE("守备优先"), CITY_ATTACK("都市攻略"), FORCE_ATTACK("势力攻略");
        public final String label;Policy(String label){this.label=label;}
    }
    public static final class District {
        public final int id,owner;String name;Policy policy;final SortedSet<Integer> cities=new TreeSet<>();
        int leader=-1,target=-1,supply=-1,points,actedTurn=-1;boolean attack,produce;
        int reserveTroops=10000,reserveGold=5000,reserveFood=40000,reportTurn=-1;
        boolean transfer=true,supplyEnabled=true;String report="尚未执行托管";
        public int reserveTroops(){return reserveTroops;}public int reserveGold(){return reserveGold;}public int reserveFood(){return reserveFood;}
        public boolean transfer(){return transfer;}public boolean supplyEnabled(){return supplyEnabled;}
        public String report(){return report;}public int reportTurn(){return reportTurn;}
        District(int id,int owner,String name){this.id=id;this.owner=owner;this.name=name;}
        public String name(){return name;}public Policy policy(){return policy;}public int leader(){return leader;}
        public int target(){return target;}public int supply(){return supply;}public int points(){return points;}
        public boolean attack(){return attack;}public boolean produce(){return produce;}
        public Set<Integer> cities(){return Collections.unmodifiableSortedSet(cities);}
    }
    private final World w;
    final SortedMap<Integer,District> groups=new TreeMap<>();final SortedMap<Integer,Integer> units=new TreeMap<>();
    int nextId=1;private int executing=-1;
    Districts(World w){this.w=w;}
    public List<District> all(){return Collections.unmodifiableList(new ArrayList<>(groups.values()));}
    public District get(int id){return groups.get(id);}
    public District city(int city){for(District d:groups.values())if(d.cities.contains(city))return d;return null;}
    public District unit(int unit){return groups.get(units.getOrDefault(unit,-1));}
    public String status(District d){
        int idle=0,people=0;for(int id:d.cities){World.City c=w.city(id);if(c!=null){idle+=w.idle(c).size();for(World.Officer o:w.officers)if(o.owner==d.owner&&o.cityId==id)people++;}}
        if(people==0)return "缺少驻城武将，请先调入人才";
        if(d.points==0)return "等待下一旬恢复军团行动力";
        if(idle==0)return "驻城武将已行动或正在执行任务";
        return "托管中 · 结束旬时自动经营 · 闲将"+idle;
    }
    public boolean directCity(int city){District d=city(city);return d==null||d.owner!=w.player||executing==d.id;}
    public boolean directUnit(int unit){District d=unit(unit);return d==null||d.owner!=w.player||executing==d.id;}
    boolean executing(int city){District d=city(city);return d!=null&&executing==d.id;}
    String reserveError(World.City c,int gold,int food,int troops){District d=city(c.id);
        if(d==null||executing!=d.id)return null;
        if(c.gold-gold<d.reserveGold||c.food-food<d.reserveFood||c.troops-troops<d.reserveTroops)return "军团金粮兵留存不足";return null;
    }
    String productionError(int city){District d=city(city);return d!=null&&executing==d.id&&!d.produce?"军团禁止生产":null;}
    String dispatchError(int source,int target,boolean cargo){District d=city(source);
        if(d==null||executing!=d.id)return null;
        if(cargo)return !d.supplyEnabled?"军团禁止补给运输":d.supply>=0&&target!=d.supply?"军团限定运输目的地":null;
        return !d.transfer?"军团禁止调将":!d.cities.contains(target)?"调将超出军团范围":null;
    }
    public World.Result settings(int id,int troops,int gold,int food,boolean transfer,boolean supply){
        String error=manageError();if(error!=null)return w.fail(error);District d=get(id);
        if(d==null||d.owner!=w.active||troops<0||troops>100000||gold<0||gold>1000000||food<0||food>1000000)return w.fail("军团或留存数值无效");
        d.reserveTroops=troops;d.reserveGold=gold;d.reserveFood=food;d.transfer=transfer;d.supplyEnabled=supply;w.actionPoints[w.active]-=20;
        return w.success(d.name+"经营设置已保存；已出发任务继续运抵，不重复扣款");
    }
    public DistrictManagement.SupplyPlan supportPlan(int source,int target){
        World.City c=w.city(source),t=w.city(target);District d=city(source);
        if(c==null||t==null||d==null||c.owner!=w.player||t.owner!=w.player||source==target)return null;
        World.Officer courier=w.idle(c).stream().filter(o->o.role!=Strategy.Role.RULER).min(Comparator.comparingInt(o->o.politics)).orElse(null);
        DistrictManagement.SupplyPlan plan=new DistrictManagement(w).plan(d,c,t,courier);
        if(d.points<10)return new DistrictManagement.SupplyPlan(source,target,courier==null?-1:courier.id,0,0,0,new int[World.Weapon.values().length],false,"出发军团行动预算不足10");
        return plan;
    }
    public World.Result requestSupport(int source,int target){
        if(w.commandsBlocked()||w.gameOver()||w.active!=w.player||executing!=-1)return w.fail("当前不能申请军团支援");
        DistrictManagement.SupplyPlan p=supportPlan(source,target);if(p==null)return w.fail("请选择已授权的己方军团来源和目的地");if(!p.valid())return w.fail(p.reason);
        District d=city(source);int points=w.actionPoints[w.active];executing=d.id;w.actionPoints[w.active]=d.points;
        try{World.Result result=new DistrictManagement(w).send(p);if(result.ok){d.report=w.city(source).name+"接受支援申请→"+w.city(target).name+"；实际派送 金"+p.gold+" / 粮"+p.food+" / 兵"+p.troops;d.reportTurn=w.turn;}return result;}
        finally{d.points=w.actionPoints[w.active];w.actionPoints[w.active]=points;executing=-1;}
    }
    private String manageError(){
        if(w.commandsBlocked()||w.gameOver()||w.active!=w.player)return "当前无法编制军团";
        return w.actionPoints[w.active]<20?"编制需要第一军团20行动力":null;
    }
    public String configureError(int id,String name,int[] members,Policy policy,int target,int supply){
        String error=manageError();if(error!=null)return error;
        District prior=id<0?null:get(id);if(id>=0&&(prior==null||prior.owner!=w.active))return "军团不存在或不属于当前势力";
        if(id<0&&(nextId>=10000000||groups.values().stream().filter(d->d.owner==w.active).count()>=7))return "最多七个委任军团";
        if(name==null||name.trim().isEmpty()||name.length()>30||members==null||members.length==0||members.length>=w.cities.size()||policy==null)return "需要名称、方针和至少一个据点";
        Set<Integer> ids=new HashSet<>();int own=0;for(World.City c:w.cities)if(c.owner==w.active)own++;
        if(members.length>=own)return "第一军团必须保留至少一个据点";
        for(int member:members){World.City c=w.city(member);District other=city(member);
            if(c==null||c.owner!=w.active||!ids.add(member)||other!=null&&other!=prior)return "据点重复、归属错误或已编入其他军团";
            for(World.Officer o:w.officers)if(o.owner==w.active&&o.role==Strategy.Role.RULER&&o.cityId==member)return "君主所在据点应保留在第一军团";
        }
        boolean direct=false;for(World.City c:w.cities)if(c.owner==w.active&&!ids.contains(c.id)&&(city(c.id)==null||city(c.id)==prior))direct=true;
        if(!direct)return "第一军团必须保留至少一个据点";
        // Groups need connected own territory; paths use the current passable map, never straight-line adjacency guesses.
        Set<Integer> linked=new HashSet<>();linked.add(members[0]);boolean changed=true;
        while(changed){changed=false;for(int member:members)if(!linked.contains(member))for(int from:new ArrayList<>(linked))if(w.domestic.route(w.city(from).hex,w.city(member).hex,w.active,true)!=null){linked.add(member);changed=true;break;}}
        if(linked.size()!=ids.size())return "军团据点之间没有可通行的运输路线";
        if(policy==Policy.CITY_ATTACK){World.City c=w.city(target);if(c==null||!w.campaign.hostile(w.active,c.owner))return "都市攻略需要交战据点";}
        else if(policy==Policy.FORCE_ATTACK){if(target<0||target>=w.factions.length||!w.alive(target)||!w.campaign.hostile(w.active,target))return "势力攻略需要交战势力";}
        else if(target!=-1)return "当前方针不需要攻略目标";
        if(supply!=-1&&(w.city(supply)==null||w.city(supply).owner!=w.active))return "运输目的地必须是己方据点";
        return null;
    }
    public World.Result configure(int id,String name,int[] members,Policy policy,int target,int supply,boolean attack,boolean produce){
        if(members==null)return w.fail("请选择军团据点");members=members.clone();
        String error=configureError(id,name,members,policy,target,supply);if(error!=null)return w.fail(error);
        District d=id<0?new District(nextId++,w.active,name.trim()):get(id);
        d.name=name.trim();d.policy=policy;d.target=target;d.supply=supply;d.attack=attack;d.produce=produce;d.cities.clear();for(int member:members){d.cities.add(member);w.government.policies.remove(member);}
        groups.put(d.id,d);units.entrySet().removeIf(e->{AiOrders.Order order=w.aiOrders.orders.get(e.getKey());return e.getValue()==d.id&&order!=null&&order.home>=0&&!d.cities.contains(order.home);});for(Map.Entry<Integer,Integer> e:units.entrySet())if(e.getValue()==d.id)w.aiOrders.orders.remove(e.getKey());w.actionPoints[w.active]-=20;chooseLeader(d);
        return w.success(d.name+"已编制 · "+policy.label+"；新军团下一旬取得行动力，直属部队与城务交由都督执行");
    }
    public World.Result dissolve(int id){
        String error=manageError();if(error!=null)return w.fail(error);District d=get(id);
        if(d==null||d.owner!=w.active)return w.fail("请选择己方委任军团");
        w.actionPoints[w.active]-=20;groups.remove(id);for(Map.Entry<Integer,Integer> e:units.entrySet())if(e.getValue()==id)w.aiOrders.orders.remove(e.getKey());units.values().removeIf(value->value==id);
        return w.success(d.name+"已撤销，据点与部队回归第一军团，未用军团行动力作废");
    }
    void deployed(int city,World.Unit u){District d=city(city);if(d!=null)units.put(u.id,d.id);}
    void captured(World.City city,World.Unit u){
        for(District d:groups.values())d.cities.remove(city.id);
        District d=unit(u.id);if(d!=null)d.cities.add(city.id);cleanup();
    }
    void cleanup(){
        boolean hasDirect=false;for(World.City c:w.cities)if(c.owner==w.player&&city(c.id)==null)hasDirect=true;
        if(!hasDirect)for(District d:groups.values()){Integer fallback=null;for(int city:d.cities)if(w.city(city)!=null&&w.city(city).owner==w.player){fallback=city;break;}if(fallback!=null){d.cities.remove(fallback);w.note(w.city(fallback).name+"接替第一军团驻地");break;}}
        for(District d:new ArrayList<>(groups.values())){
            d.cities.removeIf(id->w.city(id)==null||w.city(id).owner!=d.owner);
            // The ruler's residence always belongs to the first district.
            for(World.Officer o:w.officers)if(o.owner==d.owner&&o.role==Strategy.Role.RULER)d.cities.remove(o.cityId);
            if(d.cities.isEmpty()){groups.remove(d.id);continue;}
            if(d.supply>=0&&(w.city(d.supply)==null||w.city(d.supply).owner!=d.owner))d.supply=-1;
            if(d.policy==Policy.CITY_ATTACK&&(w.city(d.target)==null||!w.campaign.hostile(d.owner,w.city(d.target).owner))||d.policy==Policy.FORCE_ATTACK&&(!w.alive(d.target)||!w.campaign.hostile(d.owner,d.target))){d.policy=Policy.DEFENSE;d.target=-1;}
            chooseLeader(d);
        }
        units.entrySet().removeIf(e->w.unit(e.getKey())==null||get(e.getValue())==null||w.unit(e.getKey()).owner!=get(e.getValue()).owner);
    }
    private void chooseLeader(District d){
        World.Officer best=null;
        for(World.Officer o:w.officers)if(o.owner==d.owner&&!w.government.captive(o.id)&&d.cities.contains(o.cityId)&&o.role!=Strategy.Role.RULER){
            if(best==null||compareLeader(o,best)<0)best=o;
        }
        d.leader=best==null?-1:best.id;
    }
    private int compareLeader(World.Officer a,World.Officer b){
        int c=Integer.compare(w.government.commandLimit(b.id),w.government.commandLimit(a.id));if(c!=0)return c;
        c=Integer.compare(b.leadership,a.leadership);if(c!=0)return c;c=Integer.compare(b.war,a.war);if(c!=0)return c;
        c=Integer.compare(w.government.merit(b.id),w.government.merit(a.id));return c!=0?c:Integer.compare(a.id,b.id);
    }
    void reset(int owner){for(District d:groups.values())if(d.owner==owner)d.points=60;}
    void run(){
        cleanup();int owner=w.active;
        for(District d:new ArrayList<>(groups.values()))if(d.owner==owner&&d.actedTurn!=w.turn){
            d.actedTurn=w.turn;int mainPoints=w.actionPoints[owner];executing=d.id;w.actionPoints[owner]=d.points;
            try{
                List<Integer> ordered=new ArrayList<>(d.cities);
                // Rotate equal-priority cities so a large district never starves high-ID holdings.
                Collections.rotate(ordered,-(w.turn%ordered.size()));
                CampaignAi planner=new CampaignAi(w);
                ordered.sort(Comparator.comparingInt((Integer id)->{
                    World.City c=w.city(id);return c==null?0:-(planner.incoming(c)+(c.order<65?100000:0));}));
                int startPoints=w.actionPoints[owner];d.report="";d.reportTurn=w.turn;
                if(d.transfer)new DistrictManagement(w).balance(d);
                for(int pass=0;pass<4&&w.actionPoints[owner]>=10;pass++)for(int city:ordered){
                    World.City c=w.city(city);if(c!=null&&c.owner==owner&&w.actionPoints[owner]>=10){
                        int before=w.actionPoints[owner];order(d,c,pass);
                        if(w.actionPoints[owner]<before&&d.report.length()<4500)d.report+=c.name+"："+w.log.get(w.log.size()-1)+"\n";
                    }
                }
                for(World.Unit u:new ArrayList<>(w.units))if(Objects.equals(units.get(u.id),d.id)&&!u.acted)armyOrder(d,u);
                for(int id:d.cities){String reason=new DistrictManagement(w).reason(w.city(id));if(!reason.isEmpty()&&d.report.length()<5000)d.report+=w.city(id).name+"："+reason+"\n";}
                if(w.actionPoints[owner]<10)d.report+="行动预算不足10，剩余城务下旬轮换\n";
                if(d.report.isEmpty())d.report="当前无可执行需求；留守、储备和权限限制继续生效";
                if(startPoints>0)w.note(d.name+"自动经营完成 · 消耗"+(startPoints-w.actionPoints[owner])+"行动力 · 剩余"+w.actionPoints[owner]);
            }finally{d.points=w.actionPoints[owner];w.actionPoints[owner]=mainPoints;executing=-1;}
        }
        cleanup();
    }
    private void order(District d,World.City c,int pass){
        List<World.Officer> idle=w.idle(c);if(idle.isEmpty())return;
        World.Officer admin=idle.stream().max(Comparator.comparingInt(o->o.politics)).get();
        if(new DistrictManagement(w).famineTurn(c)<=3){
            int amount=Math.min(20000-w.campaign.traded(c.id),Math.min(w.campaign.foodCap(c)-c.food,Math.max(0,c.gold-1000)/w.campaign.foodPrice(c.id,true)*1000))/1000*1000;
            if(amount>=1000&&w.campaign.trade(c.id,admin.id,true,amount).ok)return;
        }
        StrategicAi civil=new StrategicAi(w);StrategicAi.Decision urgent=civil.plan(c.id,true);
        if(urgent!=null&&civil.execute(urgent).ok)return;
        if(c.order<65&&w.strategy.patrol(c.id,admin.id).ok)return;
        if(c.defense<w.campaign.defenseCap(c)/2&&w.campaign.repair(c.id,admin.id).ok)return;
        CampaignAi ai=new CampaignAi(w);
        if(pass==0&&d.supplyEnabled&&new DistrictManagement(w).supply(d,c,admin))return;
        if(d.supplyEnabled&&ai.replenish(c.id))return;
        if(d.attack&&d.policy!=Policy.ECONOMY&&ai.deploy(c.id,d.reserveTroops,
            target->d.policy!=Policy.DEFENSE&&(d.policy!=Policy.CITY_ATTACK||target.id==d.target)&&(d.policy!=Policy.FORCE_ATTACK||target.owner==d.target)))return;
        StrategicAi.Decision decision=civil.plan(c.id,false);
        if((d.policy==Policy.DEFENSE||ai.incoming(c)>0)&&decision!=null&&civil.execute(decision).ok)return;
        if(d.produce&&d.attack&&d.policy!=Policy.ECONOMY&&d.policy!=Policy.DEFENSE&&c.equipment[World.Weapon.RAM.ordinal()]==0&&w.domestic.facilities.stream().noneMatch(f->f.cityId==c.id&&f.kind==Domestic.Kind.WORKSHOP)){
            List<Hex> sites=w.domestic.buildSites(c.id);if(!sites.isEmpty()&&w.domestic.build(c.id,admin.id,Domestic.Kind.WORKSHOP,sites.get(0)).ok)return;
        }
        if(d.policy==Policy.ECONOMY||d.policy==Policy.DELEGATE){
            int expectedFood=w.domestic.monthlyFood(c.id);
            for(Domestic.Facility f:w.domestic.facilities)if(f.cityId==c.id&&f.kind==Domestic.Kind.FARM&&f.remaining>0)expectedFood+=w.strategy.cityIncome(c.id,2500*(f.level==3?150:f.level==2?120:100)/100);
            Domestic.Kind kind=expectedFood<w.cityFoodUse(c)*9+(d.supply>=0&&d.supply!=c.id?20000:0)||new DistrictManagement(w).famineTurn(c)<12?Domestic.Kind.FARM:Domestic.Kind.MARKET;
            List<Hex> sites=w.domestic.buildSites(c.id);if(c.gold>=2500&&!sites.isEmpty()&&w.domestic.build(c.id,admin.id,kind,sites.get(0)).ok)return;
        }
        if(d.produce&&c.gold>=d.reserveGold+1500){
            if(d.attack&&d.policy!=Policy.ECONOMY&&d.policy!=Policy.DEFENSE&&c.equipment[World.Weapon.RAM.ordinal()]==0&&
                w.army.productionError(c.id,admin.id,World.Weapon.RAM,null)==null&&w.army.produce(c.id,admin.id,World.Weapon.RAM,null).ok)return;
            World.Weapon preferred=World.Weapon.SPEAR;int best=Integer.MIN_VALUE;
            for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CROSSBOW,World.Weapon.CAVALRY}){
                int rank=0;for(World.Officer o:idle)rank=Math.max(rank,o.aptitude[Army.category(weapon)]);
                int score=rank*10000-c.equipment[weapon.ordinal()];
                if(c.equipment[weapon.ordinal()]<8000&&score>best){best=score;preferred=weapon;}
            }
            if(best>Integer.MIN_VALUE&&w.produce(c.id,admin.id,preferred).ok)return;
        }
        if(decision!=null)civil.execute(decision);
    }
    private World.City target(District d,Hex from){
        List<World.City> candidates=new ArrayList<>();for(World.City c:w.cities)if(w.campaign.hostile(d.owner,c.owner)&&(d.policy!=Policy.CITY_ATTACK||c.id==d.target)&&(d.policy!=Policy.FORCE_ATTACK||c.owner==d.target))candidates.add(c);
        return candidates.stream().min(Comparator.comparingInt((World.City c)->from.distance(c.hex)).thenComparingInt(c->c.id)).orElse(null);
    }
    private void armyOrder(District d,World.Unit u){
        boolean offensive=d.policy!=Policy.DEFENSE&&d.policy!=Policy.ECONOMY;
        new CampaignAi(w).runUnit(u,d.attack,
            c->offensive&&(d.policy!=Policy.CITY_ATTACK||c.id==d.target)&&(d.policy!=Policy.FORCE_ATTACK||c.owner==d.target),
            c->c.owner==d.owner&&d.cities.contains(c.id));
    }
}

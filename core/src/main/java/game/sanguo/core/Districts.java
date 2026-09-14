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
    public boolean directCity(int city){District d=city(city);return d==null||d.owner!=w.player||executing==d.id;}
    public boolean directUnit(int unit){District d=unit(unit);return d==null||d.owner!=w.player||executing==d.id;}
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
        while(changed){changed=false;for(int member:members)if(!linked.contains(member))for(int from:new ArrayList<>(linked))if(w.domestic.route(w.city(from).hex,w.city(member).hex,w.active)!=null){linked.add(member);changed=true;break;}}
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
        groups.put(d.id,d);w.actionPoints[w.active]-=20;chooseLeader(d);
        return w.success(d.name+"已编制 · "+policy.label+"；新军团下一旬取得行动力，直属部队与城务交由都督执行");
    }
    public World.Result dissolve(int id){
        String error=manageError();if(error!=null)return w.fail(error);District d=get(id);
        if(d==null||d.owner!=w.active)return w.fail("请选择己方委任军团");
        w.actionPoints[w.active]-=20;groups.remove(id);units.values().removeIf(value->value==id);
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
                for(int pass=0;pass<4&&w.actionPoints[owner]>=10;pass++)for(int city:new ArrayList<>(d.cities)){
                    World.City c=w.city(city);if(c!=null&&c.owner==owner&&w.actionPoints[owner]>=10)order(d,c,pass);
                }
                for(World.Unit u:new ArrayList<>(w.units))if(Objects.equals(units.get(u.id),d.id)&&!u.acted)armyOrder(d,u);
            }finally{d.points=w.actionPoints[owner];w.actionPoints[owner]=mainPoints;executing=-1;}
        }
        cleanup();
    }
    private void order(District d,World.City c,int pass){
        List<World.Officer> idle=w.idle(c);if(idle.isEmpty())return;
        World.Officer admin=idle.stream().max(Comparator.comparingInt(o->o.politics)).get();
        if(c.order<65&&w.strategy.patrol(c.id,admin.id).ok)return;
        if(c.defense<w.campaign.defenseCap(c)/2&&w.campaign.repair(c.id,admin.id).ok)return;
        if(d.supply>=0&&d.supply!=c.id&&pass==0){int gold=Math.min(3000,Math.max(0,c.gold-5000)),food=Math.min(20000,Math.max(0,c.food-40000));
            if((gold>0||food>0)&&w.domestic.transport(c.id,d.supply,admin.id,gold,food,0,new int[World.Weapon.values().length]).ok)return;}
        CampaignAi ai=new CampaignAi(w);
        if(ai.replenish(c.id))return;
        if(d.attack&&d.policy!=Policy.ECONOMY&&d.policy!=Policy.DEFENSE&&target(d,c.hex)!=null&&ai.deploy(c.id,10000,
            target->(d.policy!=Policy.CITY_ATTACK||target.id==d.target)&&(d.policy!=Policy.FORCE_ATTACK||target.owner==d.target)))return;
        if(d.policy==Policy.ECONOMY||d.policy==Policy.DELEGATE){List<Hex> sites=w.domestic.buildSites(c.id);if(c.gold>=2500&&!sites.isEmpty()&&w.domestic.build(c.id,admin.id,c.food<40000?Domestic.Kind.FARM:Domestic.Kind.MARKET,sites.get(0)).ok)return;}
        if(d.produce&&c.gold>=1500&&c.equipment[0]<8000&&w.produce(c.id,admin.id,World.Weapon.SPEAR).ok)return;
        StrategicAi civil=new StrategicAi(w);StrategicAi.Decision decision=civil.plan(c.id,false);if(decision!=null)civil.execute(decision);
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

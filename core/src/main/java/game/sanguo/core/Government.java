package game.sanguo.core;

import java.util.*;

/** Personnel outcomes live in the campaign, including captured officers who retain their old allegiance.
 * Rank table is community-sourced; capture/recruitment/payroll are explicit sandbox rules (GOVERNANCE_V0_9.md). */
public final class Government {
    public enum Policy { MANUAL("直接管理"), ECONOMY("委任内政"), DEFENSE("委任守备");
        public final String label; Policy(String label){this.label=label;}
    }
    public static final class Rank {
        public final String id; public final int merit,troops,salary;
        Rank(String id,int merit,int troops,int salary){this.id=id;this.merit=merit;this.troops=troops;this.salary=salary;}
    }
    private static final List<Rank> RANKS;
    static {
        List<Rank> ranks=new ArrayList<>();
        String[] names={"奋威校尉,长水校尉,破贼校尉,武卫校尉","忠义校尉,昭信校尉,儒林校尉,建议校尉",
            "牙门将军,护军,偏将军,裨将军","平东将军,平西将军,平南将军,平北将军",
            "军师将军,安国将军,破虏将军,讨逆将军","左将军,右将军,前将军,后将军",
            "安东将军,安西将军,安南将军,安北将军","镇东将军,镇西将军,镇南将军,镇北将军",
            "征东将军,征西将军,征南将军,征北将军","大都督,卫将军,骠骑将军,车骑将军"};
        for(int i=0;i<names.length;i++)for(String name:names[i].split(","))ranks.add(new Rank(name,i*4000,6000+i*1000,10+i*5));
        RANKS=Collections.unmodifiableList(ranks);
    }
    public static List<Rank> ranks(){return RANKS;}
    public static Rank rank(String id){for(Rank r:RANKS)if(r.id.equals(id))return r;return null;}
    public static final class Prisoner {
        public final int officerId,captor,capturedTurn; public int cityId,lastAttempt=-1;
        Prisoner(int officer,int captor,int city,int turn){officerId=officer;this.captor=captor;cityId=city;capturedTurn=turn;}
    }
    final World w;
    final SortedMap<Integer,Integer> merits=new TreeMap<>(),advisors=new TreeMap<>();
    final SortedMap<Integer,String> ranks=new TreeMap<>();
    final SortedMap<Integer,Prisoner> prisoners=new TreeMap<>();
    final SortedMap<Integer,Policy> policies=new TreeMap<>();
    Government(World w){this.w=w;}
    public List<Prisoner> prisoners(){return Collections.unmodifiableList(new ArrayList<>(prisoners.values()));}
    public Prisoner prisoner(int officer){return prisoners.get(officer);}
    public boolean captive(int officer){return prisoners.containsKey(officer);}
    public int merit(int officer){return merits.getOrDefault(officer,0);}
    void earn(int officer,int amount){if(w.officer(officer)!=null&&amount>0)merits.put(officer,Math.min(1000000,merit(officer)+amount));}
    public Rank office(int officer){return rank(ranks.get(officer));}
    /** Unappointed officers keep the existing sandbox's 10000 ceiling; assigning an office uses its real cap. */
    public int commandLimit(int officer){Rank r=office(officer);return r==null?10000:r.troops;}
    public World.Officer advisor(int side){return w.officer(advisors.getOrDefault(side,-1));}
    public Policy policy(int city){return policies.getOrDefault(city,Policy.MANUAL);}
    public String status(int officer){Prisoner p=prisoner(officer);return p==null?"":"被"+w.faction(p.captor)+"俘虏 · 关押于"+w.city(p.cityId).name;}
    public World.Result appointRank(int city,int actor,int target,String rankId){
        World.City c=w.city(city);World.Officer o=w.officer(actor),t=w.officer(target);Rank r=rank(rankId);
        String error=w.cityError(c,o,100);if(error!=null)return w.fail(error);
        if(t==null||t.owner!=c.owner||t.cityId!=city||t.unitId>=0||captive(target)||w.strategy.busy(target)||w.domestic.busy(target)||t.role==Strategy.Role.RULER)return w.fail("需本城未出征或执行任务的非君主武将");
        if(r==null||merit(target)<r.merit)return w.fail("官职无效或武将功绩不足");
        for(Map.Entry<Integer,String> e:ranks.entrySet())if(e.getValue().equals(rankId)&&w.officer(e.getKey()).owner==c.owner)return w.fail("本势力已有武将担任此官职");
        w.spend(c,o,100);ranks.put(target,rankId);t.acted=true;t.loyalty=Math.min(100,t.loyalty+5);
        return w.success(t.name+"受任"+rankId+"，统兵上限"+r.troops+"，月俸"+r.salary);
    }
    public World.Result removeRank(int city,int actor,int target){
        World.City c=w.city(city);World.Officer o=w.officer(actor),t=w.officer(target);String error=w.cityError(c,o,0);if(error!=null)return w.fail(error);
        if(t==null||t.owner!=c.owner||t.cityId!=city||t.unitId>=0||office(target)==null||w.strategy.busy(target)||w.domestic.busy(target))return w.fail("请选择本城可免官武将");
        w.spend(c,o,0);ranks.remove(target);t.acted=true;t.loyalty=Math.max(0,t.loyalty-5);return w.success(t.name+"被免官，忠诚下降5");
    }
    public World.Result appointAdvisor(int city,int actor,int target){
        World.City c=w.city(city);World.Officer o=w.officer(actor),t=w.officer(target);String error=w.cityError(c,o,0);if(error!=null)return w.fail(error);
        if(t==null||!w.idle(c).contains(t)||t.intelligence<70||t.role==Strategy.Role.RULER||advisor(c.owner)==t)return w.fail("军师须为本城未行动、智力至少70的非君主武将");
        w.spend(c,o,0);advisors.put(c.owner,target);t.acted=true;return w.success(t.name+"出任军师");
    }
    public String advice(int city){
        World.City c=w.city(city);if(c==null)return "城池不存在";
        World.Officer adviser=advisor(c.owner);String name=adviser==null?"军政建议":adviser.name+"建议";
        if(c.food<Math.max(6000,c.troops/2))return name+"：粮草不足，先购粮或从后方运输补给。";
        if(c.order<60)return name+"：先巡察恢复治安，再征兵出征。";
        if(w.strategy.strategicPressure(city)>=40&&c.defense<2000)return name+"：边境承压，修复城防并集结守军。";
        if(w.idle(c).isEmpty())return name+"：本城没有闲将，可从其他城池召唤人才。";
        if(!w.strategy.recruitmentTargets(city).isEmpty())return name+"：本城附近有可登用人才。";
        return name+"：保持粮草和兵装储备，利用闲将建设、研究或训练。";
    }
    public World.Result summon(int targetCity,int officer){
        World.Officer o=w.officer(officer);if(o==null||captive(officer))return w.fail("武将不可召唤");
        return w.domestic.transfer(o.cityId,targetCity,officer);
    }
    public World.Result delegate(int city,int actor,Policy p){
        World.City c=w.city(city);World.Officer o=w.officer(actor);String error=w.cityError(c,o,0);if(error!=null)return w.fail(error);
        if(p==null||p==policy(city))return w.fail("委任方针未变化");
        w.spend(c,o,0);if(p==Policy.MANUAL)policies.remove(city);else policies.put(city,p);
        return w.success(c.name+"改为"+p.label+"；结束旬时使用本势力剩余行动力处理一项城务");
    }
    void runDelegated(){
        for(Map.Entry<Integer,Policy> e:new ArrayList<>(policies.entrySet())){
            World.City c=w.city(e.getKey());if(c==null||c.owner!=w.active||w.idle(c).isEmpty())continue;
            World.Officer o=w.idle(c).stream().max(Comparator.comparingInt(x->x.politics)).get();
            if(e.getValue()==Policy.ECONOMY){
                List<Hex> sites=w.domestic.buildSites(c.id);
                if(c.gold>=1500&&!sites.isEmpty()){w.domestic.build(c.id,o.id,c.food<20000?Domestic.Kind.FARM:Domestic.Kind.MARKET,sites.get(0));continue;}
            }
            if(c.defense<2000&&c.gold>=300){w.campaign.repair(c.id,o.id);continue;}
            StrategicAi ai=new StrategicAi(w);StrategicAi.Decision decision=ai.plan(c.id,false);if(decision!=null)ai.execute(decision);
        }
    }
    void allegianceChanged(int officer){
        ranks.remove(officer);advisors.values().removeIf(id->id==officer);
    }
    World.City refuge(int owner,Hex from){return w.cities.stream().filter(c->c.owner==owner)
        .min(Comparator.comparingInt((World.City c)->c.hex.distance(from)).thenComparingInt(c->c.id)).orElse(null);}
    void capture(World.Officer o,World.City jail){
        w.strategy.releaseGovernor(o.id);allegianceChanged(o.id);o.unitId=-1;o.cityId=-1;o.otherTask="";o.otherTaskTurns=0;o.acted=true;
        prisoners.put(o.id,new Prisoner(o.id,jail.owner,jail.id,w.turn));w.note(o.name+"被俘，押往"+jail.name);
    }
    void defeated(World.Unit loser,World.Unit victor){
        List<World.Officer> crew=w.army.crew(loser);boolean blood=w.skills.has(loser,Skill.XUELU);
        boolean hostile=victor!=null&&w.campaign.hostile(victor.owner,loser.owner);
        World.City jail=hostile?refuge(victor.owner,loser.hex):null;
        boolean binding=victor!=null&&w.skills.has(victor,Skill.BOFU);
        int attack=victor==null?0:w.army.war(victor);
        for(World.Officer o:crew){
            boolean caught=jail!=null&&!blood&&!w.skills.has(o,Skill.QIANGYUN)&&
                (binding||w.strategy.nextInt(100)<Math.max(5,Math.min(60,20+(attack-o.war)/5)));
            if(caught)capture(o,jail);else w.retreat(o,loser.hex);
        }
        w.units.remove(loser);
        if(hostile)for(World.Officer o:w.army.crew(victor))earn(o.id,500);
    }
    void cityCaptured(World.City c,int oldOwner,World.Unit victor){
        policies.remove(c.id);
        for(World.Officer o:w.officers)if(o.cityId==c.id&&o.owner==oldOwner&&oldOwner>=0){
            if(!w.skills.has(o,Skill.QIANGYUN)&&w.strategy.nextInt(100)<30)capture(o,c);else w.retreat(o,c.hex);
        }
        relocatePrisoners();
    }
    private void free(Prisoner p){
        World.Officer o=w.officer(p.officerId);World.City jail=w.city(p.cityId),home=refuge(o.owner,jail.hex);prisoners.remove(o.id);
        if(home==null){allegianceChanged(o.id);o.owner=-1;o.cityId=jail.id;o.role=Strategy.Role.UNAFFILIATED;o.loyalty=0;}
        else o.cityId=home.id;
        o.unitId=-1;o.acted=true;w.note(o.name+(home==null?"获释，成为在野武将":"获释返回"+home.name));
    }
    void relocatePrisoners(){
        for(Prisoner p:new ArrayList<>(prisoners.values())){
            World.City jail=w.city(p.cityId);World.Officer o=w.officer(p.officerId);
            if(jail.owner==p.captor)continue;
            if(jail.owner==o.owner){prisoners.remove(o.id);o.cityId=jail.id;o.acted=true;w.note(o.name+"获救");continue;}
            World.City next=refuge(p.captor,jail.hex);if(next==null)free(p);else p.cityId=next.id;
        }
    }
    public int recruitChance(int actor,int target){
        World.Officer o=w.officer(actor),t=w.officer(target);Prisoner p=prisoner(target);
        if(o==null||t==null||p==null||o.owner!=p.captor||t.role==Strategy.Role.RULER&&w.alive(t.owner))return 0;
        return Math.max(5,Math.min(95,20+o.charm/2+o.politics/5-t.loyalty/2+(!w.alive(t.owner)?20:0)));
    }
    private String prisonerError(int city,int actor,int target,int gold){
        String error=w.cityError(w.city(city),w.officer(actor),gold);if(error!=null)return error;
        Prisoner p=prisoner(target);return p==null||p.captor!=w.active||p.cityId!=city?"请选择本城关押的俘虏":null;
    }
    public World.Result recruitPrisoner(int city,int actor,int target){
        String error=prisonerError(city,actor,target,100);if(error!=null)return w.fail(error);
        Prisoner p=prisoner(target);int chance=recruitChance(actor,target);
        if(chance==0||p.lastAttempt==w.turn)return w.fail("在位君主不降，且每名俘虏每旬只能尝试一次");
        World.City c=w.city(city);World.Officer o=w.officer(actor),t=w.officer(target);w.spend(c,o,100);p.lastAttempt=w.turn;
        if(w.strategy.nextInt(100)>=chance)return w.success(t.name+"拒绝招降，费用已消耗");
        prisoners.remove(target);allegianceChanged(target);t.owner=c.owner;t.cityId=city;t.role=Strategy.Role.OFFICER;t.loyalty=70;t.lastRewardTurn=-1;t.acted=true;
        return w.success(t.name+"接受招降，加入"+w.faction(c.owner));
    }
    public World.Result release(int city,int actor,int target){
        String error=prisonerError(city,actor,target,0);if(error!=null)return w.fail(error);
        World.City c=w.city(city);int old=w.officer(target).owner;w.spend(c,w.officer(actor),0);free(prisoner(target));
        w.strategy.setFactionRelation(c.owner,old,Math.min(100,w.strategy.factionRelation(c.owner,old)+10));return w.success("俘虏已释放，双方关系改善");
    }
    public int ransomCost(int officer){World.Officer o=w.officer(officer);return o==null?0:500+10*Math.max(o.war,Math.max(o.intelligence,o.politics));}
    public World.Result ransom(int city,int actor,int target){
        World.City c=w.city(city);World.Officer t=w.officer(target);Prisoner p=prisoner(target);int cost=ransomCost(target);
        String error=w.cityError(c,w.officer(actor),cost);if(error!=null)return w.fail(error);
        if(p==null||t.owner!=c.owner||p.captor==c.owner)return w.fail("请选择被其他势力俘虏的己方武将");
        World.City jail=w.city(p.cityId);if(jail.gold>1000000-cost)return w.fail("对方城池金库存已满");
        w.spend(c,w.officer(actor),cost);jail.gold+=cost;free(p);return w.success("支付"+cost+"金赎回"+t.name);
    }
    void tick(){
        relocatePrisoners();
        advisors.entrySet().removeIf(e->{World.Officer o=w.officer(e.getValue());return o==null||o.owner!=e.getKey()||captive(o.id);});
        for(Prisoner p:new ArrayList<>(prisoners.values())){
            World.Officer o=w.officer(p.officerId);if(o.role!=Strategy.Role.RULER)o.loyalty=Math.max(0,o.loyalty-2);
            if(w.turn>p.capturedTurn&&w.strategy.nextInt(100)<5)free(p);
        }
        if(w.turn%3==0)for(Map.Entry<Integer,String> e:ranks.entrySet()){
            World.Officer o=w.officer(e.getKey());if(captive(o.id))continue;
            World.City c=o.cityId>=0?w.city(o.cityId):o.unitId>=0?refuge(o.owner,w.unit(o.unitId).hex):null;
            if(c!=null){int pay=rank(e.getValue()).salary;if(c.gold>=pay)c.gold-=pay;else{o.loyalty=Math.max(0,o.loyalty-2);w.note(o.name+"俸禄不足，忠诚下降2");}}
        }
    }
    void runAi(){
        for(Prisoner p:new ArrayList<>(prisoners.values()))if(p.captor==w.active&&p.lastAttempt!=w.turn){
            World.City c=w.city(p.cityId);if(c.gold<100||w.idle(c).isEmpty())continue;
            World.Officer o=w.idle(c).stream().max(Comparator.comparingInt(x->x.charm)).get();
            if(recruitChance(o.id,p.officerId)>=50)recruitPrisoner(c.id,o.id,p.officerId);
        }
    }
}

package game.sanguo.core;

import game.sanguo.core.strategy.StrategyRules;
import java.io.*;
import java.util.*;

/** Strategic personnel and administration commands. Validate before mutation; no Android dependency. */
public final class Strategy {
    public static final int SEARCH_COST=0, HIRE_COST=100, REWARD_COST=200;
    public static final int PATROL_COST=100, RECRUIT_COST=300, TRAIN_COST=100;
    public static final int DEFAULT_RESERVE=20000, MAX_ENEMY_LOYALTY=60, RECRUIT_RANGE=6;

    public enum Role {
        RULER("君主"), GOVERNOR("太守"), OFFICER("普通武将"), UNAFFILIATED("在野");
        public final String label;
        Role(String label) { this.label=label; }
    }
    public enum Activity { IDLE, ACTED, CONSTRUCTION, TRANSFER, TRANSPORT, OTHER_TASK, DEPLOYED, UNAFFILIATED, UNAVAILABLE, CAPTIVE, UNAPPEARED, DEAD }
    public enum SearchOutcome { REJECTED, OFFICER, GOLD, NOTHING, TREASURE }

    /** Snapshot derived from the actual assignments, never a second mutable task registry. */
    public static final class OfficerState {
        public final int officerId, cityId, faction, loyalty, remainingTurns;
        public final Role role;
        public final Activity activity;
        public final boolean acted, canAct;
        private OfficerState(World.Officer o, Activity activity, int remaining, boolean canAct) {
            officerId=o.id; cityId=o.cityId; faction=o.owner; loyalty=o.loyalty;
            role=o.role; acted=o.acted; this.activity=activity; remainingTurns=remaining; this.canAct=canAct;
        }
    }
    public static final class SearchResult {
        public final World.Result result;
        public final SearchOutcome outcome;
        public final int officerId, goldFound;
        private SearchResult(World.Result result, SearchOutcome outcome, int officerId, int gold) {
            this.result=result; this.outcome=outcome; this.officerId=officerId; goldFound=gold;
        }
    }
    /** Unrevealed scenario data; a successful search moves this record into World.officers. */
    public static final class Talent {
        public final int id, cityId, leadership, war, intelligence, politics, charm, availableTurn;
        public final String name;
        public Talent(int id, String name, int cityId, int leadership, int war, int intelligence,
                      int politics, int charm, int availableTurn) {
            this.id=id; this.name=name; this.cityId=cityId; this.leadership=leadership; this.war=war;
            this.intelligence=intelligence; this.politics=politics; this.charm=charm; this.availableTurn=availableTurn;
        }
        World.Officer reveal() {
            return new World.Officer(id,name,-1,cityId,leadership,war,intelligence,politics,charm);
        }
    }

    final World w;
    final List<Talent> talents=new ArrayList<>();
    final SortedMap<Long,Integer> relations=new TreeMap<>();
    long randomState=0x53414E31314D4F42L;
    Strategy(World w) { this.w=w; }

    /** Scenario/test configuration, not a player command. State is included in save v4. */
    public void setSeed(long seed) { randomState=seed; }
    public long getRandomState() { return randomState; }
    int nextInt(int bound) {
        // SplitMix64; integer-only, identical on JVM and Android, with explicit serializable state.
        long z=(randomState+=0x9E3779B97F4A7C15L);
        z=(z^(z>>>30))*0xBF58476D1CE4E5B9L;
        z=(z^(z>>>27))*0x94D049BB133111EBL;
        return (int)(((z^(z>>>31))>>>1)%bound);
    }
    public List<Talent> hiddenTalents() { return Collections.unmodifiableList(talents); }
    public void addHiddenTalent(Talent talent) {
        try { StrategySave.validateTalent(w,talent); }
        catch(IOException e) { throw new IllegalArgumentException(e.getMessage(),e); }
        if(w.officer(talent.id)!=null||talents.stream().anyMatch(t->t.id==talent.id))
            throw new IllegalArgumentException("人才编号重复");
        if(talents.size()+w.officers.size()>=10000) throw new IllegalArgumentException("武将数量上限");
        talents.add(talent);
    }
    private long relationKey(int a,int b) {
        if(a<0||b<0||a>=w.factions.length||b>=w.factions.length||a==b)
            throw new IllegalArgumentException("势力关系引用无效");
        return ((long)Math.min(a,b)<<32)|Math.max(a,b);
    }
    /** Optional scenario-level relation modifier; this is not a diplomacy command. */
    public void setFactionRelation(int a,int b,int value) {
        long key=relationKey(a,b);
        if(value<-100||value>100)throw new IllegalArgumentException("关系范围为-100至100");
        if(value==0)relations.remove(key);else relations.put(key,value);
    }
    public int factionRelation(int a,int b) { return relations.getOrDefault(relationKey(a,b),0); }

    public boolean busy(int officerId) {
        World.Officer o=w.officer(officerId); return o!=null&&(o.otherTaskTurns>0||w.government.captive(officerId));
    }
    public OfficerState officerState(int officerId) {
        World.Officer o=w.officer(officerId);
        if(o==null)throw new IllegalArgumentException("武将不存在");
        Activity activity=Activity.IDLE; int remaining=0;
        if(!w.life.present(o.id))activity=w.life.state(o.id)==Lifecycle.State.DEAD?Activity.DEAD:Activity.UNAPPEARED;
        else if(w.government.captive(o.id))activity=Activity.CAPTIVE;
        else if(o.unitId>=0)activity=Activity.DEPLOYED;
        else {
            for(Domestic.Facility f:w.domestic.facilities)if(f.builderId==o.id){activity=Activity.CONSTRUCTION;remaining=f.remaining;break;}
            if(activity==Activity.IDLE)for(Domestic.Mission m:w.domestic.missions)if(m.contains(o.id)){activity=m.transport?Activity.TRANSPORT:Activity.TRANSFER;remaining=w.domestic.eta(m);break;}
            if(activity==Activity.IDLE&&o.otherTaskTurns>0){activity=Activity.OTHER_TASK;remaining=o.otherTaskTurns;}
            if(activity==Activity.IDLE){
                World.City c=w.city(o.cityId);
                if(o.owner<0)activity=Activity.UNAFFILIATED;
                else if(c==null||c.owner!=o.owner)activity=Activity.UNAVAILABLE;
                else if(o.acted)activity=Activity.ACTED;
            }
        }
        return new OfficerState(o,activity,remaining,activity==Activity.IDLE&&o.owner==w.active&&!w.gameOver());
    }
    /** Reserved for future non-conflicting strategic assignments, with a real multi-turn lock. */
    public World.Result beginAssignment(int cityId,int officerId,String label,int turns) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,0);
        if(error!=null)return w.fail(error);
        if(label==null||label.trim().isEmpty()||label.trim().startsWith("PK培养")||label.length()>80||turns<1||turns>12)return w.fail("任务名称或工期无效");
        w.spend(c,o,0);o.otherTask=label.trim();o.otherTaskTurns=turns;
        return w.success(o.name+"执行"+o.otherTask+"，需要"+turns+"旬");
    }
    public int searchChance(int officerId) {
        World.Officer o=w.officer(officerId);
        return o==null?0:w.skills.has(o,Skill.YANLI)?100:StrategyRules.searchChance(o.politics,o.intelligence);
    }
    List<Talent> discoverable(int cityId) {
        List<Talent> list=new ArrayList<>();
        for(Talent t:talents)if(t.cityId==cityId&&t.availableTurn<=w.turn)list.add(t);
        list.sort(Comparator.comparingInt(t->t.id));return list;
    }
    public World.Result search(int cityId,int officerId) { return searchTalent(cityId,officerId).result; }
    public SearchResult searchTalent(int cityId,int officerId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,SEARCH_COST);
        if(error!=null)return new SearchResult(w.fail(error),SearchOutcome.REJECTED,-1,0);
        List<Talent> candidates=discoverable(cityId);
        w.spend(c,o,SEARCH_COST);int roll=nextInt(100);
        if(!candidates.isEmpty()&&StrategyRules.succeeds(searchChance(o.id),roll)) {
            Talent talent=candidates.get(nextInt(candidates.size()));
            talents.remove(talent);w.officers.add(talent.reveal());
            return new SearchResult(w.success(o.name+"在"+c.name+"发现了在野武将"+talent.name),SearchOutcome.OFFICER,talent.id,0);
        }
        String treasure=w.treasures.discover(cityId,officerId,roll);
        if(treasure!=null)return new SearchResult(w.success(o.name+"搜索发现宝物"+treasure+"，已入府库"),SearchOutcome.TREASURE,-1,0);
        if(nextInt(100)<10+o.politics/5) {
            int gold=Math.min(Math.max(0,w.campaign.goldCap(c)-c.gold),30+nextInt(91));
            if(gold>0){c.gold+=gold;return new SearchResult(w.success(o.name+"搜索获得金"+gold),SearchOutcome.GOLD,-1,gold);}
        }
        return new SearchResult(w.success(o.name+"搜索结束，未有发现"),SearchOutcome.NOTHING,-1,0);
    }
    public boolean canRecruitTarget(int cityId,int targetId) {
        World.City c=w.city(cityId);World.Officer t=w.officer(targetId);
        if(c==null||c.owner!=w.active||t==null||t.owner==c.owner||t.unitId>=0||busy(t.id)||w.domestic.busy(t.id))return false;
        if(w.relations.loyalBond(targetId))return false;
        if(t.owner==-1)return t.cityId==c.id&&t.role==Role.UNAFFILIATED;
        World.City source=w.city(t.cityId);
        return !t.acted&&t.role!=Role.RULER&&t.loyalty<=MAX_ENEMY_LOYALTY&&source!=null&&source.owner==t.owner&&source.hex.distance(c.hex)<=RECRUIT_RANGE;
    }
    public List<World.Officer> recruitmentTargets(int cityId) {
        List<World.Officer> result=new ArrayList<>();
        for(World.Officer o:w.officers)if(canRecruitTarget(cityId,o.id))result.add(o);
        result.sort(Comparator.comparingInt(o->o.id));return result;
    }
    public int recruitmentChance(int cityId,int officerId,int targetId) {
        World.Officer o=w.officer(officerId),target=w.officer(targetId);
        if(o==null||!canRecruitTarget(cityId,targetId)||w.relations.refuses(targetId,officerId,w.active))return 0;
        return Math.min(100,w.relations.recruitmentBonus(targetId,officerId,w.active)+StrategyRules.recruitmentChance(o.charm,o.politics,target.loyalty,target.owner<0,
                target.owner<0?0:factionRelation(w.city(cityId).owner,target.owner)));
    }
    public World.Result recruitOfficer(int cityId,int officerId,int targetId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,HIRE_COST);
        if(error!=null)return w.fail(error);
        if(!canRecruitTarget(cityId,targetId))return w.fail("目标须为本城在野武将，或六格内未行动、忠诚不高于60的敌方非君主闲将");
        World.Officer target=w.officer(targetId);int chance=recruitmentChance(cityId,officerId,targetId);
        if(w.relations.refuses(targetId,officerId,c.owner))return w.fail("目标因结义、配偶或厌恶关系拒绝登用");
        w.spend(c,o,HIRE_COST);
        if(!StrategyRules.succeeds(chance,nextInt(100)))return w.success(o.name+"登用"+target.name+"未成功（成功率"+chance+"%）");
        releaseGovernor(target.id);w.government.allegianceChanged(target.id);target.owner=c.owner;target.cityId=c.id;target.role=Role.OFFICER;
        target.loyalty=Math.min(100,60+o.charm/10+o.politics/5);target.lastRewardTurn=-1;target.acted=true;
        return w.success(target.name+"加入"+w.faction(c.owner)+"，本旬休整");
    }
    public World.Result rewardOfficer(int cityId,int officerId,int targetId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,REWARD_COST);
        if(error!=null)return w.fail(error);
        World.Officer target=w.officer(targetId);
        if(target==null||target.owner!=c.owner||target.cityId!=c.id||target.unitId>=0||busy(target.id)||w.domestic.busy(target.id))return w.fail("只能褒奖本城未执行长期任务的己方武将");
        if(target.role==Role.RULER||target.loyalty>=100)return w.fail("目标忠诚已满或为君主");
        if(target.lastRewardTurn==w.turn)return w.fail("同一武将每旬只能接受一次褒奖");
        int gain=Math.min(100-target.loyalty,StrategyRules.rewardGain(target.politics,target.charm));
        w.spend(c,o,REWARD_COST);target.loyalty+=gain;target.lastRewardTurn=w.turn;
        return w.success("褒奖"+target.name+"，忠诚+"+gain);
    }
    public World.Result appointGovernor(int cityId,int officerId,int targetId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,0);
        if(error!=null)return w.fail(error);
        World.Officer target=w.officer(targetId);
        if(target==null||!w.idle(c).contains(target))return w.fail("太守须为本城本旬可行动的己方武将");
        if(c.governorId==target.id)return w.fail("该武将已经是本城太守");
        if(c.governorId>=0)releaseGovernor(c.governorId);
        releaseGovernor(target.id);w.spend(c,o,0);c.governorId=target.id;target.acted=true;
        if(target.role!=Role.RULER)target.role=Role.GOVERNOR;
        return w.success(target.name+"出任"+c.name+"太守，金粮收入加成"+(target.politics/4)+"%");
    }
    public int governorPolitics(int cityId) {
        World.City c=w.city(cityId);World.Officer o=c==null?null:w.officer(c.governorId);
        return o!=null&&o.cityId==c.id&&o.unitId<0&&o.owner==c.owner&&(o.role==Role.GOVERNOR||o.role==Role.RULER)?o.politics:-1;
    }
    public int cityIncome(int cityId,int base) {
        World.City c=w.city(cityId);return c==null?0:StrategyRules.income(base,c.order,governorPolitics(cityId));
    }
    void releaseGovernor(int officerId) {
        for(World.City c:w.cities)if(c.governorId==officerId)c.governorId=-1;
        World.Officer o=w.officer(officerId);if(o!=null&&o.role==Role.GOVERNOR)o.role=Role.OFFICER;
    }
    void cityCaptured(int cityId) {
        World.City c=w.city(cityId);if(c.governorId>=0)releaseGovernor(c.governorId);
        for(World.Officer o:w.officers)if(o.cityId==cityId&&o.owner!=c.owner){o.otherTaskTurns=0;o.otherTask="";}
    }
    /** Deterministic migration/default leadership. Does not auto-appoint a governor or invent talent. */
    void initializeOffices() {
        for(int side=0;side<w.factions.length;side++) {
            World.Officer first=null;boolean hasRuler=false;
            for(World.Officer o:w.officers)if(o.owner==side){hasRuler|=o.role==Role.RULER;if(first==null||o.id<first.id)first=o;}
            if(!hasRuler&&first!=null){first.role=Role.RULER;first.loyalty=100;}
        }
    }
    public World.Result patrol(int cityId,int officerId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,PATROL_COST);
        if(error!=null)return w.fail(error);
        if(c.order>=100)return w.fail("治安已满");
        int gain=Math.min(100-c.order,StrategyRules.patrolGain(o.politics,o.charm));
        w.spend(c,o,PATROL_COST);c.order+=gain;return w.success(c.name+"巡察，治安+"+gain);
    }
    public int recruitAmount(int cityId,int officerId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);
        return c==null||o==null?0:Math.min(c.recruitReserve,StrategyRules.enlistment(w.domestic.recruitAmount(cityId),c.order,o.charm,c.recruitReserve)*(w.skills.has(o,Skill.MINGSHENG)?150:100)/100);
    }
    public World.Result recruitSoldiers(int cityId,int officerId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,RECRUIT_COST);
        if(error!=null)return w.fail(error);
        if(c.order<30)return w.fail("治安低于30，先执行巡察");
        if(c.recruitReserve<=0)return w.fail("本城兵源已耗尽");
        int amount=recruitAmount(cityId,officerId);
        if(amount<=0||c.troops>w.campaign.troopCap(c)-amount)return w.fail("城池兵力已接近上限");
        w.spend(c,o,RECRUIT_COST);c.recruitReserve-=amount;c.troops+=amount;c.order=Math.max(0,c.order-w.campaign.orderLoss(c.owner,w.skills.has(o,Skill.MINGSHENG)?7:5));
        return w.success(c.name+"征得"+amount+"兵，治安−5，兵源剩余"+c.recruitReserve);
    }
    public int getArmyReadiness(int cityId) {
        World.City c=w.city(cityId);if(c==null)throw new IllegalArgumentException("城池不存在");return c.morale;
    }
    public World.Result trainArmy(int cityId,int officerId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,TRAIN_COST);
        if(error!=null)return w.fail(error);
        if(c.troops<=0)return w.fail("本城没有可训练的军队");
        if(c.morale>=w.campaign.energyCap(c.owner))return w.fail("气力已满");
        int gain=Math.min(w.campaign.energyCap(c.owner)-c.morale,StrategyRules.trainingGain(o.leadership,o.war));
        w.spend(c,o,TRAIN_COST);c.morale+=gain;return w.success(c.name+"训练，气力+"+gain);
    }
    void tick() {
        for(World.Officer o:w.officers) {
            if(o.otherTaskTurns>0&&--o.otherTaskTurns==0){o.otherTask="";o.acted=true;w.note(o.name+"完成战略任务");}
            World.City c=w.city(o.cityId);
            if(!w.relations.loyalBond(o.id)&&!w.skills.city(o.cityId,Skill.RENZHENG)&&w.turn%3==0&&o.owner>=0&&o.role!=Role.RULER&&c!=null&&c.owner==o.owner&&(c.order<40||c.gold<200))
                o.loyalty=Math.max(0,o.loyalty-w.campaign.loyaltyLoss(o.owner,2));
        }
    }
    public StrategicAi.Decision planAi(int cityId) { return new StrategicAi(w).plan(cityId,false); }
    public int strategicPressure(int cityId) { return new StrategicAi(w).pressure(w.city(cityId)); }
    void runAi(boolean emergency) { new StrategicAi(w).run(emergency); }
    void write(DataOutputStream out)throws IOException { StrategySave.write(this,out); }
    void read(DataInputStream in)throws IOException { StrategySave.read(this,in); }
    void validate()throws IOException { StrategySave.validate(this); }
}

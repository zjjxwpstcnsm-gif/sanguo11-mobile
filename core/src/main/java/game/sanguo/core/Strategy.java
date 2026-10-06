package game.sanguo.core;

import game.sanguo.core.strategy.StrategyRules;
import java.io.*;
import java.util.*;

/** Strategic personnel and administration commands. Validate before mutation; no Android dependency. */
public final class Strategy {
    public static final int SEARCH_COST=0, HIRE_COST=100, REWARD_COST=200;
    public static final int PATROL_COST=100, RECRUIT_COST=300, TRAIN_COST=100;
    public static final int DEFAULT_RESERVE=Conscription.RESERVE_CAP, MAX_ENEMY_LOYALTY=60, RECRUIT_RANGE=6;

    public enum Role {
        RULER("君主"), GOVERNOR("太守"), OFFICER("普通武将"), UNAFFILIATED("在野"), DISTRICT("都督");
        public final String label;
        Role(String label) { this.label=label; }
    }
    public enum Activity { IDLE, ACTED, CONSTRUCTION, TRANSFER, TRANSPORT, OTHER_TASK, DEPLOYED, UNAFFILIATED, UNAVAILABLE, CAPTIVE, UNAPPEARED, DEAD, UNDISCOVERED }
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
        SearchResult(World.Result result, SearchOutcome outcome, int officerId, int gold) {
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
        if(!w.life.present(o.id))activity=w.life.state(o.id)==Lifecycle.State.DEAD?Activity.DEAD:w.life.state(o.id)==Lifecycle.State.UNDISCOVERED?Activity.UNDISCOVERED:Activity.UNAPPEARED;
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
    public World.Result beginAssignment(int cityId,int officerId,String label,int turns) {w.reports.prepare();
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
    public boolean originalSearchChoiceEnabled(){return PcSearchPolicy.operative(w);}
    int cityActionBaseCost(CityActionPlan.Operation op){
        if(op==CityActionPlan.Operation.SEARCH&&PcSearchPolicy.operative(w))return 20;
        if(!BasicCityPolicy.nativeRules(w))return 10;
        if(op==null)return 10;
        return switch(op){case PATROL->PcCityActionCosts.PATROL;case TRAIN->PcCityActionCosts.TRAIN;case RECRUIT->PcCityActionCosts.RECRUIT;default->10;};
    }
    static long cityActionGold(CityActionPlan.Operation op,int[] targets){
        if(op==null)return 0;
        return switch(op){case PATROL->PATROL_COST;case TRAIN->TRAIN_COST;case RECRUIT->RECRUIT_COST;case SEARCH->SEARCH_COST;case REWARD->(long)REWARD_COST*(targets==null?0:targets.length);case APPOINT_GOVERNOR->0;};
    }
    RuleFailure cityActionFailure(CityActionPlan.Operation op,int cityId,int officerId,int[] targets){
        if(op==null)return new RuleFailure("CITY_ACTION_INVALID","operation","城市指令无效");
        if(op==CityActionPlan.Operation.REWARD){
            if(targets==null||targets.length==0||targets.length>w.officers.size())return new RuleFailure("REWARD_SELECTION","targets","请选择至少一名可褒奖武将");
            if(cityActionGold(op,targets)>Integer.MAX_VALUE)return new RuleFailure("REWARD_COUNT","targets","褒奖人数过多");
        }else if(targets==null||targets.length!=(op==CityActionPlan.Operation.APPOINT_GOVERNOR?1:0))return new RuleFailure("CITY_ACTION_TARGETS","targets","城市指令目标数量无效");
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);RuleFailure common=w.cityFailure(c,o,(int)cityActionGold(op,targets),cityActionBaseCost(op));if(common!=null)return common;
        switch(op){
            case PATROL:if(c.order>=100)return new RuleFailure("ORDER_FULL","city","治安已满");break;
            case TRAIN:
                if(c.troops<=0)return new RuleFailure("NO_TROOPS","city","本城没有可训练的军队");
                if(c.morale>=w.campaign.energyCap(c.owner))return new RuleFailure("MORALE_FULL","city","气力已满");break;
            case RECRUIT:
                if(c.kind!=World.SiteKind.CITY)return new RuleFailure("CITY_TYPE","city","港口和关卡不能征兵，请从城市运输兵员");
                String error=w.domestic.operationError(cityId,Domestic.Kind.BARRACKS);if(error!=null)return new RuleFailure("BARRACKS_UNAVAILABLE","facility",error);
                if(c.order<30)return new RuleFailure("ORDER_LOW","city","治安低于30，先执行巡察");
                if(c.recruitReserve<=0)return new RuleFailure("RECRUIT_RESERVE_EMPTY","city","本城兵源已耗尽");
                int amount=recruitAmount(cityId,officerId);if(amount<=0||c.troops>w.campaign.troopCap(c)-amount)return new RuleFailure("TROOP_CAPACITY","city","城池兵力已接近上限");break;
            case REWARD:
                Set<Integer> seen=new HashSet<>();for(int id:targets)if(!seen.add(id)||!rewardable(c,w.officer(id)))return new RuleFailure("REWARD_TARGET","targets","选择中有重复、已褒奖、忠诚已满或不在城的武将；未扣款");break;
            case APPOINT_GOVERNOR:
                if(PcGovernorPolicy.recognized(w))return new RuleFailure("GOVERNOR_AUTOMATIC","targets","\u539f\u6765\u6e90\u9009\u4efb\u7b56\u7565\u6839\u636e\u90fd\u7763\u8eab\u4efd\u3001\u6307\u6325\u3001\u7edf\u7387\u3001\u6b66\u529b\u548c\u529f\u7ee9\u81ea\u52a8\u9009\u4efb\u592a\u5b88\uff1b\u624b\u52a8\u6539\u4efb\u5c1a\u672a\u6838\u5b9e");
                World.Officer target=w.officer(targets[0]);if(target==null||!w.idle(c).contains(target))return new RuleFailure("GOVERNOR_TARGET","targets","太守须为本城本旬可行动的己方武将");
                if(c.governorId==target.id)return new RuleFailure("GOVERNOR_UNCHANGED","targets","该武将已经是本城太守");break;
            default:break;
        }
        return null;
    }
    public CityActionPlan previewCityAction(CityActionPlan.Operation op,int cityId,int officerId,int[] targets){
        int[] selected=targets==null?null:targets.clone();RuleFailure failure=cityActionFailure(op,cityId,officerId,selected);
        return new CityActionPlan(w,op,w.city(cityId),w.officer(officerId),selected,failure);
    }
    public World.Result executeCityAction(CityActionPlan.Operation op,int cityId,int officerId,int[] targets){
        RuleFailure failure=cityActionFailure(op,cityId,officerId,targets);if(failure!=null)return w.fail(failure.detail);
        return switch(op){case PATROL->patrol(cityId,officerId);case TRAIN->trainArmy(cityId,officerId);case RECRUIT->recruitSoldiers(cityId,officerId);case SEARCH->search(cityId,officerId);case REWARD->rewardOfficers(cityId,officerId,targets);case APPOINT_GOVERNOR->appointGovernor(cityId,officerId,targets[0]);};
    }
    int patrolGain(World.City c,World.Officer o){
        if(!BasicCityPolicy.nativeRules(w))return Math.min(100-c.order,BasicCityPolicy.legacyPatrol(o.politics,o.charm));
        boolean nearbyEnemy=false;
        // Original4843a0 visits rings1..3 around a city, excluding its center.
        // Use the existing authoritative treaty state; native relation-storage
        // parity and runtime age/item modifiers remain separately tracked.
        int radius=c.kind==World.SiteKind.CITY?3:2;
        for(World.Unit u:w.units){int distance=c.hex.distance(u.hex);
            if(u.owner>=0&&u.owner<w.factions.length&&distance>=1&&distance<=radius&&w.campaign.hostile(u.owner,c.owner)){nearbyEnemy=true;break;}}
        return PcPatrolRules.gain(o.leadership,c.order,nearbyEnemy);
    }
    int trainingGain(World.City c,World.Officer o){return Math.min(w.campaign.energyCap(c.owner)-c.morale,StrategyRules.trainingGain(o.leadership,o.war));}
    int recruitOrderLoss(World.City c,World.Officer o){return Math.min(c.order,w.campaign.orderLoss(c.owner,w.skills.has(o,Skill.MINGSHENG)?7:5));}
    static int rewardGain(World.Officer o){return Math.min(100-o.loyalty,StrategyRules.rewardGain(o.politics,o.charm));}
    List<Talent> discoverable(int cityId) {
        List<Talent> list=new ArrayList<>();
        for(Talent t:talents)if(t.cityId==cityId&&t.availableTurn<=w.turn)list.add(t);
        list.sort(Comparator.comparingInt(t->t.id));return list;
    }
    public World.Result search(int cityId,int officerId) {w.reports.prepare(); return searchTalent(cityId,officerId).result; }
    public SearchResult searchTalent(int cityId,int officerId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);RuleFailure failure=cityActionFailure(CityActionPlan.Operation.SEARCH,cityId,officerId,new int[0]);
        if(failure!=null)return new SearchResult(w.fail(failure.detail),SearchOutcome.REJECTED,-1,0);
        if(PcSearchPolicy.operative(w)){try{SearchResult found=PcSearchPolicy.found(w,c,o);if(found!=null)return found;}catch(java.io.IOException e){throw new IllegalStateException(e);}}
        List<Talent> candidates=discoverable(cityId);
        List<World.Officer> originals=PcSearchPolicy.handlesActor(w,o)?Collections.emptyList():w.life.undiscovered(cityId);
        w.spend(c,o,SEARCH_COST,cityActionBaseCost(CityActionPlan.Operation.SEARCH));int roll=nextInt(100);
        if(!originals.isEmpty()&&StrategyRules.succeeds(searchChance(o.id),roll)) {
            World.Officer discovered=originals.get(nextInt(originals.size()));w.life.discover(discovered.id);
            return new SearchResult(w.success(o.name+"在"+c.name+"发现了在野武将"+discovered.name),SearchOutcome.OFFICER,discovered.id,0);
        }
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
    public boolean canRecruitTarget(int cityId, int targetId) {
        World.City c = this.w.city(cityId);
        return c != null && c.owner == this.w.active && recruitable(c.owner, targetId) && !this.w.recruitment.pending(c.owner, targetId);
    }
    public List<World.Officer> recruitmentTargets(int cityId) {
        List<World.Officer> result=new ArrayList<>();
        for(World.Officer o:w.officers)if(canRecruitTarget(cityId,o.id))result.add(o);
        Map<Integer,Integer> travel=new HashMap<>();for(World.Officer o:result)travel.put(o.id,w.recruitment.travelTurns(cityId,o.id));
        result.sort(Comparator.comparingInt((World.Officer o)->travel.get(o.id)).thenComparingInt(o->o.id));
        return result;
    }
    public int recruitmentChance(int cityId, int officerId, int targetId) {
        World.City city = this.w.city(cityId);
        if (city == null) {
            return 0;
        }
        try{World.Officer actor=w.officer(officerId),target=w.officer(targetId);if(PcDirectRecruitmentPolicy.eligible(w,city,actor,target)!=null)return PcDirectRecruitmentPolicy.probability(w,actor,target);}catch(java.io.IOException e){throw new IllegalStateException(e);}
        return recruitChance(city.owner, officerId, targetId);
    }
    public RecruitmentPlan previewRecruitment(int cityId,int officerId,int targetId){return new RecruitmentPlan(w,cityId,officerId,targetId);}
    public World.Result recruitOfficer(int cityId, int officerId, int targetId) {w.reports.prepare();
        if(PcDirectRecruitmentPolicy.enabled(w)){RecruitmentPlan plan=previewRecruitment(cityId,officerId,targetId);if(!plan.allowed())return w.fail(plan.failure.detail);if(plan.nativeRules)try{return PcDirectRecruitmentPolicy.execute(w,plan);}catch(java.io.IOException e){throw new IllegalStateException(e);}}
        World.City c = this.w.city(cityId);
        World.Officer o = this.w.officer(officerId);
        String error = this.w.cityError(c, o, 100);
        if (error != null) {
            return this.w.fail(error);
        }
        if (!canRecruitTarget(cityId, targetId)) {
            return this.w.fail("目标须为已登场、未被俘或执行任务的在野武将或其他势力非君主武将；不能重复派遣");
        }
        World.Officer target = this.w.officer(targetId);
        int chance = recruitmentChance(cityId, officerId, targetId);
        if (this.w.relations.refuses(targetId, officerId, c.owner)) {
            return this.w.fail("目标因结义、配偶或厌恶关系拒绝登用");
        }
        if (chance == 0) {
            return this.w.fail("当前登用成功率为0，请先改善关系或降低目标忠诚");
        }
        this.w.spend(c, o, 100);
        if (target.cityId != cityId) {
            return this.w.recruitment.start(c, o, target);
        }
        if (!StrategyRules.succeeds(chance, nextInt(100))) {
            return this.w.success(o.name + "登用" + target.name + "未成功（成功率" + chance + "%）");
        }
        join(o, target, c.id);
        return this.w.success(target.name + "加入" + this.w.faction(c.owner) + "，本旬休整");
    }
    public boolean rewardable(World.City c,World.Officer t){
        return c!=null&&t!=null&&w.life.present(t.id)&&t.owner==c.owner&&t.cityId==c.id&&t.unitId<0&&!busy(t.id)&&!w.domestic.busy(t.id)&&t.role!=Role.RULER&&t.loyalty<100&&t.lastRewardTurn!=w.turn;
    }
    public World.Result rewardOfficer(int cityId,int officerId,int targetId){return rewardOfficers(cityId,officerId,new int[]{targetId});}
    /** Validate the entire selection before spending: one action, 200 gold per unique recipient. */
    public World.Result rewardOfficers(int cityId,int officerId,int[] targets){w.reports.prepare();
        RuleFailure failure=cityActionFailure(CityActionPlan.Operation.REWARD,cityId,officerId,targets);if(failure!=null)return w.fail(failure.detail);
        long price=cityActionGold(CityActionPlan.Operation.REWARD,targets);World.City c=w.city(cityId);World.Officer actor=w.officer(officerId);
        w.spend(c,actor,(int)price);for(int id:targets){World.Officer t=w.officer(id);int gain=rewardGain(t);t.loyalty+=gain;t.lastRewardTurn=w.turn;w.note("褒奖"+t.name+"，忠诚+"+gain);}
        return w.success("批量褒奖"+targets.length+"人，金−"+price+"、行动力−10");
    }
    public World.Result appointGovernor(int cityId,int officerId,int targetId) {w.reports.prepare();
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);RuleFailure failure=cityActionFailure(CityActionPlan.Operation.APPOINT_GOVERNOR,cityId,officerId,new int[]{targetId});
        if(failure!=null)return w.fail(failure.detail);World.Officer target=w.officer(targetId);
        if(c.governorId>=0)releaseGovernor(c.governorId);
        releaseGovernor(target.id);w.spend(c,o,0);c.governorId=target.id;target.acted=true;
        if(target.role!=Role.RULER&&target.role!=Role.DISTRICT)target.role=Role.GOVERNOR;
        return w.success(target.name+"出任"+c.name+"太守，金粮收入加成"+(target.politics/4)+"%");
    }
    public int governorPolitics(int cityId) {
        World.City c=w.city(cityId);World.Officer o=c==null?null:w.officer(c.governorId);
        return w.governance.resident(o,c)&&(o.role==Role.GOVERNOR||o.role==Role.RULER||o.role==Role.DISTRICT)?o.politics:-1;
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
        for(World.Officer o:w.officers)if(o.cityId==cityId&&o.owner!=c.owner&&!w.recruitment.returning(o)){o.otherTaskTurns=0;o.otherTask="";}
    }
    /** Deterministic migration/default leadership. Appoints existing residents only; never invents talent. */
    void initializeOffices() {
        for(int side=0;side<w.factions.length;side++) {
            World.Officer first=null;boolean hasRuler=false;
            for(World.Officer o:w.officers)if(o.owner==side){hasRuler|=o.role==Role.RULER;if(first==null||o.id<first.id)first=o;}
            if(!hasRuler&&first!=null){first.role=Role.RULER;first.loyalty=100;}
        }
        w.governance.reconcile(false);
    }
    public World.Result patrol(int cityId,int officerId) {w.reports.prepare();
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);RuleFailure failure=cityActionFailure(CityActionPlan.Operation.PATROL,cityId,officerId,new int[0]);
        if(failure!=null)return w.fail(failure.detail);int gain=patrolGain(c,o);
        OfficerExperiencePlan.award(w,o,0);w.spend(c,o,PATROL_COST,cityActionBaseCost(CityActionPlan.Operation.PATROL),OfficerExperiencePlan.merit(o,0,w.government));c.order+=gain;return w.success(c.name+"巡察，治安+"+gain);
    }
    public int recruitAmount(int cityId,int officerId) {
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);
        return c==null||o==null?0:Math.min(c.recruitReserve,SiegeRules.recruitment(w,c,
            StrategyRules.enlistment(w.domestic.recruitAmount(cityId),c.order,o.charm,1000000)*(w.skills.has(o,Skill.MINGSHENG)?150:100)/100));
    }
    public World.Result recruitSoldiers(int cityId, int officerId) {w.reports.prepare();
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);RuleFailure failure=cityActionFailure(CityActionPlan.Operation.RECRUIT,cityId,officerId,new int[0]);
        if(failure!=null)return w.fail(failure.detail);int amount=recruitAmount(cityId,officerId);
        OfficerExperiencePlan.award(w,o,4);this.w.spend(c,o,RECRUIT_COST,cityActionBaseCost(CityActionPlan.Operation.RECRUIT),OfficerExperiencePlan.merit(o,4,w.government));
        w.domestic.use(cityId,Domestic.Kind.BARRACKS);
        int moraleBefore=c.morale,orderBefore=c.order;
        c.morale=Conscription.moraleAfter(c,amount);
        c.recruitReserve -= amount;
        c.troops += amount;
        c.order -= recruitOrderLoss(c,o);
        return this.w.success(c.name+"征得"+amount+"兵，治安−"+(orderBefore-c.order)+"；气力 "+moraleBefore+"→"+c.morale+"（新兵尚未训练）；兵源剩余"+c.recruitReserve);
    }
    public int getArmyReadiness(int cityId) {
        World.City c=w.city(cityId);if(c==null)throw new IllegalArgumentException("城池不存在");return c.morale;
    }
    public World.Result trainArmy(int cityId,int officerId) {w.reports.prepare();
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);RuleFailure failure=cityActionFailure(CityActionPlan.Operation.TRAIN,cityId,officerId,new int[0]);
        if(failure!=null)return w.fail(failure.detail);int gain=trainingGain(c,o);
        OfficerExperiencePlan.award(w,o,1);w.spend(c,o,TRAIN_COST,cityActionBaseCost(CityActionPlan.Operation.TRAIN),OfficerExperiencePlan.merit(o,1,w.government));c.morale+=gain;return w.success(c.name+"训练，气力+"+gain);
    }
    void tick() {
        w.loyalty.tick();
        for(World.Officer o:w.officers) {
            if(o.otherTaskTurns>0&&--o.otherTaskTurns==0){o.otherTask="";o.acted=true;w.note(o.name+"完成战略任务");}
            World.City c=w.city(o.cityId);
            if(!w.relations.loyalBond(o.id)&&!w.skills.city(o.cityId,Skill.RENZHENG)&&w.turn%3==0&&o.owner>=0&&o.role!=Role.RULER&&c!=null&&c.owner==o.owner&&(c.order<40||c.gold<200))
                w.loyalty.lose(o,w.campaign.loyaltyLoss(o.owner,2));
        }
    }
    public StrategicAi.Decision planAi(int cityId) { return new StrategicAi(w).plan(cityId,false); }
    public int strategicPressure(int cityId) { return new StrategicAi(w).pressure(w.city(cityId)); }
    void runAi(boolean emergency) { new StrategicAi(w).run(emergency); }
    void write(DataOutputStream out)throws IOException { StrategySave.write(this,out); }
    void read(DataInputStream in)throws IOException { StrategySave.read(this,in); }
    void validate()throws IOException { StrategySave.validate(this); }
boolean recruitable(int owner,int targetId){
        World.Officer t=w.officer(targetId);if(t==null||!w.life.present(t.id)||t.owner==owner||w.government.captive(t.id)||busy(t.id)||t.role==Role.RULER||w.relations.loyalBond(t.id))return false;
        World.Unit field=w.loyalty.fieldUnit(t.id);
        if(field!=null)return t.owner>=0&&field.owner==t.owner;
        if(w.domestic.busy(t.id)||w.city(t.cityId)==null)return false;
        return t.owner<0?t.role==Role.UNAFFILIATED:w.city(t.cityId).owner==t.owner;
    }
int recruitChance(int owner,int officerId,int targetId){
        World.Officer o=w.officer(officerId),target=w.officer(targetId);
        if(o==null||o.owner!=owner||target==null||target.owner>=0&&target.loyalty+target.honor>96)return 0;
        if(!recruitable(owner,targetId)||w.relations.refuses(targetId,officerId,owner)||w.loyalty.refusesRuler(target))return 0;
        int base=StrategyRules.recruitmentChance(o.charm,o.politics,target.loyalty,target.owner<0,target.owner<0?0:factionRelation(owner,target.owner));
        return Math.max(0,Math.min(95,base+w.relations.recruitmentBonus(targetId,officerId,owner)+w.loyalty.recruitmentAdjustment(o,target,owner)));
    }
void join(World.Officer actor,World.Officer target,int city){w.loyalty.join(actor,target,city);}
}

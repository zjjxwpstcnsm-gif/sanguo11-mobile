package game.sanguo.core;

import java.util.*;
import java.util.function.Consumer;

/** Engineering rules, NOT original SAN11 formulas. All commands validate before mutation. */
public final class World {
    public enum Sex { UNKNOWN, MALE, FEMALE }
    public enum Terrain { PLAIN, FOREST, MOUNTAIN, WATER, MOUNTAIN_PATH, SHALLOWS, PLANK_ROAD, POISON, SEA, VOID, SWAMP, DAM }
    public enum SiteKind { CITY, GATE, PORT }
    public enum Weapon {
        SPEAR("枪兵",4,1,115), HALBERD("戟兵",3,1,105), CROSSBOW("弩兵",3,2,95), CAVALRY("骑兵",6,1,120),
        SWORD("剑兵",4,1,80), RAM("冲车",2,1,40), SIEGE_TOWER("井阑",2,2,100),
        WOODEN_BEAST("木兽",2,1,90), CATAPULT("投石",2,3,90);
        public final String label;
        public final int movement, range, power;
        Weapon(String label,int movement,int range,int power) { this.label=label;this.movement=movement;this.range=range;this.power=power; }
    }
    public static final class City {
        public final int id;
        public final String name;
        public final Hex hex;
        public int owner, gold=5000, food=40000, troops=12000, order=90, morale=70, defense=3000;
        public SiteKind kind=SiteKind.CITY;
        public int baseDefense=3000;
        public int recruitReserve=20000, governorId=-1;
        public final int[] equipment={12000,12000,12000,12000,0,0,0,0,0};
        public final int[] ships={0,0};
        public City(int id,String name,Hex hex,int owner) { this.id=id;this.name=name;this.hex=hex;this.owner=owner; }
    }
    public static final class Officer {
        public final int id;
        public final String name;
        public int owner, cityId, unitId=-1, leadership, war, intelligence, politics, charm;
        public boolean acted;
        public String skillId="none";
        public Sex sex=Sex.UNKNOWN;
        public final int[] aptitude={1,1,1,1,1,1};
        public int loyalty=85, otherTaskTurns=0, lastRewardTurn=-1;
        public Strategy.Role role=Strategy.Role.OFFICER;
        public String otherTask="";
        public Officer(int id,String name,int owner,int city,int l,int w,int i,int p,int c) {
            this.id=id;this.name=name;this.owner=owner;this.cityId=city;
            leadership=l;war=w;intelligence=i;politics=p;charm=c;
            if(owner<0){loyalty=0;role=Strategy.Role.UNAFFILIATED;}
        }
    }
    public static class Unit {
        public final int id;
        public int owner,officerId;
        public final Weapon weapon;
        public Hex hex;
        public int troops, food, gold, energy=80;
        public boolean acted;
        public MarchOrders.Order march;
        public int movementBudget=-1, movementSpent;
        public War.Status status=War.Status.NORMAL;
        public int statusTurns, burning;
        public int burningOwner=-1, burningPower=1;
        public int[] deputies=new int[0];
        public Army.Ship ship=Army.Ship.BOAT;
        public Unit(int id,int owner,int officerId,Weapon weapon,Hex hex,int troops,int food) {
            this.id=id;this.owner=owner;this.officerId=officerId;this.weapon=weapon;this.hex=hex;this.troops=troops;this.food=food;
        }
    }
    public enum Feedback { NONE, ATTACK, DEFEAT }
    public static final class Result {
        public final boolean ok;
        public final String message;
        public final Feedback feedback;
        public final Hex impact;
        private Result(boolean ok,String message,Feedback feedback,Hex impact) { this.ok=ok;this.message=message;this.feedback=feedback;this.impact=impact; }
    }
    public final int width,height;
    public final Terrain[][] terrain;
    /** Index follows every List mutation, including iterators/subLists; values remain authoritative. */
    private static final class OfficerRoster extends AbstractList<Officer> implements RandomAccess {
        private final List<Officer> values=new ArrayList<>();
        private final Map<Integer,Officer> ids=new HashMap<>();
        private boolean dirty=true;
        @Override public Officer get(int i){return values.get(i);}
        @Override public int size(){return values.size();}
        @Override public void add(int i,Officer o){values.add(i,o);dirty=true;modCount++;}
        @Override public Officer set(int i,Officer o){Officer old=values.set(i,o);dirty=true;return old;}
        @Override public Officer remove(int i){Officer old=values.remove(i);dirty=true;modCount++;return old;}
        Officer byId(int id){if(dirty){ids.clear();for(Officer o:values)ids.putIfAbsent(o.id,o);dirty=false;}return ids.get(id);}
    }
    private static final class CityRoster extends AbstractList<City> implements RandomAccess {
        private final List<City> values=new ArrayList<>();
        private final Map<Integer,City> ids=new HashMap<>();
        private final Map<Hex,City> positions=new HashMap<>();
        private final Map<Hex,List<City>> neighbors=new HashMap<>();
        private boolean dirty=true;
        public City get(int i){return values.get(i);}
        public int size(){return values.size();}
        public void add(int i,City c){values.add(i,c);dirty=true;modCount++;}
        public City set(int i,City c){City old=values.set(i,c);dirty=true;return old;}
        public City remove(int i){City old=values.remove(i);dirty=true;modCount++;return old;}
        private void index(){if(!dirty)return;ids.clear();positions.clear();neighbors.clear();
            for(City c:values){ids.putIfAbsent(c.id,c);positions.putIfAbsent(c.hex,c);for(Hex h:c.hex.neighbors())neighbors.computeIfAbsent(h,k->new ArrayList<>()).add(c);}dirty=false;}
        City byId(int id){index();return ids.get(id);}
        City at(Hex h){index();return positions.get(h);}
        List<City> near(Hex h){index();return neighbors.getOrDefault(h,Collections.emptyList());}
    }
    public final List<City> cities=new CityRoster();
    public final List<Officer> officers=new OfficerRoster();
    public final List<Unit> units=new ArrayList<>();
    public final List<String> log=new ArrayList<>();
    public final Lifecycle life=new Lifecycle(this);
    public final Domestic domestic=new Domestic(this);
    public final Development development=new Development(this);
    public final Strategy strategy=new Strategy(this);
    public final PersonnelTravel personnel=new PersonnelTravel(this);
    public final Recruitment recruitment=new Recruitment(this);
    public final Envoys envoys=new Envoys(this);
    public int terrainRevision;
    public final Campaign campaign=new Campaign(this);
    public final Diplomacy diplomacy=new Diplomacy(this);
    public final War war=new War(this);
    public final CityDefense cityDefense=new CityDefense(this);
    public final Army army=new Army(this);
    public final Fieldworks fieldworks=new Fieldworks(this);
    public final UnitOrders orders=new UnitOrders(this);
    public final MarchOrders marches=new MarchOrders(this);
    public final Skills skills=new Skills(this);
    public final CombatRules combat=new CombatRules(this);
    public final CombatEffects combatEffects=new CombatEffects(this);
    public final EnergyRules energy=new EnergyRules(this);
    public final AdvancedBattle advancedBattle=new AdvancedBattle(this);
    public final WorldEvents events=new WorldEvents(this);
    public final Districts districts=new Districts(this);
    public final AiOrders aiOrders=new AiOrders(this);
    public final Government government=new Government(this);
    public final Supply supply=new Supply(this);
    public final Contests contests=new Contests(this);
    public final Relations relations=new Relations(this);
    public final Treasures treasures=new Treasures(this);
    public final Editor editor=new Editor(this);
    public final AbilityResearch abilities;
    public final String[] factions;
    public final int[] actionPoints;
    public String scenarioId="m0-skirmish", scenarioName="基础演练", dataSource="engineering-original", dataHash="";
    public int sourceMapWidth; // zero: axial; positive: original odd-r width
    public int dataRevision=1, startYear=190, startMonth=1, player=0;
    public int turn=0, active=0, nextUnitId=1, winner=-1;
    public World(int width,int height) {
        this(width,height,"刘备军","曹操军");
    }
    public World(int width,int height,String... factions) {
        if(width<1||width>300||height<1||height>200||factions.length<2||factions.length>32)throw new IllegalArgumentException("地图或势力数量无效");
        this.factions=factions.clone();abilities=new AbilityResearch(this);actionPoints=new int[factions.length];Arrays.fill(actionPoints,60);
        this.width=width;this.height=height;terrain=new Terrain[width][height];
        for (Terrain[] row:terrain) Arrays.fill(row,Terrain.PLAIN);
    }
    public String date() { int month=startMonth-1+turn/3;return (startYear+month/12)+"年 "+(month%12+1)+"月 "+new String[]{"上旬","中旬","下旬"}[turn%3]; }
    public String faction(int owner) { return owner>=0&&owner<factions.length?factions[owner]:"空城"; }
    public boolean alive(int owner) {
        if (owner < 0 || owner >= this.factions.length) {
            return false;
        }
        for (City c : this.cities) {
            if (c.owner == owner && c.kind == SiteKind.CITY) {
                return true;
            }
        }
        return false;
    }
    public boolean commandsBlocked(){return contests.busy()||life.pending();}
    public boolean gameOver() { return winner>=0||!alive(player); }
    public City home() { for(City c:cities)if(c.owner==player)return c;return cities.isEmpty()?null:cities.get(0); }
    public boolean inside(Hex h) { return h!=null&&h.q>=0&&h.r>=0&&h.q<width&&h.r<height&&terrain[h.q][h.r]!=Terrain.VOID; }
    public City city(int id){return ((CityRoster)cities).byId(id);}
    public Officer officer(int id) { return ((OfficerRoster)officers).byId(id); }
    public Unit unit(int id) { for(Unit u:units) if(u.id==id) return u;
        if(id>=10000000){Domestic.Mission m=domestic.mission(id);if(m!=null&&m.transport)return m;}return null; }
    /** Read-only union; mission and battlefield refer to the same cargo object. */
    public List<Unit> fieldUnits(){List<Unit> all=new ArrayList<>(units);for(Domestic.Mission m:domestic.missions)if(m.transport&&!m.legacyOverlap&&cityAt(m.hex)==null)all.add(m);return all;}
    public City cityAt(Hex h){return ((CityRoster)cities).at(h);}
    public Unit unitAt(Hex h) {for(Unit u:units)if(u.hex.equals(h))return u;for(Domestic.Mission m:domestic.missions)if(m.transport&&!m.legacyOverlap&&m.hex.equals(h)&&cityAt(h)==null)return m;return null;}
    // Transient command feedback, never serialized or inferred by parsing translated log text.
    private Feedback feedback=Feedback.NONE;
    private Hex impact;
    private final List<String> battleOutcomes=new ArrayList<>();
    void battleImpact(Hex hex,boolean defeated){
        if(defeated||feedback==Feedback.NONE){feedback=defeated?Feedback.DEFEAT:Feedback.ATTACK;impact=hex;}
    }
    void battleOutcome(String text){battleOutcomes.add(text);note(text);}
    private Result result(boolean ok,String text){
        String message=text+(ok&&!battleOutcomes.isEmpty()?"\n"+String.join("\n",battleOutcomes):"");
        Result result=new Result(ok,message,ok?feedback:Feedback.NONE,ok?impact:null);
        feedback=Feedback.NONE;impact=null;battleOutcomes.clear();return result;
    }
    Result fail(String text) { return result(false,text); }
    private long commandRevision;
    /** Transient successful-command generation; identity plus generation guards open UI confirmations. */
    public long commandRevision(){return commandRevision;}
    Result success(String text) {commandRevision++; fieldworks.cleanup();abilities.cleanup();districts.cleanup();diplomacy.cleanup();aiOrders.cleanup();note(text);return result(true,text); }
    public void note(String text) { log.add(text);while(log.size()>40)log.remove(0); }
    private boolean available(Officer o,City c) { return !commandsBlocked()&&o!=null&&o.owner==active&&o.cityId==c.id&&o.unitId<0&&!o.acted&&!domestic.busy(o.id)&&!strategy.busy(o.id)&&!government.captive(o.id); }
    public int cityFoodUse(City c){return c.kind!=SiteKind.CITY&&skills.city(c.id,Skill.TUNTIAN)?0:(c.troops+49)/50;}
    public List<Officer> idle(City c) {
        List<Officer> found=new ArrayList<>();
        if(c!=null&&c.owner==active) for(Officer o:officers) if(available(o,c))found.add(o);
        return found;
    }
    String cityError(City c,Officer o,int gold) {
        if(commandsBlocked())return "请先完成当前对局或君主继承";
        if(gameOver())return "本局已结束";
        if(c==null||c.owner!=active)return "请选择己方城池";
        if(envoys.resolving(o==null?-1:o.id))return o!=null&&o.owner==c.owner&&o.unitId<0&&!government.captive(o.id)?null:"使者状态变化";
        if(!districts.directCity(c.id))return "该据点由委任军团管理，请先重编或撤销军团";
        if(!available(o,c))return "需要一名本旬尚未行动的在城武将";
        if(actionPoints[active]<10)return "行动力不足10";
        if(c.gold<gold)return "金不足";
        return null;
    }
    void spend(City c,Officer o,int gold) { if(envoys.resolving(o.id))return; c.gold-=gold;actionPoints[active]-=10;o.acted=true;government.earn(o.id,100); }
    /** Compatibility entry points: UI, AI and callers share the strategy rules. */
    public Result recruit(int cityId,int officerId) { return strategy.recruitSoldiers(cityId,officerId); }
    public Result train(int cityId,int officerId) { return strategy.trainArmy(cityId,officerId); }
    public Result patrol(int cityId,int officerId) { return strategy.patrol(cityId,officerId); }
    public int getArmyReadiness(int cityId) { return strategy.getArmyReadiness(cityId); }
    public Result produce(int cityId,int officerId,Weapon weapon) {
        City c=city(cityId);Officer o=officer(officerId);int gold=skills.productionGold(officerId,weapon);String error=cityError(c,o,gold);
        if(error!=null)return fail(error);
        if(c.kind!=SiteKind.CITY)return fail("港口和关卡不能生产军备");
        if(districts.productionError(cityId)!=null)return fail(districts.productionError(cityId));
        if(weapon==null||weapon==Weapon.SWORD)return fail("剑兵无需生产兵装，请选择其他兵装");
        if(Army.siegeWeapon(weapon))return army.produce(cityId,officerId,weapon,null);
        int amount=skills.produceAmount(c.id,o.id,weapon);
        if(c.equipment[weapon.ordinal()]>campaign.equipmentCap(c,weapon)-amount)return fail("兵装已接近上限");
        spend(c,o,gold);c.equipment[weapon.ordinal()]+=amount;return success(c.name+"生产"+amount+"份"+weapon.label+"兵装，金−"+gold);
    }
    public Result deploy(int cityId,int officerId,Weapon weapon,int troops) {
        return army.deploy(cityId,officerId,new int[0],weapon,Army.Ship.BOAT,troops,troops*2);
    }
    public int cost(Hex h,Weapon weapon) {
        if(h==null||weapon==null||!inside(h)||events.at(h)!=null)return -1;
        Terrain t=terrain[h.q][h.r];
        if(t==Terrain.MOUNTAIN||t==Terrain.WATER||t==Terrain.SEA||t==Terrain.VOID)return -1;
        if(t==Terrain.SWAMP)return weapon==Weapon.CAVALRY||Army.siegeWeapon(weapon)?4:2;
        return t==Terrain.DAM||t==Terrain.POISON?2:t==Terrain.FOREST?(weapon==Weapon.CAVALRY||Army.siegeWeapon(weapon)?3:2):1;
    }
    private static final class Step {
        final Hex hex;final int cost;
        Step(Hex h,int c){hex=h;cost=c;}
    }
    /** Dijkstra: excludes city/unit occupancy and includes the starting tile at cost zero. */
    public Map<Hex,Integer> reachable(Unit u) {
        return orders.reachable(u);
    }
    private String unitError(Unit u) {return orders.error(u);}
    public Result move(int unitId,Hex destination) {return orders.execute(orders.previewMove(unitId,destination));}
    public Result attack(int attackerId,int targetId) {
        return war.attack(attackerId,targetId);
    }
    public String siegeError(int unitId,int cityId) {
        Unit u=unit(unitId);City c=city(cityId);String error=unitError(u);if(error!=null)return error;
        if(u instanceof Domestic.Mission)return "运输队不能攻城";
        if(c==null||!campaign.hostile(u.owner,c.owner))return "请选择交战势力或未占领城池";
        if(u.hex.distance(c.hex)>army.siegeRange(u))return "城池不在攻城射程内";
        if(Army.siegeWeapon(u.weapon)&&!army.water(u.hex))return "兵器使用战法攻城";
        return null;
    }
    public Result siege(int unitId,int cityId) {
        String error=siegeError(unitId,cityId);if(error!=null)return fail(error);
        Unit u=unit(unitId);City c=city(cityId);
        u.acted=true;return resolveSiege(u,c,false);
    }
    /** Caller has validated and paid for the command. No nested public command or second payment. */
    Result resolveSiege(Unit u,City c,boolean tactic) {return resolveSiege(u,c,tactic,false);}
    Result resolveSiege(Unit u,City c,boolean tactic,boolean stoneSplash) {
        CombatRules.SiegeDamage damage=combat.siege(u,c,tactic);int hit=Math.min(c.defense,damage.wall),troopHit=Math.min(c.troops,damage.troops);
        c.defense=Math.max(0,c.defense-hit);c.troops=Math.max(0,c.troops-troopHit);
        battleImpact(c.hex,c.defense==0||c.troops==0);
        String message=officer(u.officerId).name+"攻城，城防−"+hit+"，守军−"+troopHit;
        if(c.defense==0||c.troops==0) {
            int old=c.owner;c.owner=u.owner;domestic.captured(c.id);strategy.cityCaptured(c.id);c.defense=Math.max(1,campaign.defenseCap(c)/4);c.troops=0;c.morale=50;c.order=60;
            government.cityCaptured(c,old,u);treasures.fallenTreasury(old,u.owner);districts.captured(c,u);
            campaign.cleanupProjects();army.cleanup();campaign.earn(u.owner,100);
            message=c.name+"被"+faction(u.owner)+"攻占";
        }
        else {int counter=cityDefense.counter(c,u);if(counter>0)message+="，据点反击−"+counter;}
        if(stoneSplash)fieldworks.stoneSplash(u,c.hex);
        checkVictory();return success(message);
    }
    public Result enter(int unitId, int cityId) {
        Unit u = unit(unitId);
        City c = city(cityId);
        String error = unitError(u);
        if (error != null) {
            return fail(error);
        }
        if (u instanceof Domestic.Mission) {
            return this.domestic.unload((Domestic.Mission) u, cityId);
        }
        if (c == null || c.owner != u.owner || u.hex.distance(c.hex) > 1) {
            return fail("请选择相邻己方城市、关卡或港口");
        }
        int gear = Army.equipmentNeeded(u.weapon, u.troops);
        int cap = this.campaign.equipmentCap(c, u.weapon);
        if (c.troops + u.troops > this.campaign.troopCap(c) || c.equipment[u.weapon.ordinal()] + gear > cap || c.food + u.food > this.campaign.foodCap(c) || c.gold + u.gold > this.campaign.goldCap(c) || (u.ship != Army.Ship.BOAT && c.ships[u.ship.ordinal() - 1] >= 100)) {
            return fail("据点容量不足：兵余" + Math.max(0, this.campaign.troopCap(c) - c.troops) + "、粮余" + Math.max(0, this.campaign.foodCap(c) - c.food) + "、金余" + Math.max(0, this.campaign.goldCap(c) - c.gold) + "、所选兵装余" + Math.max(0, cap - c.equipment[u.weapon.ordinal()]));
        }
        c.troops += u.troops;
        c.food += u.food;
        c.gold += u.gold;
        int[] iArr = c.equipment;
        int iOrdinal = u.weapon.ordinal();
        iArr[iOrdinal] = iArr[iOrdinal] + gear;
        if (u.ship != Army.Ship.BOAT) {
            int[] iArr2 = c.ships;
            int iOrdinal2 = u.ship.ordinal() - 1;
            iArr2[iOrdinal2] = iArr2[iOrdinal2] + 1;
        }
        int prisoners = this.government.entered(u, c);
        Officer o = officer(u.officerId);
        for (Officer member : this.army.crew(u)) {
            member.unitId = -1;
            member.cityId = c.id;
            member.acted = true;
        }
        this.units.remove(u);
        return success(o.name + "入城休整" + (prisoners > 0 ? "；随军俘虏" + prisoners + "人已关押于" + c.name : ""));
    }
    void retreat(Officer o,Hex from) {
        strategy.releaseGovernor(o.id);o.otherTaskTurns=0;o.otherTask="";
        City destination=null;
        for(City c:cities)if(c.owner==o.owner&&(destination==null||from.distance(c.hex)<from.distance(destination.hex)))destination=c;
        o.unitId=-1;o.cityId=destination==null?-1:destination.id;o.acted=true;
    }
    void removeUnit(Unit u) { government.defeated(u,null); }
    void defeatUnit(Unit u,Unit attacker){government.defeated(u,attacker);}
    public Result nextTurn(){return nextTurn(p->{});}
    public static final class TurnProgress {
        public final int owner,completed,total; public final String phase;
        public TurnProgress(int owner,int completed,int total,String phase){this.owner=owner;this.completed=completed;this.total=total;this.phase=phase;}
    }
    public Result nextTurn(Consumer<TurnProgress> progress){
        Objects.requireNonNull(progress);
        if(commandsBlocked())return fail("请先完成当前对局或君主继承");
        if(gameOver())return fail("本局已结束，请重开");
        if(active!=player)return fail("等待电脑行动");
        List<Integer> sides=new ArrayList<>();for(int offset=1;offset<factions.length;offset++){int side=(player+offset)%factions.length;if(alive(side))sides.add(side);}
        int total=sides.size()+3;progress.accept(new TurnProgress(player,0,total,"委任军团与太守"));
        districts.run();government.runDelegated();int completed=1;
        for(int side:sides){active=side;final int done=completed;
            if(alive(side)){progress.accept(new TurnProgress(side,done,total,"准备行动"));reset(side);runAi(phase->progress.accept(new TurnProgress(side,done,total,phase)));checkVictory();
                if(gameOver()){active=player;progress.accept(new TurnProgress(player,total,total,"战局结束"));return success(winner==player?"战场胜利":"我方势力已覆灭");}}
            completed++;
        }
        final int global=completed;settleGlobalTurn(phase->progress.accept(new TurnProgress(-1,global,total,phase)));
        active=player;progress.accept(new TurnProgress(player,completed+1,total,"恢复行动与自动行军"));reset(player);checkVictory();if(!commandsBlocked())marches.advanceAll();
        progress.accept(new TurnProgress(player,total,total,"结算完成"));return success(date()+" · 行动力恢复");
    }
    /** Exactly once after all factions have acted. Keep this order stable across save replay. */
    private void settleGlobalTurn(Consumer<String> progress){
        progress.accept("运输、建设与生产");
        turn++;contests.tick();domestic.tick();campaign.tick();army.tick();abilities.tick();recruitment.tick();envoys.tick();strategy.tick();
        progress.accept("火场、守备与武将");war.tick();cityDefense.tick();government.tick();treasures.tick();
        progress.accept("兵粮消耗与城池收入");
        for(Unit u:new ArrayList<>(units)) {
            int consumption=fieldworks.foodUse(u,Math.max(1,(u.troops+19)/20));
            if(u.food<consumption){u.food=0;u.troops-=Math.max(1,u.troops/10);note(officer(u.officerId).name+"部队断粮，兵力减少");}
            else u.food-=consumption;
            if(u.troops<=0)removeUnit(u);
        }
        for(City c:cities) if(c.owner>=0) {
            int consumption=cityFoodUse(c)-domestic.arrivalFoodCredit(c);
            if(c.food<consumption){c.food=0;c.troops=Math.max(0,c.troops-Math.max(1,c.troops/20));}
            else c.food-=consumption;
            c.gold+=Math.min(Math.max(0,campaign.goldCap(c)-c.gold),domestic.goldIncome(c.id,turn));c.food+=Math.min(Math.max(0,campaign.foodCap(c)-c.food),domestic.foodIncome(c.id,turn));
            c.defense+=cityDefense.recovery(c);
        }
        progress.accept("事件、寿命与外交");events.tick();life.tick();diplomacy.tick();
    }
    private void reset(int owner) {
        this.actionPoints[owner] = 60;
        this.districts.reset(owner);
        for (Officer o : this.officers) {
            if (o.owner == owner) {
                o.acted = this.contests.busy() && !this.contests.current().isDuel() && (this.contests.current().leftRef == o.id || this.contests.current().rightRef == o.id);
            }
        }
        for (Unit u : this.units) {
            if (u.owner == owner) {
                this.orders.reset(u);
            }
        }
        this.war.resetOwner(owner);
        this.fieldworks.continueOwner(owner);
    }
    private void runAi(Consumer<String> progress) {
        progress.accept("外交与内政");
        CampaignAi ai=new CampaignAi(this);
        diplomacy.dispatch();
        government.runAi();
        strategy.runAi(true);
        // Hot path: each AI faction only evaluates and sorts its own bases. With the
        // full historical faction set, sorting the national city list per faction made
        // end-of-turn time grow much faster than the actual amount of AI work.
        List<City> ordered=new ArrayList<>();
        for(City c:cities)if(c.owner==active)ordered.add(c);
        ordered.sort(Comparator.comparingInt((City c)->-ai.incoming(c)).thenComparingInt(c->c.id));
        progress.accept("补给与支援");for(City c:ordered)ai.replenish(c.id);
        for(City c:ordered)ai.support(c.id);
        for(City c:ordered){progress.accept("出征 · "+c.name);ai.deploy(c.id,6000);}
        progress.accept("技巧与人才");
        campaign.runAi();
        abilities.runAi();
        strategy.runAi(false);
        for(City c:ordered)ai.prepare(c.id);
        progress.accept("部队行军与交战");ai.runUnits();
        progress.accept("内政建设");domestic.runAi();
    }
    /** Plan beyond one turn so AI can detour around rivers and mountains. */
    boolean advance(Unit u,Hex target,int range) {
        Map<Hex,Integer> distances=new HashMap<>();Map<Hex,Hex> previous=new HashMap<>();
        Set<Hex> blocked=new HashSet<>();for(City c:cities)blocked.add(c.hex);for(Domestic.Facility f:domestic.facilities)blocked.add(f.hex);for(Unit b:fieldUnits())if(b.id!=u.id)blocked.add(b.hex);
        for(War.Structure s:war.structures())blocked.add(s.hex);
        PriorityQueue<Step> todo=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.hex.q).thenComparingInt(s->s.hex.r));
        distances.put(u.hex,0);todo.add(new Step(u.hex,0));
        while(!todo.isEmpty()) {
            Step step=todo.remove();if(step.cost!=distances.get(step.hex))continue;
            if(step.hex.distance(target)<=range) {
                Hex destination=step.hex;
                Map<Hex,Integer> reachable=orders.reachable(u);
                while(!reachable.containsKey(destination))destination=previous.get(destination);
                return !destination.equals(u.hex)&&move(u.id,destination).ok;
            }
            for(Hex next:step.hex.neighbors()) {
                int cost=army.moveCost(u,step.hex,next);if(cost<0||blocked.contains(next))continue;
                int total=step.cost+cost;
                if(total<distances.getOrDefault(next,Integer.MAX_VALUE)) {
                    distances.put(next,total);previous.put(next,step.hex);todo.add(new Step(next,total));
                }
            }
        }
        return false;
    }
    public void checkVictory() {
        for (int side = 0; side < this.factions.length; side++) {
            if (!alive(side)) {
                FactionCollapse.resolve(this, side);
            }
        }
        int count = 0;
        int last = -1;
        for (int side2 = 0; side2 < this.factions.length; side2++) {
            if (alive(side2)) {
                count++;
                last = side2;
            }
        }
        this.winner = count == 1 ? last : -1;
        this.domestic.cleanupDefeated();
        this.abilities.cleanup();
    }
    boolean gateBlocks(int owner,Hex from,Hex to){
        if(from==null||to==null)return false;
        for(City c:((CityRoster)cities).near(from))if(c.kind==SiteKind.GATE&&c.hex.distance(to)==1&&campaign.hostile(owner,c.owner))return true;
        return false;
    }
}

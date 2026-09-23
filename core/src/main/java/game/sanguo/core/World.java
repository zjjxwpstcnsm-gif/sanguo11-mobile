package game.sanguo.core;

import java.util.*;
import java.util.function.Consumer;

/** Engineering rules, NOT original SAN11 formulas. All commands validate before mutation. */
public final class World {
    public enum Sex { UNKNOWN, MALE, FEMALE }
    public enum Terrain { PLAIN, FOREST, MOUNTAIN, WATER, MOUNTAIN_PATH, SHALLOWS, PLANK_ROAD, POISON, SEA, VOID, SWAMP, DAM, SAND, ROAD, NON_NAVIGABLE_WATER }
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
        transient SiteKind footprintKind;
        transient List<Hex> footprint=Collections.emptyList();
        public int baseDefense=3000;
        public int recruitReserve=Conscription.RESERVE_CAP, governorId=-1;
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
        /** -1 denotes an unknown/custom or pre-v28 affinity; honor uses source levels 1–5. */
        public int affinity=-1, honor=3;
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
        /** Wounded do not fight; fractional hundredths persist to avoid rounding by hit size. */
        public int wounded, woundRemainder;
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
        /** Rejection produced by a session guard before any rule executes. */
        public static Result rejected(String message){return new Result(false,message,Feedback.NONE,null,null);}
        public final boolean ok;
        public final String message;
        public final Feedback feedback;
        public final Hex impact;
        public final CriticalHit critical;
        private Result(boolean ok,String message,Feedback feedback,Hex impact,CriticalHit critical) { this.ok=ok;this.message=message;this.feedback=feedback;this.impact=impact;this.critical=critical; }
    }
    public final int width,height;
    public final Terrain[][] terrain;
    /** Index follows every List mutation, including iterators/subLists; values remain authoritative. */
    private static final class OfficerRoster extends AbstractList<Officer> implements RandomAccess {
        private final List<Officer> values=new ArrayList<>();
        private final Map<Integer,Officer> ids=new HashMap<>();
        private final Officer[] smallIds=new Officer[8192];
        private boolean dirty=true;
        @Override public Officer get(int i){return values.get(i);}
        @Override public int size(){return values.size();}
        @Override public void add(int i,Officer o){values.add(i,o);dirty=true;modCount++;}
        @Override public Officer set(int i,Officer o){Officer old=values.set(i,o);dirty=true;return old;}
        @Override public Officer remove(int i){Officer old=values.remove(i);dirty=true;modCount++;return old;}
        Officer byId(int id){if(dirty){ids.clear();Arrays.fill(smallIds,null);for(Officer o:values){if(o.id>=0&&o.id<smallIds.length){if(smallIds[o.id]==null)smallIds[o.id]=o;}else ids.putIfAbsent(o.id,o);}dirty=false;}return id>=0&&id<smallIds.length?smallIds[id]:ids.get(id);}
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
            for(City c:values){ids.putIfAbsent(c.id,c);for(Hex cell:SiteFootprint.cells(c))positions.putIfAbsent(cell,c);for(Hex h:SiteFootprint.edge(c))neighbors.computeIfAbsent(h,k->new ArrayList<>()).add(c);}dirty=false;}
        City byId(int id){index();return ids.get(id);}
        City at(Hex h){index();return positions.get(h);}
        List<City> near(Hex h){index();return neighbors.getOrDefault(h,Collections.emptyList());}
    }
    public final List<City> cities=new CityRoster();
    public final List<Officer> officers=new OfficerRoster();
    public final List<Unit> units=new ArrayList<>();
    public final List<String> log=new ArrayList<>();
    public final SaveExtensions extensions=new SaveExtensions();
    public final BattleReports reports=new BattleReports(this);
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
    public final Loyalty loyalty=new Loyalty(this);
    public final CombatRules combat=new CombatRules(this);
    public final CombatEffects combatEffects=new CombatEffects(this);
    public final EnergyRules energy=new EnergyRules(this);
    public final AdvancedBattle advancedBattle=new AdvancedBattle(this);
    public final WorldEvents events=new WorldEvents(this);
    public final Districts districts=new Districts(this);
    public final AiOrders aiOrders=new AiOrders(this);
    public final Government government=new Government(this);
    public final Governance governance=new Governance(this);
    public final Supply supply=new Supply(this);
    public final Contests contests=new Contests(this);
    public final Relations relations=new Relations(this);
    public final Treasures treasures=new Treasures(this);
    public final Editor editor=new Editor(this);
    public final AbilityResearch abilities;
    public final String[] factions;
    public final int[] actionPoints;
    public String scenarioId="m0-skirmish", scenarioName="基础演练", dataSource="engineering-original", dataHash="";
    public int sourceMapWidth, sourceMapHeight, sourceOriginX, sourceOriginY;
    public boolean columnStaggered;
    public String mapId="custom", mapLayout="axial";
    public int mapRevision;
    public String customMapId="",customMapName="",customMapBase="",customMapFingerprint="";
    public int customMapRevision;
    /** Optional presentation only; never consumed by strategic rules or SaveCodec. */
    public transient MapPatch visualMap;
    public final SortedMap<Integer,Integer> siteParents=new TreeMap<>();
    public int sourceColumns(){return sourceMapWidth>0?sourceMapWidth:width;}
    public int sourceRows(){return sourceMapHeight>0?sourceMapHeight:height;}
    public boolean sourceInside(Hex h){return h!=null&&h.q>=0&&h.r>=0&&h.q<width&&h.r<height&&(sourceMapWidth==0||MapCoordinates.source(this,h).isInside(sourceColumns(),sourceRows()));}
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
    public String faction(int owner) { return owner>=0&&owner<factions.length?(governance.nation(owner).isEmpty()?factions[owner]:governance.nation(owner)):"空城"; }
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
    public boolean inside(Hex h) { return sourceInside(h)&&terrain[h.q][h.r]!=Terrain.VOID; }
    public City city(int id){return ((CityRoster)cities).byId(id);}
    public Officer officer(int id) { return ((OfficerRoster)officers).byId(id); }
    public Unit unit(int id) { for(Unit u:units) if(u.id==id) return u;
        if(id>=10000000){Domestic.Mission m=domestic.mission(id);if(m!=null&&m.transport)return m;}return null; }
    /** Read-only union; mission and battlefield refer to the same cargo object. */
    public List<Unit> fieldUnits(){List<Unit> all=new ArrayList<>(units);for(Domestic.Mission m:domestic.missions)if(m.transport&&!m.legacyOverlap)all.add(m);return all;}
    public City cityAt(Hex h){return ((CityRoster)cities).at(h);}
    /** Call after loading site kinds; ownership changes do not invalidate geometric indexing. */
    public void invalidateSiteIndex(){((CityRoster)cities).dirty=true;}
    public Unit unitAt(Hex h) {for(Unit u:units)if(u.hex.equals(h))return u;for(Domestic.Mission m:domestic.missions)if(m.transport&&!m.legacyOverlap&&m.hex.equals(h))return m;return null;}
    // Transient command feedback, never serialized or inferred by parsing translated log text.
    TurnJournal turnJournal;
    void visualAction(TurnJournal.Kind kind,int actor,Hex target,String label){reports.action(kind,actor,target,label);actionLabel=label;if(turnJournal!=null)turnJournal.mark(kind,actor,target,label);}
    private String actionLabel="战法";
    private CriticalHit critical;
    void tacticCritical(Unit source){
        if(critical!=null)return;Officer o=officer(source.officerId);if(o==null)return;
        critical=new CriticalHit(o,source,actionLabel);
        if(turnJournal!=null)turnJournal.critical(critical);
    }
    private Feedback feedback=Feedback.NONE;
    private Hex impact;
    private final List<String> battleOutcomes=new ArrayList<>();
    void battleImpact(Hex hex,boolean defeated){
        if(defeated||feedback==Feedback.NONE){feedback=defeated?Feedback.DEFEAT:Feedback.ATTACK;impact=hex;}
    }
    void battleOutcome(String text){battleOutcomes.add(text);note(text);}
    private Result result(boolean ok,String text){
        String message=text+(ok&&!battleOutcomes.isEmpty()?"\n"+String.join("\n",battleOutcomes):"");
        Result result=new Result(ok,message,ok?feedback:Feedback.NONE,ok?impact:null,ok?critical:null);
        feedback=Feedback.NONE;impact=null;critical=null;actionLabel="战法";battleOutcomes.clear();return result;
    }
    Result fail(String text) { if(turnJournal!=null)turnJournal.cancel();reports.clearAction();return result(false,text); }
    private long commandRevision;
    /** Transient successful-command generation; identity plus generation guards open UI confirmations. */
    public long commandRevision(){return commandRevision;}
    Result success(String text) {commandRevision++;governance.reconcile(true); fieldworks.cleanup();abilities.cleanup();districts.cleanup();diplomacy.cleanup();aiOrders.cleanup();note(text);Result r=result(true,text);if(turnJournal!=null)turnJournal.checkpoint(r.message);reports.clearAction();return r; }
    public void note(String text) { reports.note(text);log.add(text);while(log.size()>40)log.remove(0); }
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
    public Result recruit(int cityId,int officerId) {reports.prepare(); return strategy.recruitSoldiers(cityId,officerId); }
    public Result train(int cityId,int officerId) {reports.prepare(); return strategy.trainArmy(cityId,officerId); }
    public Result patrol(int cityId,int officerId) {reports.prepare(); return strategy.patrol(cityId,officerId); }
    public int getArmyReadiness(int cityId) { return strategy.getArmyReadiness(cityId); }
    public Result produce(int cityId,int officerId,Weapon weapon) {reports.prepare();
        City c=city(cityId);Officer o=officer(officerId);int gold=skills.productionGold(officerId,weapon);String error=cityError(c,o,gold);
        if(error!=null)return fail(error);
        if(c.kind!=SiteKind.CITY)return fail("港口和关卡不能生产军备");
        if(districts.productionError(cityId)!=null)return fail(districts.productionError(cityId));
        if(weapon==null||weapon==Weapon.SWORD)return fail("剑兵无需生产兵装，请选择其他兵装");
        if(Army.siegeWeapon(weapon))return army.produce(cityId,officerId,weapon,null);
        Domestic.Kind facility=Domestic.productionFacility(weapon);
        error=domestic.operationError(cityId,facility);if(error!=null)return fail(error);
        int amount=skills.produceAmount(c.id,o.id,weapon);
        if(c.equipment[weapon.ordinal()]>campaign.equipmentCap(c,weapon)-amount)return fail("兵装已接近上限");
        spend(c,o,gold);domestic.use(cityId,facility);c.equipment[weapon.ordinal()]+=amount;return success(c.name+"生产"+amount+"份"+weapon.label+"兵装，金−"+gold);
    }
    public Result deploy(int cityId,int officerId,Weapon weapon,int troops) {reports.prepare();
        return army.deploy(cityId,officerId,new int[0],weapon,Army.Ship.BOAT,troops,troops*2);
    }
    public int cost(Hex h,Weapon weapon) {
        if(h==null||weapon==null||!inside(h)||events.at(h)!=null||NationalMap.restricted(this,h))return -1;
        Terrain t=terrain[h.q][h.r];
        if(t==Terrain.MOUNTAIN||t==Terrain.WATER||t==Terrain.SEA||t==Terrain.NON_NAVIGABLE_WATER||t==Terrain.VOID)return -1;
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
    public Result move(int unitId,Hex destination) {reports.prepare();return orders.execute(orders.previewMove(unitId,destination));}
    public Result attack(int attackerId,int targetId) {reports.prepare();
        return war.attack(attackerId,targetId);
    }
    public String siegeError(int unitId,int cityId) {
        Unit u=unit(unitId);City c=city(cityId);String error=unitError(u);if(error!=null)return error;
        return siegePositionError(u,c);
    }
    String siegePositionError(Unit u,City c){
        if(u instanceof Domestic.Mission)return "运输队不能攻城";
        if(c==null||!campaign.hostile(u.owner,c.owner))return "请选择交战势力或未占领城池";
        if(siegeHit(u,c)==null)return "城市没有处于合法攻城射程的占地格";
        if(Army.siegeWeapon(u.weapon)&&!army.water(u.hex))return "兵器使用战法攻城";
        return null;
    }
    public Hex siegeHit(Unit u,City c){return siegeHit(u,c,null);}
    public Hex siegeHit(Unit u,City c,Hex preferred){return u==null?null:SiteFootprint.hit(c,u.hex,1,army.siegeRange(u),preferred,h->inside(h)&&fieldworks.landTarget(u.owner,h));}
    public Displacement.Preview siegePreview(int unitId,int cityId,Hex preferred){
        Unit u=unit(unitId);City c=city(cityId);String error=siegeError(unitId,cityId);Hex hit=siegeHit(u,c,preferred);
        String text=error!=null?error:"攻击"+c.name+" · 命中格 "+hit+"（同一城市，仅结算一次）\n"+combat.siegePreview(u,c,false);
        return new Displacement.Preview(error,text,u==null?Collections.emptyList():Collections.singletonList(u.hex),
            hit==null?Collections.emptyList():Collections.singletonList(hit),hit==null?Collections.emptyList():Collections.singletonList(hit),null,false);
    }
    public Result siege(int unitId,int cityId){return siege(unitId,cityId,null);}
    public Result siege(int unitId,int cityId,Hex preferred) {reports.prepare();
        String error=siegeError(unitId,cityId);if(error!=null)return fail(error);
        Unit u=unit(unitId);City c=city(cityId);Hex hit=siegeHit(u,c,preferred);
        if(preferred!=null&&!preferred.equals(hit))return fail("预览命中格已失效，请重新选择");
        marches.supersede(u);u.acted=true;return resolveSiege(u,c,false,false,hit);
    }
    /** Caller has validated and paid for the command. No nested public command or second payment. */
    Result resolveSiege(Unit u,City c,boolean tactic) {return resolveSiege(u,c,tactic,false);}
    Result resolveSiege(Unit u,City c,boolean tactic,boolean stoneSplash) {return resolveSiege(u,c,tactic,stoneSplash,siegeHit(u,c));}
    Result resolveSiege(Unit u,City c,boolean tactic,boolean stoneSplash,Hex hitCell) {
        if(!SiteFootprint.contains(c,hitCell))return fail("攻城命中格已失效");
        visualAction(tactic?TurnJournal.Kind.TACTIC:TurnJournal.Kind.ATTACK,u.id,hitCell,tactic?"攻城战法":"攻城");
        CombatRules.SiegeDamage damage=combat.siege(u,c,tactic);int hit=Math.min(c.defense,damage.wall),troopHit=Math.min(c.troops,damage.troops);
        if(tactic&&(hit>0||troopHit>0)&&combat.critical(u,null,true))tacticCritical(u);
        c.defense=Math.max(0,c.defense-hit);c.troops=Math.max(0,c.troops-troopHit);
        battleImpact(hitCell,c.defense==0||c.troops==0);
        String message=officer(u.officerId).name+"攻城，城防−"+hit+"，守军−"+troopHit;
        if(c.defense==0||c.troops==0) {
            int old=c.owner;c.owner=u.owner;SiteFootprint.ownershipChanged(this,c);domestic.captured(c.id);strategy.cityCaptured(c.id);c.defense=Math.max(1,campaign.defenseCap(c)/4);c.troops=0;c.morale=50;c.order=60;
            government.cityCaptured(c,old,u);treasures.fallenTreasury(old,u.owner);districts.captured(c,u);
            campaign.cleanupProjects();army.cleanup();campaign.earn(u.owner,100);
            List<String> ruined=domestic.sack(c.id);
            message=c.name+"被"+faction(u.owner)+"攻占"+(ruined.isEmpty()?"":"；战乱损毁内政设施"+ruined.size()+"座（"+String.join("、",ruined)+"）");
        }
        else {int counter=cityDefense.counter(c,u);if(counter>0)message+="，据点反击−"+counter;}
        if(stoneSplash)fieldworks.stoneSplash(u,hitCell);
        if(c.owner==u.owner){String entered=autoEnter(u,c);if(!entered.isEmpty())message+="；"+entered;}
        checkVictory();return success(message);
    }
    public Result enter(int unitId,int cityId){reports.prepare();
        Unit u=unit(unitId);City c=city(cityId);String error=unitError(u);if(error!=null)return fail(error);
        Hex arrival=c==null?null:SiteFootprint.entry(this,u,u.hex,c);error=arrivalError(u,c,arrival);
        return error==null?success(dock(u,c,arrival)):fail(error);
    }
    /** Compatibility for persistent attack orders. Normal moves enter through autoEnter below. */
    Result enterAfterCapture(Unit u,City c){
        if(u==null||unit(u.id)!=u)return fail("攻城部队已失效");
        Hex point=captureEntry(u,c);String error=arrivalError(u,c,point);
        return error==null?success(dock(u,c,point)):fail(error);
    }
    private Hex captureEntry(Unit u,City c){
        if(c==null||c.owner!=u.owner||SiteFootprint.distance(c,u.hex)>1)return null;
        Hex normal=SiteFootprint.entry(this,u,u.hex,c);if(normal!=null)return normal;
        // Adjacent conquest is the only automatic one-cell exception. Still obey shore and occupancy rules.
        for(Hex h:SiteFootprint.cells(c))if(u.hex.distance(h)==1&&inside(h)&&(unitAt(h)==null||unitAt(h)==u)&&domestic.at(h)==null&&war.at(h)==null&&war.fireAt(h)==null&&army.entryCost(u,u.hex,h)>0)return h;
        return null;
    }
    private String arrivalError(Unit u,City c,Hex point){
        if(u==null||unit(u.id)!=u||u.troops<=0)return "部队已失效";
        if(c==null||c.owner!=u.owner||point==null)return "尚未抵达合法据点入口（上下河须经港口）";
        if(u instanceof Domestic.Mission)return domestic.arrivalError((Domestic.Mission)u,c);
        int gear=Army.equipmentNeeded(u.weapon,u.troops),cap=campaign.equipmentCap(c,u.weapon);
        if((long)c.troops+u.troops+u.wounded>campaign.troopCap(c)||c.equipment[u.weapon.ordinal()]+gear>cap||c.food+u.food>campaign.foodCap(c)||c.gold+u.gold>campaign.goldCap(c)||(u.ship!=Army.Ship.BOAT&&c.ships[u.ship.ordinal()-1]>=100))
            return "据点容量不足，部队与伤兵、兵装、钱粮全部保留；腾出容量后再次行军入城";
        return null;
    }
    /** Destination only: intermediate route cells remain traversable. No action-point payment. */
    String autoEnter(Unit u,City conquered){
        if(u==null||unit(u.id)!=u||u.troops<=0)return "";
        City c=conquered==null?cityAt(u.hex):conquered;
        if(c==null||c.owner!=u.owner||conquered==null&&!SiteFootprint.contains(c,u.hex)||conquered!=null&&SiteFootprint.distance(c,u.hex)>1)return "";
        if(conquered==null&&!marches.arrivalDestination(u,c))return "";
        Hex point=conquered==null?SiteFootprint.entry(this,u,u.hex,c):captureEntry(u,c);
        String error=arrivalError(u,c,point);return error==null?dock(u,c,point):"自动入城暂缓："+error;
    }
    private String dock(Unit u,City c,Hex point){
        visualAction(TurnJournal.Kind.ENTER,u.id,point,"自动入城");marches.supersede(u);
        if(u instanceof Domestic.Mission)return domestic.arrive((Domestic.Mission)u,c);
        int recovered=u.wounded,gear=Army.equipmentNeeded(u.weapon,u.troops);
        c.troops+=u.troops+recovered;c.food+=u.food;c.gold+=u.gold;c.equipment[u.weapon.ordinal()]+=gear;
        if(u.ship!=Army.Ship.BOAT)c.ships[u.ship.ordinal()-1]++;
        int prisoners=government.entered(u,c);Officer commander=officer(u.officerId);
        for(Officer member:army.crew(u)){member.unitId=-1;member.cityId=c.id;member.acted=true;}
        u.wounded=0;u.woundRemainder=0;units.remove(u);
        return commander.name+"已进入"+c.name+"，伤兵"+recovered+"立即归队"+(prisoners>0?"；俘虏"+prisoners+"人入狱":"");
    }
    void retreat(Officer o,Hex from) {
        strategy.releaseGovernor(o.id);o.otherTaskTurns=0;o.otherTask="";
        City destination=null;
        for(City c:cities)if(c.owner==o.owner&&(destination==null||from.distance(c.hex)<from.distance(destination.hex)))destination=c;
        o.unitId=-1;o.cityId=destination==null?-1:destination.id;o.acted=true;
    }
    void removeUnit(Unit u) { government.defeated(u,null); }
    void defeatUnit(Unit u,Unit attacker){government.defeated(u,attacker);}
    public Result nextTurn(){reports.prepare();return nextTurn(p->{});}
    public static final class TurnProgress {
        public final int owner,completed,total; public final String phase;
        /** A completed rule phase: presentation may drain here, never inside an unfinished command. */
        public final boolean boundary;
        public TurnProgress(int owner,int completed,int total,String phase){this(owner,completed,total,phase,false);}
        public TurnProgress(int owner,int completed,int total,String phase,boolean boundary){this.owner=owner;this.completed=completed;this.total=total;this.phase=phase;this.boundary=boundary;}
    }
    public Result nextTurn(Consumer<TurnProgress> progress){reports.prepare();
        Objects.requireNonNull(progress);
        if(commandsBlocked())return fail("请先完成当前对局或君主继承");
        if(gameOver())return fail("本局已结束，请重开");
        if(active!=player)return fail("等待电脑行动");
        reports.beginTurn();try {
        List<Integer> sides=new ArrayList<>();for(int offset=1;offset<factions.length;offset++){int side=(player+offset)%factions.length;if(alive(side))sides.add(side);}
        int total=sides.size()+3;progress.accept(new TurnProgress(player,0,total,"委任军团与太守"));
        districts.run();government.runDelegated();int completed=1;
        progress.accept(new TurnProgress(player,completed,total,"委任军团行动完成",true));
        for(int side:sides){active=side;final int done=completed;
            if(alive(side)){progress.accept(new TurnProgress(side,done,total,"准备行动"));reset(side);runAi(phase->progress.accept(new TurnProgress(side,done,total,phase)));checkVictory();
                progress.accept(new TurnProgress(side,done+1,total,"势力行动完成",true));
                if(gameOver()){active=player;progress.accept(new TurnProgress(player,total,total,"战局结束"));return success(winner==player?"战场胜利":"我方势力已覆灭");}}
            completed++;
        }
        final int global=completed;settleGlobalTurn(phase->progress.accept(new TurnProgress(-1,global,total,phase)));
        progress.accept(new TurnProgress(-1,completed+1,total,"设施与全局后勤完成",true));
        reports.nextPlayer();active=player;progress.accept(new TurnProgress(player,completed+1,total,"恢复行动与自动行军"));reset(player);checkVictory();if(!commandsBlocked())marches.advanceAll();
        progress.accept(new TurnProgress(player,total,total,"结算完成",true));return success(date()+" · 行动力恢复");
        } finally { reports.endTurn(); }
    }
    /** Exactly once after all factions have acted. Keep this order stable across save replay. */
    private void settleGlobalTurn(Consumer<String> progress){
        progress.accept("运输、建设与生产");
        reports.globalPhase();governance.reconcile(true);turn++;contests.tick();reports.checkpoint("对局结算");domestic.tick();reports.checkpoint("建设运输结算");campaign.tick();reports.checkpoint("技巧研究结算");army.tick();reports.checkpoint("军备与持续伤害结算");abilities.tick();reports.checkpoint("能力研究结算");recruitment.tick();reports.checkpoint("登用结果结算");envoys.tick();reports.checkpoint("外交任务结算");strategy.tick();reports.checkpoint("人员内政结算");
        progress.accept("火场、守备与武将");war.tick();reports.checkpoint("火场设施结算");
        Map<Integer,SiegeRules.State> siege=SiegeRules.snapshot(this);
        Conscription.settle(this,siege);SiegeRules.settleAttrition(this,siege);reports.checkpoint("围城与季度兵源结算");
        cityDefense.tick();reports.checkpoint("据点守备结算");government.tick();reports.checkpoint("武将任职结算");treasures.tick();reports.checkpoint("宝物发现结算");
        progress.accept("兵粮消耗与城池收入");
        for(Unit u:new ArrayList<>(units)) {
            int consumption=Logistics.foodUse(this,u);
            if(u.food<consumption){int lost=Logistics.deserters(u.troops);u.food=0;u.troops-=lost;note(officer(u.officerId).name+"部队断粮，逃兵"+lost+"，剩余"+u.troops+"兵");}
            else u.food-=consumption;
            if(u.troops<=0)removeUnit(u);reports.checkpoint("部队兵粮消耗");
        }
        for(City c:cities) if(c.owner>=0) {
            int reportFoodBefore=c.food;
            int consumption=cityFoodUse(c)-domestic.arrivalFoodCredit(c);
            if(c.food<consumption){c.food=0;c.troops=Math.max(0,c.troops-Math.max(1,c.troops/20));}
            else c.food-=consumption;
            int reportFoodUse=reportFoodBefore-c.food;
            int goldIncome=Math.min(Math.max(0,campaign.goldCap(c)-c.gold),domestic.goldIncome(c.id,turn,SiegeRules.blocked(siege,c)));int foodIncome=Math.min(Math.max(0,campaign.foodCap(c)-c.food),domestic.foodIncome(c.id,turn,SiegeRules.blocked(siege,c)));
            c.gold+=goldIncome;c.food+=foodIncome;
            c.defense+=cityDefense.recovery(c,SiegeRules.blocked(siege,c));
            reports.note(c.name+"本旬收支：金收入+"+goldIncome+"，粮收入+"+foodIncome+"，驻军实际粮耗"+reportFoodUse+(SiegeRules.blocked(siege,c)?"（围城：本次钱粮收入已减25%）":""));
        }
        progress.accept("事件、寿命与外交");events.tick();reports.checkpoint("世界事件结算");life.tick();reports.checkpoint("武将生涯结算");diplomacy.tick();reports.checkpoint("外交变化结算");
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
        Set<Hex> blocked=new HashSet<>();for(Domestic.Facility f:domestic.facilities)blocked.add(f.hex);for(Unit b:fieldUnits())if(b.id!=u.id)blocked.add(b.hex);
        for(War.Structure s:war.structures())blocked.add(s.hex);
        PriorityQueue<Step> todo=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.hex.q).thenComparingInt(s->s.hex.r));
        distances.put(u.hex,0);todo.add(new Step(u.hex,0));
        while(!todo.isEmpty()) {
            Step step=todo.remove();if(step.cost!=distances.get(step.hex))continue;
            City goalCity=cityAt(target);
            if(goalCity==null?step.hex.distance(target)<=range:SiteFootprint.distance(goalCity,step.hex)<=range) {
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
        City c=cityAt(to);
        return c!=null&&c.kind==SiteKind.GATE&&c.owner!=owner;
    }
}

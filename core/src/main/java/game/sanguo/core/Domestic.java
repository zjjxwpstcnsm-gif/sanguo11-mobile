package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Strategic logistics with shared tactical transport; numeric rates remain engineering parameters. */
public final class Domestic {
    public static final int CITY_SLOTS=6, TRAVEL_SPEED=4;
    public enum Kind {
        MARKET("市场",1000,"每月金 +400"), FARM("农场",800,"每季粮 +2500"),
        BARRACKS("兵舍",1200,"每座每旬可征兵1次；每次征兵 +500"), SMITH("锻冶所",1200,"每座每旬可生产枪/戟/弩共1次；每次产量 +500"),
        MINT("造币",1500,"相邻市场产金 +50%，不重复叠加"), GRANARY("谷仓",1500,"相邻农场产粮 +50%，不重复叠加"),
        STABLE("厩舍",1200,"每座每旬可生产战马1次；每次产量 +500"), BLACK_MARKET("黑市",500,"每月金 +200，不可合并"),
        WORKSHOP("工房",1500,"每座每旬可开工1次攻城器械；跨旬完成"), SHIPYARD("造船厂",1500,"临水建造；每座每旬可开工1次舰船；跨旬完成"), BRONZE_TERRACE("铜雀台",1500,"需要铜雀；每月技巧点+100，不可合并");
        public final String label,effect;public final int cost;
        Kind(String label,int cost,String effect){this.label=label;this.cost=cost;this.effect=effect;}
    }
    public static final class Facility {
        public final int id,cityId;public final Kind kind;public final Hex hex;
        public int builderId,remaining,level=1,upgradeTo,hp=1000,lastUseTurn=-1;
        /** Engineering durability, pending original-game calibration. */
        public int maxHp(){return 1000;}
        Facility(int id,int city,Kind kind,Hex hex,int builder,int remaining){this.id=id;cityId=city;this.kind=kind;this.hex=hex;builderId=builder;this.remaining=remaining;}
    }
    /** The mission IS the battlefield unit; cargo fields are inherited, never mirrored.
     * Battle IDs occupy a disjoint namespace; taskId preserves every historical save reference. */
    public static final class Mission extends World.Unit {
        public final int taskId;public int sourceCity,targetCity;
        public boolean transport,sea,returnOfficers,returning,stopped,legacyOverlap;
        public final int[] cargoShips=new int[2];
        public String waiting="";public int escortId=-1,movementTurn=-1;

        public int consumedFood,lastTick=-1;
        public boolean contains(int officer){if(officerId==officer)return true;for(int id:deputies)if(id==officer)return true;return false;}
        public int[] crew(){int[] ids=new int[deputies.length+1];ids[0]=officerId;System.arraycopy(deputies,0,ids,1,deputies.length);return ids;}
        public final int[] equipment;
        Mission(int id,int owner,int officer,int source,int target,Hex hex,boolean transport,int gold,int food,int troops,int[] equipment){
            super(10000000+id,owner,officer,World.Weapon.SWORD,hex,troops,food);taskId=id;sourceCity=source;targetCity=target;
            this.transport=transport;this.gold=gold;this.food=food;this.troops=troops;this.equipment=Arrays.copyOf(equipment,World.Weapon.values().length);
        }
    }
    public final List<Facility> facilities=new ArrayList<>();
    public final List<Mission> missions=new ArrayList<>();
    private boolean settling;
    private final World w;int nextFacilityId=1,nextMissionId=1;
    // Per atomic turn settlement, never a cached forecast or serialized game state.
    private final Map<Integer,Integer> arrivedTroops=new HashMap<>();
    private final Map<Integer,String> receipts=new HashMap<>();
    /** Actual atomic settlement transcript, not a saved or forecast inventory. */
    public String receipt(int mission){return receipts.get(mission);}
    int arrivalFoodCredit(World.City c){int n=arrivedTroops.getOrDefault(c.id,0);return w.cityFoodUse(c)==0?0:(c.troops+49)/50-(Math.max(0,c.troops-n)+49)/50;}
    Domestic(World w){this.w=w;}
    public Facility facility(int id){for(Facility f:facilities)if(f.id==id)return f;return null;}
    public Facility at(Hex h){for(Facility f:facilities)if(f.hex.equals(h))return f;return null;}
    int damage(Facility f,int amount){
        if(f==null||!facilities.contains(f))return 0;
        int hit=Math.min(f.hp,Math.max(0,amount));f.hp-=hit;
        if(f.hp==0){
            World.Officer builder=w.officer(f.builderId);if(builder!=null)builder.acted=true;
            facilities.remove(f);w.army.cleanup();
            w.battleOutcome(f.kind.label+"已摧毁，地块已释放");
        }
        return hit;
    }
    void writeDurability(DataOutputStream d)throws IOException{
        d.writeInt(facilities.size());for(Facility f:facilities){d.writeInt(f.id);d.writeInt(f.hp);}
    }
    void readDurability(DataInputStream d)throws IOException{
        int count=bound(d.readInt(),0,6000);require(count==facilities.size(),"设施耐久数量错误");Set<Integer> ids=new HashSet<>();
        for(int i=0;i<count;i++){int id=d.readInt();Facility f=facility(id);require(f!=null&&ids.add(id),"设施耐久引用错误");f.hp=bound(d.readInt(),1,f.maxHp());}
    }
    public Mission mission(int id){for(Mission m:missions)if(m.id==id||m.taskId==id)return m;return null;}
    public boolean busy(int officer){
        if(officer<0)return false;
        for(Facility f:facilities)if(f.builderId==officer)return true;
        for(Mission m:missions)if(m.contains(officer))return true;
        return false;
    }
    public String assignment(int officer){
        for(Facility f:facilities)if(f.builderId==officer)return "建设"+f.kind.label+" · 剩"+f.remaining+"旬";
        for(Mission m:missions)if(m.contains(officer))return (m.transport?"运输":"调动")+"至"+w.city(m.targetCity).name+" · "+status(m);
        return "";
    }
    public int count(int city){int n=0;for(Facility f:facilities)if(f.cityId==city)n++;return n;}
    /** Each completed facility supplies one order per turn, shared by all products of its kind. */
    public int capacity(int city,Kind kind){int n=0;for(Facility f:facilities)if(f.cityId==city&&f.kind==kind&&operational(f))n++;return n;}
    public int remainingUses(int city,Kind kind){int n=0;for(Facility f:facilities)if(f.cityId==city&&f.kind==kind&&operational(f)&&f.lastUseTurn!=w.turn)n++;return n;}
    private boolean operational(Facility f){return f.hp>0&&(f.remaining==0||f.upgradeTo>0);}
    public String operationError(int city,Kind kind){
        World.City c=w.city(city);if(c==null||c.kind!=World.SiteKind.CITY)return "只有城市可以征兵和生产军备";
        if(kind==null)return "该兵种无需生产兵装";
        if(capacity(city,kind)==0)return "需要已建成的"+kind.label+"；建设中的设施不提供次数";
        return remainingUses(city,kind)==0?kind.label+"本旬次数已用完（每座每旬1次），请等待下一旬":null;
    }
    public String usage(int city,Kind kind){return kind.label+" · 本旬剩余 "+remainingUses(city,kind)+" / "+capacity(city,kind)+" 次";}
    void use(int city,Kind kind){
        for(Facility f:facilities)if(f.cityId==city&&f.kind==kind&&operational(f)&&f.lastUseTurn!=w.turn){f.lastUseTurn=w.turn;return;}
        throw new IllegalStateException("Facility order must be validated before spending");
    }
    public static Kind productionFacility(World.Weapon weapon){
        if(weapon==null||weapon==World.Weapon.SWORD)return null;
        return Army.siegeWeapon(weapon)?Kind.WORKSHOP:weapon==World.Weapon.CAVALRY?Kind.STABLE:Kind.SMITH;
    }
    private int yield(int city,Kind kind,int base){
        int total=0;for(Facility f:facilities)if(f.cityId==city&&f.kind==kind&&(f.remaining==0||f.upgradeTo>0)){
            int amount=base*(f.level==3?150:f.level==2?120:100)/100;
            Kind support=kind==Kind.MARKET?Kind.MINT:kind==Kind.FARM?Kind.GRANARY:null;
            if(support!=null)for(Facility n:facilities)if(n.cityId==city&&n.kind==support&&n.remaining==0&&n.hex.distance(f.hex)==1){amount=amount*3/2;break;}
            total+=amount;
        }return total;
    }
    public int monthlyGold(int city){return w.strategy.cityIncome(city,800+this.yield(city,Kind.MARKET,400)+this.yield(city,Kind.BLACK_MARKET,200));}
    public int monthlyFood(int city){return w.strategy.cityIncome(city,5000+this.yield(city,Kind.FARM,2500));}
    /** Scheduled income, including the PC month/season-only interaction of wealth and tax skills. */
    public int goldIncome(int city,int turn){
        int base=monthlyGold(city);boolean monthly=turn%3==0,tax=w.skills.city(city,Skill.ZHENGSHUI);
        if(!monthly&&!tax)return 0;
        int amount=tax?base/2:base;
        return monthly&&w.skills.city(city,Skill.FUHAO)?amount*3/2:amount;
    }
    public int foodIncome(int city,int turn){
        if(turn%3!=0)return 0;boolean season=(w.startMonth-1+turn/3)%3==0,tax=w.skills.city(city,Skill.ZHENGSHOU);
        if(!season&&!tax)return 0;int amount=tax?monthlyFood(city)/2:monthlyFood(city);
        return season&&w.skills.city(city,Skill.MIDAO)?amount*3/2:amount;
    }
    public String incomeSchedule(int city){return "月初基准金 "+monthlyGold(city)+" / 季初基准粮 "+monthlyFood(city)+"\n下旬结算 金 "+goldIncome(city,w.turn+1)+" / 粮 "+foodIncome(city,w.turn+1);}
    public int recruitAmount(int city){return 2000+this.yield(city,Kind.BARRACKS,500);}
    public int produceAmount(int city){return 2000+this.yield(city,Kind.SMITH,500);}
    public int produceAmount(int city,World.Weapon weapon){return 2000+this.yield(city,weapon==World.Weapon.CAVALRY?Kind.STABLE:Kind.SMITH,500);}
    public static boolean mergeable(Kind kind){return kind==Kind.MARKET||kind==Kind.FARM||kind==Kind.BARRACKS||kind==Kind.SMITH||kind==Kind.STABLE;}
    /** Mobile shortcut: upgradeable facilities are built at their highest level. */
    public static int buildLevel(Kind kind){return mergeable(kind)?3:1;}
    public static String buildEffect(Kind kind){
        switch(kind){
            case MARKET:return "每月金 +600";
            case FARM:return "每季粮 +3750";
            case BARRACKS:return "每座每旬可征兵1次；每次征兵 +750";
            case SMITH:return "每座每旬可生产枪/戟/弩共1次；每次产量 +750";
            case STABLE:return "每座每旬可生产战马1次；每次产量 +750";
            case WORKSHOP:return "每座每旬可开工1次攻城器械；跨旬完成";
            case SHIPYARD:return "临水建造；每座每旬可开工1次舰船；跨旬完成";
            default:return kind.effect;
        }
    }
    public List<Facility> mergeCandidates(int target){
        Facility f=facility(target);List<Facility> result=new ArrayList<>();if(f==null||f.remaining>0||f.level>=3||!mergeable(f.kind))return result;
        for(Facility other:facilities)if(other.id!=f.id&&other.cityId==f.cityId&&other.kind==f.kind&&other.remaining==0&&other.level==1&&other.hex.distance(f.hex)==1)result.add(other);return result;
    }
    public World.Result merge(int target,int consumed,int officer){w.reports.prepare();
        Facility f=facility(target),material=facility(consumed);if(f==null||material==null||!mergeCandidates(target).contains(material))return w.fail("需要相邻同类Lv1设施，目标等级须低于Lv3");
        World.City c=w.city(f.cityId);World.Officer o=w.officer(officer);String error=w.cityError(c,o,200);if(error!=null)return w.fail(error);
        w.spend(c,o,200);facilities.remove(material);f.upgradeTo=f.level+1;f.builderId=o.id;f.remaining=2;
        return w.success(f.kind.label+"开始吸收合并，2旬后达到Lv"+f.upgradeTo+"，原材料地块已腾空");
    }
    private boolean site(World.City city,Hex h){
        if(city==null||h==null||!w.inside(h)||!w.development.contains(city,h))return false;
        if(w.terrain[h.q][h.r]!=World.Terrain.PLAIN||w.cityAt(h)!=null||w.unitAt(h)!=null||at(h)!=null||w.war.at(h)!=null||w.war.fireAt(h)!=null)return false;
        for(World.City c:w.cities)if(SiteFootprint.distance(c,h)==1){
            boolean exit=false;
            for(Hex n:SiteFootprint.edge(c))if(!n.equals(h)&&w.cost(n,World.Weapon.SPEAR)>0&&w.cityAt(n)==null&&at(n)==null&&w.war.at(n)==null){exit=true;break;}
            if(!exit)return false;
        }
        return true;
    }
    public List<Hex> buildSites(int cityId){
        World.City c=w.city(cityId);List<Hex> result=new ArrayList<>();
        if(c==null||c.kind!=World.SiteKind.CITY||count(cityId)>=w.development.capacity(cityId))return result;
        if(w.development.configured(cityId)){
            for(Hex h:w.development.parcels(cityId))if(site(c,h))result.add(h);return result;
        }
        for(int q=Math.max(0,c.hex.q-3);q<=Math.min(w.width-1,c.hex.q+3);q++)
            for(int r=Math.max(0,c.hex.r-3);r<=Math.min(w.height-1,c.hex.r+3);r++){Hex h=new Hex(q,r);if(site(c,h))result.add(h);}
        result.sort(Comparator.comparingInt((Hex h)->h.distance(c.hex)).thenComparingInt(h->h.q).thenComparingInt(h->h.r));
        return result;
    }
    public World.Result build(int cityId,int officerId,Kind kind,Hex h){w.reports.prepare();
        if(kind==null)return w.fail("设施类型无效");
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,kind.cost);
        if(error!=null)return w.fail(error);
        if(c.kind!=World.SiteKind.CITY)return w.fail("港关不能开发内政设施");
        if(count(cityId)>=w.development.capacity(cityId))return w.fail("本城最多"+w.development.capacity(cityId)+"处设施（含建设中）");
        if(kind==Kind.BRONZE_TERRACE&&(!w.treasures.factionHas(c.owner,Treasures.Kind.BRONZE)||facilities.stream().anyMatch(f->f.cityId==cityId&&f.kind==kind)))return w.fail("需要持有铜雀，且本城至多一座铜雀台");
        if(kind==Kind.SHIPYARD&&(h==null||h.neighbors().stream().noneMatch(w.army::water)))return w.fail("造船厂必须建在临水的开发地");
        if(!site(c,h))return w.fail("请选择本城空闲开发地，且不能封死城池出口");
        if(nextFacilityId>=10000000)return w.fail("设施编号已达上限");
        int turns=o.politics>=80?2:3;w.spend(c,o,kind.cost);
        Facility facility=new Facility(nextFacilityId++,c.id,kind,h,o.id,turns);
        facility.level=buildLevel(kind);facilities.add(facility);
        return w.success(o.name+"开始建设"+kind.label+" Lv"+facility.level+"，需要"+turns+"旬");
    }
    public World.Result cancelBuild(int id){w.reports.prepare();
        Facility f=facility(id);
        if(w.commandsBlocked()||w.gameOver()||f==null||w.city(f.cityId).owner!=w.active||f.remaining==0)return w.fail("没有可取消的己方建设");
        w.officer(f.builderId).acted=true;
        if(f.upgradeTo>0){f.upgradeTo=0;f.remaining=0;f.builderId=-1;return w.success("已取消合并，保留原等级；费用和已吸收设施不退还");}
        facilities.remove(f);return w.success("已取消"+f.kind.label+"建设，不退还费用");
    }
    public World.Result demolish(int id,int officerId){w.reports.prepare();
        Facility f=facility(id);if(f==null||f.remaining!=0)return w.fail("请选择已完成设施");
        World.City c=w.city(f.cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,0);
        if(error!=null)return w.fail(error);
        w.spend(c,o,0);facilities.remove(f);w.army.cleanup();return w.success(c.name+"拆除"+f.kind.label+"，不退还费用");
    }
    void captured(int city){
        for(Facility f:new ArrayList<>(facilities))if(f.cityId==city&&f.remaining>0){
            World.Officer o=w.officer(f.builderId);if(o!=null)o.acted=true;
            if(f.upgradeTo>0){f.remaining=0;f.builderId=-1;f.upgradeTo=0;}else facilities.remove(f);w.note("城池失守，"+f.kind.label+"建设中止");
        }
    }
    /** Per surviving completed facility, independently. Engineering balance, not claimed PC parity. */
    public static final int CAPTURE_DESTRUCTION_PERCENT=25;
    List<String> sack(int city){
        World.City c=w.city(city);List<String> lost=new ArrayList<>();
        if(c==null||c.kind!=World.SiteKind.CITY)return lost;
        List<Facility> candidates=new ArrayList<>();
        for(Facility f:facilities)if(f.cityId==city&&f.remaining==0)candidates.add(f);
        candidates.sort(Comparator.comparingInt(f->f.id)); // Stable across saves and list iteration order.
        for(Facility f:candidates)if(w.strategy.nextInt(100)<CAPTURE_DESTRUCTION_PERCENT){
            lost.add(f.kind.label);damage(f,f.hp); // Shared destruction releases the plot and dependent production.
        }
        if(!lost.isEmpty())w.note(c.name+"战乱损毁内政设施"+lost.size()+"座："+String.join("、",lost));
        return lost;
    }
    public World.Result transfer(int source,int target,int officer){w.reports.prepare();return dispatch(source,target,officer,new int[0],false,0,0,0,new int[4],false,false);}
    public World.Result transport(int source,int target,int officer,int gold,int food,int troops,int[] equipment){w.reports.prepare();return transport(source,target,officer,new int[0],gold,food,troops,equipment,false,false);}
    public World.Result transportSea(int source,int target,int officer,int gold,int food,int troops,int[] equipment){w.reports.prepare();return transport(source,target,officer,new int[0],gold,food,troops,equipment,true,false);}
    public World.Result transport(int source,int target,int officer,int[] deputies,int gold,int food,int troops,int[] equipment,boolean sea,boolean returnOfficers){w.reports.prepare();
        return dispatch(source,target,officer,deputies,true,gold,food,troops,equipment,sea,returnOfficers);
    }
    public String shipCargoError(int source,int[] ships){World.City c=w.city(source);if(c==null||ships==null||ships.length!=2)return "舰船货物无效";for(int i=0;i<2;i++)if(ships[i]<0||ships[i]>100||ships[i]>c.ships[i])return "舰船货物超过库存或100上限";return null;}
    public World.Result transport(int source,int target,int officer,int[] deputies,int gold,int food,int troops,int[] equipment,boolean sea,boolean returning,int[] ships){w.reports.prepare();
        String error=shipCargoError(source,ships);if(error!=null)return w.fail(error);
        World.Result result=transport(source,target,officer,deputies,gold,food,troops,equipment,sea,returning);if(!result.ok)return result;
        Mission m=missions.get(missions.size()-1);for(int i=0;i<2;i++){m.cargoShips[i]=ships[i];w.city(source).ships[i]-=ships[i];}return result;
    }
    public String transportError(int source,int target,int officer,int[] deputies,int gold,int food,int troops,int[] equipment,boolean sea){
        return dispatchError(source,target,officer,deputies,true,gold,food,troops,equipment,sea);
    }
    private String dispatchError(int source,int target,int officer,int[] deputies,boolean cargo,int gold,int food,int troops,int[] equipment,boolean sea){
        World.City c=w.city(source),d=w.city(target);World.Officer o=w.officer(officer);
        String error=w.cityError(c,o,0);if(error!=null)return error;
        if(w.districts.dispatchError(source,target,cargo)!=null)return w.districts.dispatchError(source,target,cargo);
        if(d==null||d.owner!=w.active||d.id==c.id)return "请选择另一座己方城池";
        if(!payload(gold,food,troops,equipment))return "运输数量越界";
        if(deputies==null||deputies.length>2)return "运输编队最多三名武将";
        Set<Integer> crew=new HashSet<>();crew.add(officer);
        for(int id:deputies){if(!crew.add(id))return "运输武将不能重复";error=w.cityError(c,w.officer(id),0);if(error!=null)return error;}
        if(cargo&&gold+food+troops+Arrays.stream(equipment).sum()==0)return "至少携带一种资源";
        if(c.gold<gold||c.food<food||c.troops<troops)return "金、粮或兵力库存不足";
        if(cargo&&w.districts.reserveError(c,gold,food,troops)!=null)return w.districts.reserveError(c,gold,food,troops);
        for(int i=0;i<equipment.length;i++)if(c.equipment[i]<equipment[i])return "兵装库存不足";

        if(cargo){Mission probe=new Mission(0,c.owner,officer,source,target,c.hex,true,gold,food,troops,equipment);probe.sea=sea;probe.deputies=deputies.clone();
            SiteFootprint.Deployment departure=SiteFootprint.deployment(w,c,probe);if(!departure.valid())return departure.error;
            departure.apply(probe);MarchOrders.Plan plan=routePlan(probe);if(!plan.valid())return plan.error;}else if(w.personnel.turns(source,target)<0)return "没有可用人员路线";
        if(nextMissionId>=10000000)return "任务编号已达上限";
        return null;
    }
    private World.Result dispatch(int source,int target,int officer,int[] deputies,boolean cargo,int gold,int food,int troops,int[] equipment,boolean sea,boolean returnOfficers){
        String error=dispatchError(source,target,officer,deputies,cargo,gold,food,troops,equipment,sea);if(error!=null)return w.fail(error);
        World.City c=w.city(source),d=w.city(target);World.Officer o=w.officer(officer);
        Mission mission=new Mission(nextMissionId++,w.active,o.id,c.id,d.id,c.hex,cargo,gold,food,troops,equipment);
        mission.sea=sea;mission.deputies=deputies.clone();mission.returnOfficers=cargo&&returnOfficers;
        if(cargo){SiteFootprint.Deployment departure=SiteFootprint.deployment(w,c,mission);
            if(!departure.valid()){nextMissionId--;return w.fail(departure.error);}
            departure.apply(mission);mission.movementTurn=w.turn;
        }
        w.spend(c,o,0);c.gold-=gold;c.food-=food;c.troops-=troops;for(int i=0;i<equipment.length;i++)c.equipment[i]-=equipment[i];
        for(int id:mission.crew()){World.Officer member=w.officer(id);w.strategy.releaseGovernor(id);member.cityId=-1;member.acted=true;}
        missions.add(mission);
        return w.success(o.name+(cargo?"运送资源":"调动")+"前往"+d.name+(returnOfficers?"；卸货后人员返程":""));
    }
    /** Read-only forecast; numbers are the current command's actual limits, never silent clamping. */
    public String transportPreview(int source,int target,int officer,int[] deputies,int gold,int food,int troops,int[] equipment,boolean sea,boolean returnOfficers){
        String error=transportError(source,target,officer,deputies,gold,food,troops,equipment,sea);
        World.City c=w.city(source),d=w.city(target);if(c==null||d==null)return error;
        Mission probe=new Mission(0,c.owner,officer,source,target,c.hex,true,gold,food,troops,equipment==null?new int[4]:equipment);probe.sea=sea;probe.deputies=deputies==null?new int[0]:deputies;
        SiteFootprint.Deployment departure=SiteFootprint.deployment(w,c,probe);if(departure.valid())departure.apply(probe);
        int turns=departure.valid()?eta(probe):-1,use=foodUse(probe),cost=turns<0?0:turns*use;
        return "派遣费0金 · 行动力10 · 编队"+(probe.deputies.length+1)+"将\n"+
            (departure.valid()?"从城市逻辑中心计费：出城已付"+departure.cost+" · 本旬剩余"+w.orders.remaining(probe):departure.error)+"\n"+
            "可运库存上限：金"+Math.min(100000,c.gold)+" / 粮"+Math.min(200000,c.food)+" / 兵"+Math.min(20000,c.troops)+"\n"+
            "目的地剩余容量：金"+Math.max(0,w.campaign.goldCap(d)-d.gold)+" / 粮"+Math.max(0,w.campaign.foodCap(d)-d.food)+" / 兵"+Math.max(0,w.campaign.troopCap(d)-d.troops)+"\n"+
            (turns<0?"路线不通":"预计"+turns+"旬，途中耗粮约"+cost+"，预计入库粮"+Math.max(0,food-cost))+"\n"+
            (turns>=0&&food<cost?"警告：携粮不足，途中会损兵。":"途中耗粮从所携粮扣除；阻塞/截击会改变预估。")+"\n"+
            (fits(probe,d)?"当前容量可接收":"目的地容量不足，抵达后等待；不会丢弃货物")+"\n"+
            (returnOfficers?"卸货后仅武将返程，兵粮入库；返程沿真实路线。":"抵达后武将留驻。")+(error==null?"":"\n不能执行："+error);
    }
    private static boolean payload(int gold,int food,int troops,int[] equipment){
        if(gold<0||gold>100000||food<0||food>200000||troops<0||troops>20000||equipment==null||(equipment.length!=4&&equipment.length!=World.Weapon.values().length))return false;
        for(int i=0;i<equipment.length;i++)if(equipment[i]<0||equipment[i]>(i==World.Weapon.SWORD.ordinal()?0:i>4?100:20000))return false;return true;
    }
    public World.Result redirect(int id,int target){w.reports.prepare();
        Mission m=mission(id);World.City c=w.city(target);
        if(w.commandsBlocked()||w.gameOver()||m==null||m.owner!=w.active||c==null||c.owner!=m.owner||target==m.targetCity)return w.fail("请选择本势力在途任务与新的己方目的地");
        if(w.actionPoints[w.active]<10)return w.fail("行动力不足10");
        String permission=w.districts.dispatchError(m.sourceCity,target,m.transport);if(permission!=null)return w.fail(permission);
        Districts.District control=w.districts.city(m.sourceCity);
        if(control!=null&&control.owner==w.player&&!w.districts.directCity(m.sourceCity))return w.fail("委任军团任务请先调整军团方针或撤销托管");
        if(m.transport?!w.marches.convoyRoute(m,target).valid():w.personnel.turns(m.hex,target)<0)return w.fail("没有可用路线");
        w.actionPoints[w.active]-=10;m.targetCity=target;m.stopped=false;m.march=null;if(target==m.sourceCity)m.returnOfficers=false;return w.success("任务已改道至"+c.name);
    }
    private static final class Step {final Hex h;final int cost;Step(Hex h,int c){this.h=h;cost=c;}}
    private int travelCost(Hex h,int owner,boolean sea){
        int cost=sea&&w.army.water(h)?1:w.fieldworks.landCost(h,World.Weapon.SPEAR,owner);if(cost<0||at(h)!=null||w.war.at(h)!=null)return -1;
        World.City c=w.cityAt(h);return c!=null&&c.owner!=owner?-1:cost;
    }
    /** Strategic movement: avoids hostile cities/facilities but ignores tactical unit occupancy. */
    public List<Hex> route(Hex from,Hex to,int owner){return route(from,to,owner,false);}
    public List<Hex> route(Hex from,Hex to,int owner,boolean sea){
        if(from==null||to==null||!w.inside(from)||!w.inside(to))return null;
        Map<Hex,Integer> distance=new HashMap<>();Map<Hex,Hex> previous=new HashMap<>();
        PriorityQueue<Step> open=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.h.q).thenComparingInt(s->s.h.r));
        distance.put(from,0);open.add(new Step(from,0));
        while(!open.isEmpty()){
            Step s=open.remove();if(s.cost!=distance.get(s.h))continue;
            if(s.h.equals(to)){LinkedList<Hex> result=new LinkedList<>();Hex h=to;while(!h.equals(from)){result.addFirst(h);h=previous.get(h);}return result;}
            for(Hex h:s.h.neighbors()){
                int cost=travelCost(h,owner,sea);if(w.army.water(s.h)!=w.army.water(h)&&w.army.transitionPort(owner,s.h,h)==null)continue;if(cost<0||!w.army.water(s.h)&&!w.army.water(h)&&w.gateBlocks(owner,s.h,h))continue;int total=s.cost+cost;
                if(total<distance.getOrDefault(h,Integer.MAX_VALUE)){distance.put(h,total);previous.put(h,s.h);open.add(new Step(h,total));}
            }
        }
        return null;
    }
    public int eta(Mission m) {
        World.City c = this.w.city(m.targetCity);
        if (c == null || c.owner != m.owner) {
            return -1;
        }
        if (!m.transport) {
            return this.w.personnel.turns(m.hex, c.id);
        }
        MarchOrders.Plan p = routePlan(m);
        if (p.valid()) {
            return Math.max(1, p.estimatedTurns + 1);
        }
        return -1;
    }
    public int deliverableEta(Mission m) {
        World.City c = this.w.city(m.targetCity);
        if (c == null || c.owner != m.owner || !fits(m, c) || m.stopped) {
            return -1;
        }
        if (!m.transport) {
            return this.w.personnel.turns(m.hex, c.id);
        }
        if (threatReason(m) == null) {
            return eta(m);
        }
        return -1;
    }
    private int estimate(List<Hex> path,Mission m){int turns=0,budget=0;for(Hex h:path){int cost=travelCost(h,m.owner,m.sea);if(budget<cost){turns++;budget=travelSpeed(m);}budget-=cost;}return turns;
    }
    private int travelSpeed(Mission m){return TRAVEL_SPEED+(m.transport&&w.campaign.has(m.owner,Campaign.Tech.WOODEN_OX)?1:0)+(m.transport&&Arrays.stream(m.crew()).anyMatch(id->w.skills.has(w.officer(id),Skill.YUNBAN))?2:0);}
    /** Transport rations use half the field-army base rate; current friendly camp auras apply. */
    public int foodUse(Mission m){return Logistics.foodUse(w,m);}
    public int expectedFood(Mission m){int turns=eta(m);return turns<0?0:Math.max(0,m.food-turns*foodUse(m));}
    public String status(Mission m){
        World.City c=w.city(m.targetCity);if(c==null||c.owner!=m.owner)return "目的地失守，待选择可达己城";
        if(m.stopped)return m.march==null?"已停止，仍消耗携粮；选择地图目的地可继续":"按地图路线行军，到达后待命 · "+w.marches.label(m.march);
        if(!m.waiting.isEmpty())return m.waiting;
        if(w.army.canEnterSite(m,m.hex,c))return fits(m,c)?"已到达，等待结算":"满仓等待，货物仍在队中";
        int turns=eta(m);if(turns<0)return "道路受阻 / 等待改道";
        return (m.returning?"人员返程 · ":"")+"预计"+turns+"旬（依当前道路与威胁）"+(m.transport?" · 旬耗粮"+foodUse(m)+" · "+(m.food<foodUse(m)?"已断粮，将损兵":"携粮可支撑"+(foodUse(m)==0?"无限":m.food/foodUse(m))+"旬"):"");
    }
    private boolean fits(Mission m,World.City c){
        for(int i=0;i<2;i++)if(c.ships[i]>100-m.cargoShips[i])return false;
        if(c.gold>w.campaign.goldCap(c)-m.gold||c.food>w.campaign.foodCap(c)-m.food||c.troops>w.campaign.troopCap(c)-m.troops-m.wounded)return false;
        for(int i=0;i<m.equipment.length;i++)if(c.equipment[i]>w.campaign.equipmentCap(c,World.Weapon.values()[i])-m.equipment[i])return false;return true;
    }
    void tick(){
        arrivedTroops.clear();receipts.clear();cleanupDefeated();
        for(Facility f:facilities)if(f.remaining>0){
            f.remaining--;if(f.remaining==0){World.Officer o=w.officer(f.builderId);o.acted=true;f.builderId=-1;if(f.upgradeTo>0){f.level=f.upgradeTo;f.upgradeTo=0;}w.note(w.city(f.cityId).name+"的"+f.kind.label+"建成 · Lv"+f.level);}
        }
        for(Mission m:new ArrayList<>(missions)){
            if(m.lastTick==w.turn)continue;m.lastTick=w.turn;
            if(m.movementTurn!=w.turn){w.orders.reset(m);m.movementTurn=w.turn;}
            if(m.transport){int use=foodUse(m),paid=Math.min(m.food,use);m.food-=paid;m.consumedFood+=paid;
                if(paid<use){int lost=Logistics.deserters(m.troops);m.troops-=lost;w.note(w.officer(m.officerId).name+"运输队断粮，损失"+lost+"兵；余粮"+m.food);
                    if(m.troops==0){w.removeUnit(m);continue;}}
            }
            World.City c=w.city(m.targetCity);
            if(c.owner!=m.owner){
                World.City best=null;int bestCost=Integer.MAX_VALUE;
                for(World.City d:w.cities)if(d.owner==m.owner){int cost;
                    if(m.transport){MarchOrders.Plan path=w.marches.convoyRoute(m,d.id);if(!path.valid())continue;cost=path.cost;}
                    else {cost=w.personnel.turns(m.hex,d.id);if(cost<0)continue;}
                    if(cost<bestCost||(cost==bestCost&&(best==null||d.id<best.id))){best=d;bestCost=cost;}}
                if(best==null)continue;m.targetCity=best.id;m.march=null;c=best;w.note(w.officer(m.officerId).name+"因目的地失守改道至"+c.name);
            }
            if(m.transport){advanceTransport(m);continue;}
            m.hex=w.personnel.next(m.hex,c.id);
            if(m.hex.equals(c.hex)&&fits(m,c))deliver(m,c);

        }
    }
    public boolean commandable(Mission m){return settling||w.districts.directCity(m.sourceCity);}
    private MarchOrders.Plan routePlan(Mission m){
        // Prediction has no mutations: the caller's actual active faction is honored by shared commands.
        return w.marches.convoyRoute(m);
    }
    private void advanceTransport(Mission m){
        int active=w.active;boolean previous=settling;w.active=m.owner;settling=true;
        try{
            if(m.stopped&&m.march==null){m.waiting="已停止，携粮继续消耗";return;}
            World.City c=w.city(m.targetCity);m.waiting="";
            if(m.escortId>=0&&(w.unit(m.escortId)==null||w.unit(m.escortId).owner!=m.owner))m.escortId=-1;
            String threat=threatReason(m);if(!m.stopped&&threat!=null){m.waiting=threat;return;}
            if(m.march!=null){w.marches.advance(m);if(mission(m.id)!=m||!m.transport)return;if(m.march!=null){m.waiting=m.march.paused.isEmpty()?"":"道路受阻 / 等待："+m.march.paused;if(!m.waiting.isEmpty()){approachQueue(m);yieldConvoy(m);}return;}}
            if(m.stopped)return;
            if(!w.army.canEnterSite(m,m.hex,c)){
                MarchOrders.Plan plan=routePlan(m);
                if(!plan.valid()){m.waiting="道路受阻，等待改道："+plan.error;approachQueue(m);yieldConvoy(m);return;}
                World.Result result=w.marches.execute(w.marches.preview(m,w.city(m.targetCity).hex));if(!result.ok){m.waiting=result.message;return;}
                if(mission(m.id)!=m||!m.transport)return;
            }
            if(w.army.canEnterSite(m,m.hex,c)){if(fits(m,c))deliver(m,c);else m.waiting="满仓等待，货物仍在队中";}
        }finally{w.active=active;settling=previous;}
    }
    /** Near-term safety decision shared by manual automatic orders and delegated logistics. */
    String threatReason(Mission m){
        if(m.gold+m.food/10+m.troops<1500)return null;
        boolean nearby=false;for(World.Unit u:w.units)if(w.campaign.hostile(m.owner,u.owner)&&u.hex.distance(m.hex)<=10){nearby=true;break;}if(!nearby)return null;
        MarchOrders.Plan plan=routePlan(m);if(!plan.valid())return null;
        int danger=0,guard=0;
        for(World.Unit u:w.units){
            if(w.campaign.hostile(m.owner,u.owner)){for(int i=0;i<Math.min(5,plan.path.size());i++)if(u.hex.distance(plan.path.get(i))<=2){danger+=u.troops;break;}}
            else if(u.owner==m.owner&&!Army.siegeWeapon(u.weapon)&&u.hex.distance(m.hex)<=4)guard+=u.troops;
        }
        return danger>0&&guard<danger?"前路敌军"+danger+"兵，附近护军"+guard+"兵；等待真实护军接近、敌军撤离或地图改道":null;
    }
    private void approachQueue(Mission m){
        MarchOrders.Plan plan=w.marches.convoyQueueRoute(m);if(!plan.valid()||plan.path.size()<2)return;
        Map<Hex,Integer> reachable=w.orders.reachable(m);int end=0;
        for(int i=1;i<plan.path.size();i++){Hex h=plan.path.get(i);if(w.unitAt(h)!=null||!reachable.containsKey(h))break;end=i;}
        if(end>0){Hex h=plan.path.get(end);if(w.orders.executeRoute(w.orders.previewMove(m.id,h),plan.path.subList(0,end+1)).ok)m.waiting="已到队列前端，等待前方友军通过";}
    }
    private void yieldConvoy(Mission m){
        if(w.orders.remaining(m)==0)return;
        for(Mission other:missions)if(other.transport&&other.owner==m.owner&&other.taskId<m.taskId&&m.hex.distance(other.hex)==1){
            Hex goal=w.city(other.targetCity).hex;
            for(Hex h:m.hex.neighbors())if(h.distance(goal)>m.hex.distance(goal)&&w.unitAt(h)==null&&SiteFootprint.transit(w.cityAt(h),m.owner)){
                UnitOrders.MovePlan move=w.orders.previewMove(m.id,h);if(move.valid()&&w.orders.execute(move).ok){m.waiting="排队让行：为运输"+other.taskId+"让出狭口，下旬继续";return;}
            }
        }
    }
    String arrivalError(Mission m,World.City c){
        if(mission(m.id)!=m||!m.transport||c==null||c.owner!=m.owner)return "请选择己方据点";
        String permission=w.districts.dispatchError(m.sourceCity,c.id,true);if(permission!=null)return permission;
        return fits(m,c)?null:"据点容量不足，货物和伤兵保留在运输队";
    }
    String arrive(Mission m,World.City c){int healed=m.wounded;deliver(m,c);return "运输队抵达"+c.name+"，物资一次入库，伤兵"+healed+"立即归队";}
    public World.Result unload(Mission m,int city){w.reports.prepare();
        World.City c=w.city(city);if(c==null||!w.army.canEnterSite(m,m.hex,c))return w.fail("尚未抵达合法入口");
        String error=arrivalError(m,c);return error==null?w.success(arrive(m,c)):w.fail(error);
    }
    private void deliver(Mission m,World.City c){
        if(!missions.contains(m))return; // Receipt and cargo transfer are exactly once.
        c.gold+=m.gold;c.food+=m.food;c.troops+=m.troops+m.wounded;
        if(settling)arrivedTroops.merge(c.id,m.troops+m.wounded,Integer::sum);
        for(int i=0;i<m.equipment.length;i++)c.equipment[i]+=m.equipment[i];
        for(int i=0;i<2;i++)c.ships[i]+=m.cargoShips[i];
        String receipt="抵达"+c.name+"，入库：金 "+m.gold+" · 粮 "+m.food+" · 兵 "+m.troops+"，伤兵恢复 "+m.wounded+"；累计途中耗粮"+m.consumedFood;
        receipts.put(m.id,receipt);w.note(receipt);m.hex=c.hex;m.march=null;m.waiting="";
        m.gold=0;m.food=0;m.troops=0;m.wounded=0;m.woundRemainder=0;Arrays.fill(m.equipment,0);Arrays.fill(m.cargoShips,0);
        if(m.transport&&m.returnOfficers&&c.id!=m.sourceCity&&w.city(m.sourceCity).owner==m.owner){
            m.transport=false;m.returning=true;m.returnOfficers=false;m.stopped=false;m.targetCity=m.sourceCity;
            w.note(w.officer(m.officerId).name+"等"+m.crew().length+"将卸货返程，无返程物资");
        }else{for(int id:m.crew()){World.Officer member=w.officer(id);member.cityId=c.id;member.acted=true;}missions.remove(m);}
    }
    /** v20 tasks retain their exact cargo/personnel/location; only their battlefield identity is enabled.
     * Overlapping legacy tasks wait at that exact position until they can leave legally. */
    void migrateTactical(){Set<Hex> occupied=new HashSet<>();for(World.Unit u:w.units)occupied.add(u.hex);
        for(Mission m:missions)if(m.transport){m.legacyOverlap=w.cityAt(m.hex)==null&&!occupied.add(m.hex);m.movementTurn=w.turn;
            // Old task movement already ran this turn; older formats have no per-turn budget trace.
            // Freeze in-flight legacy movement until the next tick, without moving or refunding cargo.
            if(m.lastTick==w.turn||w.cityAt(m.hex)==null){m.movementBudget=w.war.movement(m);m.movementSpent=m.movementBudget;}}
    }
    void writeTactical(DataOutputStream d)throws IOException{
        d.writeInt(missions.size());for(Mission m:missions){d.writeInt(m.taskId);d.writeBoolean(m.stopped);d.writeBoolean(m.legacyOverlap);d.writeInt(m.movementTurn);d.writeInt(m.movementBudget);d.writeInt(m.movementSpent);d.writeBoolean(m.acted);d.writeByte(m.status.ordinal());d.writeInt(m.statusTurns);d.writeInt(m.energy);d.writeInt(m.burning);d.writeInt(m.burningOwner);d.writeInt(m.burningPower);d.writeInt(m.escortId);d.writeUTF(m.waiting);
            for(int n:m.cargoShips)d.writeInt(n);d.writeBoolean(m.march!=null);if(m.march!=null){MarchOrders.Order o=m.march;d.writeByte(o.kind.ordinal());d.writeInt(o.tile.q);d.writeInt(o.tile.r);d.writeInt(o.targetId);d.writeInt(o.owner);d.writeUTF(o.paused);}}
    }
    void readTactical(DataInputStream d)throws IOException{
        int n=bound(d.readInt(),0,10000);require(n==missions.size(),"运输战术记录数错误");Set<Integer> ids=new HashSet<>();
        for(int i=0;i<n;i++){Mission m=mission(d.readInt());require(m!=null&&ids.add(m.id),"运输战术引用错误");m.stopped=d.readBoolean();m.legacyOverlap=d.readBoolean();m.movementTurn=d.readInt();m.movementBudget=d.readInt();m.movementSpent=d.readInt();m.acted=d.readBoolean();m.status=War.Status.values()[bound(d.readUnsignedByte(),0,2)];m.statusTurns=d.readInt();m.energy=d.readInt();m.burning=d.readInt();m.burningOwner=d.readInt();m.burningPower=d.readInt();m.escortId=d.readInt();m.waiting=d.readUTF();
            for(int j=0;j<2;j++)m.cargoShips[j]=d.readInt();if(d.readBoolean()){MarchOrders.Kind kind=MarchOrders.Kind.values()[bound(d.readUnsignedByte(),0,4)];Hex tile=new Hex(d.readInt(),d.readInt());m.march=new MarchOrders.Order(kind,tile,d.readInt(),d.readInt());m.march.paused=d.readUTF();}}
    }
    void cleanupDefeated(){for(Mission m:new ArrayList<>(missions))if(!w.alive(m.owner)){missions.remove(m);for(int id:m.crew())w.officer(id).acted=true;w.note(w.faction(m.owner)+"覆灭，在途任务终止");}}
    void runAi(){
        for(World.City c:w.cities)if(c.owner==w.active&&c.gold>=2500){
            List<World.Officer> idle=w.idle(c);if(idle.isEmpty())continue;
            Kind kind=null;boolean market=false,farm=false;
            for(Facility f:facilities)if(f.cityId==c.id){market|=f.kind==Kind.MARKET;farm|=f.kind==Kind.FARM;}
            if(!market)kind=Kind.MARKET;else if(!farm)kind=Kind.FARM;
            List<Hex> sites=buildSites(c.id);if(kind!=null&&!sites.isEmpty())build(c.id,idle.get(0).id,kind,sites.get(0));
        }
    }
    void writeLogistics(DataOutputStream d)throws IOException{
        d.writeInt(missions.size());for(Mission m:missions){d.writeInt(m.taskId);d.writeInt(m.deputies.length);for(int id:m.deputies)d.writeInt(id);d.writeBoolean(m.returnOfficers);d.writeBoolean(m.returning);d.writeInt(m.consumedFood);d.writeInt(m.lastTick);}
    }
    void readLogistics(DataInputStream d)throws IOException{
        int n=bound(d.readInt(),0,10000);require(n==missions.size(),"物流扩展数量错误");Set<Integer> seen=new HashSet<>();
        for(int i=0;i<n;i++){Mission m=mission(d.readInt());require(m!=null&&seen.add(m.id),"物流扩展引用错误");m.deputies=new int[bound(d.readInt(),0,2)];for(int j=0;j<m.deputies.length;j++)m.deputies[j]=d.readInt();m.returnOfficers=d.readBoolean();m.returning=d.readBoolean();m.consumedFood=d.readInt();m.lastTick=d.readInt();}
    }
    void officerDied(int officer){for(Mission m:new ArrayList<>(missions))if(m.contains(officer)){
        if(m.officerId!=officer)m.deputies=Arrays.stream(m.deputies).filter(id->id!=officer).toArray();
        else if(m.deputies.length>0){m.officerId=m.deputies[0];m.deputies=Arrays.copyOfRange(m.deputies,1,m.deputies.length);w.note(w.officer(m.officerId).name+"接掌在途任务，货物继续运输");}
        else{missions.remove(m);w.note(w.officer(officer).name+"在途中去世，任务终止，余货散失");}
    }}
    void write(DataOutputStream d)throws IOException{
        d.writeInt(nextFacilityId);d.writeInt(nextMissionId);d.writeInt(facilities.size());
        for(Facility f:facilities){d.writeInt(f.id);d.writeInt(f.cityId);d.writeByte(f.kind.ordinal());d.writeInt(f.hex.q);d.writeInt(f.hex.r);d.writeInt(f.builderId);d.writeInt(f.remaining);}
        d.writeInt(missions.size());for(Mission m:missions){d.writeInt(m.taskId);d.writeInt(m.owner);d.writeInt(m.officerId);d.writeInt(m.sourceCity);d.writeInt(m.targetCity);d.writeInt(m.hex.q);d.writeInt(m.hex.r);d.writeBoolean(m.transport);d.writeInt(m.gold);d.writeInt(m.food);d.writeInt(m.troops);for(int j=0;j<4;j++)d.writeInt(m.equipment[j]);}
    }
    void writeUsage(DataOutputStream d)throws IOException{
        d.writeInt(facilities.size());for(Facility f:facilities){d.writeInt(f.id);d.writeInt(f.lastUseTurn);}
    }
    void readUsage(DataInputStream d)throws IOException{
        int count=bound(d.readInt(),0,6000);require(count==facilities.size(),"设施次数数量错误");Set<Integer> ids=new HashSet<>();
        for(int i=0;i<count;i++){Facility f=facility(d.readInt());require(f!=null&&ids.add(f.id),"设施次数引用错误");f.lastUseTurn=bound(d.readInt(),-1,w.turn);}
    }
    void read(DataInputStream d)throws IOException{
        nextFacilityId=d.readInt();nextMissionId=d.readInt();int n=bound(d.readInt(),0,6000);
        for(int i=0;i<n;i++)facilities.add(new Facility(d.readInt(),d.readInt(),Kind.values()[bound(d.readUnsignedByte(),0,Kind.values().length-1)],new Hex(d.readInt(),d.readInt()),d.readInt(),d.readInt()));
        n=bound(d.readInt(),0,10000);for(int i=0;i<n;i++){
            int id=d.readInt(),owner=d.readInt(),officer=d.readInt(),source=d.readInt(),target=d.readInt();Hex h=new Hex(d.readInt(),d.readInt());boolean cargo=d.readBoolean();int gold=d.readInt(),food=d.readInt(),troops=d.readInt();int[] eq=new int[4];for(int j=0;j<4;j++)eq[j]=d.readInt();
            missions.add(new Mission(id,owner,officer,source,target,h,cargo,gold,food,troops,eq));
        }
    }
    private static int bound(int n,int low,int high)throws IOException{require(n>=low&&n<=high,"战略层字段越界");return n;}
    private static void require(boolean ok,String reason)throws IOException{if(!ok)throw new IOException(reason);}
    void validate()throws IOException{
        bound(nextFacilityId,1,10000000);bound(nextMissionId,1,10000000);bound(facilities.size(),0,6000);bound(missions.size(),0,10000);
        Set<Integer> ids=new HashSet<>(),assigned=new HashSet<>();Set<Hex> occupied=new HashSet<>();
        for(Facility f:facilities){
            require(f.id>0&&f.id<nextFacilityId&&ids.add(f.id),"设施编号重复或无效");World.City c=w.city(f.cityId);
            require(c!=null&&f.kind!=null&&count(c.id)<=w.development.capacity(c.id),"设施城池或数量错误");
            require(f.hex!=null&&w.inside(f.hex)&&w.terrain[f.hex.q][f.hex.r]==World.Terrain.PLAIN&&occupied.add(f.hex)&&w.cityAt(f.hex)==null&&w.unitAt(f.hex)==null&&w.development.contains(c,f.hex),"设施位置冲突或无效");
            require(f.kind!=Kind.SHIPYARD||f.hex.neighbors().stream().anyMatch(w.army::water),"造船厂必须临水");
            bound(f.remaining,0,3);
            bound(f.hp,1,f.maxHp());
            bound(f.lastUseTurn,-1,w.turn);
            if(f.remaining==0)require(f.builderId==-1,"已建成设施仍占用武将");
            else{World.Officer o=w.officer(f.builderId);require(o!=null&&o.owner==c.owner&&o.cityId==c.id&&o.unitId==-1&&assigned.add(o.id),"建设武将引用错误");}
        }
        ids.clear();
        for(Mission m:missions){
            require(m.taskId>0&&m.taskId<nextMissionId&&ids.add(m.taskId),"任务编号重复或无效");bound(m.owner,0,w.factions.length-1);
            require(w.city(m.sourceCity)!=null&&w.city(m.targetCity)!=null,"在途城池引用错误");
            require(m.hex!=null&&w.inside(m.hex)&&(w.cost(m.hex,World.Weapon.SPEAR)>0||m.sea&&w.army.water(m.hex)),"任务位置无效");
            require(m.deputies!=null&&m.deputies.length<=2,"运输编队过大");
            for(int id:m.crew()){World.Officer o=w.officer(id);require(o!=null&&o.owner==m.owner&&o.cityId==-1&&o.unitId==-1&&assigned.add(o.id),"在途武将引用错误");}
            bound(m.movementTurn,-1,w.turn);bound(m.movementBudget,-1,1000);bound(m.movementSpent,0,1000);
            require(m.movementBudget<0?m.movementSpent==0:m.movementSpent<=m.movementBudget,"运输移动预算无效");
            require(m.waiting!=null&&m.waiting.length()<=600,"运输阻塞原因无效");for(int n:m.cargoShips)bound(n,0,100);
            bound(m.escortId,-1,9999999);require(m.transport||Arrays.stream(m.cargoShips).sum()==0,"返程不能携带舰船货物");
            bound(m.energy,0,150);bound(m.statusTurns,0,10);bound(m.burning,0,2);bound(m.burningOwner,-1,w.factions.length-1);bound(m.burningPower,1,2);
            if(m.transport&&!m.legacyOverlap)require(w.unitAt(m.hex)==m&&w.domestic.at(m.hex)==null&&w.war.at(m.hex)==null,"运输占格冲突");
            if(m.march!=null)require(m.march.tile!=null&&w.inside(m.march.tile)&&m.march.paused.length()<=300,"运输行军目标无效");
            bound(m.lastTick,-1,w.turn);bound(m.consumedFood,0,200000);require(!m.returning||!m.transport,"返程不能重复携货");
            require(payload(m.gold,m.food,m.troops,m.equipment),"运输货物越界");int sum=m.gold+m.food+m.troops+Arrays.stream(m.equipment).sum();
            require(m.transport?sum>0:sum==0,"任务种类与货物不符");
        }
    }
}

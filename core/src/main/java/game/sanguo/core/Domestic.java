package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Original engineering strategic rules, not SAN11/PK formulas or tactical transport. */
public final class Domestic {
    public static final int CITY_SLOTS=6, TRAVEL_SPEED=4;
    public enum Kind {
        MARKET("市场",1000,"每月金 +400"), FARM("农场",800,"每月粮 +2500"),
        BARRACKS("兵舍",1200,"每次征兵 +500"), SMITH("锻冶所",1200,"每次兵装生产 +500"),
        MINT("造币",1500,"相邻市场产金 +50%，不重复叠加"), GRANARY("谷仓",1500,"相邻农场产粮 +50%，不重复叠加"),
        STABLE("厩舍",1200,"骑兵兵装生产额外 +500"), BLACK_MARKET("黑市",500,"每月金 +200，不可合并");
        public final String label,effect;public final int cost;
        Kind(String label,int cost,String effect){this.label=label;this.cost=cost;this.effect=effect;}
    }
    public static final class Facility {
        public final int id,cityId;public final Kind kind;public final Hex hex;
        public int builderId,remaining,level=1,upgradeTo;
        Facility(int id,int city,Kind kind,Hex hex,int builder,int remaining){this.id=id;cityId=city;this.kind=kind;this.hex=hex;builderId=builder;this.remaining=remaining;}
    }
    public static final class Mission {
        public final int id,owner,officerId,sourceCity;public int targetCity;
        public Hex hex;public final boolean transport;public final int gold,food,troops;
        public final int[] equipment;
        Mission(int id,int owner,int officer,int source,int target,Hex hex,boolean transport,int gold,int food,int troops,int[] equipment){
            this.id=id;this.owner=owner;officerId=officer;sourceCity=source;targetCity=target;this.hex=hex;
            this.transport=transport;this.gold=gold;this.food=food;this.troops=troops;this.equipment=equipment.clone();
        }
    }
    public final List<Facility> facilities=new ArrayList<>();
    public final List<Mission> missions=new ArrayList<>();
    private final World w;int nextFacilityId=1,nextMissionId=1;
    Domestic(World w){this.w=w;}
    public Facility facility(int id){for(Facility f:facilities)if(f.id==id)return f;return null;}
    public Facility at(Hex h){for(Facility f:facilities)if(f.hex.equals(h))return f;return null;}
    public Mission mission(int id){for(Mission m:missions)if(m.id==id)return m;return null;}
    public boolean busy(int officer){
        if(officer<0)return false;
        for(Facility f:facilities)if(f.builderId==officer)return true;
        for(Mission m:missions)if(m.officerId==officer)return true;
        return false;
    }
    public String assignment(int officer){
        for(Facility f:facilities)if(f.builderId==officer)return "建设"+f.kind.label+" · 剩"+f.remaining+"旬";
        for(Mission m:missions)if(m.officerId==officer)return (m.transport?"运输":"调动")+"至"+w.city(m.targetCity).name+" · "+status(m);
        return "";
    }
    public int count(int city){int n=0;for(Facility f:facilities)if(f.cityId==city)n++;return n;}
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
    public int recruitAmount(int city){return 2000+this.yield(city,Kind.BARRACKS,500);}
    public int produceAmount(int city){return 2000+this.yield(city,Kind.SMITH,500);}
    public int produceAmount(int city,World.Weapon weapon){return produceAmount(city)+(weapon==World.Weapon.CAVALRY?this.yield(city,Kind.STABLE,500):0);}
    public static boolean mergeable(Kind kind){return kind==Kind.MARKET||kind==Kind.FARM||kind==Kind.BARRACKS||kind==Kind.SMITH||kind==Kind.STABLE;}
    public List<Facility> mergeCandidates(int target){
        Facility f=facility(target);List<Facility> result=new ArrayList<>();if(f==null||f.remaining>0||f.level>=3||!mergeable(f.kind))return result;
        for(Facility other:facilities)if(other.id!=f.id&&other.cityId==f.cityId&&other.kind==f.kind&&other.remaining==0&&other.level==1&&other.hex.distance(f.hex)==1)result.add(other);return result;
    }
    public World.Result merge(int target,int consumed,int officer){
        Facility f=facility(target),material=facility(consumed);if(f==null||material==null||!mergeCandidates(target).contains(material))return w.fail("需要相邻同类Lv1设施，目标等级须低于Lv3");
        World.City c=w.city(f.cityId);World.Officer o=w.officer(officer);String error=w.cityError(c,o,200);if(error!=null)return w.fail(error);
        w.spend(c,o,200);facilities.remove(material);f.upgradeTo=f.level+1;f.builderId=o.id;f.remaining=2;
        return w.success(f.kind.label+"开始吸收合并，2旬后达到Lv"+f.upgradeTo+"，原材料地块已腾空");
    }
    private boolean site(World.City city,Hex h){
        if(city==null||h==null||!w.inside(h)||city.hex.distance(h)<1||city.hex.distance(h)>2)return false;
        if(w.terrain[h.q][h.r]!=World.Terrain.PLAIN||w.cityAt(h)!=null||w.unitAt(h)!=null||at(h)!=null||w.war.at(h)!=null||w.war.fireAt(h)!=null)return false;
        for(World.City c:w.cities)if(c.hex.distance(h)==1){
            boolean exit=false;
            for(Hex n:c.hex.neighbors())if(!n.equals(h)&&w.cost(n,World.Weapon.SPEAR)>0&&w.cityAt(n)==null&&at(n)==null&&w.war.at(n)==null){exit=true;break;}
            if(!exit)return false;
        }
        return true;
    }
    public List<Hex> buildSites(int cityId){
        World.City c=w.city(cityId);List<Hex> result=new ArrayList<>();
        if(c==null||count(cityId)>=CITY_SLOTS)return result;
        for(int q=Math.max(0,c.hex.q-2);q<=Math.min(w.width-1,c.hex.q+2);q++)
            for(int r=Math.max(0,c.hex.r-2);r<=Math.min(w.height-1,c.hex.r+2);r++){Hex h=new Hex(q,r);if(site(c,h))result.add(h);}
        result.sort(Comparator.comparingInt((Hex h)->h.distance(c.hex)).thenComparingInt(h->h.q).thenComparingInt(h->h.r));
        return result;
    }
    public World.Result build(int cityId,int officerId,Kind kind,Hex h){
        if(kind==null)return w.fail("设施类型无效");
        World.City c=w.city(cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,kind.cost);
        if(error!=null)return w.fail(error);
        if(count(cityId)>=CITY_SLOTS)return w.fail("每城最多6处设施（含建设中）");
        if(!site(c,h))return w.fail("请选择城池两格内空闲平地，且不能封死城池出口");
        if(nextFacilityId>=10000000)return w.fail("设施编号已达上限");
        int turns=o.politics>=80?2:3;w.spend(c,o,kind.cost);
        facilities.add(new Facility(nextFacilityId++,c.id,kind,h,o.id,turns));
        return w.success(o.name+"开始建设"+kind.label+"，需要"+turns+"旬");
    }
    public World.Result cancelBuild(int id){
        Facility f=facility(id);
        if(w.gameOver()||f==null||w.city(f.cityId).owner!=w.active||f.remaining==0)return w.fail("没有可取消的己方建设");
        w.officer(f.builderId).acted=true;
        if(f.upgradeTo>0){f.upgradeTo=0;f.remaining=0;f.builderId=-1;return w.success("已取消合并，保留原等级；费用和已吸收设施不退还");}
        facilities.remove(f);return w.success("已取消"+f.kind.label+"建设，不退还费用");
    }
    public World.Result demolish(int id,int officerId){
        Facility f=facility(id);if(f==null||f.remaining!=0)return w.fail("请选择已完成设施");
        World.City c=w.city(f.cityId);World.Officer o=w.officer(officerId);String error=w.cityError(c,o,0);
        if(error!=null)return w.fail(error);
        w.spend(c,o,0);facilities.remove(f);return w.success(c.name+"拆除"+f.kind.label+"，不退还费用");
    }
    void captured(int city){
        for(Facility f:new ArrayList<>(facilities))if(f.cityId==city&&f.remaining>0){
            World.Officer o=w.officer(f.builderId);if(o!=null)o.acted=true;
            if(f.upgradeTo>0){f.remaining=0;f.builderId=-1;f.upgradeTo=0;}else facilities.remove(f);w.note("城池失守，"+f.kind.label+"建设中止");
        }
    }
    public World.Result transfer(int source,int target,int officer){return dispatch(source,target,officer,false,0,0,0,new int[4]);}
    public World.Result transport(int source,int target,int officer,int gold,int food,int troops,int[] equipment){return dispatch(source,target,officer,true,gold,food,troops,equipment);}
    private World.Result dispatch(int source,int target,int officer,boolean cargo,int gold,int food,int troops,int[] equipment){
        World.City c=w.city(source),d=w.city(target);World.Officer o=w.officer(officer);int fee=cargo?100:0;
        String error=w.cityError(c,o,fee);if(error!=null)return w.fail(error);
        if(d==null||d.owner!=w.active||d.id==c.id)return w.fail("请选择另一座己方城池");
        if(!payload(gold,food,troops,equipment))return w.fail("运输数量越界");
        if(cargo&&gold+food+troops+Arrays.stream(equipment).sum()==0)return w.fail("至少携带一种资源");
        if(c.gold-fee<gold||c.food<food||c.troops<troops)return w.fail("金、粮或兵力不足（运输另收金100）");
        for(int i=0;i<4;i++)if(c.equipment[i]<equipment[i])return w.fail("兵装库存不足");
        if(route(c.hex,d.hex,w.active)==null)return w.fail("没有可用陆路，暂不支持水运");
        if(nextMissionId>=10000000)return w.fail("任务编号已达上限");
        w.spend(c,o,fee);c.gold-=gold;c.food-=food;c.troops-=troops;for(int i=0;i<4;i++)c.equipment[i]-=equipment[i];
        w.strategy.releaseGovernor(o.id);o.cityId=-1;missions.add(new Mission(nextMissionId++,w.active,o.id,c.id,d.id,c.hex,cargo,gold,food,troops,equipment));
        return w.success(o.name+(cargo?"运送资源":"调动")+"前往"+d.name);
    }
    private static boolean payload(int gold,int food,int troops,int[] equipment){
        if(gold<0||gold>100000||food<0||food>200000||troops<0||troops>20000||equipment==null||equipment.length!=4)return false;
        for(int e:equipment)if(e<0||e>20000)return false;return true;
    }
    public World.Result redirect(int id,int target){
        Mission m=mission(id);World.City c=w.city(target);
        if(w.gameOver()||m==null||m.owner!=w.active||c==null||c.owner!=m.owner||target==m.targetCity)return w.fail("请选择本势力在途任务与新的己方目的地");
        if(w.actionPoints[w.active]<10)return w.fail("行动力不足10");
        if(route(m.hex,c.hex,m.owner)==null)return w.fail("没有可用陆路");
        w.actionPoints[w.active]-=10;m.targetCity=target;return w.success("任务已改道至"+c.name);
    }
    private static final class Step {final Hex h;final int cost;Step(Hex h,int c){this.h=h;cost=c;}}
    private int travelCost(Hex h,int owner){
        int cost=w.cost(h,World.Weapon.SPEAR);if(cost<0||at(h)!=null||w.war.at(h)!=null)return -1;
        World.City c=w.cityAt(h);return c!=null&&c.owner!=owner?-1:cost;
    }
    /** Strategic movement: avoids hostile cities/facilities but ignores tactical unit occupancy. */
    public List<Hex> route(Hex from,Hex to,int owner){
        if(from==null||to==null||!w.inside(from)||!w.inside(to))return null;
        Map<Hex,Integer> distance=new HashMap<>();Map<Hex,Hex> previous=new HashMap<>();
        PriorityQueue<Step> open=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.h.q).thenComparingInt(s->s.h.r));
        distance.put(from,0);open.add(new Step(from,0));
        while(!open.isEmpty()){
            Step s=open.remove();if(s.cost!=distance.get(s.h))continue;
            if(s.h.equals(to)){LinkedList<Hex> result=new LinkedList<>();Hex h=to;while(!h.equals(from)){result.addFirst(h);h=previous.get(h);}return result;}
            for(Hex h:s.h.neighbors()){
                int cost=travelCost(h,owner);if(cost<0)continue;int total=s.cost+cost;
                if(total<distance.getOrDefault(h,Integer.MAX_VALUE)){distance.put(h,total);previous.put(h,s.h);open.add(new Step(h,total));}
            }
        }
        return null;
    }
    public int eta(Mission m){
        World.City c=w.city(m.targetCity);if(c==null||c.owner!=m.owner)return -1;
        List<Hex> path=route(m.hex,c.hex,m.owner);if(path==null)return -1;
        int turns=0,budget=0;for(Hex h:path){int cost=travelCost(h,m.owner);if(budget<cost){turns++;budget=TRAVEL_SPEED;}budget-=cost;}return turns;
    }
    public String status(Mission m){int turns=eta(m);return turns<0?"道路受阻 / 等待改道":turns==0?"已到达，等待结算或库存空间":"预计"+turns+"旬";}
    private boolean fits(Mission m,World.City c){
        if(c.gold>1000000-m.gold||c.food>1000000-m.food||c.troops>100000-m.troops)return false;
        for(int i=0;i<4;i++)if(c.equipment[i]>100000-m.equipment[i])return false;return true;
    }
    void tick(){
        cleanupDefeated();
        for(Facility f:facilities)if(f.remaining>0){
            f.remaining--;if(f.remaining==0){World.Officer o=w.officer(f.builderId);o.acted=true;f.builderId=-1;if(f.upgradeTo>0){f.level=f.upgradeTo;f.upgradeTo=0;}w.note(w.city(f.cityId).name+"的"+f.kind.label+"建成 · Lv"+f.level);}
        }
        for(Mission m:new ArrayList<>(missions)){
            World.City c=w.city(m.targetCity);
            if(c.owner!=m.owner){
                World.City best=null;int bestCost=Integer.MAX_VALUE;
                for(World.City d:w.cities)if(d.owner==m.owner){List<Hex> path=route(m.hex,d.hex,m.owner);if(path==null)continue;int cost=0;for(Hex h:path)cost+=travelCost(h,m.owner);
                    if(cost<bestCost||(cost==bestCost&&(best==null||d.id<best.id))){best=d;bestCost=cost;}}
                if(best==null)continue;m.targetCity=best.id;c=best;w.note(w.officer(m.officerId).name+"因目的地失守改道至"+c.name);
            }
            List<Hex> path=route(m.hex,c.hex,m.owner);if(path==null)continue;
            int budget=TRAVEL_SPEED;
            for(Hex h:path){int cost=travelCost(h,m.owner);if(cost>budget)break;budget-=cost;m.hex=h;}
            if(m.hex.equals(c.hex)&&fits(m,c)){
                c.gold+=m.gold;c.food+=m.food;c.troops+=m.troops;for(int i=0;i<4;i++)c.equipment[i]+=m.equipment[i];
                World.Officer o=w.officer(m.officerId);o.cityId=c.id;o.acted=true;missions.remove(m);w.note(o.name+"抵达"+c.name+(m.transport?"，资源已入库":""));
            }
        }
    }
    void cleanupDefeated(){for(Mission m:new ArrayList<>(missions))if(!w.alive(m.owner)){missions.remove(m);w.officer(m.officerId).acted=true;w.note(w.faction(m.owner)+"覆灭，在途任务终止");}}
    void runAi(){
        for(World.City c:w.cities)if(c.owner==w.active&&c.gold>=2500){
            List<World.Officer> idle=w.idle(c);if(idle.isEmpty())continue;
            Kind kind=null;boolean market=false,farm=false;
            for(Facility f:facilities)if(f.cityId==c.id){market|=f.kind==Kind.MARKET;farm|=f.kind==Kind.FARM;}
            if(!market)kind=Kind.MARKET;else if(!farm)kind=Kind.FARM;
            List<Hex> sites=buildSites(c.id);if(kind!=null&&!sites.isEmpty())build(c.id,idle.get(0).id,kind,sites.get(0));
        }
    }
    void write(DataOutputStream d)throws IOException{
        d.writeInt(nextFacilityId);d.writeInt(nextMissionId);d.writeInt(facilities.size());
        for(Facility f:facilities){d.writeInt(f.id);d.writeInt(f.cityId);d.writeByte(f.kind.ordinal());d.writeInt(f.hex.q);d.writeInt(f.hex.r);d.writeInt(f.builderId);d.writeInt(f.remaining);}
        d.writeInt(missions.size());for(Mission m:missions){d.writeInt(m.id);d.writeInt(m.owner);d.writeInt(m.officerId);d.writeInt(m.sourceCity);d.writeInt(m.targetCity);d.writeInt(m.hex.q);d.writeInt(m.hex.r);d.writeBoolean(m.transport);d.writeInt(m.gold);d.writeInt(m.food);d.writeInt(m.troops);for(int e:m.equipment)d.writeInt(e);}
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
            require(c!=null&&f.kind!=null&&count(c.id)<=CITY_SLOTS,"设施城池或数量错误");
            require(f.hex!=null&&w.inside(f.hex)&&w.terrain[f.hex.q][f.hex.r]==World.Terrain.PLAIN&&occupied.add(f.hex)&&w.cityAt(f.hex)==null&&w.unitAt(f.hex)==null&&f.hex.distance(c.hex)>=1&&f.hex.distance(c.hex)<=2,"设施位置冲突或无效");
            bound(f.remaining,0,3);
            if(f.remaining==0)require(f.builderId==-1,"已建成设施仍占用武将");
            else{World.Officer o=w.officer(f.builderId);require(o!=null&&o.owner==c.owner&&o.cityId==c.id&&o.unitId==-1&&assigned.add(o.id),"建设武将引用错误");}
        }
        ids.clear();
        for(Mission m:missions){
            require(m.id>0&&m.id<nextMissionId&&ids.add(m.id),"任务编号重复或无效");bound(m.owner,0,w.factions.length-1);
            require(w.city(m.sourceCity)!=null&&w.city(m.targetCity)!=null,"在途城池引用错误");
            require(m.hex!=null&&w.inside(m.hex)&&w.cost(m.hex,World.Weapon.SPEAR)>0,"任务位置无效");
            World.Officer o=w.officer(m.officerId);require(o!=null&&o.owner==m.owner&&o.cityId==-1&&o.unitId==-1&&assigned.add(o.id),"在途武将引用错误");
            require(payload(m.gold,m.food,m.troops,m.equipment),"运输货物越界");int sum=m.gold+m.food+m.troops+Arrays.stream(m.equipment).sum();
            require(m.transport?sum>0:sum==0,"任务种类与货物不符");
        }
    }
}

package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Persistent objectives. Paths and final actions use the authoritative movement/combat rules. */
public final class MarchOrders {
    // Keep historical ordinals: saves and transport missions share this identity record.
    public enum Kind { TILE, CITY, UNIT, STRUCTURE, FACILITY }
    public enum Intent { LEGACY, MOVE, ATTACK, GARRISON, REPAIR, APPROACH }
    public static final class Order {
        public final Kind kind;
        public final Hex tile;
        public final int targetId, owner;
        public final Intent intent;
        public String paused="";
        Order(Kind kind,Hex tile,int targetId,int owner){this(kind,tile,targetId,owner,Intent.LEGACY);}
        Order(Kind kind,Hex tile,int targetId,int owner,Intent intent){this.kind=kind;this.tile=tile;this.targetId=targetId;this.owner=owner;this.intent=intent;}
    }
    public static final class Plan {
        public final int unitId,cost,stepsNow,estimatedTurns;
        public final List<Hex> path;
        public final Hex target;
        public final String label,error,actionLabel,completion;
        public final Order order;
        private final byte[] snapshot;
        Plan(int unitId,Order order,Hex target,String label,List<Hex> path,int cost,int stepsNow,int turns,String error,byte[] snapshot){
            this.unitId=unitId;this.order=order;this.target=target;this.label=label;this.path=Collections.unmodifiableList(new ArrayList<>(path));
            this.cost=cost;this.stepsNow=stepsNow;estimatedTurns=turns;this.error=error;this.snapshot=snapshot;
            Intent intent=order==null?Intent.MOVE:order.intent;actionLabel=actionLabel(intent);completion=completion(intent);
        }
        public boolean valid(){return error==null;}
    }
    private static final class Step {final Hex h;final int cost,priority;Step(Hex h,int cost,int heuristic){this.h=h;this.cost=cost;this.priority=cost+heuristic;}}
    private final World w;
    private boolean executing;
    MarchOrders(World w){this.w=w;}
    private String error(World.Unit u){
        if(w.commandsBlocked())return "请先完成当前对局或君主继承";
        if(w.gameOver())return "本局已结束";
        if(u==null||w.unit(u.id)!=u||u.owner!=w.active)return "请选择当前势力的部队";
        if(u instanceof Domestic.Mission&&!w.domestic.commandable((Domestic.Mission)u))return "该运输队由委任军团指挥";
        if(!w.districts.directUnit(u.id))return "该部队由委任军团指挥";
        War.Structure project=w.fieldworks.project(u.id);
        if(project!=null&&(u.march==null||u.march.intent!=Intent.REPAIR||u.march.targetId!=project.id))return "请先中止部队施工";
        return null;
    }
    public Hex target(Order o){
        if(o==null)return null;
        if(o.kind==Kind.UNIT){World.Unit u=w.unit(o.targetId);return u!=null&&u.owner==o.owner?u.hex:null;}
        if(o.kind==Kind.CITY){World.City c=w.city(o.targetId);return c!=null&&c.owner==o.owner?c.hex:null;}
        if(o.kind==Kind.STRUCTURE){War.Structure s=w.war.at(o.tile);return s!=null&&(o.targetId<0||s.id==o.targetId)&&s.owner==o.owner?o.tile:null;}
        if(o.kind==Kind.FACILITY){Domestic.Facility f=w.domestic.facility(o.targetId);return f!=null&&f.hex.equals(o.tile)&&w.city(f.cityId).owner==o.owner?f.hex:null;}
        return o.tile;
    }
    public String label(Order o){
        if(o==null)return "未设置目标";
        if(o.kind==Kind.CITY&&w.city(o.targetId)!=null)return w.city(o.targetId).name;
        if(o.kind==Kind.UNIT&&w.unit(o.targetId)!=null)return w.officer(w.unit(o.targetId).officerId).name+"部队";
        if(o.kind==Kind.STRUCTURE&&w.war.at(o.tile)!=null)return w.war.at(o.tile).kind.label;
        if(o.kind==Kind.FACILITY&&w.domestic.facility(o.targetId)!=null)return w.domestic.facility(o.targetId).kind.label;
        return "地块 · "+MapCoordinates.display(w,o.tile);
    }
    private static String actionLabel(Intent i){switch(i){case ATTACK:return "自动攻击";case GARRISON:return "自动进驻";case REPAIR:return "自动修理";case APPROACH:return "接近目标";case LEGACY:return "原行军指令";default:return "自动行军";}}
    private static String completion(Intent i){switch(i){case ATTACK:return "进入有效射程后每旬攻击；目标消灭结束，据点攻占后自动进驻";case GARRISON:return "到达后自动进驻；容量不足则保留全部兵粮并等待";case REPAIR:return "到达后每旬补修，修满后停止";case APPROACH:return "接近后待命，不攻击、不自动宣战";case LEGACY:return "保留旧指令行为；重新选目标可启用连续攻击或修理";default:return "到达指定地块后停止";}}
    public String describe(World.Unit u){return u==null||u.march==null?"未设置任务":actionLabel(u.march.intent)+" → "+label(u.march)+"\n"+(u.march.paused.isEmpty()?completion(u.march.intent):"等待："+u.march.paused);}
    public Plan preview(int id,Hex tile){return preview(w.unit(id),tile);}
    public Plan previewMove(int id,Hex tile){return plan(w.unit(id),tile==null?null:new Order(Kind.TILE,tile,-1,-1,Intent.MOVE),true,false);}
    public Plan previewCity(int id,int city){
        World.Unit u=w.unit(id);World.City c=w.city(city);
        Intent intent=u!=null&&c!=null&&u.owner==c.owner?Intent.GARRISON:u!=null&&c!=null&&w.campaign.hostile(u.owner,c.owner)?Intent.ATTACK:Intent.APPROACH;
        return plan(u,c==null?null:new Order(Kind.CITY,c.hex,c.id,c.owner,intent),true,false);
    }
    public Plan preview(World.Unit u,Hex tile){
        Order o=null;
        if(tile!=null&&w.inside(tile)){
            World.City c=w.cityAt(tile);World.Unit other=w.unitAt(tile);War.Structure s=w.war.at(tile);Domestic.Facility f=w.domestic.at(tile);
            Kind kind=other!=null&&other!=u?Kind.UNIT:c!=null?Kind.CITY:other!=null?Kind.UNIT:s!=null?Kind.STRUCTURE:f!=null?Kind.FACILITY:Kind.TILE;
            int id=kind==Kind.UNIT?other.id:c!=null?c.id:s!=null?s.id:f!=null?f.id:-1;
            int owner=kind==Kind.UNIT?other.owner:c!=null?c.owner:s!=null?s.owner:f!=null?w.city(f.cityId).owner:-1;
            Intent intent=kind==Kind.TILE?Intent.MOVE:u!=null&&owner==u.owner&&kind==Kind.CITY?Intent.GARRISON:
                u!=null&&owner==u.owner&&kind==Kind.STRUCTURE&&!(u instanceof Domestic.Mission)?Intent.REPAIR:
                u!=null&&w.campaign.hostile(u.owner,owner)&&!(u instanceof Domestic.Mission)?Intent.ATTACK:Intent.APPROACH;
            o=new Order(kind,tile,id,owner,intent);
        }
        return plan(u,o,true,false);
    }
    private Order convoyOrder(Domestic.Mission m){World.City c=w.city(m.targetCity);return c==null?null:new Order(Kind.CITY,c.hex,c.id,c.owner,Intent.GARRISON);}
    Plan convoyRoute(Domestic.Mission m){return plan(m,convoyOrder(m),false,false);}
    Plan convoyQueueRoute(Domestic.Mission m){return plan(m,convoyOrder(m),false,true);}
    public Plan current(World.Unit u){return plan(u,u==null?null:u.march,false,false);}
    public String tileError(World.Unit u,Hex tile){
        if(tile==null||!w.inside(tile))return "目标在地图范围外";
        if(u==null)return "请选择己方部队";
        World.City site=w.cityAt(tile);if(!SiteFootprint.transit(site,u.owner))return "该据点不能作为穿行地块；请选择进驻或攻击据点";
        Domestic.Facility f=w.domestic.at(tile);if(f!=null)return f.kind.label+"占据目标格，请选择旁边空地";
        War.Fire fire=w.war.fireAt(tile);if(fire!=null)return "目标格有火场（剩"+fire.remaining+"旬），自动行军避火";
        // This is a destination check, not an edge: a remote river tile may be reachable via a port.
        if(w.army.water(tile))return u instanceof Domestic.Mission&&!((Domestic.Mission)u).sea?"陆路运输队不能下水，请使用水陆运输":null;
        if(w.fieldworks.landCost(tile,u.weapon,u.owner)<1)return "当前兵种无法进入该地形，可能需要难所行军技巧";
        return null;
    }
    private Order normalized(World.Unit u,Order o){
        if(u!=null&&o!=null&&o.kind==Kind.CITY&&o.intent==Intent.ATTACK){World.City c=w.city(o.targetId);if(c!=null&&c.owner==u.owner)return new Order(o.kind,c.hex,c.id,c.owner,Intent.GARRISON);}
        return o;
    }
    private Intent effective(World.Unit u,Order o){return o.intent==Intent.LEGACY?(o.kind==Kind.TILE?Intent.MOVE:o.kind==Kind.CITY&&o.owner==u.owner?Intent.GARRISON:Intent.APPROACH):o.intent;}
    private World.Unit probe(World.Unit u){World.Unit p=new World.Unit(u.id,u.owner,u.officerId,u.weapon,u.hex,u.troops,u.food);p.deputies=u.deputies;p.ship=u.ship;p.energy=u.energy;p.gold=u.gold;return p;}
    /** -2 unavailable, -1 normal attack, otherwise Army.Tactic ordinal. No world mutation. */
    private int attackChoice(World.Unit p,Order o){
        Hex h=target(o);if(h==null)return -2;String error;
        switch(o.kind){
            case UNIT:error=w.war.attackPositionError(p,w.unit(o.targetId));break;
            case CITY:error=w.siegePositionError(p,w.city(o.targetId));break;
            case STRUCTURE:error=w.war.structureAttackPositionError(p,h);break;
            case FACILITY:error=w.war.facilityAttackPositionError(p,h);break;
            default:return -2;
        }
        if(error==null)return -1;
        for(Army.Tactic tactic:w.army.tactics(p))if((o.kind==Kind.CITY?w.army.tacticCityPositionError(p,w.city(o.targetId),tactic):w.army.tacticPositionError(p,h,tactic))==null)return tactic.ordinal();
        return -2;
    }
    private Set<Hex> goals(World.Unit u,Order o,Hex destination){
        Set<Hex> result=new HashSet<>();Intent intent=effective(u,o);
        if(intent==Intent.MOVE){result.add(destination);return result;}
        if(intent==Intent.GARRISON){result.addAll(SiteFootprint.entryGoals(w,u,w.city(o.targetId)));return result;}
        if(intent!=Intent.ATTACK){
            if(o.kind==Kind.CITY)result.addAll(SiteFootprint.edge(w.city(o.targetId)));
            else result.addAll(destination.neighbors());
            return result;
        }
        World.Unit p=probe(u);p.energy=100; // Route to a firing position; spent energy is recovered by waiting, not invented.
        int range=Math.max(6,Math.max(u.weapon.range,u.ship.range)+2);
        for(int q=-range;q<=range;q++)for(int r=Math.max(-range,-q-range);r<=Math.min(range,-q+range);r++){
            Hex h=new Hex(destination.q+q,destination.r+r);if(h.equals(destination)||!w.inside(h))continue;
            p.hex=h;if(attackChoice(p,o)!=-2)result.add(h);
        }
        return result;
    }
    private int budget(World.Unit u,Hex h,int[] budgets){int mode=w.army.water(h)?1:0;if(budgets[mode]<0)budgets[mode]=w.war.movementAt(u,h);return budgets[mode];}
    private Plan plan(World.Unit u,Order original,boolean snapshot,boolean queued){
        Order o=normalized(u,original);int[] budgets={-1,-1};
        String problem=snapshot?error(u):u==null?"请选择部队":null;Hex destination=target(o);List<Hex> path=new ArrayList<>();int cost=0,stepsNow=0,turns=0;
        if(problem==null&&(o==null||destination==null||!w.inside(destination)))problem="目标已消失或归属改变，请重新选择";
        if(problem==null&&o.kind==Kind.UNIT&&o.targetId==u.id)problem="请选择其他目标";
        if(problem==null&&o.kind==Kind.TILE)problem=tileError(u,destination);
        if(problem==null&&o.intent==Intent.ATTACK&&!w.campaign.hostile(u.owner,o.owner))problem="目标已停战或结盟，自动攻击停止";
        if(problem==null&&effective(u,o)==Intent.GARRISON&&o.owner!=u.owner)problem="只能进驻己方据点";
        if(problem==null&&o.intent==Intent.REPAIR){War.Structure s=w.war.at(o.tile);if(s.hp>=s.kind.hp)problem="设施已完好，无需修理";else if(s.builder>=0&&s.builder!=u.id)problem="已有其他部队正在修理该设施";}
        if(problem==null){
            Set<Hex> goals=goals(u,o,destination),blocked=new HashSet<>();
            // Site traversal is checked by Army.moveCost against the shared footprint index.
            for(World.Unit other:w.fieldUnits())if(other.id!=u.id&&(!queued||other.owner!=u.owner))blocked.add(other.hex);
            for(Domestic.Facility f:w.domestic.facilities)blocked.add(f.hex);
            for(War.Structure s:w.war.structures())blocked.add(s.hex);
            for(War.Fire f:w.war.fires())blocked.add(f.hex);
            Map<Hex,Integer> distance=new HashMap<>();Map<Hex,Hex> previous=new HashMap<>();
            // Consistent lower bound: one paid step per axial distance outside the goal radius.
            // No full-map scan, and read-only per-query movement costs cannot become stale.
            final int radius=goals.stream().mapToInt(h->h.distance(destination)).max().orElse(0);
            Army.MovementCosts movementCosts=w.army.movementCosts(u);
            PriorityQueue<Step> queue=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.priority).thenComparingInt(s->s.cost).thenComparingInt(s->s.h.q).thenComparingInt(s->s.h.r));
            distance.put(u.hex,0);queue.add(new Step(u.hex,0,Math.max(0,u.hex.distance(destination)-radius)));Hex finish=null;
            while(!queue.isEmpty()&&!goals.isEmpty()){
                Step s=queue.remove();if(s.cost!=distance.get(s.h))continue;
                if(goals.contains(s.h)){finish=s.h;cost=s.cost;break;}
                for(Hex next:s.h.neighbors()){
                    int step=movementCosts.cost(s.h,next);if(step<1||blocked.contains(next))continue;
                    int available=budget(u,s.h,budgets);if(s.h.equals(u.hex))available=Math.max(available,w.orders.remaining(u));
                    if(step>available)continue;
                    int total=s.cost+step;if(total<distance.getOrDefault(next,Integer.MAX_VALUE)){distance.put(next,total);previous.put(next,s.h);queue.add(new Step(next,total,Math.max(0,next.distance(destination)-radius)));}
                }
            }
            if(finish==null)problem=goals.isEmpty()?"当前兵种或适性不能攻击该目标（包括森林射击限制）":"没有可达路线：检查占格、火场、地形；上下河必须经过己方港口";
            else{
                LinkedList<Hex> route=new LinkedList<>();for(Hex h=finish;h!=null;h=previous.get(h))route.addFirst(h);path.addAll(route);
                int available=w.orders.remaining(u),spent=0;boolean now=true;
                for(int i=1;i<path.size();i++){
                    int step=w.army.moveCost(u,path.get(i-1),path.get(i));
                    if(spent+step>available){turns++;available=budget(u,path.get(i-1),budgets);spent=0;now=false;}
                    spent+=step;if(w.advancedBattle.zone(u,path.get(i)))spent=available;
                    if(now)stepsNow=i;
                }
            }
        }
        byte[] state=null;if(problem==null&&snapshot)try{state=SaveCodec.encode(w);}catch(IOException e){problem="局面无法保存："+e.getMessage();}
        return new Plan(u==null?-1:u.id,o,destination,label(o),path,cost,stepsNow,turns,problem,state);
    }
    public World.Result execute(Plan plan){w.reports.prepare();
        if(plan==null||!plan.valid()||plan.snapshot==null)return w.fail(plan==null?"请先选择目标":plan.error==null?"请重新预览路线":plan.error);
        try{if(!Arrays.equals(plan.snapshot,SaveCodec.encode(w)))return w.fail("局面已变化，请重新预览路线");}catch(IOException e){return w.fail("局面校验失败");}
        World.Unit u=w.unit(plan.unitId);String problem=error(u);if(problem!=null)return w.fail(problem);
        if(u instanceof Domestic.Mission){Domestic.Mission m=(Domestic.Mission)u;
            if(plan.order.kind==Kind.CITY&&plan.order.owner==u.owner){String permission=w.districts.dispatchError(m.sourceCity,plan.order.targetId,true);if(permission!=null)return w.fail(permission);m.targetCity=plan.order.targetId;m.stopped=false;}else m.stopped=true;
        }
        releaseBuilder(u);u.march=new Order(plan.order.kind,plan.order.tile,plan.order.targetId,plan.order.owner,plan.order.intent);
        String name=w.officer(u.officerId).name;advance(u);
        return w.success(name+" · "+(w.unit(u.id)==null?(u.troops<=0&&!(u instanceof Domestic.Mission)?"部队已被击破，任务终止":"已进驻据点 / 完成入库"):u.march==null?"目标任务已完成":describe(u)));
    }
    private void releaseBuilder(World.Unit u){War.Structure s=w.fieldworks.project(u.id);if(s!=null&&u.march!=null&&u.march.intent==Intent.REPAIR&&s.id==u.march.targetId)s.builder=-1;}
    /** Called only after a manual command passes validation; never refunds action or resources. */
    void supersede(World.Unit u){if(!executing&&u!=null){releaseBuilder(u);u.march=null;}}
    public World.Result stop(int id){w.reports.prepare();
        World.Unit u=w.unit(id);if(u==null||u.owner!=w.active||w.commandsBlocked())return w.fail("当前不能变更任务");
        if(u instanceof Domestic.Mission&&!w.domestic.commandable((Domestic.Mission)u)||!w.districts.directUnit(u.id))return w.fail("该部队由委任军团指挥");
        if(u.march==null&&!(u instanceof Domestic.Mission))return w.fail("部队没有自动任务");
        releaseBuilder(u);u.march=null;if(u instanceof Domestic.Mission)((Domestic.Mission)u).stopped=true;
        return w.success("自动任务已停止，保留兵粮与已完成工作，不返还本旬行动");
    }
    void advanceAll(){if(w.gameOver())return;for(World.Unit u:new ArrayList<>(w.units))if(u.owner==w.active&&u.march!=null)advance(u);}
    private void pause(World.Unit u,String reason){if(u.march==null)return;if(!reason.equals(u.march.paused))w.note(w.officer(u.officerId).name+"任务等待："+reason);u.march.paused=reason;}
    private void finish(World.Unit u,String message){releaseBuilder(u);u.march=null;w.note(w.officer(u.officerId).name+" · "+message);}
    void advance(World.Unit u){boolean prior=executing;executing=true;try{advanceObjective(u);}finally{executing=prior;}}
    private void advanceObjective(World.Unit u){
        if(u==null||w.unit(u.id)!=u||u.march==null)return;
        Order original=u.march;Order o=normalized(u,original);u.march=o;
        if(target(o)==null){finish(u,"原目标已消失或归属改变，任务结束");return;}
        if(o.intent==Intent.ATTACK&&!w.campaign.hostile(u.owner,o.owner)){finish(u,"目标已停战或结盟，停止攻击");return;}
        if(o.intent==Intent.REPAIR&&w.war.at(o.tile).hp>=w.war.at(o.tile).kind.hp){finish(u,"设施已修复完毕");return;}
        if(u.status!=War.Status.NORMAL){pause(u,"异常状态，恢复后继续");return;}
        if(u.acted){pause(u,"本旬已行动，下旬继续");return;}
        String permission=error(u);if(permission!=null){pause(u,permission);return;}
        Plan route=plan(u,o,false,false);if(!route.valid()){pause(u,route.error);return;}
        if(route.stepsNow>0){
            World.Result moved=w.orders.executeImmediateRoute(u.id,route.path.subList(0,route.stepsNow+1));
            if(!moved.ok){pause(u,moved.message);return;}
            if(w.unit(u.id)!=u)return;
        }
        if(u.status!=War.Status.NORMAL||u.acted){pause(u,"途中受异常状态影响，恢复后继续");return;}
        o.paused="";
        if(!u.hex.equals(route.path.get(route.path.size()-1)))return;
        Intent intent=effective(u,o);
        if(intent==Intent.GARRISON){
            World.Result r=w.enter(u.id,o.targetId);if(!r.ok)pause(u,r.message);else u.march=null;return;
        }
        if(intent==Intent.REPAIR){
            World.Result r=w.fieldworks.repair(u.id,o.targetId);if(!r.ok){pause(u,r.message);return;}
            if(w.war.at(o.tile).hp>=w.war.at(o.tile).kind.hp)finish(u,"设施已修复完毕");return;
        }
        if(intent!=Intent.ATTACK){finish(u,intent==Intent.MOVE?"已到达指定地块":"已接近目标，部队待命");return;}
        int choice=attackChoice(u,o);
        if(choice==-2){
            World.Unit charged=probe(u);charged.energy=100;int future=attackChoice(charged,o);
            if(future>=0&&u.energy<Army.Tactic.values()[future].energy){World.Result wait=w.war.waitUnit(u.id);pause(u,wait.ok?"气力不足，自动待命恢复后继续攻击":wait.message);}
            else pause(u,"当前攻击条件不满足，下旬重新评估");
            return;
        }
        World.Result result;
        if(choice>=0)result=o.kind==Kind.CITY?w.army.tacticCity(u.id,o.targetId,Army.Tactic.values()[choice]):w.army.tactic(u.id,target(o),Army.Tactic.values()[choice]);
        else switch(o.kind){case UNIT:result=w.attack(u.id,o.targetId);break;case CITY:result=w.siege(u.id,o.targetId);break;case STRUCTURE:result=w.war.attackStructure(u.id,o.tile);break;case FACILITY:result=w.war.attackFacility(u.id,o.tile);break;default:result=w.fail("无效攻击目标");}
        if(w.unit(u.id)!=u)return; // Counterattacks may destroy the marching unit.
        if(!result.ok){pause(u,result.message);return;}
        if(o.kind==Kind.CITY&&w.city(o.targetId).owner==u.owner){
            World.City c=w.city(o.targetId);u.march=new Order(Kind.CITY,c.hex,c.id,c.owner,Intent.GARRISON);
            if(w.army.canEnterSite(u,u.hex,c)){World.Result entered=w.enterAfterCapture(u,c);if(entered.ok)u.march=null;else pause(u,entered.message);}
            else pause(u,"据点已攻占，下旬靠近并进驻");
        }else if(target(o)==null)finish(u,"目标已消灭，自动攻击完成");
        else pause(u,"已攻击，下旬继续");
    }
    void write(DataOutputStream d)throws IOException{
        int count=0;for(World.Unit u:w.units)if(u.march!=null)count++;d.writeInt(count);
        for(World.Unit u:w.units)if(u.march!=null){Order o=u.march;d.writeInt(u.id);d.writeByte(o.kind.ordinal());d.writeInt(o.tile.q);d.writeInt(o.tile.r);d.writeInt(o.targetId);d.writeInt(o.owner);d.writeUTF(o.paused);}
    }
    void read(DataInputStream d)throws IOException{
        int count=d.readInt();if(count<0||count>w.units.size())throw new IOException("行军指令数量错误");
        for(int i=0;i<count;i++){World.Unit u=w.unit(d.readInt());int kind=d.readUnsignedByte();Hex h=new Hex(d.readInt(),d.readInt());int id=d.readInt(),owner=d.readInt();String paused=d.readUTF();
            if(u==null||u.march!=null||kind>=Kind.values().length)throw new IOException("行军指令引用错误");u.march=new Order(Kind.values()[kind],h,id,owner);u.march.paused=paused;}
    }
    /** v27 appended section: old v26 objectives remain approach-only, never silently become attacks. */
    void writeIntents(DataOutputStream d)throws IOException{
        List<World.Unit> units=new ArrayList<>();for(World.Unit u:w.fieldUnits())if(u.march!=null)units.add(u);
        d.writeInt(units.size());for(World.Unit u:units){d.writeInt(u.id);d.writeByte(u.march.intent.ordinal());}
    }
    void readIntents(DataInputStream d)throws IOException{
        int expected=0;for(World.Unit u:w.fieldUnits())if(u.march!=null)expected++;
        int count=d.readInt();if(count!=expected)throw new IOException("自动任务意图数量错误");Set<Integer> ids=new HashSet<>();
        for(int i=0;i<count;i++){int id=d.readInt(),intent=d.readUnsignedByte();World.Unit u=w.unit(id);
            if(u==null||u.march==null||!ids.add(id)||intent>=Intent.values().length)throw new IOException("自动任务意图引用错误");
            Order o=u.march;u.march=new Order(o.kind,o.tile,o.targetId,o.owner,Intent.values()[intent]);u.march.paused=o.paused;}
    }
    void validate()throws IOException{
        for(World.Unit u:w.fieldUnits())if(u.march!=null){Order o=u.march;
            if(o.kind==null||o.intent==null||o.tile==null||!w.inside(o.tile)||o.paused==null||o.paused.length()>300||o.owner< -1||o.owner>=w.factions.length||o.targetId< -1||o.targetId>=20000000)throw new IOException("行军目标无效");
            if((o.kind==Kind.CITY||o.kind==Kind.UNIT)&&o.targetId<0||o.kind==Kind.UNIT&&o.targetId==u.id)throw new IOException("行军目标引用无效");
            if(o.intent==Intent.MOVE&&o.kind!=Kind.TILE||o.intent==Intent.GARRISON&&o.kind!=Kind.CITY||o.intent==Intent.REPAIR&&(o.kind!=Kind.STRUCTURE||o.targetId<0)||o.intent==Intent.ATTACK&&o.kind==Kind.TILE||u instanceof Domestic.Mission&&(o.intent==Intent.ATTACK||o.intent==Intent.REPAIR))throw new IOException("自动任务类型不匹配");
        }
    }
}

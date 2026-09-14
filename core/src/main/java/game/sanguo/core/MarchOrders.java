package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Persistent approach orders. Movement shares the tactical budget; combat always needs a new command. */
public final class MarchOrders {
    public enum Kind { TILE, CITY, UNIT, STRUCTURE }
    public static final class Order {
        public final Kind kind;
        public final Hex tile;
        public final int targetId, owner;
        public String paused="";
        Order(Kind kind,Hex tile,int targetId,int owner){this.kind=kind;this.tile=tile;this.targetId=targetId;this.owner=owner;}
    }
    public static final class Plan {
        public final int unitId,cost,stepsNow,estimatedTurns;
        public final List<Hex> path;
        public final Hex target;
        public final String label,error;
        private final Order order;
        private final byte[] snapshot;
        Plan(int unitId,Order order,Hex target,String label,List<Hex> path,int cost,int stepsNow,int turns,String error,byte[] snapshot){
            this.unitId=unitId;this.order=order;this.target=target;this.label=label;this.path=Collections.unmodifiableList(new ArrayList<>(path));
            this.cost=cost;this.stepsNow=stepsNow;estimatedTurns=turns;this.error=error;this.snapshot=snapshot;
        }
        public boolean valid(){return error==null;}
    }
    private static final class Step {
        final Hex h;final int cost;
        Step(Hex h,int cost){this.h=h;this.cost=cost;}
    }
    private final World w;
    MarchOrders(World w){this.w=w;}
    private String error(World.Unit u){
        if(w.contests.busy())return "请先完成当前对局";
        if(w.gameOver())return "本局已结束";
        if(u==null||u.owner!=w.active)return "请选择当前势力的部队";
        if(w.fieldworks.project(u.id)!=null)return "请先中止部队施工";
        return null;
    }
    public Hex target(Order o){
        if(o==null)return null;
        if(o.kind==Kind.UNIT){World.Unit unit=w.unit(o.targetId);return unit!=null&&unit.owner==o.owner?unit.hex:null;}
        if(o.kind==Kind.CITY){World.City city=w.city(o.targetId);return city!=null&&city.owner==o.owner?city.hex:null;}
        if(o.kind==Kind.STRUCTURE){War.Structure s=w.war.at(o.tile);return s!=null&&s.owner==o.owner?o.tile:null;}
        return o.tile;
    }
    public String label(Order o){
        if(o==null)return "未设置目标";
        if(o.kind==Kind.CITY&&w.city(o.targetId)!=null)return w.city(o.targetId).name;
        if(o.kind==Kind.UNIT&&w.unit(o.targetId)!=null)return w.officer(w.unit(o.targetId).officerId).name+"部队";
        if(o.kind==Kind.STRUCTURE&&w.war.at(o.tile)!=null)return w.war.at(o.tile).kind.label;
        return "地块 "+o.tile;
    }
    public Plan preview(int id,Hex tile){
        World.Unit u=w.unit(id);Order o=null;
        if(tile!=null&&w.inside(tile)){
            World.City city=w.cityAt(tile);World.Unit unit=w.unitAt(tile);War.Structure structure=w.war.at(tile);
            o=city!=null?new Order(Kind.CITY,tile,city.id,city.owner):unit!=null?new Order(Kind.UNIT,tile,unit.id,unit.owner):structure!=null?new Order(Kind.STRUCTURE,tile,-1,structure.owner):new Order(Kind.TILE,tile,-1,-1);
        }
        return plan(u,o,true);
    }
    /** Read-only current route for the map, including paused orders. */
    public Plan current(World.Unit u){return plan(u,u==null?null:u.march,false);}
    private int budget(World.Unit u,Hex h,int[] budgets){
        int mode=w.army.water(h)?1:0;if(budgets[mode]<0)budgets[mode]=w.war.movementAt(u,h);return budgets[mode];
    }
    private Plan plan(World.Unit u,Order o,boolean snapshot){
        int[] budgets={-1,-1};
        String problem=error(u);Hex destination=target(o);List<Hex> path=new ArrayList<>();int cost=0,stepsNow=0,turns=0;
        if(problem==null&&(o==null||destination==null||!w.inside(destination)))problem="目标已消失或归属改变，请重新选择";
        if(problem==null&&o.kind==Kind.UNIT&&o.targetId==u.id)problem="请选择其他目标";
        if(problem==null){
            Set<Hex> blocked=new HashSet<>();for(World.City c:w.cities)blocked.add(c.hex);
            for(World.Unit other:w.units)if(other.id!=u.id)blocked.add(other.hex);
            for(Domestic.Facility f:w.domestic.facilities)blocked.add(f.hex);
            for(War.Structure s:w.war.structures())blocked.add(s.hex);
            for(War.Fire f:w.war.fires())blocked.add(f.hex);
            Map<Hex,Integer> distance=new HashMap<>();Map<Hex,Hex> previous=new HashMap<>();
            PriorityQueue<Step> queue=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.h.q).thenComparingInt(s->s.h.r));
            distance.put(u.hex,0);queue.add(new Step(u.hex,0));Hex finish=null;
            while(!queue.isEmpty()){
                Step s=queue.remove();if(s.cost!=distance.get(s.h))continue;
                if(o.kind==Kind.TILE?s.h.equals(destination):s.h.distance(destination)==1){finish=s.h;cost=s.cost;break;}
                for(Hex next:s.h.neighbors()){
                    int step=w.army.moveCost(u,s.h,next);if(step<1||blocked.contains(next))continue;
                    // An edge that cannot fit in any turn at its source must never produce an endless order.
                    int budget=budget(u,s.h,budgets);if(s.h.equals(u.hex))budget=Math.max(budget,w.orders.remaining(u));
                    if(step>budget)continue;
                    int total=s.cost+step;if(total<distance.getOrDefault(next,Integer.MAX_VALUE)){distance.put(next,total);previous.put(next,s.h);queue.add(new Step(next,total));}
                }
            }
            if(finish==null)problem="没有可通行路线：请检查占格、火场、地形或难所行军技巧";
            else {
                LinkedList<Hex> route=new LinkedList<>();for(Hex h=finish;h!=null;h=previous.get(h))route.addFirst(h);path.addAll(route);
                int available=w.orders.remaining(u),spent=0;boolean now=true;
                for(int i=1;i<path.size();i++){
                    int step=w.army.moveCost(u,path.get(i-1),path.get(i));
                    if(spent+step>available){turns++;available=budget(u,path.get(i-1),budgets);spent=0;now=false;}
                    spent+=step;if(now)stepsNow=i;
                }
            }
        }
        byte[] state=null;if(problem==null&&snapshot)try{state=SaveCodec.encode(w);}catch(IOException e){problem="局面无法保存："+e.getMessage();}
        return new Plan(u==null?-1:u.id,o,destination,label(o),path,cost,stepsNow,turns,problem,state);
    }
    public World.Result execute(Plan plan){
        if(plan==null||!plan.valid()||plan.snapshot==null)return w.fail(plan==null?"请先选择行军目标":plan.error==null?"请重新预览路线":plan.error);
        try{if(!Arrays.equals(plan.snapshot,SaveCodec.encode(w)))return w.fail("局面已变化，请重新预览路线");}catch(IOException e){return w.fail("局面校验失败");}
        World.Unit u=w.unit(plan.unitId);String problem=error(u);if(problem!=null)return w.fail(problem);
        u.march=new Order(plan.order.kind,plan.order.tile,plan.order.targetId,plan.order.owner);advance(u);
        return w.success(w.officer(u.officerId).name+" · "+(u.march==null?"已抵达目标附近，可继续下令":u.march.paused.isEmpty()?"向"+label(u.march)+"行军，下旬自动继续":"行军暂停："+u.march.paused));
    }
    public World.Result stop(int id){
        World.Unit u=w.unit(id);if(u==null||u.owner!=w.active||w.contests.busy())return w.fail("当前不能变更行军指令");
        if(u.march==null)return w.fail("部队没有行军指令");u.march=null;return w.success(w.officer(u.officerId).name+"停止自动行军");
    }
    void advanceAll(){
        if(w.gameOver())return;
        for(World.Unit u:new ArrayList<>(w.units))if(u.owner==w.active&&u.march!=null)advance(u);
    }
    private void advance(World.Unit u){
        Order order=u.march;if(order==null)return;
        if(u.acted){order.paused="本旬已行动，下旬继续";return;}
        if(u.status!=War.Status.NORMAL){order.paused="异常状态，恢复后继续";return;}
        Plan route=plan(u,order,false);
        if(!route.valid()){order.paused=route.error;w.note(w.officer(u.officerId).name+"行军暂停："+route.error);return;}
        order.paused="";
        if(route.stepsNow>0){
            UnitOrders.MovePlan movement=w.orders.previewMove(u.id,route.path.get(route.stepsNow));
            // Use the chosen route itself: the tactical planner may choose a shorter path through fire.
            World.Result moved=w.orders.executeRoute(movement,route.path.subList(0,route.stepsNow+1));
            if(!moved.ok){order.paused=moved.message;u.march=order;return;}
            u.march=order;
        }
        if(u.hex.equals(route.path.get(route.path.size()-1))){u.march=null;w.note(w.officer(u.officerId).name+"抵达"+route.label+"附近，请选择下一道命令");}
    }
    void write(DataOutputStream d)throws IOException {
        int count=0;for(World.Unit u:w.units)if(u.march!=null)count++;d.writeInt(count);
        for(World.Unit u:w.units)if(u.march!=null){Order o=u.march;d.writeInt(u.id);d.writeByte(o.kind.ordinal());d.writeInt(o.tile.q);d.writeInt(o.tile.r);d.writeInt(o.targetId);d.writeInt(o.owner);d.writeUTF(o.paused);}
    }
    void read(DataInputStream d)throws IOException {
        int count=d.readInt();if(count<0||count>w.units.size())throw new IOException("行军指令数量错误");
        for(int i=0;i<count;i++){
            World.Unit u=w.unit(d.readInt());int kind=d.readUnsignedByte();Hex h=new Hex(d.readInt(),d.readInt());int id=d.readInt(),owner=d.readInt();String paused=d.readUTF();
            if(u==null||u.march!=null||kind>=Kind.values().length)throw new IOException("行军指令引用错误");
            u.march=new Order(Kind.values()[kind],h,id,owner);u.march.paused=paused;
        }
    }
    void validate()throws IOException {
        for(World.Unit u:w.units)if(u.march!=null){Order o=u.march;
            if(o.kind==null||o.tile==null||!w.inside(o.tile)||o.paused==null||o.paused.length()>300||o.owner< -1||o.owner>=w.factions.length||o.targetId< -1||o.targetId>10000000)throw new IOException("行军目标无效");
            if((o.kind==Kind.CITY||o.kind==Kind.UNIT)&&o.targetId<0||o.kind==Kind.UNIT&&o.targetId==u.id)throw new IOException("行军目标引用无效");
        }
    }
}

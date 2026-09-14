package game.sanguo.core;

import java.io.IOException;
import java.util.*;

/** A move spends only movement; a subsequent command ends the action. Preview is read-only. */
public final class UnitOrders {
    public static final class MovePlan {
        public final int unitId, cost, remaining;
        public final List<Hex> path;
        public final String error;
        private final byte[] snapshot;
        private MovePlan(int id,int cost,int remaining,List<Hex> path,String error,byte[] snapshot) {
            unitId=id;this.cost=cost;this.remaining=remaining;
            this.path=Collections.unmodifiableList(new ArrayList<>(path));this.error=error;this.snapshot=snapshot;
        }
        public boolean valid(){return error==null;}
    }
    private static final class Step {
        final Hex hex;final int cost;
        Step(Hex hex,int cost){this.hex=hex;this.cost=cost;}
    }
    private static final class Paths {
        final Map<Hex,Integer> costs=new LinkedHashMap<>();
        final Map<Hex,Hex> previous=new HashMap<>();
    }
    private final World w;
    UnitOrders(World w){this.w=w;}
    public String error(World.Unit u) {
        if(w.commandsBlocked())return "请先完成当前对局或君主继承";
        if(w.gameOver())return "本局已结束";
        if(u==null||w.unit(u.id)!=u||u.owner!=w.active)return "请选择当前势力的部队";
        if(!w.districts.directUnit(u.id))return "该部队由委任军团指挥";
        if(u.acted)return "这支部队本旬已行动";
        if(u.status!=War.Status.NORMAL)return "部队处于异常状态，需要镇静";
        return null;
    }
    public int remaining(World.Unit u) {
        return u==null||u.acted||u.status!=War.Status.NORMAL?0:
            Math.max(0,(u.movementBudget<0?w.war.movement(u):u.movementBudget)-u.movementSpent);
    }
    private Paths paths(World.Unit u) {
        Paths result=new Paths();if(error(u)!=null)return result;
        PriorityQueue<Step> queue=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost)
            .thenComparingInt(s->s.hex.q).thenComparingInt(s->s.hex.r));
        int budget=remaining(u);result.costs.put(u.hex,0);queue.add(new Step(u.hex,0));
        while(!queue.isEmpty()) {
            Step step=queue.remove();if(step.cost!=result.costs.get(step.hex))continue;
            for(Hex next:step.hex.neighbors()) {
                int cost=w.army.moveCost(u,step.hex,next);
                if(cost<0||w.cityAt(next)!=null||w.domestic.at(next)!=null||w.war.at(next)!=null)continue;
                World.Unit other=w.unitAt(next);if(other!=null&&other.id!=u.id)continue;
                int total=step.cost+cost;
                if(total<=budget&&w.advancedBattle.zone(u,next))total=budget;
                if(total<=budget&&total<result.costs.getOrDefault(next,Integer.MAX_VALUE)) {
                    result.costs.put(next,total);result.previous.put(next,step.hex);queue.add(new Step(next,total));
                }
            }
        }
        return result;
    }
    public Map<Hex,Integer> reachable(World.Unit u){return Collections.unmodifiableMap(paths(u).costs);}
    public MovePlan previewMove(int unitId,Hex destination) {
        World.Unit u=w.unit(unitId);String error=error(u);
        Paths paths=error==null?paths(u):new Paths();
        if(error==null&&(destination==null||destination.equals(u.hex)||!paths.costs.containsKey(destination)))error="目标格不可达";
        if(error!=null)return new MovePlan(unitId,0,0,Collections.emptyList(),error,null);
        LinkedList<Hex> route=new LinkedList<>();
        for(Hex h=destination;h!=null;h=paths.previous.get(h))route.addFirst(h);
        try {
            int cost=paths.costs.get(destination);
            return new MovePlan(unitId,cost,remaining(u)-cost,route,null,SaveCodec.encode(w));
        } catch(IOException e){return new MovePlan(unitId,0,0,Collections.emptyList(),"局面校验失败："+e.getMessage(),null);}
    }
    public World.Result execute(MovePlan plan) {
        if(plan==null||!plan.valid())return w.fail(plan==null?"移动预览无效":plan.error);
        try {if(!Arrays.equals(plan.snapshot,SaveCodec.encode(w)))return w.fail("局面已变化，请重新预览移动");}
        catch(IOException e){return w.fail("局面校验失败："+e.getMessage());}
        World.Unit u=w.unit(plan.unitId);String error=error(u);if(error!=null)return w.fail(error);
        if(u.movementBudget<0)u.movementBudget=w.war.movement(u); // Freeze before any water/land conversion.
        u.march=null;
        u.movementSpent+=plan.cost;u.hex=plan.path.get(plan.path.size()-1);w.fieldworks.traveled(u,plan.path);
        return w.success(w.officer(u.officerId).name+"部队移动，剩余移动"+remaining(u)+"，仍可执行命令");
    }
    World.Result executeRoute(MovePlan base,List<Hex> route) {
        if(base==null||!base.valid())return w.fail(base==null?"移动预览无效":base.error);
        World.Unit u=w.unit(base.unitId);int cost=0;
        if(u==null||route.isEmpty()||!route.get(0).equals(u.hex))return w.fail("路线起点已变化");
        for(int i=1;i<route.size();i++){
            Hex from=route.get(i-1),to=route.get(i);int step=w.army.moveCost(u,from,to);
            if(from.distance(to)!=1||step<1||w.unitAt(to)!=null||w.cityAt(to)!=null||w.domestic.at(to)!=null||w.war.at(to)!=null)return w.fail("路线已被阻挡");
            cost+=step;
            if(cost>remaining(u))return w.fail("本旬移动力不足");
            if(w.advancedBattle.zone(u,to)){
                if(i<route.size()-1)return w.fail("敌军阻挡，需要下旬继续");
                cost=remaining(u);
            }
        }
        if(cost>remaining(u))return w.fail("本旬移动力不足");
        return execute(new MovePlan(u.id,cost,remaining(u)-cost,route,null,base.snapshot));
    }
    void reset(World.Unit u){u.acted=false;u.movementBudget=-1;u.movementSpent=0;}
}

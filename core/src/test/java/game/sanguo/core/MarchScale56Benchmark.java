package game.sanguo.core;
import java.util.*;
public class MarchScale56Benchmark {
 static final class Step{Hex h;int cost;Step(Hex h,int c){this.h=h;cost=c;}}
 public static void main(String[] args)throws Exception{
  World w=ScenarioCatalog.load("heroes-250",0,56L);for(World.City c:w.cities)c.owner=0;
  World.Officer o=new World.Officer(99999,"march-probe",0,20015,80,80,80,80,80);w.officers.add(o);
  int[][] routes={{20015,20017},{20029,20030},{20036,20039}};
  for(int[] ids:routes){World.City start=w.city(ids[0]),goal=w.city(ids[1]);World.Unit u=new World.Unit(-1,0,o.id,World.Weapon.SPEAR,start.hex,10000,100000);Army.MovementCosts mc=w.army.movementCosts(u);
   Map<Hex,Integer> cost=new HashMap<>(),cells=new HashMap<>();PriorityQueue<Step> queue=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.h.q).thenComparingInt(s->s.h.r));queue.add(new Step(u.hex,0));cost.put(u.hex,0);cells.put(u.hex,0);int expanded=0;
   while(!queue.isEmpty()){Step s=queue.poll();if(cost.get(s.h)!=s.cost)continue;expanded++;if(SiteFootprint.contains(goal,s.h)){
     int movement=w.war.movement(u),turns=(s.cost+movement-1)/movement;System.out.printf(java.util.Locale.ROOT,"%d,%d,%d,%d,%d,%d,%d,%d%n",ids[0],ids[1],cells.get(s.h),s.cost,movement,turns,turns*Logistics.foodUse(w,u),expanded);break;}
    for(Hex n:s.h.neighbors()){int d=mc.cost(s.h,n);if(d<1||w.army.water(n))continue;int nc=s.cost+d;if(nc>=cost.getOrDefault(n,Integer.MAX_VALUE))continue;cost.put(n,nc);cells.put(n,cells.get(s.h)+1);queue.add(new Step(n,nc));}
   }
   if(queue.isEmpty()&&!cost.containsKey(goal.hex))System.out.println(ids[0]+","+ids[1]+",unreachable");
  }
 }
}

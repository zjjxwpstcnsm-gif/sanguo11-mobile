package game.sanguo.core;
import java.util.*;
import java.io.*;

public final class MarchOrdersTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World fixture(){
        World w=new World(24,12,"甲军","乙军");w.cities.add(new World.City(10,"甲城",new Hex(1,1),0));w.cities.add(new World.City(20,"乙城",new Hex(22,10),1));
        World.Officer o=new World.Officer(0,"行军将",0,-1,80,80,80,80,80);o.role=Strategy.Role.RULER;o.loyalty=100;o.unitId=1;w.officers.add(o);
        World.Unit u=new World.Unit(1,0,0,World.Weapon.SPEAR,new Hex(3,6),5000,100000);w.units.add(u);w.nextUnitId=2;return w;
    }
    public static void main(String[] args)throws Exception {
        previewAndTurns();rerouteAndPause();terrain();futureBudgets();targets();legacy();performance();
        System.out.println("PASS: "+checks+" march assertions: pure/stale preview, weighted routes, multi-turn budget, retarget/stop, obstacles, water, target identity, v12 migration, deterministic save continuation.");
    }
    private static void previewAndTurns()throws Exception {
        World w=fixture();World.Unit u=w.unit(1);byte[] before=bytes(w);MarchOrders.Plan plan=w.marches.preview(1,new Hex(19,6));
        check(plan.valid()&&plan.cost==16&&plan.stepsNow==4&&plan.estimatedTurns==3,"long plain route has exact cost and future turns");check(plan.path.get(0).equals(u.hex)&&plan.path.get(plan.path.size()-1).equals(new Hex(19,6)),"origin and destination included");
        check(Arrays.equals(before,bytes(w)),"preview/cancel is pure");ok(w.marches.execute(plan));
        check(u.hex.equals(new Hex(7,6))&&u.march!=null&&!u.acted&&w.orders.remaining(u)==0,"starts now using only current budget, action remains");
        byte[] issued=bytes(w);check(!w.marches.execute(plan).ok&&Arrays.equals(issued,bytes(w)),"stale preview rejected atomically");
        World restored=SaveCodec.decode(issued);check(restored.unit(1).march!=null,"order saved");
        for(int i=0;i<3;i++){Hex from=u.hex;ok(w.nextTurn());ok(restored.nextTurn());check(from.distance(u.hex)<=4,"no turn teleports");check(Arrays.equals(bytes(w),bytes(restored)),"save resume deterministic");}
        check(u.hex.equals(new Hex(19,6))&&u.march==null&&!u.acted,"arrival ends route and preserves command");Hex destination=u.hex;ok(w.nextTurn());check(u.hex.equals(destination),"arrival never repeats movement");
        u.acted=true;ok(w.marches.execute(w.marches.preview(1,new Hex(12,6))));check(u.hex.equals(destination)&&u.march!=null,"can queue after acting");ok(w.nextTurn());check(!u.hex.equals(destination),"queued order resumes next turn");
        int spent=u.movementSpent;ok(w.marches.stop(1));check(u.march==null&&u.movementSpent==spent,"stop does not refund movement");
    }
    private static void rerouteAndPause()throws Exception {
        World w=fixture();World.Unit u=w.unit(1);ok(w.marches.execute(w.marches.preview(1,new Hex(19,6))));
        for(int r=0;r<w.height;r++)w.terrain[10][r]=World.Terrain.MOUNTAIN;
        Hex before=u.hex;ok(w.nextTurn());check(u.hex.equals(before)&&u.march!=null&&!u.march.paused.isEmpty(),"dynamic blockage pauses without teleport");
        w.terrain[10][6]=World.Terrain.PLAIN;ok(w.nextTurn());check(!u.hex.equals(before)&&u.march.paused.isEmpty(),"unblocked route replanned");
        ok(w.marches.execute(w.marches.preview(1,new Hex(4,8))));check(w.marches.target(u.march).equals(new Hex(4,8)),"retarget replaces existing order");
        u.status=War.Status.CONFUSED;u.statusTurns=2;w.orders.reset(u);before=u.hex;w.marches.advanceAll();check(u.hex.equals(before)&&u.march.paused.contains("异常"),"abnormal status waits");
        u.status=War.Status.NORMAL;u.statusTurns=0;w.marches.advanceAll();check(!u.hex.equals(before),"recovered unit resumes");
        ok(w.marches.stop(1));w.orders.reset(u);before=u.hex;w.marches.advanceAll();check(u.hex.equals(before),"stopped order stays stopped");
        w.orders.reset(u);ok(w.marches.execute(w.marches.preview(1,new Hex(20,3))));w.orders.reset(u);Hex near=w.reachable(u).keySet().stream().filter(h->!h.equals(u.hex)).findFirst().get();ok(w.move(1,near));check(u.march==null,"manual tactical move overrides march");
    }
    private static void terrain()throws Exception {
        World w=fixture();World.Unit u=w.unit(1);
        for(int q=4;q<19;q++)w.terrain[q][6]=World.Terrain.FOREST;
        MarchOrders.Plan p=w.marches.preview(1,new Hex(19,6));check(p.valid()&&p.cost<30,"weighted route avoids expensive forest corridor");
        for(int r=0;r<w.height;r++)w.terrain[11][r]=World.Terrain.WATER;
        p=w.marches.preview(1,new Hex(19,6));check(p.valid()&&p.path.stream().anyMatch(h->h.q==11),"water route crosses river");
        ok(w.marches.execute(p));int n=0;while(u.march!=null&&n++<20)ok(w.nextTurn());check(u.march==null&&u.weapon==World.Weapon.SPEAR&&u.ship==Army.Ship.BOAT,"water/land route arrives retaining equipment");
        World blocked=fixture();for(Hex h:blocked.unit(1).hex.neighbors())blocked.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;byte[] before=bytes(blocked);MarchOrders.Plan impossible=blocked.marches.preview(1,new Hex(19,6));check(!impossible.valid()&&!blocked.marches.execute(impossible).ok&&Arrays.equals(before,bytes(blocked)),"unreachable is read-only");
        World narrow=new World(12,1);narrow.cities.add(new World.City(10,"甲",new Hex(0,0),0));narrow.cities.add(new World.City(20,"乙",new Hex(11,0),1));World.Officer o=new World.Officer(0,"冲车将",0,-1,80,80,80,80,80);o.unitId=1;narrow.officers.add(o);narrow.units.add(new World.Unit(1,0,0,World.Weapon.RAM,new Hex(1,0),3000,20000));narrow.nextUnitId=2;narrow.terrain[3][0]=World.Terrain.FOREST;
        check(!narrow.marches.preview(1,new Hex(10,0)).valid(),"edge costing more than full turn is not an endless route");
    }
    private static void futureBudgets()throws Exception {
        World w=fixture();World.Unit u=w.unit(1);Hex land=u.hex,water=new Hex(4,6);w.terrain[4][6]=World.Terrain.WATER;
        w.campaign.learned.put(0,EnumSet.of(Campaign.Tech.ELITE_SPEAR));
        for(Skill skill:new Skill[]{Skill.QIANGXING,Skill.CAODUO}){
            w.officer(0).skillId=skill.id;int predicted=w.war.movementAt(u,water);u.hex=water;check(predicted==w.war.movement(u),"future water skill and technology budget equals actual budget");
            predicted=w.war.movementAt(u,land);u.hex=land;check(predicted==w.war.movement(u),"future land skill and technology budget equals actual budget");
        }
    }
    private static void targets()throws Exception {
        World w=fixture();World.Unit u=w.unit(1);MarchOrders.Plan city=w.marches.preview(1,w.city(20).hex);check(city.valid()&&city.path.get(city.path.size()-1).distance(w.city(20).hex)==1,"city approached on a free adjacent tile");
        ok(w.marches.execute(city));w.cities.add(new World.City(21,"乙营",new Hex(22,0),1));w.city(20).owner=-1;Hex from=u.hex;ok(w.nextTurn());check(u.hex.equals(from)&&u.march.paused.contains("归属"),"ownership change does not silently repurpose target");
        World.Unit enemy=new World.Unit(2,1,2,World.Weapon.SPEAR,new Hex(18,6),5000,20000);World.Officer o=new World.Officer(2,"目标将",1,-1,80,80,80,80,80);o.unitId=2;w.officers.add(o);w.units.add(enemy);w.nextUnitId=3;
        ok(w.marches.execute(w.marches.preview(1,enemy.hex)));enemy.hex=new Hex(20,4);check(w.marches.current(u).target.equals(enemy.hex),"unit target tracks identity");
        w.units.remove(enemy);o.unitId=-1;o.cityId=20;o.owner=-1;o.role=Strategy.Role.UNAFFILIATED;o.loyalty=0;w.orders.reset(u);from=u.hex;w.marches.advanceAll();check(u.hex.equals(from)&&u.march.paused.contains("消失"),"destroyed target pauses");check(Arrays.equals(bytes(w),bytes(SaveCodec.decode(bytes(w)))),"lost target pause can be saved");
        World arrival=fixture();ok(arrival.marches.execute(arrival.marches.preview(1,arrival.city(20).hex)));int hp=arrival.city(20).defense;int guard=0;while(arrival.unit(1).march!=null&&guard++<20)ok(arrival.nextTurn());check(arrival.unit(1).hex.distance(arrival.city(20).hex)==1&&arrival.city(20).owner==1&&arrival.city(20).defense==hp,"arrival does not silently siege or capture");
    }
    private static void legacy()throws Exception {
        try(InputStream input=MarchOrdersTest.class.getResourceAsStream("/legacy-v12.sg11.b64")){
            byte[] old=Base64.getMimeDecoder().decode(input.readAllBytes());check(old[7]==12,"fixture produced by actual v12 writer");World w=SaveCodec.decode(old);check(w.scenarioId.equals("estates-drill")&&w.units.stream().noneMatch(u->u.march!=null),"v12 gains no invented orders");check(bytes(w)[7]==22&&Arrays.equals(bytes(w),bytes(SaveCodec.decode(bytes(w)))),"v12 upgrades to stable v17");
        }
    }
    private static void performance(){
        World w=new World(128,128);w.cities.add(new World.City(0,"甲",new Hex(0,0),0));w.cities.add(new World.City(1,"乙",new Hex(127,127),1));World.Officer o=new World.Officer(0,"将",0,-1,80,80,80,80,80);o.unitId=1;w.officers.add(o);w.units.add(new World.Unit(1,0,0,World.Weapon.SPEAR,new Hex(1,1),3000,20000));w.nextUnitId=2;
        long start=System.nanoTime();MarchOrders.Plan p=w.marches.preview(1,new Hex(126,126));long ms=(System.nanoTime()-start)/1000000;check(p.valid()&&p.cost==250,"128x128 route complete");System.out.println("March 128x128 cold preview: "+ms+" ms");
    }
}

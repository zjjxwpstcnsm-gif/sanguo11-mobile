package game.sanguo.core;

import java.util.*;
import java.util.function.Supplier;

/** Real commands: per-facility quotas, atomic rejection, save/reload and AI construction. */
public final class FacilityProductionTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static void reject(World w,Supplier<World.Result> action)throws Exception{
        byte[] before=SaveCodec.encode(w);check(!action.get().ok,"unavailable production rejected");
        check(Arrays.equals(before,SaveCodec.encode(w)),"rejection preserves resources, quota, personnel and RNG");
    }
    private static World fixture(){
        World w=new World(22,16);w.cities.add(new World.City(10,"甲城",new Hex(4,5),0));
        w.cities.add(new World.City(11,"乙城",new Hex(12,11),0));w.cities.add(new World.City(20,"敌都",new Hex(19,3),1));
        for(int i=0;i<8;i++)w.officers.add(new World.Officer(i,"甲将"+i,0,10,80,80,80,90,90));
        w.officers.add(new World.Officer(9,"乙将",0,11,80,80,80,90,90));w.officers.add(new World.Officer(20,"敌将",1,20,80,80,80,80,80));
        w.city(10).gold=50000;w.city(10).food=200000;w.city(11).gold=10000;w.city(20).gold=0;w.city(20).troops=0;
        w.strategy.initializeOffices();return w;
    }
    static Domestic.Facility facility(World w,int city,Domestic.Kind kind){
        Hex h=w.domestic.buildSites(city).get(0);
        if(kind==Domestic.Kind.SHIPYARD)for(Hex n:h.neighbors())if(w.inside(n)&&w.cityAt(n)==null&&w.domestic.at(n)==null){w.terrain[n.q][n.r]=World.Terrain.WATER;break;}
        Domestic.Facility f=new Domestic.Facility(w.domestic.nextFacilityId++,city,kind,h,-1,0);w.domestic.facilities.add(f);return f;
    }
    private static void recruiting()throws Exception{
        World missing=fixture();reject(missing,()->missing.recruit(10,0));
        ok(missing.domestic.build(10,0,Domestic.Kind.BARRACKS,missing.domestic.buildSites(10).get(0)));
        reject(missing,()->missing.recruit(10,1));check(missing.domestic.capacity(10,Domestic.Kind.BARRACKS)==0,"construction has no quota");
        ok(missing.nextTurn());ok(missing.nextTurn());check(missing.domestic.capacity(10,Domestic.Kind.BARRACKS)==1,"completed building supplies quota");ok(missing.recruit(10,1));

        World w=fixture();Domestic.Facility first=facility(w,10,Domestic.Kind.BARRACKS);first.level=3;
        facility(w,10,Domestic.Kind.BARRACKS);facility(w,11,Domestic.Kind.BARRACKS);
        check(w.domestic.remainingUses(10,Domestic.Kind.BARRACKS)==2,"levels affect yield, not number of orders");
        ok(w.recruit(10,0));ok(w.strategy.recruitSoldiers(10,1));reject(w,()->w.recruit(10,2));
        byte[] saved=SaveCodec.encode(w);World restored=SaveCodec.decode(saved);
        check(Arrays.equals(saved,SaveCodec.encode(restored)),"usage exact roundtrip");reject(restored,()->restored.recruit(10,2));
        ok(restored.recruit(11,9));check(restored.domestic.remainingUses(10,Domestic.Kind.BARRACKS)==0,"other city has independent quota");
        ok(restored.nextTurn());check(restored.domestic.remainingUses(10,Domestic.Kind.BARRACKS)==2,"new turn refreshes each surviving facility");ok(restored.recruit(10,2));
        World removed=fixture();Domestic.Facility a=facility(removed,10,Domestic.Kind.BARRACKS),b=facility(removed,10,Domestic.Kind.BARRACKS);
        ok(removed.recruit(10,0));removed.domestic.damage(b,1000);reject(removed,()->removed.recruit(10,1));
        check(a.lastUseTurn==removed.turn,"destroying unused facility does not refresh used facility");
    }
    private static void equipment()throws Exception{
        World w=fixture();reject(w,()->w.produce(10,0,World.Weapon.SPEAR));reject(w,()->w.produce(10,0,World.Weapon.CAVALRY));
        facility(w,10,Domestic.Kind.SMITH);facility(w,10,Domestic.Kind.SMITH);
        reject(w,()->w.produce(10,0,World.Weapon.CAVALRY));ok(w.produce(10,0,World.Weapon.SPEAR));ok(w.produce(10,1,World.Weapon.HALBERD));
        reject(w,()->w.produce(10,2,World.Weapon.CROSSBOW));facility(w,10,Domestic.Kind.STABLE);
        ok(w.produce(10,2,World.Weapon.CAVALRY));reject(w,()->w.produce(10,3,World.Weapon.CAVALRY));
        World stableOnly=fixture();facility(stableOnly,10,Domestic.Kind.STABLE);
        check(stableOnly.domestic.produceAmount(10,World.Weapon.CAVALRY)==2500,"warhorse yield comes from stables alone");ok(stableOnly.produce(10,0,World.Weapon.CAVALRY));
        World full=fixture();facility(full,10,Domestic.Kind.SMITH);full.city(10).equipment[0]=100000;
        reject(full,()->full.produce(10,0,World.Weapon.SPEAR));check(full.domestic.remainingUses(10,Domestic.Kind.SMITH)==1,"failed inventory check does not use quota");
        full.city(10).equipment[0]=0;ok(full.produce(10,0,World.Weapon.SPEAR));
    }
    private static void manufacture()throws Exception{
        World w=fixture();reject(w,()->w.army.produce(10,0,World.Weapon.RAM,null));
        facility(w,10,Domestic.Kind.WORKSHOP);facility(w,10,Domestic.Kind.SHIPYARD);
        reject(w,()->w.army.produce(10,0,World.Weapon.CATAPULT,null));check(w.domestic.remainingUses(10,Domestic.Kind.WORKSHOP)==1,"locked tech does not consume quota");
        ok(w.produce(10,0,World.Weapon.RAM));reject(w,()->w.army.produce(10,1,World.Weapon.SIEGE_TOWER,null));
        ok(w.army.produce(10,1,null,Army.Ship.TOWER_SHIP));ok(w.army.cancelProduction(1));
        reject(w,()->w.army.produce(10,2,null,Army.Ship.TOWER_SHIP));
        World restored=SaveCodec.decode(SaveCodec.encode(w));reject(restored,()->restored.army.produce(10,2,World.Weapon.RAM,null));
        ok(restored.nextTurn());ok(restored.army.produce(10,2,World.Weapon.SIEGE_TOWER,null));
        int before=restored.city(10).equipment[World.Weapon.RAM.ordinal()];ok(restored.nextTurn());ok(restored.nextTurn());
        check(restored.city(10).equipment[World.Weapon.RAM.ordinal()]==before+1,"in-progress manufacturing still completes once");
    }
    private static void ai()throws Exception{
        World w=fixture();World.City c=w.city(10);c.troops=1000;c.order=100;c.morale=100;
        StrategicAi ai=new StrategicAi(w);StrategicAi.Decision d=ai.plan(c.id,false);
        check(d!=null&&d.command==StrategicAi.Command.BUILD&&d.targetId==Domestic.Kind.BARRACKS.ordinal(),"AI builds missing barracks instead of impossible recruitment");
        ok(ai.execute(d));check(w.domestic.capacity(c.id,Domestic.Kind.BARRACKS)==0,"AI construction also waits for completion");
        ok(w.nextTurn());ok(w.nextTurn());d=ai.plan(c.id,false);
        check(d!=null&&d.command==StrategicAi.Command.RECRUIT,"AI recruits after construction");ok(ai.execute(d));
        d=ai.plan(c.id,false);check(d==null||d.command!=StrategicAi.Command.RECRUIT,"AI respects used quota");
    }
    public static void main(String[] args)throws Exception{recruiting();equipment();manufacture();ai();System.out.println("PASS: "+checks+" facility production assertions");}
}

package game.sanguo.core;
import java.util.*;

/** Real commands; only deterministic setup and action reset are fixture conveniences. */
public final class FieldLifecycleFixture {
    public static World start(){return FieldSceneFixture.create(1,false,false);}
    public static void build(World w){ok(w.fieldworks.build(1,War.StructureKind.CAMP,new Hex(13,14),0));}
    public static void finish(World w){
        War.Structure s=w.war.at(new Hex(13,14));int guard=20;
        while(!s.complete&&guard-->0){w.turn++;w.orders.reset(w.unit(1));w.fieldworks.continueOwner(0);}
        if(!s.complete)throw new AssertionError("construction did not complete");
    }
    public static void upgrade(World w){learn(w,Campaign.Tech.FACILITY_REINFORCEMENT);}
    private static void learn(World w,Campaign.Tech t){if(t.prerequisite!=null)learn(w,t.prerequisite);w.campaign.finishTech(0,t);}
    public static void damage(World w){
        World.Officer officer=w.officer(4);w.strategy.releaseGovernor(4);officer.cityId=-1;officer.unitId=w.nextUnitId;
        World.Unit enemy=new World.Unit(w.nextUnitId++,1,4,World.Weapon.RAM,new Hex(13,15),8000,30000);w.units.add(enemy);w.active=1;
        ok(w.war.attackStructure(enemy.id,new Hex(13,14)));w.active=0;
    }
    public static void repair(World w){w.orders.reset(w.unit(1));ok(w.fieldworks.repair(1,w.war.at(new Hex(13,14)).id));}
    public static void destroy(World w){
        w.active=1;World.Unit enemy=w.units.get(w.units.size()-1);int guard=50;
        while(w.war.at(new Hex(13,14))!=null&&guard-->0){w.orders.reset(enemy);ok(w.war.attackStructure(enemy.id,new Hex(13,14)));}
        if(w.war.at(new Hex(13,14))!=null)throw new AssertionError("facility not destroyed");w.active=0;
    }
    public static void domesticBuild(World w){ok(w.domestic.build(0,2,Domestic.Kind.FARM,w.domestic.buildSites(0).get(0)));}
    public static void domesticFinish(World w){for(int i=0;i<4;i++){w.turn++;w.domestic.tick();}w.officer(2).acted=false;Arrays.fill(w.actionPoints,60);}
    public static void domesticDemolish(World w){ok(w.domestic.demolish(w.domestic.facilities.get(0).id,2));}
    private static void ok(World.Result r){if(!r.ok)throw new AssertionError(r.message);}
}

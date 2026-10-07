package game.sanguo.core;
import java.util.*;
/** Semantic boundaries: seven-cell funding, arbitrary legal amount, no preview mutation or failed debit. */
public final class SessionBFieldworksTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    public static void main(String[] args)throws Exception{
        var source=PcScenarioCatalog.all().get(14);World w=PcScenarioCatalog.load(source.identity.scenarioId,1,42);
        World.City city=w.home();World.Officer leader=w.idle(city).get(0);
        World.Result deployed=w.army.deploy(city.id,leader.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,3000,6000,3000);check(deployed.ok,deployed.message);
        World.Unit unit=w.unit(leader.unitId);byte[] deployedBytes=bytes(w);
        for(Hex target:unit.hex.neighbors()){
            var result=w.fieldworks.buildCheck(unit.id,War.StructureKind.CAMP,target,0);
            check(Objects.equals(result.detail,w.fieldworks.buildError(unit.id,War.StructureKind.CAMP,target,0)),"shared legacy/new failure detail");
            check(w.fieldworks.sites(unit.id,War.StructureKind.CAMP).contains(target)==result.allowed(),"same coordinates drive target visibility");
            check(Arrays.equals(deployedBytes,bytes(w)),"selection/check leave full save and both RNG unchanged");
            if(!result.allowed()){World rejected=SaveCodec.decode(deployedBytes);check(!rejected.fieldworks.build(unit.id,War.StructureKind.CAMP,target,0).ok,"actual illegal construction rejected");check(Arrays.equals(deployedBytes,bytes(rejected)),"rejection leaves all persisted state unchanged");}
        }
        for(War.StructureKind kind:List.of(War.StructureKind.EARTH_WALL,War.StructureKind.FIRE_SEED)){
            World placed=SaveCodec.decode(deployedBytes);List<Hex> sites=placed.fieldworks.sites(unit.id,kind);
            check(!sites.isEmpty(),"original category2/3 offers legitimate city-near placement "+kind);
            Hex target=sites.get(0);check(SiteFootprint.distance(placed.city(city.id),target)<=2,"regression target actually falls inside old overbroad city exclusion");
            check(placed.fieldworks.build(unit.id,kind,target,0).ok,"real previously rejected wall/trap build "+kind);
            check(placed.unit(unit.id).gold==3000-placed.fieldworks.buildCost(unit.id,kind,target)&&placed.unit(unit.id).acted&&placed.war.at(target)!=null,"true gold/action/structure debit "+kind);
            check(Arrays.equals(bytes(placed),bytes(SaveCodec.decode(bytes(placed)))),"placed wall/trap complete saved world/RNG "+kind);
        }
        World funded=SaveCodec.decode(deployedBytes);World.Unit f=funded.unit(unit.id);
        check(funded.orders.reachable(f).containsKey(city.hex),"ordinary movement can reach own city center");
        check(funded.move(f.id,city.hex).ok,"normal move reaches own center");
        check(funded.army.canEnterSite(f,f.hex,funded.city(city.id)),"center is real lawful entry position");
        System.out.println("CENTER unitExists="+(funded.unit(f.id)!=null)+" error="+funded.orders.combatError(funded.unit(f.id))+" gold="+f.gold+" citygold="+funded.city(city.id).gold+" validation="+funded.fieldworks.withdrawCheck(f.id,city.id,1).code);
        check(funded.fieldworks.fundingSites(f.id).stream().anyMatch(c->c.id==city.id),"funding list includes lawful center (centerDistance0)");
        byte[] before=bytes(funded);int maximum=funded.fieldworks.withdrawMaximum(f.id,city.id);
        check(maximum>0&&funded.fieldworks.withdrawCheck(f.id,city.id,maximum).allowed(),"maximum follows actual stock and capacity");check(Arrays.equals(before,bytes(funded)),"funding previews pure");
        int amount=Math.min(137,maximum),gold=f.gold,stock=funded.city(city.id).gold;
        check(funded.fieldworks.withdraw(f.id,city.id,amount).ok,"arbitrary lawful amount below200 submits");check(f.gold==gold+amount&&funded.city(city.id).gold==stock-amount&&f.acted,"real one-time transfer/action");
        before=bytes(funded);check(!funded.fieldworks.withdraw(f.id,city.id,amount).ok,"second same-turn funding rejected");check(Arrays.equals(before,bytes(funded)),"second funding rejection keeps state/RNG");
        check(Arrays.equals(before,bytes(SaveCodec.decode(before))),"funded source policy complete roundtrip");
        repairParity(SaveCodec.decode(deployedBytes),unit.id);
        System.out.println("PASS SESSION B FIELDWORKS "+checks+" assertions; normal source/deploy/center movement/funding and full save/bothRNG");
    }
    /** Declared damaged structures isolate selection/submission parity. Ordinary
     * Android repair/abort/damage acceptance remains a separate required flow. */
    private static void repairParity(World w,int unitId)throws Exception{
        World.Unit unit=w.unit(unitId);w.orders.reset(unit);
        List<Hex> sites=w.fieldworks.sites(unitId,War.StructureKind.EARTH_WALL);check(sites.size()>=2,"source geography has two lawful construction targets");
        War.Structure current=new War.Structure(w.war.nextStructureId++,unit.owner,War.StructureKind.CAMP,sites.get(0),20);
        War.Structure other=new War.Structure(w.war.nextStructureId++,unit.owner,War.StructureKind.EARTH_WALL,sites.get(1),20);
        current.complete=false;current.builder=unitId;other.complete=false;w.war.structures.add(current);w.war.structures.add(other);
        byte[] before=bytes(w);
        check(w.fieldworks.repairCheck(unitId,current.id).allowed()&&w.fieldworks.repairSites(unitId).contains(current),"own current project remains repairable");
        check(w.fieldworks.repairCheck(unitId,other.id).code.equals("UNIT_BUILDING")&&!w.fieldworks.repairSites(unitId).contains(other),"other project hidden by exact formal rejection");
        check(Arrays.equals(before,bytes(w)),"repair visibility leaves wholeWorld/bothRNG unchanged");
        World.Result rejected=w.fieldworks.repair(unitId,other.id);check(!rejected.ok&&rejected.message.equals(w.fieldworks.repairCheck(unitId,other.id).detail),"actual rejection shares displayed detail");
        check(Arrays.equals(before,bytes(w)),"rejected other repair bytepure");
        check(w.fieldworks.stop(unitId).ok&&current.builder<0&&unit.gold==3000&&!unit.acted,"stop releases project without refund or extra action");
        byte[] stopped=bytes(w);w=SaveCodec.decode(stopped);unit=w.unit(unitId);other=w.fieldworks.byId(other.id);
        final int otherId=other.id;check(w.fieldworks.repairSites(unitId).stream().anyMatch(s->s.id==otherId),"stopped/load current project no longer hides legal target");
        int gold=unit.gold,hp=other.hp;check(w.fieldworks.repair(unitId,other.id).ok&&unit.acted&&unit.gold==gold&&other.hp>hp,"formal repair consumes action and increases real durable HP without gold debit");
        byte[] repaired=bytes(w);check(Arrays.equals(repaired,bytes(SaveCodec.decode(repaired))),"repair fullsourceWorld and bothRNG roundtrip");
        check(!w.fieldworks.repair(unitId,other.id).ok&&Arrays.equals(repaired,bytes(w)),"same-turn repair duplicate leaves allbytes unchanged");
    }
}

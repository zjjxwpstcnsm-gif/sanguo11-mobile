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
        System.out.println("PASS SESSION B FIELDWORKS "+checks+" assertions; normal source/deploy/center movement/funding and full save/bothRNG");
    }
}

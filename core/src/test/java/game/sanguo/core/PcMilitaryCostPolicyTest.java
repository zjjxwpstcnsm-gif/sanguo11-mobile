package game.sanguo.core;
import java.nio.file.*;import java.util.*;

/** Fresh-source base fees, old policy preservation and real ordinary military construction. */
public final class PcMilitaryCostPolicyTest {
    private static int checks;
    private static void check(boolean b,String text){checks++;if(!b)throw new AssertionError(text);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    public static void main(String[] args)throws Exception{
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(14).identity.scenarioId,1,42);
        check(PcMilitaryCostPolicy.enabled(w),"fresh explicit source policy attached");
        byte[] initial=bytes(w);check(initial[7]==38&&Arrays.equals(initial,bytes(SaveCodec.decode(initial))),"same source header/fullWorld/bothRNG exact");
        for(War.StructureKind kind:List.of(War.StructureKind.CAMP,War.StructureKind.FORT,War.StructureKind.FORTRESS))check(w.fieldworks.baseBuildCost(kind)==500,"original three base fees500 "+kind);
        check(Arrays.equals(initial,bytes(w)),"fee query preserves entire save/RNG");
        World.City city=w.home();World.Officer actor=w.idle(city).get(0);
        check(w.army.deploy(city.id,actor.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,3000,6000,1000).ok,"ordinary deployment1000 gold");
        World.Unit unit=w.unit(actor.unitId);check(w.move(unit.id,new Hex(202,45)).ok,"ordinary legal move");Hex target=new Hex(202,44);
        check(w.fieldworks.buildCost(unit.id,War.StructureKind.CAMP,target)==500&&w.fieldworks.buildCheck(unit.id,War.StructureKind.CAMP,target,0).allowed(),"1000 enough at legitimate camp cell");
        byte[] preview=bytes(w);World legacy=SaveCodec.decode(preview);legacy.extensions.put(PcMilitaryCostPolicy.NAMESPACE,null);byte[] old=bytes(legacy);
        check(legacy.fieldworks.baseBuildCost(War.StructureKind.CAMP)==1500&&"UNIT_GOLD".equals(legacy.fieldworks.buildCheck(unit.id,War.StructureKind.CAMP,target,0).code),"saved legacy fee is not silently upgraded");
        check(Arrays.equals(old,bytes(SaveCodec.decode(old)))&&!PcMilitaryCostPolicy.enabled(SaveCodec.decode(old)),"missing saved policy remains absent/fullWorld/RNG exact");
        check(w.fieldworks.build(unit.id,War.StructureKind.CAMP,target,0).ok,"formal build uses same500 fee");
        check(w.unit(unit.id).gold==500&&w.unit(unit.id).acted&&w.war.at(target)!=null&&!w.war.at(target).complete,"actual one-time500 debit/action/construction");
        byte[] built=bytes(w);check(Arrays.equals(built,bytes(SaveCodec.decode(built))),"existing construction and new fee policy exact resume");
        check(!w.fieldworks.build(unit.id,War.StructureKind.CAMP,target,0).ok&&Arrays.equals(built,bytes(w)),"second submit no debit/RNG change");
        World historical=SaveCodec.decode(Files.readAllBytes(Path.of("docs/handoff/20261004/session1/batch19-actual-art-mid.sg11")));byte[] original=bytes(historical);
        check(!PcMilitaryCostPolicy.enabled(historical)&&historical.fieldworks.baseBuildCost(War.StructureKind.CAMP)==1500&&Arrays.equals(original,bytes(historical)),"genuine old39 keeps its fee and full active contest/RNG");
        byte[] policy=w.extensions.get(PcMilitaryCostPolicy.NAMESPACE);policy[3]++;w.extensions.put(PcMilitaryCostPolicy.NAMESPACE,policy);byte[] unknown=bytes(w);
        check(!PcMilitaryCostPolicy.enabled(w)&&Arrays.equals(unknown,bytes(SaveCodec.decode(unknown))),"future unknown magic retained opaque without activation");
        System.out.println("PASS PcMilitaryCostPolicy "+checks+" fresh500/ordinary1000gold/actualdebit/fullsave/old39/no-backfill/opaque-unknown; militaryHQ discount still pending");
    }
}

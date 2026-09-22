package game.sanguo.core;

import java.util.*;

/** Added entities use production movement, conquest, garrison, AI and save commands. */
public final class MapEditorRuntimeTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        MapPatch patch=MapEditorContinuationTest.fixture();World w=CustomMaps.load(patch,patch.preview,0,670L);
        World.City gate=w.city(100006703);gate.owner=-1;gate.defense=1;gate.troops=0;
        World.Officer leader=w.idle(w.home()).stream().filter(o->o.role!=Strategy.Role.RULER).findFirst().orElseThrow();
        World.Unit unit=new World.Unit(w.nextUnitId++,0,leader.id,World.Weapon.SPEAR,gate.hex,3000,9000);Hex approach=null,start=null;
        for(Hex near:gate.hex.neighbors())if(w.inside(near)&&!w.army.water(near)&&w.cityAt(near)==null&&w.cost(near,unit.weapon)>0)
            for(Hex far:near.neighbors())if(!far.equals(gate.hex)&&w.inside(far)&&!w.army.water(far)&&w.cityAt(far)==null&&w.army.entryCost(unit,far,near)>0){approach=near;start=far;break;}
        check(start!=null,"legal new-gate approach fixture");unit.hex=start;w.units.add(unit);leader.cityId=-1;leader.unitId=unit.id;
        World.Result move=w.move(unit.id,approach);check(move.ok,"production approach movement: "+move.message);
        // New action boundary for this isolated attack fixture; movement rules stay unchanged.
        unit.acted=false;leader.acted=false;unit.movementBudget=-1;unit.movementSpent=0;w.actionPoints[0]=60;
        check(w.siegeHit(unit,gate).equals(gate.hex),"new gate is the formal attack target");World.Result hit=w.siege(unit.id,gate.id);check(hit.ok,"production gate attack: "+hit.message);
        check(gate.owner==0,"new gate captured independently");check(w.unit(unit.id)==null&&leader.cityId==gate.id,"adjacent conqueror enters new gate");
        World.City parent=SiteAffiliation.parent(w,gate);int gateOwner=gate.owner;parent.owner=1;
        check(gate.owner==gateOwner,"parent conquest does not transfer gate ownership");check(w.districts.city(gate.id)==null||w.districts.city(gate.id).owner==gate.owner,"gate cannot inherit enemy district");
        World simulation=CustomMaps.load(patch,patch.preview,0,671L);byte[] opening=SaveCodec.encode(simulation);World replay=SaveCodec.decode(opening);
        World.Result turn=simulation.nextTurn();check(turn.ok,"all national AI factions finish custom-map turn: "+turn.message);
        check(simulation.city(100006701)!=null&&simulation.city(100006702)!=null&&simulation.city(100006703)!=null,"AI turn preserves added entity IDs");
        check(replay.nextTurn().ok,"saved custom-map AI replay finishes");check(Arrays.equals(SaveCodec.encode(simulation),SaveCodec.encode(replay)),"custom-map turn replay is byte deterministic");
        System.out.println("PASS "+checks+" added-site movement/conquest/AI replay checks");
    }
}

package game.sanguo.runtime;

import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.query.TerrainWireCode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** The normal compatibility command host must commit displacement exactly once. */
public final class CityDisplacementSessionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static World fixture()throws Exception{
        World w=new World(16,12,"我军","敌军");w.strategy.setSeed(29016);
        for(int side=0;side<2;side++){
            World.City camp=new World.City(10+side,"后营"+side,new Hex(1+side*6,1),side);w.cities.add(camp);
            World.Officer ruler=new World.Officer(10+side,"君主"+side,side,camp.id,80,80,80,80,80);ruler.role=Strategy.Role.RULER;ruler.loyalty=100;w.officers.add(ruler);
            World.Officer officer=new World.Officer(100+side,"战将"+side,side,-1,80,80,80,80,80);Arrays.fill(officer.aptitude,3);officer.unitId=1+side;w.officers.add(officer);
            World.Unit u=new World.Unit(1+side,side,officer.id,side==0?World.Weapon.CAVALRY:World.Weapon.SPEAR,new Hex(12-side,6),8000,30000);u.energy=100;w.units.add(u);
        }
        w.nextUnitId=3;w.unit(2).status=War.Status.CONFUSED;w.unit(2).statusTurns=1;
        w.cities.add(new World.City(20,"敌方七格城",new Hex(8,6),1));
        return SaveCodec.decode(SaveCodec.encode(w));
    }
    public static void main(String[] args)throws Exception{
        try(GameSession game=new GameSession(fixture())){
            List<GameEvent> facts=new ArrayList<>();game.subscribe(facts::add);
            byte[] before=game.captureSave();StateToken token=game.state();LegacyView view=game.legacyView(),stale=game.legacyView();
            Displacement.Preview preview=view.draft.war.tacticPreview(1,2,War.Tactic.ADVANCE);
            check(preview.valid()&&new Hex(9,6).equals(preview.blocked),"real host draft previews blocked city rim: "+preview.error+" "+preview.text);
            GameSnapshot snapshot=game.snapshot();Hex occupied=view.draft.unit(2).hex;
            check(snapshot.terrain.charAt(occupied.r*snapshot.width+occupied.q)==TerrainWireCode.encode(World.Terrain.PLAIN),"terrain query includes occupied unit cell");
            check(token.equals(game.state())&&facts.isEmpty()&&Arrays.equals(before,game.captureSave()),"preview/map query leaves authority, events and RNG unchanged");
            World direct=SaveCodec.decode(before);check(direct.war.tactic(1,2,War.Tactic.ADVANCE).ok,"ordinary command control succeeds");
            TurnJournal journal=new TurnJournal(view.draft);
            World.Result result=game.legacy(view.draft,()->view.draft.war.tactic(1,2,War.Tactic.ADVANCE));journal.close();
            check(result.ok,result.message);
            check(game.state().revision==token.revision+1&&facts.size()==1&&facts.get(0).kind==GameEvent.Kind.LEGACY_COMMITTED,"one committed fact with one revision");
            check(Arrays.equals(SaveCodec.encode(direct),game.captureSave()),"host commit equals ordinary rule/save/RNG result");
            check(SaveCodec.decode(game.captureSave()).unit(2).hex.equals(new Hex(10,6)),"authority retains external landing");
            byte[] committed=game.captureSave();AtomicInteger called=new AtomicInteger();
            check(!game.legacy(stale.draft,()->{called.incrementAndGet();return stale.draft.war.tactic(1,2,War.Tactic.ADVANCE);}).ok,"stale draft rejected");
            check(!game.legacy(view.draft,()->{called.incrementAndGet();return view.draft.war.tactic(1,2,War.Tactic.ADVANCE);}).ok,"consumed draft rejected");
            check(called.get()==0&&facts.size()==1&&Arrays.equals(committed,game.captureSave()),"rejected duplicate never evaluates rules or publishes sound facts");
            Set<String> ids=new HashSet<>();World visual=SaveCodec.decode(before);
            for(TurnJournal.Event event:journal.events()){check(ids.add(event.id),"journal fact has unique deduplication identity");event.applyVisual(visual);}
            check(!ids.isEmpty()&&Arrays.equals(committed,game.captureSave()),"presentation event replay is detached from authority");
            LegacyView rejected=game.legacyView();check(!game.legacy(rejected.draft,()->rejected.draft.war.tactic(1,2,War.Tactic.ADVANCE)).ok,"acted unit rejected through normal host");
            check(facts.size()==1&&Arrays.equals(committed,game.captureSave()),"rule rejection has no new commit/event");
            TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());World continuation=SaveCodec.decode(committed);
            check(computed.nextTurn().ok&&continuation.nextTurn().ok,"normal turn executes after displacement");
            check(game.commitTurn(ticket,computed)&&!game.commitTurn(ticket,computed),"complete turn commits once");
            check(Arrays.equals(SaveCodec.encode(continuation),game.captureSave()),"turn and saved continuation are byte exact");
            game.replace(SaveCodec.decode(committed));check(Arrays.equals(committed,game.captureSave()),"explicit load preserves position and full saved state");
        }
        System.out.println("PASS CityDisplacementSessionTest checks="+checks+" normal host/preview/commit/token/journal/occupied terrain/turn/save boundaries");
    }
}

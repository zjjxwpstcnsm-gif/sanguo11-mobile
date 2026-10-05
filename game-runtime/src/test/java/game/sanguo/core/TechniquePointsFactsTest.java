package game.sanguo.core;
import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;
/** Actual commands and turn commits; presentation facts cannot change reward/save semantics. */
public final class TechniquePointsFactsTest {
 static int checks;static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  World initial=ProductionSessionTest.fixture();initial.campaign.learned.get(0).clear();initial.campaign.points.put(0,9000);initial.campaign.points.put(1,500);
  try(GameSession session=new GameSession(initial)){
   List<GameEvent> events=new ArrayList<>();session.subscribe(events::add);byte[] before=session.captureSave();var token=session.state();
   var command=new ProductionCommand(token,"EQUIPMENT",10,1,"SPEAR");session.preview(command);check(events.isEmpty()&&Arrays.equals(before,session.captureSave()),"preview emits no point facts and preserves full save/RNG");
   var result=session.execute(command);check(result.ok()&&events.size()==1&&result.event.techniquePointsChanges.isEmpty(),"production keeps actual current zero-TP behavior; no invented reward");
   var view=session.legacyView();World direct=SaveCodec.decode(session.captureSave());int actor=view.draft.idle(view.draft.city(10)).get(0).id;Campaign.Tech tech=Campaign.Tech.SPEAR_DRILL;
   check(direct.campaign.research(10,actor,tech).ok,"normal research control");check(session.legacy(view.draft,()->view.draft.campaign.research(10,actor,tech)).ok,"actual compatibility command research");
   check(Arrays.equals(session.captureSave(),SaveCodec.encode(direct)),"facts leave entire command/save/RNG exact");var fact=events.get(events.size()-1);check(fact.techniquePointsChanges.size()==1,"one faction deduction fact");check(fact.id.equals(fact.state.sessionId+":"+fact.state.generation+":"+fact.state.revision+":"+fact.kind.name()),"dedup ID is the committed state boundary");var delta=fact.techniquePointsChanges.get(0);check(delta.owner==0&&delta.before==9000&&delta.after==9000-tech.points&&delta.delta==-tech.points,"research deduction contains immutable before/after");
   boolean immutable=false;try{fact.techniquePointsChanges.clear();}catch(UnsupportedOperationException e){immutable=true;}check(immutable,"fact collection immutable");
   var rejected=session.legacyView();int count=events.size();before=session.captureSave();check(!session.legacy(rejected.draft,()->rejected.draft.campaign.research(10,actor,tech)).ok&&count==events.size()&&Arrays.equals(before,session.captureSave()),"rejected command emits no fact and changes no save");
   var ticket=session.beginTurn();World computed=SaveCodec.decode(ticket.initial());check(computed.nextTurn().ok,"normal full turn");int[] prior={direct.campaign.points(0),direct.campaign.points(1)};check(session.commitTurn(ticket,computed),"actual turn commit");var turn=events.get(events.size()-1);check(turn.kind==GameEvent.Kind.TURN_COMMITTED,"turn fact boundary");for(var c:turn.techniquePointsChanges)check(c.before==prior[c.owner]&&c.after==computed.campaign.points(c.owner),"turn net actual values per faction");check(Arrays.equals(session.captureSave(),SaveCodec.encode(computed)),"turn fact leaves save/RNG exact");
   session.replace(initial);check(events.get(events.size()-1).techniquePointsChanges.isEmpty(),"load establishes baseline without synthetic loss/gain");check(delta.before==9000&&delta.after==9000-tech.points,"prior fact survives load unchanged");session.close();check(events.get(events.size()-1).techniquePointsChanges.isEmpty(),"close adds no reward");
  }
  World w=CityActionSessionTest.fixture();w.campaign.points.put(0,100);w.campaign.points.put(1,900);beforeJournal(w);
  System.out.println("PASS TechniquePointsFactsTest checks="+checks+" actual research/production/full turn, restore, immutable journal facts, full-save/RNG parity");
 }
 static void beforeJournal(World w)throws Exception{
  byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();TurnJournal journal=new TurnJournal(w);check(Arrays.equals(before,SaveCodec.encode(w)),"creating journal keeps save exact");w.campaign.earn(0,20);w.campaign.points.put(1,800);journal.checkpoint("actual rule values");var batch=journal.drainEvents();check(batch.size()==1&&batch.get(0).visibleAction()&&batch.get(0).techniquePointsChanges.size()==2,"point-only checkpoint is retained and visible");var first=batch.get(0).techniquePointsChanges.get(0);check(first.owner==0&&first.before==100&&first.after==120,"journal ordered faction scalar");boolean immutable=false;try{batch.get(0).techniquePointsChanges.clear();}catch(UnsupportedOperationException e){immutable=true;}check(immutable,"journal changes immutable");byte[] after=SaveCodec.encode(w);journal.checkpoint("no change");check(journal.drainEvents().isEmpty(),"unchanged checkpoint adds no duplicate");w.campaign.earn(0,10);journal.close();check(first.after==120&&journal.events().get(0).techniquePointsChanges.get(0).before==120,"later checkpoint does not mutate retained facts");World visual=SaveCodec.decode(after);before=SaveCodec.encode(w);for(var event:batch)event.applyVisual(visual);check(Arrays.equals(before,SaveCodec.encode(w))&&w.strategy.getRandomState()==rng,"replaying facts cannot change authority or RNG");
 }
}

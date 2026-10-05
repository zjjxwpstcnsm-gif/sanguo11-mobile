package game.sanguo.core;
import game.sanguo.api.*;import game.sanguo.runtime.*;import java.util.*;
public final class PcDelayedProductionSessionTest {
 static int checks;static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 static World fixture(){World w=PcProductionCrewSessionTest.fixture();for(int id:new int[]{1,2,3})OfficerAbilities.setBase(w.officer(id),2,80);return w;}
 public static void main(String[] args)throws Exception{
  for(String item:new String[]{"RAM","TOWER_SHIP"})try(GameSession game=new GameSession(fixture())){
   String op=item.equals("RAM")?"EQUIPMENT":"SHIP";int[] ids={1,2,3};var token=game.state();ProductionCommand command=new ProductionCommand(token,op,10,ids,item);List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);byte[] before=game.captureSave();var p=game.preview(command);check(p.allowed()&&p.effects.delayed&&p.effects.busyTurns==5&&p.effects.actors.size()==3,"normal source crew preview");check(p.effects.actors.stream().allMatch(a->a.meritAfter==a.meritBefore&&a.experience.requestedAmount==0),"start projection noXP/merit");check(Arrays.equals(before,game.captureSave())&&events.isEmpty(),"pure typed forecast");check(game.execute(command).ok()&&events.size()==1&&game.state().revision==token.revision+1,"one start event/revision");byte[] started=game.captureSave();check(game.execute(command).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(started,game.captureSave()),"double start no duplicate");World state=SaveCodec.decode(started);check(state.army.productions.get(0).officers().length==3,"saved full crew");game.replace(state);
   for(int turn=0;turn<5;turn++){var ticket=game.beginTurn();World next=SaveCodec.decode(ticket.initial());check(next.nextTurn().ok&&game.commitTurn(ticket,next),"normal full turn transaction");check(Arrays.equals(game.captureSave(),SaveCodec.encode(next)),"actual saved turn roundtrip");}
   World complete=SaveCodec.decode(game.captureSave());check(complete.army.productions.isEmpty()&&Arrays.stream(ids).allMatch(id->complete.officerAbilities.experience(id,2)==4),"one completion perstaff through APIturn");
  }
  World cancelled=fixture();check(cancelled.army.produce(10,new int[]{1,2,3},World.Weapon.RAM,null).ok&&cancelled.army.cancelProduction(3).ok,"cancel by any crew");check(cancelled.army.productions.isEmpty()&&cancelled.officerAbilities.experience(1,2)==0&&cancelled.domestic.remainingUses(10,Domestic.Kind.WORKSHOP)==1,"cancel clears reservation,no inventedcompletion");check(Arrays.stream(new int[]{1,2,3}).allMatch(id->cancelled.officer(id).otherTaskTurns==0),"cancel all tasks");check(Arrays.equals(SaveCodec.encode(cancelled),SaveCodec.encode(SaveCodec.decode(SaveCodec.encode(cancelled)))),"cancel codec exact");
  World lost=fixture();check(lost.army.produce(10,new int[]{1,2},World.Weapon.RAM,null).ok,"startlosscase");lost.city(10).owner=1;lost.army.cleanup();check(lost.army.productions.isEmpty()&&lost.officerAbilities.experience(1,2)==0&&lost.officer(2).otherTaskTurns==0,"loss abort no completion XP");
  System.out.println("PASS PcDelayedProductionSessionTest checks="+checks+" typednormalturns/cancel/loss/save; stagedonly");
 }
}

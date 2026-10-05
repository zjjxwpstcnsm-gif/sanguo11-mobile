package game.sanguo.core;
import game.sanguo.api.*;import game.sanguo.runtime.*;import java.util.*;
public final class PcProductionCrewSessionTest {
 static int checks;static void check(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
 static World fixture(){World w=ProductionSessionTest.fixture();w.officerAbilities.initializeOpening(null,false,false);w.merchantMarket.initializeOpening();w.pcProduction.initializeOpening();return w;}
 public static void main(String[] args)throws Exception{
  World w=fixture();for(int id:new int[]{1,2,3}){OfficerAbilities.setBase(w.officer(id),2,80-id*10);w.officerAbilities.gainExperience(id,2,99);}w.officer(3).skillId=Skill.NENGLI.id;
  try(GameSession game=new GameSession(w)){
   int[] ids={1,2,3};var token=game.state();ProductionCommand cmd=new ProductionCommand(token,"EQUIPMENT",10,ids,"SPEAR");ids[1]=999;check(Arrays.equals(cmd.officers(),new int[]{1,2,3}),"input defensive copy");int[] copy=cmd.officers();copy[0]=20;check(cmd.officerId==1&&cmd.officers()[0]==1,"returned array defensive copy");
   byte[] before=game.captureSave();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);var p=game.preview(cmd);check(p.allowed()&&p.effects.actors.size()==3,"typed preview includes all3 actors");for(int n=0;n<3;n++){var actor=p.effects.actors.get(n);check(actor.officerId==n+1&&actor.experience.experienceBefore==99&&actor.experience.experienceAfter==101&&actor.actedAfter,"typed each reward/action");}
   boolean immutable=false;try{p.effects.actors.clear();}catch(UnsupportedOperationException e){immutable=true;}check(immutable,"actor rows immutable");
   for(int n=0;n<3;n++)check(game.preview(cmd).effects.outputQuantity==p.effects.outputQuantity,"repeat query deterministic");check(Arrays.equals(before,game.captureSave())&&token.equals(game.state())&&events.isEmpty(),"preview authority/token/save/RNG unchanged");
   World expected=SaveCodec.decode(before);check(expected.produce(10,new int[]{1,2,3},World.Weapon.SPEAR).ok,"normal rules direct");var result=game.execute(cmd);check(result.ok()&&game.state().revision==token.revision+1&&events.size()==1&&result.event.kind==GameEvent.Kind.PRODUCTION_COMMITTED,"one revision/committed fact");check(Arrays.equals(game.captureSave(),SaveCodec.encode(expected)),"API commits same full normal crew/save/RNG");byte[] after=game.captureSave();check(game.execute(cmd).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(after,game.captureSave())&&events.size()==1,"duplicate never double rewards");
   game.replace(SaveCodec.decode(before));check(game.preview(cmd).error==CommandResult.Error.STALE_SESSION,"load expires draft");for(int[] bad:new int[][]{{},{1,1},{1,2,3,0},{1,999},{1,20}}){byte[] original=game.captureSave();var t=game.state();var c=new ProductionCommand(t,"EQUIPMENT",10,bad,"SPEAR");var rejected=game.preview(c);var executed=game.execute(c);check(!rejected.allowed()&&executed.error==CommandResult.Error.RULE_REJECTED&&rejected.reasonCode.equals(executed.reasonCode),"typed invalid crew rejection");check(Arrays.equals(original,game.captureSave())&&t.equals(game.state()),"invalid crew authority unchanged");}
   var busy=game.beginTurn();var c=new ProductionCommand(game.state(),"EQUIPMENT",10,new int[]{1,2,3},"SPEAR");check(game.preview(c).error==CommandResult.Error.HOST_BUSY&&game.execute(c).error==CommandResult.Error.HOST_BUSY,"turn ownership barrier");game.cancelTurn(busy);
   for(int turn=0;turn<3;turn++){var ticket=game.beginTurn();World state=SaveCodec.decode(ticket.initial());check(state.nextTurn().ok&&game.commitTurn(ticket,state),"real turn transaction native profile");check(SaveCodec.decode(game.captureSave()).pcProduction.enabled(),"turn/load source policy stays persisted");}
  }
  System.out.println("PASS PcProductionCrewSessionTest checks="+checks+" normal typed crew/StateToken/save/events; staging only");
 }
}

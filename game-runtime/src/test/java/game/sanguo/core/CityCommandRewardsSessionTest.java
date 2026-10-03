package game.sanguo.core;
import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;

public final class CityCommandRewardsSessionTest {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static World fixture(int stat){World w=ProductionSessionTest.fixture();w.officerAbilities.initializeOpening(null,false,false);w.officerAbilities.gainExperience(1,stat,98);w.government.earn(1,59999);return w;}
 public static void main(String[] args)throws Exception{
  String[] operations={"PATROL","TRAIN","RECRUIT"};int[] stats={0,1,4};
  for(int n=0;n<operations.length;n++)try(GameSession game=new GameSession(fixture(stats[n]))){
   String operation=operations[n];int stat=stats[n];byte[] before=game.captureSave();StateToken token=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
   CityActionCommand command=new CityActionCommand(token,operation,10,1,new int[0]);var p=game.preview(command);check(p.allowed(),p.detail);
   var e=p.effects.officers.stream().filter(o->o.id==1).findFirst().get();
   check(e.experience.managed&&e.experience.stat==stat&&e.experience.experienceBefore==98&&e.experience.experienceAfter==100&&e.experience.currentBefore==80&&e.experience.currentAfter==81,"typed fixed XP/current forecast");
   check(e.meritBefore==59999&&e.meritAfter==60000,"typed native merit cap");
   for(int i=0;i<3;i++)game.preview(command);check(Arrays.equals(before,game.captureSave())&&events.isEmpty()&&token.equals(game.state()),"query/save/RNG/events/token purity");
   World direct=SaveCodec.decode(before);check(direct.strategy.executeCityAction(CityActionPlan.Operation.valueOf(operation),10,1,new int[0]).ok,"ordinary core action");
   if(n!=1)try(GameSession legacy=new GameSession(SaveCodec.decode(before))){check(legacy.execute(new GameCommand(n==0?GameCommand.Operation.PATROL:GameCommand.Operation.RECRUIT,legacy.state(),10,1)).ok()&&Arrays.equals(legacy.captureSave(),SaveCodec.encode(direct)),"legacy live entry shares fixed reward with typed entry");}
   check(game.execute(command).ok()&&Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"actual typed command/save/RNG equals ordinary source reward");
   check(events.size()==1&&events.get(0).kind==GameEvent.Kind.CITY_ACTION_COMMITTED&&game.state().revision==token.revision+1,"exactly one authoritative fact and revision");
   byte[] committed=game.captureSave();check(game.execute(command).error==CommandResult.Error.STALE_REVISION&&game.preview(command).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(committed,game.captureSave())&&events.size()==1,"duplicate never re-awards");
   var fresh=new CityActionCommand(game.state(),operation,10,1,new int[0]);check(!game.preview(fresh).allowed()&&!game.execute(fresh).ok()&&Arrays.equals(committed,game.captureSave()),"fresh unavailable actor gains no reward");
   for(int i=0;i<3;i++){var ticket=game.beginTurn();World calculated=SaveCodec.decode(ticket.initial());check(calculated.nextTurn().ok&&direct.nextTurn().ok&&game.commitTurn(ticket,calculated)&&Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"full turn and save continuation retain rewards");}
  }
  for(String item:new String[]{"SPEAR","HALBERD","CROSSBOW","CAVALRY"})try(GameSession game=new GameSession(fixture(2))){
   byte[] before=game.captureSave();StateToken token=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
   ProductionCommand command=new ProductionCommand(token,"EQUIPMENT",10,1,item);var p=game.preview(command);check(p.allowed(),p.detail);var e=p.effects.experience;
   check(e.managed&&e.stat==2&&e.experienceBefore==98&&e.experienceAfter==100&&e.currentAfter==81&&p.effects.meritAfter==60000,"typed immediate production XP/intelligence/merit");
   for(int i=0;i<3;i++)game.preview(command);check(Arrays.equals(before,game.captureSave())&&events.isEmpty()&&token.equals(game.state()),"production preview pure");
   World direct=SaveCodec.decode(before);check(direct.produce(10,1,World.Weapon.valueOf(item)).ok,"normal production call");
   check(game.execute(command).ok()&&Arrays.equals(game.captureSave(),SaveCodec.encode(direct))&&events.size()==1&&events.get(0).kind==GameEvent.Kind.PRODUCTION_COMMITTED&&game.state().revision==token.revision+1,"production typed and normal command share one exact reward/fact/revision");
   byte[] after=game.captureSave();check(game.execute(command).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(after,game.captureSave())&&events.size()==1,"production duplicate rejected without reward");
  }
  try(GameSession game=new GameSession(fixture(0))){
   var command=new CityActionCommand(game.state(),"PATROL",10,1,new int[0]);var ticket=game.beginTurn();byte[] before=game.captureSave();check(game.preview(command).error==CommandResult.Error.HOST_BUSY&&game.execute(command).error==CommandResult.Error.HOST_BUSY&&Arrays.equals(before,game.captureSave()),"busy host cannot award");game.cancelTurn(ticket);
   game.replace(fixture(0));check(game.preview(command).error==CommandResult.Error.STALE_SESSION&&game.execute(command).error==CommandResult.Error.STALE_SESSION,"old-session token cannot award");
  }
  System.out.println("PASS CityCommandRewardsSessionTest checks="+checks+" fixed source rewards, normal entries, typed effects, state/events/save boundaries");
 }
}

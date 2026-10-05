package game.sanguo.core;
import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
public final class CityActionSessionTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static World fixture(){
  World w=new World(24,20);w.cities.add(new World.City(10,"甲",new Hex(4,4),0));w.cities.add(new World.City(20,"乙",new Hex(18,14),1));
  for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,80,80,80,90,80));w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));w.strategy.initializeOffices();w.city(10).order=70;w.city(10).morale=40;w.city(10).gold=30000;
  var h=w.domestic.buildSites(10).get(0);w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,Domestic.Kind.BARRACKS,h,-1,0));w.strategy.setSeed(20261003L);return w;
 }
 static int[] targets(String op){return op.equals("REWARD")?new int[]{2,3}:op.equals("APPOINT_GOVERNOR")?new int[]{2}:new int[0];}
 static World.Result ordinary(World w,String op,int[] t){return switch(op){case "PATROL"->w.strategy.patrol(10,1);case "TRAIN"->w.strategy.trainArmy(10,1);case "RECRUIT"->w.strategy.recruitSoldiers(10,1);case "SEARCH"->w.strategy.search(10,1);case "REWARD"->w.strategy.rewardOfficers(10,1,t);default->w.strategy.appointGovernor(10,1,t[0]);};}
 public static void main(String[] args)throws Exception{
  for(var op:CityActionPlan.Operation.values())try(GameSession game=new GameSession(fixture())){
   StateToken token=game.state();byte[] before=game.captureSave();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
   int[] targets=targets(op.name());CityActionCommand c=new CityActionCommand(token,op.name(),10,1,targets);
   if(targets.length>0){targets[0]=999;c.targets()[0]=998;check(c.targets()[0]==2,"command owns defensive selection");}
   CityActionPreview p=game.preview(c);check(p.allowed(),p.detail);
   for(int n=0;n<5;n++)check(game.preview(c).effects.orderAfter==p.effects.orderAfter,"repeat cached forecast");
   check(Arrays.equals(before,game.captureSave())&&events.isEmpty()&&token.equals(game.state()),"preview is pure across full save/RNG/events/revision");
   try{p.effects.officers.clear();throw new AssertionError("mutable effects");}catch(UnsupportedOperationException expected){checks++;}
   World direct=SaveCodec.decode(before);check(ordinary(direct,op.name(),c.targets()).ok,"normal core command");
   if(op==CityActionPlan.Operation.PATROL||op==CityActionPlan.Operation.RECRUIT)try(GameSession legacy=new GameSession(SaveCodec.decode(before))){
    var kind=op==CityActionPlan.Operation.PATROL?GameCommand.Operation.PATROL:GameCommand.Operation.RECRUIT;
    check(legacy.execute(new GameCommand(kind,legacy.state(),10,1)).ok(),"existing normal GameCommand executes new native AP rule");
    check(Arrays.equals(legacy.captureSave(),SaveCodec.encode(direct)),"legacy and typed pathways share exact state/RNG");
   }
   CommandResult result=game.execute(c);check(result.ok(),result.detail);
   check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"typed command matches ordinary full save/RNG "+op);
   check(result.event.kind==GameEvent.Kind.CITY_ACTION_COMMITTED&&events.size()==1&&result.state.revision==token.revision+1,"one authoritative commit/event");
   CityActionCommand fresh=new CityActionCommand(game.state(),op.name(),10,1,c.targets());var next=game.preview(fresh);
   check(!next.allowed()&&next.reasonCode.equals("LEADER_UNAVAILABLE")&&next.resources.goldAvailable==direct.city(10).gold,"query cache invalidated after real debit");
   byte[] after=game.captureSave();check(game.preview(c).error==CommandResult.Error.STALE_REVISION&&game.execute(c).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(after,game.captureSave()),"no double spending");
   for(int n=0;n<3;n++){TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());check(computed.nextTurn().ok&&direct.nextTurn().ok&&game.commitTurn(ticket,computed),"ordinary full turn");check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"multi-turn/save RNG parity");}
  }
  try(GameSession game=new GameSession(fixture())){
   StateToken token=game.state();byte[] before=game.captureSave();
   for(String op:new String[]{null,"bad","PATROL","REWARD","APPOINT_GOVERNOR"}){
    var c=new CityActionCommand(token,op,10,1,new int[]{999,999});var p=game.preview(c);var result=game.execute(c);
    check(!p.allowed()&&p.error==CommandResult.Error.RULE_REJECTED&&p.reasonCode.equals(result.reasonCode)&&p.detail.equals(result.detail)&&p.effects==null&&p.search==null,"same structured error without fictional effects");
    check(Arrays.equals(before,game.captureSave())&&token.equals(game.state()),"rejected request atomic");
   }
   var c=new CityActionCommand(token,"SEARCH",10,1,new int[0]);TurnTicket ticket=game.beginTurn();
   check(game.preview(c).error==CommandResult.Error.HOST_BUSY&&game.preview(c).resources==null&&game.execute(c).error==CommandResult.Error.HOST_BUSY,"turn in progress blocks city command");game.cancelTurn(ticket);
   AtomicReference<Throwable> wrong=new AtomicReference<>();Thread t=new Thread(()->{try{game.preview(c);}catch(Throwable e){wrong.set(e);}});t.start();t.join();check(wrong.get() instanceof IllegalStateException,"logic thread enforced");
   game.replace(fixture());check(game.preview(c).error==CommandResult.Error.STALE_SESSION&&game.execute(c).error==CommandResult.Error.STALE_SESSION,"replacement expires old requests");
   game.close();check(game.preview(c).error==CommandResult.Error.CLOSED&&game.execute(c).error==CommandResult.Error.CLOSED,"closed session rejects");
  }
  System.out.println("PASS CityActionSessionTest checks="+checks);
 }
}

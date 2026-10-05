package game.sanguo.core;
import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
public final class TradeSessionTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  for(String op:new String[]{"BUY","SELL"})try(GameSession game=new GameSession(CityActionSessionTest.fixture())){
   byte[] before=game.captureSave();StateToken token=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
   TradeCommand c=new TradeCommand(token,op,10,1,5000);TradePreview p=game.preview(c);check(p.allowed(),p.detail);
   for(int n=0;n<5;n++)check(game.preview(c).quote.quotedGold==p.quote.quotedGold,"repeat quote");
   check(Arrays.equals(before,game.captureSave())&&token.equals(game.state())&&events.isEmpty(),"no RNG save revision or event effects");
   World direct=SaveCodec.decode(before);check(direct.campaign.trade(10,1,op.equals("BUY"),5000).ok,"normal trade");
   var r=game.execute(c);check(r.ok()&&r.state.revision==token.revision+1&&events.size()==1&&r.event.kind==GameEvent.Kind.TRADE_COMMITTED,"one committed fact");
   check(Arrays.equals(SaveCodec.encode(direct),game.captureSave()),"typed and normal save/RNG parity");
   var fresh=game.preview(new TradeCommand(game.state(),op,10,2,5000));check(fresh.quote.tradedBefore==5000&&fresh.quote.goldBefore==p.effects.goldAfter&&fresh.quote.foodBefore==p.effects.foodAfter,"cache invalidated by commit");
   byte[] used=game.captureSave();StateToken usedToken=game.state();
   TradeCommand repeat=new TradeCommand(usedToken,op.equals("BUY")?"SELL":"BUY",10,2,1000);
   var blocked=game.preview(repeat);var denied=game.execute(repeat);
   check(!blocked.allowed()&&blocked.reasonCode.equals("TRADE_USED")&&blocked.field.equals("city")&&blocked.quote.quotaRemaining==0&&blocked.effects==null,"native city use authoritative in preview");
   check(denied.reasonCode.equals(blocked.reasonCode)&&denied.field.equals(blocked.field)&&usedToken.equals(game.state())&&events.size()==1&&Arrays.equals(used,game.captureSave()),"typed second actor/op cannot commit or consume RNG");
   byte[] after=game.captureSave();check(game.execute(c).error==CommandResult.Error.STALE_REVISION&&game.preview(c).error==CommandResult.Error.STALE_REVISION&&events.size()==1&&Arrays.equals(after,game.captureSave()),"no duplicate debit");
   for(int n=0;n<3;n++){TurnTicket t=game.beginTurn();World computed=SaveCodec.decode(t.initial());check(computed.nextTurn().ok&&direct.nextTurn().ok&&game.commitTurn(t,computed),"normal full turn");check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"continuation equality");}
  }
  try(GameSession game=new GameSession(CityActionSessionTest.fixture())){
   byte[] before=game.captureSave();StateToken token=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
   for(String op:new String[]{null,"missing","BUY","SELL"})for(int food:new int[]{-1,999,20001,Integer.MAX_VALUE}){
    TradeCommand c=new TradeCommand(token,op,10,1,food);var p=game.preview(c);var r=game.execute(c);
    check(!p.allowed()&&p.effects==null&&p.quote.requestedFood==food&&p.quote.step==1000,"bad input retained with limits");
    check(r.reasonCode.equals(p.reasonCode)&&r.field.equals(p.field)&&r.detail.equals(p.detail),"structured same rejection");
    check(Arrays.equals(before,game.captureSave())&&token.equals(game.state())&&events.isEmpty(),"reject atomic");
   }
   var c=new TradeCommand(token,"BUY",10,1,1000);TurnTicket t=game.beginTurn();check(game.preview(c).error==CommandResult.Error.HOST_BUSY&&game.execute(c).error==CommandResult.Error.HOST_BUSY,"serialized turn");game.cancelTurn(t);
   AtomicReference<Throwable> wrong=new AtomicReference<>();Thread worker=new Thread(()->{try{game.preview(c);}catch(Throwable e){wrong.set(e);}});worker.start();worker.join();check(wrong.get() instanceof IllegalStateException,"thread confined");
   game.replace(CityActionSessionTest.fixture());check(game.preview(c).error==CommandResult.Error.STALE_SESSION&&game.execute(c).error==CommandResult.Error.STALE_SESSION,"load invalidates");
   game.close();check(game.preview(c).error==CommandResult.Error.CLOSED&&game.execute(c).error==CommandResult.Error.CLOSED,"close rejects");
  }
  for(var kind:new World.SiteKind[]{World.SiteKind.GATE,World.SiteKind.PORT}){
   World sites=CityActionSessionTest.fixture();var site=new World.City(30,"关港",new Hex(4,14),0);site.kind=kind;site.gold=5000;site.food=50000;sites.cities.add(site);sites.officer(2).cityId=30;
   try(GameSession game=new GameSession(sites)){
    byte[] before=game.captureSave();StateToken token=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
    for(String op:new String[]{"BUY","SELL"}){
     var c=new TradeCommand(token,op,30,2,1000);var p=game.preview(c);var r=game.execute(c);
     check(!p.allowed()&&p.reasonCode.equals("TRADE_SITE")&&p.field.equals("city")&&p.effects==null&&p.quote.availableMaximum==0,"typed original site restriction");
     check(r.reasonCode.equals(p.reasonCode)&&r.detail.equals(p.detail)&&Arrays.equals(before,game.captureSave())&&token.equals(game.state())&&events.isEmpty(),"site rejection has no revision/event/save/RNG effects");
    }
   }
  }
  for(String op:new String[]{"BUY","SELL"})for(int merit:new int[]{0,59949,59950,59999,60000,60001,1000000}){
   World world=CityActionSessionTest.fixture();if(merit>0)world.government.merits.put(1,merit);
   try(GameSession game=new GameSession(world)){
    byte[] before=game.captureSave();var token=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
    var c=new TradeCommand(token,op,10,1,1000);var p=game.preview(c);int expected=merit>=60000?merit:Math.min(60000,merit+50);
    check(p.allowed()&&p.effects.meritBefore==merit&&p.effects.meritAfter==expected,"typed native50/cap60000 forecast preserves old excess");
    check(Arrays.equals(before,game.captureSave())&&token.equals(game.state())&&events.isEmpty(),"merit forecast pure");
    var r=game.execute(c);World after=SaveCodec.decode(game.captureSave());
    check(r.ok()&&after.government.merit(1)==expected&&after.officer(1).acted&&events.size()==1&&r.state.revision==token.revision+1,"typed exact merit and one authoritative event");
    byte[] saved=game.captureSave();check(!game.execute(c).ok()&&Arrays.equals(saved,game.captureSave())&&events.size()==1,"duplicate cannot award merit twice");
   }
  }
  for(String op:new String[]{"BUY","SELL"}){
   World w=CityActionSessionTest.fixture();w.officerAbilities.initializeOpening(null,false,false);w.officerAbilities.gainExperience(1,3,95);
   try(GameSession game=new GameSession(w)){
    byte[] before=game.captureSave();StateToken state=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
    TradeCommand command=new TradeCommand(state,op,10,1,1000);TradePreview preview=game.preview(command);
    check(preview.allowed()&&preview.effects.abilityStateManaged&&preview.effects.politicsExperienceBefore==95&&preview.effects.politicsExperienceAfter==100,"typed XP95 to100 forecast");
    check(preview.effects.politicsAfter==preview.effects.politicsBefore+1&&Arrays.equals(before,game.captureSave())&&events.isEmpty()&&state.equals(game.state()),"typed ability preview pure");
    World direct=SaveCodec.decode(before);check(direct.campaign.trade(10,1,op.equals("BUY"),1000).ok,"ordinary managed command");
    var result=game.execute(command);World after=SaveCodec.decode(game.captureSave());
    check(result.ok()&&Arrays.equals(SaveCodec.encode(direct),game.captureSave())&&after.officerAbilities.experience(1,3)==100&&after.officer(1).politics==preview.effects.politicsAfter,"typed authority matches ordinary save/RNG");
    check(events.size()==1&&result.event.kind==GameEvent.Kind.TRADE_COMMITTED&&game.state().revision==state.revision+1,"one visual event and revision for complete commit");
    byte[] committed=game.captureSave();check(!game.execute(command).ok()&&Arrays.equals(committed,game.captureSave())&&events.size()==1,"stale cannot gain XP twice");
   }
  }
  System.out.println("PASS TradeSessionTest checks="+checks);
 }
}

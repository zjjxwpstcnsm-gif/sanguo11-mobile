package game.sanguo.core;
import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
public final class ProductionSessionTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static World fixture(){
  World w=CityActionSessionTest.fixture();for(var kind:new Domestic.Kind[]{Domestic.Kind.SMITH,Domestic.Kind.STABLE,Domestic.Kind.WORKSHOP,Domestic.Kind.SHIPYARD}){
   var h=w.domestic.buildSites(10).get(0);if(kind==Domestic.Kind.SHIPYARD){var water=h.neighbors().stream().filter(n->w.inside(n)&&w.cityAt(n)==null&&w.domestic.at(n)==null).findFirst().orElseThrow();w.terrain[water.q][water.r]=World.Terrain.WATER;}w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,kind,h,-1,0));
  }w.campaign.learned.put(0,EnumSet.allOf(Campaign.Tech.class));w.campaign.learned.get(0).remove(Campaign.Tech.WARSHIP);return w;
 }
 public static void main(String[] args)throws Exception{
  for(String op:new String[]{"EQUIPMENT","SHIP"})for(String item:op.equals("SHIP")?new String[]{"TOWER_SHIP","WARSHIP"}:new String[]{"SPEAR","HALBERD","CROSSBOW","CAVALRY","RAM","SIEGE_TOWER","WOODEN_BEAST","CATAPULT"})try(GameSession game=new GameSession(fixture())){
   byte[] before=game.captureSave();StateToken token=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);ProductionCommand c=new ProductionCommand(token,op,10,1,item);var p=game.preview(c);check(p.allowed(),item+":"+p.detail);
   for(int n=0;n<3;n++)check(game.preview(c).effects.outputQuantity==p.effects.outputQuantity,"repeat pure forecast");check(Arrays.equals(before,game.captureSave())&&events.isEmpty()&&token.equals(game.state()),"full purity");
   World direct=SaveCodec.decode(before);var result=op.equals("SHIP")?direct.army.produce(10,1,null,Army.Ship.valueOf(item)):direct.produce(10,1,World.Weapon.valueOf(item));check(result.ok,"ordinary manufacture");var r=game.execute(c);
   check(r.ok()&&r.state.revision==token.revision+1&&events.size()==1&&r.event.kind==GameEvent.Kind.PRODUCTION_COMMITTED,"one event and commit");check(Arrays.equals(SaveCodec.encode(direct),game.captureSave()),"same normal command save/RNG");
   byte[] after=game.captureSave();check(game.execute(c).error==CommandResult.Error.STALE_REVISION&&game.preview(c).error==CommandResult.Error.STALE_REVISION&&events.size()==1&&Arrays.equals(after,game.captureSave()),"duplicate rejects");
   var fresh=game.preview(new ProductionCommand(game.state(),op,10,2,item));check(fresh.resources.goldAvailable==p.effects.goldAfter&&fresh.resources.facilityUsesBefore==p.effects.facilityUsesAfter,"query cache follows commit");
   for(int n=0;n<4;n++){var t=game.beginTurn();World computed=SaveCodec.decode(t.initial());check(computed.nextTurn().ok&&direct.nextTurn().ok&&game.commitTurn(t,computed),"full normal turn");check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"continuation exact");}
  }
  try(GameSession game=new GameSession(fixture())){
   byte[] before=game.captureSave();StateToken token=game.state();
   for(String op:new String[]{null,"bad","EQUIPMENT","SHIP"})for(String item:new String[]{null,"bad","SWORD","BOAT"}){
    var c=new ProductionCommand(token,op,10,1,item);var p=game.preview(c);var r=game.execute(c);check(!p.allowed()&&p.effects==null&&p.reasonCode.equals(r.reasonCode)&&p.detail.equals(r.detail),"structured rejection");check(Arrays.equals(before,game.captureSave())&&token.equals(game.state()),"reject atomic");
   }
   var c=new ProductionCommand(token,"EQUIPMENT",10,1,"SPEAR");var t=game.beginTurn();check(game.preview(c).error==CommandResult.Error.HOST_BUSY&&game.execute(c).error==CommandResult.Error.HOST_BUSY,"turn busy");game.cancelTurn(t);
   AtomicReference<Throwable> wrong=new AtomicReference<>();Thread worker=new Thread(()->{try{game.preview(c);}catch(Throwable e){wrong.set(e);}});worker.start();worker.join();check(wrong.get() instanceof IllegalStateException,"thread confined");game.replace(fixture());check(game.execute(c).error==CommandResult.Error.STALE_SESSION&&game.preview(c).error==CommandResult.Error.STALE_SESSION,"load expires intent");game.close();check(game.execute(c).error==CommandResult.Error.CLOSED,"closed session");
  }
  System.out.println("PASS ProductionSessionTest checks="+checks);
 }
}

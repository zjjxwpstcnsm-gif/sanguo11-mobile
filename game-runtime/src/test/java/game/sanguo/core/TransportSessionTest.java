package game.sanguo.core;
import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;
public final class TransportSessionTest {
 static int checks;
 static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static World fixture()throws Exception{
  World w=new World(24,20);w.cities.add(new World.City(10,"A",new Hex(4,4),0));w.cities.add(new World.City(11,"B",new Hex(14,4),0));w.cities.add(new World.City(20,"E",new Hex(20,17),1));
  for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"O"+i,0,10,80,80,80,90,80));w.officers.add(new World.Officer(20,"E",1,20,80,80,80,80,80));
  w.city(10).ships[0]=3;w.city(10).ships[1]=2;w.strategy.initializeOffices();return SaveCodec.decode(SaveCodec.encode(w));
 }
 static TransportCommand command(StateToken t,boolean sea,boolean returning){return new TransportCommand(t,10,11,0,new int[]{1,2},700,10000,2000,new int[4],sea,returning,new int[]{2,1});}
 public static void main(String[] args)throws Exception{
  for(boolean sea:new boolean[]{false,true})for(boolean returning:new boolean[]{false,true})try(GameSession game=new GameSession(fixture())){
   byte[] before=game.captureSave();StateToken t=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);TransportCommand c=command(t,sea,returning);
   c.deputies()[0]=999;c.ships()[0]=999;c.equipment()[0]=999;
   TransportPreview p=game.preview(c);check(p.allowed(),p.detail);check(game.preview(c).forecast.foodPerTurn==p.forecast.foodPerTurn&&Arrays.equals(before,game.captureSave())&&events.isEmpty(),"cached preview and array copies pure");
   try{p.resources.stocks.clear();throw new AssertionError("mutable DTO");}catch(UnsupportedOperationException expected){checks++;}
   World direct=SaveCodec.decode(before);check(direct.domestic.transport(10,11,0,c.deputies(),c.gold,c.food,c.troops,c.equipment(),sea,returning,c.ships()).ok,"ordinary command");
   CommandResult r=game.execute(c);check(r.ok(),r.detail);check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"typed is byte-identical to ordinary command");check(events.size()==1&&r.state.revision==t.revision+1&&r.event.kind==GameEvent.Kind.TRANSPORT_DISPATCHED&&r.event.troopsDelta==-2000,"one event and one revision with source debit");
   byte[] saved=game.captureSave();check(game.preview(c).error==CommandResult.Error.STALE_REVISION&&game.execute(c).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(saved,game.captureSave()),"double submit rejected");
   check(!game.preview(command(game.state(),sea,returning)).allowed(),"committed state invalidates query cache");
   for(int n=0;n<6;n++){TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());check(computed.nextTurn().ok&&direct.nextTurn().ok&&game.commitTurn(ticket,computed),"real complete turn");check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"whole save/RNG replay through session");}
  }
  GameSession game=new GameSession(fixture());StateToken t=game.state();TransportCommand c=command(t,false,false);byte[] before=game.captureSave();
  TransportCommand bad=new TransportCommand(t,10,11,0,new int[]{0},700,10000,2000,new int[4],false,false,new int[2]);
  check(game.preview(bad).reasonCode.equals("CREW_DUPLICATE")&&game.execute(bad).reasonCode.equals("CREW_DUPLICATE")&&Arrays.equals(before,game.captureSave()),"structured failure and atomic refusal");
  TurnTicket ticket=game.beginTurn();check(game.preview(c).error==CommandResult.Error.HOST_BUSY&&game.execute(c).error==CommandResult.Error.HOST_BUSY,"host busy rejects transport");game.cancelTurn(ticket);
  game.replace(fixture());check(game.preview(c).error==CommandResult.Error.STALE_SESSION&&game.execute(c).error==CommandResult.Error.STALE_SESSION,"world replacement rejects old command");
  TransportCommand current=command(game.state(),false,false);game.close();check(game.preview(current).error==CommandResult.Error.CLOSED&&game.execute(current).error==CommandResult.Error.CLOSED,"closed host rejects command");
  System.out.println("PASS TransportSessionTest checks="+checks);
 }
}

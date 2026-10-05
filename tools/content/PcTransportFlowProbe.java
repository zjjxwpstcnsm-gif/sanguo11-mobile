import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
/** Production campaign data, unmodified resources, normal commands and full turns. */
public final class PcTransportFlowProbe {
 static int checks;
 static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args[0]);Files.createDirectories(out);int dispatched=0;
  for(var row:ScenarioCatalog.summaries()){
   World initial=ScenarioCatalog.load(row.id,0,20261003L);GameSession game=new GameSession(initial);byte[] before=game.captureSave();StateToken token=game.state();TransportCommand selected=null;TransportPreview plan=null;int rejected=0;
   search:for(World.City city:initial.cities)if(city.owner==initial.player){
    var idle=initial.idle(city);if(idle.isEmpty())continue;
    var destinations=new ArrayList<World.City>();for(var d:initial.cities)if(d.owner==initial.player&&d.id!=city.id)destinations.add(d);
    destinations.sort(Comparator.comparingInt(d->city.hex.distance(d.hex)));
    for(var destination:destinations){
     TransportCommand c=new TransportCommand(token,city.id,destination.id,idle.get(0).id,new int[0],100,3000,1000,new int[4],true,false,new int[]{Math.min(1,city.ships[0]),Math.min(1,city.ships[1])});
     long started=System.nanoTime();TransportPreview p=game.preview(c);double millis=(System.nanoTime()-started)/1e6;
     check(Arrays.equals(before,game.captureSave())&&token.equals(game.state()),"full production query pure");
     if(p.allowed()){selected=c;plan=p;System.out.println("PREVIEW "+row.id+" millis="+millis+" eta="+p.forecast.turns+" food="+p.forecast.foodPerTurn);break search;}
     check(p.error==CommandResult.Error.RULE_REJECTED,"real rule reason instead of hidden host failure "+p.reasonCode);rejected++;
    }
   }
   if(selected==null){System.out.println("UNAVAILABLE "+row.id+" rejected="+rejected+"; no available transport in current player startup state");game.close();continue;}
   World direct=SaveCodec.decode(before);check(direct.domestic.transport(selected.sourceCityId,selected.targetCityId,selected.officerId,selected.deputies(),selected.gold,selected.food,selected.troops,selected.equipment(),selected.sea,selected.returnOfficers,selected.ships()).ok,"ordinary production departure");
   check(game.execute(selected).ok(),"typed production departure");byte[] departed=game.captureSave();check(Arrays.equals(departed,SaveCodec.encode(direct)),"production typed/ordinary save equality");
   check(game.execute(selected).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(departed,game.captureSave()),"duplicate cannot dispatch again");
   check(direct.domestic.foodUse(direct.domestic.missions.get(direct.domestic.missions.size()-1))==plan.forecast.foodPerTurn,"real initial ration");
   for(int n=0;n<6;n++){
    TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());check(computed.nextTurn().ok&&direct.nextTurn().ok&&game.commitTurn(ticket,computed),"ordinary full production turn");
    byte[] saved=game.captureSave();check(Arrays.equals(saved,SaveCodec.encode(direct)),"production save/RNG replay");game.close();game=new GameSession(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"reopen session after each turn");
   }
   byte[] saved=game.captureSave();Files.write(out.resolve(row.id+"-transport.sg11"),saved);System.out.println("PASS "+row.id+" source="+selected.sourceCityId+" target="+selected.targetCityId+" ships="+Arrays.toString(selected.ships())+" sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(saved)));dispatched++;game.close();
  }
  check(dispatched>0,"at least one real campaign transport executed");System.out.println("PASS checks="+checks+" dispatched="+dispatched+"; production engine evidence, not PC-parity or Android UI proof");
 }
}

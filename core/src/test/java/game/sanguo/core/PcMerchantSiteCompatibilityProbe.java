package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Real port-merchant save from pre-rule core, then strict non-destructive migration check. */
public final class PcMerchantSiteCompatibilityProbe {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path path=Path.of(args[1]);
  if(args[0].equals("write-legacy")){
   World w=new World(24,20);w.cities.add(new World.City(10,"A",new Hex(4,4),0));w.cities.add(new World.City(20,"B",new Hex(18,14),1));
   var port=new World.City(30,"Port",new Hex(4,14),0);port.kind=World.SiteKind.PORT;port.gold=5000;port.food=50000;w.cities.add(port);
   w.officers.add(new World.Officer(0,"A",0,10,80,80,80,90,80));w.officers.add(new World.Officer(20,"B",1,20,80,80,80,80,80));
   for(int id=30;id<33;id++)w.officers.add(new World.Officer(id,"P"+id,0,30,80,80,80,90,80));w.strategy.initializeOffices();w.strategy.setSeed(20261003L);
   check(w.campaign.trade(30,30,true,1000).ok&&w.campaign.trade(30,31,false,1000).ok,"frozen core executes real old port orders");
   check(w.campaign.traded(30)==2000,"old port ledger");Files.write(path,SaveCodec.encode(w));
  }else if(args[0].equals("read-candidate")){
   byte[] original=Files.readAllBytes(path);World w=SaveCodec.decode(original);check(Arrays.equals(original,SaveCodec.encode(w)),"old port byte roundtrip");
   for(var op:TradePlan.Operation.values()){
    var p=w.campaign.previewTrade(30,32,op,1000);check(p.failure.code.equals("TRADE_SITE")&&p.effects==null,"old port uses original site restriction");
    check(!w.campaign.trade(30,32,op==TradePlan.Operation.BUY,1000).ok&&Arrays.equals(original,SaveCodec.encode(w)),"port reject never changes old balance, use, or RNG");
   }
   World replay=SaveCodec.decode(original);
   for(int turn=0;turn<3;turn++){
    check(w.nextTurn().ok&&replay.nextTurn().ok&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"old port three normal turns save/RNG exact");
    byte[] before=SaveCodec.encode(w);check(!w.campaign.trade(30,32,true,1000).ok&&Arrays.equals(before,SaveCodec.encode(w)),"next turn still rejects port transaction without mutation");
   }
  }else throw new IllegalArgumentException(args[0]);
  System.out.println("PASS port "+args[0]);
 }
}

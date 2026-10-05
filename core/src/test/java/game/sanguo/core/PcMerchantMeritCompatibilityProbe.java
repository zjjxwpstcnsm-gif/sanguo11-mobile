package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Compiles against the frozen old core: its actual100-merit transaction creates this save. */
public final class PcMerchantMeritCompatibilityProbe {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path path=Path.of(args[1]);
  if(args[0].equals("write-legacy")){
   World w=new World(24,20);w.cities.add(new World.City(10,"A",new Hex(4,4),0));w.cities.add(new World.City(20,"B",new Hex(18,14),1));
   w.officers.add(new World.Officer(0,"A",0,10,80,80,80,90,80));w.officers.add(new World.Officer(1,"Merchant",0,10,80,80,80,90,80));w.officers.add(new World.Officer(20,"B",1,20,80,80,80,80,80));
   w.strategy.initializeOffices();w.strategy.setSeed(20261003L);w.government.merits.put(1,59990);
   check(w.campaign.trade(10,1,true,1000).ok&&w.government.merit(1)==60090,"frozen real100-merit award above native cap");
   Files.write(path,SaveCodec.encode(w));
  }else if(args[0].equals("read-candidate")){
   byte[] bytes=Files.readAllBytes(path);World w=SaveCodec.decode(bytes),replay=SaveCodec.decode(bytes);
   check(w.government.merit(1)==60090&&Arrays.equals(bytes,SaveCodec.encode(w)),"actual legacy merit/save preserved exactly");
   for(int turn=0;turn<3;turn++){
    check(w.nextTurn().ok&&replay.nextTurn().ok&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"three actual next-turn save/RNG continuations");
    var p=w.campaign.previewTrade(10,1,TradePlan.Operation.BUY,1000);
    check(p.allowed()&&p.effects.meritBefore==60090&&p.effects.meritAfter==60090,"legacy above-cap merit is not confiscated or increased");
    check(w.campaign.trade(10,1,true,1000).ok&&replay.campaign.trade(10,1,true,1000).ok&&w.government.merit(1)==60090,"normal transactions preserve historical excess");
    check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"full normal transaction/save/RNG equality");
   }
   check(Arrays.equals(bytes,Files.readAllBytes(path)),"original legacy file never rewritten");
  }else throw new IllegalArgumentException(args[0]);
  System.out.println("PASS merit "+args[0]);
 }
}

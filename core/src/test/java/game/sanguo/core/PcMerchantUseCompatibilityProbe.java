package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Compile this probe separately against the frozen pre-rule core and the candidate. */
public final class PcMerchantUseCompatibilityProbe {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path path=Path.of(args[1]);
  if(args[0].equals("write-legacy")){
   World w=new World(24,20);w.cities.add(new World.City(10,"A",new Hex(4,4),0));w.cities.add(new World.City(20,"B",new Hex(18,14),1));
   for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"A"+i,0,10,80,80,80,90,80));w.officers.add(new World.Officer(20,"B",1,20,80,80,80,80,80));
   w.strategy.initializeOffices();w.city(10).gold=30000;w.city(10).food=100000;w.strategy.setSeed(20261003L);
   check(w.campaign.trade(10,0,true,5000).ok&&w.campaign.trade(10,1,false,5000).ok,"frozen core must execute two legacy orders");
   check(w.campaign.traded(10)==10000,"actual legacy cumulative volume");Files.write(path,SaveCodec.encode(w));
  }else if(args[0].equals("read-candidate")){
   byte[] bytes=Files.readAllBytes(path);World w=SaveCodec.decode(bytes);
   check(Arrays.equals(bytes,SaveCodec.encode(w)),"legacy bytes round-trip losslessly, no balance/volume migration");
   check(w.campaign.traded(10)==10000,"legacy cumulative volume retained");
   for(var op:TradePlan.Operation.values()){
    var p=w.campaign.previewTrade(10,2,op,1000);
    check(!p.allowed()&&p.failure.code.equals("TRADE_USED")&&p.quotaRemaining==0,"legacy positive volume is city use");
    check(!w.campaign.trade(10,2,op==TradePlan.Operation.BUY,1000).ok&&Arrays.equals(bytes,SaveCodec.encode(w)),"legacy repeat rejection is atomic including RNG");
   }
   World replay=SaveCodec.decode(bytes);
   for(int turn=0;turn<3;turn++){
    check(w.nextTurn().ok&&replay.nextTurn().ok&&w.campaign.traded(10)==0,"normal turn clears legacy use");
    check(w.campaign.trade(10,2,true,1000).ok&&replay.campaign.trade(10,2,true,1000).ok,"normal subsequent turn readmits once");
    check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"three-turn save/RNG replay exact");
   }
  }else throw new IllegalArgumentException(args[0]);
  System.out.println("PASS "+args[0]);
 }
}

package game.sanguo.core;
import java.util.*;
/** Native credit capacity and lossless legacy over-cap balances through real commands. */
public final class PcCityCapacityTest {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static void replay(World w,int turns)throws Exception{
  World loaded=SaveCodec.decode(SaveCodec.encode(w));
  for(int n=0;n<turns;n++){check(w.nextTurn().ok&&loaded.nextTurn().ok,"complete turn");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(loaded)),"full save/RNG replay");}
 }
 public static void main(String[] args)throws Exception{
  for(int gold:new int[]{99999,100000,100001,250000,999999,1000000}){
   World w=TradePlanTest.fixture();w.city(10).gold=gold;w.city(10).troops=0;
   byte[] before=SaveCodec.encode(w);w=SaveCodec.decode(before);
   check(Arrays.equals(before,SaveCodec.encode(w))&&w.city(10).gold==gold,"lossless load above gameplay cap "+gold);
   check(w.campaign.goldCap(w.city(10))==100000&&w.campaign.foodCap(w.city(10))==1000000,"native city capacities");
   var preview=w.campaign.previewTrade(10,1,TradePlan.Operation.SELL,1000);long rng=w.strategy.getRandomState();
   check(!preview.allowed()&&preview.availableMaximum==0&&preview.goldCapacity==100000,"sell capacity includes over-cap legacy states");
   check(!w.campaign.trade(10,1,false,1000).ok&&Arrays.equals(before,SaveCodec.encode(w)),"normal sell rejects atomically");
   int price=w.campaign.foodPrice(10,true);check(w.campaign.trade(10,1,true,1000).ok,"legacy stock remains spendable");
   check(w.city(10).gold==gold-price&&w.strategy.getRandomState()==rng,"buy does not clamp old balance or consume RNG");
   int after=w.city(10).gold;replay(w,6);check(w.city(10).gold>=after,"income cannot destroy preserved balance");
   if(after>=100000)check(w.city(10).gold==after,"no further income above native cap");
  }
  World w=TradePlanTest.fixture();int price=w.campaign.foodPrice(10,false);w.city(10).gold=100000-price;
  var p=w.campaign.previewTrade(10,1,TradePlan.Operation.SELL,1000);
  check(p.allowed()&&p.effects.goldAfter==100000&&w.campaign.trade(10,1,false,1000).ok&&w.city(10).gold==100000,"normal sale reaches native cap exactly");
  w=TradePlanTest.fixture();w.city(10).gold=99999;w.city(10).troops=0;w.turn=2;
  check(new RealmOverview(w).factions.get(0).goldIncome==1,"normal monthly forecast shares native cap");
  replay(w,1);check(w.city(10).gold==100000,"normal monthly income stops exactly at native cap");
  w=LogisticsCampaignTest.fixture();w.city(12).gold=250000;int gear=w.city(12).equipment[0];
  var emptyGold=w.domestic.previewTransport(11,12,4,new int[0],0,5000,1000,new int[]{200,0,0,0},false,false,new int[2]);
  var newGold=w.domestic.previewTransport(11,12,4,new int[0],1,5000,1000,new int[]{200,0,0,0},false,false,new int[2]);
  check(emptyGold.allowed()&&emptyGold.forecast.capacityFits&&newGold.allowed()&&!newGold.forecast.capacityFits,"over-cap gold blocks positive gold, not other cargo");
  check(w.domestic.transport(11,12,4,new int[0],0,5000,1000,new int[]{200,0,0,0},false,false).ok,"normal zero-gold convoy dispatch");
  replay(w,6);check(w.domestic.missions.isEmpty()&&w.officer(4).cityId==12&&w.city(12).equipment[0]==gear+200&&w.city(12).gold==250000,"actual delivery and officer arrival preserve old gold");
  System.out.println("PASS PcCityCapacityTest checks="+checks);
 }
}

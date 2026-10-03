package game.sanguo.core;
import java.util.*;
public final class TradePlanTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static World fixture(){World w=CityActionPlanTest.fixture();w.city(10).food=100000;return w;}
 public static void main(String[] args)throws Exception{
  for(var op:TradePlan.Operation.values())for(int food:new int[]{Integer.MIN_VALUE,-1000,0,999,1000,1999,2000,19000,20000,21000,Integer.MAX_VALUE})for(int mode=0;mode<9;mode++){
   World w=fixture();int actor=1,city=10;
   if(mode==1)w.actionPoints[0]=9;if(mode==2)w.city(10).gold=99;if(mode==3)w.officer(1).acted=true;if(mode==4)actor=999;if(mode==5)city=999;
   if(mode==6)w.campaign.traded.put(10,19000);if(mode==7)w.city(10).food=999;if(mode==8){w.city(10).food=w.campaign.foodCap(w.city(10))-1000;w.city(10).gold=w.campaign.goldCap(w.city(10))-100;}
   byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();var p=w.campaign.previewTrade(city,actor,op,food);
   for(int n=0;n<3;n++)w.campaign.previewTrade(city,actor,op,food);
   check(Arrays.equals(before,SaveCodec.encode(w)),"preview pure "+op+":"+food+":"+mode);
   check(p.requestedFood==food&&p.minimum==1000&&p.maximum==20000&&p.step==1000,"input and constraints retained");
   var r=w.campaign.trade(city,actor,op==TradePlan.Operation.BUY,food);check(r.ok==p.allowed(),"normal admission "+r.message);
   if(!r.ok){check(p.effects==null&&p.failure.detail.equals(r.message)&&Arrays.equals(before,SaveCodec.encode(w)),"exact rejection atomic");continue;}
   var e=p.effects;var c=w.city(city);var o=w.officer(actor);
   check(c.gold==e.goldAfter&&c.food==e.foodAfter&&w.actionPoints[0]==e.actionPointsAfter&&w.campaign.traded(city)==e.tradedAfter,"actual city ledger");
   check(o.acted==e.actedAfter&&w.government.merit(actor)==e.meritAfter,"actual officer ledger");
   check(w.strategy.getRandomState()==rng,"trade consumes no RNG");
   for(int n=0;n<2;n++){World restored=SaveCodec.decode(SaveCodec.encode(w));check(w.nextTurn().ok&&restored.nextTurn().ok,"full normal turn");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"save RNG continuation");}
  }
  World w=fixture();int price=w.campaign.foodPrice(10,true);w.city(10).gold=price*3+price-1;
  check(w.campaign.previewTrade(10,1,TradePlan.Operation.BUY,7).availableMaximum==3000,"invalid quantity still exposes affordability rounded down");
  w.campaign.traded.put(10,18000);check(w.campaign.previewTrade(10,1,TradePlan.Operation.BUY,7).availableMaximum==0,"any prior volume consumes shared buy/sell use");
  w.city(10).food=w.campaign.foodCap(w.city(10))-999;check(w.campaign.previewTrade(10,1,TradePlan.Operation.BUY,7).availableMaximum==0,"capacity below minimum");
  w=fixture();check(w.campaign.trade(10,0,true,5000).ok,"first normal order succeeds");
  check(w.actionPoints[0]==40,"one native20 AP order debited");
  byte[] full=SaveCodec.encode(w);long random=w.strategy.getRandomState();
  for(var op:TradePlan.Operation.values())for(int actor:new int[]{1,2,3}){
   var p=w.campaign.previewTrade(10,actor,op,1000);
   check(p.failure.code.equals("TRADE_USED")&&p.failure.field.equals("city")&&p.availableMaximum==0&&p.quotaRemaining==0,"city use shared across actors and directions");
   check(!w.campaign.trade(10,actor,op==TradePlan.Operation.BUY,1000).ok&&Arrays.equals(full,SaveCodec.encode(w))&&random==w.strategy.getRandomState(),"second normal command rejects atomically");
  }
  World restored=SaveCodec.decode(full);check(restored.campaign.traded(10)==5000&&restored.nextTurn().ok&&restored.campaign.traded(10)==0,"use persists then normal turn resets");
  check(restored.campaign.trade(10,1,false,1000).ok,"normal next-turn transaction readmitted");
  check(!w.campaign.previewTrade(10,1,null,1000).allowed()&&Arrays.equals(full,SaveCodec.encode(w)),"invalid operation pure");
  World cities=fixture();cities.cities.add(new World.City(30,"丙",new Hex(4,14),0));cities.city(30).gold=30000;cities.city(30).food=100000;cities.officer(2).cityId=30;
  check(cities.campaign.trade(10,0,true,1000).ok&&cities.campaign.trade(30,2,false,1000).ok,"city use does not block another owned city");
  check(cities.campaign.traded(10)==1000&&cities.campaign.traded(30)==1000&&cities.actionPoints[0]==20,"separate city flags and shared actual AP budget");
  for(var kind:new World.SiteKind[]{World.SiteKind.GATE,World.SiteKind.PORT})for(int volume:new int[]{0,1000,20000}){
   World sites=fixture();var site=new World.City(30,"关港",new Hex(4,14),0);site.kind=kind;site.gold=5000;site.food=50000;sites.cities.add(site);sites.officer(2).cityId=30;
   if(volume>0)sites.campaign.traded.put(30,volume);
   byte[] legacy=SaveCodec.encode(sites);sites=SaveCodec.decode(legacy);check(Arrays.equals(legacy,SaveCodec.encode(sites)),"legacy site stock and trade ledger remain readable");
   for(var op:TradePlan.Operation.values()){
    var p=sites.campaign.previewTrade(30,2,op,1000);check(p.failure.code.equals("TRADE_SITE")&&p.failure.field.equals("city")&&p.effects==null&&p.quotaRemaining==0&&p.availableMaximum==0,"native city-only gate in preview");
    check(!sites.campaign.trade(30,2,op==TradePlan.Operation.BUY,1000).ok&&Arrays.equals(legacy,SaveCodec.encode(sites)),"normal gate/port rejection preserves all state/RNG");
   }
   World replay=SaveCodec.decode(legacy);
   for(int turn=0;turn<2;turn++)check(sites.nextTurn().ok&&replay.nextTurn().ok&&Arrays.equals(SaveCodec.encode(sites),SaveCodec.encode(replay)),"legacy gate/port save continues across real turns");
  }
  System.out.println("PASS TradePlanTest checks="+checks);
 }
}

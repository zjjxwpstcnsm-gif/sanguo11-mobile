package game.sanguo.core;
import java.util.*;
/** Pure forecasts checked against normal commands, complete saves and RNG. */
public final class CityActionPlanTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static World fixture(){
  World w=new World(24,20);w.cities.add(new World.City(10,"甲",new Hex(4,4),0));w.cities.add(new World.City(20,"乙",new Hex(18,14),1));
  for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,80,80,80,90,80));w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));w.strategy.initializeOffices();w.city(10).order=70;w.city(10).morale=40;w.city(10).gold=30000;
  var h=w.domestic.buildSites(10).get(0);w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,Domestic.Kind.BARRACKS,h,-1,0));w.strategy.setSeed(20261003L);return w;
 }
 static int[] targets(CityActionPlan.Operation op){return op==CityActionPlan.Operation.REWARD?new int[]{2,3}:op==CityActionPlan.Operation.APPOINT_GOVERNOR?new int[]{2}:new int[0];}
 static World.Result ordinary(World w,CityActionPlan.Operation op,int actor,int[] t){return switch(op){case PATROL->w.strategy.patrol(10,actor);case TRAIN->w.strategy.trainArmy(10,actor);case RECRUIT->w.strategy.recruitSoldiers(10,actor);case SEARCH->w.strategy.search(10,actor);case REWARD->w.strategy.rewardOfficers(10,actor,t);case APPOINT_GOVERNOR->w.strategy.appointGovernor(10,actor,t[0]);};}
 static void effects(World w,CityActionPlan p,CityActionPlan.Operation op){
  var c=w.city(10);var e=p.effects;
  check(c.order==e.orderAfter&&c.morale==e.moraleAfter&&c.troops==e.troopsAfter&&c.recruitReserve==e.reserveAfter&&c.governorId==e.governorAfter,"all deterministic city effects "+op);
  check(w.actionPoints[0]==p.actionPointsRemaining,"actual AP debit");
  if(op!=CityActionPlan.Operation.SEARCH)check(c.gold==p.goldRemaining,"actual gold debit");else check(c.gold>=p.goldRemaining&&c.gold<=p.goldRemaining+p.search.goldFoundMaximum,"search gold range");
  if(op==CityActionPlan.Operation.RECRUIT)check(w.domestic.remainingUses(10,Domestic.Kind.BARRACKS)==e.barracksUsesAfter,"actual barracks use");
  for(var f:e.officers){var o=w.officer(f.id);check(o.loyalty==f.loyaltyAfter&&o.acted==f.actedAfter&&o.lastRewardTurn==f.lastRewardTurnAfter&&o.role.name().equals(f.roleAfter)&&w.government.merit(o.id)==f.meritAfter&&w.strategy.officerState(o.id).remainingTurns==f.remainingTurnsAfter,"all actual officer effects "+op+":"+o.id);}
 }
 public static void main(String[] args)throws Exception{
  for(var op:CityActionPlan.Operation.values())for(int v=0;v<16;v++){
   World w=fixture();int actor=1;int[] t=targets(op);
   if(v==1)w.actionPoints[0]=9;if(v==2)w.city(10).gold=0;if(v==3)w.officer(actor).acted=true;if(v==4)actor=999;
   if(v==5){w.city(10).order=100;w.city(10).morale=100;}if(v==6)w.city(10).recruitReserve=0;if(v==7)w.city(10).order=29;
   if(v==8){w.domestic.facilities.get(0).remaining=2;w.domestic.facilities.get(0).builderId=3;}
   if(v==9&&op==CityActionPlan.Operation.REWARD)t=new int[]{2,2};if(v==10&&(op==CityActionPlan.Operation.REWARD||op==CityActionPlan.Operation.APPOINT_GOVERNOR))t=new int[]{0};
   if(v==11)w.strategy.addHiddenTalent(new Strategy.Talent(100,"Hidden",10,70,70,70,70,70,0));
   if(v==12&&(op==CityActionPlan.Operation.REWARD||op==CityActionPlan.Operation.APPOINT_GOVERNOR))t=new int[]{1};
   if(v==13){w.city(10).order=99;w.city(10).morale=99;w.city(10).recruitReserve=1;w.officer(2).loyalty=99;}
   if(v==14)w.actionPoints[0]=19;if(v==15)w.actionPoints[0]=20;
   byte[] before=SaveCodec.encode(w);CityActionPlan p=w.strategy.previewCityAction(op,10,actor,t);
   for(int n=0;n<3;n++)w.strategy.previewCityAction(op,10,actor,t);
   check(Arrays.equals(before,SaveCodec.encode(w)),"preview full-byte purity "+op+":"+v);
   var result=ordinary(w,op,actor,t);check(result.ok==p.allowed(),"ordinary acceptance equals preview "+op+":"+v);
   if(!result.ok){check(result.message.equals(p.failure.detail)&&p.effects==null&&p.search==null,"exact error/no forecast");check(Arrays.equals(before,SaveCodec.encode(w)),"failure atomic");continue;}
   effects(w,p,op);try{p.effects.officers.clear();throw new AssertionError("mutable effects");}catch(UnsupportedOperationException expected){checks++;}
   for(int n=0;n<2;n++){World restored=SaveCodec.decode(SaveCodec.encode(w));check(w.nextTurn().ok&&restored.nextTurn().ok,"normal turn");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"save and RNG replay");}
  }
  Set<String> outcomes=new HashSet<>();
  for(int seed=1;seed<=120;seed++){
   World w=fixture();w.strategy.setSeed(seed);if(seed%2==0)w.strategy.addHiddenTalent(new Strategy.Talent(100,"Hidden",10,70,70,70,70,70,0));
   byte[] before=SaveCodec.encode(w);var p=w.strategy.previewCityAction(CityActionPlan.Operation.SEARCH,10,1,new int[0]);
   check(Arrays.equals(before,SaveCodec.encode(w)),"search preview never rolls");World direct=SaveCodec.decode(before);
   direct.reports.prepare();var actual=direct.strategy.searchTalent(10,1);outcomes.add(actual.outcome.name());check(w.strategy.search(10,1).ok,"ordinary search");
   check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(direct)),"exact search state and report per outcome");
   check(w.strategy.getRandomState()==direct.strategy.getRandomState(),"exact RNG consumption per outcome");effects(w,p,CityActionPlan.Operation.SEARCH);
  }
  check(outcomes.containsAll(Arrays.asList("OFFICER","GOLD","NOTHING")),"multiple actual stochastic branches exercised");
  boolean foundTreasure=false;
  for(int seed=0;seed<100&&!foundTreasure;seed++){
   World treasure=TestScenarios.load("estates-drill",0,41);treasure.strategy.setSeed(seed);
   byte[] saved=SaveCodec.encode(treasure);var p=treasure.strategy.previewCityAction(CityActionPlan.Operation.SEARCH,10,1,new int[0]);
   check(p.allowed()&&Arrays.equals(saved,SaveCodec.encode(treasure)),"treasure search preview pure");
   World direct=SaveCodec.decode(saved);direct.reports.prepare();var r=direct.strategy.searchTalent(10,1);
   check(treasure.strategy.search(10,1).ok&&Arrays.equals(SaveCodec.encode(treasure),SaveCodec.encode(direct)),"treasure branch normal-command/save/RNG equality");
   if(r.outcome==Strategy.SearchOutcome.TREASURE){foundTreasure=true;check(treasure.treasures.item("item-015").place==Treasures.Place.TREASURY,"actual treasure discovered once");}
  }
  check(foundTreasure,"treasure outcome exercised");
  World w=fixture();byte[] before=SaveCodec.encode(w);
  for(var op:CityActionPlan.Operation.values())for(int[] t:new int[][]{null,new int[]{999,999,999}}){var p=w.strategy.previewCityAction(op,10,1,t);check(!p.allowed()&&p.effects==null,"malformed selection structured rejection");}
  check(!w.strategy.previewCityAction(null,10,1,new int[0]).allowed()&&Arrays.equals(before,SaveCodec.encode(w)),"invalid operation pure");
  for(int mode=0;mode<3;mode++)for(int ap:new int[]{0,9,10,19,20,60}){
   World aiWorld=fixture();aiWorld.city(10).order=mode==0?40:100;aiWorld.city(10).morale=mode==1?10:100;aiWorld.city(10).troops=mode==2?100:20000;
   aiWorld.strategy.addHiddenTalent(new Strategy.Talent(100,"Hidden",10,70,70,70,70,70,0));aiWorld.actionPoints[0]=ap;
   byte[] saved=SaveCodec.encode(aiWorld);StrategicAi ai=new StrategicAi(aiWorld);var d=ai.plan(10,false);
   check(Arrays.equals(saved,SaveCodec.encode(aiWorld)),"AI choice remains pure at AP boundary");
   if(ap<10){check(d==null,"no action below minimum AP");continue;}
   check(d!=null&&ai.execute(d).ok,"AI does not choose a now-unaffordable20AP command");
   if(ap<20)check(d.command==StrategicAi.Command.SEARCH,"AI retains valid10AP search instead of wasting a city pass");
   check(aiWorld.actionPoints[0]>=0,"AI cannot overspend AP");
  }
  System.out.println("PASS CityActionPlanTest checks="+checks+" searchOutcomes="+outcomes);
 }
}

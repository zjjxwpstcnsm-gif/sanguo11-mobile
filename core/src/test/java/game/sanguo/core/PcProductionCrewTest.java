package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class PcProductionCrewTest {
 static int checks;static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 static int[] ints(String s){return Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray();}
 public static void main(String[] args)throws Exception{
  int samples=0;
  try(var reader=new BufferedReader(new InputStreamReader(PcProductionCrewTest.class.getResourceAsStream("/pc-production-native.tsv"),StandardCharsets.UTF_8))){reader.readLine();for(String line;(line=reader.readLine())!=null;){
   String[] a=line.split("\t");int[] values=ints(a[0]),currents=ints(a[7]);int skill=Integer.parseInt(a[1]),item=Integer.parseInt(a[2]),tier=Integer.parseInt(a[3]),xp=Integer.parseInt(a[4]),injury=Integer.parseInt(a[5]),expected=Integer.parseInt(a[6]);World.Weapon weapon=World.Weapon.values()[item-1];
   World w=CityCommandRewardsTest.fixture(true,weapon);w.merchantMarket.initializeOpening();w.pcProduction.initializeOpening();w.city(10).gold=10000;for(var f:w.domestic.facilities)if(f.kind==Domestic.productionFacility(weapon))f.level=tier;
   int[] ids=new int[values.length];for(int n=0;n<ids.length;n++){ids[n]=n+1;var o=w.officer(ids[n]);OfficerAbilities.setBase(o,2,values[n]);w.officerAbilities.gainExperience(o.id,2,xp);w.government.earn(o.id,59999);o.skillId=n==ids.length-1?skill==80?Skill.NENGLI.id:skill==81?Skill.FANZHI.id:"none":"none";if(injury>0)w.contests.injuries.put(o.id,new Contests.Injury(injury,w.turn+3));w.officerAbilities.refresh(o);}
   byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();var p=w.previewProduction(10,ids,ProductionPlan.Operation.EQUIPMENT,weapon,null);check(p.allowed(),"normal admission "+line);check(p.effects.outputQuantity==expected&&p.effects.actors.size()==ids.length,"native golden quantity/all actors "+line);check(Arrays.equals(before,SaveCodec.encode(w)),"full preview pure");World replay=SaveCodec.decode(before);int stock=w.city(10).equipment[weapon.ordinal()];check(w.produce(10,ids,weapon).ok&&replay.produce(10,ids,weapon).ok,"normal crew commit");
   check(w.city(10).equipment[weapon.ordinal()]==stock+expected&&w.city(10).gold==9300&&w.actionPoints[0]==40,"normal single cost/inventory");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay))&&w.strategy.getRandomState()==rng,"normal replay/RNG");
   for(int n=0;n<ids.length;n++)check(w.officer(ids[n]).intelligence==currents[n]&&w.officer(ids[n]).acted&&w.government.merit(ids[n])==60000&&w.officerAbilities.experience(ids[n],2)==Math.min(3000,xp+2),"each native current/XP/merit/action");check(w.domestic.remainingUses(10,Domestic.productionFacility(weapon))==0,"one facility use");samples++;
  }}check(samples==972,"all original source cases");
  for(int[] ids:new int[][]{null,{}, {1,1},{1,2,3,0},{1,999},{1,20}}){World w=PcProductionFlowTest.fixture(true);byte[] before=SaveCodec.encode(w);check(!w.produce(10,ids,World.Weapon.SPEAR).ok&&Arrays.equals(before,SaveCodec.encode(w)),"malformed/unavailable crew entire state unchanged");}
  World old=PcProductionFlowTest.fixture(false);byte[] prior=SaveCodec.encode(old);check(!old.produce(10,new int[]{1,2},World.Weapon.SPEAR).ok&&Arrays.equals(prior,SaveCodec.encode(old)),"old profile does not guess multi semantics");
  System.out.println("PASS PcProductionCrewTest checks="+checks+" native972 normal commands/golden/costs/save/RNG; staging only");
 }
}

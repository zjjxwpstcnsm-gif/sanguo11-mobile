package game.sanguo.core;
import java.util.*;
public final class PcProductionFlowTest {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static World fixture(boolean nativeMode){
  World w=CityCommandRewardsTest.fixture(true,World.Weapon.SPEAR);w.merchantMarket.initializeOpening();if(nativeMode)w.pcProduction.initializeOpening();return w;
 }
 public static void main(String[] args)throws Exception{
  World w=fixture(true);OfficerAbilities.setBase(w.officer(1),2,80);w.officerAbilities.gainExperience(1,2,99);w.officer(1).skillId=Skill.NENGLI.id;w.city(10).gold=10000;
  byte[] original=SaveCodec.encode(w);check(java.nio.ByteBuffer.wrap(original).getInt(4)==36,"new source profile writes36");long rng=w.strategy.getRandomState();var p=w.previewProduction(10,1,ProductionPlan.Operation.EQUIPMENT,World.Weapon.SPEAR,null);
  check(p.allowed()&&p.goldCost==700&&p.effects.outputQuantity==3620&&p.effects.experience.currentAfter==81,"original postXP80→81,能吏,oneLv1,700 gold");
  check(Arrays.equals(original,SaveCodec.encode(w)),"preview whole state/RNG pure");World restored=SaveCodec.decode(original);check(restored.pcProduction.enabled(),"source mode survives real codec");
  int before=w.city(10).equipment[0];check(w.produce(10,1,World.Weapon.SPEAR).ok&&restored.produce(10,1,World.Weapon.SPEAR).ok,"normal command commits");
  check(w.city(10).equipment[0]==before+3620&&w.city(10).gold==9300&&w.actionPoints[0]==40,"normal stock/gold/AP matches native source");check(w.strategy.getRandomState()==rng&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"restored execution and RNG equal");
  byte[] committed=SaveCodec.encode(w);check(!w.produce(10,1,World.Weapon.SPEAR).ok&&Arrays.equals(committed,SaveCodec.encode(w)),"duplicate normal command adds no state/reward");
  for(int turn=0;turn<3;turn++){check(w.nextTurn().ok&&restored.nextTurn().ok,"normal complete turns");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"multi-turn native profile continuation");}
  World old=fixture(false);byte[] legacy=SaveCodec.encode(old);check(java.nio.ByteBuffer.wrap(legacy).getInt(4)==35&&!SaveCodec.decode(legacy).pcProduction.enabled(),"old35 keeps old production, no guessed flags");check(old.skills.productionGold(10,1,World.Weapon.SPEAR)==700,"old base cost remains700");old.officer(1).skillId=Skill.NENGLI.id;check(old.skills.productionGold(10,1,World.Weapon.SPEAR)==350,"old skill discount remains350");
  World bad=fixture(true);bad.actionPoints[0]=19;byte[] rejected=SaveCodec.encode(bad);check(!bad.produce(10,1,World.Weapon.SPEAR).ok&&Arrays.equals(rejected,SaveCodec.encode(bad)),"failed source command keeps whole state");
  System.out.println("PASS PcProductionFlowTest checks="+checks+" normal commands/save/turns; staged only, full parity not claimed");
 }
}

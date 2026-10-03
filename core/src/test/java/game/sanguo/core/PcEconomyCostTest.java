package game.sanguo.core;
import java.util.*;
/** Native20 AP constants at normal-command, preview, rejection, RNG and save boundaries. */
public final class PcEconomyCostTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void production(World.Weapon weapon,Army.Ship ship,int ap)throws Exception{
  World w=ProductionPlanTest.fixture(weapon,ship);w.actionPoints[0]=ap;long rng=w.strategy.getRandomState();byte[] before=SaveCodec.encode(w);
  var p=w.previewProduction(10,1,ship==null?ProductionPlan.Operation.EQUIPMENT:ProductionPlan.Operation.SHIP,weapon,ship);
  check(p.actionPointsCost==20&&p.allowed()==(ap>=20),"native production admission "+weapon+":"+ship+":"+ap);
  var r=ship==null?w.produce(10,1,weapon):w.army.produce(10,1,null,ship);check(r.ok==(ap>=20),"ordinary production uses native AP");
  if(ap<20){check(p.failure.code.equals("ACTION_POINTS")&&r.message.equals(p.failure.detail)&&Arrays.equals(before,SaveCodec.encode(w)),"under20 atomic rejection");return;}
  check(w.actionPoints[0]==ap-20&&p.effects.actionPointsAfter==ap-20&&rng==w.strategy.getRandomState(),"exact20 debit without RNG");
  byte[] after=SaveCodec.encode(w);check(!(ship==null?w.produce(10,1,weapon):w.army.produce(10,1,null,ship)).ok&&Arrays.equals(after,SaveCodec.encode(w)),"same actor cannot spend again");
  World loaded=SaveCodec.decode(after);check(w.nextTurn().ok&&loaded.nextTurn().ok&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(loaded)),"native-cost save continuation");
 }
 public static void main(String[] args)throws Exception{
  check(PcCityActionCosts.PRODUCTION==20&&PcCityActionCosts.TRADE==20,"generated PC constants");
  for(int ap:new int[]{0,9,10,19,20,21,60}){
   for(var weapon:World.Weapon.values())if(weapon!=World.Weapon.SWORD)production(weapon,null,ap);
   for(var ship:Army.Ship.values())if(ship!=Army.Ship.BOAT)production(null,ship,ap);
   for(var op:TradePlan.Operation.values()){
    World w=TradePlanTest.fixture();w.actionPoints[0]=ap;byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();var p=w.campaign.previewTrade(10,1,op,1000);
    check(p.actionPointsCost==20&&p.allowed()==(ap>=20),"native trade admission");var r=w.campaign.trade(10,1,op==TradePlan.Operation.BUY,1000);check(r.ok==(ap>=20),"ordinary trade uses native AP");
    if(ap<20){check(p.failure.code.equals("ACTION_POINTS")&&r.message.equals(p.failure.detail)&&Arrays.equals(before,SaveCodec.encode(w)),"under20 trade rejects atomically");continue;}
    check(w.actionPoints[0]==ap-20&&p.effects.actionPointsAfter==ap-20&&rng==w.strategy.getRandomState(),"exact20 trade debit and no RNG");World loaded=SaveCodec.decode(SaveCodec.encode(w));check(w.nextTurn().ok&&loaded.nextTurn().ok&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(loaded)),"trade save continuation");
   }
  }
  System.out.println("PASS PcEconomyCostTest checks="+checks);
 }
}

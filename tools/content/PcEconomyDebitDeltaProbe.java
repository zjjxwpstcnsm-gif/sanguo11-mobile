package game.sanguo.core;
import java.util.*;
import java.security.*;
/** Compare successful normal commands, permitting exactly the independently proved extra10 AP debit. */
public final class PcEconomyDebitDeltaProbe {
 static boolean legacy;
 static String hash(World w)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w)));}
 static void record(World w,String key,int initial,World.Result result)throws Exception{
  if(!result.ok)throw new AssertionError(key+":"+result.message);
  if(w.actionPoints[0]!=initial-(legacy?10:20))throw new AssertionError("unexpected AP "+key);
  // This is the sole permitted baseline transformation, after the real command.
  if(legacy)w.actionPoints[0]-=10;
  System.out.println(key+"|"+result.message+"|"+hash(w)+"|"+w.strategy.getRandomState());
  World restored=SaveCodec.decode(SaveCodec.encode(w));if(!w.nextTurn().ok||!restored.nextTurn().ok||!hash(w).equals(hash(restored)))throw new AssertionError("replay "+key);
  System.out.println(key+":turn|"+hash(w)+"|"+w.strategy.getRandomState());
 }
 public static void main(String[] args)throws Exception{
  legacy=args.length==1&&args[0].equals("legacy10");
  for(int ap:new int[]{20,21,60})for(int skill=0;skill<2;skill++){
   for(var weapon:World.Weapon.values())if(weapon!=World.Weapon.SWORD){World w=PcProductionCompatibilityProbe.fixture(weapon,null);w.actionPoints[0]=ap;if(skill==1)w.officer(1).skillId=Army.siegeWeapon(weapon)?Skill.FAMING.id:weapon==World.Weapon.CAVALRY?Skill.FANZHI.id:Skill.NENGLI.id;record(w,weapon+":"+skill+":"+ap,ap,w.produce(10,1,weapon));}
   for(var ship:Army.Ship.values())if(ship!=Army.Ship.BOAT){World w=PcProductionCompatibilityProbe.fixture(null,ship);w.actionPoints[0]=ap;if(skill==1)w.officer(1).skillId=Skill.ZAOCHUAN.id;record(w,ship+":"+skill+":"+ap,ap,w.army.produce(10,1,null,ship));}
   for(boolean buy:new boolean[]{true,false}){World w=PcTradeCompatibilityProbe.fixture();w.actionPoints[0]=ap;record(w,"TRADE:"+buy+":"+skill+":"+ap,ap,w.campaign.trade(10,1,buy,1000));}
  }
 }
}

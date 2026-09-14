package game.sanguo.core;
import java.util.*;import java.nio.file.*;
public class GenerateV6 {
 public static void main(String[] args)throws Exception{
  java.lang.reflect.Method m=ArmyTest.class.getDeclaredMethod("fixture");m.setAccessible(true);World w=(World)m.invoke(null);
  w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,Domestic.Kind.WORKSHOP,new Hex(6,4),-1,0));
  require(w.army.deploy(10,0,new int[]{1,2},World.Weapon.CATAPULT,Army.Ship.WARSHIP,5000,24000));
  require(w.army.produce(10,3,World.Weapon.RAM,null));w.campaign.earn(0,5000);
  require(w.campaign.research(10,4,Campaign.Tech.SPEAR_DRILL));
  int[] gear=new int[World.Weapon.values().length];gear[0]=123;gear[5]=1;
  require(w.domestic.transport(10,11,5,321,4321,1234,gear));require(w.campaign.study(10,6,Campaign.Study.NAVY));
  World.Unit u=w.unit(1);u.hex=new Hex(9,8);u.burning=2;u.burningOwner=1;u.status=War.Status.CONFUSED;u.statusTurns=1;
  w.war.fires.add(new War.Fire(new Hex(8,8),1,2));w.strategy.setSeed(0x66778899L);
  byte[] raw=SaveCodec.encode(w);if(java.nio.ByteBuffer.wrap(raw).getInt(4)!=6)throw new AssertionError("Requires untouched v6 encoder");
  Files.writeString(Path.of("core/src/test/resources/legacy-v6.sg11.b64"),Base64.getEncoder().encodeToString(raw)+"\n");
  Files.write(Path.of("/tmp/legacy-v6.sg11"),raw);System.out.println("real v6 fixture: "+raw.length+" bytes; crew/manufacturing/research/study/transport/ship/burning/status/fire/RNG");
 }
 static void require(World.Result r){if(!r.ok)throw new AssertionError(r.message);}
}

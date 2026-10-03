package game.sanguo.core;
import java.io.*;import java.util.*;
public final class PcProductionSavePolicyTest {
 static int checks;static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static void refused(World w,String reason)throws Exception{try{SaveCodec.encode(w);throw new AssertionError(reason);}catch(IOException expected){checks++;}}
 public static void main(String[] args)throws Exception{
  World w=PcDelayedProductionFlowTest.fixture(true);var h=w.domestic.buildSites(10).get(0);w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,Domestic.Kind.WORKSHOP,h,-1,0));
  check(w.army.produce(10,new int[]{1,2},World.Weapon.RAM,null).ok&&w.army.produce(10,3,World.Weapon.CATAPULT,null).ok,"two jobs own distinct original facilities");int[] staff=w.army.productions.get(0).officers();staff[1]=999;check(w.army.productions.get(0).officers()[1]==2,"saved crew defensive copy");byte[] original=SaveCodec.encode(w);World copy=SaveCodec.decode(original);check(Arrays.equals(original,SaveCodec.encode(copy))&&copy.army.productions.size()==2,"two reservations roundtrip");
  int facility=w.army.productions.get(1).facilityId;w.army.productions.get(1).facilityId=w.army.productions.get(0).facilityId;refused(w,"duplicated native facility must reject");w.army.productions.get(1).facilityId=facility;
  w.officer(2).otherTaskTurns++;refused(w,"unequal crew clocks must reject");w.officer(2).otherTaskTurns--;
  w.army.productions.get(0).crew=new int[]{1,1};refused(w,"duplicate staff rejected");w.army.productions.get(0).crew=new int[]{1,2};
  w.army.productions.get(0).facilityId=999;refused(w,"missing facility rejected");w.army.productions.get(0).facilityId=copy.army.productions.get(0).facilityId;
  check(Arrays.equals(original,SaveCodec.encode(w)),"invalid metadata checks did not silently change baseline");
  World old=PcDelayedProductionFlowTest.fixture(false);check(old.army.produce(10,1,World.Weapon.RAM,null).ok,"real old-policy task exists");byte[] saved=SaveCodec.encode(old);check(java.nio.ByteBuffer.wrap(saved).getInt(4)==35&&!SaveCodec.decode(saved).pcProduction.enabled(),"old35 task stays old policy");
  System.out.println("PASS PcProductionSavePolicyTest checks="+checks+" strict native metadata/old policy; stagingonly");
 }
}

package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Compile against the unmodified b75e249 runtime and test fixtures, never the v027 writer. */
public final class GenerateV20 {
 public static void main(String[] args)throws Exception {
  World w=LogisticsCampaignTest.fixture();
  if(!w.domestic.transport(11,12,4,new int[]{5,6},700,5000,1000,new int[4],false,true).ok)throw new AssertionError();
  for(int i=0;i<3;i++){w.turn++;w.domestic.tick();}
  if(!w.domestic.missions.get(0).returning)throw new AssertionError("actual returning state");
  if(!w.domestic.transport(12,11,8,new int[]{9,10},300,10000,1000,new int[4],false,true).ok)throw new AssertionError();
  w.turn++;w.domestic.tick();
  if(!w.districts.configure(-1,"旧档后勤军",new int[]{11},Districts.Policy.ECONOMY,-1,12,false,false).ok)throw new AssertionError();
  if(!w.army.deploy(10,1,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000).ok)throw new AssertionError();
  new CampaignAi(w).runUnits();
  byte[] data=SaveCodec.encode(w);if(data[7]!=20)throw new AssertionError("must use original writer");
  Files.writeString(Path.of(args[0]),Base64.getEncoder().encodeToString(data)+"\n");
 }
}

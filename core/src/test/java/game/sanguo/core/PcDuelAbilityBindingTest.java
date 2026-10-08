package game.sanguo.core;
import java.util.*;
/** Model-local injury reads through saved authoritative ability state. */
public final class PcDuelAbilityBindingTest {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[]args)throws Exception{
  World w=PcScenarioCatalog.load("pc-scen000-843abd9f9702618fc95223b0993454252645e926c2d2a10e9384c411551e643c",2,23);
  int[]natives={116,163,195,222,658,590};int[][]original={{84,67,42,25},{78,62,39,23},{64,51,32,19},{66,52,33,19},{68,54,34,20},{65,52,32,19}};
  Map<Integer,PcContestProfiles.Fact>facts=PcContestProfiles.saved(w);byte[]before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();
  for(int i=0;i<natives.length;i++){
   int n=natives[i];PcContestProfiles.Fact f=facts.values().stream().filter(x->x.nativeId==n).findFirst().orElseThrow();World.Officer o=w.officer(f.officerId);int base=w.officerAbilities.base(o.id,1),xp=w.officerAbilities.experience(o.id,1),current=o.war;
   for(int injury=0;injury<4;injury++)check(w.officerAbilities.currentWithInjury(o.id,1,injury)==original[i][injury],"original source war native="+n+" injury="+injury);
   check(base==w.officerAbilities.base(o.id,1)&&xp==w.officerAbilities.experience(o.id,1)&&current==o.war,"model getter retains base/XP/current");
  }
  check(Arrays.equals(before,SaveCodec.encode(w))&&rng==w.strategy.getRandomState(),"whole World and all saved RNG unchanged");
  World copy=SaveCodec.decode(before);check(Arrays.equals(before,SaveCodec.encode(copy)),"saved full state remains byte exact");
  for(int injury:new int[]{-1,4}){boolean rejected=false;try{w.officerAbilities.currentWithInjury(w.officers.get(0).id,1,injury);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"unknown injury rejected without writing");}
  World legacy=PcOfficerRankTest.world();boolean rejected=false;try{legacy.officerAbilities.currentWithInjury(2,1,0);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"unmanaged legacy base never reconstructed");
  System.out.println("PASS original duel current ability binding "+checks+" checks; fullnormal duel not yet enabled");
 }
}

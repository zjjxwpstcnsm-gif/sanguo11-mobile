package game.sanguo.core;
import java.util.*;
/** Actual source0/date184/growth0 full original51dd10+5d3d40 outputs, all8 terminal receipts. */
public final class PcDebateCampaignRulesTest {
 static int checks;static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 static World.Officer person(World w,int nativeId)throws Exception {for(var p:PcScenarioPeople.saved(w))if(p.nativeId==nativeId)return w.officer(p.officerId);throw new AssertionError("source identity");}
 public static void main(String[] args)throws Exception {
  for(int winner=0;winner<2;winner++)for(int outcome=0;outcome<4;outcome++){
   World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,42);if(!PcNativeDebatePolicy.enabled(w))PcNativeDebatePolicy.initializeOpening(w);var a=person(w,116);var t=person(w,222);long strategic=w.strategy.randomState,life=w.life.randomState;int seed=PcNativeDebatePolicy.seed(w);
   PcDebateCampaignRules.terminalRewards(w,a,t,winner,outcome);PcDebateCampaignRules.recruitmentResult(w,a,t,a.cityId,winner==0);
   boolean severe=outcome==1||outcome==2;check(w.government.merit(a.id)==(winner==0?(severe?1400:1300):1110),"actor merit original receipt");check(w.government.merit(t.id)==(winner==0?10:severe?200:100),"target merit original receipt");check(w.officerAbilities.experience(a.id,2)==(winner==0?(outcome==1?30:10):1),"actor IQ experience");check(w.officerAbilities.experience(t.id,2)==(winner==1?(outcome==1?30:10):1),"target IQ experience");check(w.officerAbilities.experience(a.id,3)==3&&w.officerAbilities.experience(a.id,4)==(winner==0?5:0),"ordinary callback politics/charm XP");check(PcNativeHealthPolicy.injury(w,winner==0?t.id:a.id)==(severe?1:0),"original loser injury");check(t.owner==(winner==0?2:-1)&&t.loyalty==(winner==0?94:0),"real source/date/ruler loyalty and join");check(w.campaign.points(2)==(winner==0?(outcome==2?37:12):0),"native half technique points after settlement and join");check(w.strategy.randomState==strategic&&w.life.randomState==life&&PcNativeDebatePolicy.seed(w)==seed,"actual source0 complete original settlement no RNG draws");byte[] saved=SaveCodec.encode(w);check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"fullWorld/all RNG exact saved terminal receipt");
  }
  System.out.println("PASS source0 full original numeric+ordinary recruitment callback "+checks+" checks/8 outcomes; normal trigger/AP/production once-only finalization remains separate");
 }
}

package game.sanguo.core;
import game.sanguo.api.*;import game.sanguo.runtime.query.*;import java.util.*;
/** Original source/live facts joined to a serial token, never renderer-controlled rules. */
public final class PcGovernorRuntimeFactsTest {
 static int checks;static void check(boolean ok,String detail){checks++;if(!ok)throw new AssertionError(detail);}
 public static void main(String[] args)throws Exception {
  StateToken token=new StateToken("governor-facts",1,0);
  for(var source:PcScenarioCatalog.all()){
   World w=PcScenarioCatalog.preview(source.identity.scenarioId);byte[] before=SaveCodec.encode(w);
   var facts=SceneFactsQuery.capture(w,token);var view=PcGovernorPolicy.view(w);
   check(facts.state.equals(token)&&facts.administration.originalElectionEnabled,"same token/current source strategy");
   check(facts.administration.armies.size()==47&&facts.administration.siteArmies.size()==87&&facts.administration.siteNativeIds.size()==87,"complete joined administrative domains");
   check(facts.administration.siteNativeIds.equals(view.siteNativeIds)&&new HashSet<>(view.siteNativeIds.values()).size()==87,"strict detached source/runtime site joins");
   for(var site:PcGovernorPolicy.source(w).sites.values())check(facts.administration.siteNativeIds.get(site.id)==site.nativeId,"each pinned original source native/runtime site identity");
   boolean sitesImmutable=false;try{facts.administration.siteNativeIds.clear();}catch(UnsupportedOperationException expected){sitesImmutable=true;}check(sitesImmutable,"native site joins immutable");
   for(var army:facts.administration.armies){var expected=view.armies.get(army.nativeId);check(army.owner==expected.owner&&army.display==expected.display&&army.leaderNativeId==expected.leaderNativeId&&army.leaderOfficerId==expected.leaderOfficerId,"native/runtime army leader projection");}
   check(facts.administration.officerArmies.equals(view.officerArmies)&&facts.administration.officerAdministrativeHomeNative.equals(view.officerAdministrativeHomeNative),"administrative home remains separate from lifecycle home");
   check(Arrays.equals(before,SaveCodec.encode(w)),"query preserves completeWorld/bothRNG");
   boolean immutable=false;try{facts.administration.siteArmies.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"detached immutable mappings");
   World restored=SaveCodec.decode(before);check(SceneFactsQuery.capture(restored,token).administration.siteArmies.equals(facts.administration.siteArmies),"complete saved strategy projected after reload");
  }
  World authored=ScenarioCatalog.load("coalition-190",0,42);check(!SceneFactsQuery.capture(authored,token).administration.originalElectionEnabled,"existing authored policies not upgraded");
  System.out.println("PASS governance read-only runtime facts "+checks+" checks; original budgets/controllers/new army allocation remain unknown");
 }
}

package game.sanguo.runtime;
import game.sanguo.core.*;import java.util.*;
/** Actual direct ordinary adapter, complete authority save and identity fences. */
public final class PcDirectRecruitmentSessionTest {
 static int checks;static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static int id(World world,int nativeId)throws java.io.IOException{return PcScenarioPeople.saved(world).stream().filter(p->p.nativeId==nativeId).findFirst().orElseThrow().officerId;}
 public static void main(String[] args)throws Exception {
  World world=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);
  try(GameSession game=new GameSession(world)){
   var stale=game.legacyView();var chosen=game.legacyView();int actor=id(chosen.draft,163),target=id(chosen.draft,222),city=chosen.draft.officer(actor).cityId;
   byte[]before=game.captureSave();var plan=chosen.draft.strategy.previewRecruitment(city,actor,target);check(plan.allowed()&&plan.nativeRules&&plan.goldCost==0&&plan.actionPointsCost==20,"same actual ordinary native quote");check(Arrays.equals(before,game.captureSave()),"preview/cancel authority pure");
   check(game.legacy(chosen.draft,()->chosen.draft.strategy.recruitOfficer(city,actor,target)).ok,"ordinary adapter native commit");byte[]after=game.captureSave();check(SaveCodec.decode(after).officer(target).owner==2,"actual joined authority");
   boolean[]evaluated={false};check(!game.legacy(chosen.draft,()->{evaluated[0]=true;return chosen.draft.strategy.recruitOfficer(city,actor,target);}).ok&&!evaluated[0]&&Arrays.equals(after,game.captureSave()),"double confirmation supplier not evaluated/resources pure");
   check(!game.legacy(stale.draft,()->{evaluated[0]=true;return stale.draft.strategy.recruitOfficer(city,actor,target);}).ok&&!evaluated[0]&&Arrays.equals(after,game.captureSave()),"old StateToken supplier not evaluated/fullsave pure");
   var oldIdentity=game.legacyView();game.replace(SaveCodec.decode(before));check(Arrays.equals(before,game.captureSave()),"complete restore original state/all RNG");check(!game.legacy(oldIdentity.draft,()->{evaluated[0]=true;return oldIdentity.draft.strategy.recruitOfficer(city,actor,target);}).ok&&!evaluated[0]&&Arrays.equals(before,game.captureSave()),"restore identity fence blocks old confirmation");
   var fresh=game.legacyView();check(game.legacy(fresh.draft,()->fresh.draft.strategy.recruitOfficer(city,actor,target)).ok&&Arrays.equals(after,game.captureSave()),"fresh postrestore ordinary command same fullstate/all RNG");
  }
  System.out.println("PASS ordinary direct adapter StateToken/double/restore "+checks+" checks; actual APK separate");
 }
}

package game.sanguo.runtime.query;
import game.sanguo.api.*;import game.sanguo.core.*;import java.util.*;
/** Read-only resource projection and explicit saved choice. No World creation,
 * initialization, reconciliation, RNG or policy adoption during a query. */
public final class PcOpeningOptionsQuery {
 private static List<PcOpeningOptionsSnapshot.Group> previewGroups(int flag)throws java.io.IOException {
  var menu=PcDuelMenuOptions.choices();List<PcOpeningOptionsSnapshot.Group>groups=new ArrayList<>();String[]ids={"difficulty","death","life"},titles={"难度","战死","寿命"};
  for(int k=0;k<3;k++){String id=ids[k];var choices=menu.stream().filter(c->c.field.equals(id)).map(c->new PcOpeningOptionsSnapshot.Choice(c.value,c.label,c.rawHex,c.controlId)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));groups.add(new PcOpeningOptionsSnapshot.Group(id,titles[k],choices,null,k==2&&flag!=0?"原剧本固定":null,k==2&&flag!=0?2:null));}
  return List.copyOf(groups);
 }
 /** First-launch catalog projection. It deliberately has no World/StateToken. */
 public static PcNewGameOptionsSnapshot newGame(String scenarioId){
  try{int flag=PcSourceOpeningOptions.sourceFlag(scenarioId);var source=PcScenarioCatalog.all().stream().filter(s->s.identity.scenarioId.equals(scenarioId)).findFirst().orElseThrow().identity;
   return new PcNewGameOptionsSnapshot(source.scenarioId,source.path,source.sha,source.sharedSha,source.sourceVariant,source.unknown,flag,PcScenarioIdentity.EXE_SHA,PcDuelMenuOptions.RECEIPT_SHA,previewGroups(flag));
  }catch(java.io.IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
 }
 public static PcOpeningOptionsSnapshot preview(String scenarioId,StateToken state){
  try{int flag=PcSourceOpeningOptions.sourceFlag(scenarioId);return new PcOpeningOptionsSnapshot(state,true,PcScenarioIdentity.EXE_SHA,PcDuelMenuOptions.RECEIPT_SHA,previewGroups(flag),flag,scenarioId);
  }catch(java.io.IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
 }
 public static PcOpeningOptionsSnapshot capture(World w,StateToken state){
  try{var menu=PcDuelMenuOptions.choices();var current=PcDuelOptions.current(w);var source=PcSourceOpeningOptions.saved(w);List<PcOpeningOptionsSnapshot.Group>groups=new ArrayList<>();String[]ids={"difficulty","death","life"},titles={"难度","战死","寿命"};
   for(int k=0;k<3;k++){String id=ids[k];var choices=menu.stream().filter(c->c.field.equals(id)).map(c->new PcOpeningOptionsSnapshot.Choice(c.value,c.label,c.rawHex,c.controlId)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));Integer saved=current==null?null:k==0?current.difficulty:k==1?current.death:current.life;groups.add(new PcOpeningOptionsSnapshot.Group(id,titles[k],choices,saved,k==2&&source!=null&&source.flag18!=0?"原剧本固定":null));}
   return new PcOpeningOptionsSnapshot(state,true,PcScenarioIdentity.EXE_SHA,PcDuelMenuOptions.RECEIPT_SHA,groups,source==null?null:source.flag18);
  }catch(java.io.IOException e){throw new IllegalStateException(e);}
 }
 private PcOpeningOptionsQuery(){}
}

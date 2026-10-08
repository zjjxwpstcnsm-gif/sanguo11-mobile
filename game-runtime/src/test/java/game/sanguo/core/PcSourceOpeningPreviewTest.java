package game.sanguo.core;
import java.nio.file.*;import java.util.*;import game.sanguo.runtime.GameSession;
/** Real prior save previews all16 target sources without replacing authority. */
public final class PcSourceOpeningPreviewTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception {
  byte[] predecessor=Files.readAllBytes(Path.of("out/session-b/duel-ruler-ai-seed1-nonterminal-v1.sg11"));var flags=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of("out/session-b/source-flag-header-v1.json"))));
  try(GameSession game=new GameSession(SaveCodec.decode(predecessor))){var token=game.state();for(Object row:MapJson.array(flags.get("rows"))){var source=MapJson.object(row);String id=(String)source.get("sourceId");int expected=((Number)source.get("flag18")).intValue();var facts=game.pcOpeningOptions(id);check(facts.state.equals(token)&&facts.previewSourceId.equals(id)&&facts.sourceFlag18==expected,"selected target source flags at current immutable token");check(!facts.hasSavedValues()&&!facts.defaultsKnown&&facts.automaticOverridesKnown,"newgame preview never imports predecessor choices/defaults");for(var g:facts.groups){check(g.savedValue==null&&g.choices.size()==3,"three original choices, no selected default");check(Objects.equals(g.fixedMenuValue,g.id.equals("life")&&expected==1?2:null),"full original544900 life constraint");}check(Arrays.equals(predecessor,game.captureSave())&&token.equals(game.state())&&PcSourceOpeningOptions.saved(SaveCodec.decode(game.captureSave()))==null,"target preview/cancel keeps entire prior World/dualRNG/policy");}
   boolean rejected=false;try{game.pcOpeningOptions("pc-missing-source");}catch(IllegalArgumentException e){rejected=true;}check(rejected&&Arrays.equals(predecessor,game.captureSave())&&game.state().equals(token),"invalid target rejects without replacing or mutating save");
  }
  for(var source:PcScenarioCatalog.all())if(PcSourceOpeningOptions.sourceFlag(source.identity.scenarioId)==1){World w=PcScenarioCatalog.load(source.identity.scenarioId,-1,24,PcDuelOptions.fromMenu(2,1,0));byte[]before=SaveCodec.encode(w);try(GameSession game=new GameSession(w)){var facts=game.pcOpeningOptions();check(facts.groups.get(2).savedValue==3&&facts.groups.get(2).savedLabel().equals("原剧本固定"),"actual declared newgame effective source override summary");check(Arrays.equals(before,game.captureSave()),"actual selected-source summary is wholeWorld/RNG pure");}}
  System.out.println("PASS source-specific opening preview "+checks+" checks; normal A dialog/full PC GUI/current APK remain separate");
 }
}

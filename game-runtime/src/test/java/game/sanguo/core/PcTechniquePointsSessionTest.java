package game.sanguo.core;
import game.sanguo.api.*;import game.sanguo.runtime.*;import java.util.*;
/** Native amount facts come from real successful host transactions, never from previews/UI. */
public final class PcTechniquePointsSessionTest {
 static int checks;static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static World fixture(){World w=PcProductionCrewSessionTest.fixture();w.pcTechniquePoints.initializeOpening();return w;}
 public static void main(String[] args)throws Exception{
  for(int workers=1;workers<=3;workers++)for(int room:new int[]{1,299,300,600,100000}){
   World w=fixture();int[] ids=new int[workers];for(int i=0;i<workers;i++)ids[i]=i+1;w.city(10).equipment[0]=100000-room;w.campaign.points.put(0,250);try(GameSession game=new GameSession(w)){
    var token=game.state();var c=new ProductionCommand(token,"EQUIPMENT",10,ids,"SPEAR");byte[] before=game.captureSave();List<GameEvent> facts=new ArrayList<>();game.subscribe(facts::add);var p=game.preview(c);check(p.allowed()&&p.effects.nativeTechniquePoints&&p.effects.techniquePoints.owner==0,"typed verified point preview");check(p.effects.techniquePoints.before==250&&p.effects.techniquePoints.after==PcTechniquePoints.after(250,Math.min(10,p.effects.outputQuantity/300+1)),"typed actual quantity bound preview");check(Arrays.equals(before,game.captureSave())&&facts.isEmpty()&&token.equals(game.state()),"preview no save/RNG/revision/event changes");
    World direct=SaveCodec.decode(before);check(direct.produce(10,ids,World.Weapon.SPEAR).ok,"normal direct rule control");var result=game.execute(c);check(result.ok()&&facts.size()==1&&result.event.techniquePointsChanges.size()==1,"one committed native amount fact");var delta=result.event.techniquePointsChanges.get(0);check(delta.before==p.effects.techniquePoints.before&&delta.after==p.effects.techniquePoints.after&&delta.owner==0,"preview and immutable commit agree");check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"entire actual host/direct save and RNG exact");byte[] after=game.captureSave();check(game.execute(c).error==CommandResult.Error.STALE_REVISION&&facts.size()==1&&Arrays.equals(after,game.captureSave()),"duplicate stale cannot credit again");game.replace(SaveCodec.decode(before));check(facts.get(facts.size()-1).techniquePointsChanges.isEmpty()&&game.preview(c).error==CommandResult.Error.STALE_SESSION,"load neither rewards nor reuses old token");
   }
  }
  try(GameSession game=new GameSession(fixture())){byte[] before=game.captureSave();List<GameEvent> facts=new ArrayList<>();game.subscribe(facts::add);var bad=new ProductionCommand(game.state(),"EQUIPMENT",10,new int[]{1,1},"SPEAR");check(!game.preview(bad).allowed()&&!game.execute(bad).ok()&&Arrays.equals(before,game.captureSave())&&facts.isEmpty(),"malformed crew preserves every saved value and emits no fact");var ticket=game.beginTurn();var cmd=new ProductionCommand(game.state(),"EQUIPMENT",10,1,"SPEAR");check(game.execute(cmd).error==CommandResult.Error.HOST_BUSY&&facts.isEmpty(),"turn ownership barrier no credit");game.cancelTurn(ticket);}
  System.out.println("PASS PcTechniquePointsSessionTest checks="+checks+" real typed production/preview/commit/facts/duplicate/load/busy/full-save/RNG");
 }
}

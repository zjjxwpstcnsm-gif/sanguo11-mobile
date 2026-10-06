package game.sanguo.core;
import game.sanguo.runtime.GameSession;import game.sanguo.api.*;import java.util.*;
public final class PcDebateSettlementSessionTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static World.Officer person(World w,int nativeId)throws Exception{for(var p:PcScenarioPeople.saved(w))if(p.nativeId==nativeId)return w.officer(p.officerId);throw new AssertionError("native identity");}
 public static void main(String[] args)throws Exception{
  World fresh=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,3);GameSession game=new GameSession(fresh);var draft=game.legacyView();World.Officer actor=person(draft.draft,116),target=person(draft.draft,222);var start=game.legacy(draft.draft,()->draft.draft.contests.persuade(actor.cityId,actor.id,target.id));check(start.ok,"normal legacy/GameSession ordinary source caller commit");int id=game.contest().contestId;int steps=0;
  while(game.contest().kind!=ContestSnapshot.Kind.NONE&&steps++<400){var f=game.contest();byte[] before=game.captureSave();ContestCommand command;
   if(f.waitingMercy)command=ContestCommand.finishDebate(f.state,f.contestId,f.revision,true);
   else if(f.phase==9)command=ContestCommand.finishDebate(f.state,f.contestId,f.revision,false);
   else{var card=f.cards.stream().filter(ContestSnapshot.Card::enabled).filter(c->c.nativeCard!=0).findFirst().orElseThrow();command=ContestCommand.card(f.state,f.contestId,f.revision,card.slot);}
   var result=game.execute(command);check(result.ok(),"actual validated native human progress: "+result.detail);byte[] applied=game.captureSave();check(!Arrays.equals(before,applied),"actual authoritative progress");check(!game.execute(command).ok()&&Arrays.equals(applied,game.captureSave()),"same token/input rejected twice without extra effect");check(Arrays.equals(applied,SaveCodec.encode(SaveCodec.decode(applied))),"every native progress wholeSave/all RNG");
  }
  check(game.contest().kind==ContestSnapshot.Kind.NONE,"ordinary native contest actually closes");byte[] finalSave=game.captureSave();var old=ContestCommand.finishDebate(game.contest().state,id,0,false);check(!game.execute(old).ok()&&Arrays.equals(finalSave,game.captureSave()),"finished result cannot be written twice");World resumed=SaveCodec.decode(finalSave);check(resumed.nextTurn().ok,"production campaign continues normal whole turn");System.out.println("PASS native debate GameSession "+checks+" checks actual ordinary caller/protocol/stale+double/no duplicate terminal/fullSave/all RNG/nextturn; Android and original admission/fees/diplomacy/abandon separate");
 }
}

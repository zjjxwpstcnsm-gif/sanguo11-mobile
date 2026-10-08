package game.sanguo.core;
import game.sanguo.api.*;import game.sanguo.runtime.GameSession;import java.nio.file.*;import java.util.*;
/** Actual original39 mid-contest save, explicit old-format adoption and full continuation. */
public final class PcHanLegacy39SessionTest {
 static int checks;static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception{
  var path=Path.of("docs/handoff/20261004/session1/batch19-actual-art-mid.sg11");byte[]raw=Files.readAllBytes(path);World w=SaveCodec.decode(raw);PcDebateCampaignPolicy.initialize(w);check(PcDebateCampaignPolicy.read(w).version==1,"genuine39 explicit original-format adoption");
  try(GameSession game=new GameSession(w)){
   int steps=0;
   while(game.contest().kind!=ContestSnapshot.Kind.NONE&&steps++<400){var f=game.contest();ContestCommand command;
    if(f.waitingMercy)command=ContestCommand.finishDebate(f.state,f.contestId,f.revision,true);
    else if(f.phase==9)command=ContestCommand.finishDebate(f.state,f.contestId,f.revision,false);
    else{var card=f.cards.stream().filter(ContestSnapshot.Card::enabled).filter(c->c.nativeCard!=0).findFirst().orElseThrow();command=ContestCommand.card(f.state,f.contestId,f.revision,card.slot);}
    var result=game.execute(command);check(result.ok(),"actual39 input "+result.detail);byte[]saved=game.captureSave();check(!game.execute(command).ok()&&Arrays.equals(saved,game.captureSave()),"old token/double once");World cold=SaveCodec.decode(saved);check(PcDebateCampaignPolicy.read(cold).version==1,"each frame preserves old policy");game.replace(cold);check(Arrays.equals(saved,game.captureSave()),"each fullWorld cold");
   }
   check(game.contest().kind==ContestSnapshot.Kind.NONE,"actual39 natural completed terminal");
   for(int i=0;i<3;i++){var ticket=game.beginTurn();var next=SaveCodec.decode(ticket.initial());check(next.nextTurn().ok,"whole campaign turn");check(game.commitTurn(ticket,next),"turn commit once");byte[]saved=game.captureSave();World cold=SaveCodec.decode(saved);check(PcDebateCampaignPolicy.read(cold).version==1,"turn retains old policy");game.replace(cold);check(Arrays.equals(saved,game.captureSave()),"allWorld/RNG turn cold");}
   check(Arrays.equals(raw,Files.readAllBytes(path)),"actual original39 file preserved");System.out.println("PASS actual39 PDC1 full human continuation/terminal/3wholeTurns/cold "+checks+" checks; current APK separate");
  }
 }
}

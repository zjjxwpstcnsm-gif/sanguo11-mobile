package game.sanguo.core;
import java.nio.file.*;import java.util.*;import game.sanguo.runtime.GameSession;
/** Replay exact real turn13 fault; no world, RNG, officer or unit resets. */
public final class PcGovernorTurnBoundarySessionTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception {
  Path input=Path.of("out/session-b/sworn-escort-continuation-v2-fault.sg11");byte[] raw=Files.readAllBytes(input);World initial=SaveCodec.decode(raw);check(initial.turn==13&&Arrays.equals(raw,SaveCodec.encode(initial)),"actual complete turn13 checkpoint byte exact");
  int id=PcDuelSwornDoubleBattleSessionTest.ids(initial).get(10);check(initial.officer(id).cityId==20016&&PcGovernorPolicy.data(initial).assignments.get(id).home==14,"actual person already returned away from unchanged administrative home");
  try(GameSession game=new GameSession(initial)){
   for(int t=0;t<5;t++){
    var ticket=game.beginTurn();World next=SaveCodec.decode(ticket.initial());check(next.nextTurn().ok,"actual full AI/global campaign turn");byte[] after=SaveCodec.encode(next);check(game.commitTurn(ticket,next),"whole turn once commits with valid native governance");check(!game.commitTurn(ticket,next)&&Arrays.equals(after,game.captureSave()),"duplicate turn commit byte pure");check(Arrays.equals(after,SaveCodec.encode(SaveCodec.decode(after))),"entire next World/dualRNG cold exact");game.replace(SaveCodec.decode(after));
    if(t==0){check(next.turn==14&&next.city(20014).governorId!=id&&next.officer(id).cityId==20016&&PcGovernorPolicy.data(next).assignments.get(id).home==14,"normal election excludes offsite native10 without teleport or home rewrite");Path output=Path.of("out/session-b/governor-location-turn14-v1.sg11");if(Files.exists(output))throw new AssertionError("preserve prior output");Files.write(output,after);}
   }
   check(Arrays.equals(raw,Files.readAllBytes(input)),"actual failed predecessor original bytes preserved");
  }
  System.out.println("PASS actual governor/Save turn boundary "+checks+" checks; native original controller/menu/APK remain separate");
 }
}

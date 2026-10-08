package game.sanguo.core;
import java.nio.file.*;import java.util.*;import game.sanguo.runtime.GameSession;
/** Existing actual second encounter checkpoint. Only legal commands and full
 * AI turns advance it; no reinsertion/reseeding/HP or result substitutions.
 * Original encounter fixture remains separate from ordinary deployment/APK. */
public final class PcDuelSwornContinuationSessionTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception {
  byte[] initial=Files.readAllBytes(Path.of(args[0]));World w=SaveCodec.decode(initial);check(Arrays.equals(initial,SaveCodec.encode(w)),"exact saved ready World/dualRNG");var ids=PcDuelSwornDoubleBattleSessionTest.ids(w);int target=ids.get(635),first=ids.get(614),actor=ids.get(args.length>1?Integer.parseInt(args[1]):355);check(w.life.state(first)==Lifecycle.State.DEAD&&w.loyalty.ruler(4).id==target,"actual first death/AI heir persisted");boolean done=false;String prefix=args.length>2?args[2]:"out/session-b/sworn-continuation";if(!prefix.startsWith("out/session-b/")||prefix.contains(".."))throw new IllegalArgumentException("own continuation output required");
  try(GameSession game=new GameSession(w)){
   try {
    for(int step=0;step<16&&!done;step++){
     var view=game.legacyView();World current=view.draft;var enemy=current.unit(current.officer(target).unitId);var own=current.unit(current.officer(actor).unitId);check(own!=null&&enemy!=null&&current.life.present(target),"current actual participants remain in field; no reset");String admission=current.contests.duelError(own.id,enemy.id);System.out.println("Current turn "+current.turn+" actor "+own.hex+" / target "+enemy.hex+" troops "+own.troops+" / "+enemy.troops+" admission "+admission);
     if(admission==null){done=PcDuelSwornDoubleBattleSessionTest.battle(game,own.id,enemy.id,target);check(game.contest().nativeDuel==null,"every noncapture/refusal completes before full turn");}
     else if(!own.acted){var reachable=current.orders.reachable(own);Hex destination=reachable.keySet().stream().filter(h->!h.equals(own.hex)&&current.cityAt(h)==null).min(Comparator.comparingInt((Hex h)->h.distance(enemy.hex)).thenComparingInt(h->reachable.get(h))).orElse(null);if(destination!=null&&destination.distance(enemy.hex)<own.hex.distance(enemy.hex)){var moved=game.legacy(current,()->current.move(own.id,destination));check(moved.ok,"ordinary admitted approach "+moved.message);}}
     if(!done)PcDuelSwornDoubleBattleSessionTest.turn(game);
    }
    check(done,"actual continued second native capture/EXECUTE reached without resetting state");World result=SaveCodec.decode(game.captureSave());int heir=result.loyalty.ruler(4).id;check(result.life.state(first)==Lifecycle.State.DEAD&&result.life.state(target)==Lifecycle.State.DEAD&&heir!=target,"two real production deaths/current successor");for(int turn=0;turn<4;turn++)PcDuelSwornDoubleBattleSessionTest.turn(game);result=SaveCodec.decode(game.captureSave());check(result.loyalty.ruler(4).id==heir&&result.life.state(target)==Lifecycle.State.DEAD,"second heir/death survives four actual full turns");Files.write(Path.of(prefix+"-finished.sg11"),game.captureSave());System.out.println("PASS actual continued second ruler/sworn EXECUTE "+checks+" checks; encounter fixture ancestry and ordinary menu/APK still separate");
   }catch(Exception|AssertionError e){Path p=Path.of(prefix+"-fault.sg11");if(!Files.exists(p))Files.write(p,game.captureSave());throw e;}
  }
 }
}

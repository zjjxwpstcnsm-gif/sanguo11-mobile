package game.sanguo.core;
import game.sanguo.runtime.GameSession;
import game.sanguo.api.*;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;

/** Exact failed Android loss. Explicit adoption and once-only actual core settlement. */
public final class SessionBActualDefeatPolicyTest {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  Path file=Path.of(args[0]);byte[] actual=Files.readAllBytes(file);
  check(PcCommandCapacityPolicy.hex(MessageDigest.getInstance("SHA-256").digest(actual)).equals("96bfcc08d64f4c53699a1506ef31c06df571fa418a1c8d32a7046618f2d3778f"),"exact actual Android normal defeat");
  World world=SaveCodec.decode(actual);var originalDuel=world.contests.current().nativeDuel;byte[] originalModel=PcDuelModelSave.write(originalDuel.state),rawRows=world.extensions.get(PcDuelRawLoyalty.NAMESPACE);
  try(GameSession game=new GameSession(world)){
   byte[] before=game.captureSave();var facts=game.contest();var token=game.state();
   check(facts.nativeDuel.terminal&&!facts.nativeDuel.loyaltyInputEnabled&&facts.nativeDuel.loyaltyInputAdoptionAvailable,"existing failed loss advertises explicit adoption, no load upgrade");
   check(Arrays.equals(before,game.captureSave())&&token.equals(game.state()),"preview/cancel full World/RNG/token pure");
   var oldFinish=game.execute(ContestCommand.finishNativeDuel(token,facts.contestId,facts.revision));check(!oldFinish.ok()&&Arrays.equals(before,game.captureSave()),"old exact failure still rejects without mutation");
   var command=ContestCommand.adoptNativeLoyaltyInput(game.state(),facts.contestId,facts.revision);var adopted=game.execute(command);check(adopted.ok(),"explicit typed adoption "+adopted.detail);
   byte[] after=game.captureSave();World adoptedWorld=SaveCodec.decode(after);check(Arrays.equals(originalModel,PcDuelModelSave.write(adoptedWorld.contests.current().nativeDuel.state)),"numeric original model/all native RNG unchanged");check(Arrays.equals(rawRows,adoptedWorld.extensions.get(PcDuelRawLoyalty.NAMESPACE)),"PDL1 original unknown rows untouched");
   check(adoptedWorld.officer(10503).loyalty==93&&adoptedWorld.officer(10555).loyalty==97,"actual participant values preserved");
   var duplicate=game.execute(command);check(!duplicate.ok()&&Arrays.equals(after,game.captureSave()),"double/old token cannot adopt again");
   game.replace(SaveCodec.decode(after));check(Arrays.equals(after,game.captureSave()),"adoption saved whole World/dual RNG continues exactly");
   facts=game.contest();var finish=game.execute(ContestCommand.finishNativeDuel(game.state(),facts.contestId,facts.revision));check(finish.ok(),"original AI outcome resolves using unique input "+finish.detail);
   check(game.contest().kind==ContestSnapshot.Kind.NONE,"actual normal loss leaves contest");byte[] settled=game.captureSave();
   var stale=game.execute(ContestCommand.finishNativeDuel(game.state(),facts.contestId,facts.revision));check(!stale.ok()&&Arrays.equals(settled,game.captureSave()),"once-only final settlement");
   game.replace(SaveCodec.decode(settled));check(Arrays.equals(settled,game.captureSave()),"final full World/model/RNG exact");
  }
  check(Arrays.equals(actual,Files.readAllBytes(file)),"original actual failed evidence retained");
  System.out.println("PASS exact actual defeat explicit input adoption "+checks+" checks; no raw/backfill/AI-selection/seed replacement; Host boundary, fresh APK still required");
 }
}

package game.sanguo.core;
import java.nio.file.*;import java.util.*;import game.sanguo.api.*;import game.sanguo.runtime.GameSession;
public final class SessionBActualHumanActorPolicyTest{
 static int n;static void check(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception{
  byte[]raw=Files.readAllBytes(Path.of(args[0]));World world=SaveCodec.decode(raw);byte[]model=PcDuelModelSave.write(world.contests.current().nativeDuel.state),pdl=world.extensions.get(PcDuelRawLoyalty.NAMESPACE);long rng=world.strategy.getRandomState();
  try(var game=new GameSession(world)){
   byte[]before=game.captureSave();var f=game.contest();check(f.nativeDuel.humanActorNativeId==503&&!f.nativeDuel.humanActorPolicyEnabled&&f.nativeDuel.humanActorPolicyAdoptionAvailable,"actual old player victory commander DTO");check(Arrays.equals(before,game.captureSave()),"query cancel pure");
   var cmd=ContestCommand.adoptNativeHumanActor(f.state,f.contestId,f.revision);var result=game.execute(cmd);check(result.ok(),"explicit typed human binding "+result.detail);byte[]after=game.captureSave();var w=SaveCodec.decode(after);check(Arrays.equals(model,PcDuelModelSave.write(w.contests.current().nativeDuel.state))&&Arrays.equals(pdl,w.extensions.get(PcDuelRawLoyalty.NAMESPACE))&&w.strategy.getRandomState()==rng,"all original model/dual RNG/raw unknown unchanged");check(!game.execute(cmd).ok()&&Arrays.equals(after,game.captureSave()),"double/stale adopt pure");game.replace(SaveCodec.decode(after));check(Arrays.equals(after,game.captureSave()),"explicit whole cold exact");
   f=game.contest();check(f.nativeDuel.humanActorNativeId==403&&f.nativeDuel.humanActorPolicyEnabled,"current original force ruler403 DTO");result=game.execute(ContestCommand.adoptNativeLoyaltyInput(f.state,f.contestId,f.revision));check(result.ok(),"separate current loyalty input adoption");
   f=game.contest();result=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(result.ok(),"original human pending selection created "+result.detail);f=game.contest();check(f.nativeDuel!=null&&!f.nativeDuel.disposition.isEmpty(),"actual original captured target pending");var pending=f.nativeDuel.disposition.get(0);byte[]pendingSave=game.captureSave();
   result=game.execute(ContestCommand.nativeDuelDisposition(f.state,f.contestId,f.revision,pending.officerId,0));check(result.ok(),"actual typed recruitment with correct actor "+result.detail);f=game.contest();var row=f.nativeDuel.disposition.get(0);System.out.println("OBSERVED original recruitment row choice="+row.choice+" mask="+row.mask+" result="+result.detail);
   check(!f.nativeDuel.humanActorPolicyAdoptionAvailable,"attempt cannot be rebound or re-rolled");byte[]attempt=game.captureSave();game.replace(SaveCodec.decode(attempt));check(Arrays.equals(attempt,game.captureSave()),"saved successful or failed attempt exact");
   if(row.choice==4){f=game.contest();result=game.execute(ContestCommand.nativeDuelDisposition(f.state,f.contestId,f.revision,pending.officerId,1));check(result.ok(),"original failed recruit permits real detain "+result.detail);}
   f=game.contest();result=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(result.ok()&&game.contest().kind==ContestSnapshot.Kind.NONE,"once original callback settled "+result.detail);
  }
  check(Arrays.equals(raw,Files.readAllBytes(Path.of(args[0]))),"actual original Android victory unchanged");System.out.println("PASS actual human actor "+n+" checks; original probability/RNG result preserved, fresh APK still required");
 }
}

package game.sanguo.core;
import java.nio.file.*;import java.util.*;import game.sanguo.api.*;import game.sanguo.runtime.GameSession;
public final class SessionBActualAiActorPolicyTest{
 static int n;static void check(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  byte[]original=Files.readAllBytes(Path.of(args[0]));World w=SaveCodec.decode(original);byte[]model=PcDuelModelSave.write(w.contests.current().nativeDuel.state),raw=w.extensions.get(PcDuelRawLoyalty.NAMESPACE);long rng=w.strategy.getRandomState();
  try(GameSession game=new GameSession(w)){
   byte[]before=game.captureSave();var f=game.contest();check(f.nativeDuel.aiActorNativeId==189&&!f.nativeDuel.aiActorPolicyEnabled&&f.nativeDuel.aiActorPolicyAdoptionAvailable,"old exact AI unit head DTO");
   check(Arrays.equals(before,game.captureSave()),"query/cancel pure");var c=ContestCommand.adoptNativeAiActor(f.state,f.contestId,f.revision);var result=game.execute(c);check(result.ok(),"typed AI actor adoption "+result.detail);
   byte[]adopted=game.captureSave();var after=SaveCodec.decode(adopted);check(Arrays.equals(model,PcDuelModelSave.write(after.contests.current().nativeDuel.state))&&Arrays.equals(raw,after.extensions.get(PcDuelRawLoyalty.NAMESPACE))&&rng==after.strategy.getRandomState(),"all native numeric model/dual RNG/raw unknown untouched");
   check(!game.execute(c).ok()&&Arrays.equals(adopted,game.captureSave()),"double old token rejected pure");game.replace(SaveCodec.decode(adopted));check(Arrays.equals(adopted,game.captureSave()),"explicit policy full cold exact");
   f=game.contest();check(f.nativeDuel.aiActorNativeId==91&&f.nativeDuel.aiActorPolicyEnabled&&!f.nativeDuel.aiActorPolicyAdoptionAvailable,"correct original current force ruler DTO");
   result=game.execute(ContestCommand.adoptNativeLoyaltyInput(f.state,f.contestId,f.revision));check(result.ok(),"separate explicitly declared current loyalty representation");
   f=game.contest();result=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(result.ok(),"original AI decision using correct actor "+result.detail);check(game.contest().kind==ContestSnapshot.Kind.NONE,"actual existing loss settles");
   byte[]settled=game.captureSave();check(!game.execute(ContestCommand.finishNativeDuel(game.state(),f.contestId,f.revision)).ok()&&Arrays.equals(settled,game.captureSave()),"once-only settlement");game.replace(SaveCodec.decode(settled));check(Arrays.equals(settled,game.captureSave()),"settled World/dual RNG exact");
   var a=SaveCodec.decode(settled);var target=a.officer(10503);System.out.println("OBSERVED target503 owner="+target.owner+" captive="+a.government.captive(target.id)+" life="+a.life.state(target.id)+" loyalty="+target.loyalty+" unit="+target.unitId);
  }
  check(Arrays.equals(original,Files.readAllBytes(Path.of(args[0]))),"actual original Android save unchanged");System.out.println("PASS actual defeat correct AI actor "+n+" checks; Host only, fresh APK required");
 }
}

package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Read-only whole-save and original return facts; never prepares APK input. */
public final class RecruitReturnArtifactProbe {
 public static void main(String[]args)throws Exception{
  for(String arg:args){Path p=Path.of(arg);byte[]raw=Files.readAllBytes(p);World w=SaveCodec.decode(raw);byte[]canon=SaveCodec.encode(w);if(!Arrays.equals(canon,SaveCodec.encode(SaveCodec.decode(canon))))throw new AssertionError("whole World/dual RNG cold bytes");
   var target=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==660).findFirst().orElseThrow();var o=w.officer(target.officerId);var a=PcGovernorPolicy.data(w).assignments.get(o.id);System.out.println(p.getFileName()+" turn="+w.turn+" stable="+o.id+" native="+target.nativeId+" name="+o.name+" owner="+o.owner+" city="+o.cityId+" unit="+o.unitId+" role="+o.role+" acted="+o.acted+" home="+a.home+" busy="+PcDuelRelease.busy(w,o.id)+" remaining="+PcDuelRelease.remaining(w,o.id)+" campaignRng="+w.strategy.getRandomState());
   if(p.getFileName().toString().contains("settled")){if(o.owner!=4||o.unitId!=-1||!PcDuelRelease.busy(w,o.id)||PcDuelRelease.remaining(w,o.id)!=2||a.home!=2)throw new AssertionError("natural recruitment callback/real duration2");}
   if(p.getFileName().toString().equals("052-turn.sg11")){if(o.owner!=4||o.unitId!=-1||PcDuelRelease.busy(w,o.id)||o.cityId!=20002||a.home!=2||w.government.captive(o.id))throw new AssertionError("normal whole-turn return home");}
   if(!Arrays.equals(raw,Files.readAllBytes(p)))throw new AssertionError("source file changed");
  }System.out.println("PASS read-only Host route whole World/dual RNG and duration2 return; NOT APK evidence");
 }
}

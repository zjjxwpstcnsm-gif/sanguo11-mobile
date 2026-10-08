package game.sanguo.core;
import java.nio.file.*;import java.util.*;
public final class SessionBActualRecruitReturnProbe{
 public static void main(String[]args)throws Exception{
  byte[]raw=Files.readAllBytes(Path.of(args[0]));World w=SaveCodec.decode(raw);byte[]before=SaveCodec.encode(w);var s=w.contests.current();int win=PcDuelKernel.readManager(s.nativeDuel.state.manager,0x54);World.Unit own=w.unit(win==0?s.leftRef:s.rightRef),other=w.unit(win==0?s.rightRef:s.leftRef);var admin=PcGovernorPolicy.data(w);var a=admin.assignments.get(own.officerId);int origin=PcPersonnelReturnRules.cityAt(w,other.hex);System.out.println("source="+w.scenarioId+" winner="+own.id+" head="+own.officerId+" army="+a.army+" nativeHome="+a.home+" parent="+PcPersonnelReturnRules.parent(a.home)+" origin="+origin+" duration="+PcPersonnelReturnRules.duration(origin,PcPersonnelReturnRules.parent(a.home))+" ownHex="+own.hex+" enemyHex="+other.hex);System.out.println("PGO="+admin.format+" PDU="+PcDuelCampaignPolicy.read(w).version);if(!Arrays.equals(before,SaveCodec.encode(w))||!Arrays.equals(raw,Files.readAllBytes(Path.of(args[0]))))throw new AssertionError("actual source/RNG changed");
 }
}

package game.sanguo.core;
import java.util.*;import java.nio.file.*;
public final class PcDuelAiActorPolicyTest{
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  byte[]actual=Files.readAllBytes(Path.of(args[0]));World w=SaveCodec.decode(actual);byte[]before=SaveCodec.encode(w);var u=w.unit(25);
  check(!PcDuelAiActorPolicy.enabled(w)&&PcDuelAiActorPolicy.actor(w,u).id==10189,"old actual strategy uses native189 unit head");
  World cold=SaveCodec.decode(before);check(!PcDuelAiActorPolicy.enabled(cold)&&Arrays.equals(before,SaveCodec.encode(cold)),"old load no adoption and full World/RNG exact");
  PcDuelAiActorPolicy.adopt(w);check(PcDuelAiActorPolicy.actor(w,u).id==10091,"explicit original force ruler native91");
  check(PcDuelAiActorPolicy.actor(w,u).id!=u.officerId,"ruler distinct from captain");
  World stripped=SaveCodec.decode(SaveCodec.encode(w));stripped.extensions.put(PcDuelAiActorPolicy.NAMESPACE,null);check(Arrays.equals(before,SaveCodec.encode(stripped)),"only explicit receipt changes World/RNG");
  byte[]saved=SaveCodec.encode(w);check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"new receipt cold exact");
  var station=PcDuelAiDisposition.retention(w,u.officerId);check(station.nativeHome>=0,"unit administrative station remains independent");
  byte[]receipt=w.extensions.get(PcDuelAiActorPolicy.NAMESPACE);receipt[receipt.length-1]^=1;w.extensions.put(PcDuelAiActorPolicy.NAMESPACE,receipt);boolean bad=false;try{SaveCodec.encode(w);}catch(java.io.IOException e){bad=true;}check(bad,"known invalid receipt rejected");
  World fresh=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,28,0,new PcDuelOptions(0,2,0));check(PcDuelAiActorPolicy.enabled(fresh),"explicit new-source factory enables original AI actor");
  check(Arrays.equals(actual,Files.readAllBytes(Path.of(args[0]))),"actual original evidence preserved");
  System.out.println("PASS original AI actor policy "+checks+" checks; explicit ruler, old unit-head strategy unchanged, station separate, allWorld/RNG exact; APK pending");
 }
}

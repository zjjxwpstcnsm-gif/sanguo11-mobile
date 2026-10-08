package game.sanguo.core;
import java.util.*;import java.nio.file.*;
public final class PcDuelHumanActorPolicyTest{
 static int n;static void check(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception{
  byte[]raw=Files.readAllBytes(Path.of(args[0]));World w=SaveCodec.decode(raw);byte[]before=SaveCodec.encode(w);var u=w.unit(16);
  check(!PcDuelHumanActorPolicy.enabled(w)&&PcDuelHumanActorPolicy.actor(w,u).id==10503,"old actual player commander503 retained");
  var old=SaveCodec.decode(before);check(!PcDuelHumanActorPolicy.enabled(old)&&Arrays.equals(before,SaveCodec.encode(old)),"old cold does not adopt");
  PcDuelHumanActorPolicy.adopt(w);check(PcDuelHumanActorPolicy.actor(w,u).id==10403,"explicit current ruler403 authoritative join");
  World strip=SaveCodec.decode(SaveCodec.encode(w));strip.extensions.put(PcDuelHumanActorPolicy.NAMESPACE,null);check(Arrays.equals(before,SaveCodec.encode(strip)),"only explicit policy receipt changed full World/RNG");
  byte[]saved=SaveCodec.encode(w);check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"new full cold exact");
  byte[]r=w.extensions.get(PcDuelHumanActorPolicy.NAMESPACE);r[r.length-1]^=1;w.extensions.put(PcDuelHumanActorPolicy.NAMESPACE,r);boolean bad=false;try{SaveCodec.encode(w);}catch(java.io.IOException e){bad=true;}check(bad,"known policy corruption rejected");
  World fresh=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,28,0,new PcDuelOptions(0,2,0));check(PcDuelHumanActorPolicy.enabled(fresh)&&PcDuelAiActorPolicy.enabled(fresh),"explicit new source selects independent AI/human policies");
  check(Arrays.equals(raw,Files.readAllBytes(Path.of(args[0]))),"actual original evidence preserved");System.out.println("PASS human actor policy "+n+" checks; old commander versus explicit ruler, allWorld/RNG; APK pending");
 }
}

package game.sanguo.core;
import java.io.*;
import java.util.*;

/** Current-property representation versus historical raw trust, no season substitution. */
public final class PcLoyaltyProperty23PolicyTest {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void rejects(Throwing call,String fragment)throws Exception{try{call.run();throw new AssertionError("Expected rejection");}catch(IOException e){check(e.getMessage().contains(fragment),e.getMessage());}}
 interface Throwing{void run()throws Exception;}
 public static void main(String[]args)throws Exception{
  World old=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,28,0);
  check(!PcLoyaltyProperty23Policy.enabled(old),"three-argument historical factory stays old strategy");
  int id=PcDuelSourceFacts.saved(old).values().stream().filter(x->x.nativeId==503).findFirst().orElseThrow().officerId;
  var o=old.officer(id);PcDuelRawLoyalty.invalidate(old,id);o.loyalty=93;
  byte[] saved=SaveCodec.encode(old),pdl=old.extensions.get(PcDuelRawLoyalty.NAMESPACE);
  rejects(()->PcDuelRawLoyalty.current(old,id),"尚未核实");check(Arrays.equals(saved,SaveCodec.encode(old)),"old unknown stays pure");
  World restored=SaveCodec.decode(saved);check(Arrays.equals(saved,SaveCodec.encode(restored))&&!PcLoyaltyProperty23Policy.enabled(restored),"old full saved World/RNG no adoption");
  PcLoyaltyProperty23Policy.adopt(old);check(PcDuelRawLoyalty.current(old,id)==93,"explicit property inverse");
  check(Arrays.equals(pdl,old.extensions.get(PcDuelRawLoyalty.NAMESPACE)),"original raw bytes and trust flag preserved");
  World stripped=SaveCodec.decode(SaveCodec.encode(old));stripped.extensions.put(PcLoyaltyProperty23Policy.NAMESPACE,null);check(Arrays.equals(saved,SaveCodec.encode(stripped)),"only explicit receipt changed whole World/RNG");
  for(int value=0;value<100;value++){o.loyalty=value;byte[] before=SaveCodec.encode(old);check(PcDuelRawLoyalty.current(old,id)==value,"unique original getter inverse "+value);check(Arrays.equals(before,SaveCodec.encode(old)),"pure inverse "+value);}
  o.loyalty=100;rejects(()->PcDuelRawLoyalty.current(old,id),"不能唯一");
  PcDuelRawLoyalty.originalWrite(old,id,255);check(PcDuelRawLoyalty.current(old,id)==255,"original proven raw255/display100 retained");
  PcDuelRawLoyalty.invalidate(old,id);o.loyalty=93;int owner=o.owner;o.owner=29;rejects(()->PcDuelRawLoyalty.current(old,id),"归属变更");o.owner=owner;
  byte[] good=old.extensions.get(PcLoyaltyProperty23Policy.NAMESPACE),bad=good.clone();bad[bad.length-1]^=1;old.extensions.put(PcLoyaltyProperty23Policy.NAMESPACE,bad);rejects(()->SaveCodec.encode(old),"忠诚输入");old.extensions.put(PcLoyaltyProperty23Policy.NAMESPACE,good);
  byte[] explicit=SaveCodec.encode(old);check(Arrays.equals(explicit,SaveCodec.encode(SaveCodec.decode(explicit))),"explicit new strategy full cold continuation");
  World fresh=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,28,0,new PcDuelOptions(0,2,0));check(PcLoyaltyProperty23Policy.enabled(fresh),"explicit four-argument new game enables declared input strategy");
  System.out.println("PASS property23 input policy "+checks+" checks; historical strategies/raw trust preserved, unique0..99, ambiguous100 rejects, exact full saved World/RNG");
 }
}

package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Must run with the pre-fix complete-workspace classes; validates the historical bug. */
public final class PcTransportSaveProbe {
 public static void main(String[] args)throws Exception{
  World w=LogisticsCampaignTest.fixture();w.strategy.setSeed(27016);w.city(11).ships[0]=3;w.city(11).ships[1]=2;
  if(!w.domestic.transport(11,12,4,new int[]{5,6},700,10000,2000,new int[]{1000,0,0,0},false,true,new int[]{2,1}).ok)throw new AssertionError("ordinary ship-cargo departure");
  byte[] raw=SaveCodec.encode(w);World loaded=SaveCodec.decode(raw);
  if(!w.nextTurn().ok||!loaded.nextTurn().ok||Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(loaded)))throw new AssertionError("Expected genuine historical report divergence; do not regenerate with fixed classes");
  Files.write(Path.of(args[0]),raw);System.out.println("Wrote historical ship-cargo save with unchanged version and real command");
 }
}

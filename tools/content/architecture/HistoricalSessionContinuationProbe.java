package game.sanguo.runtime;
import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;
/** Diagnostic comparison only: every historical fixed-hash assertion remains enforced. */
public final class HistoricalSessionContinuationProbe {
 public static void main(String[] args)throws Exception{
  Path folder=Path.of(args[0]);byte[] input=Files.readAllBytes(folder.resolve("prepared-9548bb35-v33.sg11"));World world=SaveCodec.decode(input);
  if(!Arrays.equals(input,SaveCodec.encode(world)))throw new AssertionError("Old input changed on load");
  StringBuilder log=new StringBuilder("step\tok\told_ap\tcurrent_ap\told_hash\tcurrent_hash\tpayload_diff_count\tother_rule_state_equal\n");int index=0;
  for(int[] op:BaselineSequence.operations(world)){
   World.Result result=BaselineSequence.direct(world,op,true);byte[] current=SaveCodec.encode(world),old=Files.readAllBytes(folder.resolve("states/"+index+".sg11"));World original=SaveCodec.decode(old);
   if(current.length!=old.length)throw new AssertionError("Unexpected payload length drift");int changes=0;for(int n=20;n<old.length;n++)if(current[n]!=old[n])changes++;
   System.out.println("STEP "+index+" old order="+original.home().order+" current order="+world.home().order+" old log="+original.log+" current log="+world.log);
   int[] ap=world.actionPoints.clone();System.arraycopy(original.actionPoints,0,world.actionPoints,0,ap.length);boolean onlyAp=Arrays.equals(old,SaveCodec.encode(world));System.arraycopy(ap,0,world.actionPoints,0,ap.length);
   Files.write(folder.resolve("current-"+index+".sg11"),current);log.append(index++).append('\t').append(result.ok).append('\t').append(Arrays.toString(original.actionPoints)).append('\t').append(Arrays.toString(ap)).append('\t').append(BaselineSequence.hash(old)).append('\t').append(BaselineSequence.hash(current)).append('\t').append(changes).append('\t').append(onlyAp).append('\n');
  }
  Files.writeString(folder.resolve("continuation-differences.tsv"),log.toString());System.out.print(log);
 }
}

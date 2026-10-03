import game.sanguo.core.*;
import java.util.*;
import java.nio.file.*;
import java.security.*;
/** Full production turns, save/reopen replay; hash output has no timing noise. */
public final class PcAiTurnReplayProbe {
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args[0]);Files.createDirectories(out);long started=System.nanoTime();int turns=0;
  for(var row:ScenarioCatalog.summaries()){
   World direct=ScenarioCatalog.load(row.id,0,20261003L),loaded=SaveCodec.decode(SaveCodec.encode(direct));
   for(int n=0;n<3;n++){
    if(!direct.nextTurn().ok||!loaded.nextTurn().ok)throw new AssertionError("turn "+row.id);
    byte[] saved=SaveCodec.encode(direct);if(!Arrays.equals(saved,SaveCodec.encode(loaded)))throw new AssertionError("save replay "+row.id+":"+n);
    String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(saved));System.out.println(row.id+" "+direct.turn+" "+direct.strategy.getRandomState()+" "+hash);loaded=SaveCodec.decode(saved);turns++;
   }
   Files.write(out.resolve(row.id+"-turn3.sg11"),SaveCodec.encode(direct));
  }
  System.err.println("PASS ordinary turns="+turns+" exact save/RNG; elapsedMs="+(System.nanoTime()-started)/1000000);
 }
}

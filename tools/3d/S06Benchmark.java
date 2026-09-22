import game.sanguo.core.*;
import java.security.MessageDigest;
import java.util.HexFormat;
public class S06Benchmark {
 public static void main(String[] args)throws Exception{
  World initial=ScenarioCatalog.load("heroes-250",0,12345L);byte[] saved=SaveCodec.encode(initial);
  for(int i=0;i<4;i++){
   World w=SaveCodec.decode(saved);long start=System.nanoTime();TurnJournal j=new TurnJournal(w);int[] events={0};
   w.nextTurn(p->{j.checkpoint(p.phase);if(p.boundary)events[0]+=j.drainEvents().size();});j.close();events[0]+=j.drainEvents().size();
   double millis=(System.nanoTime()-start)/1e6;String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w)));
   System.out.printf("heroes-250 factions=%d run=%d computeMs=%.2f events=%d hash=%s%n",w.factions.length,i,millis,events[0],hash);
  }
 }
}

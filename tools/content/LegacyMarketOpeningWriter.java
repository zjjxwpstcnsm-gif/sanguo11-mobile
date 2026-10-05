package game.sanguo.core;
import java.io.*;import java.nio.file.*;import java.util.*;
public class LegacyMarketOpeningWriter {
 public static void main(String[] args)throws Exception{
  File directory=new File(args[0]);if(!directory.mkdirs())throw new IOException("Fresh output required");int n=0;
  for(var summary:ScenarioCatalog.summaries()){
   World w=ScenarioCatalog.load(summary.id,0,23);byte[] raw=SaveCodec.encode(w);
   if(java.nio.ByteBuffer.wrap(raw).getInt(4)!=35||!w.officerAbilities.enabled()||!Arrays.equals(raw,SaveCodec.encode(SaveCodec.decode(raw))))throw new AssertionError("genuine old-source v35 opening exact");
   Files.write(new File(directory,summary.id+".sg11").toPath(),raw);System.out.println(summary.id+" "+raw.length);n++;
  }
  if(n!=9)throw new AssertionError("nine inherited project scenarios");System.out.println("PASS genuine frozen R29 v35 openings; not official identity proof");
 }
}

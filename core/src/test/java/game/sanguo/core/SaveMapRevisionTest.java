package game.sanguo.core;
import java.io.*;import java.util.*;
import static game.sanguo.core.Native56Checks.*;
public final class SaveMapRevisionTest {
 public static void main(String[] args)throws Exception {
  World w=world();byte[] current=SaveCodec.encode(w);check(Arrays.equals(current,SaveCodec.encode(SaveCodec.decode(current))),"new save exact roundtrip");
  byte[] old=current.clone();old[7]=30;
  try{SaveCodec.decode(old);throw new AssertionError("old save accepted");}catch(IOException expected){check(expected.getMessage().contains("旧版本"),"actionable rejection");}
  check(Arrays.equals(current,SaveCodec.encode(w)),"rejection leaves world and original bytes untouched");
  World bad=SaveCodec.decode(current);bad.mapRevision=55;try{SaveCodec.encode(bad);throw new AssertionError("old map accepted");}catch(IOException expected){check(true,"old map rejected");}
  pass("SaveMapRevisionTest");
 }
}

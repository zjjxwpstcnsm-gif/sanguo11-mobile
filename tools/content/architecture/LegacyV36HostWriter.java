import game.sanguo.core.*;import java.nio.file.*;
/** Runs against frozen v36 source; no v37 downgrade. */
public final class LegacyV36HostWriter {
 public static void main(String[] args)throws Exception{
  World w=ScenarioCatalog.load("coalition-190",0,20261003L);Files.write(Path.of(args[1]),SaveCodec.encode(w));
  byte[] old=Files.readAllBytes(Path.of(args[0]));World loaded=SaveCodec.decode(old);Files.write(Path.of(args[2]),SaveCodec.encode(loaded));
 }
}

package game.sanguo.runtime;
import game.sanguo.core.*;
import java.nio.file.*;
/** Compiles against the genuine archived historical core, never today's rules. */
public final class HistoricalSessionFixtureWriter {
 public static void main(String[] args)throws Exception{
  byte[] source=SaveCodec.encode(BaselineSequence.prepared());Files.write(Path.of(args[0]),source);
  System.out.println("Historical fixture "+BaselineSequence.hash(source));
  World world=SaveCodec.decode(source);Path states=Path.of(args[0]).getParent().resolve("states");Files.createDirectories(states);Files.write(states.resolve("initial.sg11"),source);
  int index=0;for(int[] op:BaselineSequence.operations(world)){BaselineSequence.direct(world,op,true);Files.write(states.resolve((index++)+".sg11"),SaveCodec.encode(world));}

 }
}

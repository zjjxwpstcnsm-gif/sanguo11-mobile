import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

/** Test-owned fixture only; references production classes from the installed APK. */
public final class ArtOfficerFixtureCanonicalizer {
    public static void main(String[] args)throws Exception{
        byte[] input=Files.readAllBytes(Paths.get(args[0]));World world=SaveCodec.decode(input);
        byte[] output=SaveCodec.encode(world);
        if(!Arrays.equals(output,SaveCodec.encode(SaveCodec.decode(output))))throw new AssertionError("ART fixture unstable");
        Files.write(Paths.get(args[1]),output);
        System.out.println("PASS ART fixture canonicalized scenario="+world.scenarioId+" turn="+world.turn+" bytes="+output.length);
    }
}

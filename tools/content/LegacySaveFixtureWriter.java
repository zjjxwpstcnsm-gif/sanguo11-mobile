package game.sanguo.core;

import java.nio.file.*;
import java.util.*;

/** Compile against each actual historical core; do not relabel a current save header. */
public final class LegacySaveFixtureWriter {
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args[0]);Files.createDirectories(output);
        int count=0;
        for(World world:ScenarioCatalog.all()) {
            for(int turn=0;turn<=3;turn++) {
                byte[] save=SaveCodec.encode(world);
                if(!Arrays.equals(save,SaveCodec.encode(SaveCodec.decode(save))))throw new AssertionError("historical writer self-roundtrip "+world.scenarioId);
                Files.write(output.resolve(world.scenarioId+"-turn"+world.turn+".sg11"),save);
                count++;
                if(turn<3)world.nextTurn();
            }
        }
        System.out.println("PASS historical writer fixtures="+count);
    }
}

package game.sanguo.core;

import java.nio.file.*;
import java.util.*;

/** Current reader, canonicalization and normal multi-turn commands on genuine old output. */
public final class SaveCompatibilityProbe {
    public static void main(String[] args)throws Exception {
        int checked=0;
        for(String argument:args)try(var paths=Files.list(Path.of(argument))) {
            for(Path path:paths.filter(p->p.toString().endsWith(".sg11")).sorted().toList()) {
                byte[] original=Files.readAllBytes(path);
                World first=SaveCodec.decode(original),second=SaveCodec.decode(original);
                int initial=first.turn;
                for(int turn=0;turn<=6;turn++) {
                    byte[] current=SaveCodec.encode(first);
                    if(!Arrays.equals(current,SaveCodec.encode(second)))throw new AssertionError("independent authority/RNG "+path+" turn"+turn);
                    first=SaveCodec.decode(current);
                    if(!Arrays.equals(current,SaveCodec.encode(first)))throw new AssertionError("current exact roundtrip "+path);
                    if(first.turn!=initial+turn)throw new AssertionError("normal turn progression "+path);
                    checked+=3;
                    if(turn<6){first.nextTurn();second.nextTurn();}
                }
                if(!Arrays.equals(original,Files.readAllBytes(path)))throw new AssertionError("legacy input overwritten "+path);
                checked++;
                System.out.println("PASS "+path.getFileName()+" v"+java.nio.ByteBuffer.wrap(original).getInt(4)+" six normal turns, exact current save/RNG");
            }
        }
        System.out.println("PASS save compatibility checks="+checked);
    }
}

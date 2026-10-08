package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Exact unchanged original unit-response observations, including signed truncation. */
public final class PcDuelResponseUnitFactorTest {
    public static void main(String[]args)throws Exception {
        int checks=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){
            if(line.startsWith("#")||line.isEmpty())continue;
            int[]v=Arrays.stream(line.split("\t")).mapToInt(Integer::parseInt).toArray();
            if(v.length!=7||PcDuelResponseRules.unitFactor(v[0],v[1],v[2],v[3],v[4],v[5])!=v[6])throw new AssertionError("Original unit contribution differs: "+line);
            checks++;
        }
        if(checks!=108)throw new AssertionError("Original matrix coverage changed");
        System.out.println("PASS original response unit contribution "+checks+" observations; current unit C9/CA binding and ordinary command remain pending");
    }
}

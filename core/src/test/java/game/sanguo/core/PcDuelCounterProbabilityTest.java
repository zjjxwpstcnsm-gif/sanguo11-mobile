package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Original independent getter inputs/full unchanged58a5e0 output. */
public final class PcDuelCounterProbabilityTest {
    public static void main(String[]args)throws Exception {
        int count=0;for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#")||line.isEmpty())continue;
            int[]v=Arrays.stream(line.split("\t")).mapToInt(Integer::parseInt).toArray();if(v.length!=13)throw new AssertionError("Original counter columns differ");
            var c=new PcDuelAdmissionRules.Candidate(v[5],v[5],v[6],v[7],v[8],v[9],v[10]!=0);int actual=PcDuelResponseRules.counter(v[0],v[1],v[2],v[3],v[4],c,v[11]);
            if(actual!=v[12])throw new AssertionError("Original counter differs expected="+v[12]+" actual="+actual+" row="+line);count++;
        }
        if(count!=180)throw new AssertionError("Original counter matrix differs");
        System.out.println("PASS original counter probability "+count+"; ordinary human selection/fees/campaign/APK pending");
    }
}

package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Separate untouched source instructions supply every intermediate operand. */
public final class PcDuelResponseArithmeticTest {
    public static void main(String[]args)throws Exception {
        int checks=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){
            if(line.startsWith("#")||line.isEmpty())continue;
            int[]v=Arrays.stream(line.split("\t")).mapToInt(Integer::parseInt).toArray();
            if(v.length!=17)throw new AssertionError("Original receipt columns differ");
            int actual=PcDuelResponseRules.response(v[0],v[1],v[2],v[4],v[3],v[5],v[6],v[7],v[8],v[9],v[10]!=0,v[11]!=0,v[12]!=0,v[13],v[14]!=0);
            if(actual!=v[15])throw new AssertionError("Original response differs expected="+v[15]+" actual="+actual+" row="+line);
            checks++;
        }
        int expected=args.length>1?Integer.parseInt(args[1]):108;
        if(checks!=expected)throw new AssertionError("Original response coverage changed");
        System.out.println("PASS original response arithmetic "+checks+"; current getter/RNG binding, early branch matrix and ordinary campaign remain pending");
    }
}

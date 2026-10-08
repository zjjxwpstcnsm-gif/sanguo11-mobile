package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Independent original full496570 outputs, including clamped1 and byte wrap. */
public final class PcDuelUnitStatsTest {
    public static void main(String[]args)throws Exception {
        int checks=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){
            if(line.startsWith("#")||line.isEmpty())continue;
            long[]v=Arrays.stream(line.split("\t")).mapToLong(Long::parseLong).toArray();if(v.length!=9)throw new AssertionError("Original columns differ");
            var c=PcDuelUnitStats.combat((int)v[0],(int)v[1],(int)v[2],(int)v[3],(int)v[4],(int)v[5],(int)v[6]);
            if(c.attack!=v[7]||c.defense!=v[8])throw new AssertionError("Original combat differs expected="+v[7]+","+v[8]+" actual="+c.attack+","+c.defense+" row="+line);
            checks++;
        }
        if(checks!=120)throw new AssertionError("Original matrix count differs");
        System.out.println("PASS original unit combat bytes "+checks+"; current crew/template/terrain binding and ordinary campaign pending");
    }
}

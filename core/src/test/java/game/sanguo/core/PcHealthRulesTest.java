package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class PcHealthRulesTest {
    public static void main(String[]args)throws Exception{
        int checks=0;try(InputStream in=PcHealthRulesTest.class.getResourceAsStream("/pc-health/recovery-original.tsv")){
            if(in==null)throw new IOException("Original recovery fixture missing");
            for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList()){
                int[] n=Arrays.stream(line.split("\t")).mapToInt(v->(int)Long.parseLong(v)).toArray();PcHealthRules.Recovery r=PcHealthRules.recover(n[0],n[1],n[2],n[3]!=0,n[4]!=0,n[5]!=0,n[6]);
                if(r.injury!=n[7]||r.seed!=n[8]||r.draws!=n[9])throw new AssertionError("Original recovery injury/RNG/ordering differs case="+checks);checks++;
            }
        }
        if(checks!=816)throw new AssertionError("Original coverage differs");System.out.println("PASS PcHealthRulesTest "+checks+" original full recovery trajectories/admission and exact RNG; explicit lifetime mode3 only");
    }
}

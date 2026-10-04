package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class PcOfficerJoinRulesTest {
    public static void main(String[] args)throws Exception{
        int checks=0;try(InputStream in=PcOfficerJoinRulesTest.class.getResourceAsStream("/pc-officer-join/loyalty-original.tsv")){
            if(in==null)throw new IOException("Original loyalty fixture missing");
            for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList()){
                int[] n=Arrays.stream(line.split("\t")).mapToInt(Integer::parseInt).toArray();
                int result=PcOfficerJoinRules.loyalty(n[0],n[1],n[2],n[3],n[4],n[5]!=0,n[6]!=0,n[7]!=0,n[8]!=0,n[9]!=0,n[10]!=0,n[11]!=0,n[12]!=0,n[13]!=0);
                if(result!=n[14])throw new AssertionError("Original loyalty differs case="+checks+" expected="+n[14]+" actual="+result);checks++;
            }
        }
        if(checks!=2366)throw new AssertionError("Original coverage differs");
        System.out.println("PASS PcOfficerJoinRulesTest "+checks+" raw original loyalty/relationship/boundary results; no ordinary loyalty policy changed");
    }
}

package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class PcDebateSettlementTest {
    public static void main(String[]args)throws Exception{
        int checks=0;try(InputStream in=PcDebateSettlementTest.class.getResourceAsStream("/pc-debate/settlement-original.tsv")){
            if(in==null)throw new IOException("Original settlement fixture missing");
            for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList()){
                int[] n=Arrays.stream(line.split("\t")).mapToInt(v->(int)Long.parseLong(v)).toArray();
                int[] merit=Arrays.copyOfRange(n,4,6),xp=Arrays.copyOfRange(n,6,8),injury=Arrays.copyOfRange(n,8,10),owner=Arrays.copyOfRange(n,10,12),points=Arrays.copyOfRange(n,20,67);
                PcDebateSettlement.Result result=PcDebateSettlement.calculate(n[1],n[2],merit,xp,injury,owner,points,new boolean[]{n[114]!=0,n[115]!=0},new boolean[]{n[116]!=0,n[117]!=0});
                if(!Arrays.equals(result.merit,Arrays.copyOfRange(n,12,14))||!Arrays.equals(result.intelligenceExperience,Arrays.copyOfRange(n,14,16))||!Arrays.equals(result.injury,Arrays.copyOfRange(n,16,18))||!Arrays.equals(result.techniquePoints,Arrays.copyOfRange(n,67,114)))throw new AssertionError("Original settlement differs source="+n[0]+" winner="+n[1]+" outcome="+n[2]+" boundary="+n[3]);checks++;
            }
        }
        System.out.println("PASS PcDebateSettlementTest "+checks+" full original source settlement XP/merit/injury/47 force outputs; party guidance/recovery/recruitment callback remain separate");
    }
}

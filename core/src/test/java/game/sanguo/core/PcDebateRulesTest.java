package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;
/** Original native outputs, independent of the Java implementation under test. */
public final class PcDebateRulesTest {
 public static void main(String[] args)throws Exception{
  int checks=0;try(InputStream stream=PcDebateRulesTest.class.getResourceAsStream("/pc-debate/original-rules.tsv");BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
   String line;while((line=reader.readLine())!=null){String[] p=line.split("\\t");int[] n=new int[p.length-1];for(int i=1;i<p.length;i++)n[i-1]=(int)Long.parseLong(p[i]);boolean pass;
    switch(p[0]){
     case "capacity":pass=PcDebateRules.handSlots(n[0])==n[1];break;
     case "card":pass=PcDebateRules.topic(n[0])==n[1]&&PcDebateRules.size(n[0])==n[2];break;
     case "compare":pass=PcDebateRules.compare(n[0],n[1],n[2],n[3],n[4],n[5],n[6])==n[7];break;
     case "damage":PcMerchantRules.Random random=new PcMerchantRules.Random(n[5]);pass=PcDebateRules.damage(n[0],n[1],n[2],n[3],n[4],random)==n[6]&&random.state==n[7]&&random.draws==1;break;
     case "clamp":pass=PcDebateRules.health(n[0])==n[1]&&PcDebateRules.anger(n[0])==n[2];break;
     default:throw new AssertionError("Unknown original oracle row");
    }checks++;if(!pass)throw new AssertionError("Original native comparison failed: "+line);
   }
  }
  System.out.println("PASS PcDebateRulesTest "+checks+" independent native capacity/card/temper/damage/RNG/clamp outputs; complete saved-state engine integration remains pending");
 }
}

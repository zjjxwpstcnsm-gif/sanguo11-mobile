package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Full original field-mode probability, honor scaling, decision and RNG;
 * this receipt does not replace the separate relationship admission gate. */
public final class PcDuelRecruitmentRulesTest {
    static int checks;static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static int n(Map<String,Object>m,String key){return ((Number)m.get(key)).intValue();}
    public static void main(String[]args)throws Exception {
        String raw=Files.readString(Path.of(args[0]));var numbers=java.util.regex.Pattern.compile("(?<=[\\s:\\[,])\\d{10,}(?=\\s*[,}\\]])").matcher(raw);
        var receipt=MapJson.object(MapJson.parse(numbers.replaceAll(m->Integer.toString((int)Long.parseLong(m.group()))).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        check(PcScenarioIdentity.EXE_SHA.equals(receipt.get("exeSha")),"original exe SHA");var input=MapJson.object(receipt.get("inputs"));int count=0;
        for(Object row:MapJson.array(receipt.get("rows"))){var r=MapJson.object(row);count++;
            check(Boolean.FALSE.equals(r.get("forced")),"original source558/365 fallback gate");var hashArgs=MapJson.array(r.get("adjustmentArguments"));check(((Number)hashArgs.get(0)).intValue()==5-n(input,"honor"),"original hash bound5-honor");
            int probability=PcDuelRecruitmentRules.probability(n(r,"loyalty"),n(input,"honor"),n(r,"mode"),n(input,"oldRulerGap"),n(input,"newRulerGap"),n(input,"actorCharm"),n(input,"captiveBonus"),n(input,"familyPenalty"),n(input,"likedPenalty"),n(input,"dislikedBonus"),365,558,365,0);
            check(probability==n(r,"probability"),"original5c4f80 probability "+count);
            check(PcDuelRecruitmentRules.chance(probability,n(input,"honor"))==n(r,"chance"),"original4afd60 honor scaling "+count);
            var random=new PcDuelKernel.Random(n(r,"seed"));check(PcDuelRecruitmentRules.decision(probability,n(input,"honor"),random)==(n(r,"decision")!=0),"original decision "+count);check(random.state==n(r,"rngAfter"),"original consumed RNG "+count);
        }
        check(count==512,"complete original512 cases");var random=new PcDuelKernel.Random(24);check(!PcDuelRecruitmentRules.decision(0,4,random)&&random.draws==0&&random.state==24,"zero chance no RNG");check(PcDuelRecruitmentRules.decision(1000,0,random)&&random.draws==1,"capped100 still consumes RNG");
        System.out.println("PASS original field recruitment "+checks+" checks; full forced gate/ordinary command/APK pending");
    }
}

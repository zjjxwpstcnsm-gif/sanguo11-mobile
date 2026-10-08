package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
public final class PcDuelCommandRulesTest {
    static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static int n(Map<String,Object>r,String key){return ((Number)r.get(key)).intValue();}
    public static void main(String[]args)throws Exception {
        String raw=Files.readString(Path.of(args[0]));var numbers=java.util.regex.Pattern.compile("(?<=[\\s:\\[,])\\d{10,}(?=\\s*[,}\\]])").matcher(raw);var receipt=MapJson.object(MapJson.parse(numbers.replaceAll(m->Integer.toString((int)Long.parseLong(m.group()))).getBytes(java.nio.charset.StandardCharsets.UTF_8)));int count=0;
        for(Object row:MapJson.array(receipt.get("visibility"))){var r=MapJson.object(row);check(PcDuelCommandRules.visible(n(r,"x"),n(r,"y"),(x,y)->n(r,"marked"))==(n(r,"result")==0),"original coarse visibility "+count++);}
        check(count==588,"all original signed/bounds/grid fixtures");count=0;
        for(Object row:MapJson.array(receipt.get("refusal"))){var r=MapJson.object(row);var random=new PcDuelKernel.Random(n(r,"seed"));var result=PcDuelCommandRules.refusal(n(r,"troops"),true,true,false,random);check(result.troopLoss==n(r,"loss"),"original refusal loss "+count);check(random.state==n(r,"rngAfter"),"original refusal RNG "+count);check(result.ownEnergyDelta==10&&result.targetEnergyDelta==-5,"original normal delta "+count);
            random=new PcDuelKernel.Random(n(r,"seed"));result=PcDuelCommandRules.refusal(n(r,"troops"),true,true,true,random);check(result.troopLoss==0&&result.ownEnergyDelta==0&&result.targetEnergyDelta==0&&random.state==n(r,"rngAfter"),"tactic draws without normal writes "+count++);}
        check(count==65,"all source troop/seed cases");
        for(boolean manual:new boolean[]{false,true})for(boolean visible:new boolean[]{false,true})for(boolean nominee:new boolean[]{false,true}){var random=new PcDuelKernel.Random(23);int voice=PcDuelCommandRules.speech(manual,visible,nominee,random);check(random.draws==(manual&&visible&&nominee?1:0),"speech exact draw condition");check((voice>=35)==(manual&&visible&&nominee),"speech voice admission");}
        var random=new PcDuelKernel.Random(23);var invalid=PcDuelCommandRules.refusal(5000,false,false,false,random);check(random.draws==0&&invalid.troopLoss==0&&invalid.ownEnergyDelta==0&&invalid.targetEnergyDelta==0,"invalid target no RNG/mutation");
        System.out.println("PASS original command boundaries "+checks+" checks; ordinary command/action/AP/camera strategy and APK still pending");
    }
}

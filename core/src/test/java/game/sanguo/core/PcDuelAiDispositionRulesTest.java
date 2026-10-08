package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
public final class PcDuelAiDispositionRulesTest {
    static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static int n(Map<String,Object>r,String key){return ((Number)r.get(key)).intValue();}
    public static void main(String[]args)throws Exception {
        String raw=Files.readString(Path.of(args[0]));var numbers=java.util.regex.Pattern.compile("(?<=[\\s:\\[,])\\d{10,}(?=\\s*[,}\\]])").matcher(raw);var receipt=MapJson.object(MapJson.parse(numbers.replaceAll(m->Integer.toString((int)Long.parseLong(m.group()))).getBytes(java.nio.charset.StandardCharsets.UTF_8)));int count=0;
        for(Object row:MapJson.array(receipt.get("rows"))){var r=MapJson.object(row);int[]abilities=MapJson.array(r.get("abilities")).stream().mapToInt(v->((Number)v).intValue()).toArray();String relation=(String)r.get("relation");var random=new PcDuelKernel.Random(n(r,"seed"));int expectedChance=-1;for(Object call:MapJson.array(r.get("calls"))){var c=MapJson.object(call);if("0x4721d0".equals(c.get("address")))expectedChance=n(c,"argument");}var chanceRandom=new PcDuelKernel.Random(n(r,"seed"));int chance=PcDuelAiDispositionRules.executionChance(Boolean.TRUE.equals(r.get("ruler")),abilities,n(r,"merit"),n(r,"targetAmbition"),n(r,"actorAmbition"),n(r,"honor"),n(r,"personality"),n(r,"option"),relation.equals("equal-invalid"),relation.equals("spouse"),relation.equals("liked"),chanceRandom);check(chance==expectedChance,"original AI raw chance "+count);boolean actual=PcDuelAiDispositionRules.execution(Boolean.TRUE.equals(r.get("ruler")),abilities,n(r,"merit"),n(r,"targetAmbition"),n(r,"actorAmbition"),n(r,"honor"),n(r,"personality"),n(r,"option"),relation.equals("equal-invalid"),relation.equals("spouse"),relation.equals("liked"),random);check(actual==(n(r,"decision")!=0),"original AI execution result "+count);check(random.state==n(r,"rngAfter"),"original AI execution RNG "+count++);}
        check(count==2304,"complete original2304 execution cases");System.out.println("PASS original AI execution "+checks+" checks; full first predicate/current prison context/AI campaign ordinary entry pending");
    }
}

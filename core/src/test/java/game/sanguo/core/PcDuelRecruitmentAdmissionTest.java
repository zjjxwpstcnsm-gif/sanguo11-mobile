package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Current real source people versus original512 decision fixtures; declared
 * raw-byte fixtures are separate from ordinary campaign admission. */
public final class PcDuelRecruitmentAdmissionTest {
    static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static int n(Map<String,Object>m,String key){return ((Number)m.get(key)).intValue();}
    public static void main(String[]args)throws Exception {
        String raw=Files.readString(Path.of(args[0]));var numbers=java.util.regex.Pattern.compile("(?<=[\\s:\\[,])\\d{10,}(?=\\s*[,}\\]])").matcher(raw);var receipt=MapJson.object(MapJson.parse(numbers.replaceAll(m->Integer.toString((int)Long.parseLong(m.group()))).getBytes(java.nio.charset.StandardCharsets.UTF_8)));World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);if(!PcRecruitmentBanPolicy.enabled(w))PcRecruitmentBanPolicy.initializeOpening(w);var facts=PcDuelSourceFacts.saved(w);int target=facts.values().stream().filter(f->f.nativeId==558).findFirst().orElseThrow().officerId,actor=facts.values().stream().filter(f->f.nativeId==365).findFirst().orElseThrow().officerId;int cases=0;
        for(Object item:MapJson.array(receipt.get("rows"))){var r=MapJson.object(item);var o=w.officer(target);PcDuelRawLoyalty.invalidate(w,target);o.loyalty=n(r,"display");PcDuelRawLoyalty.originalWrite(w,target,n(r,"loyalty"));byte[]before=SaveCodec.encode(w);var plan=PcDuelRecruitmentAdmission.preview(w,target,actor,n(r,"mode"));check(plan.forced==-1&&!Boolean.TRUE.equals(r.get("forced")),"original current fallback gate");check(plan.probability==n(r,"probability"),"original current5c4f80 probability "+cases);var random=new PcDuelKernel.Random(n(r,"seed"));check(plan.decision(random)==(n(r,"decision")!=0),"original current decision "+cases);check(random.state==n(r,"rngAfter"),"original current RNG "+cases);check(Arrays.equals(before,SaveCodec.encode(w)),"preview and detached decision wholeWorld/bothRNG pure "+cases);cases++;}
        check(cases==512,"full source512 cases");byte[]saved=SaveCodec.encode(w);check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"current raw255/display100/fullWorld allRNG cold save");PcRecruitmentBanPolicy.originalWrite(w,target,365,2);var plan=PcDuelRecruitmentAdmission.preview(w,target,actor,1);var random=new PcDuelKernel.Random(24);check(plan.forced==0&&!plan.decision(random)&&random.state==24&&random.draws==0,"temporary refusal no native RNG");
        System.out.println("PASS current field recruitment adapter "+checks+" checks; ordinary contest creation/terminal and APK remain required");
    }
}

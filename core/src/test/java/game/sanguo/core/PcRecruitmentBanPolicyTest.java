package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
public final class PcRecruitmentBanPolicyTest {
    static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static int n(Map<String,Object>m,String k){return ((Number)m.get(k)).intValue();}
    public static void main(String[]args)throws Exception {
        byte[]raw;try(var in=new java.util.zip.GZIPInputStream(Files.newInputStream(Path.of(args[0])))){raw=in.readAllBytes();}var receipt=MapJson.object(MapJson.parse(raw));int cases=0;
        for(Object item:MapJson.array(receipt.get("monthlyCases"))){var row=MapJson.object(item);var before=MapJson.object(row.get("before"));var target=MapJson.object(row.get("targetBefore"));var after=MapJson.object(row.get("after"));var r=new PcRecruitmentBanPolicy.Row(1,222,n(before,"hateNativeId"),n(before,"hateMonths"),true);PcRecruitmentBanPolicy.monthly(r,Boolean.TRUE.equals(before.get("allowed")),Boolean.TRUE.equals(target.get("allowed")));check(r.ruler==n(after,"hateNativeId")&&r.months==n(after,"hateMonths"),"original complete monthly refusal "+cases++);}
        check(cases==144,"all144 original cases");
        for(var s:PcScenarioCatalog.all()){World w=PcScenarioCatalog.load(s.identity.scenarioId,-1,23);w.extensions.put(PcRecruitmentBanPolicy.NAMESPACE,null);byte[]before=SaveCodec.encode(w);PcRecruitmentBanPolicy.validate(w);check(Arrays.equals(before,SaveCodec.encode(w)),"missing policy no legacy adoption");PcRecruitmentBanPolicy.initializeOpening(w);for(var f:PcDuelSourceFacts.saved(w).values()){var r=PcRecruitmentBanPolicy.current(w,f.officerId);check(r.nativeId==f.nativeId&&r.ruler==-1&&r.months==0,"source initial ban exact");}byte[]saved=SaveCodec.encode(w);check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"complete saved source/current refusal roundtrip");int id=w.officers.get(0).id;PcRecruitmentBanPolicy.invalidate(w,id);boolean rejected=false;try{PcRecruitmentBanPolicy.current(w,id);}catch(java.io.IOException e){rejected=true;}check(rejected,"unproven current ban remains unknown");byte[]unknown=SaveCodec.encode(w);check(Arrays.equals(unknown,SaveCodec.encode(SaveCodec.decode(unknown))),"unknown saved without initial refill");}
        System.out.println("PASS original monthly refusal and16-source saved metadata "+checks+" checks; original captive month counter/ordinary trigger separate");
    }
}

package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Full original source0 double-coronation sequence versus shared production
 * execution, including intermediate weighted1 and subsequent weighted0 raw.
 * Explicit heirs/declared units are not normal menu or original GUI proof. */
public final class PcDuelSwornSuccessorPolicyTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static int n(Map<String,Object>m,String k){return ((Number)m.get(k)).intValue();}
    public static void main(String[]args)throws Exception {
        var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));check(Boolean.TRUE.equals(receipt.get("wholeWorldAndRngRestored")),"original full World/RNG restored");
        for(Object value:MapJson.array(receipt.get("rows"))){var row=MapJson.object(value);World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,4,24,new PcDuelOptions(0,0,0));var ids=PcDuelSwornPolicyTest.ids(w);int firstDead=ids.get(614),firstHeir=ids.get(n(row,"heirNative")),nextHeir=ids.get(n(row,"nextHeirNative")),winner=ids.get(365);Hex land=PcDuelSwornPolicyTest.land(w);PcDuelSwornPolicyTest.deploy(w,winner,1,land);PcDuelSwornPolicyTest.deploy(w,firstDead,2,new Hex(land.q+1,land.r));PcDuelHealthPolicy.writeDuelResult(w,Map.of(firstDead,1));PcDuelExecution.apply(w,w.unit(1),w.unit(2),firstDead,firstHeir);check(w.loyalty.ruler(4).id==firstHeir,"first real production group coronation");
            PcDuelSwornPolicyTest.deploy(w,firstHeir,3,new Hex(land.q+1,land.r));byte[] before=SaveCodec.encode(w);var crown=PcRulerCoronation.validateExecution(w,firstHeir,nextHeir);var sworn=PcDuelSwornPolicy.death(w,firstHeir,crown);PcDuelExecution.validate(w,w.unit(1),w.unit(3),firstHeir,nextHeir);check(Arrays.equals(before,SaveCodec.encode(w)),"full future-crown/death preview pure World/dualRNG");
            Set<Integer> weighted=new TreeSet<>(),group=new TreeSet<>();for(Object call:MapJson.array(row.get("calls"))){var c=MapJson.object(call);int member=ids.get(n(c,"personNative"));if(n(c,"weighted")==1){weighted.add(member);check(crown.loyalty.get(member)!=null&&crown.loyalty.get(member)==n(c,"computedRaw"),"original exact intermediate weighted1 native "+n(c,"personNative"));}else{group.add(member);check(sworn.loyalty.get(member)!=null&&sworn.loyalty.get(member)==n(c,"computedRaw"),"original subsequent weighted0/max raw");}}
            check(weighted.equals(crown.loyalty.keySet())&&group.equals(sworn.loyalty.keySet()),"exact original callback write rosters");check(!sworn.loyalty.containsKey(nextHeir)&&PcDuelRawLoyalty.current(w,nextHeir)==150,"future ruler retains raw150 without self recomputation");
            PcDuelHealthPolicy.writeDuelResult(w,Map.of(firstHeir,1));PcDuelExecution.apply(w,w.unit(1),w.unit(3),firstHeir,nextHeir);check(w.life.state(firstHeir)==Lifecycle.State.DEAD&&w.life.state(firstDead)==Lifecycle.State.DEAD&&w.loyalty.ruler(4).id==nextHeir,"two actual production deaths/current chosen ruler");
            for(Object person:MapJson.array(row.get("after"))){var p=MapJson.object(person);int nativeId=n(p,"nativeId"),id=ids.get(nativeId);check(PcDuelSwornPolicy.current(w,nativeId)==n(p,"anchor"),"all670 original current anchors "+nativeId);if(w.officer(id).owner==4||nativeId==614||nativeId==n(row,"target"))check(PcDuelRawLoyalty.current(w,id)==n(p,"rawLoyalty"),"all force4 original final raw loyalty "+nativeId);}
            byte[] saved=SaveCodec.encode(w);w=SaveCodec.decode(saved);check(Arrays.equals(saved,SaveCodec.encode(w)),"full second crown/death/anchors/dualRNG cold exact");check(PcDuelRawLoyalty.current(w,nextHeir)==150&&w.officer(nextHeir).loyalty==100,"raw150/display100 ruler survives Save");
        }
        System.out.println("PASS original sworn successor death "+checks+" checks; typed normal campaign/APK/captive-absent/emptyforce remain separate");
    }
}

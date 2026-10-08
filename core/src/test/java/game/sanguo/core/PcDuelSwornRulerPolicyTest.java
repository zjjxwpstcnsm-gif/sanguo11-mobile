package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Six full original current group callbacks after actual source coronation.
 * Current execution uses source army/item/life bindings and all-world saves.
 * Declared units and explicit heirs are separate from normal menu/APK proof. */
public final class PcDuelSwornRulerPolicyTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static int n(Map<String,Object> m,String k){return ((Number)m.get(k)).intValue();}
    public static void main(String[]args)throws Exception {
        var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));check(Boolean.TRUE.equals(receipt.get("wholeWorldAndRngRestored")),"original World/RNG restored");
        for(Object value:MapJson.array(receipt.get("rows"))){var row=MapJson.object(value);World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,4,24,new PcDuelOptions(0,0,0));var ids=PcDuelSwornPolicyTest.ids(w);int departed=ids.get(614),heir=ids.get(n(row,"heirNative")),target=ids.get(n(row,"target")),winner=ids.get(365);Hex first=PcDuelSwornPolicyTest.land(w);PcDuelSwornPolicyTest.deploy(w,winner,1,first);PcDuelSwornPolicyTest.deploy(w,departed,2,new Hex(first.q+1,first.r));
            PcDuelHealthPolicy.writeDuelResult(w,Map.of(departed,1));PcDuelExecution.apply(w,w.unit(1),w.unit(2),departed,heir);check(w.loyalty.ruler(4).id==heir&&w.life.state(departed)==Lifecycle.State.DEAD,"production explicit sourcegroup coronation/death");PcDuelSwornPolicyTest.deploy(w,target,3,new Hex(first.q+1,first.r));byte[] before=SaveCodec.encode(w);PcDuelExecution.validate(w,w.unit(1),w.unit(3),target);check(Arrays.equals(before,SaveCodec.encode(w)),"ruler group preview wholeWorld/RNG pure");var plan=PcDuelSwornPolicy.death(w,target,-1);check(plan.loyalty.size()==1,"exact original one nonruler loyalty write");for(Object call:MapJson.array(row.get("calls"))){var c=MapJson.object(call);int member=ids.get(n(c,"personNative"));check(n(c,"weighted")==0&&plan.loyalty.get(member)==n(c,"computedRaw")&&PcDuelRawLoyalty.current(w,member)==n(c,"oldRaw"),"original weighted0 raw150 preserved");}
            PcDuelHealthPolicy.writeDuelResult(w,Map.of(target,1));PcDuelExecution.apply(w,w.unit(1),w.unit(3),target);check(w.life.state(target)==Lifecycle.State.DEAD&&w.loyalty.ruler(4).id==heir,"actual death preserves surviving ruler");
            for(Object person:MapJson.array(row.get("after"))){var p=MapJson.object(person);int nativeId=n(p,"nativeId"),id=ids.get(nativeId);check(PcDuelSwornPolicy.current(w,nativeId)==n(p,"anchor"),"all670 original current anchors "+nativeId);if(nativeId==614||nativeId==98||nativeId==432||nativeId==635)check(PcDuelRawLoyalty.current(w,id)==n(p,"rawLoyalty"),"full original affected group raw loyalty "+nativeId);}
            byte[] saved=SaveCodec.encode(w);w=SaveCodec.decode(saved);check(Arrays.equals(saved,SaveCodec.encode(w)),"full crown/death/ruler group World/RNG cold exact");var runtime=PcDuelRuntimeFacts.saved(w);var facts=PcDuelSourceFacts.saved(w);for(int nativeId:new int[]{98,432,635})PcDuelRecruitmentAdmission.unchangedGroup(w,ids.get(nativeId),runtime,facts);check(Arrays.equals(saved,SaveCodec.encode(w)),"postdeath current group queries pure");
        }
        System.out.println("PASS original surviving ruler sworn loyalty "+checks+" checks; inherited-during-death/captive-absent/normal campaign APK pending");
    }
}

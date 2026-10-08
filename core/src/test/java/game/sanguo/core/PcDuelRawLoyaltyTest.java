package game.sanguo.core;
import java.util.*;
/** Original raw120/display100 and source joins, explicit trust invalidation;
 * no invention of engineering reward/loss raw-byte behavior. */
public final class PcDuelRawLoyaltyTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void unknown(World w,int id,String why)throws Exception{boolean rejected=false;try{PcDuelRawLoyalty.current(w,id);}catch(java.io.IOException e){rejected=true;}check(rejected,why);}
    public static void main(String[]args)throws Exception{
        for(var source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.load(source.identity.scenarioId,-1,23);var runtime=PcDuelRuntimeFacts.saved(w);byte[]before=SaveCodec.encode(w);
            for(var fact:PcDuelSourceFacts.saved(w).values())check(PcDuelRawLoyalty.current(w,fact.officerId)==runtime.people.get(fact.nativeId).rawLoyalty,"exact raw byte source="+source.identity.scenarioId+" native="+fact.nativeId);
            check(Arrays.equals(before,SaveCodec.encode(w)),"all raw reads preserve World and both RNG");World restored=SaveCodec.decode(before);check(Arrays.equals(before,SaveCodec.encode(restored)),"raw original facts survive full source save exactly");
            byte[]opaque={9,8,7,6,5};restored.extensions.put(PcDuelRawLoyalty.NAMESPACE,opaque);byte[]future=SaveCodec.encode(restored);check(Arrays.equals(future,SaveCodec.encode(SaveCodec.decode(future))),"opaque policy kept without reinterpretation");PcDuelRawLoyalty.invalidate(restored,restored.officers.get(0).id);check(Arrays.equals(future,SaveCodec.encode(restored)),"opaque mutation boundary inert");
        }
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);int id=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==116).findFirst().orElseThrow().officerId;var o=w.officer(id);
        check(PcDuelRawLoyalty.current(w,id)==120&&o.loyalty==100,"original120 must not become display100");long rng=w.strategy.getRandomState();int nativeRng=PcNativeDebatePolicy.seed(w);PcDuelRawLoyalty.invalidate(w,id);o.loyalty=100;unknown(w,id,"capped display100 cannot preserve raw120 trust");byte[]unknown=SaveCodec.encode(w);unknown(SaveCodec.decode(unknown),id,"unknown remains unknown after complete save");check(Arrays.equals(unknown,SaveCodec.encode(SaveCodec.decode(unknown))),"unknown policy bytes exact");
        // Independently proved native recruitment setter writes94. Engineering
        // writes do not take this path, even when their display matches.
        o.loyalty=94;PcDuelRawLoyalty.originalWrite(w,id,94);check(PcDuelRawLoyalty.current(w,id)==94,"explicit proven raw setter may establish current94");check(rng==w.strategy.getRandomState()&&nativeRng==PcNativeDebatePolicy.seed(w),"raw tracking never draws either RNG");
        byte[]good=w.extensions.get(PcDuelRawLoyalty.NAMESPACE);byte[]version=good.clone();version[7]=9;byte[]badFlag=good.clone();badFlag[badFlag.length-1]=2;
        for(byte[]bad:new byte[][]{Arrays.copyOf(good,good.length-1),version,badFlag}){w.extensions.put(PcDuelRawLoyalty.NAMESPACE,bad);boolean rejected=false;try{SaveCodec.encode(w);}catch(java.io.IOException e){rejected=true;}check(rejected,"known raw policy corruption rejects before save");w.extensions.put(PcDuelRawLoyalty.NAMESPACE,good);}
        w.extensions.put(PcDuelRawLoyalty.NAMESPACE,null);byte[]old=SaveCodec.encode(w);PcDuelRawLoyalty.invalidate(w,id);check(Arrays.equals(old,SaveCodec.encode(w)),"old absence never adopted");
        System.out.println("PASS native raw loyalty "+checks+" checks; unknown engineering raw mutations retained explicitly");
    }
}

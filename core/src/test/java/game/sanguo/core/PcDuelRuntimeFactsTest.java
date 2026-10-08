package game.sanguo.core;
import java.util.*;
import java.io.*;

/** Actual source construction/fullWorld serialization; no playable activation. */
public final class PcDuelRuntimeFactsTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception {
        int sources=0;for(var source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);var s=PcDuelRuntimeFacts.saved(w);check(s!=null&&s.people.size()==670&&s.items.size()==100,"distinct source complete runtime binding");
            Map<Integer,PcDuelSourceFacts.Fact> byNative=new HashMap<>();for(var f:PcDuelSourceFacts.saved(w).values())byNative.put(f.nativeId,f);
            for(var p:s.people.values()){int[][]held=s.held(p.nativeId);check(p.treasureBonus==PcDuelKernel.treasureAiBonus(held),"actual original held item numeric bonus");var canonical=byNative.get(p.nativeId);check(w.officer(canonical.officerId)!=null,"stable existing runtime ID only");}
            byte[]before=SaveCodec.encode(w);check(Arrays.equals(before,SaveCodec.encode(w)),"allWorld/RNG query pure");World copy=SaveCodec.decode(before);check(Arrays.equals(before,SaveCodec.encode(copy)),"allWorld/source/native owner references roundtrip");
            w.extensions.put(PcDuelRuntimeFacts.NAMESPACE,null);byte[]old=SaveCodec.encode(w);World legacy=SaveCodec.decode(old);check(PcDuelRuntimeFacts.saved(legacy)==null&&Arrays.equals(old,SaveCodec.encode(legacy)),"old missing source never refilled");
            legacy.extensions.put(PcDuelRuntimeFacts.NAMESPACE,new byte[]{1,2,3,4,5});byte[]future=SaveCodec.encode(legacy);World opaque=SaveCodec.decode(future);check(PcDuelRuntimeFacts.saved(opaque)==null&&Arrays.equals(future,SaveCodec.encode(opaque)),"future unknown preserved inactive");
            copy.extensions.put(PcDuelRuntimeFacts.NAMESPACE,new byte[]{0x50,0x44,0x42,0x31});boolean rejected=false;try{SaveCodec.encode(copy);}catch(IOException e){rejected=true;}check(rejected,"known invalid source rejects");sources++;System.out.println("PASS native bindings source "+source.identity.scenarioId);
        }
        check(sources==16,"all16 distinct sources");System.out.println("PASS original runtime binding "+checks+" checks; ordinary duel/API/APK incomplete");
    }
}

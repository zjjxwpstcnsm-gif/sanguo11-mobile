package game.sanguo.core;
import java.io.*;
import java.util.*;

/** Whole source Worlds and persistence, not a substitute for normal combat. */
public final class PcDuelHealthPolicyTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception {
        int sources=0;for(var source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);var facts=PcDuelSourceFacts.saved(w);check(PcDuelHealthPolicy.enabled(w),"fresh explicit strategy");
            int first=facts.keySet().iterator().next();byte[]initial=SaveCodec.encode(w);long rng=w.strategy.getRandomState();
            check(PcDuelHealthPolicy.health(w,first)==100,"native initial physical health");check(Arrays.equals(initial,SaveCodec.encode(w)),"query keeps whole World/RNG");
            PcDuelHealthPolicy.writeDuelResult(w,Map.of(first,0));check(PcDuelHealthPolicy.health(w,first)==1,"original terminal minimum1");check(facts.get(first).initialPhysicalHealth==100,"provenance immutable");
            byte[]saved=SaveCodec.encode(w);World copy=SaveCodec.decode(saved);check(PcDuelHealthPolicy.health(copy,first)==1&&Arrays.equals(saved,SaveCodec.encode(copy)),"complete saved current health");check(rng==w.strategy.getRandomState(),"no engineering RNG drawn");
            boolean invalid=false;try{PcDuelHealthPolicy.writeDuelResult(w,Map.of(first,101));}catch(IOException e){invalid=true;}check(invalid&&Arrays.equals(saved,SaveCodec.encode(w)),"invalid result writes nothing");
            w.extensions.put(PcDuelHealthPolicy.NAMESPACE,null);byte[]old=SaveCodec.encode(w);World legacy=SaveCodec.decode(old);check(!PcDuelHealthPolicy.enabled(legacy)&&Arrays.equals(old,SaveCodec.encode(legacy)),"old absence preserved");
            legacy.extensions.put(PcDuelHealthPolicy.NAMESPACE,new byte[]{1,2,3,4,5});byte[]opaque=SaveCodec.encode(legacy);World future=SaveCodec.decode(opaque);check(!PcDuelHealthPolicy.enabled(future)&&Arrays.equals(opaque,SaveCodec.encode(future)),"opaque strategy inert");
            copy.extensions.put(PcDuelHealthPolicy.NAMESPACE,new byte[]{0x50,0x44,0x48,0x31});invalid=false;try{SaveCodec.encode(copy);}catch(IOException e){invalid=true;}check(invalid,"bad known state rejected");sources++;
        }
        check(sources==16,"all distinct sources");System.out.println("PASS original mutable physical health "+checks+" checks; full normal duel remains pending");
    }
}

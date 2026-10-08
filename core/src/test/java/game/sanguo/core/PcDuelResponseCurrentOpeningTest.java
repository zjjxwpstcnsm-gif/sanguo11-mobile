package game.sanguo.core;
import java.util.*;
/** Actual fresh source items/current persons; detached RNG and bytepure World. */
public final class PcDuelResponseCurrentOpeningTest {
    public static void main(String[]args)throws Exception {
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);
        int[]ids=new int[]{116,163,365};for(int i=0;i<ids.length;i++){final int n=ids[i];ids[i]=PcDuelSourceFacts.saved(w).values().stream().filter(p->p.nativeId==n).findFirst().orElseThrow().officerId;}
        byte[]before=SaveCodec.encode(w);int checks=0;
        var settings=new PcDuelKernel.OriginalSettings(false,-1,false,-1);
        for(int left:ids)for(int right:ids)for(int seed:new int[]{0,23,-1}){
            // Fixture has no native kind4 on these source actors: independent
            // flag expectation, rather than recomputing production predicate.
            if(Arrays.stream(PcNativeItemPolicy.held(w,left)).anyMatch(item->item[1]==4)||Arrays.stream(PcNativeItemPolicy.held(w,right)).anyMatch(item->item[1]==4))throw new AssertionError("Source fixture gear changed");
            var expectedRng=new PcDuelKernel.Random(seed);var actualRng=new PcDuelKernel.Random(seed);
            var expected=PcDuelResponseRules.opening(PcDuelBindings.actor(w,left,settings),PcDuelBindings.actor(w,right,settings),false,false,expectedRng,settings);
            var actual=PcDuelResponseRules.currentOpening(w,left,right,actualRng,settings);
            if(actual.side!=expected.side||actual.chance!=expected.chance||actualRng.state!=expectedRng.state||actualRng.draws!=expectedRng.draws)throw new AssertionError("Current original adapter differs");
            checks++;
        }
        if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Opening adapter changed full World or stored RNG");
        System.out.println("PASS current opening adapter "+checks+" with detached RNG/fullWorld pure; ordinary current unit binding pending");
    }
}

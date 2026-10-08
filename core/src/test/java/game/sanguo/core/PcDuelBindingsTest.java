package game.sanguo.core;
import java.util.*;

/** Real source/current ability joins; gear fixtures use verified original IDs. */
public final class PcDuelBindingsTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception {
        World w=PcScenarioCatalog.load("pc-scen000-843abd9f9702618fc95223b0993454252645e926c2d2a10e9384c411551e643c",2,23);
        var facts=PcContestProfiles.saved(w);var settings=new PcDuelKernel.OriginalSettings(true,3,false,-1);byte[]before=SaveCodec.encode(w);
        for(int nativeId:new int[]{116,163,195,222,658,590,98,432,365}){
            var f=facts.values().stream().filter(x->x.nativeId==nativeId).findFirst().orElseThrow();int[][]held=nativeId==98?new int[][]{{12,2}}:nativeId==432?new int[][]{{13,2}}:nativeId==365?new int[][]{{30,5}}:new int[0][];
            if(!w.life.present(f.officerId)){boolean rejected=false;try{PcDuelBindings.actor(w,f.officerId,held,settings);}catch(java.io.IOException e){rejected=true;}check(rejected,"source wait/dead record never activated for gear test");continue;}
            var a=PcDuelBindings.actor(w,f.officerId,held,settings);check(a.nativeId==nativeId&&a.personality==f.nativePersonality,"canonical identity/personality");check(a.originalAge==w.life.age(f.officerId),"actual current age");check(a.raw489080==w.officer(f.officerId).war,"current low-byte WAR");check(a.treasureBonus==(nativeId==98?10:nativeId==432?7:0),"original actual held bonus");check(a.sourceInjuryProtection==ContentRuntime.skill("skill-032").id.equals(w.officer(f.officerId).skillId),"current primary native skill32 only");
            check(a.virtual48Value==(w.officer(f.officerId).owner==w.player),"original current force player property independent of deployment");
        }
        check(Arrays.equals(before,SaveCodec.encode(w)),"wholeWorld/allRNG query pure");System.out.println("PASS original current duel bindings "+checks+" checks; full campaign integration pending");
    }
}

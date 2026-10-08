package game.sanguo.core;
import java.io.*;
import java.util.*;

/** Real new-source Worlds, full saved snapshots and explicit old absence. */
public final class PcDuelSourceFactsTest {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception{
        int sources=0;for(PcScenarioCatalog.Source source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);Map<Integer,PcDuelSourceFacts.Fact>facts=PcDuelSourceFacts.saved(w);check(facts.size()==670,"registered source identities, not770 activated persons");Map<Integer,PcContestProfiles.Fact>canonical=PcContestProfiles.saved(w);
            for(PcDuelSourceFacts.Fact f:facts.values()){
                check(f.initialPhysicalHealth==100&&f.initialInjury==0&&f.nativeId<670,"original postloader initial health");
                check(w.officer(f.officerId)!=null&&canonical.get(f.officerId).nativeId==f.nativeId,"canonical stable join");
            }
            byte[]before=SaveCodec.encode(w);check(Arrays.equals(before,SaveCodec.encode(w)),"full query/save purity");World copy=SaveCodec.decode(before);check(Arrays.equals(before,SaveCodec.encode(copy)),"complete World/all RNG exact cold serialization");check(PcDuelSourceFacts.saved(copy).size()==670,"saved metadata reads without catalog refill");
            w.extensions.put(PcDuelSourceFacts.NAMESPACE,null);byte[]legacy=SaveCodec.encode(w);World old=SaveCodec.decode(legacy);check(PcDuelSourceFacts.saved(old).isEmpty()&&Arrays.equals(legacy,SaveCodec.encode(old)),"absent old strategy never filled from catalog");
            byte[]opaque={1,2,3,4,5};old.extensions.put(PcDuelSourceFacts.NAMESPACE,opaque);byte[]future=SaveCodec.encode(old);World futureCopy=SaveCodec.decode(future);check(PcDuelSourceFacts.saved(futureCopy).isEmpty()&&Arrays.equals(future,SaveCodec.encode(futureCopy)),"opaque future strategy inert and preserved");
            copy.extensions.put(PcDuelSourceFacts.NAMESPACE,new byte[]{0x50,0x44,0x49,0x31});boolean rejected=false;try{SaveCodec.encode(copy);}catch(IOException e){rejected=true;}check(rejected,"corrupt known snapshot rejected");
            sources++;System.out.println("PASS new source "+source.identity.scenarioId);
        }
        check(sources==16,"all16 distinct source worlds");System.out.println("PASS native duel initialfacts sources="+sources+" checks="+checks+"; normal duel campaign/APK still pending");
    }
}

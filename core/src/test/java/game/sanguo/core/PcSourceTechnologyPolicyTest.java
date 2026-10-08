package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Independent original getter receipt across all16; explicit new-source policy. */
public final class PcSourceTechnologyPolicyTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception {
        var report=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));var original=MapJson.array(report.get("rows"));check(original.size()==16,"all16 original sources");
        for(int i=0;i<16;i++){
            var src=PcScenarioCatalog.all().get(i);World w=PcScenarioCatalog.load(src.identity.scenarioId,2,23);check(PcSourceTechnologyPolicy.enabled(w),"explicit fresh tech policy");
            var row=MapJson.object(original.get(i));var forces=MapJson.array(row.get("forces"));check(forces.size()==47,"all47 source force slots");
            for(int side=0;side<42;side++){
                var f=MapJson.object(forces.get(side));long bits=Long.parseLong(MapJson.string(f.get("rawBits"),32));boolean active=w.loyalty.ruler(side)!=null;
                for(int t=0;t<36;t++)check(PcSourceTechnologyPolicy.has(w,side,t)==(active&&(bits&(1L<<t))!=0),"original current tech source="+i+" force="+side+" tech="+t);
            }
            byte[]saved=SaveCodec.encode(w);World cold=SaveCodec.decode(saved);check(Arrays.equals(saved,SaveCodec.encode(cold)),"complete source tech/World/RNG exact save");
            // Learned state must survive as current state; provenance mask
            // must never refill a removed technology on decoding.
            w.campaign.learned.clear();byte[]cleared=SaveCodec.encode(w);World reloaded=SaveCodec.decode(cleared);check(reloaded.campaign.learned.values().stream().allMatch(Set::isEmpty),"source bits do not refill current saved state");
            reloaded.campaign.learned.put(2,EnumSet.of(Campaign.Tech.ELITE_SPEAR));check(PcSourceTechnologyPolicy.has(reloaded,2,3),"current completion/learning maps native3");
            check(PcSourceTechnologyPolicy.technology(3)==Campaign.Tech.ELITE_SPEAR&&PcSourceTechnologyPolicy.technology(7)==Campaign.Tech.ELITE_HALBERD&&PcSourceTechnologyPolicy.technology(11)==Campaign.Tech.ELITE_CROSSBOW&&PcSourceTechnologyPolicy.technology(15)==Campaign.Tech.ELITE_CAVALRY,"four original elite identities");
            w.extensions.put(PcSourceTechnologyPolicy.NAMESPACE,null);byte[]legacy=SaveCodec.encode(w);check(!PcSourceTechnologyPolicy.enabled(SaveCodec.decode(legacy)),"absent previous policy stays absent");
        }
        System.out.println("PASS source technologies "+checks+"; ordinary research/API/APK and genuine old saves remain separate");
    }
}

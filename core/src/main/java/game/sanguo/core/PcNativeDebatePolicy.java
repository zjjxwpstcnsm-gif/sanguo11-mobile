package game.sanguo.core;
import java.io.*;

/** Explicit opt-in verification policy. No restore-time or catalog backfill. */
final class PcNativeDebatePolicy {
    static final String NAMESPACE="pc-native-debate-effects-v1";
    static final int SAVE_VERSION=39,MAGIC=0x504e4431;
    static boolean enabled(World w){return w.extensions.get(NAMESPACE)!=null;}
    static void initializeOpening(World w)throws IOException{
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||enabled(w)||PcContestProfiles.saved(w).isEmpty())throw new IOException("Only explicit fresh source game may initialize native debate policy");
        // Explicit deterministic verification seed, not certified original
        // PC global startup RNG. Persisted separately from existing SplitMix64.
        setSeed(w,(int)w.strategy.getRandomState());
        PcNativeHealthPolicy.initializeOpening(w);
    }
    static void setSeed(World w,int seed){try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(seed);w.extensions.put(NAMESPACE,b.toByteArray());}catch(IOException impossible){throw new IllegalStateException(impossible);}}
    static int seed(World w)throws IOException{byte[] b=w.extensions.get(NAMESPACE);if(b==null||b.length!=8)throw new IOException("Native debate policy missing");DataInputStream d=new DataInputStream(new ByteArrayInputStream(b));if(d.readInt()!=MAGIC||PcScenarioIdentity.saved(w)==null)throw new IOException("Native policy source invalid");return d.readInt();}
    private PcNativeDebatePolicy(){}
}

package game.sanguo.core;
import java.io.IOException;
import java.util.Arrays;

/** Explicit native basic-command policy for fresh unmanaged authoring worlds.
 * Historical31-33 lack this marker and retain10AP/the old patrol formula.
 * Saved34+ base/growth strategy already opts into the native command model. */
final class BasicCityPolicy {
    static final String NAMESPACE="pc-basic-city-policy-v1";
    private static final byte[] NATIVE={0x42,0x43,0x50,0x31,20};
    private BasicCityPolicy(){}
    static void fresh(World w){w.extensions.put(NAMESPACE,NATIVE);}
    static void managed(World w){w.extensions.put(NAMESPACE,null);}
    static boolean nativeRules(World w){return w.officerAbilities.enabled()||Arrays.equals(w.extensions.get(NAMESPACE),NATIVE);}
    /** Frozen9548bb35 StrategyRules: no source/current ability inference. */
    static int legacyPatrol(int politics,int charm){return 5+(Math.max(0,Math.min(100,politics))+Math.max(0,Math.min(100,charm)))/20;}
    static void validate(World w)throws IOException{
        byte[] bytes=w.extensions.get(NAMESPACE);
        if(bytes!=null&&!Arrays.equals(bytes,NATIVE))throw new IOException("未知基础城市指令保存策略；未追填或替换");
    }
}

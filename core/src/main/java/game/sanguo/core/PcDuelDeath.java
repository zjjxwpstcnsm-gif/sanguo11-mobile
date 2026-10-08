package game.sanguo.core;
import java.io.*;
/** Original manager outcome2 enters4acbe0 directly: no captive selection,
 * no4a9120 confiscation and no200-merit capture reward. */
final class PcDuelDeath {
    static void validate(World w,World.Unit winner,World.Unit loser,int target,int killer)throws IOException {
        validate(w,winner,loser,target,killer,-1);
    }
    static void validate(World w,World.Unit winner,World.Unit loser,int target,int killer,int heir)throws IOException {
        if(!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3||PcGovernorPolicy.data(w).format!=3)throw new IOException("旧保存没有明确原单挑阵亡策略，完整终局已保留");
        var o=w.officer(target);var actor=w.officer(killer);
        if(o==null||actor==null||winner==null||actor.owner!=winner.owner||!w.army.contains(winner,killer)||!w.life.present(killer))throw new IOException("原单挑阵亡者/胜方上阵人物连接不同");
        recipient(w,target,killer);
        PcDuelExecution.validate(w,winner,loser,target,heir);
    }
    /** Full4aa680/489d40 uses the killer's CURRENT force ruler, distinct
     * from the active fighter, unit commander and immutable opening ruler. */
    static int recipient(World w,int target,int killer)throws IOException {
        PcNativeItemPolicy.held(w,target);if(w.treasures.held(target).isEmpty())return -1;
        var actor=w.officer(killer);var ruler=actor==null?null:w.loyalty.ruler(actor.owner);
        if(ruler==null||!w.life.present(ruler.id)||ruler.owner!=actor.owner||!PcDuelSourceFacts.saved(w).containsKey(ruler.id))throw new IOException("原阵亡携物的当前君主身份未知，完整终局已保留");
        PcNativeItemPolicy.held(w,ruler.id);return ruler.id;
    }
    static void apply(World w,World.Unit winner,World.Unit loser,int target,int killer)throws IOException {
        apply(w,winner,loser,target,killer,-1);
    }
    static void apply(World w,World.Unit winner,World.Unit loser,int target,int killer,int heir)throws IOException {
        validate(w,winner,loser,target,killer,heir);PcDuelExecution.applyNatural(w,winner,loser,target,recipient(w,target,killer),heir);
    }
    private PcDuelDeath(){}
}

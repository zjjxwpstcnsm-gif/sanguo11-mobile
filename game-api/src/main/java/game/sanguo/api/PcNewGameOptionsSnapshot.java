package game.sanguo.api;

import java.util.*;

/** Immutable source catalog facts for first launch without a campaign.
 * This is a source-bound draft description, with no StateToken, save or RNG. */
public final class PcNewGameOptionsSnapshot {
    public final String scenarioId,sourcePath,sourceSha,sharedSha,sourceVariant;
    public final String executableSha,receiptSha;
    public final List<String> sourceUnknown;
    public final int sourceFlag18;
    public final boolean defaultsKnown=false;
    /** Only the verified difficulty/death/life source override is covered. */
    public final boolean automaticOverridesKnown=true;
    public final List<PcOpeningOptionsSnapshot.Group> groups;

    public PcNewGameOptionsSnapshot(String id,String path,String sha,String shared,
            String variant,List<String> unknown,int flag,String exe,String receipt,
            List<PcOpeningOptionsSnapshot.Group> groups){
        scenarioId=Objects.requireNonNull(id);sourcePath=Objects.requireNonNull(path);
        sourceSha=Objects.requireNonNull(sha);sharedSha=Objects.requireNonNull(shared);
        sourceVariant=Objects.requireNonNull(variant);sourceUnknown=List.copyOf(unknown);
        sourceFlag18=flag;executableSha=Objects.requireNonNull(exe);receiptSha=Objects.requireNonNull(receipt);
        this.groups=List.copyOf(groups);
        if(this.groups.stream().anyMatch(g->g.savedValue!=null))throw new IllegalArgumentException("新局目录选项不能含当前存档值");
    }
}

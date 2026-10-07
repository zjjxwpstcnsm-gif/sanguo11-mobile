package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Source metadata only. Host preview is not Android normal-flow acceptance. */
public final class VoiceSourceMetadataProbe {
    public static void main(String[] args) throws Exception {
        for (var source : PcScenarioCatalog.all()) {
            World world = PcScenarioCatalog.preview(source.identity.scenarioId);
            byte[] before = SaveCodec.encode(world);
            var identities = PcOfficerInfo.saved(world);
            int count = 0;
            for (var person : PcScenarioPeople.saved(world)) {
                if (person.officerId < 0) continue;
                var identity = identities.get(person.officerId);
                if (identity == null || identity.nativeId != person.nativeId
                        || !identity.recordSha.equals(person.recordSha)) {
                    throw new AssertionError("Exact saved source join missing");
                }
                System.out.println(person.officerId + "\t" + person.nativeId + "\t"
                    + identity.sourceVariant + "\t" + identity.sourcePath + "\t"
                    + identity.sourceSha + "\t" + person.recordSha + "\t" + person.field(48));
                count++;
            }
            if (count != 670 || !Arrays.equals(before, SaveCodec.encode(world))) {
                throw new AssertionError("Complete source metadata or full Save/RNG purity missing");
            }
        }
    }
}

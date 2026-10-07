package game.sanguo.mobile;

import game.sanguo.api.StateToken;
import java.util.Map;

/** Adapter admission regression. Explicit identity fixture, not a real voice caller. */
public final class VoiceDirectiveDomainsProbe {
    public static void main(String[] args) {
        String hash = "0".repeat(64);
        StateToken state = new StateToken("domain-fixture", 1, 2);
        int accepted = 0, mismatchesRejected = 0;
        for (int type = 0; type < 8; type++) {
            var identity = new PortraitMediaIdentity(1, 1, "fixture", "fixture", hash, hash,
                null, type, Map.of(48, type));
            for (int profile = 0; profile <= 70; profile++) {
                for (var selector : PcVoiceDirective.Selector.values()) {
                    var directive = new PcVoiceDirective(state, "fact", "parent", "phase", identity,
                        type, profile, 0, false, 1, selector, new int[]{50, 60, 40, 30}, 0, null);
                    if (directive.state != state || directive.voiceTypeRaw != type
                            || directive.nativeProfile != profile || directive.nativeVoiceId < 0) {
                        throw new AssertionError("Valid actor type and distinct action profile rejected or changed");
                    }
                    accepted++;
                    try {
                        new PcVoiceDirective(state, "fact", "parent", "phase", identity,
                            (type+1)%8, profile, 0, false, 1, selector, new int[]{50, 60, 40, 30}, 0, null);
                        throw new AssertionError("Mismatched saved original voice type accepted");
                    } catch (IllegalArgumentException expected) {
                        mismatchesRejected++;
                    }
                }
            }
        }
        if (accepted != 1704 || mismatchesRejected != 1704) {
            throw new AssertionError("All source type/profile/selector domains not covered");
        }
        System.out.println("PASS explicit voice domain admission accepted="+accepted+" mismatchesRejected="+mismatchesRejected
            +"; original event/speaker/playback and Android acceptance not claimed");
    }
}

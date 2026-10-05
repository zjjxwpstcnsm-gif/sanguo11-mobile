package game.sanguo.mobile;

import game.sanguo.api.StateToken;
import java.util.Objects;

/** Pending producer contract for source caller 57fc70 -> 5880e0.
 * All values belong to state; no project owner/calendar conversion occurs here. */
final class PcMapMusicDirective {
    final StateToken state;
    final String id, parentId, presentationParentId, establishedScene;
    final Integer controlSlotRaw, originalOwnedCities, seasonRaw;
    final Boolean sourceFactionValid, predicate587f00, predicate587d70, predicate587fb0;

    PcMapMusicDirective(StateToken state, String id, String parentId, String presentationParentId,
            String establishedScene, Integer controlSlotRaw, Boolean sourceFactionValid,
            Boolean predicate587f00, Boolean predicate587d70, Boolean predicate587fb0,
            Integer originalOwnedCities, Integer seasonRaw) {
        this.state = Objects.requireNonNull(state);
        this.id = required(id); this.parentId = required(parentId);
        this.presentationParentId = required(presentationParentId);
        this.establishedScene = required(establishedScene);
        this.controlSlotRaw = controlSlotRaw; this.sourceFactionValid = sourceFactionValid;
        this.predicate587f00 = predicate587f00; this.predicate587d70 = predicate587d70;
        this.predicate587fb0 = predicate587fb0; this.originalOwnedCities = originalOwnedCities;
        this.seasonRaw = seasonRaw;
    }
    private static String required(String value) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("Missing source music identity");
        return value;
    }
    int selectedMusicId() {
        if (controlSlotRaw == null || controlSlotRaw < 0 || controlSlotRaw > 7) return PcMusicPolicy.UNBOUND;
        return PcMusicPolicy.select(sourceFactionValid, predicate587f00, predicate587d70,
            predicate587fb0, originalOwnedCities, seasonRaw);
    }
}

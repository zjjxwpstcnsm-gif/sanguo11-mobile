package game.sanguo.mobile;

/** Read-only translation of source 5880e0. Inputs must describe one committed source state.
 * Scene/caller identity is established separately; unknown inputs never mean false. */
final class PcMusicPolicy {
    static final int UNBOUND = -1;
    private PcMusicPolicy() {}

    static int select(Boolean sourceFactionValid, Boolean predicate587f00,
            Boolean predicate587d70, Boolean predicate587fb0,
            Integer originalOwnedCities, Integer seasonRaw) {
        if (!Boolean.TRUE.equals(sourceFactionValid) || predicate587f00 == null) return UNBOUND;
        if (predicate587f00) return 9;
        if (predicate587d70 == null) return UNBOUND;
        if (predicate587d70) return sized(originalOwnedCities, 8, 10);
        if (predicate587fb0 == null) return UNBOUND;
        if (predicate587fb0) return sized(originalOwnedCities, 7, 11);
        if (seasonRaw == null) return UNBOUND;
        return seasonRaw >= 0 && seasonRaw <= 3 ? 3 + seasonRaw : 7;
    }

    private static int sized(Integer count, int small, int large) {
        if (count == null || count < 0 || count > 42) return UNBOUND;
        return count >= 10 ? large : small;
    }
}

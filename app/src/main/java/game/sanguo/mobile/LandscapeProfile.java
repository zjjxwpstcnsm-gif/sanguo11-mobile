package game.sanguo.mobile;

/** Versioned, camera/quality-independent proportions for the bundled landscape family.
 * Shared by the normal streaming path everywhere; no showcase-coordinate condition. */
final class LandscapeProfile {
    static final String ID="central-plains-r09-v1";
    static final int VERSION=1;
    static final float TREE_SCALE=.86f, PATH_HALF_WIDTH=.085f, PLANK_HALF_WIDTH=.105f;
    static final float JUNCTION_TRIM=.22f, SURFACE_LIFT=.012f;
    static float sceneryScale(int family,float variation){return variation*(family<2?TREE_SCALE:1);}
    static float pathWidth(boolean plank){return plank?PLANK_HALF_WIDTH:PATH_HALF_WIDTH;}
    private LandscapeProfile(){}
}

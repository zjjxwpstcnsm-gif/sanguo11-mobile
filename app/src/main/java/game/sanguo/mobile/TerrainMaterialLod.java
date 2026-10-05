package game.sanguo.mobile;

/** Material detail follows the normal coarse geometry threshold, with hysteresis.
 * This changes neither geometry nor picking, and is independent of tests/device/OS. */
final class TerrainMaterialLod {
    private TerrainMaterialLod() {}
    static boolean select(boolean overview,float span){
        if(!Float.isFinite(span))return overview;
        return overview?span>=36f:span>=40f;
    }
}

package game.sanguo.mobile;

/** Match the explicit conversion camera's1..1000 source-depth interval.
 * Preserve the map's projection size/aspect and rigid pose. Its zoom-dependent
 * far plane must not clip the original screen layers. Fullscreen PC FOV/aspect
 * calibration remains pending; these values match pc_presentation_source.py. */
final class PcPresentationLens {
    static final double NEAR=PcEffectCoordinates.SCALE;
    static final double FAR=1000*PcEffectCoordinates.SCALE;
    static void depthRange(double[] projection){
        if(projection.length!=16||Math.abs(projection[11]+1)>1e-9||projection[15]!=0)
            throw new IllegalArgumentException("Original presentation expects a perspective projection");
        projection[10]=-(FAR+NEAR)/(FAR-NEAR);
        projection[14]=-2*FAR*NEAR/(FAR-NEAR);
    }
    private PcPresentationLens(){}
}

package game.sanguo.mobile;

/** Source-labelled infantry actions; explicit committed primary hit is required. */
final class PcTacticSoundPolicy {
    static int choose(int nativeTacticId,boolean appliedPrimaryHit,boolean appliedCritical){
        return appliedPrimaryHit&&nativeTacticId>=0&&nativeTacticId<=8?(appliedCritical?78:49):-1;
    }
    private PcTacticSoundPolicy(){}
}

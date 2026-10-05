package game.sanguo.mobile;

/** Original labelled spear tactic namespace, applied critical outcome only. */
final class PcTacticSoundPolicy {
    static int choose(int nativeTacticId,boolean appliedCritical){
        return nativeTacticId>=0&&nativeTacticId<=2?(appliedCritical?78:49):-1;
    }
    private PcTacticSoundPolicy(){}
}

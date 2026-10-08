package game.sanguo.core;

/** Original5c4f80 and4afd60 modes1/2 arithmetic. The original4af7d0
 * forced relationship/eligibility gate MUST run separately before fallback.
 * These functions alone do not admit an ordinary campaign recruitment. */
final class PcDuelRecruitmentRules {
    /** Caller must bind original valid-person/virtual-force/property75 facts.
     * Initial references cannot substitute for changed campaign relationships. */
    static final class Gate {
        int mode,targetNative,actorNative,rulerNative,refusedRuler=-1,targetForce=-1,rulerForce=-1;
        boolean targetValid=true,actorValid=true,rulerValid=true,ruler;
        int swornNative=-1,swornForce=-1,spouseNative=-1,spouseForce=-1,oldRuler=-1;
        boolean swornValid,spouseValid,swornProperty75Old,swornProperty75New,
                dislikesRuler,dislikesActor,likesOldRuler,likesNewRuler;
    }
    /** -1 = original unhandled fallback; 0/1 = handled refusal/success. */
    static int forced(Gate g){
        if(g.mode!=1&&g.mode!=2)throw new IllegalArgumentException("Field recruitment modes1/2 required");
        if(!g.targetValid||!g.actorValid||!g.rulerValid)return 0;
        boolean owned=g.mode!=2&&force(g.targetForce);
        if(g.refusedRuler==g.rulerNative||g.ruler&&owned)return 0;
        boolean sworn=g.swornValid&&g.swornNative!=g.targetNative;
        if(sworn){if(owned&&force(g.targetForce)&&g.swornForce==g.targetForce)return 0;if(g.swornNative==g.rulerNative||g.swornNative==g.actorNative)return 1;}
        if(g.spouseValid&&owned&&force(g.targetForce)&&g.spouseForce==g.targetForce)return 0;
        if(owned&&sworn&&force(g.targetForce)&&g.swornProperty75Old)return 0;
        if(sworn&&(owned||g.swornForce!=g.targetForce)&&force(g.swornForce)&&g.swornForce!=g.rulerForce)return 0;
        if(g.spouseValid&&(owned||g.spouseForce!=g.targetForce)&&force(g.spouseForce)&&g.spouseForce!=g.rulerForce)return 0;
        if(g.spouseValid&&(g.spouseNative==g.rulerNative||g.spouseNative==g.actorNative))return 1;
        if(g.dislikesRuler||g.dislikesActor)return 0;
        if(g.swornProperty75New)return 1;
        if(g.spouseValid&&g.spouseForce==g.rulerForce)return 1;
        if(owned&&!g.ruler&&g.oldRuler>=0&&g.oldRuler<1100&&g.likesOldRuler)return 0;
        return g.likesNewRuler?1:-1;
    }
    private static boolean force(int n){return n>=0&&n<47;}
    static int probability(int rawLoyalty,int honor,int mode,int oldGap,int newGap,int charm,
                           int captiveBonus,int familyPenalty,int likedPenalty,int dislikedBonus,
                           int actorNative,int targetNative,int rulerNative,int adjustmentArgument){
        if(rawLoyalty<0||rawLoyalty>255||honor<0||honor>4||(mode!=1&&mode!=2))throw new IllegalArgumentException("Original field recruitment inputs invalid");
        int adjustment=PcCommandRoll.calculate(5-honor,actorNative,targetNative,charm,adjustmentArgument,rulerNative,0,0);
        return PcRecruitmentFormula.calculate(mode==2?Math.min(rawLoyalty,70):rawLoyalty,honor,oldGap,newGap,Math.max(30,charm),captiveBonus,familyPenalty,likedPenalty,dislikedBonus,adjustment);
    }
    static int chance(int probability,int honor){
        if(probability<0||honor<0||honor>4)throw new IllegalArgumentException("Original recruitment probability invalid");
        return Math.min(100,probability*Math.min(10,15-2*honor)/10);
    }
    static boolean decision(int probability,int honor,PcDuelKernel.Random random){return random.percent(chance(probability,honor));}
    private PcDuelRecruitmentRules(){}
}

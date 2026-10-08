package game.sanguo.core;
/** Original4b03d0/4adb30/4aed40/4afed0 numeric rules. Caller supplies
 * original current predicates/settings and must apply full fate callbacks. */
final class PcDuelAiDispositionRules {
    static int optionPenalty(int option){return option==0?20:option==1?10:option==2?0:10;}
    static boolean forcedExecution(boolean targetRuler,boolean captorDislikesTarget,
                                   boolean targetForceField3cZero,boolean captorForceField3cZero,
                                   boolean targetPredicate481910,boolean captorPredicate481910){
        return captorDislikesTarget||targetRuler&&(targetForceField3cZero&&captorPredicate481910||targetPredicate481910&&captorForceField3cZero);
    }
    static int executionChance(boolean ruler,int[]current,int merit,int targetAmbition,
                             int captorAmbition,int captorHonor,int captorPersonality,
                             int option,boolean rawSwornEqual,boolean captorSpouse,boolean captorLikes,
                             PcDuelKernel.Random random){
        if(current.length!=5||merit<0||merit>65535||targetAmbition<0||targetAmbition>4||captorAmbition<0||captorAmbition>4||captorHonor<0||captorHonor>4||captorPersonality<0||captorPersonality>4)throw new IllegalArgumentException("Original AI disposition inputs invalid");
        // Raw equality includes-1/-1; do not add a valid-group condition.
        if(rawSwornEqual||captorSpouse||captorLikes)return -1;
        int max=0,sum=0;for(int value:current){if(value<0||value>255)throw new IllegalArgumentException("Original current stat byte invalid");max=Math.max(max,value);sum+=value;}max=Math.max(50,max);int average=Math.max(50,sum/5),chance;
        if(ruler){int offset=random.uniform(20);chance=Math.max(0,2*(targetAmbition+captorAmbition)+(16-captorHonor)*5-(max+average)/2-optionPenalty(option)-offset);}
        else {float meritFactor=captorPersonality==1?1.5f:1.0f,randomFactor=captorPersonality==4?1.5f:1.0f;int ambitionFactor=captorPersonality==3?5:1;float baseline=(float)(100-(max+average)/2)-(Math.max(20000,merit)/2000)*meritFactor-targetAmbition*ambitionFactor;int offset=random.uniform(25);chance=(int)Math.max(0.0f,baseline-offset*randomFactor-optionPenalty(option));}
        return chance;
    }
    static boolean execution(boolean ruler,int[]current,int merit,int targetAmbition,
                             int captorAmbition,int captorHonor,int captorPersonality,
                             int option,boolean rawSwornEqual,boolean captorSpouse,boolean captorLikes,
                             PcDuelKernel.Random random){return random.percent(executionChance(ruler,current,merit,targetAmbition,captorAmbition,captorHonor,captorPersonality,option,rawSwornEqual,captorSpouse,captorLikes,random));}
    static boolean retain(int troops,int rankSalarySum,int prisonerSkillCount){
        if(troops<0||rankSalarySum<0||prisonerSkillCount<0)throw new IllegalArgumentException("Original detention capacity inputs invalid");
        return (rankSalarySum+(prisonerSkillCount+2)*50)*2<=troops;
    }
    private PcDuelAiDispositionRules(){}
}

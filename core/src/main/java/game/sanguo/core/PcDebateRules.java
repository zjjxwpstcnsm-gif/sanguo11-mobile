package game.sanguo.core;

/** Original native51d470/51e900/51e960/51ea10 primitives. Native IDs stay explicit.
 * The full saved-state engine must be verified before any ordinary world uses it. */
final class PcDebateRules {
    private PcDebateRules(){}
    static final int RECONSIDER=0,SHOUT=10,GUILE=11,IGNORE=12,CALM=13,RAGE=14;
    static final int TIMID=0,STEADY=1,BOLD=2,RASH=3;
    static int handSlots(int currentIntelligence){return currentIntelligence<70?4:currentIntelligence<80?5:currentIntelligence<90?6:7;}
    static int topic(int card){return card>=1&&card<=9?(card-1)/3:-1;}
    static int size(int card){return card>=1&&card<=9?(card-1)%3:-1;}
    private static int score(int currentTopic,int card){
        if(card==IGNORE)return 120;if(card==SHOUT)return 110;if(card==GUILE)return 20;
        if(card>=1&&card<=9)return size(card)+1+(currentTopic>=0&&currentTopic<=2&&topic(card)==currentTopic?10:0);return 0;
    }
    /** Original result: -1 tie,0 left,1 right. Both post-action talks have score0. */
    static int compare(int currentTopic,int left,int right,int leftTemper,int leftFury,int rightTemper,int rightFury){
        int a=leftFury>0&&leftTemper==BOLD?100:score(currentTopic,left),b=rightFury>0&&rightTemper==BOLD?100:score(currentTopic,right);return a>b?0:a<b?1:-1;
    }
    static int intelligenceModifier(int currentIntelligence,int opponentIntelligence){return 100+40*(currentIntelligence-opponentIntelligence)/(131-currentIntelligence);}
    static int health(int value){return Math.max(-100,Math.min(1000,value));}
    static int anger(int value){return Math.max(0,Math.min(100,value));}
    /**51ea10: original signed32bit multiply and division; exactly one uniform5. */
    static int damage(int currentTopic,int card,int temper,int fury,int modifier,PcMerchantRules.Draws random){
        int topicFactor=6;
        if(fury>0){if(temper==BOLD||temper==TIMID)topicFactor=10;}
        else if(card==SHOUT)topicFactor=12;
        else if(topic(card)==currentTopic)topicFactor=10;
        int cardFactor=card==SHOUT?15:size(card)<0?0:new int[]{10,15,20}[size(card)];
        int furyFactor=fury>0&&temper==STEADY?15:10;
        int result=modifier+random.uniform(5);result*=furyFactor;result*=cardFactor;result*=topicFactor;return result/1000;
    }
    static int ordinaryAnger(int card){return card==SHOUT?15:size(card)<0?0:new int[]{10,15,20}[size(card)];}
    /** Original51e3c0. Availability is separate from actually holding the card. */
    static boolean legal(int card,int ownTemper,int ownFury,int opponentTemper,int opponentFury,boolean reconsiderAvailable){
        if(card<0||card>14)return false;
        boolean ownRestricted=ownFury>0&&(ownTemper==STEADY||ownTemper==BOLD);
        boolean opponentSteady=opponentFury>0&&opponentTemper==STEADY;
        if(card==RECONSIDER)return reconsiderAvailable&&(!ownRestricted||ownTemper==STEADY);
        if(card>=SHOUT&&card<=IGNORE)return (!ownRestricted||ownTemper==STEADY)&&!opponentSteady;
        if(card==CALM||card==RAGE)return !ownRestricted&&!opponentSteady;
        return true;
    }
}

package game.sanguo.core;

/** Original5167d0..517711 AI port. No campaign invokes this staged class until
 * complete contest protocol and saved-state integration are verified. */
final class PcDebateAi {
    final PcDebateState state;
    final int side;
    PcDebateAi(PcDebateState state,int side){this.state=state;state.speaker(side);this.side=side;}
    private PcDebateState.Speaker self(){return state.speaker(side);}
    private PcDebateState.Speaker other(){return state.speaker(1-side);}
    private int find(int card){PcDebateState.Speaker s=self();for(int i=0;i<s.slots;i++)if(s.hand[i]==card)return i;return -1;}
    private int legalSlot(int card){int slot=find(card);return slot>=0&&state.legal(side,card)?slot:-1;}
    /** Original516770 counts duplicate legal cards, not distinct card types. */
    int count(int card){if(!state.legal(side,card))return 0;int n=0;PcDebateState.Speaker s=self();for(int i=0;i<s.slots;i++)if(s.hand[i]==card)n++;return n;}
    /** Original5167d0/516a80: least-size ordinary card, random50 on size ties. */
    private int leastOrdinary(){
        int chosen=-1,size=Integer.MAX_VALUE;PcDebateState.Speaker s=self();
        for(int i=0;i<s.slots;i++){int card=s.hand[i];if(card<1||card>9||!state.legal(side,card))continue;int next=PcDebateRules.size(card);
            if(next<size||(next==size&&state.random.percent(50))){size=next;chosen=i;}
        }return chosen;
    }
    int boldFury(){return self().fury>0&&self().temper==PcDebateRules.BOLD?leastOrdinary():-1;}
    /** Original5168a0. Topic large-card map is original8b44c8:{3,6,9}. */
    int steadyFury(){
        if(self().fury<=0||self().temper!=PcDebateRules.STEADY)return -1;
        int strong=count((state.topic+1)*3)+count(PcDebateRules.IGNORE)+count(PcDebateRules.GUILE)+count(PcDebateRules.SHOUT);
        if(strong<=0){int reconsider=legalSlot(0);if(reconsider>=0)return reconsider;}
        if(state.random.percent(30))return legalSlot(0);return -1;
    }
    /** Original516980, including duplicate counter-card thresholds. */
    int opposeBoldFury(){
        if(other().fury<=0||other().temper!=PcDebateRules.BOLD)return -1;
        int slot=legalSlot(PcDebateRules.SHOUT);if(slot>=0)return slot;
        slot=legalSlot(PcDebateRules.IGNORE);if(slot>=0)return slot;
        int calm=count(PcDebateRules.CALM),rage=count(PcDebateRules.RAGE);
        if(calm>1&&rage>1){slot=legalSlot(PcDebateRules.RAGE);if(slot>=0)return slot;}
        if(calm>2&&rage==0){slot=legalSlot(PcDebateRules.CALM);if(slot>=0)return slot;}
        return leastOrdinary();
    }
    int opposeSteadyFury(){return other().fury>0&&other().temper==PcDebateRules.STEADY?legalSlot(PcDebateRules.IGNORE):-1;}
    int steadyStrong(){
        if(self().fury<=0||self().temper!=PcDebateRules.STEADY)return -1;
        for(int card:new int[]{10,11,(state.topic+1)*3}){int slot=legalSlot(card);if(slot>=0)return slot;}return -1;
    }
    int specials(int mode,int chance,int otherCard){
        if(mode==1){if(self().temper!=PcDebateRules.TIMID)return -1;}
        else if(mode==2){if(otherCard<0||otherCard>=15||otherCard>=1&&otherCard<=9)return -1;}
        else if(!state.random.percent(chance))return -1;
        if(mode==2){int slot=legalSlot(12);if(slot>=0)return slot;}
        for(int card:new int[]{10,11}){int slot=legalSlot(card);if(slot>=0)return slot;}
        return mode==2?-1:legalSlot(12);
    }
    private int topicCount(PcDebateState.Speaker speaker){int count=0;for(int i=0;i<speaker.slots;i++){int card=speaker.hand[i];if(card>=1&&card<=9&&PcDebateRules.topic(card)==state.topic)count++;}return count;}
    int topicOrdered(boolean largeFirst){int base=state.topic*3;int[] order=largeFirst?new int[]{base+3,base+2,base+1}:new int[]{base+1,base+2,base+3};for(int card:order){int slot=legalSlot(card);if(slot>=0)return slot;}return -1;}
    int uncontestedTopic(){return topicCount(other())==0?topicOrdered(false):-1;}
    int rageChance(){int anger=self().anger,chance=anger>=70?100:anger>=60?40:anger>=50?10:0;return state.random.percent(chance)?legalSlot(14):-1;}
    int calmChance(){if((count(14)==0&&count(13)<=1)||self().health<200)return -1;return state.random.percent(50)?legalSlot(13):-1;}
    int reconsiderNoTopic(int force){
        if(topicCount(self())>0||force==0&&topicCount(other())==0)return -1;
        int chance=force!=0?100:70;if(count(10)+count(11)+count(12)>0)chance/=2;
        return state.random.percent(chance)?legalSlot(0):-1;
    }
    int chanceReconsider(int chance){return state.random.percent(chance)?legalSlot(0):-1;}
    int ignoreAngry(){return other().anger>=70&&count(14)>0?legalSlot(12):-1;}
    int largestOrdinary(){
        int chosen=-1,size=Integer.MIN_VALUE;PcDebateState.Speaker s=self();
        for(int i=0;i<s.slots;i++){int card=s.hand[i];if(card<1||card>9||!state.legal(side,card))continue;int next=PcDebateRules.size(card);
            if(next>size||(next==size&&state.random.percent(50))){size=next;chosen=i;}
        }return chosen;
    }
    private int shuffled(int[] slots,int count){
        if(count<=0)return -1;
        if(count>2)for(int i=0;i<1000;i++){int from=state.random.uniform(count),to=state.random.uniform(count);if(from!=to){int slot=slots[from];slots[from]=slots[to];slots[to]=slot;}}
        return slots[0];
    }
    /** Original5173f0 contains a second GUILE lookup guarded by IGNORE legality.
     * Preserve that original duplicated candidate rather than correcting it. */
    int randomUseful(){
        int[] slots=new int[6];int count=0,slot=legalSlot(10);if(slot>=0)slots[count++]=slot;
        slot=legalSlot(11);if(slot>=0)slots[count++]=slot;
        slot=find(11);if(slot>=0&&state.legal(side,12))slots[count++]=slot;
        for(int card=state.topic*3+1;card<=state.topic*3+3;card++){slot=legalSlot(card);if(slot>=0)slots[count++]=slot;}
        return shuffled(slots,count);
    }
    /** Original517560. >2 choices triggers1000 pairs of original uniform draws. */
    int fallback(){
        PcDebateState.Speaker s=self();int[] slots=new int[7];int count=0;
        for(int i=0;i<s.slots;i++)if(s.hand[i]>=0&&s.hand[i]<15&&state.legal(side,s.hand[i]))slots[count++]=i;
        if(count==0)throw new IllegalStateException("No original legal-card precondition");
        return shuffled(slots,count);
    }
    private static final int[][] ON_TOPIC={{0x5167d0,0,0},{0x5168a0,0,0},{0x516980,0,0},{0x516b00,0,0},{0x516c30,0,60},{0x517060,1,0},{0x516e30,0,0},{0x517280,30,0},{0x516c30,0,100},{0x516fd0,0,0},{0x517340,0,0}};
    private static final int[][] OFF_TOPIC={{0x5167d0,0,0},{0x5168a0,0,0},{0x516980,0,0},{0x516b00,0,0},{0x516c30,2,0},{0x516c30,0,30},{0x517060,0,0},{0x516ec0,0,0},{0x517280,30,0},{0x516c30,0,100},{0x516fd0,0,0},{0x517340,0,0}};
    private static final int[][] UNSEEN={{0x5167d0,0,0},{0x5168a0,0,0},{0x516980,0,0},{0x516b00,0,0},{0x516d60,0,0},{0x516b60,0,0},{0x517060,0,0},{0x5172e0,0,0},{0x516c30,1,0},{0x516c30,0,40},{0x516e30,0,0},{0x516c30,0,100},{0x516f50,0,0},{0x517280,30,0},{0x516fd0,0,0},{0x517340,0,0}};
    private static final int[][] SIMPLE={{0x5173f0,0,0},{0x517280,50,0},{0x517560,0,0}};
    private int evaluate(int[] entry,int otherCard){
        switch(entry[0]){
            case 0x5167d0:return boldFury();case 0x5168a0:return steadyFury();case 0x516980:return opposeBoldFury();case 0x516b00:return opposeSteadyFury();
            case 0x516b60:return steadyStrong();case 0x516c30:return specials(entry[1],entry[2],otherCard);case 0x516d60:return uncontestedTopic();
            case 0x516e30:return topicOrdered(true);case 0x516ec0:return topicOrdered(false);case 0x516f50:return rageChance();case 0x516fd0:return calmChance();
            case 0x517060:return reconsiderNoTopic(entry[1]);case 0x517280:return chanceReconsider(entry[1]);case 0x5172e0:return ignoreAngry();
            case 0x517340:return largestOrdinary();case 0x5173f0:return randomUseful();case 0x517560:return fallback();default:throw new IllegalStateException("Unknown original AI table function");
        }
    }
    int choose(int otherCard){
        int[][] table=otherCard>=1&&otherCard<=9&&PcDebateRules.topic(otherCard)==state.topic?ON_TOPIC:otherCard>=0&&otherCard<15&&PcDebateRules.topic(otherCard)!=state.topic?OFF_TOPIC:UNSEEN;
        int chance=Math.max(0,60-self().intelligence)/2;
        if(state.random.percent(chance))table=SIMPLE;
        if(!state.random.percent(chance))for(int[] entry:table){int slot=evaluate(entry,otherCard);if(slot>=0)return slot;}
        return fallback();
    }
}

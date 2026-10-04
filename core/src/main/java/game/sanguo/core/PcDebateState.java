package game.sanguo.core;

import java.util.*;

/** Direct native51d220/51d2b0/51d610/51da70 state port. Not enabled on a campaign
 *until the complete frame/AI/settlement/save protocol passes native traces. */
final class PcDebateState {
    static final class Speaker {
        final int intelligence,temper,talkMask,modifier,slots;
        int health=1000,anger,fury,deckCursor;
        final int[] deck=new int[18],hand=new int[7],specialPool=new int[5];
        final int specialCount;
        Speaker(int intelligence,int otherIntelligence,int temper,int talkMask,PcMerchantRules.Draws random){
            if(intelligence<0||intelligence>100||temper<0||temper>3||talkMask<0||talkMask>31)throw new IllegalArgumentException("Unexamined native speaker input");
            this.intelligence=intelligence;this.temper=temper;this.talkMask=talkMask;modifier=PcDebateRules.intelligenceModifier(intelligence,otherIntelligence);slots=PcDebateRules.handSlots(intelligence);
            Arrays.fill(deck,-1);Arrays.fill(hand,-1);Arrays.fill(specialPool,-1);int count=0;for(int i=0;i<5;i++)if((talkMask&(1<<i))!=0)specialPool[count++]=10+i;specialCount=count;
            shuffle(random);refill(random);
        }
        void shuffle(PcMerchantRules.Draws random){
            int[] weights={4,1,7,5,2,8,6,3,9};int index=0;
            for(int i=0;i<8;i++)deck[index++]=weights[random.uniform(3)];
            for(int i=0;i<4;i++)deck[index++]=weights[3+random.uniform(3)];
            for(int i=0;i<2;i++)deck[index++]=weights[6+random.uniform(3)];
            if(specialCount>0)for(int i=0;i<2000;i++){int from=i%specialCount,to=random.uniform(specialCount),value=specialPool[from];specialPool[from]=specialPool[to];specialPool[to]=value;}
            int special=0;while(index<18)deck[index++]=special<specialCount?specialPool[special++]:weights[random.uniform(9)];
            for(int i=0;i<2000;i++){int from=i%18,to=random.uniform(18),value=deck[from];deck[from]=deck[to];deck[to]=value;}
            deckCursor=0;
        }
        void refill(PcMerchantRules.Draws random){
            hand[0]=0;int lastRefilled=-1;
            for(int i=1;i<slots;i++)if(hand[i]==-1){lastRefilled=i;if(deckCursor>=18)shuffle(random);hand[i]=deck[deckCursor++];}
            boolean ordinary=false;for(int i=1;i<slots;i++)if(hand[i]>=1&&hand[i]<=9)ordinary=true;
            if(!ordinary){if(lastRefilled<0)throw new IllegalStateException("Original refill precondition violated");hand[lastRefilled]=1+random.uniform(9);}
            Arrays.sort(hand,0,slots);
        }
        int select(int index){
            if(index<0||index>=slots||hand[index]<0||hand[index]>=15)throw new IllegalArgumentException("Original hand selection rejected");int card=hand[index];
            if(index>0){hand[index]=-1;int[] before=hand.clone();Arrays.fill(hand,-1);int to=0;for(int i=0;i<slots;i++)if(before[i]!=-1)hand[to++]=before[i];}return card;
        }
    }
    final PcMerchantRules.Random random;
    final Speaker left,right;
    int topic;
    final boolean[] reconsiderAvailable={true,true};
    Speaker speaker(int side){if(side==0)return left;if(side==1)return right;throw new IllegalArgumentException("Native side must be 0 or 1");}
    boolean legal(int side,int card){Speaker self=speaker(side),other=speaker(1-side);return PcDebateRules.legal(card,self.temper,self.fury,other.temper,other.fury,reconsiderAvailable[side]);}
    void tie(){left.anger=PcDebateRules.anger(left.anger+15);right.anger=PcDebateRules.anger(right.anger+15);}
    void damageEffect(int side,int damage,int anger,boolean reflected){Speaker target=speaker(reflected?side:1-side);target.health=PcDebateRules.health(target.health-damage);target.anger=PcDebateRules.anger(target.anger+anger);}
    void calmEffect(int side,int amount,boolean reflected){Speaker target=speaker(reflected?side:1-side);target.anger=PcDebateRules.anger(target.anger+amount);}
    void rageEffect(int side,int amount,boolean reflected){Speaker target=speaker(reflected?1-side:side);target.anger=PcDebateRules.anger(target.anger+amount);}
    /** Original519fd0 queue generation +51cedb..51cf3c hand effect only.
     * This is not the complete fury state transition. The two animation choices
     * consume original RNG here so a future renderer only consumes the record. */
    int[] counterQueue(int side,int counter){
        if(counter!=-1&&counter!=PcDebateRules.CALM&&counter!=PcDebateRules.RAGE)throw new IllegalArgumentException("Unexamined native counter");
        int first=random.percent(50)?7:8,second=-1;
        if(counter!=-1){
            Speaker target=speaker(1-side);for(int i=0;i<target.slots;i++)if(target.hand[i]==counter){target.select(i);break;}
            second=counter==PcDebateRules.CALM?(random.percent(50)?9:10):(random.percent(50)?7:8);
        }
        return new int[]{first,second};
    }
    PcDebateState(int leftIntelligence,int rightIntelligence,int leftTemper,int rightTemper,int leftTalks,int rightTalks,int seed){
        random=new PcMerchantRules.Random(seed);topic=random.uniform(3);
        left=new Speaker(leftIntelligence,rightIntelligence,leftTemper,leftTalks,random);
        right=new Speaker(rightIntelligence,leftIntelligence,rightTemper,rightTalks,random);
    }
}

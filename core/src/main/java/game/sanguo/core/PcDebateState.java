package game.sanguo.core;

import java.util.*;

/** Direct native51d220/51d2b0/51d610/51da70 state port. Not enabled on a campaign
 *until the complete frame/AI/settlement/save protocol passes native traces. */
final class PcDebateState {
    static final class Speaker {
        final int intelligence,war,temper,talkMask,modifier,slots;
        int health=1000,anger,fury,deckCursor;
        final int[] deck=new int[18],hand=new int[7],specialPool=new int[5];
        final int specialCount;
        Speaker(int intelligence,int war,int otherIntelligence,int temper,int talkMask,PcMerchantRules.Draws random){
            if(intelligence<0||intelligence>100||war<0||war>100||temper<0||temper>3||talkMask<0||talkMask>31)throw new IllegalArgumentException("Unexamined native speaker input");
            this.intelligence=intelligence;this.war=war;this.temper=temper;this.talkMask=talkMask;modifier=PcDebateRules.intelligenceModifier(intelligence,otherIntelligence);slots=PcDebateRules.handSlots(intelligence);
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
    int topic,leader=-1,selectedWinner=-1;
    final boolean[] reconsiderAvailable={true,true};
    final int[] stage={0,0};
    final boolean[] stageChanged={false,false};
    int furySide=-1,burstSide,burstIndex;
    boolean burstActive;
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
        speaker(side);
        if(counter!=-1&&counter!=PcDebateRules.CALM&&counter!=PcDebateRules.RAGE)throw new IllegalArgumentException("Unexamined native counter");
        int first=random.percent(50)?7:8,second=-1;
        if(counter!=-1){
            Speaker target=speaker(1-side);for(int i=0;i<target.slots;i++)if(target.hand[i]==counter){target.select(i);break;}
            second=counter==PcDebateRules.CALM?(random.percent(50)?9:10):(random.percent(50)?7:8);
        }
        return new int[]{first,second};
    }
    /** Bounded original51e960/51f9e0/51fed0/51e7d0 sequence. Selection,
     * refill, fury activation, AI, terminal choice and UI callbacks are separate. */
    void selectedCardEffects(int leftCard,int rightCard){
        if(leftCard<1||leftCard>14||rightCard<1||rightCard>14)throw new IllegalArgumentException("Native rethink transition not yet ported");
        int[] cards={leftCard,rightCard};selectedWinner=PcDebateRules.compare(topic,leftCard,rightCard,left.temper,left.fury,right.temper,right.fury);
        if(selectedWinner<0)tie();
        else{
            int winningCard=cards[selectedWinner],otherCard=cards[1-selectedWinner];Speaker actor=speaker(selectedWinner);
            if(winningCard==PcDebateRules.IGNORE)calmEffect(selectedWinner,30,false);
            else if(winningCard==PcDebateRules.GUILE){
                if(otherCard>=1&&otherCard<=9){int damage=PcDebateRules.damage(topic,otherCard,actor.temper,actor.fury,actor.modifier,random);if(damage>0)damageEffect(1-selectedWinner,damage,PcDebateRules.ordinaryAnger(otherCard),true);}
            }else damageEffect(selectedWinner,PcDebateRules.damage(topic,winningCard,actor.temper,actor.fury,actor.modifier,random),PcDebateRules.ordinaryAnger(winningCard),false);
            postCard(selectedWinner,cards);
            if(winningCard!=PcDebateRules.IGNORE)postCard(1-selectedWinner,cards);
        }
        if(left.fury>0)left.fury--;if(right.fury>0)right.fury--;
        if(selectedWinner>=0){
            leader=selectedWinner;int winningCard=cards[selectedWinner];
            if(winningCard>=1&&winningCard<=9)topic=PcDebateRules.topic(winningCard);
            boolean guile=(leftCard==PcDebateRules.GUILE&&rightCard!=PcDebateRules.IGNORE)||(rightCard==PcDebateRules.GUILE&&leftCard!=PcDebateRules.IGNORE);
            if(left.health>0&&right.health>0&&guile)topic=random.uniform(3);
        }
    }
    private void postCard(int side,int[] cards){
        boolean reflected=cards[1-side]==PcDebateRules.GUILE;
        if(cards[side]==PcDebateRules.RAGE)rageEffect(side,40,reflected);
        else if(cards[side]==PcDebateRules.CALM)calmEffect(side,-Math.max(30,speaker(1-side).anger/2),reflected);
    }
    private boolean holds(int side,int card){Speaker s=speaker(side);for(int i=0;i<s.slots;i++)if(s.hand[i]==card)return true;return false;}
    /** Original51fb60/51ee50 model effects, excluding the optional UI branch.
     * Does not claim headed counter consumption or animation RNG. */
    int resolveFuryModelOnly(boolean allowRageCounter){
        if(leader<0||leader>1)throw new IllegalStateException("Original initiative input is required");
        furySide=-1;
        for(int candidate:new int[]{leader,1-leader})if(speaker(candidate).fury<=0&&speaker(candidate).anger>=100){furySide=candidate;break;}
        int counter=-1;
        if(furySide>=0){
            int other=1-furySide;
            if(speaker(other).fury<=0){if(allowRageCounter&&holds(other,PcDebateRules.RAGE))counter=PcDebateRules.RAGE;else if(holds(other,PcDebateRules.CALM))counter=PcDebateRules.CALM;}
            if(counter==PcDebateRules.RAGE)speaker(other).anger=PcDebateRules.anger(speaker(other).anger-100);
            speaker(furySide).anger=PcDebateRules.anger(speaker(furySide).anger-(counter>=0?50:100));
            if(counter==PcDebateRules.RAGE)furySide=other;else if(counter==PcDebateRules.CALM)furySide=-1;
        }
        if(furySide>=0){
            Speaker self=speaker(furySide);self.fury=self.temper==PcDebateRules.STEADY||self.temper==PcDebateRules.BOLD?4:1;
            if(self.temper==PcDebateRules.RASH){int damage=self.war+200;damageEffect(furySide,damage,damage/15,false);}
            else if(self.temper==PcDebateRules.TIMID){burstSide=furySide;burstIndex=0;burstActive=true;}
        }
        return counter;
    }
    boolean updateStages(){
        for(int side=0;side<2;side++){int next=Math.max(0,Math.min(3,(1000-speaker(side).health)/250));if(stage[side]!=next){stage[side]=next;stageChanged[side]=true;}}
        return stageChanged[0]||stageChanged[1];
    }
    PcDebateState(int leftIntelligence,int rightIntelligence,int leftTemper,int rightTemper,int leftTalks,int rightTalks,int seed){
        // Existing bounded fixtures explicitly leave the original war cache0.
        this(leftIntelligence,rightIntelligence,leftTemper,rightTemper,leftTalks,rightTalks,seed,0,0);
    }
    PcDebateState(int leftIntelligence,int rightIntelligence,int leftTemper,int rightTemper,int leftTalks,int rightTalks,int seed,int leftWar,int rightWar){
        random=new PcMerchantRules.Random(seed);topic=random.uniform(3);
        left=new Speaker(leftIntelligence,leftWar,rightIntelligence,leftTemper,leftTalks,random);
        right=new Speaker(rightIntelligence,rightWar,leftIntelligence,rightTemper,rightTalks,random);
    }
}

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
        Speaker(int intelligence,int war,int otherIntelligence,int temper,int talkMask,PcMerchantRules.Draws random){this(intelligence,war,otherIntelligence,temper,talkMask,random,true);}
        Speaker(int intelligence,int war,int otherIntelligence,int temper,int talkMask,PcMerchantRules.Draws random,boolean initialize){
            if(intelligence<0||intelligence>100||war<0||war>100||temper<0||temper>3||talkMask<0||talkMask>31)throw new IllegalArgumentException("Unexamined native speaker input");
            this.intelligence=intelligence;this.war=war;this.temper=temper;this.talkMask=talkMask;modifier=PcDebateRules.intelligenceModifier(intelligence,otherIntelligence);slots=PcDebateRules.handSlots(intelligence);
            Arrays.fill(deck,-1);Arrays.fill(hand,-1);Arrays.fill(specialPool,-1);int count=0;for(int i=0;i<5;i++)if((talkMask&(1<<i))!=0)specialPool[count++]=10+i;specialCount=count;
            if(initialize){shuffle(random);refill(random);}
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
    /** Explicit new portable effects-only mode; null preserves prior model. */
    PcDebateUiEffects ui;
    int topic,leader=-1,selectedWinner=-1;
    final boolean[] reconsiderAvailable={true,true};
    final int[] stage={0,0};
    final boolean[] stageChanged={false,false};
    int furySide=-1,burstSide,burstIndex;
    boolean burstActive;
    Speaker speaker(int side){if(side==0)return left;if(side==1)return right;throw new IllegalArgumentException("Native side must be 0 or 1");}
    boolean legal(int side,int card){Speaker self=speaker(side),other=speaker(1-side);return PcDebateRules.legal(card,self.temper,self.fury,other.temper,other.fury,reconsiderAvailable[side]);}
    void tie(){left.anger=PcDebateRules.anger(left.anger+15);right.anger=PcDebateRules.anger(right.anger+15);if(ui!=null)ui.tie();}
    private void callback(PcDebateUiRandom.Callback kind,int side,int card,boolean reflected,int counter){if(ui!=null)ui.callback(this,kind,side,card,reflected,counter);}
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
        selectedWinner=PcDebateRules.compare(topic,leftCard,rightCard,left.temper,left.fury,right.temper,right.fury);
        winnerEffects(leftCard,rightCard);postSelectedEffects(leftCard,rightCard);endRoundEffects(leftCard,rightCard);
    }
    void winnerEffects(int leftCard,int rightCard){
        int[] cards={leftCard,rightCard};
        if(selectedWinner<0)tie();
        else{
            int winningCard=cards[selectedWinner],otherCard=cards[1-selectedWinner];Speaker actor=speaker(selectedWinner);
            if(winningCard==PcDebateRules.IGNORE){calmEffect(selectedWinner,30,false);callback(PcDebateUiRandom.Callback.CALM,selectedWinner,winningCard,false,-1);}
            else if(winningCard==PcDebateRules.GUILE){
                if(otherCard>=1&&otherCard<=9){int damage=PcDebateRules.damage(topic,otherCard,actor.temper,actor.fury,actor.modifier,random);if(damage>0){damageEffect(1-selectedWinner,damage,PcDebateRules.ordinaryAnger(otherCard),true);callback(PcDebateUiRandom.Callback.ORDINARY,1-selectedWinner,otherCard,true,-1);}}
            }else{damageEffect(selectedWinner,PcDebateRules.damage(topic,winningCard,actor.temper,actor.fury,actor.modifier,random),PcDebateRules.ordinaryAnger(winningCard),false);callback(winningCard==PcDebateRules.SHOUT?PcDebateUiRandom.Callback.SHOUT:PcDebateUiRandom.Callback.ORDINARY,selectedWinner,winningCard,false,-1);}
        }
    }
    void postSelectedEffects(int leftCard,int rightCard){
        if(selectedWinner>=0){int[] cards={leftCard,rightCard};postCard(selectedWinner,cards);if(cards[selectedWinner]!=PcDebateRules.IGNORE)postCard(1-selectedWinner,cards);}
    }
    void endRoundEffects(int leftCard,int rightCard){
        int[] cards={leftCard,rightCard};
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
        if(cards[side]==PcDebateRules.RAGE){rageEffect(side,40,reflected);callback(PcDebateUiRandom.Callback.IGNORE,side,cards[side],reflected,-1);}
        else if(cards[side]==PcDebateRules.CALM){calmEffect(side,-Math.max(30,speaker(1-side).anger/2),reflected);callback(PcDebateUiRandom.Callback.GUILE,side,cards[side],reflected,-1);}
    }
    private boolean holds(int side,int card){Speaker s=speaker(side);for(int i=0;i<s.slots;i++)if(s.hand[i]==card)return true;return false;}
    /** Original51fb60/51ee50 model effects. Default/null ui retains prior
     * model-only policy; explicit ui records its draws and counter consumption. */
    int resolveFuryModelOnly(boolean allowRageCounter){
        int counter=selectFuryModelOnly(allowRageCounter);activateFuryModelOnly();return counter;
    }
    int selectFuryModelOnly(boolean allowRageCounter){
        if(leader<0||leader>1)throw new IllegalStateException("Original initiative input is required");
        furySide=-1;
        for(int candidate:new int[]{leader,1-leader})if(speaker(candidate).fury<=0&&speaker(candidate).anger>=100){furySide=candidate;break;}
        int counter=-1;
        if(furySide>=0){
            int other=1-furySide;
            if(speaker(other).fury<=0){if(allowRageCounter&&holds(other,PcDebateRules.RAGE))counter=PcDebateRules.RAGE;else if(holds(other,PcDebateRules.CALM))counter=PcDebateRules.CALM;}
            if(counter==PcDebateRules.RAGE)speaker(other).anger=PcDebateRules.anger(speaker(other).anger-100);
            speaker(furySide).anger=PcDebateRules.anger(speaker(furySide).anger-(counter>=0?50:100));
            callback(PcDebateUiRandom.Callback.COUNTER,furySide,-1,false,counter);
            if(counter==PcDebateRules.RAGE)furySide=other;else if(counter==PcDebateRules.CALM)furySide=-1;
        }
        return counter;
    }
    void activateFuryModelOnly(){
        if(furySide>=0){
            Speaker self=speaker(furySide);self.fury=self.temper==PcDebateRules.STEADY||self.temper==PcDebateRules.BOLD?4:1;
            if(self.temper==PcDebateRules.RASH){int damage=self.war+200;damageEffect(furySide,damage,damage/15,false);callback(PcDebateUiRandom.Callback.RASH,furySide,-1,false,-1);}
            else if(self.temper==PcDebateRules.TIMID){burstSide=furySide;burstIndex=0;burstActive=true;}
        }
    }
    boolean updateStages(){
        for(int side=0;side<2;side++){int next=Math.max(0,Math.min(3,(1000-speaker(side).health)/250));if(stage[side]!=next){stage[side]=next;stageChanged[side]=true;}}
        boolean changed=stageChanged[0]||stageChanged[1];if(changed)callback(PcDebateUiRandom.Callback.STAGES,0,-1,false,-1);return changed;
    }
    /** Original51d800/51ec50; full queue callbacks remain separate. */
    void reconsider(int side){
        Speaker speaker=speaker(side);Arrays.fill(speaker.hand,0,speaker.slots,-1);speaker.refill(random);
        if(speaker.fury>0&&speaker.temper==PcDebateRules.STEADY&&random.percent(40))speaker.hand[1]=(topic+1)*3;
        Arrays.sort(speaker.hand,0,speaker.slots);callback(PcDebateUiRandom.Callback.RETHINK,side,0,false,-1);reconsiderAvailable[side]=false;
    }
    /** Original51ef30 model-only timid sequence, consuming one ordinary card. */
    void timidBurstStep(){
        Speaker self=speaker(burstSide);int slot=-1;
        for(int i=0;i<self.slots;i++)if(self.hand[i]>=1&&self.hand[i]<=9){slot=i;break;}
        if(slot<0){callback(PcDebateUiRandom.Callback.BURST_END,burstSide,-1,false,-1);burstActive=false;return;}
        int card=self.hand[slot],damage=PcDebateRules.damage(topic,card,self.temper,self.fury,self.modifier,random);
        damageEffect(burstSide,damage,damage/15,false);callback(PcDebateUiRandom.Callback.BURST_STRIKE,burstSide,card,false,-1);self.select(slot);burstIndex++;
    }
    /** Original51f060 model-side terminal choice. Simultaneous zero health
     * checks the initiative side first; it does not invent a draw. */
    int terminalWinner=-1;
    boolean checkTerminal(){
        if(terminalWinner<0){if(leader<0||leader>1)throw new IllegalStateException("Original initiative input required");
            for(int side:new int[]{leader,1-leader})if(speaker(side).health<=0){terminalWinner=1-side;break;}}
        if(terminalWinner>=0)callback(PcDebateUiRandom.Callback.STAGES,0,-1,false,-1);return terminalWinner>=0;
    }
    PcDebateState(int leftIntelligence,int rightIntelligence,int leftTemper,int rightTemper,int leftTalks,int rightTalks,int seed){
        // Existing bounded fixtures explicitly leave the original war cache0.
        this(leftIntelligence,rightIntelligence,leftTemper,rightTemper,leftTalks,rightTalks,seed,0,0);
    }
    PcDebateState(int leftIntelligence,int rightIntelligence,int leftTemper,int rightTemper,int leftTalks,int rightTalks,int seed,int leftWar,int rightWar){
        this(leftIntelligence,rightIntelligence,leftTemper,rightTemper,leftTalks,rightTalks,seed,leftWar,rightWar,true);
    }
    PcDebateState(int leftIntelligence,int rightIntelligence,int leftTemper,int rightTemper,int leftTalks,int rightTalks,int seed,int leftWar,int rightWar,boolean initialize){
        random=new PcMerchantRules.Random(seed);if(initialize)topic=random.uniform(3);
        left=new Speaker(leftIntelligence,leftWar,rightIntelligence,leftTemper,leftTalks,random,initialize);
        right=new Speaker(rightIntelligence,rightWar,leftIntelligence,rightTemper,rightTalks,random,initialize);
    }
}

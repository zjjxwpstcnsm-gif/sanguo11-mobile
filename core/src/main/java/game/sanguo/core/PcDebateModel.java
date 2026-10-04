package game.sanguo.core;

/** Original51e300/51f1c0 derived model protocol, optional UI branch disabled.
 * Not a production campaign policy: headed callbacks and settlement are pending. */
final class PcDebateModel {
    final PcDebateState state;
    int phase,previousPhase=-1,sub,mercy=-1,outcome;
    final int[] selected={-1,-1};
    final boolean[] human={false,false};
    /** Original51f0e0 external manager/option28 gate must be resolved by caller.
     * The isolated original flow fixture has no external terminal gate. */
    boolean externalTerminalChoiceSuppressed;
    /** Original5166f0: preference0/1 from original relationship tests; -1 random.
     * Source relationship binding is required before production activation. */
    int terminalChoicePreference=-1;
    PcDebateModel(PcDebateState state,int initialLeader){
        if(initialLeader<0||initialLeader>1)throw new IllegalArgumentException("Original input initiative required");
        this.state=state;state.leader=initialLeader;state.selectedWinner=0;state.furySide=0;
    }
    private void selectAi(int side){selected[side]=state.speaker(side).select(new PcDebateAi(state,side).choose(selected[1-side]));}
    boolean selectHuman(int side,int slot){
        if(side<0||side>1||!human[side]||phase!=4||selected[side]>=0)return false;
        int current=sub==0?state.leader:sub==5?1-state.leader:-1;
        if(current!=side||slot<0||slot>=state.speaker(side).slots||!state.legal(side,state.speaker(side).hand[slot]))return false;
        selected[side]=state.speaker(side).select(slot);return true;
    }
    private void setupRound(){
        for(int side=0;side<2;side++){
            PcDebateState.Speaker speaker=state.speaker(side);selected[side]=-1;
            if(state.stageChanged[side]||speaker.fury>0&&speaker.temper==PcDebateRules.STEADY)state.reconsiderAvailable[side]=true;
            speaker.refill(state.random);
            if(speaker.fury>0&&speaker.temper==PcDebateRules.BOLD){boolean ordinary=false;for(int i=0;i<speaker.slots;i++)if(speaker.hand[i]>=1&&speaker.hand[i]<=9)ordinary=true;if(!ordinary)speaker.fury=0;}
            state.stageChanged[side]=false;
        }
    }
    private boolean instantVictory(){
        if(state.terminalWinner<0)for(int side=0;side<2;side++){
            PcDebateState.Speaker self=state.speaker(side),other=state.speaker(1-side);
            if(self.intelligence>80&&self.intelligence>other.intelligence+10&&self.modifier+state.random.uniform(200)>=270){state.terminalWinner=side;break;}
        }
        if(state.terminalWinner>=0){state.speaker(1-state.terminalWinner).health=-100;outcome=3;return true;}return false;
    }
    private void afterTerminalCheck(int nextPhase){phase=state.checkTerminal()?(!externalTerminalChoiceSuppressed&&state.speaker(1-state.terminalWinner).health<=-100?8:9):nextPhase;}
    void frame(){
        if(phase!=previousPhase){sub=0;previousPhase=phase;if(phase==5)state.selectedWinner=PcDebateRules.compare(state.topic,selected[0],selected[1],state.left.temper,state.left.fury,state.right.temper,state.right.fury);}
        switch(phase){
            case 0:if(sub<3)sub++;else phase=1;break;
            case 1:if(sub==0){if(!instantVictory())phase=2;sub++;}else if(sub==1)phase=9;break;
            case 2:if(sub==0)sub++;else if(sub==1)phase=3;break;
            case 3:setupRound();phase=4;break;
            case 4:selectionFrame();break;
            case 5:
                if(sub==0)sub++;
                else if(sub==1){state.winnerEffects(selected[0],selected[1]);sub++;}
                else if(sub==2){state.postSelectedEffects(selected[0],selected[1]);sub++;}
                else if(sub==3)afterTerminalCheck(6);break;
            case 6:furyFrame();break;
            case 7:if(sub==0){state.endRoundEffects(selected[0],selected[1]);sub++;}else if(sub==1)phase=3;break;
            case 8:
                if(sub==0){mercy=terminalChoicePreference<0?state.random.uniform(2):terminalChoicePreference;sub++;}
                else if(sub==1){outcome=mercy==0?1:2;sub++;}
                else if(sub==2)phase=9;break;
            case 9:break;
            default:throw new IllegalStateException("Unknown original debate phase");
        }
    }
    private void selectionFrame(){
        int first=state.leader,second=1-first;
        switch(sub){
            case 0:if(selected[first]<0){if(human[first])return;selectAi(first);}sub++;break;
            case 1:sub++;break;
            case 2:if(selected[first]==0){state.reconsider(first);sub++;}else sub=4;break;
            case 3:selected[first]=-1;sub=0;break;
            case 4:sub++;break;
            case 5:if(selected[second]<0){if(human[second])return;selectAi(second);}sub++;break;
            case 6:sub++;break;
            case 7:if(selected[second]==0){state.reconsider(second);sub++;}else sub=9;break;
            case 8:selected[second]=-1;sub=4;break;
            case 9:phase=5;break;
            default:throw new IllegalStateException("Unknown original selection step");
        }
    }
    private void furyFrame(){
        switch(sub){
            case 0:state.selectFuryModelOnly(true);sub++;break;
            case 1:state.activateFuryModelOnly();sub++;break;
            case 2:if(state.burstActive)state.timidBurstStep();else sub=3;break;
            case 3:if(state.checkTerminal())afterTerminalCheck(7);else sub++;break;
            case 4:state.selectFuryModelOnly(false);sub++;break;
            case 5:state.activateFuryModelOnly();sub++;break;
            case 6:if(state.burstActive)state.timidBurstStep();else sub=7;break;
            case 7:state.updateStages();sub++;break;
            case 8:afterTerminalCheck(7);break;
            default:throw new IllegalStateException("Unknown original fury step");
        }
    }
}

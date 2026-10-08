package game.sanguo.api;

import java.util.Objects;

/** Progress only the current contest; no rule callback or mutable world crosses this boundary. */
public final class ContestCommand {
    public enum Operation { DUEL_MOVE, DEBATE_CARD, RETHINK, FINISH_DEBATE, CONCEDE, ADOPT_NATIVE_SETTLEMENT, SEARCH_CHOICE, FINISH_NATIVE_DUEL, NATIVE_DUEL_INPUT, NATIVE_DUEL_DISPOSITION, START_NATIVE_DUEL, NATIVE_DUEL_HEIR, ADOPT_NATIVE_LOYALTY_INPUT, ADOPT_NATIVE_AI_ACTOR, ADOPT_NATIVE_HUMAN_ACTOR, ADOPT_NATIVE_RECRUIT_ITEM_RECIPIENT, ADOPT_NATIVE_PHYSICAL_RECOVERY }
    public final Operation operation;
    public final StateToken expected;
    public final int contestId,contestRevision,choice;
    public final String stance,move;
    public final boolean mercy;
    public final int nativeStance,nativeSpecial,nativeReplacement;
    private ContestCommand(Operation op,StateToken expected,int id,int revision,int choice,String stance,String move,boolean mercy){
        this(op,expected,id,revision,choice,stance,move,mercy,-1,-1,-1);
    }
    private ContestCommand(Operation op,StateToken expected,int id,int revision,int choice,String stance,String move,boolean mercy,int nativeStance,int nativeSpecial,int nativeReplacement){
        this.nativeStance=nativeStance;this.nativeSpecial=nativeSpecial;this.nativeReplacement=nativeReplacement;
        operation=Objects.requireNonNull(op);this.expected=Objects.requireNonNull(expected);contestId=id;contestRevision=revision;
        this.choice=choice;this.stance=stance;this.move=move;this.mercy=mercy;
    }
    public static ContestCommand duel(StateToken state,int id,int revision,String stance,String move,int replacement){return new ContestCommand(Operation.DUEL_MOVE,state,id,revision,replacement,stance,move,false);}
    public static ContestCommand card(StateToken state,int id,int revision,int index){return new ContestCommand(Operation.DEBATE_CARD,state,id,revision,index,null,null,false);}
    public static ContestCommand rethink(StateToken state,int id,int revision){return new ContestCommand(Operation.RETHINK,state,id,revision,-1,null,null,false);}
    public static ContestCommand finishDebate(StateToken state,int id,int revision,boolean mercy){return new ContestCommand(Operation.FINISH_DEBATE,state,id,revision,-1,null,null,mercy);}
    public static ContestCommand adoptNativeSettlement(StateToken state,int id,int revision){return new ContestCommand(Operation.ADOPT_NATIVE_SETTLEMENT,state,id,revision,-1,null,null,false);}
    public static ContestCommand concede(StateToken state,int id,int revision){return new ContestCommand(Operation.CONCEDE,state,id,revision,-1,null,null,false);}
    public static ContestCommand finishNativeDuel(StateToken state,int id,int revision){return new ContestCommand(Operation.FINISH_NATIVE_DUEL,state,id,revision,-1,null,null,false);}
    public static ContestCommand nativeDuelInput(StateToken state,int id,int revision,int stance,int special,int replacement){return new ContestCommand(Operation.NATIVE_DUEL_INPUT,state,id,revision,-1,null,null,false,stance,special,replacement);}
    public static ContestCommand nativeDuelDisposition(StateToken state,int id,int revision,int officer,int action){return new ContestCommand(Operation.NATIVE_DUEL_DISPOSITION,state,id,revision,officer,null,null,false,action,-1,-1);}
    public static ContestCommand startNativeDuel(StateToken state,int actorUnit,int targetUnit,int nominee){return new ContestCommand(Operation.START_NATIVE_DUEL,state,actorUnit,targetUnit,nominee,null,null,false);}
    public static ContestCommand nativeDuelHeir(StateToken state,int id,int revision,int heir){return new ContestCommand(Operation.NATIVE_DUEL_HEIR,state,id,revision,heir,null,null,false);}
    public static ContestCommand adoptNativeLoyaltyInput(StateToken state,int id,int revision){return new ContestCommand(Operation.ADOPT_NATIVE_LOYALTY_INPUT,state,id,revision,-1,null,null,false);}
    public static ContestCommand adoptNativeAiActor(StateToken state,int id,int revision){return new ContestCommand(Operation.ADOPT_NATIVE_AI_ACTOR,state,id,revision,-1,null,null,false);}
    public static ContestCommand adoptNativePhysicalRecovery(StateToken state,int id,int revision){return new ContestCommand(Operation.ADOPT_NATIVE_PHYSICAL_RECOVERY,state,id,revision,-1,null,null,false);}
    public static ContestCommand adoptNativeRecruitItemRecipient(StateToken state,int id,int revision){return new ContestCommand(Operation.ADOPT_NATIVE_RECRUIT_ITEM_RECIPIENT,state,id,revision,-1,null,null,false);}
    public static ContestCommand adoptNativeHumanActor(StateToken state,int id,int revision){return new ContestCommand(Operation.ADOPT_NATIVE_HUMAN_ACTOR,state,id,revision,-1,null,null,false);}
    public static ContestCommand searchChoice(StateToken state,int id,int revision,boolean yes){return new ContestCommand(Operation.SEARCH_CHOICE,state,id,revision,-1,null,null,yes);}
}

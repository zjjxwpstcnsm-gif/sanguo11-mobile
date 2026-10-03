package game.sanguo.api;

import java.util.Objects;

/** Progress only the current contest; no rule callback or mutable world crosses this boundary. */
public final class ContestCommand {
    public enum Operation { DUEL_MOVE, DEBATE_CARD, RETHINK, FINISH_DEBATE, CONCEDE }
    public final Operation operation;
    public final StateToken expected;
    public final int contestId,contestRevision,choice;
    public final String stance,move;
    public final boolean mercy;
    private ContestCommand(Operation op,StateToken expected,int id,int revision,int choice,String stance,String move,boolean mercy){
        operation=Objects.requireNonNull(op);this.expected=Objects.requireNonNull(expected);contestId=id;contestRevision=revision;
        this.choice=choice;this.stance=stance;this.move=move;this.mercy=mercy;
    }
    public static ContestCommand duel(StateToken state,int id,int revision,String stance,String move,int replacement){return new ContestCommand(Operation.DUEL_MOVE,state,id,revision,replacement,stance,move,false);}
    public static ContestCommand card(StateToken state,int id,int revision,int index){return new ContestCommand(Operation.DEBATE_CARD,state,id,revision,index,null,null,false);}
    public static ContestCommand rethink(StateToken state,int id,int revision){return new ContestCommand(Operation.RETHINK,state,id,revision,-1,null,null,false);}
    public static ContestCommand finishDebate(StateToken state,int id,int revision,boolean mercy){return new ContestCommand(Operation.FINISH_DEBATE,state,id,revision,-1,null,null,mercy);}
    public static ContestCommand concede(StateToken state,int id,int revision){return new ContestCommand(Operation.CONCEDE,state,id,revision,-1,null,null,false);}
}

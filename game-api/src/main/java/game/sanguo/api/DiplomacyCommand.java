package game.sanguo.api;

/** Stable operation codes: GOODWILL, CEASEFIRE, ALLIANCE, BREAK_TREATY. */
public final class DiplomacyCommand {
    public final StateToken expected;
    public final int cityId,officerId,targetSide,turns;
    public final String operation;
    public DiplomacyCommand(StateToken expected,int cityId,int officerId,int targetSide,String operation,int turns){
        this.expected=expected;this.cityId=cityId;this.officerId=officerId;this.targetSide=targetSide;this.operation=operation;this.turns=turns;
    }
}

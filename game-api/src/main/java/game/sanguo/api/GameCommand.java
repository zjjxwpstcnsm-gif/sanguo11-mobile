package game.sanguo.api;

import java.util.Objects;

/** Only commands with real production callers belong in this contract. */
public final class GameCommand {
    public enum Operation { RECRUIT, PATROL }
    public final Operation operation;
    public final StateToken expected;
    public final int cityId, officerId;
    public GameCommand(Operation operation,StateToken expected,int cityId,int officerId){
        this.operation=Objects.requireNonNull(operation);this.expected=Objects.requireNonNull(expected);
        this.cityId=cityId;this.officerId=officerId;
    }
}

package game.sanguo.api;

public final class CommandResult {
    public enum Error { NONE, STALE_SESSION, STALE_REVISION, HOST_BUSY, RULE_REJECTED, HOST_ERROR, CLOSED }
    public final Error error;
    public final String detail;
    public final StateToken state;
    public final GameEvent event;
    public CommandResult(Error error,String detail,StateToken state,GameEvent event){
        this.error=error;this.detail=detail;this.state=state;this.event=event;
    }
    public boolean ok(){return error==Error.NONE;}
}

package game.sanguo.api;

public final class CommandResult {
    public enum Error { NONE, STALE_SESSION, STALE_REVISION, HOST_BUSY, RULE_REJECTED, HOST_ERROR, CLOSED }
    public final Error error;
    public final String detail;
    public final String reasonCode,field;
    public final StateToken state;
    public final GameEvent event;
    public CommandResult(Error error,String detail,StateToken state,GameEvent event){
        this(error,detail,state,event,error.name(),"global");
    }
    public CommandResult(Error error,String detail,StateToken state,GameEvent event,String reasonCode,String field){
        this.error=error;this.detail=detail;this.state=state;this.event=event;this.reasonCode=reasonCode;this.field=field;
    }
    public boolean ok(){return error==Error.NONE;}
}

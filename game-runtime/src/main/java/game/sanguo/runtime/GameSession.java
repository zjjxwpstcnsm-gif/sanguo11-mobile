package game.sanguo.runtime;

import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.query.SnapshotQuery;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Single authority. Every write runs on the construction thread; no Activity/View is retained.
 * Rule operations run on disposable candidates. Validation and copying precede installation,
 * so failures (including exceptions) never partially modify the authoritative world. */
public final class GameSession implements GameApi, AutoCloseable {
    private final Thread logicThread=Thread.currentThread();
    private World authority;
    private String id=UUID.randomUUID().toString();
    private long generation,revision;
    private boolean closed,publishing;
    private TurnTicket turn;
    private final Map<World,StateToken> views=new WeakHashMap<>();
    private final List<Consumer<GameEvent>> listeners=new ArrayList<>();
    private final Consumer<RuntimeException> observerFailure;
    public GameSession(World initial)throws IOException{this(initial,e->System.err.println("Session observer failed: "+e));}
    public GameSession(World initial,Consumer<RuntimeException> observerFailure)throws IOException{
        this.observerFailure=Objects.requireNonNull(observerFailure);authority=WorldCopies.copy(Objects.requireNonNull(initial));
    }
    private void thread(){if(Thread.currentThread()!=logicThread)throw new IllegalStateException("Serial logic thread required");}
    private void write(){thread();if(publishing)throw new IllegalStateException("Commands cannot reenter commit notifications");}
    @Override public StateToken state(){thread();return new StateToken(id,generation,revision);}
    @Override public boolean busy(){thread();return closed||turn!=null||authority.commandsBlocked();}
    @Override public Subscription subscribe(Consumer<GameEvent> listener){
        thread();if(closed)throw new IllegalStateException("Session closed");listeners.add(Objects.requireNonNull(listener));
        return ()->{thread();listeners.remove(listener);};
    }
    private GameEvent emit(GameEvent.Kind kind,int city,int officer,int troops,int order,String detail){
        GameEvent event=new GameEvent(kind,state(),city,officer,troops,order,detail);
        publishing=true;
        try{for(Consumer<GameEvent> listener:new ArrayList<>(listeners)){
            if(!listeners.contains(listener))continue;
            try{listener.accept(event);}catch(RuntimeException failure){
                try{observerFailure.accept(failure);}catch(RuntimeException ignored){System.err.println("Observer error handler failed");}
            }
        }}finally{publishing=false;}
        return event;
    }
    private CommandResult.Error invalid(StateToken expected,boolean allowTurn){
        if(closed)return CommandResult.Error.CLOSED;
        if(expected==null||!id.equals(expected.sessionId)||generation!=expected.generation)return CommandResult.Error.STALE_SESSION;
        if(expected.revision!=revision)return CommandResult.Error.STALE_REVISION;
        if(!allowTurn&&(turn!=null))return CommandResult.Error.HOST_BUSY;
        return CommandResult.Error.NONE;
    }
    private CommandResult failed(CommandResult.Error error,String detail){return new CommandResult(error,detail,state(),null);}
    @Override public CommandResult execute(GameCommand command){
        write();Objects.requireNonNull(command);
        CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return failed(error,error.name());
        if(authority.commandsBlocked())return failed(CommandResult.Error.HOST_BUSY,"HOST_BUSY");
        try{
            World candidate=WorldCopies.copy(authority);World.City before=candidate.city(command.cityId);
            int troops=before==null?0:before.troops,order=before==null?0:before.order;
            World.Result result=command.operation==GameCommand.Operation.RECRUIT?
                candidate.recruit(command.cityId,command.officerId):candidate.patrol(command.cityId,command.officerId);
            if(!result.ok)return failed(CommandResult.Error.RULE_REJECTED,result.message);
            World installed=WorldCopies.copy(candidate); // may throw; nothing committed yet
            long next=Math.addExact(revision,1);
            authority=installed;revision=next;views.clear();
            World.City after=installed.city(command.cityId);
            GameEvent event=emit(command.operation==GameCommand.Operation.RECRUIT?GameEvent.Kind.RECRUITED:GameEvent.Kind.PATROLLED,
                command.cityId,command.officerId,after==null?0:after.troops-troops,after==null?0:after.order-order,result.message);
            return new CommandResult(CommandResult.Error.NONE,result.message,state(),event);
        }catch(IOException|RuntimeException failure){return failed(CommandResult.Error.HOST_ERROR,"HOST_ERROR");}
    }
    /** Only the enumerated legacy adapters may obtain a detached draft. */
    public LegacyView legacyView(){
        thread();if(closed)throw new IllegalStateException("Session closed");
        try{World draft=WorldCopies.copy(authority);StateToken token=state();views.put(draft,token);return new LegacyView(token,draft);}
        catch(IOException e){throw new IllegalStateException("Cannot capture valid world",e);}
    }
    /** Compatibility transaction. The supplier is evaluated ONLY after stale/busy checks.
     * On any outcome its view is consumed; the caller must bind a fresh view. */
    public World.Result legacy(World draft,Supplier<World.Result> operation){
        write();StateToken expected=views.remove(draft);CommandResult.Error error=invalid(expected,false);
        if(error!=CommandResult.Error.NONE)return World.Result.rejected(error.name());
        if(authority.commandsBlocked())return World.Result.rejected("HOST_BUSY");
        try{
            World.Result result=Objects.requireNonNull(operation.get());
            if(!result.ok)return result;
            World installed=WorldCopies.copy(draft);long next=Math.addExact(revision,1);
            authority=installed;revision=next;views.clear();
            emit(GameEvent.Kind.LEGACY_COMMITTED,-1,-1,0,0,result.message);return result;
        }catch(IOException|RuntimeException failure){return World.Result.rejected("HOST_ERROR");}
    }
    /** Assembly/restore boundary. Do not use to submit individual UI commands. */
    public void replace(World prepared)throws IOException{
        write();if(closed)throw new IllegalStateException("Session closed");
        World installed=WorldCopies.copy(Objects.requireNonNull(prepared));long next=Math.addExact(generation,1);
        authority=installed;generation=next;id=UUID.randomUUID().toString();revision=0;turn=null;views.clear();
        emit(GameEvent.Kind.WORLD_REPLACED,-1,-1,0,0,"World replaced");
    }
    public byte[] captureSave()throws IOException{thread();if(closed)throw new IOException("Session closed");return SaveCodec.encode(authority);}
    @Override public GameSnapshot snapshot(){
        thread();if(closed)throw new IllegalStateException("Session closed");
        try{return SnapshotQuery.capture(WorldCopies.copy(authority),state());}
        catch(IOException e){throw new IllegalStateException("Snapshot capture",e);}
    }
    public TurnTicket beginTurn()throws IOException{
        write();if(busy())throw new IllegalStateException("HOST_BUSY");
        turn=new TurnTicket(state(),captureSave());return turn;
    }
    public boolean commitTurn(TurnTicket ticket,World computed)throws IOException{
        write();if(ticket==null||ticket!=turn||invalid(ticket.expected,true)!=CommandResult.Error.NONE)return false;
        World installed=WorldCopies.copy(computed);long next=Math.addExact(revision,1);
        authority=installed;revision=next;turn=null;views.clear();emit(GameEvent.Kind.TURN_COMMITTED,-1,-1,0,0,"Turn committed");return true;
    }
    public void cancelTurn(TurnTicket ticket){write();if(ticket==turn)turn=null;}
    @Override public void close(){
        write();if(closed)return;closed=true;turn=null;views.clear();emit(GameEvent.Kind.CLOSED,-1,-1,0,0,"Session closed");listeners.clear();authority=null;
    }
}

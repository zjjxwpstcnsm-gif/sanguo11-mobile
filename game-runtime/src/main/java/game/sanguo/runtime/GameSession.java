package game.sanguo.runtime;

import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.query.SnapshotQuery;
import game.sanguo.runtime.query.DeploymentQuery;
import game.sanguo.runtime.query.DiplomacyQuery;
import game.sanguo.runtime.query.ConstructionQuery;
import game.sanguo.runtime.query.TransportQuery;
import game.sanguo.runtime.query.CityActionQuery;
import game.sanguo.runtime.query.TradeQuery;
import game.sanguo.runtime.query.ProductionQuery;
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
    private World ruleQueryWorld;
    private StateToken ruleQueryState;
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
    private void invalidateQueries(){views.clear();ruleQueryWorld=null;ruleQueryState=null;}
    private World ruleQueryWorld()throws IOException{
        StateToken current=state();
        if(ruleQueryWorld==null||!current.equals(ruleQueryState)){
            World prepared=WorldCopies.copy(authority);ruleQueryWorld=prepared;ruleQueryState=current;
        }
        return ruleQueryWorld;
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
            authority=installed;revision=next;invalidateQueries();
            World.City after=installed.city(command.cityId);
            GameEvent event=emit(command.operation==GameCommand.Operation.RECRUIT?GameEvent.Kind.RECRUITED:GameEvent.Kind.PATROLLED,
                command.cityId,command.officerId,after==null?0:after.troops-troops,after==null?0:after.order-order,result.message);
            return new CommandResult(CommandResult.Error.NONE,result.message,state(),event);
        }catch(IOException|RuntimeException failure){return failed(CommandResult.Error.HOST_ERROR,"HOST_ERROR");}
    }
    @Override public DeploymentPreview preview(DeploymentCommand command){
        thread();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return DeploymentPreview.unavailable(state(),error);
        if(authority.commandsBlocked())return DeploymentPreview.unavailable(state(),CommandResult.Error.HOST_BUSY);
        try{return DeploymentQuery.capture(ruleQueryWorld(),state(),command);}
        catch(IOException|RuntimeException failure){return DeploymentPreview.unavailable(state(),CommandResult.Error.HOST_ERROR);}
    }
    @Override public CommandResult execute(DeploymentCommand command){
        write();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return failed(error,error.name());
        if(authority.commandsBlocked())return failed(CommandResult.Error.HOST_BUSY,"HOST_BUSY");
        try{
            World candidate=WorldCopies.copy(authority);DeploymentPreview checked=DeploymentQuery.capture(candidate,state(),command);
            if(!checked.allowed())return new CommandResult(checked.error,checked.detail,state(),null,checked.reasonCode,checked.field);
            World.Result result=candidate.army.deploy(command.cityId,command.leaderId,command.deputies(),DeploymentQuery.weapon(command.weapon),DeploymentQuery.ship(command.ship),command.troops,command.food,command.gold);
            if(!result.ok)return failed(CommandResult.Error.HOST_ERROR,"DEPLOYMENT_VALIDATION_DIVERGED");
            World installed=WorldCopies.copy(candidate);long next=Math.addExact(revision,1);
            authority=installed;revision=next;invalidateQueries();
            GameEvent event=emit(GameEvent.Kind.DEPLOYED,command.cityId,command.leaderId,-command.troops,0,result.message);
            return new CommandResult(CommandResult.Error.NONE,result.message,state(),event);
        }catch(IOException|RuntimeException failure){return failed(CommandResult.Error.HOST_ERROR,"HOST_ERROR");}
    }
    @Override public DiplomacyPreview preview(DiplomacyCommand command){
        thread();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return DiplomacyPreview.unavailable(state(),error);
        if(authority.commandsBlocked())return DiplomacyPreview.unavailable(state(),CommandResult.Error.HOST_BUSY);
        try{return DiplomacyQuery.capture(ruleQueryWorld(),state(),command);}
        catch(IOException|RuntimeException failure){return DiplomacyPreview.unavailable(state(),CommandResult.Error.HOST_ERROR);}
    }
    @Override public CommandResult execute(DiplomacyCommand command){
        write();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return failed(error,error.name());
        if(authority.commandsBlocked())return failed(CommandResult.Error.HOST_BUSY,"HOST_BUSY");
        try{
            World candidate=WorldCopies.copy(authority);DiplomacyPreview checked=DiplomacyQuery.capture(candidate,state(),command);
            if(!checked.allowed())return new CommandResult(checked.error,checked.detail,state(),null,checked.reasonCode,checked.field);
            DiplomacyPlan.Operation operation=DiplomacyQuery.operation(command.operation);
            World.Result result=candidate.campaign.diplomaticAction(command.cityId,command.officerId,command.targetSide,operation,command.turns);
            if(!result.ok)return failed(CommandResult.Error.HOST_ERROR,"DIPLOMACY_VALIDATION_DIVERGED");
            World installed=WorldCopies.copy(candidate);long next=Math.addExact(revision,1);
            authority=installed;revision=next;invalidateQueries();
            GameEvent event=emit(operation==DiplomacyPlan.Operation.BREAK_TREATY?GameEvent.Kind.TREATY_BROKEN:GameEvent.Kind.DIPLOMACY_DISPATCHED,
                command.cityId,command.officerId,0,0,result.message);
            return new CommandResult(CommandResult.Error.NONE,result.message,state(),event);
        }catch(IOException|RuntimeException failure){return failed(CommandResult.Error.HOST_ERROR,"HOST_ERROR");}
    }
    @Override public ConstructionPreview preview(ConstructionCommand command){
        thread();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return ConstructionPreview.unavailable(state(),error);
        if(authority.commandsBlocked())return ConstructionPreview.unavailable(state(),CommandResult.Error.HOST_BUSY);
        try{return ConstructionQuery.capture(ruleQueryWorld(),state(),command);}
        catch(IOException|RuntimeException failure){return ConstructionPreview.unavailable(state(),CommandResult.Error.HOST_ERROR);}
    }
    @Override public CommandResult execute(ConstructionCommand command){
        write();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return failed(error,error.name());
        if(authority.commandsBlocked())return failed(CommandResult.Error.HOST_BUSY,"HOST_BUSY");
        try{
            World candidate=WorldCopies.copy(authority);ConstructionPreview checked=ConstructionQuery.capture(candidate,state(),command);
            if(!checked.allowed())return new CommandResult(checked.error,checked.detail,state(),null,checked.reasonCode,checked.field);
            World.Result result=candidate.domestic.build(command.cityId,command.officerId,ConstructionQuery.kind(command.kind),new Hex(command.q,command.r));
            if(!result.ok)return failed(CommandResult.Error.HOST_ERROR,"CONSTRUCTION_VALIDATION_DIVERGED");
            World installed=WorldCopies.copy(candidate);long next=Math.addExact(revision,1);
            authority=installed;revision=next;invalidateQueries();
            GameEvent event=emit(GameEvent.Kind.CONSTRUCTION_STARTED,command.cityId,command.officerId,0,0,result.message);
            return new CommandResult(CommandResult.Error.NONE,result.message,state(),event);
        }catch(IOException|RuntimeException failure){return failed(CommandResult.Error.HOST_ERROR,"HOST_ERROR");}
    }
    @Override public CityActionPreview preview(CityActionCommand command){
        thread();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return CityActionPreview.unavailable(state(),error);
        if(authority.commandsBlocked())return CityActionPreview.unavailable(state(),CommandResult.Error.HOST_BUSY);
        try{return CityActionQuery.capture(ruleQueryWorld(),state(),command);}
        catch(IOException|RuntimeException failure){return CityActionPreview.unavailable(state(),CommandResult.Error.HOST_ERROR);}
    }
    @Override public CommandResult execute(CityActionCommand command){
        write();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return failed(error,error.name());
        if(authority.commandsBlocked())return failed(CommandResult.Error.HOST_BUSY,"HOST_BUSY");
        try{
            World candidate=WorldCopies.copy(authority);CityActionPreview checked=CityActionQuery.capture(candidate,state(),command);
            if(!checked.allowed())return new CommandResult(checked.error,checked.detail,state(),null,checked.reasonCode,checked.field);
            World.Result result=candidate.strategy.executeCityAction(CityActionQuery.operation(command.operation),command.cityId,command.officerId,command.targets());
            if(!result.ok)return failed(CommandResult.Error.HOST_ERROR,"CITY_ACTION_VALIDATION_DIVERGED");
            World installed=WorldCopies.copy(candidate);long next=Math.addExact(revision,1);
            authority=installed;revision=next;invalidateQueries();
            GameEvent event=emit(GameEvent.Kind.CITY_ACTION_COMMITTED,command.cityId,command.officerId,checked.effects.troopsAfter-checked.effects.troopsBefore,checked.effects.orderAfter-checked.effects.orderBefore,result.message);
            return new CommandResult(CommandResult.Error.NONE,result.message,state(),event);
        }catch(IOException|RuntimeException failure){return failed(CommandResult.Error.HOST_ERROR,"HOST_ERROR");}
    }
    @Override public ProductionPreview preview(ProductionCommand command){
        thread();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return ProductionPreview.unavailable(state(),error);
        if(authority.commandsBlocked())return ProductionPreview.unavailable(state(),CommandResult.Error.HOST_BUSY);
        try{return ProductionQuery.capture(ruleQueryWorld(),state(),command);}
        catch(IOException|RuntimeException failure){return ProductionPreview.unavailable(state(),CommandResult.Error.HOST_ERROR);}
    }
    @Override public CommandResult execute(ProductionCommand command){
        write();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return failed(error,error.name());
        if(authority.commandsBlocked())return failed(CommandResult.Error.HOST_BUSY,"HOST_BUSY");
        try{
            World candidate=WorldCopies.copy(authority);ProductionPreview checked=ProductionQuery.capture(candidate,state(),command);
            if(!checked.allowed())return new CommandResult(checked.error,checked.detail,state(),null,checked.reasonCode,checked.field);
            World.Result result=ProductionQuery.operation(command.operation)==ProductionPlan.Operation.SHIP?candidate.army.produce(command.cityId,command.officers(),null,ProductionQuery.ship(command)):candidate.produce(command.cityId,command.officers(),ProductionQuery.weapon(command));
            if(!result.ok)return failed(CommandResult.Error.HOST_ERROR,"PRODUCTION_VALIDATION_DIVERGED");
            World installed=WorldCopies.copy(candidate);long next=Math.addExact(revision,1);
            authority=installed;revision=next;invalidateQueries();
            GameEvent event=emit(GameEvent.Kind.PRODUCTION_COMMITTED,command.cityId,command.officerId,0,0,result.message);
            return new CommandResult(CommandResult.Error.NONE,result.message,state(),event);
        }catch(IOException|RuntimeException failure){return failed(CommandResult.Error.HOST_ERROR,"HOST_ERROR");}
    }
    @Override public TradePreview preview(TradeCommand command){
        thread();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return TradePreview.unavailable(state(),error);
        if(authority.commandsBlocked())return TradePreview.unavailable(state(),CommandResult.Error.HOST_BUSY);
        try{return TradeQuery.capture(ruleQueryWorld(),state(),command);}
        catch(IOException|RuntimeException failure){return TradePreview.unavailable(state(),CommandResult.Error.HOST_ERROR);}
    }
    @Override public CommandResult execute(TradeCommand command){
        write();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return failed(error,error.name());
        if(authority.commandsBlocked())return failed(CommandResult.Error.HOST_BUSY,"HOST_BUSY");
        try{
            World candidate=WorldCopies.copy(authority);TradePreview checked=TradeQuery.capture(candidate,state(),command);
            if(!checked.allowed())return new CommandResult(checked.error,checked.detail,state(),null,checked.reasonCode,checked.field);
            World.Result result=candidate.campaign.trade(command.cityId,command.officerId,TradeQuery.operation(command.operation)==TradePlan.Operation.BUY,command.food);
            if(!result.ok)return failed(CommandResult.Error.HOST_ERROR,"TRADE_VALIDATION_DIVERGED");
            World installed=WorldCopies.copy(candidate);long next=Math.addExact(revision,1);
            authority=installed;revision=next;invalidateQueries();
            GameEvent event=emit(GameEvent.Kind.TRADE_COMMITTED,command.cityId,command.officerId,0,0,result.message);
            return new CommandResult(CommandResult.Error.NONE,result.message,state(),event);
        }catch(IOException|RuntimeException failure){return failed(CommandResult.Error.HOST_ERROR,"HOST_ERROR");}
    }
    @Override public TransportPreview preview(TransportCommand command){
        thread();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return TransportPreview.unavailable(state(),error);
        if(authority.commandsBlocked())return TransportPreview.unavailable(state(),CommandResult.Error.HOST_BUSY);
        try{return TransportQuery.capture(ruleQueryWorld(),state(),command);}
        catch(IOException|RuntimeException failure){return TransportPreview.unavailable(state(),CommandResult.Error.HOST_ERROR);}
    }
    @Override public CommandResult execute(TransportCommand command){
        write();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return failed(error,error.name());
        if(authority.commandsBlocked())return failed(CommandResult.Error.HOST_BUSY,"HOST_BUSY");
        try{
            World candidate=WorldCopies.copy(authority);TransportPreview checked=TransportQuery.capture(candidate,state(),command);
            if(!checked.allowed())return new CommandResult(checked.error,checked.detail,state(),null,checked.reasonCode,checked.field);
            World.Result result=candidate.domestic.transport(command.sourceCityId,command.targetCityId,command.officerId,command.deputies(),command.gold,command.food,command.troops,command.equipment(),command.sea,command.returnOfficers,command.ships());
            if(!result.ok)return failed(CommandResult.Error.HOST_ERROR,"TRANSPORT_VALIDATION_DIVERGED");
            World installed=WorldCopies.copy(candidate);long next=Math.addExact(revision,1);
            authority=installed;revision=next;invalidateQueries();
            GameEvent event=emit(GameEvent.Kind.TRANSPORT_DISPATCHED,command.sourceCityId,command.officerId,-command.troops,0,result.message);
            return new CommandResult(CommandResult.Error.NONE,result.message,state(),event);
        }catch(IOException|RuntimeException failure){return failed(CommandResult.Error.HOST_ERROR,"HOST_ERROR");}
    }
    /** Ordinary commands stay blocked during a contest; only these validated progress commands enter. */
    @Override public CommandResult execute(ContestCommand command){
        write();Objects.requireNonNull(command);CommandResult.Error error=invalid(command.expected,false);
        if(error!=CommandResult.Error.NONE)return failed(error,error.name());
        Contests.Session current=authority.contests.current();
        if(current==null||current.id()!=command.contestId||current.revision()!=command.contestRevision)
            return failed(CommandResult.Error.RULE_REJECTED,"对局已变化，请使用当前指令");
        if(authority.life.pending())return failed(CommandResult.Error.HOST_BUSY,"HOST_BUSY");
        try{
            World candidate=WorldCopies.copy(authority);Contests contests=candidate.contests;World.Result result;
            switch(command.operation){
                case DUEL_MOVE:
                    Duel.Stance stance;Duel.Move move;
                    try{stance=Duel.Stance.valueOf(command.stance);move=Duel.Move.valueOf(command.move);}
                    catch(IllegalArgumentException|NullPointerException invalid){return failed(CommandResult.Error.RULE_REJECTED,"单挑指令无效");}
                    result=contests.duelMove(command.contestId,command.contestRevision,stance,move,command.choice);break;
                case DEBATE_CARD:result=contests.debateCard(command.contestId,command.contestRevision,command.choice);break;
                case RETHINK:result=contests.rethink(command.contestId,command.contestRevision);break;
                case FINISH_DEBATE:result=contests.finishDebate(command.contestId,command.contestRevision,command.mercy);break;
                case CONCEDE:result=contests.concede(command.contestId,command.contestRevision);break;
                default:throw new IllegalArgumentException("Contest operation");
            }
            if(!result.ok)return failed(CommandResult.Error.RULE_REJECTED,result.message);
            World installed=WorldCopies.copy(candidate);long next=Math.addExact(revision,1);
            authority=installed;revision=next;invalidateQueries();GameEvent event=emit(GameEvent.Kind.CONTEST_ADVANCED,-1,-1,0,0,result.message);
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
            authority=installed;revision=next;invalidateQueries();
            emit(GameEvent.Kind.LEGACY_COMMITTED,-1,-1,0,0,result.message);return result;
        }catch(IOException|RuntimeException failure){return World.Result.rejected("HOST_ERROR");}
    }
    /** Assembly/restore boundary. Do not use to submit individual UI commands. */
    public void replace(World prepared)throws IOException{
        write();if(closed)throw new IllegalStateException("Session closed");
        World installed=WorldCopies.copy(Objects.requireNonNull(prepared));long next=Math.addExact(generation,1);
        authority=installed;generation=next;id=UUID.randomUUID().toString();revision=0;turn=null;invalidateQueries();
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
        authority=installed;revision=next;turn=null;invalidateQueries();emit(GameEvent.Kind.TURN_COMMITTED,-1,-1,0,0,"Turn committed");return true;
    }
    public void cancelTurn(TurnTicket ticket){write();if(ticket==turn)turn=null;}
    @Override public void close(){
        write();if(closed)return;closed=true;turn=null;invalidateQueries();emit(GameEvent.Kind.CLOSED,-1,-1,0,0,"Session closed");listeners.clear();authority=null;
    }
}

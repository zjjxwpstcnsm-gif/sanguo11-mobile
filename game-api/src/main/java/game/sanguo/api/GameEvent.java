package game.sanguo.api;

import java.util.*;

/** Committed facts; presentation must not parse the human-readable detail as a rule. */
public final class GameEvent {
    public enum Kind { RECRUITED, PATROLLED, LEGACY_COMMITTED, WORLD_REPLACED, TURN_COMMITTED, CLOSED, CONTEST_ADVANCED, DEPLOYED, DIPLOMACY_DISPATCHED, TREATY_BROKEN, CONSTRUCTION_STARTED, TRANSPORT_DISPATCHED, CITY_ACTION_COMMITTED, TRADE_COMMITTED, PRODUCTION_COMMITTED }
    public final Kind kind;
    public final StateToken state;
    /** Deduplication identity; never saved and never uses rule RNG. */
    public final String id;
    public final int cityId, officerId, troopsDelta, orderDelta;
    public final String detail;
    /** Net per-faction changes in this commit; empty for world restore/close. */
    public final List<TechniquePointsChange> techniquePointsChanges;
    public GameEvent(Kind kind,StateToken state,int cityId,int officerId,int troopsDelta,int orderDelta,String detail){
        this(kind,state,cityId,officerId,troopsDelta,orderDelta,detail,Collections.emptyList());
    }
    public GameEvent(Kind kind,StateToken state,int cityId,int officerId,int troopsDelta,int orderDelta,String detail,List<TechniquePointsChange> changes){
        this.techniquePointsChanges=Collections.unmodifiableList(new ArrayList<>(changes));
        this.id=state.sessionId+":"+state.generation+":"+state.revision+":"+kind.name();
        this.kind=kind;this.state=state;this.cityId=cityId;this.officerId=officerId;
        this.troopsDelta=troopsDelta;this.orderDelta=orderDelta;this.detail=detail;
    }
}

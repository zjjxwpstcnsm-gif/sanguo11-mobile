package game.sanguo.api;

/** Committed facts; presentation must not parse the human-readable detail as a rule. */
public final class GameEvent {
    public enum Kind { RECRUITED, PATROLLED, LEGACY_COMMITTED, WORLD_REPLACED, TURN_COMMITTED, CLOSED, CONTEST_ADVANCED, DEPLOYED, DIPLOMACY_DISPATCHED, TREATY_BROKEN, CONSTRUCTION_STARTED, TRANSPORT_DISPATCHED, CITY_ACTION_COMMITTED, TRADE_COMMITTED, PRODUCTION_COMMITTED }
    public final Kind kind;
    public final StateToken state;
    public final int cityId, officerId, troopsDelta, orderDelta;
    public final String detail;
    public GameEvent(Kind kind,StateToken state,int cityId,int officerId,int troopsDelta,int orderDelta,String detail){
        this.kind=kind;this.state=state;this.cityId=cityId;this.officerId=officerId;
        this.troopsDelta=troopsDelta;this.orderDelta=orderDelta;this.detail=detail;
    }
}

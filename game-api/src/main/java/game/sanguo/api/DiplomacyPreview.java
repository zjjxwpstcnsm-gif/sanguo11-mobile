package game.sanguo.api;

import java.util.*;

/** Immutable current-engine query. No World, live collections, random draws, or committed outcome. */
public final class DiplomacyPreview {
    public final StateToken state;
    public final CommandResult.Error error;
    public final String reasonCode,field,detail;
    /** Null for host failures; hypothetical remaining values may be negative on rule rejection. */
    public final Resources resources;
    /** Present only when allowed. Arrival estimates can change during travel. */
    public final Forecast forecast;
    public final List<Integer> treatyDurations;
    public DiplomacyPreview(StateToken state,CommandResult.Error error,String reasonCode,String field,String detail,
            Resources resources,Forecast forecast,List<Integer> treatyDurations){
        this.state=state;this.error=error;this.reasonCode=reasonCode;this.field=field;this.detail=detail;
        this.resources=resources;this.forecast=forecast;this.treatyDurations=Collections.unmodifiableList(new ArrayList<>(treatyDurations));
    }
    public boolean allowed(){return error==CommandResult.Error.NONE;}
    public static DiplomacyPreview unavailable(StateToken state,CommandResult.Error error){
        return new DiplomacyPreview(state,error,error.name(),"global",error.name(),null,null,Collections.emptyList());
    }
    public static final class Resources {
        public final int goldAvailable,goldCost,actionPointsAvailable,actionPointsCost;
        public final long goldRemaining,actionPointsRemaining;
        public Resources(int goldAvailable,int goldCost,long goldRemaining,int actionPointsAvailable,int actionPointsCost,long actionPointsRemaining){
            this.goldAvailable=goldAvailable;this.goldCost=goldCost;this.goldRemaining=goldRemaining;
            this.actionPointsAvailable=actionPointsAvailable;this.actionPointsCost=actionPointsCost;this.actionPointsRemaining=actionPointsRemaining;
        }
    }
    public static final class Forecast {
        public final boolean delayed,debateOnRejection;
        public final int destinationId,oneWayTurns,roundTripTurns,treatyTurns,currentRelation;
        public final String destinationName;
        /** -1 means no random acceptance test; goodwill can still be aborted by changed world state. */
        public final int initialAcceptancePercent,relationDeltaOnSuccess;
        /** Nominal change to other living factions, subject to each relation's lower bound. */
        public final int otherRelationsDelta;
        public Forecast(boolean delayed,int destinationId,String destinationName,int oneWayTurns,int roundTripTurns,
                int treatyTurns,int currentRelation,int initialAcceptancePercent,int relationDeltaOnSuccess,int otherRelationsDelta,boolean debateOnRejection){
            this.delayed=delayed;this.destinationId=destinationId;this.destinationName=destinationName;this.oneWayTurns=oneWayTurns;this.roundTripTurns=roundTripTurns;
            this.treatyTurns=treatyTurns;this.currentRelation=currentRelation;this.initialAcceptancePercent=initialAcceptancePercent;
            this.relationDeltaOnSuccess=relationDeltaOnSuccess;this.otherRelationsDelta=otherRelationsDelta;this.debateOnRejection=debateOnRejection;
        }
    }
}

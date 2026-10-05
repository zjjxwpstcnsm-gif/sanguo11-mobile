package game.sanguo.runtime.query;

import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;

/** Decode wire names and map immutable facts; formulas and validation stay in core. */
public final class DiplomacyQuery {
    private DiplomacyQuery(){}
    public static DiplomacyPlan.Operation operation(String name){
        try{return DiplomacyPlan.Operation.valueOf(name);}catch(IllegalArgumentException|NullPointerException e){return null;}
    }
    public static DiplomacyPreview capture(World w,StateToken state,DiplomacyCommand c){
        DiplomacyPlan p=w.campaign.previewDiplomacy(c.cityId,c.officerId,c.targetSide,operation(c.operation),c.turns);
        DiplomacyPreview.Forecast forecast=p.allowed()?new DiplomacyPreview.Forecast(p.delayed,p.destinationId,p.destinationName,
            p.oneWayTurns,p.roundTripTurns,p.treatyTurns,p.currentRelation,p.initialAcceptancePercent,p.relationDeltaOnSuccess,p.otherRelationsDelta,p.debateOnRejection):null;
        return new DiplomacyPreview(state,p.allowed()?CommandResult.Error.NONE:CommandResult.Error.RULE_REJECTED,
            p.allowed()?"NONE":p.failure.code,p.allowed()?"global":p.failure.field,p.allowed()?"":p.failure.detail,
            new DiplomacyPreview.Resources(p.goldAvailable,p.goldCost,p.goldRemaining,p.actionPointsAvailable,p.actionPointsCost,p.actionPointsRemaining),
            forecast,w.campaign.treatyDurations());
    }
}

package game.sanguo.runtime.query;

import game.sanguo.api.*;
import game.sanguo.core.*;

public final class ConstructionQuery {
    private ConstructionQuery(){}
    public static Domestic.Kind kind(String value){try{return Domestic.Kind.valueOf(value);}catch(IllegalArgumentException|NullPointerException e){return null;}}
    public static ConstructionPreview capture(World w,StateToken state,ConstructionCommand c){
        ConstructionPlan p=w.domestic.previewBuild(c.cityId,c.officerId,kind(c.kind),new Hex(c.q,c.r));
        return new ConstructionPreview(state,p.allowed()?CommandResult.Error.NONE:CommandResult.Error.RULE_REJECTED,
            p.allowed()?"NONE":p.failure.code,p.allowed()?"global":p.failure.field,p.allowed()?"":p.failure.detail,
            new ConstructionPreview.Resources(p.goldAvailable,p.goldCost,p.goldRemaining,p.actionPointsAvailable,p.actionPointsCost,p.actionPointsRemaining),
            p.allowed()?new ConstructionPreview.Completion(p.turns,p.level,p.initialDurability,p.maximumDurability,p.label,p.effect):null);
    }
}

package game.sanguo.core;

/** Immutable ordinary-build facts. Base gold/AP costs and initial level have native evidence; district AP scope remains pending. */
public final class ConstructionPlan {
    public final RuleFailure failure;
    public final int goldAvailable,goldCost,actionPointsAvailable,actionPointsCost,turns,level,initialDurability,maximumDurability;
    public final long goldRemaining,actionPointsRemaining;
    public final String label,effect;
    ConstructionPlan(World w,World.City c,World.Officer o,Domestic.Kind kind,RuleFailure failure){
        this.failure=failure;goldAvailable=c==null?0:c.gold;goldCost=kind==null||w.cityActionCost(o,PcFacilityCosts.BUILD_ACTION_POINTS)==0?0:kind.cost;
        actionPointsAvailable=w.cityActionPoints(c);
        actionPointsCost=w.cityActionCost(o,PcFacilityCosts.BUILD_ACTION_POINTS);goldRemaining=(long)goldAvailable-goldCost;actionPointsRemaining=(long)actionPointsAvailable-actionPointsCost;
        turns=o==null?0:Domestic.constructionTurns(o);level=kind==null?0:Domestic.buildLevel(kind);
        Domestic.Facility preview=kind==null?null:new Domestic.Facility(0,c==null?-1:c.id,kind,null,o==null?-1:o.id,turns);
        initialDurability=preview==null?0:preview.hp;maximumDurability=preview==null?0:preview.maxHp();
        label=kind==null?"":kind.label;effect=kind==null?"":Domestic.buildEffect(kind);
    }
    public boolean allowed(){return failure==null;}
}

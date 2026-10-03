package game.sanguo.core;

/** Current-engine facts. Forecasts at departure do not guarantee the later arrival outcome. */
public final class DiplomacyPlan {
    public enum Operation { GOODWILL, CEASEFIRE, ALLIANCE, BREAK_TREATY }
    public final RuleFailure failure;
    public final int goldAvailable,goldCost,actionPointsAvailable,actionPointsCost;
    public final long goldRemaining,actionPointsRemaining;
    public final int destinationId,oneWayTurns,roundTripTurns,treatyTurns,currentRelation;
    /** -1 means no probability applies. Relation delta is bounded by the current [-100,100] limits. */
    public final int initialAcceptancePercent,relationDeltaOnSuccess,otherRelationsDelta;
    public final boolean delayed,debateOnRejection;
    public final String destinationName;
    DiplomacyPlan(World w,int city,int officer,int target,Operation operation,int turns,RuleFailure failure){
        this.failure=failure;World.City c=w.city(city);World.Officer o=w.officer(officer);
        boolean resolving=w.envoys.resolving(officer);
        goldAvailable=c==null?0:c.gold;goldCost=resolving?0:operation==Operation.GOODWILL?500:
            operation==Operation.CEASEFIRE||operation==Operation.ALLIANCE?1000:0;
        actionPointsAvailable=w.active<0||w.active>=w.actionPoints.length?0:w.actionPoints[w.active];
        actionPointsCost=w.cityActionCost(o);goldRemaining=(long)goldAvailable-goldCost;
        actionPointsRemaining=(long)actionPointsAvailable-actionPointsCost;
        delayed=operation!=null&&operation!=Operation.BREAK_TREATY&&!resolving;
        boolean validTarget=target>=0&&target<w.factions.length;
        int destination=delayed&&validTarget?w.personnel.destination(target):-1;
        World.City to=w.city(destination);destinationId=to==null?-1:to.id;destinationName=to==null?"":to.name;
        oneWayTurns=to!=null&&c!=null?w.envoys.travelTurns(city,destination):0;roundTripTurns=oneWayTurns*2;
        treatyTurns=operation==Operation.CEASEFIRE||operation==Operation.ALLIANCE?turns:0;
        currentRelation=c!=null&&validTarget?w.strategy.factionRelation(c.owner,target):0;
        Campaign.TreatyKind kind=operation==Operation.CEASEFIRE?Campaign.TreatyKind.CEASEFIRE:
            operation==Operation.ALLIANCE?Campaign.TreatyKind.ALLIANCE:null;
        initialAcceptancePercent=kind==null?-1:w.campaign.treatyChance(officer,target,kind);
        int change=operation==Operation.GOODWILL&&o!=null?w.campaign.goodwillGain(o):
            operation==Operation.BREAK_TREATY?-50:kind!=null?10:0;
        relationDeltaOnSuccess=Math.max(-100,Math.min(100,currentRelation+change))-currentRelation;
        otherRelationsDelta=operation==Operation.BREAK_TREATY?-10:0;
        debateOnRejection=kind!=null&&o!=null&&w.active==w.player&&w.skills.has(o,Skill.LUNKE);
    }
    public boolean allowed(){return failure==null;}
}

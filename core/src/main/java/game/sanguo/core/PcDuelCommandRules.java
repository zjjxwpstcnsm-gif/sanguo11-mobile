package game.sanguo.core;

/** Original58b640 prebattle speech and refusal. Camera visibility is an
 * explicit command fact: terrain/water/fire and render queries cannot stand
 * in for it. Ordinary action/AP admission remains a separate binding. */
final class PcDuelCommandRules {
    /** Exact signed-word truncation and division59fe00, including overflow. */
    static int coarseX(int sourceX){return (short)(sourceX*4+112)/4;}
    static int coarseY(int sourceX,int sourceY){return (short)(2*((sourceX&1)+2*sourceY+56))/4;}
    static boolean visible(int sourceX,int sourceY,java.util.function.IntBinaryOperator coarseByte){
        int x=coarseX(sourceX),y=coarseY(sourceX,sourceY);
        return x>=0&&y>=0&&x<256&&y<256&&coarseByte.applyAsInt(x,y)!=0;
    }
    /** Returns original voice35/36, or -1 without drawing. The command owner
     * supplies a saved/explicit visibility snapshot, never a live renderer. */
    static int speech(boolean anyManual,boolean visible,boolean validNominee,PcDuelKernel.Random random){
        return anyManual&&visible&&validNominee?(random.percent(50)?35:36):-1;
    }
    static final class Refusal {
        final int troopLoss,ownEnergyDelta,targetEnergyDelta;
        Refusal(int loss,int own,int other){troopLoss=loss;ownEnergyDelta=own;targetEnergyDelta=other;}
    }
    /** Native draws the cap even when five percent is already smaller. Valid
     * tactic refusals still draw but return before energy/troop writes. */
    static Refusal refusal(int targetTroops,boolean validTarget,boolean validOwn,boolean tactic,PcDuelKernel.Random random){
        if(targetTroops<0||targetTroops>65535)throw new IllegalArgumentException("原单挑目标兵力字段无效");
        int loss=validTarget?Math.min(targetTroops*5/100,300+random.uniform(50)):0;
        return new Refusal(tactic?0:loss,tactic||!validOwn?0:10,tactic||!validTarget?0:-5);
    }
    /** Original59f570 marks the actor unit and its current represented crew.
     * The ordinary command caller owns movement/task replacement and token.
     * Opponent action, budgets, resources and both RNG are untouched. */
    static void consumeActor(World w,World.Unit actor)throws java.io.IOException {
        if(actor==null||w.unit(actor.id)!=actor||actor instanceof Domestic.Mission)throw new java.io.IOException("原单挑行动部队无效");
        var crew=w.army.crew(actor);if(crew.isEmpty()||crew.size()>3)throw new java.io.IOException("原单挑行动编队无效");
        for(var person:crew)if(!w.life.present(person.id)||person.unitId!=actor.id)throw new java.io.IOException("原单挑当前行动人物无效");
        actor.acted=true;for(var person:crew)person.acted=true;
    }
    private PcDuelCommandRules(){}
}

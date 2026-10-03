package game.sanguo.core;

/** Explicit legal saved formation with a leader and deputy on both sides. */
public final class ControlIntelligenceFixture {
    private ControlIntelligenceFixture(){}
    public static World world(int attackLeader,int attackDeputy,int defenseLeader,int defenseDeputy,long seed){
        World w=CombatSceneFixture.world("counter");w.scenarioName="control intelligence verification";
        w.unit(1).energy=100;
        for(int side=0;side<2;side++){
            World.Unit unit=w.unit(side+1);
            World.Officer deputy=new World.Officer(4+side,"副将"+side,side,-1,80,80,80,80,80);
            deputy.sex=World.Sex.MALE;deputy.unitId=unit.id;w.officers.add(deputy);
            unit.deputies=new int[]{deputy.id};
        }
        w.officer(1).intelligence=attackLeader;w.officer(4).intelligence=attackDeputy;
        w.officer(3).intelligence=defenseLeader;w.officer(5).intelligence=defenseDeputy;
        w.strategy.setSeed(seed);return w;
    }
}

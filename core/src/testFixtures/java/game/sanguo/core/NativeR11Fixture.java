package game.sanguo.core;

/** Explicit legal fixture inputs, never packaged as production game/scenario data. */
public final class NativeR11Fixture {
    public static World world(String kind){
        if(kind.startsWith("equipment-"))return UnitR10Fixture.fighting(kind.substring(10));
        World w=CombatSceneFixture.world(kind.equals("facility-counter")?"facilities":kind.equals("status")?"counter":kind.equals("fire-arrow")?"arrow":kind);
        w.scenarioName="R11 explicit legal-command fixture: "+kind;
        if(kind.equals("status")){w.officer(1).intelligence=100;w.officer(1).skillId=Skill.SHENSUAN.id;}
        return w;
    }
    public static World.Result command(World w,String kind){
        if(kind.startsWith("equipment-"))return UnitR10Fixture.fight(w,kind.substring(10));
        if(kind.equals("facility-counter"))return w.war.attackStructure(1,new Hex(9,8));
        if(kind.equals("status"))return w.war.plot(1,w.unit(2).hex,War.Plot.CONFUSE);
        if(kind.equals("fire-arrow"))return w.war.tactic(1,2,War.Tactic.FIRE_ARROW);
        return CombatSceneFixture.command(w,kind);
    }
}

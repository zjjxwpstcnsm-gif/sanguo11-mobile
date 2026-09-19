package game.sanguo.core;

/** Immutable presentation evidence emitted only by an applied, damaging tactic critical hit. */
public final class CriticalHit {
    public final int officerId,owner,unitId;
    public final String name,tactic;
    private final int leadership,war,intelligence,politics,charm;
    private final World.Sex sex;
    CriticalHit(World.Officer o,World.Unit u,String tactic){
        officerId=o.id;owner=u.owner;unitId=u.id;name=o.name;this.tactic=tactic;
        leadership=o.leadership;war=o.war;intelligence=o.intelligence;politics=o.politics;charm=o.charm;sex=o.sex;
    }
    public World.Officer officerCopy(){World.Officer o=new World.Officer(officerId,name,owner,-1,leadership,war,intelligence,politics,charm);o.sex=sex;return o;}
}

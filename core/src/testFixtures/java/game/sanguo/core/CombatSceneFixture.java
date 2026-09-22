package game.sanguo.core;

/** Legal production actions on explicit small fixtures; setup never ships in runtime. */
public final class CombatSceneFixture {
    public static World world(String kind){
        World w=kind.equals("facilities")?Turn48Fixture.world():Realm52Fixture.criticalWorld(kind.equals("critical"));
        if(kind.equals("facilities"))return w;
        World.Unit a=w.unit(1),b=w.unit(2);
        if(kind.equals("arrow"))replace(w,a,World.Weapon.CROSSBOW);
        if(kind.equals("stone"))replace(w,a,World.Weapon.CATAPULT);
        if(kind.equals("charge"))replace(w,a,World.Weapon.CAVALRY);
        if(kind.equals("lightning"))w.officer(1).skillId=Skill.GUIMEN.id;
        if(kind.equals("fire"))w.officer(1).intelligence=100;
        if(kind.equals("defeat"))b.troops=1;
        if(kind.equals("enemy")){w.active=1;a.status=War.Status.CONFUSED;a.statusTurns=1;b.status=War.Status.NORMAL;b.statusTurns=0;}
        if(kind.equals("site")){w.cities.set(1,new World.City(1,"乙城",new Hex(11,8),1));a.hex=new Hex(9,8);w.units.remove(b);w.officer(3).unitId=-1;w.officer(3).cityId=1;w.city(1).defense=1;w.city(1).troops=1;}
        return w;
    }
    private static void replace(World w,World.Unit old,World.Weapon weapon){
        World.Unit u=new World.Unit(old.id,old.owner,old.officerId,weapon,old.hex,old.troops,old.food);u.energy=100;w.units.set(w.units.indexOf(old),u);
    }
    public static void action(World w,String kind){
        if(kind.equals("facilities")){Turn48Fixture.facilityCommands(w);return;}
        World.Result r;
        if(kind.equals("stone"))r=w.army.tactic(1,w.unit(2).hex,Army.Tactic.STONE);
        else if(kind.equals("critical"))r=Realm52Fixture.criticalCommand(w);
        else if(kind.equals("fire")||kind.equals("lightning"))r=w.war.plot(1,w.unit(2).hex,kind.equals("fire")?War.Plot.FIRE:War.Plot.LIGHTNING);
        else if(kind.equals("site"))r=w.siege(1,1);
        else r=kind.equals("enemy")?w.attack(2,1):w.attack(1,2);
        if(!r.ok)throw new AssertionError(kind+": "+r.message);
    }
}

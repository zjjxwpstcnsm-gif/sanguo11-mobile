package game.sanguo.core;

/** Legal production actions on explicit small fixtures; setup never ships in runtime. */
public final class CombatSceneFixture {
    public static World world(String kind){
        World w=kind.equals("facilities")?Turn48Fixture.world():Realm52Fixture.criticalWorld(kind.equals("critical"));
        if(kind.equals("facilities"))return w;
        World.Unit a=w.unit(1),b=w.unit(2);
        if(kind.equals("counter")){b.status=War.Status.NORMAL;b.statusTurns=0;}
        if(kind.equals("arrow"))replace(w,a,World.Weapon.CROSSBOW);
        if(kind.equals("stone"))replace(w,a,World.Weapon.CATAPULT);
        if(kind.equals("charge"))replace(w,a,World.Weapon.CAVALRY);
        if(kind.equals("lightning"))w.officer(1).skillId=Skill.GUIMEN.id;
        if(kind.equals("critical-fire")){w.officer(1).skillId=Skill.SHENSUAN.id;w.officer(1).intelligence=100;}
        if(kind.equals("fire")||kind.startsWith("trap")){w.officer(1).intelligence=100;}
        if(kind.startsWith("trap")){
            War.StructureKind trap=kind.equals("trap-ball")?War.StructureKind.FIRE_BALL:kind.equals("trap-ship")?War.StructureKind.FIRE_SHIP:War.StructureKind.FIRE_SEED;
            Hex tile=b.hex;b.hex=new Hex(10,8);w.war.structures.add(new War.Structure(w.war.nextStructureId++,1,trap,tile,trap.hp));
            if(kind.equals("trap-ship"))w.terrain[tile.q][tile.r]=World.Terrain.WATER;
        }
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
        World.Result r=command(w,kind);
        if(!r.ok)throw new AssertionError(kind+": "+r.message);
    }
    /** Same actual result used by the normal MainActivity command transaction in R11 probes. */
    public static World.Result command(World w,String kind){
        if(kind.equals("facilities"))throw new IllegalArgumentException("facility ticks are journaled separately");
        World.Result r;
        if(kind.equals("stone"))r=w.army.tactic(1,w.unit(2).hex,Army.Tactic.STONE);
        else if(kind.equals("critical"))r=Realm52Fixture.criticalCommand(w);
        else if(kind.equals("critical-fire")||kind.equals("fire")||kind.equals("lightning")||kind.startsWith("trap"))r=w.war.plot(1,kind.startsWith("trap")?new Hex(9,8):w.unit(2).hex,kind.equals("lightning")?War.Plot.LIGHTNING:War.Plot.FIRE);
        else if(kind.equals("site"))r=w.siege(1,1);
        else r=kind.equals("enemy")?w.attack(2,1):w.attack(1,2);
        return r;
    }
}

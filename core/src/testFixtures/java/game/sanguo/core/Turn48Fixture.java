package game.sanguo.core;

import java.util.*;

/** Shared real-command fixture for core assertions and installed Android presentation checks. */
public final class Turn48Fixture {
    private Turn48Fixture(){}
    public static World world(){
        World w=new World(28,18,"甲军","乙军","丙军");
        w.cities.add(new World.City(0,"甲城",new Hex(2,2),0));
        w.cities.add(new World.City(1,"乙城",new Hex(24,13),1));
        w.cities.add(new World.City(2,"丙城",new Hex(24,3),2));
        for(World.City c:w.cities){c.gold=25000;c.food=100000;c.troops=24000;Arrays.fill(c.equipment,0,4,18000);Arrays.fill(c.equipment,5,c.equipment.length,2);Arrays.fill(c.ships,2);}
        for(int side=0;side<3;side++)for(int i=0;i<3;i++){
            int id=side*3+i;World.Officer o=new World.Officer(id,"将"+id,side,side,80,80,80,80,80);
            o.loyalty=100;Arrays.fill(o.aptitude,3);w.officers.add(o);
        }
        w.strategy.initializeOffices();
        unit(w,1,new Hex(8,8));unit(w,4,new Hex(10,8));unit(w,7,new Hex(18,5));
        structure(w,0,War.StructureKind.ARROW_TOWER,new Hex(9,7));
        structure(w,1,War.StructureKind.CAMP,new Hex(9,8));
        structure(w,0,War.StructureKind.MUSIC,new Hex(7,8));
        w.unit(1).energy=95;return w;
    }
    private static void unit(World w,int officer,Hex hex){
        World.Officer o=w.officer(officer);w.strategy.releaseGovernor(officer);o.cityId=-1;o.unitId=w.nextUnitId;
        World.Unit u=new World.Unit(w.nextUnitId++,o.owner,o.id,World.Weapon.SPEAR,hex,8000,30000);u.gold=10000;w.units.add(u);
    }
    private static void structure(World w,int owner,War.StructureKind kind,Hex hex){w.war.structures.add(new War.Structure(w.war.nextStructureId++,owner,kind,hex,kind.hp));}
    public static void facilityCommands(World w){
        w.fieldworks.towers();
        World.Result result=w.war.attackStructure(1,new Hex(9,8));if(!result.ok)throw new AssertionError(result.message);
        w.energy.settleTurn();
    }
}

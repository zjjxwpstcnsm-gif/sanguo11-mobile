package game.sanguo.core;

import java.util.Arrays;

/** Four-rule fixture shared by JVM and instrumentation; never in the app source set. */
public final class ArchitectureFixture {
    private ArchitectureFixture(){}
    public static World create(){
        World w=new World(20,16,"甲","乙");
        w.cities.add(new World.City(10,"甲城",new Hex(2,3),0));w.cities.add(new World.City(20,"乙城",new Hex(17,10),1));
        for(int i=0;i<6;i++){World.Officer o=new World.Officer(i,"将"+i,i<3?0:1,-1,90,90,90,90,90);Arrays.fill(o.aptitude,3);o.unitId=i<3?1:2;w.officers.add(o);}
        World.Unit a=new World.Unit(1,0,0,World.Weapon.CROSSBOW,new Hex(6,6),10000,50000),b=new World.Unit(2,1,3,World.Weapon.SPEAR,new Hex(7,6),10000,50000);
        a.deputies=new int[]{1,2};b.deputies=new int[]{4,5};w.units.add(a);w.units.add(b);w.nextUnitId=3;w.strategy.setSeed(88);return w;
    }
}

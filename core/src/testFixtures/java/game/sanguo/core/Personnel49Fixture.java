package game.sanguo.core;

import java.util.Arrays;

/** Small explicit scenario shared with installed UI checks; not a selectable gameplay scenario. */
public final class Personnel49Fixture {
    private Personnel49Fixture(){}
    public static World world(){
        World w=new World(26,20,"甲军","乙军","丙军");
        w.cities.add(new World.City(0,"本城",new Hex(2,2),0));w.cities.add(new World.City(1,"友城",new Hex(4,4),0));
        w.cities.add(new World.City(10,"敌都",new Hex(21,16),1));w.cities.add(new World.City(20,"第三城",new Hex(23,3),2));
        for(World.City c:w.cities){c.food=100000;c.gold=100000;c.troops=24000;Arrays.fill(c.equipment,0,4,24000);}
        for(int id:new int[]{0,1,2,3,4,5,6,10,11,12,13,14,20,21}){
            int owner=id<10?0:id<20?1:2;int city=owner==0?0:owner==1?10:20;
            World.Officer o=new World.Officer(id,"将"+id,owner,city,80,80,80,100,100);o.affinity=owner*50;o.honor=3;o.loyalty=100;Arrays.fill(o.aptitude,3);w.officers.add(o);
        }
        w.strategy.initializeOffices();w.officer(11).loyalty=30;w.officer(12).loyalty=0;
        attach(w,11,new int[]{12,13},new Hex(9,9));attach(w,2,new int[]{3},new Hex(6,7));
        return w;
    }
    public static World.Unit attach(World w,int officer,int[] deputies,Hex hex){
        World.Officer leader=w.officer(officer);World.Unit u=new World.Unit(w.nextUnitId++,leader.owner,officer,World.Weapon.SPEAR,hex,6000,12000);u.deputies=deputies;u.gold=500;
        w.units.add(u);for(World.Officer o:w.army.crew(u)){o.cityId=-1;o.unitId=u.id;w.strategy.releaseGovernor(o.id);}return u;
    }
}

package game.sanguo.core;

import java.util.*;

/** Explicit synthetic stress scenes, not changes to any official scenario. */
public final class FieldSceneFixture {
    public static World create(int units,boolean forest,boolean facilities){
        World w=new World(40,40,"演练甲","演练乙");
        w.cities.add(new World.City(0,"测试城",new Hex(4,4),0));
        w.cities.add(new World.City(1,"对照城",new Hex(35,35),1));
        for(World.City c:w.cities){c.gold=50000;c.food=200000;c.troops=20000;}
        for(int i=0;i<units+4;i++){
            World.Officer o=new World.Officer(i,"演练将"+i,i==units+3?1:0,i==units+3?1:0,80,80,80,90,85);o.loyalty=100;Arrays.fill(o.aptitude,3);w.officers.add(o);
        }
        w.strategy.initializeOffices();
        if(forest)for(int q=9;q<29;q++)for(int r=9;r<29;r++)w.terrain[q][r]=World.Terrain.FOREST;
        for(int i=0;i<units;i++){
            World.Officer o=w.officer(i+1);w.strategy.releaseGovernor(o.id);o.cityId=-1;o.unitId=w.nextUnitId;
            World.Unit u=new World.Unit(w.nextUnitId++,0,o.id,World.Weapon.values()[i%World.Weapon.values().length],new Hex(12+i%10,14+i/10),4000,30000);u.gold=10000;w.units.add(u);
        }
        if(facilities){int i=0;for(Domestic.Kind kind:Domestic.Kind.values()){
            Domestic.Facility f=new Domestic.Facility(w.domestic.nextFacilityId++,0,kind,new Hex(12+(i%6)*2,24+(i/6)*2),-1,0);f.level=Domestic.buildLevel(kind);w.domestic.facilities.add(f);i++;
        }i=0;for(War.StructureKind kind:War.StructureKind.values()){
            Hex h=new Hex(10+(i%7)*2,8+(i/7)*2);w.terrain[h.q][h.r]=kind==War.StructureKind.FIRE_SHIP?World.Terrain.WATER:World.Terrain.PLAIN;
            w.war.structures.add(new War.Structure(w.war.nextStructureId++,0,kind,h,kind.hp));i++;
        }}
        return w;
    }
    public static World port(){
        World w=create(0,false,false);World.City west=new World.City(2,"西测试港",new Hex(18,20),0),east=new World.City(3,"东测试港",new Hex(21,18),0);
        west.kind=east.kind=World.SiteKind.PORT;w.cities.add(west);w.cities.add(east);
        for(int q=19;q<=20;q++)for(int r=0;r<w.height;r++)w.terrain[q][r]=World.Terrain.WATER;
        World.Officer o=w.officer(1);w.strategy.releaseGovernor(1);o.cityId=-1;o.unitId=w.nextUnitId;
        World.Unit u=new World.Unit(w.nextUnitId++,0,1,World.Weapon.CAVALRY,new Hex(18,19),5000,30000);u.ship=Army.Ship.TOWER_SHIP;w.units.add(u);return w;
    }
}

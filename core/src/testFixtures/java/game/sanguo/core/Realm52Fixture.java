package game.sanguo.core;

import java.util.*;

/** Real paid tactic, deterministic hit, shipped portrait; shared core and Android test fixture. */
public final class Realm52Fixture {
    private Realm52Fixture(){}
    public static World criticalWorld(boolean critical){
        World w=new World(22,16,"刘备军","曹操军");w.scenarioName="暴击演示验证";
        w.cities.add(new World.City(0,"甲城",new Hex(2,2),0));w.cities.add(new World.City(1,"乙城",new Hex(19,13),1));
        for(int i=0;i<4;i++){
            int side=i/2;World.Officer o=new World.Officer(i,new String[]{"刘备","赵云","曹操","夏侯惇"}[i],side,side,90,90,80,80,80);
            o.sex=World.Sex.MALE;Arrays.fill(o.aptitude,3);w.officers.add(o);
        }
        w.strategy.initializeOffices();w.strategy.setSeed(52);
        for(int side=0;side<2;side++){
            World.Officer o=w.officer(side*2+1);o.cityId=-1;o.unitId=w.nextUnitId;
            World.Unit u=new World.Unit(w.nextUnitId++,side,o.id,World.Weapon.SPEAR,new Hex(8+side,8),8000,16000);w.units.add(u);
        }
        w.officer(1).skillId=critical?Skill.QIANGSHEN.id:"none";
        w.unit(2).status=War.Status.CONFUSED;w.unit(2).statusTurns=1;
        return w;
    }
    public static World.Result criticalCommand(World w){return w.war.tactic(1,2,War.Tactic.SPIRAL);}
}

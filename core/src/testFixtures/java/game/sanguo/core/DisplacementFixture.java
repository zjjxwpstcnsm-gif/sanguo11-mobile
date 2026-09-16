package game.sanguo.core;

import java.util.Arrays;

/** Small, named battle arrangements; commands are always issued by the caller. */
public final class DisplacementFixture {
    public static final String[] CASES={"plain","mountain","cliff","shore","edge","friend","enemy","ally","ceasefire","city","gate","port","facility","tower","own-seed","enemy-ball","fire","second-block","kill","hook-retreat","break-forest","hook-cliff","naval-shore"};
    public static War.Tactic tactic(String scene){
        if(scene.startsWith("hook"))return War.Tactic.HOOK;
        if(scene.startsWith("break"))return War.Tactic.BREAKTHROUGH;
        if(scene.equals("kill"))return War.Tactic.CHARGE;
        return scene.equals("second-block")?War.Tactic.DOUBLE_THRUST:War.Tactic.THRUST;
    }
    public static World create(String scene){
        World w=new World(16,12,"我军","敌军","第三军");w.scenarioId="displacement-probe";w.scenarioName="位移验收";w.strategy.setSeed(29016);
        for(int i=0;i<3;i++){
            w.cities.add(new World.City(10+i,"后营"+i,new Hex(1+i*6,1),i));
            World.Officer ruler=new World.Officer(10+i,"主公"+i,i,10+i,80,80,80,80,80);ruler.role=Strategy.Role.RULER;ruler.loyalty=100;w.officers.add(ruler);
        }
        World.Unit a=unit(w,1,0,tactic(scene).weapon,new Hex(5,6),8000),b=unit(w,2,1,World.Weapon.SPEAR,new Hex(6,6),8000);
        b.status=War.Status.CONFUSED;b.statusTurns=1;Hex rear=new Hex(7,6);
        switch(scene){
            case "mountain":w.terrain[7][6]=World.Terrain.MOUNTAIN;break;
            case "cliff":w.terrain[7][6]=World.Terrain.MOUNTAIN_PATH;break;
            case "shore":w.terrain[7][6]=World.Terrain.WATER;break;
            case "edge":a.hex=new Hex(14,6);b.hex=new Hex(15,6);break;
            case "friend":unit(w,3,0,World.Weapon.SPEAR,rear,4000);break;
            case "enemy":unit(w,3,1,World.Weapon.SPEAR,rear,4000);break;
            case "ally":case "ceasefire":unit(w,3,2,World.Weapon.SPEAR,rear,4000);w.campaign.treaties.add(new Campaign.Treaty(0,2,scene.equals("ally")?Campaign.TreatyKind.ALLIANCE:Campaign.TreatyKind.CEASEFIRE,12));break;
            case "city":case "gate":case "port":World.City c=new World.City(20,"阻挡据点",rear,1);c.kind=scene.equals("gate")?World.SiteKind.GATE:scene.equals("port")?World.SiteKind.PORT:World.SiteKind.CITY;w.cities.add(c);break;
            case "facility":w.domestic.facilities.add(new Domestic.Facility(1,11,Domestic.Kind.MARKET,rear,-1,0));w.domestic.nextFacilityId=2;break;
            case "tower":case "own-seed":case "enemy-ball":War.StructureKind kind=scene.equals("tower")?War.StructureKind.ARROW_TOWER:scene.equals("own-seed")?War.StructureKind.FIRE_SEED:War.StructureKind.FIRE_BALL;w.war.structures.add(new War.Structure(1,scene.equals("own-seed")?0:1,kind,rear,kind.hp));w.war.nextStructureId=2;break;
            case "fire":w.war.fires.add(new War.Fire(rear,0,2));break;
            case "second-block":w.terrain[8][6]=World.Terrain.MOUNTAIN;break;
            case "kill":b.troops=1;b.gold=123;b.food=456;break;
            case "hook-retreat":w.terrain[4][6]=World.Terrain.MOUNTAIN;break;
            case "break-forest":w.terrain[7][6]=World.Terrain.FOREST;break;
            case "hook-cliff":w.terrain[4][6]=World.Terrain.MOUNTAIN_PATH;break;
            case "naval-shore":w.terrain[5][6]=World.Terrain.WATER;w.terrain[6][6]=World.Terrain.WATER;a.ship=Army.Ship.TOWER_SHIP;b.ship=Army.Ship.TOWER_SHIP;break;
            default:break;
        }
        return w;
    }
    public static World.Unit unit(World w,int id,int side,World.Weapon weapon,Hex hex,int troops){
        World.Officer o=new World.Officer(id,"战将"+id,side,-1,80,80,80,80,80);Arrays.fill(o.aptitude,3);o.unitId=id;w.officers.add(o);
        World.Unit u=new World.Unit(id,side,id,weapon,hex,troops,30000);u.energy=100;w.units.add(u);w.nextUnitId=Math.max(w.nextUnitId,id+1);return u;
    }
}

package game.sanguo.core;

import java.util.*;

/** Explicit, saveable visual fixtures. No fixture source belongs to main or an official scenario. */
public final class UnitR10Fixture {
    public static final String[] KINDS={"SPEAR","HALBERD","CROSSBOW","CAVALRY","SWORD","RAM","SIEGE_TOWER","WOODEN_BEAST","CATAPULT","transport","BOAT","TOWER_SHIP","WARSHIP"};
    public static World moving(String kind,boolean ridge) {
        World w=FieldSceneFixture.create(1,false,false);w.scenarioName="R10 explicit "+kind+" movement fixture";
        World.Unit old=w.units.get(0),unit;
        boolean ship=kind.equals("BOAT")||kind.equals("TOWER_SHIP")||kind.equals("WARSHIP");
        if(kind.equals("transport")){
            w.units.clear();Domestic.Mission m=new Domestic.Mission(1,0,old.officerId,0,1,old.hex,true,1000,30000,8000,new int[9]);
            w.domestic.nextMissionId=2;w.domestic.missions.add(m);unit=m;w.officer(m.officerId).unitId=-1;w.officer(m.officerId).cityId=-1;
        }else{
            unit=new World.Unit(old.id,old.owner,old.officerId,ship?World.Weapon.SPEAR:World.Weapon.valueOf(kind),old.hex,8000,30000);
            w.units.set(0,unit);if(ship)unit.ship=Army.Ship.valueOf(kind);
        }
        if(ship)for(int q=8;q<23;q++)for(int r=8;r<23;r++)w.terrain[q][r]=World.Terrain.WATER;
        if(ridge&&!ship){
            w.campaign.learned.computeIfAbsent(0,k->EnumSet.noneOf(Campaign.Tech.class)).addAll(EnumSet.of(Campaign.Tech.LOGISTICS,Campaign.Tech.DIFFICULT_MARCH));
            for(int q=10;q<=16;q++)for(int r=12;r<=18;r++)w.terrain[q][r]=World.Terrain.MOUNTAIN;
            for(Hex h:new Hex[]{new Hex(12,14),new Hex(13,14),new Hex(14,14),new Hex(14,15),new Hex(14,16),new Hex(15,16)})w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN_PATH;
        }
        return w;
    }
    public static World.Unit actor(World w){return w.units.isEmpty()?w.domestic.missions.get(0):w.units.get(0);}
    public static Hex destination(World w){
        World.Unit u=actor(w);Hex chosen=null;int distance=-1;
        for(Hex h:w.orders.marchReachable(u).keySet()){
            if(h.equals(u.hex))continue;var p=w.orders.previewMove(u.id,h);
            if(p.valid()&&(p.path.size()>distance||p.path.size()==distance&&(chosen==null||h.r<chosen.r||h.r==chosen.r&&h.q<chosen.q))){chosen=h;distance=p.path.size();}
        }
        if(chosen==null)throw new AssertionError("no legal route for "+u.weapon);return chosen;
    }
    public static World.Result move(World w){return w.move(actor(w).id,destination(w));}
    public static World fighting(String kind){
        boolean siege=kind.equals("RAM")||kind.equals("SIEGE_TOWER")||kind.equals("WOODEN_BEAST");
        World w=CombatSceneFixture.world(siege?"site":"counter");w.scenarioName="R10 explicit "+kind+" combat fixture";
        World.Unit old=w.unit(1);boolean ship=kind.equals("BOAT")||kind.equals("TOWER_SHIP")||kind.equals("WARSHIP");
        World.Unit unit=new World.Unit(old.id,old.owner,old.officerId,ship?World.Weapon.SPEAR:World.Weapon.valueOf(kind),old.hex,old.troops,old.food);unit.energy=100;w.units.set(w.units.indexOf(old),unit);
        if(ship){for(int q=6;q<=12;q++)for(int r=6;r<=10;r++)w.terrain[q][r]=World.Terrain.WATER;unit.ship=Army.Ship.valueOf(kind);w.unit(2).ship=Army.Ship.WARSHIP;}
        return w;
    }
    public static World.Result fight(World w,String kind){
        if(kind.equals("RAM"))return w.army.tacticCity(1,1,Army.Tactic.RAM);
        if(kind.equals("SIEGE_TOWER"))return w.army.tacticCity(1,1,Army.Tactic.FIRE_ARROW);
        if(kind.equals("WOODEN_BEAST"))return w.army.tacticCity(1,1,Army.Tactic.FLAME);
        if(kind.equals("CATAPULT"))return w.army.tactic(1,w.unit(2).hex,Army.Tactic.STONE);
        return w.attack(1,2);
    }
}

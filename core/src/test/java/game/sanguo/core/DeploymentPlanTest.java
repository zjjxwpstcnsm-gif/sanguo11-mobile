package game.sanguo.core;

import java.util.*;
import java.util.function.Consumer;

public final class DeploymentPlanTest {
    private static int checks;
    private static void check(boolean v,String detail){checks++;if(!v)throw new AssertionError(detail);}
    private static World fixture(){
        World w=new World(22,16);w.cities.add(new World.City(10,"甲",new Hex(5,5),0));w.cities.add(new World.City(20,"乙",new Hex(18,5),1));
        for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,70+i,80+i,90+i,85,85));
        w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));
        World.City c=w.city(10);c.gold=30000;c.food=200000;c.troops=30000;Arrays.fill(c.equipment,12000);c.equipment[World.Weapon.SWORD.ordinal()]=0;for(int i=5;i<9;i++)c.equipment[i]=2;Arrays.fill(c.ships,2);
        w.strategy.initializeOffices();return w;
    }
    private static void rejected(Consumer<World> setup,int city,int leader,int[] deputies,World.Weapon weapon,Army.Ship ship,int troops,int food,int gold,String code,String field)throws Exception{
        World w=fixture();setup.accept(w);byte[] before=SaveCodec.encode(w);int id=w.nextUnitId;int logs=w.log.size();
        DeploymentPlan p=w.army.previewDeployment(city,leader,deputies,weapon,ship,troops,food,gold);
        check(!p.allowed()&&p.failure.code.equals(code)&&p.failure.field.equals(field),"reason "+code+" / "+(p.failure==null?"allowed":p.failure.code+":"+p.failure.field));
        check(p.unit==null,"no hypothetical usable unit for rejection");
        check(Arrays.equals(before,SaveCodec.encode(w))&&w.nextUnitId==id&&w.log.size()==logs,"rejected query is pure "+code);
        World.Result result=w.army.deploy(city,leader,deputies,weapon,ship,troops,food,gold);
        check(!result.ok&&result.message.equals(p.failure.detail),"normal command uses same failure and ordering "+code);
        check(Arrays.equals(before,SaveCodec.encode(w)),"rejection retains save/RNG "+code);
    }
    public static void main(String[] args)throws Exception{
        for(World.Weapon weapon:World.Weapon.values())for(Army.Ship ship:Army.Ship.values()){
            World w=fixture();byte[] before=SaveCodec.encode(w);int id=w.nextUnitId;
            DeploymentPlan p=w.army.previewDeployment(10,0,new int[]{1,2},weapon,ship,3000,9000,123);
            check(p.allowed(),"allowed "+weapon+" / "+ship);check(Arrays.equals(before,SaveCodec.encode(w))&&w.nextUnitId==id,"query changes no save/RNG/ID");
            World.Result r=w.army.deploy(10,0,new int[]{1,2},weapon,ship,3000,9000,123);check(r.ok,r.message);World.Unit u=w.unit(id);
            check(w.nextUnitId==id+1&&w.actionPoints[0]==p.actionPointsAvailable-p.actionPointsCost,"single ID/AP charge");
            check(w.city(10).troops==p.remainingTroops&&w.city(10).food==p.remainingFood&&w.city(10).gold==p.remainingGold,"predicted remaining garrison");
            check(w.city(10).equipment[weapon.ordinal()]==p.stockEquipment-p.equipmentCost,"actual equipment units");
            check(ship==Army.Ship.BOAT||w.city(10).ships[ship.ordinal()-1]==p.stockShips-p.shipCost,"actual ships");
            check(u.hex.equals(new Hex(p.unit.exitQ,p.unit.exitR))&&u.movementSpent==p.unit.movementSpent&&w.orders.remaining(u)==p.unit.movementRemaining,"paid real departure");
            check(w.combat.attackRating(u)==p.unit.attackRating&&w.combat.defenseRating(u)==p.unit.defenseRating&&w.army.aptitude(u)==p.unit.aptitude,"actual combat formation");
            check(p.unit.energy==u.energy&&p.unit.attackRange==w.war.range(u)&&p.unit.equipmentLabel.equals(w.army.equipmentLabel(u)),"unit detail uses actual core state");
            for(DeploymentPlan.TacticFact t:p.unit.tactics){String error=t.group.equals("INFANTRY")?w.war.tacticFormationError(u,War.Tactic.valueOf(t.code)):w.army.tacticFormationError(u,Army.Tactic.valueOf(t.code));check(Objects.equals(t.formationError,error),"tactic metadata shares actual formation gate");}
            boolean immutable=false;try{p.unit.tactics.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"detail lists immutable");
            check(Logistics.foodUse(w,u)==p.unit.foodUse&&Logistics.turns(u.food,p.unit.foodUse)==p.unit.foodTurns,"actual logistics");
            for(int member:new int[]{0,1,2})check(w.officer(member).unitId==id&&w.officer(member).acted,"crew locked exactly once");
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(SaveCodec.decode(SaveCodec.encode(w)))),"deployment exact roundtrip");
        }
        Consumer<World> none=w->{};
        rejected(none,-1,-1,null,null,null,-1,-1,-1,"CITY_UNAVAILABLE","city");
        rejected(none,10,-1,null,null,null,-1,-1,-1,"LEADER_UNAVAILABLE","leader");
        rejected(w->w.actionPoints[0]=0,10,0,null,null,null,-1,-1,-1,"ACTION_POINTS","global");
        rejected(none,10,0,null,null,null,-1,-1,-1,"GOLD_RANGE","gold");
        rejected(none,10,0,new int[0],null,null,3000,9000,0,"FORMATION_INVALID","weapon");
        rejected(none,10,0,new int[0],World.Weapon.SPEAR,null,3000,9000,0,"FORMATION_INVALID","ship");
        rejected(none,10,0,new int[]{1,2,3},World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000,0,"FORMATION_INVALID","deputies");
        rejected(none,10,0,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,999,9000,0,"LOAD_RANGE","troops");
        rejected(none,10,0,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,2999,0,"LOAD_RANGE","food");
        rejected(none,10,0,new int[]{1,1},World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000,0,"DEPUTY_UNAVAILABLE","deputies");
        rejected(none,10,0,new int[]{0},World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000,0,"DEPUTY_UNAVAILABLE","deputies");
        rejected(none,10,0,new int[]{20},World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000,0,"DEPUTY_UNAVAILABLE","deputies");
        rejected(w->w.city(10).troops=0,10,0,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000,0,"STOCK_INSUFFICIENT","troops");
        rejected(w->w.city(10).food=0,10,0,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000,0,"STOCK_INSUFFICIENT","food");
        rejected(w->w.city(10).equipment[0]=0,10,0,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000,0,"STOCK_INSUFFICIENT","weapon");
        rejected(w->w.city(10).ships[0]=0,10,0,new int[0],World.Weapon.SPEAR,Army.Ship.TOWER_SHIP,3000,9000,0,"STOCK_INSUFFICIENT","ship");
        rejected(w->w.nextUnitId=10000000,10,0,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000,0,"UNIT_ID_LIMIT","global");
        rejected(w->{for(Hex h:SiteFootprint.edge(w.city(10)))w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;},10,0,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,9000,0,"EXIT_UNAVAILABLE","target");
        System.out.println("PASS DeploymentPlanTest checks="+checks);
    }
}

package game.sanguo.core;

import java.util.*;

/** Immutable current-rule facts. A range can be empty (max < min); it never authorizes a commit. */
public final class DeploymentPlan {
    public final RuleFailure failure;
    public final int commandLimit,troopsMin=1000,troopsMax,foodMin,foodMax,goldMin=0,goldMax;
    public final int stockTroops,stockFood,stockGold,stockEquipment,stockShips;
    public final int reserveTroops,reserveFood,reserveGold;
    public final boolean reserveEnforced;
    public final int actionPointsAvailable,actionPointsCost,equipmentCost,shipCost,troopsCost,foodCost,goldCost;
    public final long remainingTroops,remainingFood,remainingGold;
    public final UnitFacts unit;
    DeploymentPlan(World w,World.City c,World.Officer leader,World.Weapon weapon,Army.Ship ship,int troops,int food,int gold,RuleFailure failure,World.Unit u){
        this.failure=failure;commandLimit=leader==null?0:w.government.commandLimit(leader.id);
        stockTroops=c==null?0:c.troops;stockFood=c==null?0:c.food;stockGold=c==null?0:c.gold;
        stockEquipment=c==null||weapon==null?0:c.equipment[weapon.ordinal()];
        stockShips=c==null||ship==null||ship==Army.Ship.BOAT?0:c.ships[ship.ordinal()-1];
        Districts.District district=c==null?null:w.districts.city(c.id);
        reserveTroops=district==null?0:district.reserveTroops();reserveFood=district==null?0:district.reserveFood();reserveGold=district==null?0:district.reserveGold();
        reserveEnforced=c!=null&&w.districts.executing(c.id);
        foodMin=troops;foodMax=Math.min(1000000,Math.max(0,stockFood-(reserveEnforced?reserveFood:0)));
        goldMax=Math.min(10000,Math.max(0,stockGold-(reserveEnforced?reserveGold:0)));
        int equipmentLimit=weapon==null?0:weapon==World.Weapon.SWORD?commandLimit:Army.siegeWeapon(weapon)?(stockEquipment>0?commandLimit:0):stockEquipment;
        troopsMax=Math.max(0,Math.min(Math.min(commandLimit,equipmentLimit),Math.min(foodMax,stockTroops-(reserveEnforced?reserveTroops:0))));
        actionPointsAvailable=w.cityActionPoints(c);actionPointsCost=w.cityActionCost(leader);
        equipmentCost=weapon==null?0:Army.equipmentNeeded(weapon,troops);shipCost=ship==null||ship==Army.Ship.BOAT?0:1;
        troopsCost=troops;foodCost=food;goldCost=gold;
        remainingTroops=(long)stockTroops-troops;remainingFood=(long)stockFood-food;remainingGold=(long)stockGold-gold;
        unit=u==null?null:new UnitFacts(w,u);
    }
    public boolean allowed(){return failure==null;}
    /** Estimates assume the current formation, position and supply aura; not a damage promise. */
    public static final class UnitFacts {
        public final int exitQ,exitR,movementBudget,movementSpent,movementRemaining,aptitude,leadership,war,intelligence,baseAttack,baseDefense,foodUse,foodTurns;
        public final double attackRating,defenseRating;
        public final int energy,attackRange;
        public final String equipmentLabel;
        public final List<SkillFact> skills;
        public final List<TacticFact> tactics;
        UnitFacts(World w,World.Unit u){
            exitQ=u.hex.q;exitR=u.hex.r;movementBudget=u.movementBudget;movementSpent=u.movementSpent;movementRemaining=w.orders.remaining(u);
            aptitude=w.army.aptitude(u);leadership=w.army.leadership(u);war=w.army.war(u);intelligence=w.army.intelligence(u);
            baseAttack=w.combat.baseAttack(u);baseDefense=w.combat.baseDefense(u);attackRating=w.combat.attackRating(u);defenseRating=w.combat.defenseRating(u);
            foodUse=Logistics.foodUse(w,u);foodTurns=Logistics.turns(u.food,foodUse);
            energy=u.energy;attackRange=w.war.range(u);equipmentLabel=w.army.equipmentLabel(u);
            List<SkillFact> skillFacts=new ArrayList<>();Set<String> seen=new HashSet<>();
            for(World.Officer o:w.army.crew(u))if(!"none".equals(o.skillId)&&seen.add(o.skillId))skillFacts.add(new SkillFact(o.skillId,Skill.label(o.skillId),Skill.description(o.skillId)));
            skills=Collections.unmodifiableList(skillFacts);
            List<TacticFact> tacticFacts=new ArrayList<>();
            if(!w.army.water(u.hex))for(War.Tactic t:War.Tactic.values())if(t.weapon==u.weapon)
                tacticFacts.add(new TacticFact("INFANTRY",t.name(),t.label,t.effect,t.energy,t.rank,t.minRange,w.war.tacticMaxRange(u,t),w.war.tacticFormationError(u,t)));
            for(Army.Tactic t:w.army.tactics(u))tacticFacts.add(new TacticFact("EQUIPMENT",t.name(),t.label,t.effect,w.army.tacticCost(u,t),t.rank,1,t==Army.Tactic.RAM?1:w.war.range(u),w.army.tacticFormationError(u,t)));
            tactics=Collections.unmodifiableList(tacticFacts);
        }
    }
    public static final class SkillFact {
        public final String id,label,description;
        SkillFact(String id,String label,String description){this.id=id;this.label=label;this.description=description;}
    }
    /** formationError=null still requires target/range/command-state validation. */
    public static final class TacticFact {
        public final String group,code,label,description,formationError;
        public final int energy,rank,minRange,maxRange;
        TacticFact(String group,String code,String label,String description,int energy,int rank,int minRange,int maxRange,String formationError){
            this.group=group;this.code=code;this.label=label;this.description=description;this.energy=energy;this.rank=rank;this.minRange=minRange;this.maxRange=maxRange;this.formationError=formationError;
        }
    }
}

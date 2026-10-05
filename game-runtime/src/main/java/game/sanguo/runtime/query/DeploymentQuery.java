package game.sanguo.runtime.query;

import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;

/** Enum decoding and immutable mapping only. All validation and calculations belong to core. */
public final class DeploymentQuery {
    private DeploymentQuery(){}
    public static World.Weapon weapon(String name){try{return World.Weapon.valueOf(name);}catch(IllegalArgumentException|NullPointerException e){return null;}}
    public static Army.Ship ship(String name){try{return Army.Ship.valueOf(name);}catch(IllegalArgumentException|NullPointerException e){return null;}}
    public static DeploymentPreview capture(World w,StateToken state,DeploymentCommand c){
        DeploymentPlan p=w.army.previewDeployment(c.cityId,c.leaderId,c.deputies(),weapon(c.weapon),ship(c.ship),c.troops,c.food,c.gold);
        DeploymentPlan.UnitFacts u=p.unit;
        List<DeploymentPreview.SkillFact> skills=new ArrayList<>();List<DeploymentPreview.TacticFact> tactics=new ArrayList<>();
        if(u!=null){
            for(DeploymentPlan.SkillFact fact:u.skills)skills.add(new DeploymentPreview.SkillFact(fact.id,fact.label,fact.description));
            for(DeploymentPlan.TacticFact fact:u.tactics)tactics.add(new DeploymentPreview.TacticFact(fact.group,fact.code,fact.label,fact.description,fact.energy,fact.rank,fact.minRange,fact.maxRange,fact.formationError));
        }
        return new DeploymentPreview(state,p.allowed()?CommandResult.Error.NONE:CommandResult.Error.RULE_REJECTED,
            p.allowed()?"NONE":p.failure.code,p.allowed()?"global":p.failure.field,p.allowed()?"":p.failure.detail,
            new DeploymentPreview.Limits(p.commandLimit,p.troopsMin,p.troopsMax,p.foodMin,p.foodMax,p.goldMin,p.goldMax),
            new DeploymentPreview.Resources(p.stockTroops,p.stockFood,p.stockGold,p.stockEquipment,p.stockShips),
            new DeploymentPreview.Resources(p.reserveTroops,p.reserveFood,p.reserveGold,0,0),
            new DeploymentPreview.Resources(p.troopsCost,p.foodCost,p.goldCost,p.equipmentCost,p.shipCost),
            new DeploymentPreview.Resources(p.remainingTroops,p.remainingFood,p.remainingGold,(long)p.stockEquipment-p.equipmentCost,(long)p.stockShips-p.shipCost),
            p.reserveEnforced,p.actionPointsAvailable,p.actionPointsCost,
            u==null?null:new DeploymentPreview.UnitFacts(u.exitQ,u.exitR,u.movementBudget,u.movementSpent,u.movementRemaining,u.aptitude,u.leadership,u.war,u.intelligence,
                u.baseAttack,u.baseDefense,u.attackRating,u.defenseRating,u.foodUse,u.foodTurns,u.energy,u.attackRange,u.equipmentLabel,skills,tactics));
    }
}
